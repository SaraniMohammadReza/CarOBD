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
fun EvidenceScreen(viewModel: ReadinessViewModel) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.readiness_title))
        Button(enabled = !state.loading, onClick = viewModel::refresh) { Text(stringResource(R.string.read_evidence)) }
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
