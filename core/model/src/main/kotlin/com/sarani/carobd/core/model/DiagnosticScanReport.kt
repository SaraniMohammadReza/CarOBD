package com.sarani.carobd.core.model

enum class ScanCompleteness { COMPLETE, PARTIAL, FAILED }
enum class ScanOutcome { CODES, NO_CODES, NEGATIVE_RESPONSE, NO_DATA, TIMEOUT, MALFORMED, TRANSPORT_FAILURE }
data class ResponderScanResult(
    val request: String,
    val responder: String?,
    val outcome: ScanOutcome,
    val codes: List<DiagnosticCode> = emptyList(),
    val rawLines: List<String> = emptyList(),
    val detail: String? = null,
)
data class DiagnosticScanReport(val responders: List<ResponderScanResult>) {
    val codes: List<DiagnosticCode> get() = responders.flatMap { it.codes }
    val completeness: ScanCompleteness get() {
        val successful = responders.count { it.outcome == ScanOutcome.CODES || it.outcome == ScanOutcome.NO_CODES }
        return when {
            successful == 0 -> ScanCompleteness.FAILED
            successful == responders.size && setOf("03", "07", "0A").all { request -> responders.any { it.request == request } } -> ScanCompleteness.COMPLETE
            else -> ScanCompleteness.PARTIAL
        }
    }
}
class DiagnosticScanException(val report: DiagnosticScanReport) : Exception(
    "Diagnostic scan ${report.completeness}: " + report.responders.filter { it.outcome !in setOf(ScanOutcome.CODES, ScanOutcome.NO_CODES) }
        .joinToString { "${it.request}/${it.responder ?: "unknown"}: ${it.outcome} ${it.detail.orEmpty()}" }
)
