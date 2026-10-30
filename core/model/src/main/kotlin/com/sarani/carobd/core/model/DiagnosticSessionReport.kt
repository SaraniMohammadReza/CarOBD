package com.sarani.carobd.core.model

/** Immutable evidence for the executed 03/07/0A scope, never a vehicle health assessment. */
data class DiagnosticSessionReport(
    val id: String,
    val connectionSessionId: String?,
    val startedAtMs: Long,
    val finishedAtMs: Long,
    val vehicleId: String?,
    val vehicleLabel: String?,
    val profileId: String?,
    val profileLabel: String?,
    val simulator: Boolean,
    val observedProtocol: String?,
    /** Adapter-reported identity; does not establish authenticity. */
    val adapterIdentity: String?,
    val outcomes: List<DiagnosticReportOutcome>,
) {
    val scope: String get() = "DTC_03_07_0A"
    val completeness: ScanCompleteness get() = DiagnosticScanReport(outcomes.map {
        ResponderScanResult(it.request, it.responder, it.outcome)
    }).completeness
    val responders: Set<String> get() = outcomes.mapNotNull { it.responder }.toSet()
    val codes: List<DiagnosticReportCode> get() = outcomes.flatMap { it.codes }.distinct()

    companion object {
        fun outcomes(scan: DiagnosticScanReport): List<DiagnosticReportOutcome> = scan.responders.map { row ->
            DiagnosticReportOutcome(row.request, row.responder, row.outcome,
                row.codes.map { DiagnosticReportCode(it.code, it.category, it.responderAddressHex) }, row.rawLines, row.detail)
        }
    }
}

data class DiagnosticReportCode(val code: String, val category: DtcCategory, val responder: String?)
data class DiagnosticReportOutcome(val request: String, val responder: String?, val outcome: ScanOutcome,
    val codes: List<DiagnosticReportCode> = emptyList(), val rawLines: List<String> = emptyList(), val detail: String? = null)
