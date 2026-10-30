package com.sarani.carobd.core.model

/**
 * Canonical diagnostic transcript (§38).
 *
 * Captures a complete request/response diagnostic session with transport,
 * adapter and protocol context, plus per-frame raw bytes, direction, timing and
 * how the parser classified each response. A transcript is the single source
 * for export (JSON/CSV) and for deterministic replay through the same parsing
 * stack, so a recorded physical session becomes a regression fixture.
 *
 * This model is transport-agnostic: recording happens at the [ObdTransport]
 * layer (raw TX/RX/ERR with timing), while responder/outcome annotation is
 * contributed by the adapter layer where framing is decoded.
 */

/** Direction of a frame on the diagnostic wire (§38). */
enum class Direction {
    TX,   // host -> adapter/ECU
    RX,   // adapter/ECU -> host
    ERR   // adapter-side error line (UNABLE TO CONNECT, CAN ERROR, ...)
}

/**
 * Outcome category for a recorded response (§24 outcome categories), preserved
 * per responder so a recording can capture positive/negative/mixed multi-ECU
 * results without collapsing them.
 */
enum class DiagnosticOutcome {
    POSITIVE,
    NEGATIVE_RESPONSE, // 7F <service> <NRC> — ECU responded and rejected
    NO_DATA,
    TIMEOUT,
    ADAPTER_FAILURE,  // UNABLE TO CONNECT / BUS INIT: ERROR / CAN ERROR / BUFFER FULL
    MALFORMED,
    PARTIAL,
    UNSUPPORTED,
    RAW
}

/** A single frame on the diagnostic wire, as observed by the transport. */
data class DiagnosticEvent(
    val direction: Direction,
    /** Wall-clock epoch ms when the frame was observed. */
    val timestampMs: Long,
    /** Elapsed ms since [TranscriptMetadata.startedAtMs] / first frame. */
    val elapsedMs: Long,
    /** Transmitted command as hex-with-spaces (e.g. "01 00"); null for RX/ERR. */
    val command: String? = null,
    /** Raw wire bytes (ASCII hex with spaces, e.g. "41 00 BE 3F B8 11"). */
    val rawHex: String,
    /** Hex responder address when known (e.g. "7E8"); null for TX/ERR/unknown. */
    val responder: String? = null,
    /** How the parser classified the response (transport records POSITIVE/raw). */
    val outcome: DiagnosticOutcome = DiagnosticOutcome.RAW,
    /** Protocol-level metadata (e.g. ISO-TP frame type, SID) when available. */
    val protocolMetadata: Map<String, String> = emptyMap()
)

/** Metadata describing the session that produced a transcript (§38). */
data class TranscriptMetadata(
    val profileName: String,
    val transportType: TransportType,
    /** Device address / IP / port / USB device identifier where appropriate. */
    val target: String? = null,
    /** Adapter identity reported by ATI, where available. */
    val adapterIdentity: String? = null,
    /** Vehicle protocol reported/applied (e.g. "ISO 15765-4 CAN 11/500"). */
    val vehicleProtocol: String? = null,
    val startedAtMs: Long = System.currentTimeMillis(),
    val sessionId: String = java.util.UUID.randomUUID().toString(),
    val commandTimeoutMs: Long? = null
)

/** An immutable, exportable diagnostic transcript. */
data class DiagnosticTranscript(
    val metadata: TranscriptMetadata,
    val events: List<DiagnosticEvent>
) {
    val endedAtMs: Long get() = events.lastOrNull()?.timestampMs ?: metadata.startedAtMs

    // ---- export -----------------------------------------------------------

    /**
     * JSON export (§38: JSON). Hand-serialized (no optional runtime dependency)
     * for a stable, inspectable regression-fixture format.
     */
    fun toJson(): String {
        val md = metadata
        val eventsJson = events.joinToString(separator = ",") { e ->
            val cmd = e.command?.let { jsonStr(it) } ?: "null"
            val rsp = e.responder?.let { jsonStr(it) } ?: "null"
            val meta = e.protocolMetadata.entries.joinToString(separator = ",") { (k, v) ->
                "${jsonStr(k)}:${jsonStr(v)}"
            }
            """{"dir":"${e.direction.name}","ts":${e.timestampMs},"el":${e.elapsedMs},""" +
                """"cmd":$cmd,"raw":"${hexEsc(e.rawHex)}","responder":$rsp,""" +
                """"outcome":"${e.outcome.name}","meta":{$meta}}"""
        }
        return """{"metadata":{"profileName":"${stringEsc(md.profileName)}","transport":"${md.transportType.name}",""" +
            """"target":${md.target?.let { jsonStr(it) } ?: "null"},""" +
            """"adapterIdentity":${md.adapterIdentity?.let { jsonStr(it) } ?: "null"},""" +
            """"vehicleProtocol":${md.vehicleProtocol?.let { jsonStr(it) } ?: "null"},""" +
            """"startedAtMs":${md.startedAtMs},"sessionId":${jsonStr(md.sessionId)},"commandTimeoutMs":${md.commandTimeoutMs ?: "null"}},"events":[$eventsJson]}"""
    }

    /**
     * CSV export (§38: CSV where suitable). One header row + one row per event.
     * Suitable for spreadsheet inspection of a recorded session.
     */
    fun toCsv(): String {
        val sb = StringBuilder()
        sb.append("direction,elapsedMs,command,rawHex,responder,outcome,protocolMetadata,timestampMs,sessionId,transport,adapter,protocol\n")
        for (e in events) {
            sb.append(csvField(e.direction.name))
                .append(',').append(e.elapsedMs)
                .append(',').append(csvField(e.command))
                .append(',').append(csvField(e.rawHex))
                .append(',').append(csvField(e.responder))
                .append(',').append(csvField(e.outcome.name))
                .append(',').append(csvField(e.protocolMetadata.entries.joinToString(";") { "${it.key}=${it.value}" }))
                .append(',').append(e.timestampMs)
                .append(',').append(csvField(metadata.sessionId))
                .append(',').append(csvField(metadata.transportType.name))
                .append(',').append(csvField(metadata.adapterIdentity))
                .append(',').append(csvField(metadata.vehicleProtocol))
                .append('\n')
        }
        return sb.toString()
    }

    private fun jsonStr(s: String): String = "\"" + stringEsc(s) + "\""
    private fun hexEsc(s: String): String = stringEsc(s) // rawHex contains only [0-9A-F ]
    private fun stringEsc(s: String): String = buildString {
        s.forEach { c -> when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            else -> if (c.code < 0x20) append("\\u%04X".format(c.code)) else append(c)
        } }
    }
    private fun csvField(s: String?): String {
        val v = s ?: ""
        return if (v.contains(',') || v.contains('\"') || v.contains('\n') || v.contains('\r')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }

    companion object {
        fun fromJson(json: String): DiagnosticTranscript = TranscriptJson.decode(json)
    }
}
