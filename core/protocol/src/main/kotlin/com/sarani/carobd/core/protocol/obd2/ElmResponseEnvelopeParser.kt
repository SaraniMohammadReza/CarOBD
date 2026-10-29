package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*

/** ELM-managed payload boundary. Accepts assembled payloads and visible CAN single
 * frames. Raw FF/CF streams and numbered ELM multi-frame dumps fail explicitly;
 * they are not silently merged or double-reassembled. Unknown source stays null. */
object ElmResponseEnvelopeParser {
    private val errors = listOf("STOPPED", "UNABLE TO CONNECT", "BUS INIT", "BUS BUSY", "CAN ERROR", "BUFFER FULL", "LV RESET", "?")
    fun parse(request: String, rawLines: List<String>, timestampMs: Long, elapsedMs: Long): List<DiagnosticWireResponse> {
        val lines = rawLines.filter { it.isNotBlank() && !it.trim().equals("SEARCHING...", true) }
        // ELM CAF1 numbered CAN output: explicit total length and sequence, one
        // headerless assembly only. Never infer a responder or merge ECU streams.
        if (request.startsWith("09") && lines.firstOrNull()?.trim()?.matches(Regex("[0-9A-Fa-f]{3}")) == true && lines.drop(1).any { it.contains(":") }) {
            val total = lines.first().trim().toInt(16)
            val bytes = mutableListOf<Byte>()
            var valid = total in 3..4095 && lines.size <= 600
            lines.drop(1).forEachIndexed { index, line ->
                val part = Regex("^([0-9A-Fa-f]):\\s*(.*)$").matchEntire(line.trim())
                if (part == null || part.groupValues[1].toInt(16) != index % 16) valid = false
                val data = part?.groupValues?.get(2)?.replace(" ", "").orEmpty()
                if (data.isEmpty() || data.length % 2 != 0 || data.length > 14 || !data.matches(Regex("[0-9A-Fa-f]+"))) valid = false
                else bytes.addAll(data.chunked(2).map { it.toInt(16).toByte() })
            }
            if (valid && bytes.size == total) {
                val decoded = parseLine(request, bytes.joinToString("") { "%02X".format(it) }, timestampMs, elapsedMs)
                val result = (decoded.result as? DiagnosticResult.Positive)?.copy(rawLines = lines) ?: decoded.result
                return listOf(decoded.copy(rawLines = lines, result = result))
            }
            return listOf(DiagnosticWireResponse(request, null, lines, byteArrayOf(), DiagnosticService.MODE_09, IsoTpFraming.RAW,
                timestampMs, elapsedMs, DiagnosticResult.Malformed("Invalid numbered ELM assembly", lines)))
        }
        return lines.map { line -> parseLine(request, line.trim().removePrefix("SEARCHING...").trim(), timestampMs, elapsedMs) }
    }

    private fun parseLine(request: String, line: String, ts: Long, elapsed: Long): DiagnosticWireResponse {
        var responder: String? = null
        var framing = IsoTpFraming.RAW
        fun envelope(result: DiagnosticResult, payload: ByteArray = byteArrayOf(), service: DiagnosticService = DiagnosticService.UNKNOWN) =
            DiagnosticWireResponse(request, responder, listOf(line), payload, service, framing, ts, elapsed, result)
        fun malformed(reason: String) = envelope(DiagnosticResult.Malformed(reason, listOf(line)))
        if (line.isEmpty() || line.length > 4096) return malformed("Empty/oversized response")
        var content = line
        val header = Regex("^([0-9A-Fa-f]{3}|[0-9A-Fa-f]{8})(?::|\\s+)(.*)$").matchEntire(content)
        if (header != null) {
            val id = header.groupValues[1].uppercase()
            val value = id.toLongOrNull(16) ?: return malformed("Invalid CAN identifier")
            if (value > if (id.length == 3) 0x7FF else 0x1FFFFFFF) return malformed("Invalid CAN identifier")
            responder = id
            content = header.groupValues[2].trim()
        }
        if (content.equals("NO DATA", true)) return envelope(DiagnosticResult.NoData(listOf(line)))
        if (errors.any { content.contains(it, true) }) return envelope(DiagnosticResult.AdapterFailure(content, listOf(line)))
        var data: String
        val tokens = content.uppercase().split(Regex("\\s+"))
        if (tokens.size > 1) {
            var start = 0
            val first = tokens.first()
            if ((first.length == 3 || first.length == 8) && first.toLongOrNull(16)?.let { it <= 0x1FFFFFFF } == true) {
                if (first.length == 3 && first.toInt(16) > 0x7FF) return malformed("Invalid CAN identifier")
                responder = first; start = 1
            }
            if (start == tokens.size || tokens.drop(start).any { !it.matches(Regex("[0-9A-F]{2}")) }) return malformed("Expected whole byte tokens")
            data = tokens.drop(start).joinToString("")
        } else {
            data = tokens.single()
            if (!data.matches(Regex("[0-9A-F]+"))) return malformed("Invalid hex")
            if ((data.startsWith("18DA") || data.startsWith("18DB")) && data.length > 8 && data.length % 2 == 0) {
                responder = data.take(8); data = data.drop(8)
            } else if (data.length % 2 == 1 && data.length >= 7 && data.take(3) in (0x7E8..0x7EF).map { it.toString(16).uppercase() }) {
                responder = data.take(3); data = data.drop(3)
            }
        }
        if (data.isEmpty() || data.length % 2 != 0) return malformed("Odd/truncated hex")
        // Every conversion is bounded to exactly one byte; malformed input never throws.
        var bytes = data.chunked(2).map { it.toIntOrNull(16)?.toByte() ?: return malformed("Invalid byte") }.toByteArray()
        if (responder != null && (bytes[0].toInt() and 0xFF) < 0x40) {
            val pci = bytes[0].toInt() and 0xFF
            if (pci !in 1..7 || bytes.size !in (pci + 1)..8) return malformed("Unsupported raw ISO-TP or invalid SF length")
            bytes = bytes.copyOfRange(1, pci + 1)
            framing = IsoTpFraming.SINGLE_FRAME
        }
        val requested = request.replace(" ", "").take(2).toIntOrNull(16) ?: return malformed("Invalid request")
        val first = bytes[0].toInt() and 0xFF
        if (first == 0x7F) {
            if (bytes.size < 3 || (bytes[1].toInt() and 0xFF) != requested) return malformed("Truncated/unrelated negative response")
            val svc = DiagnosticService.fromNegativeServiceByte(requested)
            return envelope(DiagnosticResult.NegativeResponse(responder, svc, bytes[2].toInt() and 0xFF, listOf(line)), bytes.drop(1).toByteArray(), svc)
        }
        val svc = DiagnosticService.fromPositiveResponseByte(first)
        if (svc == DiagnosticService.UNKNOWN || first != requested + 0x40) return malformed("Unexpected service response")
        when (requested) {
            1 -> {
                val pid = request.replace(" ", "").drop(2).take(2).toIntOrNull(16) ?: return malformed("Missing requested PID")
                val count = if (pid % 0x20 == 0) 4 else if (pid in setOf(0x0C, 0x10, 0x42)) 2 else 1
                if (bytes.size < 2 + count || (bytes[1].toInt() and 0xFF) != pid) return malformed("Truncated/unrelated PID")
            }
            3, 7, 10 -> if (bytes.size < 2) return malformed("Missing DTC payload")
            4 -> if (bytes.size != 1) return malformed("Unexpected clear payload")
            else -> if (bytes.size < 2) return malformed("Missing service payload")
        }
        val payload = bytes.copyOfRange(1, bytes.size)
        return envelope(DiagnosticResult.Positive(responder, svc, payload, listOf(line)), payload, svc)
    }
}
