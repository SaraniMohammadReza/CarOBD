package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.DiagnosticResult
import com.sarani.carobd.core.model.DiagnosticService
import com.sarani.carobd.core.model.DiagnosticWireResponse
import com.sarani.carobd.core.model.IsoTpFraming
import com.sarani.carobd.core.protocol.obd2.ElmResponseEnvelopeParser

/** Preserve primary adapter errors; partial bytes remain evidence, never authorization
 * for a positive outcome. Success lines pass through the canonical ELM parser. */
fun AdapterResponse.toWireEnvelopes(request: String): List<DiagnosticWireResponse> {
    val ts = System.currentTimeMillis()
    return when (this) {
        is AdapterResponse.Success ->
            ElmResponseEnvelopeParser.parse(request, cleanedLines, ts, executionTimeMs)

        is AdapterResponse.Error -> {
            val lines = if (rawResponse.isNotBlank()) listOf(rawResponse) else emptyList()
                // Nothing decodable: synthesize a canonical failure envelope.
                val result = when (error) {
                    is AdapterError.Timeout -> DiagnosticResult.Timeout
                    AdapterError.NoData -> DiagnosticResult.NoData(lines)
                    else -> DiagnosticResult.AdapterFailure(error.messageEn, lines)
                }
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
