package com.sarani.carobd.ui.screens.trip

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.logger.TripLogger
import com.sarani.carobd.core.logger.TripSummary
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.launch

data class TripState(
    val summary: TripSummary = TripSummary(),
    val exportedCsvSnippet: String? = null
) : UiState

sealed interface TripIntent : UiIntent {
    data object ToggleRecording : TripIntent
    data object ExportCsv : TripIntent
}

sealed interface TripEffect : UiEffect {
    data class ShowToast(val message: String) : TripEffect
}

class TripViewModel(
    private val tripLogger: TripLogger,
    private val observeTelemetryUseCase: ObserveTelemetryUseCase
) : MviViewModel<TripState, TripIntent, TripEffect>(TripState()) {

    init {
        viewModelScope.launch {
            tripLogger.tripState.collect { sum ->
                setState { copy(summary = sum) }
            }
        }
        viewModelScope.launch {
            observeTelemetryUseCase.telemetry.collect { tele ->
                tripLogger.logSample(tele)
            }
        }
    }

    override fun onIntent(intent: TripIntent) {
        when (intent) {
            is TripIntent.ToggleRecording -> {
                if (uiState.value.summary.isRecording) {
                    tripLogger.stopTrip()
                } else {
                    tripLogger.startTrip()
                }
            }
            is TripIntent.ExportCsv -> {
                val csv = tripLogger.exportToCsv()
                setState { copy(exportedCsvSnippet = csv.take(500)) }
            }
        }
    }
}
