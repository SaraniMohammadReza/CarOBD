package com.sarani.carobd.core.model

/**
 * §10 RESPONDER-AWARE WIRE ENVELOPE.
 *
 * Canonical parsed representation of ONE diagnostic response line, produced by
 * [com.sarani.carobd.core.protocol.obd2.ElmResponseEnvelopeParser] at the adapter
 * boundary. It is deliberately distinct from [DiagnosticEvent] (the transport-level
 * transcript record): this type is the *parsed, classified* response that the
 * repository/domain layers consume, so responder identity and framing survive
 * BEFORE flattening into a live-telemetry / DTC / string value.
 *
 * §10 rules honored here:
 *  - responderId is the source address actually observed on the wire
 *    (e.g. 7E8/7E9/7EA/7EB for 11-bit CAN, 18DAF1xx for 29-bit). It is NEVER
 *    fabricated as "7E8": when the adapter/protocol provides no source header it
 *    stays [null] (explicitly UNKNOWN).
 *  - A single request may yield multiple envelopes (multi-ECU: distinct positive
 *    responders, a positive + a negative, same DID from two responders). Each
 *    responder line keeps its own identity — no response merging.
 *  - [result] carries the classification; [payload]/[service] carry decoded info
 *    but the raw line is always preserved in [rawLines].
 */
data class DiagnosticWireResponse(
    /** Hex of the command that produced this response, e.g. "01 0C". */
    val request: String,
    /** Observed target/source address (e.g. "7E8", "18DAF110") or null when unknown. */
    val responderId: String?,
    /** Raw wire lines that made up this response (spaces normalized to single space). */
    val rawLines: List<String>,
    /** Payload bytes after the leading response indicator byte (0x4N positive,
     * 0x7F negative), including the PID/DID echo for J1979/UDS. Downstream
     * decoders use [rawLines] for full fidelity; this is the convenience slice. */
    val payload: ByteArray,
    /** Diagnostic service inferred from the response (e.g. MODE_01 for 0x41). */
    val service: DiagnosticService,
    /** ISO-TP / transport framing annotation for this response. */
    val framing: IsoTpFraming,
    /** Wall-clock epoch ms the response was observed. */
    val timestampMs: Long,
    /** Elapsed ms for this response (adapter round-trip). */
    val elapsedMs: Long,
    /** Classified outcome. The payload-bearing cases mirror the §10 spec. */
    val result: DiagnosticResult
) {
    override fun equals(other: Any?): Boolean = other is DiagnosticWireResponse &&
        request == other.request && responderId == other.responderId &&
        rawLines == other.rawLines && service == other.service &&
        framing == other.framing && timestampMs == other.timestampMs &&
        elapsedMs == other.elapsedMs && result == other.result &&
        payload.contentEquals(other.payload)

    override fun hashCode(): Int {
        var h = request.hashCode()
        h = 31 * h + (responderId?.hashCode() ?: 0)
        h = 31 * h + rawLines.hashCode()
        h = 31 * h + payload.contentHashCode()
        h = 31 * h + service.hashCode()
        h = 31 * h + framing.hashCode()
        h = 31 * h + timestampMs.hashCode()
        h = 31 * h + elapsedMs.hashCode()
        h = 31 * h + result.hashCode()
        return h
    }
}

/** Diagnostic service bytes carried on the wire (§10: positive 0x4N = 0xN + 0x40). */
enum class DiagnosticService(val modeByte: Int, val positiveResponseByte: Int) {
    MODE_01(0x01, 0x41),
    MODE_02(0x02, 0x42),
    MODE_03(0x03, 0x43),
    MODE_04(0x04, 0x44),
    MODE_05(0x05, 0x45),
    MODE_06(0x06, 0x46),
    MODE_07(0x07, 0x47),
    MODE_08(0x08, 0x48),
    MODE_09(0x09, 0x49),
    MODE_0A(0x0A, 0x4A),
    UDS_22(0x22, 0x62),   // ReadDataByIdentifier
    UDS_2E(0x2E, 0x6E),   // WriteDataByIdentifier
    UNKNOWN(-1, -1);

    companion object {
        fun fromPositiveResponseByte(b: Int): DiagnosticService =
            entries.find { it.positiveResponseByte == (b and 0xFF) } ?: UNKNOWN

        /** Service byte embedded in a negative response (7F <sid> <nrc>). */
        fun fromNegativeServiceByte(b: Int): DiagnosticService =
            entries.find { it.modeByte == (b and 0xFF) } ?: UNKNOWN
    }
}

/** ISO-TP / transport framing shape (§26) for a single response line. */
enum class IsoTpFraming {
    SINGLE_FRAME,         // SF: response fits one frame
    FIRST_FRAME,          // FF: multi-frame start
    CONSECUTIVE_FRAME,    // CF
    FLOW_CONTROL,         // FC
    RAW                   // adapter opaque (ELM strips headers, framing unknown)
}

/**
 * §10 classification of a parsed response. Replaces ad-hoc "success vs string"
 * handling so the domain can reason about truthfulness (no 0-from-unknown, no
 * fabricated responder).
 */
sealed class DiagnosticResult {
    /** Positive response: 0x4N <pid/data>. */
    data class Positive(
        val responderId: String?,
        val service: DiagnosticService,
        val payload: ByteArray,
        val rawLines: List<String>
    ) : DiagnosticResult() {
        override fun equals(other: Any?) = other is Positive &&
            responderId == other.responderId && service == other.service &&
            payload.contentEquals(other.payload) && rawLines == other.rawLines
        override fun hashCode(): Int = 31 * (responderId?.hashCode() ?: 0) + service.hashCode()
    }

    /** Negative response: 7F <sid> <nrc>. */
    data class NegativeResponse(
        val responderId: String?,
        val service: DiagnosticService,
        val nrc: Int?,
        val rawLines: List<String>
    ) : DiagnosticResult() {
        override fun equals(other: Any?) = other is NegativeResponse &&
            responderId == other.responderId && service == other.service &&
            nrc == other.nrc && rawLines == other.rawLines
        override fun hashCode(): Int = (responderId?.hashCode() ?: 0) + 31 * (service.hashCode())
    }

    /** No data / empty positive (e.g. 43 00). */
    data class NoData(val rawLines: List<String>) : DiagnosticResult()

    /** No response within the command timeout. */
    data object Timeout : DiagnosticResult()

    /** Adapter-side failure (UNABLE TO CONNECT / CAN ERROR / BUS INIT / BUFFER FULL ...). */
    data class AdapterFailure(val detail: String?, val rawLines: List<String>) : DiagnosticResult()

    /** Response line present but could not be decoded as a known service. */
    data class Malformed(val reason: String, val rawLines: List<String>) : DiagnosticResult()

    /** Positive header present but payload incomplete/truncated (multi-ECU partial). */
    data class Partial(
        val responderId: String?,
        val service: DiagnosticService,
        val payload: ByteArray,
        val rawLines: List<String>
    ) : DiagnosticResult() {
        override fun equals(other: Any?) = other is Partial &&
            responderId == other.responderId && service == other.service &&
            payload.contentEquals(other.payload) && rawLines == other.rawLines
        override fun hashCode(): Int = (responderId?.hashCode() ?: 0) + 31 * service.hashCode()
    }

    /** Response for a service this stack does not model (e.g. UDS 10 session). */
    data class Unsupported(
        val responderId: String?,
        val service: DiagnosticService,
        val rawLines: List<String>
    ) : DiagnosticResult() {
        override fun equals(other: Any?) = other is Unsupported &&
            responderId == other.responderId && service == other.service && rawLines == other.rawLines
        override fun hashCode(): Int = (responderId?.hashCode() ?: 0) + 31 * service.hashCode()
    }
}
