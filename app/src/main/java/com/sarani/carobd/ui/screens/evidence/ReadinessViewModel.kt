package com.sarani.carobd.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.usecase.ReadinessUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ReadinessUiState(val loading: Boolean = false, val readings: List<DiagnosticReading<ReadinessStatus>> = emptyList())
class ReadinessViewModel(private val read: ReadinessUseCase) : ViewModel() {
    private val _state = MutableStateFlow(ReadinessUiState())
    val state = _state.asStateFlow()
    private var work: Job? = null
    fun refresh() {
        if (work?.isActive == true) return
        work = viewModelScope.launch {
            _state.value = ReadinessUiState(loading = true)
            try { _state.value = ReadinessUiState(readings = read()) }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
}
