package com.sarani.carobd.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.usecase.FreezeFrameUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class FreezeFrameUiState(val loading: Boolean = false, val frame: Int = 0, val source: String = "—", val readings: List<DiagnosticReading<FreezeFrameValue>> = emptyList())
class FreezeFrameViewModel(private val read: FreezeFrameUseCase) : ViewModel() {
    private val _state = MutableStateFlow(FreezeFrameUiState())
    val state = _state.asStateFlow()
    private var work: Job? = null
    fun refresh(frame: Int) {
        if (frame !in 0..255 || work?.isActive == true) return
        work = viewModelScope.launch {
            val source = read.sourceLabel
            _state.value = FreezeFrameUiState(loading = true, frame = frame, source = source)
            try { _state.value = FreezeFrameUiState(frame = frame, source = source, readings = read(frame)) }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
}
