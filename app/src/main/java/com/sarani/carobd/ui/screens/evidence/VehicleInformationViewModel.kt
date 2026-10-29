package com.sarani.carobd.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class VehicleInformationUiState(val loading: Boolean = false, val source: String = "—", val readings: List<VehicleInfoReading> = emptyList())
class VehicleInformationViewModel(private val read: VehicleInformationUseCase) : ViewModel() {
    private val _state = MutableStateFlow(VehicleInformationUiState())
    val state = _state.asStateFlow()
    private var work: Job? = null
    fun refresh() {
        if (work?.isActive == true) return
        work = viewModelScope.launch {
            val source = read.sourceLabel
            _state.value = VehicleInformationUiState(loading = true, source = source)
            try { _state.value = VehicleInformationUiState(readings = read(), source = source) }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
}
