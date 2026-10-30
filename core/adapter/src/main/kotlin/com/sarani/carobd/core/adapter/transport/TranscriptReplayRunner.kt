package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.adapter.command.*
import com.sarani.carobd.core.adapter.elm327.Elm327BufferParser
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

data class ReplayedCommand(val command: String, val evidence: List<DiagnosticWireResponse>, val detail: String? = null)
/** Offline only. No transport factory, Android dependency, routing or physical session admission. */
class TranscriptReplayRunner {
    suspend fun run(transcript: DiagnosticTranscript, timeoutMs: Long = 2500): List<ReplayedCommand> = coroutineScope {
        require(timeoutMs in 100..30_000)
        val transport = ReplayTransport(transcript, parentScope = this)
        val chars = Channel<Char>(65_536)
        val reader = launch {
            try { transport.incomingBytes.collect { bytes -> bytes.toString(Charsets.ISO_8859_1).forEach { chars.send(it) } } }
            finally { chars.close() }
        }
        val output = mutableListOf<ReplayedCommand>()
        transport.connect()
        try {
            for (tx in transcript.events.filter { it.direction == Direction.TX }) {
                val command = tx.command ?: tx.rawHex.removeSuffix("\r")
                try { transport.write((command + "\r").toByteArray(Charsets.US_ASCII)) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { output += ReplayedCommand(command, emptyList(), e.message); break }
                val buffer = StringBuilder()
                val prompt = withTimeoutOrNull(timeoutMs) {
                    while (true) {
                        val c = chars.receiveCatching().getOrNull() ?: return@withTimeoutOrNull false
                        buffer.append(c)
                        require(buffer.length <= 65_536)
                        if (c == '>') return@withTimeoutOrNull true
                    }
                    @Suppress("UNREACHABLE_CODE") false
                }
                if (prompt == true) {
                    output += ReplayedCommand(command, Elm327BufferParser.parseBuffer(buffer.toString(), command, 0).toWireEnvelopes(command))
                } else if (prompt == false) {
                    output += ReplayedCommand(command, AdapterResponse.Error(AdapterError.TransportFault("Replay disconnected"), buffer.toString(), 0).toWireEnvelopes(command), "Replay disconnected")
                    break
                } else {
                    output += ReplayedCommand(command, AdapterResponse.Error(AdapterError.Timeout(timeoutMs), buffer.toString(), timeoutMs).toWireEnvelopes(command))
                    val boundary = withTimeoutOrNull(500) {
                        while (true) { val c = chars.receiveCatching().getOrNull() ?: return@withTimeoutOrNull false; if (c == '>') return@withTimeoutOrNull true }
                        @Suppress("UNREACHABLE_CODE") false
                    }
                    if (boundary != true) break
                }
            }
        } finally { transport.disconnect(); reader.cancelAndJoin(); chars.cancel() }
        output
    }
}
