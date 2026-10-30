package com.sarani.carobd.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

enum class ReportExportStatus { NONE, BUSY, SAVED, FAILED }

class DtcHistoryViewModel(private val repository: ObdRepository, private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO) : ViewModel() {
    private var exportJob: Job? = null
    private val _exportStatus = MutableStateFlow(ReportExportStatus.NONE)
    val exportStatus = _exportStatus.asStateFlow()
    fun resetExportStatus() { if (exportJob?.isActive != true) _exportStatus.value = ReportExportStatus.NONE }
    fun export(reportId: String, write: (String) -> Unit) {
        if (exportJob?.isActive == true) return
        _exportStatus.value = ReportExportStatus.BUSY
        exportJob = viewModelScope.launch {
            try {
                val report = requireNotNull(_reports.value.firstOrNull { it.id == reportId }) { "Selected report is no longer available; refresh and select it again" }
                withContext(ioDispatcher) { write(DiagnosticReportExport.toJson(report)) }
                _exportStatus.value = ReportExportStatus.SAVED
            } catch (e: CancellationException) { _exportStatus.value = ReportExportStatus.NONE; throw e }
            catch (e: Exception) { _error.value = e.message; _exportStatus.value = ReportExportStatus.FAILED }
        }
    }
    private val _reports = MutableStateFlow<List<DiagnosticSessionReport>>(emptyList())
    val reports = _reports.asStateFlow()
    val warning = repository.historyWarning
    private val _sessions = MutableStateFlow<List<DtcHistorySession>>(emptyList())
    val sessions = _sessions.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    fun refresh() { viewModelScope.launch {
        repository.recentDiagnosticReports().onSuccess { _reports.value = it }.onFailure { _error.value = it.message; return@launch }
        repository.diagnosticHistory().onSuccess { _sessions.value = it; _error.value = null }.onFailure { _error.value = it.message }
    } }
    init { refresh() }
}
