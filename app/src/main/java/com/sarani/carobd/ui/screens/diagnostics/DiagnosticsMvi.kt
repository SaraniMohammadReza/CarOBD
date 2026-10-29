package com.sarani.carobd.ui.screens.diagnostics

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.domain.usecase.ClearDtcUseCase
import com.sarani.carobd.domain.usecase.ScanDtcUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.launch

data class DiagnosticsState(
    val troubleCodes: List<DiagnosticCode> = emptyList(),
    val isScanning: Boolean = false,
    val isClearing: Boolean = false,
    val showConfirmDialog: Boolean = false,
    val lastScanMessage: String? = null,
    val scanCompletedSuccessfully: Boolean = false,
    val responderEvidence: List<String> = emptyList()
) : UiState

sealed interface DiagnosticsIntent : UiIntent {
    data object ScanCodes : DiagnosticsIntent
    data object RequestClearCodes : DiagnosticsIntent
    data object ConfirmClearCodes : DiagnosticsIntent
    data object DismissConfirmDialog : DiagnosticsIntent
}

sealed interface DiagnosticsEffect : UiEffect {
    data class ShowMessage(val text: String) : DiagnosticsEffect
}

class DiagnosticsViewModel(
    private val scanDtcUseCase: ScanDtcUseCase,
    private val clearDtcUseCase: ClearDtcUseCase
) : MviViewModel<DiagnosticsState, DiagnosticsIntent, DiagnosticsEffect>(DiagnosticsState()) {

    override fun onIntent(intent: DiagnosticsIntent) {
        when (intent) {
            is DiagnosticsIntent.ScanCodes -> performScan()
            is DiagnosticsIntent.RequestClearCodes -> setState { copy(showConfirmDialog = true) }
            is DiagnosticsIntent.DismissConfirmDialog -> setState { copy(showConfirmDialog = false) }
            is DiagnosticsIntent.ConfirmClearCodes -> performClear()
        }
    }

    private var scanJob: kotlinx.coroutines.Job? = null
    private var clearJob: kotlinx.coroutines.Job? = null

    private fun performScan() {
        if (scanJob?.isActive == true || clearJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            setState { copy(isScanning = true, troubleCodes = emptyList(), lastScanMessage = null, responderEvidence = emptyList(), scanCompletedSuccessfully = false) }
            try {
                val result = scanDtcUseCase()
                result.fold(
                    onSuccess = { codes ->
                        setState {
                            copy(
                                isScanning = false,
                                troubleCodes = codes,
                                lastScanMessage = if (codes.isEmpty()) "کد خطایی یافت نشد" else "${codes.size} کد خطا شناسایی شد",
                                scanCompletedSuccessfully = true
                            )
                        }
                    },
                    onFailure = { err ->
                        setState {
                            copy(
                                isScanning = false,
                                lastScanMessage = "Scan incomplete: ${err.message}",
                                troubleCodes = (err as? com.sarani.carobd.core.model.DiagnosticScanException)?.report?.codes ?: emptyList(),
                                responderEvidence = (err as? com.sarani.carobd.core.model.DiagnosticScanException)?.report?.responders?.map {
                                    "${it.request} / ${it.responder ?: "Unknown ECU"}: ${it.outcome}"
                                } ?: emptyList(),
                                scanCompletedSuccessfully = false
                            )
                        }
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                setState { copy(lastScanMessage = "Scan canceled; current session has no complete scan", scanCompletedSuccessfully = false) }
                throw e
            } finally { setState { copy(isScanning = false) } }
        }
    }

    private fun performClear() {
        if (scanJob?.isActive == true || clearJob?.isActive == true) return
        clearJob = viewModelScope.launch {
            setState { copy(showConfirmDialog = false, isClearing = true, scanCompletedSuccessfully = false) }
            try {
                val result = clearDtcUseCase()
                result.fold(
                    onSuccess = {
                        setState {
                            copy(
                                isClearing = false,
                                troubleCodes = emptyList(),
                                lastScanMessage = "پاک‌سازی محلی شبیه‌ساز تأیید شد (0x44)",
                                scanCompletedSuccessfully = false
                            )
                        }
                    },
                    onFailure = { err ->
                        setState {
                            copy(
                                isClearing = false,
                                lastScanMessage = "خطا در پاک کردن خطاها: ${err.message}",
                                scanCompletedSuccessfully = false
                            )
                        }
                    }
                )
            } finally { setState { copy(isClearing = false) } }
        }
    }
}
