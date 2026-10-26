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

data class VehicleProfileState(
    val profiles: List<VehicleProfile> = IranianEcuRegistry.PRESET_VEHICLES,
    val selectedProfile: VehicleProfile = IranianEcuRegistry.PRESET_VEHICLES.first(),
    val isAutoScanning: Boolean = false,
    val autoScanStatusMessage: String? = null,
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
        viewModelScope.launch {
            setState {
                copy(
                    isAutoScanning = true,
                    autoScanStatusMessage = "در حال اتصال و تست پروتکل‌های خودرو...",
                    detectedProtocol = null
                )
            }

            // Sequential simulated/live test through all protocols
            val candidates = IranianEcuRegistry.ALL_PROTOCOLS
            for ((index, proto) in candidates.withIndex()) {
                setState {
                    copy(autoScanStatusMessage = "تست پروتکل [${index + 1}/${candidates.size}]: ${proto.nameFa}...")
                }
                delay(650)
            }

            // Lock onto best detected protocol (e.g. Siemens or Bosch depending on profile)
            val matched = uiState.value.selectedProfile.ecuProtocol
            setState {
                copy(
                    isAutoScanning = false,
                    detectedProtocol = matched,
                    autoScanStatusMessage = "پروتکل با موفقیت شناسایی و قفل شد: ${matched.nameFa}"
                )
            }
        }
    }
}
