package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.protocol.dtc.DtcFrame
import com.sarani.carobd.core.protocol.dtc.DtcParser
import kotlinx.coroutines.CancellationException

/** Production repository scan engine. Keeps each responder's result, including failures. */
class DiagnosticScanRunner(private val adapter: ObdAdapter, private val active: () -> Boolean = { true }) {
    suspend fun scan(): DiagnosticScanReport {
        val results = mutableListOf<ResponderScanResult>()
        for ((request, category) in listOf("03" to DtcCategory.STORED, "07" to DtcCategory.PENDING, "0A" to DtcCategory.PERMANENT)) {
            if (!active()) {
                results.add(ResponderScanResult(request, null, ScanOutcome.TRANSPORT_FAILURE, detail = "Session ended"))
                continue
            }
            val response = try { adapter.executeCommand(request, 4000L) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { AdapterResponse.Error(AdapterError.TransportFault(e.message.orEmpty()), "", 0) }
            val envelopes = response.toWireEnvelopes(request)
            if (envelopes.isEmpty()) results.add(ResponderScanResult(request, null, ScanOutcome.MALFORMED, detail = "No complete response"))
            for (env in envelopes) results.add(decode(request, category, env))
        }
        return DiagnosticScanReport(results)
    }
    private fun decode(request: String, category: DtcCategory, env: DiagnosticWireResponse): ResponderScanResult {
        fun result(outcome: ScanOutcome, detail: String? = null, codes: List<DiagnosticCode> = emptyList()) =
            ResponderScanResult(request, env.responderId, outcome, codes, env.rawLines, detail)
        return when (val r = env.result) {
            is DiagnosticResult.Positive -> {
                val expected = request.toInt(16) + 0x40
                if (r.service.positiveResponseByte != expected) return result(ScanOutcome.MALFORMED, "Wrong service")
                // The canonical envelope strips SID. Reconstruct it HERE in production,
                // once, for the explicitly full-frame DTC decoder contract.
                val full = byteArrayOf(expected.toByte()) + r.payload
                val frame = if (adapter.isCanTransport) DtcFrame.CAN_WITH_COUNT else DtcFrame.NON_CAN_PAIRS
                val valid = if (frame == DtcFrame.CAN_WITH_COUNT) {
                    val count = r.payload.firstOrNull()?.toInt()?.and(0xFF) ?: return result(ScanOutcome.MALFORMED, "Missing count")
                    val end = 2 + count * 2
                    full.size >= end && full.drop(end).all { it == 0.toByte() }
                } else r.payload.isNotEmpty() && r.payload.size % 2 == 0
                if (!valid) return result(ScanOutcome.MALFORMED, "Truncated/invalid DTC framing")
                val codes = DtcParser.parseMode03Payload(full, category, env.responderId, frame)
                if (frame == DtcFrame.CAN_WITH_COUNT && codes.size != (r.payload[0].toInt() and 0xFF))
                    return result(ScanOutcome.MALFORMED, "DTC count does not match decoded evidence")
                result(if (codes.isEmpty()) ScanOutcome.NO_CODES else ScanOutcome.CODES, codes = codes)
            }
            is DiagnosticResult.NegativeResponse -> result(ScanOutcome.NEGATIVE_RESPONSE, "NRC ${r.nrc}")
            is DiagnosticResult.NoData -> result(ScanOutcome.NO_DATA)
            DiagnosticResult.Timeout -> result(ScanOutcome.TIMEOUT)
            is DiagnosticResult.AdapterFailure -> result(ScanOutcome.TRANSPORT_FAILURE, r.detail)
            else -> result(ScanOutcome.MALFORMED, r.toString())
        }
    }
}
