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
fun EvidenceScreen(viewModel: ReadinessViewModel, onVehicleInfo: () -> Unit, onFreezeFrame: () -> Unit, onRecordings: () -> Unit, onHistory: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onHistory) { Text(stringResource(R.string.dtc_history)) }
        TextButton(onClick = onRecordings) { Text(stringResource(R.string.recordings_title)) }
        TextButton(onClick = onFreezeFrame) { Text(stringResource(R.string.freeze_frame)) }
        TextButton(onClick = onVehicleInfo) { Text(stringResource(R.string.vehicle_information)) }
        Text(stringResource(R.string.readiness_title))
        Button(enabled = !state.loading, onClick = viewModel::refresh) { Text(stringResource(R.string.read_evidence)) }
        Text(state.source)
        if (state.loading) CircularProgressIndicator()
        state.readings.forEach { reading ->
            Text("${reading.state} · ${reading.evidence.firstOrNull()?.responderId ?: "—"}")
            reading.detail?.let { Text(it) }
            reading.value?.let { value ->
                Text("MIL: ${value.milActive} · DTC: ${value.dtcCount} · ${value.ignitionType}")
                value.monitors.forEach { monitor ->
                    Text("${monitor.id}: " + stringResource(if (!monitor.isSupported) R.string.monitor_unsupported else if (monitor.isComplete) R.string.monitor_complete else R.string.monitor_incomplete))
                }
            }
            Text(reading.evidence.flatMap { it.rawLines }.joinToString("\n"))
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
            Text("09%02X · %s · %s".format(row.pid, r.state, r.evidence.firstOrNull()?.responderId ?: "—"))
            r.value?.values?.forEach { Text(it) }
            r.detail?.let { Text(it) }
            Text(r.evidence.flatMap { it.rawLines }.joinToString("\n"))
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
            Text("${r.state} · ${r.evidence.firstOrNull()?.responderId ?: "—"}")
            r.value?.let { v ->
                val converted = v.value?.let { com.sarani.carobd.ui.theme.displayMeasurement(it, v.unit.orEmpty(), com.sarani.carobd.ui.theme.LocalUnitSystem.current) }
                Text("PID %02X · frame %02X · %s %s · %s".format(v.pid, v.frameIndex, converted?.value ?: "—", converted?.unit.orEmpty(), v.rawHex))
            }
            r.detail?.let { Text(it) }
            Text(r.evidence.flatMap { it.rawLines }.joinToString("\n"))
        }
        Text(stringResource(R.string.frame_limitations))
    }
}

@Composable
fun DtcHistoryScreen(viewModel: DtcHistoryViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val error by viewModel.error.collectAsState()
    val warning by viewModel.warning.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.dtc_history))
        TextButton(onClick = viewModel::refresh) { Text(stringResource(R.string.record_refresh)) }
        error?.let { Text(it) }; warning?.let { Text(it) }
        sessions.forEach { s ->
            Text("${s.observedAtMs} · ${s.protocol ?: "—"} · ${s.connectionSessionId ?: "—"}")
            Text("MIL: " + stringResource(when (s.mil) { true -> R.string.mil_on; false -> R.string.mil_off; null -> R.string.mil_unknown }))
            if (s.legacy) Text(stringResource(R.string.history_legacy))
            if (s.codes.isEmpty()) Text(stringResource(R.string.dtc_no_codes))
            s.codes.forEach { c -> Text("${c.code} · ${c.category ?: "—"} · ${c.responder ?: "—"}") }
        }
    }
}
