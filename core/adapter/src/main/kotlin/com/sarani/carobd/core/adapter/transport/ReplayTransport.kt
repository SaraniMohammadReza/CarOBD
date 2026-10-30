package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.adapter.safety.AuthorizedDiagnosticCommand
import com.sarani.carobd.core.adapter.safety.SingleCommandParser
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicBoolean

/** Isolated one-shot, command-correlated replay. There is no physical delegate. */
class ReplayTransport(
    private val transcript: DiagnosticTranscript,
    private val replayTiming: Boolean = true,
    parentScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : ObdTransport {
    private val owner = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + owner)
    private val incoming = Channel<ByteArray>(64)
    private val collected = AtomicBoolean(false)
    private val segments = transcript.events.indices.filter { transcript.events[it].direction == Direction.TX }
    private var position = 0
    private var pending: Job? = null
    private var retired = false
    @Volatile private var connected = false
    override val transportType = TransportType.SIMULATOR
    override val isConnected get() = connected
    override suspend fun connect() { check(!retired) { "Replay retired" }; connected = true }
    override suspend fun disconnect() { connected = false; retired = true; owner.cancel(); incoming.close() }
    override suspend fun write(data: ByteArray) = synchronized(this) {
        check(connected && !retired) { "Replay disconnected" }
        check(pending?.isActive != true) { "Replay response still pending" }
        val command = SingleCommandParser.canonicalize(data.toString(Charsets.US_ASCII).removeSuffix("\r"))
            ?: error("Invalid replay command")
        check(AuthorizedDiagnosticCommand.initialization(command) != null) { "Unsafe replay command denied" }
        check(position < segments.size) { "Replay exhausted" }
        val start = segments[position]
        val tx = transcript.events[start]
        val expected = SingleCommandParser.canonicalize((tx.command ?: tx.rawHex).removeSuffix("\r"))
        check(command == expected) { "Replay command mismatch: expected $expected" }
        val end = segments.getOrNull(position + 1) ?: transcript.events.size
        val response = transcript.events.subList(start + 1, end)
        require(response.all { it.elapsedMs >= tx.elapsedMs && it.elapsedMs - tx.elapsedMs <= 60_000 }) { "Replay timing outside bounded window" }
        position++
        pending = scope.launch {
            var previous = tx.elapsedMs
            for (event in response) {
                if (replayTiming) delay(event.elapsedMs - previous)
                previous = event.elapsedMs
                if (!connected) break
                if (event.direction == Direction.RX) incoming.send(event.rawHex.toByteArray(Charsets.ISO_8859_1))
                if (event.protocolMetadata["event"] == "disconnect") { connected = false; incoming.close(); break }
            }
        }
    }
    override val incomingBytes = flow {
        check(collected.compareAndSet(false, true)) { "Replay has one reader" }
        for (bytes in incoming) emit(bytes)
    }
}
