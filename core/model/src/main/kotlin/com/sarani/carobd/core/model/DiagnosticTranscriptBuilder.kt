package com.sarani.carobd.core.model

/** Serialized wire observation with monotonic elapsed time; snapshots never expose mutable lists. */
class DiagnosticTranscriptBuilder(private var metadata: TranscriptMetadata) {
    private val events = mutableListOf<DiagnosticEvent>()
    private val startedNanos = System.nanoTime()
    private var connected = false
    private var characters = 0
    @Synchronized fun recordTx(command: String, raw: String = command) = add(Direction.TX, command, raw)
    @Synchronized fun recordRx(rawHex: String, responder: String? = null, outcome: DiagnosticOutcome = DiagnosticOutcome.RAW,
        protocolMetadata: Map<String, String> = emptyMap()) = add(Direction.RX, null, rawHex, responder, outcome, protocolMetadata)
    @Synchronized fun recordErr(message: String) = add(Direction.ERR, null, message, outcome = DiagnosticOutcome.ADAPTER_FAILURE)
    @Synchronized fun recordParsed(command: String, envelope: DiagnosticWireResponse, timeoutMs: Long? = null) {
        val outcome = when (envelope.result) {
            is DiagnosticResult.Positive -> DiagnosticOutcome.POSITIVE
            is DiagnosticResult.NegativeResponse -> DiagnosticOutcome.NEGATIVE_RESPONSE
            is DiagnosticResult.NoData -> DiagnosticOutcome.NO_DATA
            DiagnosticResult.Timeout -> DiagnosticOutcome.TIMEOUT
            is DiagnosticResult.Malformed -> DiagnosticOutcome.MALFORMED
            is DiagnosticResult.Partial -> DiagnosticOutcome.PARTIAL
            is DiagnosticResult.Unsupported -> DiagnosticOutcome.UNSUPPORTED
            is DiagnosticResult.AdapterFailure -> DiagnosticOutcome.ADAPTER_FAILURE
        }
        add(Direction.ERR, command, envelope.rawLines.joinToString("\r"), envelope.responderId, outcome, mapOf("event" to "parsed", "service" to envelope.service.name) + (timeoutMs?.let { mapOf("timeoutMs" to it.toString()) } ?: emptyMap()))
    }
    @Synchronized fun updateIdentity(adapter: String?, protocol: String?) { metadata = metadata.copy(adapterIdentity = adapter, vehicleProtocol = protocol) }
    @Synchronized fun recordBench(phase: BenchPhase, profileId: String? = null, state: String? = null) {
        add(Direction.ERR, null, "", outcome = DiagnosticOutcome.RAW,
            extra = mapOf("event" to "bench", "phase" to phase.name) +
                (profileId?.let { mapOf("profileId" to it.take(80)) } ?: emptyMap()) +
                (state?.let { mapOf("state" to it.take(160)) } ?: emptyMap()))
    }
    @Synchronized fun recordConnect() { connected = true }
    @Synchronized fun recordDisconnect() {
        if (connected) add(Direction.ERR, null, "", outcome = DiagnosticOutcome.ADAPTER_FAILURE, extra = mapOf("event" to "disconnect"))
        connected = false
    }
    @Synchronized fun isLive(): Boolean = connected
    private fun add(direction: Direction, command: String?, raw: String, responder: String? = null,
        outcome: DiagnosticOutcome = DiagnosticOutcome.RAW, extra: Map<String, String> = emptyMap()) {
        check(events.size < 50_000) { "Recording event limit reached" }
        require(raw.length <= 65_536) { "Recording frame limit reached" }
        check(characters + raw.length <= 1_048_576) { "Recording byte budget reached" }
        characters += raw.length
        events += DiagnosticEvent(direction, System.currentTimeMillis(), (System.nanoTime() - startedNanos) / 1_000_000,
            command, raw, responder, outcome, extra.toMap())
    }
    @Synchronized fun toTranscript() = DiagnosticTranscript(metadata, events.toList())
}
