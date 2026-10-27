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
    val scanCompletedSuccessfully: Boolean = false
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

    private fun performScan() {
        viewModelScope.launch {
            setState { copy(isScanning = true, lastScanMessage = null, scanCompletedSuccessfully = false) }
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
                            lastScanMessage = "خطا در اسکن: ${err.message}",
                            scanCompletedSuccessfully = false
                        )
                    }
                }
            )
        }
    }

    private fun performClear() {
        viewModelScope.launch {
            setState { copy(showConfirmDialog = false, isClearing = true, scanCompletedSuccessfully = false) }
            val result = clearDtcUseCase()
            result.fold(
                onSuccess = {
                    setState {
                        copy(
                            isClearing = false,
                            troubleCodes = emptyList(),
                            lastScanMessage = "درخواست پاک‌سازی کدهای خطا توسط ECU تأیید شد (0x44)",
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
        }
    }
}
