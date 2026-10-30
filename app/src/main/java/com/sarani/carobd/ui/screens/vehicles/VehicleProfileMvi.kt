package com.sarani.carobd.ui.screens.vehicles

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.EcuProtocol
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.protocol.AutoProtocolScanner
import com.sarani.carobd.core.protocol.ProtocolScanStep
import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import com.sarani.carobd.domain.repository.ObdRepository
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.domain.usecase.SetVehicleProfileUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ProtocolProbeStatus { DISCONNECTED, READING, OBSERVED, UNAVAILABLE }

data class VehicleProfileState(
    val profiles: List<VehicleProfile> = IranianEcuRegistry.PRESET_VEHICLES,
    val selectedProfile: VehicleProfile? = null,
    val isAutoScanning: Boolean = false,
    val autoScanStatusMessage: ProtocolProbeStatus? = null,
    val probeEvidence: String? = null,
    val detectedProtocol: EcuProtocol? = null
) : UiState

sealed interface VehicleProfileIntent : UiIntent {
    data class SelectProfile(val profile: VehicleProfile) : VehicleProfileIntent
    data object StartAutoProtocolScan : VehicleProfileIntent
}

sealed interface VehicleProfileEffect : UiEffect {
    data class ShowToast(val message: String) : VehicleProfileEffect
}

class VehicleProfileViewModel(
    private val observeTelemetryUseCase: ObserveTelemetryUseCase,
    private val setVehicleProfileUseCase: SetVehicleProfileUseCase,
    private val repository: ObdRepository
) : MviViewModel<VehicleProfileState, VehicleProfileIntent, VehicleProfileEffect>(VehicleProfileState()) {

    init {
        viewModelScope.launch {
            observeTelemetryUseCase.activeProfile.collect { current ->
                setState { copy(selectedProfile = current) }
            }
        }
    }

    override fun onIntent(intent: VehicleProfileIntent) {
        when (intent) {
            is VehicleProfileIntent.SelectProfile -> {
                viewModelScope.launch {
                    setVehicleProfileUseCase(intent.profile)
                    setState { copy(selectedProfile = intent.profile) }
                }
            }
            is VehicleProfileIntent.StartAutoProtocolScan -> {
                performAutoScan()
            }
        }
    }

    private fun performAutoScan() {
        if (uiState.value.isAutoScanning) return
        viewModelScope.launch {
            val token = repository.foregroundSessionId.value
            if (token == null) {
                setState { copy(autoScanStatusMessage = ProtocolProbeStatus.DISCONNECTED, probeEvidence = null) }
                return@launch
            }
            setState { copy(isAutoScanning = true, autoScanStatusMessage = ProtocolProbeStatus.READING, detectedProtocol = null, probeEvidence = null) }
            try {
                val result = repository.sendRawCommand("ATDPN")
                val stillCurrent = repository.foregroundSessionId.value == token
                setState { copy(autoScanStatusMessage = if (result.isSuccess && stillCurrent) ProtocolProbeStatus.OBSERVED else ProtocolProbeStatus.UNAVAILABLE,
                    probeEvidence = if (stillCurrent) result.getOrNull() ?: result.exceptionOrNull()?.message else "Session changed") }
            } finally { setState { copy(isAutoScanning = false) } }
        }
    }
}
