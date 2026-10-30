package com.sarani.carobd.ui.screens.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.adapter.transport.*
import com.sarani.carobd.core.model.*
import com.sarani.carobd.data.recording.RecordingController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RecordingUiState(val sessions: List<TranscriptMetadata> = emptyList(), val selected: DiagnosticTranscript? = null,
    val replay: List<ReplayedCommand> = emptyList(), val busy: Boolean = false, val error: String? = null)
class RecordingViewModel(private val controller: RecordingController) : ViewModel() {
    val recording = controller.state
    val benchEnabled = controller.benchEnabled
    fun enableBench(enabled: Boolean) = controller.enableBench(enabled)
    private val _state = MutableStateFlow(RecordingUiState())
    val state = _state.asStateFlow()
    private var work: Job? = null
    init { refresh() }
    private fun operation(block: suspend () -> Unit) {
        if (work?.isActive == true) return
        work = viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(error = e.message) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun start() = operation { controller.start() }
    fun stop() = operation { controller.stop(); refreshList() }
    fun refresh() = operation { refreshList() }
    private suspend fun refreshList() { val list = withContext(Dispatchers.IO) { controller.store.list() }; _state.update { it.copy(sessions = list) } }
    fun select(id: String) = operation { val t = withContext(Dispatchers.IO) { controller.store.read(id) }; _state.update { it.copy(selected = t, replay = emptyList()) } }
    fun delete(id: String) = operation { check(controller.builder?.toTranscript()?.metadata?.sessionId != id) { "Stop recording before deleting this session" }; withContext(Dispatchers.IO) { check(controller.store.delete(id)) }; _state.update { it.copy(selected = null, replay = emptyList()) }; refreshList() }
    fun replay() = operation {
        val transcript = requireNotNull(_state.value.selected)
        val results = withContext(Dispatchers.Default) { TranscriptReplayRunner().run(transcript) }
        _state.update { it.copy(replay = results) }
    }
    fun cancel() { work?.cancel() }
    fun importJson(read: () -> String) = operation {
        withContext(Dispatchers.IO) {
            val t = DiagnosticTranscript.fromJson(read())
            // Imported data receives a new local ID; it cannot overwrite a capture.
            controller.store.save(t.copy(metadata = t.metadata.copy(sessionId = java.util.UUID.randomUUID().toString())))
        }
        refreshList()
    }
    fun export(csv: Boolean, write: (String) -> Unit) = operation {
        val t = requireNotNull(_state.value.selected)
        withContext(Dispatchers.IO) { write(if (csv) t.toCsv() else t.toJson()) }
    }
}
