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
    val selectedProfile: VehicleProfile? = null,
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
            val conn = repository.connectionState.value
            if (conn !is com.sarani.carobd.core.model.ConnectionState.Connected) {
                setState {
                    copy(
                        isAutoScanning = false,
                        autoScanStatusMessage = "اتصال به دیاگ برقرار نیست. ابتدا آداپتور OBD-II را متصل نمایید.",
                        detectedProtocol = null
                    )
                }
                setEffect { VehicleProfileEffect.ShowToast("ابتدا آداپتور OBD-II را متصل نمایید.") }
                return@launch
            }

            setState {
                copy(
                    isAutoScanning = true,
                    autoScanStatusMessage = "در حال ارسال فرمان شناسایی پروتکل به آداپتور...",
                    detectedProtocol = null
                )
            }

            // Real probe via AT DPN (Describe Protocol by Number)
            val dpnResult = repository.sendRawCommand("AT DPN")
            val rawDpn = dpnResult.getOrNull()?.replace(">", "")?.trim() ?: ""
            val protocolNum = rawDpn.removePrefix("A").trim()

            val matchedStandard = when (protocolNum) {
                "6" -> com.sarani.carobd.core.model.ProtocolStandard.ISO_15765_4_CAN_11_500
                "7" -> com.sarani.carobd.core.model.ProtocolStandard.ISO_15765_4_CAN_29_500
                "5" -> com.sarani.carobd.core.model.ProtocolStandard.ISO_14230_4_KWP_FAST
                "4" -> com.sarani.carobd.core.model.ProtocolStandard.ISO_14230_4_KWP_5BAUD
                "3" -> com.sarani.carobd.core.model.ProtocolStandard.ISO_9141_2
                "1" -> com.sarani.carobd.core.model.ProtocolStandard.SAE_J1850_PWM
                "2" -> com.sarani.carobd.core.model.ProtocolStandard.SAE_J1850_VPW
                else -> null
            }

            val matchedProtocol = if (matchedStandard != null) {
                IranianEcuRegistry.ALL_PROTOCOLS.find { it.standard == matchedStandard }
            } else null

            if (matchedProtocol != null) {
                setState {
                    copy(
                        isAutoScanning = false,
                        detectedProtocol = matchedProtocol,
                        autoScanStatusMessage = "پروتکل سخت‌افزاری فعال ایسیو: ${matchedProtocol.nameFa} (${matchedProtocol.standard.displayName})"
                    )
                }
            } else {
                setState {
                    copy(
                        isAutoScanning = false,
                        detectedProtocol = null,
                        autoScanStatusMessage = if (rawDpn.isNotBlank()) "پاسخ آداپتور: $rawDpn (پروتکل استاندارد قفل نشده)" else "ایسیو پاسخ نداد (UNABLE TO CONNECT)"
                    )
                }
            }
        }
    }
}
