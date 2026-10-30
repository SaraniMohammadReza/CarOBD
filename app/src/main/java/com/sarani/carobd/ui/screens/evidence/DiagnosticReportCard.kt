package com.sarani.carobd.ui.screens.evidence

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R
import com.sarani.carobd.core.model.*
import com.sarani.carobd.ui.components.*

@Composable
internal fun DiagnosticReportCard(report: DiagnosticSessionReport) {
    var expanded by remember(report.id) { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(report.startedAtMs)))
            Text(stringResource(if (report.simulator) R.string.connection_simulator else R.string.diagnosis_physical))
            Text("${report.vehicleLabel ?: "—"} · ${report.profileLabel ?: "—"}")
            Text(stringResource(R.string.diagnosis_scope))
            Text(stringResource(when (report.completeness) {
                ScanCompleteness.COMPLETE -> R.string.diagnosis_complete
                ScanCompleteness.PARTIAL -> R.string.diagnosis_partial
                ScanCompleteness.FAILED -> R.string.diagnosis_failed
            }))
            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.diagnosis_codes, report.codes.size, report.codes.size))
            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.diagnosis_responders, report.responders.size, report.responders.size))
            Text(stringResource(R.string.diagnosis_unknown_count))
            Text(stringResource(R.string.diagnosis_not_collected))
            if (expanded) {
                Text("${report.observedProtocol ?: "—"} · ${report.adapterIdentity ?: "—"}")
                Text(stringResource(R.string.diagnosis_adapter_reported))
                DtcCategory.entries.filter { it != DtcCategory.HISTORY }.forEach { category ->
                    Text(categoryLabel(category), style = MaterialTheme.typography.titleSmall)
                    val rows = report.codes.filter { it.category == category }
                    rows.forEach { DiagnosticIdentifier("${it.code} · ${it.responder ?: "—"}") }
                    // Empty codes do not imply a successful category; retain every outcome below.
                    if (rows.isEmpty()) Text("—")
                }
                report.outcomes.forEach { row ->
                    val outcome = when (row.outcome) {
                        ScanOutcome.CODES, ScanOutcome.NO_CODES -> ReadingState.Success
                        ScanOutcome.NEGATIVE_RESPONSE -> ReadingState.NegativeResponse
                        ScanOutcome.NO_DATA -> ReadingState.NoData
                        ScanOutcome.TIMEOUT -> ReadingState.Timeout
                        ScanOutcome.MALFORMED -> ReadingState.Malformed
                        ScanOutcome.TRANSPORT_FAILURE -> ReadingState.AdapterFailure
                    }
                    Text("${row.request} · ${row.responder ?: "—"} · ${readingLabel(outcome)}")
                    if (row.outcome == ScanOutcome.NO_CODES) Text(stringResource(R.string.diagnosis_category_no_codes))
                    DeveloperEvidence("${row.request} · ${row.responder ?: "—"} · ${row.outcome}\n${row.detail.orEmpty()}\n${row.rawLines.joinToString("\n")}".take(6000))
                }
                DeveloperEvidence("${report.connectionSessionId ?: "—"} · ${report.startedAtMs} → ${report.finishedAtMs}")
                Text(stringResource(R.string.evidence_limitations))
            }
        }
    }
}
