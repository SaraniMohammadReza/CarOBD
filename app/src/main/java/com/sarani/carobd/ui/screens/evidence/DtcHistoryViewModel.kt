package com.sarani.carobd.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DtcHistoryViewModel(private val repository: ObdRepository) : ViewModel() {
    val warning = repository.historyWarning
    private val _sessions = MutableStateFlow<List<DtcHistorySession>>(emptyList())
    val sessions = _sessions.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun refresh() { viewModelScope.launch {
        repository.diagnosticHistory().onSuccess { _sessions.value = it; _error.value = null }.onFailure { _error.value = it.message }
    } }
    init { refresh() }
}
