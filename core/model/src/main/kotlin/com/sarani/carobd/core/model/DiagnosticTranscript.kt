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
    UNSUPPORTED
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
    val outcome: DiagnosticOutcome = DiagnosticOutcome.POSITIVE,
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
    val startedAtMs: Long = System.currentTimeMillis()
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
            """"startedAtMs":${md.startedAtMs}},"events":[$eventsJson]}"""
    }

    /**
     * CSV export (§38: CSV where suitable). One header row + one row per event.
     * Suitable for spreadsheet inspection of a recorded session.
     */
    fun toCsv(): String {
        val sb = StringBuilder()
        sb.append("direction,elapsedMs,command,rawHex,responder,outcome,protocolMetadata\n")
        for (e in events) {
            sb.append(csvField(e.direction.name))
                .append(',').append(e.elapsedMs)
                .append(',').append(csvField(e.command))
                .append(',').append(csvField(e.rawHex))
                .append(',').append(csvField(e.responder))
                .append(',').append(csvField(e.outcome.name))
                .append(',').append(csvField(e.protocolMetadata.entries.joinToString(";") { "${it.key}=${it.value}" }))
                .append('\n')
        }
        return sb.toString()
    }

    private fun jsonStr(s: String): String = "\"" + stringEsc(s) + "\""
    private fun hexEsc(s: String): String = stringEsc(s) // rawHex contains only [0-9A-F ]
    private fun stringEsc(s: String): String = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun csvField(s: String?): String {
        val v = s ?: ""
        return if (v.contains(',') || v.contains('\"') || v.contains('\n')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else {
            v
        }
    }

    companion object {
        /**
         * Minimal schema-aware JSON reader for the [toJson] format (§38: reload
         * recorded sessions as regression fixtures). Robust to field ordering
         * within an event object.
         */
        fun fromJson(json: String): DiagnosticTranscript {
            val md = parseMetadata(json)
            val events = parseEvents(json).map { e ->
                DiagnosticEvent(
                    direction = Direction.valueOf(str(e, "dir")!!),
                    timestampMs = num(e, "ts"),
                    elapsedMs = num(e, "el"),
                    command = str(e, "cmd"),
                    rawHex = str(e, "raw")!!,
                    responder = str(e, "responder"),
                    outcome = DiagnosticOutcome.valueOf(str(e, "outcome") ?: "POSITIVE"),
                    protocolMetadata = parseMeta(extractObject(e, "meta"))
                )
            }
            return DiagnosticTranscript(md, events)
        }

        private fun parseMetadata(json: String): TranscriptMetadata {
            val m = json.substringAfter("\"metadata\":", "").takeIf { it.startsWith("{") }
                ?: return TranscriptMetadata("", TransportType.SIMULATOR)
            return TranscriptMetadata(
                profileName = str(m, "profileName") ?: "",
                transportType = TransportType.valueOf(str(m, "transport") ?: "SIMULATOR"),
                target = str(m, "target"),
                adapterIdentity = str(m, "adapterIdentity"),
                vehicleProtocol = str(m, "vehicleProtocol"),
                startedAtMs = num(m, "startedAtMs")
            )
        }

        private fun parseEvents(json: String): List<String> {
            val start = json.indexOf("\"events\":[")
            if (start < 0) return emptyList()
            var i = start + "\"events\":".length
            while (i < json.length && json[i] != '[') i++
            if (i >= json.length) return emptyList()
            // capture balanced '[ ... ]' then split top-level '{ }' objects
            val end = findClosingArray(json, i)
            val body = json.substring(i + 1, end)
            val out = mutableListOf<String>()
            var depth = 0
            var s = -1
            for (j in body.indices) {
                when (body[j]) {
                    '{' -> { if (depth == 0) s = j; depth++ }
                    '}' -> { depth--; if (depth == 0) { out.add(body.substring(s, j + 1)); s = -1 } }
                }
            }
            return out
        }

        private fun findClosingArray(s: String, open: Int): Int {
            var depth = 0
            var i = open
            while (i < s.length) {
                when (s[i]) {
                    '[' -> depth++
                    ']' -> { depth--; if (depth == 0) return i }
                }
                i++
            }
            return s.length - 1
        }

        private fun parseMeta(meta: String?): Map<String, String> {
            val wrapped = meta?.takeIf { it.startsWith("{") && it.endsWith("}") }
            val body = wrapped?.substring(1, wrapped.length - 1)?.trim()
            if (body.isNullOrEmpty()) return emptyMap()
            val map = LinkedHashMap<String, String>()
            var i = 0
            while (i < body.length) {
                val k = readStr(body, i) ?: break
                i = k.second
                i = skipWs(body, i)
                if (i >= body.length || body[i] != ':') break
                i++
                i = skipWs(body, i)
                val v = readStr(body, i) ?: break
                map[k.first] = v.first
                i = v.second
                i = skipWs(body, i)
                if (i < body.length && body[i] == ',') i++
                i = skipWs(body, i)
            }
            return map
        }

        private fun extractObject(obj: String, key: String): String? {
            val m = "\"$key\":".toRegex().find(obj) ?: return null
            var i = m.range.last + 1          // just past the colon
            while (i < obj.length && (obj[i] == ' ' || obj[i] == '\t')) i++
            if (i >= obj.length || obj[i] != '{') return null
            var depth = 0
            val start = i
            while (i < obj.length) {
                when (obj[i]) {
                    '{' -> depth++
                    '}' -> { depth--; if (depth == 0) return obj.substring(start, i + 1) }
                }
                i++
            }
            return null
        }

        private fun str(obj: String, key: String): String? {
            val m = "\"$key\":".toRegex().find(obj) ?: return null
            var i = m.range.last + 1           // position just past the colon
            while (i < obj.length && (obj[i] == ' ' || obj[i] == '\t')) i++
            if (i >= obj.length) return null
            if (obj[i] == '"') {
                val sb = StringBuilder(); i++
                while (i < obj.length) {
                    val c = obj[i]
                    if (c == '\\' && i + 1 < obj.length) { sb.append(obj[i + 1]); i += 2; continue }
                    if (c == '"') return sb.toString()
                    sb.append(c); i++
                }
                return sb.toString()
            }
            // unquoted literal (null/true/false) — treat null as absent.
            return if (obj.startsWith("null", i)) null else null
        }

        private fun readStr(s: String, i: Int): Pair<String, Int>? {
            var j = i
            while (j < s.length && s[j] != '"') j++
            if (j >= s.length) return null
            val sb = StringBuilder()
            j++
            while (j < s.length) {
                val c = s[j]
                if (c == '\\' && j + 1 < s.length) { sb.append(s[j + 1]); j += 2; continue }
                if (c == '"') return sb.toString() to (j + 1)
                sb.append(c); j++
            }
            return sb.toString() to (j + 1)
        }

        private fun num(obj: String, key: String): Long {
            val pat = "\"$key\":".toRegex()
            val m = pat.find(obj) ?: return 0L
            var i = m.range.last + 1           // past the colon
            while (i < obj.length && (obj[i] == ' ' || obj[i] == '\t')) i++
            val sb = StringBuilder()
            while (i < obj.length && (obj[i].isDigit() || obj[i] == '-')) { sb.append(obj[i]); i++ }
            return sb.toString().toLongOrNull() ?: 0L
        }

        private fun skipWs(s: String, i: Int): Int {
            var j = i
            while (j < s.length && (s[j] == ' ' || s[j] == ',' || s[j] == '\t')) j++
            return j
        }
    }
}
