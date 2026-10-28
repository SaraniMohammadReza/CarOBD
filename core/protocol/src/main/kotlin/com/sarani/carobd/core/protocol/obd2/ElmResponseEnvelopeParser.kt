package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.DiagnosticResult
import com.sarani.carobd.core.model.DiagnosticService
import com.sarani.carobd.core.model.DiagnosticWireResponse
import com.sarani.carobd.core.model.IsoTpFraming

/**
 * §10 RESPONDER-AWARE WIRE PARSER.
 *
 * Pure, transport-agnostic parser that turns the cleaned ELM/STN response lines
 * for a request into one or more [DiagnosticWireResponse] envelopes — one per
 * responder line — so each responder keeps its own identity. This is the adapter
 * boundary where responder identity is decoded from the wire BEFORE flattening.
 *
 * §10 rules honored:
 *  - Responder address is taken ONLY from an explicit CAN header on the line
 *    (11-bit 7E8/7E9/7EA/7EB or 29-bit 18DAxxxxxx). When the adapter provides no
 *    source header (classic ELM strips it), responderId is [null] — never
 *    fabricated as "7E8".
 *  - Multi-responder: each line → its own envelope; distinct positive responders,
 *    positive + negative, and out-of-order lines each survive as separate
 *    envelopes (no merging).
 *  - Classification: positive (0x4N), negative (7F <sid> <nrc>), NoData when the
 *    line is empty/NO_DATA, AdapterFailure for ELM error strings, Malformed for
 *    undecodable hex.
 */
object ElmResponseEnvelopeParser {

    private val CAN_IDS_11BIT = setOf("7E8", "7E9", "7EA", "7EB", "7EC", "7ED", "7EE", "7EF")
    private val CAN_ID_29BIT = Regex("^(18DA[0-9A-Fa-f]{4})$")
    private val HEX_PAIR = Regex("^[0-9A-Fa-f]{2}$")
    // A response line is hex tokens of width 2..8: 8-bit data bytes (0x4N/7F/PID) or a
    // CAN responder header (7E8..7EF = 3, 18DAxxxxxx = 8, up to 29-bit).
    private val HEX_LINE = Regex("^[0-9A-Fa-f]{2,8}([\\s][0-9A-Fa-f]{2,8})*$")
    private val ELM_ERROR_KEYWORDS = arrayOf(
        "UNABLE TO CONNECT", "BUS INIT", "BUS BUSY", "CAN ERROR",
        "BUFFER FULL", "LV RESET", "STOPPED", "?"
    )

    /**
     * @param request Hex of the command that produced [rawLines], e.g. "01 0C".
     * @param rawLines Cleaned response lines (header/echo already stripped by the
     *   ELM buffer parser). May include explicit CAN responder headers.
     * @return One envelope per responder line. Empty when there is no decodable
     *   response line (caller maps emptiness + adapter errors to the session model).
     */
    fun parse(request: String, rawLines: List<String>, timestampMs: Long, elapsedMs: Long): List<DiagnosticWireResponse> {
        val out = mutableListOf<DiagnosticWireResponse>()
        for (line in rawLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.equals("NO DATA", ignoreCase = true)) {
                // A genuine ELM NO DATA token represents the request; emit one envelope.
                if (out.none { it.result is DiagnosticResult.NoData })
                    out.add(noData(request, timestampMs, elapsedMs, listOf(trimmed)))
                continue
            }
            // ELM error strings (transport-level) -> AdapterFailure; other non-hex
            // lines are Malformed (the adapter Success path should not emit these,
            // but be defensive at the boundary).
            if (!HEX_LINE.matches(trimmed)) {
                val err = ELM_ERROR_KEYWORDS.any { kw -> trimmed.contains(kw, ignoreCase = true) }
                val outcome = if (err) DiagnosticResult.AdapterFailure(trimmed, listOf(trimmed))
                else DiagnosticResult.Malformed("non-hex line", listOf(trimmed))
                out.add(DiagnosticWireResponse(
                    request = request, responderId = null, rawLines = listOf(trimmed),
                    payload = byteArrayOf(), service = DiagnosticService.UNKNOWN,
                    framing = IsoTpFraming.RAW, timestampMs = timestampMs, elapsedMs = elapsedMs,
                    result = outcome
                ))
                continue
            }
            envelope(request, trimmed, timestampMs, elapsedMs)?.let { out.add(it) }
        }
        return out
    }

    private fun envelope(request: String, line: String, ts: Long, elapsed: Long): DiagnosticWireResponse? {
        val tokens = line.split(Regex("[\\s]+")).filter { it.isNotEmpty() }
        var responder: String? = null
        var idx = 0
        // Explicit CAN responder header (11-bit 7E8..7EF, or 29-bit 18DAxxxxxx).
        if (tokens.isNotEmpty() && isResponder(tokens[0])) {
            responder = tokens[0].uppercase()
            idx = 1
        }
        if (idx >= tokens.size || !HEX_PAIR.matches(tokens[idx])) return null

        val first = tokens[idx].uppercase()
        val rest = tokens.subList(idx + 1, tokens.size)

        // Negative response: 7F <service> <nrc> ...
        if (first == "7F") {
            val serviceByte = rest.firstOrNull()?.let { it.toInt(16) }
            val nrc = rest.getOrNull(1)?.let { it.toInt(16) }
            val service = serviceByte?.let { DiagnosticService.fromNegativeServiceByte(it) } ?: DiagnosticService.UNKNOWN
            val payload = rest.map { it.toInt(16).toByte() }.toByteArray()
            return DiagnosticWireResponse(
                request = request, responderId = responder, rawLines = listOf(line.trim()),
                payload = payload, service = service,
                framing = if (responder != null) IsoTpFraming.SINGLE_FRAME else IsoTpFraming.RAW,
                timestampMs = ts, elapsedMs = elapsed,
                result = DiagnosticResult.NegativeResponse(responder, service, nrc, listOf(line.trim()))
            )
        }

        val respByte = first.toInt(16)
        val service = DiagnosticService.fromPositiveResponseByte(respByte)
        if (service == DiagnosticService.UNKNOWN) {
            return DiagnosticWireResponse(
                request = request, responderId = responder, rawLines = listOf(line.trim()),
                payload = byteArrayOf(), service = DiagnosticService.UNKNOWN,
                framing = if (responder != null) IsoTpFraming.SINGLE_FRAME else IsoTpFraming.RAW,
                timestampMs = ts, elapsedMs = elapsed,
                result = DiagnosticResult.Unsupported(responder, DiagnosticService.UNKNOWN, listOf(line.trim()))
            )
        }

        val payload = rest.map { it.toInt(16).toByte() }.toByteArray()
        val framing = if (responder != null) IsoTpFraming.SINGLE_FRAME else IsoTpFraming.RAW
        return DiagnosticWireResponse(
            request = request, responderId = responder, rawLines = listOf(line.trim()),
            payload = payload, service = service, framing = framing,
            timestampMs = ts, elapsedMs = elapsed,
            result = DiagnosticResult.Positive(responder, service, payload, listOf(line.trim()))
        )
    }

    private fun isResponder(token: String): Boolean {
        val t = token.uppercase()
        if (t in CAN_IDS_11BIT) return true
        if (CAN_ID_29BIT.matches(t)) return true
        // A bare 2-hex token is a data byte (service/response byte), not a responder.
        return false
    }

    private fun noData(request: String, ts: Long, elapsed: Long, lines: List<String>): DiagnosticWireResponse =
        DiagnosticWireResponse(
            request = request, responderId = null, rawLines = lines, payload = byteArrayOf(),
            service = DiagnosticService.UNKNOWN, framing = IsoTpFraming.RAW,
            timestampMs = ts, elapsedMs = elapsed,
            result = DiagnosticResult.NoData(lines)
        )
}
