package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.DiagnosticTranscriptBuilder
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * Decorates an [ObdTransport], recording every TX/RX/ERR frame together with
 * timing into a [DiagnosticTranscriptBuilder] (§38).
 *
 * Transparent to the adapter: [incomingBytes] is the same byte sequence the
 * delegate would have produced, so the existing parsing stack is unchanged.
 * Transport-layer recording captures raw wire fidelity (timestamp, elapsed, TX,
 * RX, ERR); the adapter layer is responsible for annotating responder/outcome
 * once framing is decoded. Adapter write failures are recorded as [ERR] before
 * being rethrown.
 */
class RecordingTransport(
    private val delegate: ObdTransport,
    private val builder: DiagnosticTranscriptBuilder
) : ObdTransport {

    override val transportType: TransportType get() = delegate.transportType
    override val isConnected: Boolean get() = delegate.isConnected

    override suspend fun connect() {
        delegate.connect()
        builder.recordConnect()
    }

    override suspend fun disconnect() {
        try {
            delegate.disconnect()
        } finally {
            builder.recordDisconnect()
        }
    }

    override suspend fun write(data: ByteArray) {
        val command = data.toString(Charsets.US_ASCII).trim()
        try {
            delegate.write(data)
            builder.recordTx(command)
        } catch (e: TransportException) {
            builder.recordErr("write failed: ${e.message}")
            throw e
        }
    }

    override val incomingBytes: Flow<ByteArray> = delegate.incomingBytes
        .onEach { bytes -> builder.recordRx(bytes.toString(Charsets.US_ASCII).trim()) }
}
