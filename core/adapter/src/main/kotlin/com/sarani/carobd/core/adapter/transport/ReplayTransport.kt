package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.DiagnosticTranscript
import com.sarani.carobd.core.model.Direction
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Replays a recorded [DiagnosticTranscript] as a live [ObdTransport] (§38).
 *
 * `write` is a no-op (replay is read-only); [incomingBytes] emits the recorded
 * RX frames in capture order, optionally honoring the recorded inter-frame
 * timing so the adapter's timeout / late-prompt logic can be exercised against
 * previously captured physical sessions. The emitted bytes flow through the
 * **same** parsing stack as a live transport, so a recording becomes a
 * regression fixture for framing, multi-ECU and ISO-TP behaviour.
 *
 * This is explicitly a software-only replay: [transportType] advertises the
 * *original* recording transport for traceability and never asserts a physical
 * link is present.
 */
class ReplayTransport(
    private val transcript: DiagnosticTranscript,
    /** When true, delay each RX frame by its recorded elapsed delta (deterministic). */
    private val replayTiming: Boolean = false
) : ObdTransport {

    /** Precomputed (ascii bytes, elapsedMs) of each recorded RX frame, in order. */
    private val rxFrames: List<Pair<ByteArray, Long>> = transcript.events
        .filter { it.direction == Direction.RX }
        .mapNotNull { e ->
            val ascii = e.rawHex.trim()
            if (ascii.isEmpty()) null else ascii.toByteArray(Charsets.US_ASCII) to e.elapsedMs
        }

    override val transportType: TransportType get() = transcript.metadata.transportType
    private var connected = false
    override val isConnected: Boolean get() = connected

    override suspend fun connect() {
        connected = true
    }

    override suspend fun disconnect() {
        connected = false
    }

    override suspend fun write(data: ByteArray) {
        // Read-only replay: commands are ignored; recorded RX is emitted on demand.
    }

    override val incomingBytes: Flow<ByteArray> = flow {
        var prev: Long? = null
        for ((bytes, elapsed) in rxFrames) {
            if (replayTiming && prev != null) {
                val delayMs = elapsed - prev
                if (delayMs > 0) delay(delayMs)
            }
            prev = elapsed
            emit(bytes)
        }
    }
}
