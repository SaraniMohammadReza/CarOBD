package com.sarani.carobd.ui.screens.recording

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sarani.carobd.R

@Composable
fun RecordingScreen(viewModel: RecordingViewModel) {
    val state by viewModel.state.collectAsState()
    val recording by viewModel.recording.collectAsState()
    val resolver = LocalContext.current.contentResolver
    val json = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.export(false) { text -> requireNotNull(resolver.openOutputStream(it, "wt")).bufferedWriter().use { out -> out.write(text) } } }
    }
    val csv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.export(true) { text -> requireNotNull(resolver.openOutputStream(it, "wt")).bufferedWriter().use { out -> out.write(text) } } }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importJson {
            requireNotNull(resolver.openInputStream(it)).use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) { val n = input.read(buffer); if (n < 0) break; require(out.size() + n <= 16 * 1024 * 1024) { "Transcript too large" }; out.write(buffer, 0, n) }
                out.toString("UTF-8")
            }
        } }
    }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.recordings_title))
        Text(com.sarani.carobd.ui.components.recordingLabel(recording))
        Text(stringResource(R.string.recording_limitations))
        Row {
            TextButton(enabled = !state.busy && recording != "Recording", onClick = viewModel::start) { Text(stringResource(R.string.record_start)) }
            TextButton(enabled = !state.busy, onClick = viewModel::stop) { Text(stringResource(R.string.record_stop)) }
            TextButton(enabled = !state.busy, onClick = viewModel::refresh) { Text(stringResource(R.string.record_refresh)) }
        }
        TextButton(enabled = !state.busy, onClick = { importer.launch(arrayOf("application/json", "text/plain")) }) { Text(stringResource(R.string.record_import)) }
        if (state.busy) TextButton(onClick = viewModel::cancel) { Text(stringResource(R.string.cancel)) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.sessions.forEach { session -> TextButton(enabled = !state.busy, onClick = { viewModel.select(session.sessionId) }) { Text("${session.profileName} · ${session.transportType} · ${session.startedAtMs}") } }
        state.selected?.let { t ->
            Text("${t.metadata.sessionId}\n${t.metadata.adapterIdentity ?: "—"}\n${t.metadata.vehicleProtocol ?: "—"}\n${t.events.size}")
            Row {
                TextButton(enabled = !state.busy, onClick = { json.launch("${t.metadata.sessionId}.json") }) { Text("JSON") }
                TextButton(enabled = !state.busy, onClick = { csv.launch("${t.metadata.sessionId}.csv") }) { Text("CSV") }
                TextButton(enabled = !state.busy, onClick = viewModel::replay) { Text(stringResource(R.string.record_replay)) }
                TextButton(enabled = !state.busy, onClick = { viewModel.delete(t.metadata.sessionId) }) { Text(stringResource(R.string.record_delete)) }
            }
            t.events.take(100).forEach { Text("${it.elapsedMs} · ${it.direction} · ${it.command ?: "—"} · ${it.rawHex}") }
        }
        state.replay.forEach { result ->
            Text("REPLAY · ${result.command} · ${result.detail ?: ""}")
            result.evidence.forEach { Text("${it.responderId ?: "—"} · ${it.result}\n${it.rawLines.joinToString("\n")}") }
        }
    }
}
