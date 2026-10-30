package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.onEach

/** Passive wire observation. Recorder failures never change transport dispatch/results. */
class RecordingTransport(
    private val delegate: ObdTransport,
    private val builder: () -> DiagnosticTranscriptBuilder?,
    private val onRecordingFailure: (Throwable) -> Unit = {},
) : ObdTransport {
    constructor(delegate: ObdTransport, builder: DiagnosticTranscriptBuilder) : this(delegate, { builder })
    override val transportType get() = delegate.transportType
    override val isConnected get() = delegate.isConnected
    private fun observe(block: (DiagnosticTranscriptBuilder) -> Unit) {
        try { builder()?.let(block) } catch (e: Exception) { runCatching { onRecordingFailure(e) } }
    }
    override suspend fun connect() { delegate.connect(); observe { it.recordConnect() } }
    override suspend fun disconnect() { try { delegate.disconnect() } finally { observe { it.recordDisconnect() } } }
    override suspend fun write(data: ByteArray) {
        val raw = data.toString(Charsets.ISO_8859_1)
        observe { it.recordTx(raw.removeSuffix("\r"), raw) }
        try { delegate.write(data) } catch (e: Exception) { observe { it.recordErr("write failed: ${e.message}") }; throw e }
    }
    override val incomingBytes = delegate.incomingBytes.onEach { bytes -> observe { it.recordRx(bytes.toString(Charsets.ISO_8859_1)) } }
}
