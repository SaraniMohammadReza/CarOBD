package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.DiagnosticResult
import com.sarani.carobd.core.model.DiagnosticService
import com.sarani.carobd.core.model.DiagnosticWireResponse
import com.sarani.carobd.core.model.IsoTpFraming
import com.sarani.carobd.core.protocol.obd2.ElmResponseEnvelopeParser

/**
 * §7 canonical production response path.
 *
 * Converts an [AdapterResponse] into responder-aware [DiagnosticWireResponse]
 * envelopes via [ElmResponseEnvelopeParser]. This is the single choke point
 * where raw ELM/STN output enters the diagnostic stack — every production
 * service decoder consumes envelopes, never flattened bytes.
 *
 * Success: cleaned lines are parsed by ElmResponseEnvelopeParser, which
 *   classifies each line as Positive / NegativeResponse / Malformed / etc.
 * Error:   the error token or partial buffer is fed to the parser; if nothing
 *   decodable is present, a Timeout or AdapterFailure envelope is synthesized.
 */
fun AdapterResponse.toWireEnvelopes(request: String): List<DiagnosticWireResponse> {
    val ts = System.currentTimeMillis()
    return when (this) {
        is AdapterResponse.Success ->
            ElmResponseEnvelopeParser.parse(request, cleanedLines, ts, executionTimeMs)

        is AdapterResponse.Error -> {
            val lines = if (rawResponse.isNotBlank()) listOf(rawResponse) else emptyList()
            val parsed = ElmResponseEnvelopeParser.parse(request, lines, ts, executionTimeMs)
            if (parsed.isNotEmpty()) {
                parsed
            } else {
                // Nothing decodable: synthesize a canonical failure envelope.
                val result = if (error is AdapterError.Timeout)
                    DiagnosticResult.Timeout
                else
                    DiagnosticResult.AdapterFailure(error.messageEn, lines)
                listOf(
                    DiagnosticWireResponse(
                        request = request,
                        responderId = null,
                        rawLines = lines,
                        payload = byteArrayOf(),
                        service = DiagnosticService.UNKNOWN,
                        framing = IsoTpFraming.RAW,
                        timestampMs = ts,
                        elapsedMs = executionTimeMs,
                        result = result
                    )
                )
            }
        }
    }
}
