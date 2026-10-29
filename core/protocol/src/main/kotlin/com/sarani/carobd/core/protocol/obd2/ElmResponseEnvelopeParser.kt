package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*

/** ELM-managed payload boundary. Accepts assembled payloads and visible CAN single
 * frames. Raw FF/CF streams and numbered ELM multi-frame dumps fail explicitly;
 * they are not silently merged or double-reassembled. Unknown source stays null. */
object ElmResponseEnvelopeParser {
    private val errors = listOf("STOPPED", "UNABLE TO CONNECT", "BUS INIT", "BUS BUSY", "CAN ERROR", "BUFFER FULL", "LV RESET", "?")
    fun parse(request: String, rawLines: List<String>, timestampMs: Long, elapsedMs: Long): List<DiagnosticWireResponse> =
        rawLines.filter { it.isNotBlank() && !it.trim().equals("SEARCHING...", true) }.map { line ->
            parseLine(request, line.trim().removePrefix("SEARCHING...").trim(), timestampMs, elapsedMs)
        }

    private fun parseLine(request: String, line: String, ts: Long, elapsed: Long): DiagnosticWireResponse {
        var responder: String? = null
        var framing = IsoTpFraming.RAW
        fun envelope(result: DiagnosticResult, payload: ByteArray = byteArrayOf(), service: DiagnosticService = DiagnosticService.UNKNOWN) =
            DiagnosticWireResponse(request, responder, listOf(line), payload, service, framing, ts, elapsed, result)
        fun malformed(reason: String) = envelope(DiagnosticResult.Malformed(reason, listOf(line)))
        if (line.equals("NO DATA", true)) return envelope(DiagnosticResult.NoData(listOf(line)))
        if (errors.any { line.contains(it, true) }) return envelope(DiagnosticResult.AdapterFailure(line, listOf(line)))
        if (line.isEmpty() || line.length > 4096) return malformed("Empty/oversized response")
        var data: String
        val tokens = line.uppercase().split(Regex("\\s+"))
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
