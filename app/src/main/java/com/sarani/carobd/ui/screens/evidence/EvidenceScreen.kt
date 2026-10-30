package com.sarani.carobd.ui.screens.evidence

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R

@Composable
fun EvidenceScreen(viewModel: ReadinessViewModel, onVehicleInfo: () -> Unit, onFreezeFrame: () -> Unit, onRecordings: () -> Unit, onHistory: () -> Unit, onMonitorTests: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onMonitorTests) { Text(stringResource(R.string.monitor_tests)) }
        TextButton(onClick = onHistory) { Text(stringResource(R.string.dtc_history)) }
        TextButton(onClick = onRecordings) { Text(stringResource(R.string.recordings_title)) }
        TextButton(onClick = onFreezeFrame) { Text(stringResource(R.string.freeze_frame)) }
        TextButton(onClick = onVehicleInfo) { Text(stringResource(R.string.vehicle_information)) }
        Text(stringResource(R.string.readiness_title))
        Button(enabled = !state.loading, onClick = viewModel::refresh) { Text(stringResource(R.string.read_evidence)) }
        Text(state.source)
        if (state.loading) CircularProgressIndicator()
        state.readings.forEach { reading ->
            Text("${com.sarani.carobd.ui.components.readingLabel(reading.state)} · ${reading.evidence.firstOrNull()?.responderId ?: "—"}")
            reading.detail?.let { com.sarani.carobd.ui.components.DeveloperEvidence(it) }
            reading.value?.let { value ->
                Text("MIL: " + stringResource(if (value.milActive) R.string.mil_on else R.string.mil_off) + " · DTC: ${value.dtcCount} · " + stringResource(if (value.ignitionType == com.sarani.carobd.core.model.IgnitionType.Spark) R.string.ui_ignition_spark else R.string.ui_ignition_compression))
                value.monitors.forEach { monitor ->
                    Text("${monitor.id}: " + stringResource(if (!monitor.isSupported) R.string.monitor_unsupported else if (monitor.isComplete) R.string.monitor_complete else R.string.monitor_incomplete))
                }
            }
            com.sarani.carobd.ui.components.DeveloperEvidence(reading.evidence.flatMap { it.rawLines }.joinToString("\n"))
        }
        Text(stringResource(R.string.evidence_limitations))
    }
}

@Composable
fun VehicleInformationScreen(viewModel: VehicleInformationViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.vehicle_information))
        Button(enabled = !state.loading, onClick = viewModel::refresh) { Text(stringResource(R.string.read_evidence)) }
        Text(state.source)
        if (state.loading) CircularProgressIndicator()
        state.readings.forEach { row ->
            val r = row.reading
            Text("09%02X · %s · %s".format(row.pid, com.sarani.carobd.ui.components.readingLabel(r.state), r.evidence.firstOrNull()?.responderId ?: "—"))
            r.value?.values?.forEach { com.sarani.carobd.ui.components.DiagnosticIdentifier(it) }
            r.detail?.let { com.sarani.carobd.ui.components.DeveloperEvidence(it) }
            com.sarani.carobd.ui.components.DeveloperEvidence(r.evidence.flatMap { it.rawLines }.joinToString("\n"))
        }
        Text(stringResource(R.string.evidence_limitations))
    }
}

@Composable
fun FreezeFrameScreen(viewModel: FreezeFrameViewModel) {
    val state by viewModel.state.collectAsState()
    var frame by remember { mutableStateOf("0") }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.freeze_frame))
        OutlinedTextField(frame, { if (it.length <= 3 && it.all(Char::isDigit)) frame = it }, label = { Text(stringResource(R.string.frame_index)) })
        Button(enabled = !state.loading && (frame.toIntOrNull()?.let { it in 0..255 } == true), onClick = { viewModel.refresh(frame.toInt()) }) { Text(stringResource(R.string.read_evidence)) }
        Text("${state.source} · ${state.frame}")
        if (state.loading) CircularProgressIndicator()
        state.readings.forEach { r ->
            Text("${com.sarani.carobd.ui.components.readingLabel(r.state)} · ${r.evidence.firstOrNull()?.responderId ?: "—"}")
            r.value?.let { v ->
                val converted = v.value?.let { com.sarani.carobd.ui.theme.displayMeasurement(it, v.unit.orEmpty(), com.sarani.carobd.ui.theme.LocalUnitSystem.current) }
                Text(stringResource(R.string.s5_frame_line, v.pid, v.frameIndex, converted?.value ?: "—", converted?.unit.orEmpty(), v.rawHex))
            }
            r.detail?.let { com.sarani.carobd.ui.components.DeveloperEvidence(it) }
            com.sarani.carobd.ui.components.DeveloperEvidence(r.evidence.flatMap { it.rawLines }.joinToString("\n"))
        }
        Text(stringResource(R.string.frame_limitations))
    }
}

@Composable
fun DtcHistoryScreen(viewModel: DtcHistoryViewModel) {
    val resolver = androidx.compose.ui.platform.LocalContext.current.contentResolver
    var pendingReportId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val exportStatus by viewModel.exportStatus.collectAsState()
    val exporter = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val id = pendingReportId
        pendingReportId = null
        if (uri != null && id != null) viewModel.export(id) { json ->
            requireNotNull(resolver.openOutputStream(uri, "wt")).bufferedWriter(Charsets.UTF_8).use { it.write(json) }
        }
    }
    val reports by viewModel.reports.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val error by viewModel.error.collectAsState()
    val warning by viewModel.warning.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dtc_history))
        TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.record_refresh)) }
        error?.let { Text(stringResource(R.string.s5_operation_failed)); com.sarani.carobd.ui.components.DeveloperEvidence(it) }; warning?.let { com.sarani.carobd.ui.components.DeveloperEvidence(it) }
        Text(stringResource(R.string.diagnosis_recent), style = MaterialTheme.typography.titleMedium)
        if (reports.isEmpty()) Text(stringResource(R.string.diagnosis_empty))
        when (exportStatus) {
            ReportExportStatus.BUSY -> LinearProgressIndicator(Modifier.fillMaxWidth())
            ReportExportStatus.SAVED -> Text(stringResource(R.string.diagnosis_export_saved))
            ReportExportStatus.FAILED -> Text(stringResource(R.string.s5_operation_failed))
            else -> Unit
        }
        reports.forEach { report -> DiagnosticReportCard(report, exportStatus == ReportExportStatus.BUSY) {
            viewModel.resetExportStatus()
            pendingReportId = report.id
            exporter.launch("carobd-dtc-${report.startedAtMs}.json")
        } }
        Text(stringResource(R.string.diagnosis_legacy_history), style = MaterialTheme.typography.titleMedium)
        sessions.forEach { s ->
            Text("${s.observedAtMs} · ${s.protocol ?: "—"} · ${s.connectionSessionId ?: "—"}")
            Text("MIL: " + stringResource(when (s.mil) { true -> R.string.mil_on; false -> R.string.mil_off; null -> R.string.mil_unknown }))
            if (s.legacy) Text(stringResource(R.string.history_legacy))
            if (s.codes.isEmpty() && !s.legacy) Text(stringResource(R.string.dtc_no_codes))
            s.codes.forEach { c -> Text("${c.code} · ${com.sarani.carobd.ui.components.categoryLabel(c.category)} · ${c.responder ?: "—"}") }
        }
    }
}

@Composable
fun MonitorTestsScreen(viewModel: MonitorTestsViewModel) {
    val state by viewModel.state.collectAsState()
    var mid by remember { mutableStateOf("01") }
    val number = mid.toIntOrNull(16)
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.monitor_tests))
        Text(stringResource(R.string.monitor_tests_limits))
        OutlinedTextField(mid, { if (it.length <= 2 && it.all { c -> c in "0123456789abcdefABCDEF" }) mid = it.uppercase() }, label = { Text(stringResource(R.string.s5_mid)) })
        Button(enabled = !state.loading && number != null && number in 1..255 && number % 32 != 0, onClick = { number?.let(viewModel::refresh) }) { Text(stringResource(R.string.read_evidence)) }
        Text(state.source)
        if (state.loading) CircularProgressIndicator()
        state.readings.forEach { r ->
            Text(com.sarani.carobd.ui.components.readingLabel(r.state) + " · " + (r.evidence.firstOrNull()?.responderId ?: "—"))
            r.value?.forEach { record -> Text("MID %02X · TID %02X · UASID %02X\nRAW %04X / %04X / %04X".format(record.monitorId, record.testId, record.unitScalingId, record.rawMeasured, record.rawMinimum, record.rawMaximum)) }
            r.detail?.let { com.sarani.carobd.ui.components.DeveloperEvidence(it) }
            com.sarani.carobd.ui.components.DeveloperEvidence(r.evidence.flatMap { it.rawLines }.joinToString("\n"))
        }
    }
}
