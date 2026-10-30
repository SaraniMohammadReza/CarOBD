package com.sarani.carobd.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.usecase.MonitorTestsUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class MonitorTestsUiState(val loading: Boolean = false, val source: String = "—", val readings: List<DiagnosticReading<List<MonitorTestResult>>> = emptyList())
class MonitorTestsViewModel(private val read: MonitorTestsUseCase) : ViewModel() {
    private val _state = MutableStateFlow(MonitorTestsUiState())
    val state = _state.asStateFlow()
    private var work: Job? = null
    fun refresh(mid: Int) {
        if (mid !in 1..255 || mid % 32 == 0 || work?.isActive == true) return
        work = viewModelScope.launch {
            val source = read.sourceLabel
            _state.value = MonitorTestsUiState(true, source)
            try { _state.value = MonitorTestsUiState(source = source, readings = read(mid)) }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
}
