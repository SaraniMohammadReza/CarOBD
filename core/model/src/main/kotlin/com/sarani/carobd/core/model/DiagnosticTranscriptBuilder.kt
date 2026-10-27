package com.sarani.carobd.core.model

/**
 * Mutable accumulator that builds a [DiagnosticTranscript] as frames cross the
 * wire (§38). The [RecordingTransport] feeds it; the adapter layer can also
 * annotate events with responder/outcome after parsing.
 */
class DiagnosticTranscriptBuilder(private val metadata: TranscriptMetadata) {

    private val events = mutableListOf<DiagnosticEvent>()
    private val startedAt = metadata.startedAtMs
    private var connected = false

    /** Records a host->wire transmit (command already ASCII-hex normalized). */
    fun recordTx(command: String) = add(Direction.TX, command, command)

    /** Records a wire->host receive (raw ASCII-hex frame, already normalized). */
    fun recordRx(rawHex: String, responder: String? = null,
                 outcome: DiagnosticOutcome = DiagnosticOutcome.POSITIVE,
                 protocolMetadata: Map<String, String> = emptyMap()) =
        add(Direction.RX, null, rawHex, responder, outcome, protocolMetadata)

    /** Records an adapter-side error line (UNABLE TO CONNECT, CAN ERROR, ...). */
    fun recordErr(message: String) = add(Direction.ERR, null, message, null,
        DiagnosticOutcome.ADAPTER_FAILURE)

    fun recordConnect() {
        connected = true
        // transport connect is implicit in startedAt; no frame emitted.
    }

    fun recordDisconnect() {
        connected = false
    }

    fun isLive(): Boolean = connected

    private fun add(direction: Direction, command: String?, rawHex: String,
                    responder: String? = null,
                    outcome: DiagnosticOutcome = DiagnosticOutcome.POSITIVE,
                    protocolMetadata: Map<String, String> = emptyMap()) {
        val now = System.currentTimeMillis()
        events.add(
            DiagnosticEvent(
                direction = direction,
                timestampMs = now,
                elapsedMs = now - startedAt,
                command = command,
                rawHex = rawHex,
                responder = responder,
                outcome = outcome,
                protocolMetadata = protocolMetadata
            )
        )
    }

    /** Snapshotted, immutable transcript ready for export or replay. */
    fun toTranscript(): DiagnosticTranscript = DiagnosticTranscript(metadata, events.toList())
}
