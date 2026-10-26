package com.sarani.carobd.ui.screens.dashboard

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.calculator.FuelEconomyEngine
import com.sarani.carobd.core.calculator.FuelEconomyMetrics
import com.sarani.carobd.core.connectivity.DiscoveredBluetoothDevice
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DashboardWidgetConfig
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.core.storage.UserVehicleSetup
import com.sarani.carobd.domain.usecase.ConnectUseCase
import com.sarani.carobd.domain.usecase.DisconnectUseCase
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.launch

data class DashboardState(
    val telemetry: LiveTelemetry = LiveTelemetry(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val activeProfile: VehicleProfile = IranianEcuRegistry.PRESET_VEHICLES.first(),
    val isHudMode: Boolean = false,
    val isChartMode: Boolean = false,
    val selectedConnectionType: ConnectionType = ConnectionType.VIRTUAL_SIMULATOR,
    val showBluetoothDialog: Boolean = false,
    val showSensorPicker: Boolean = false,
    val showOnboarding: Boolean = false,
    val discoveredDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val isScanningBt: Boolean = false,
    val widgets: List<DashboardWidgetConfig> = emptyList(),
    val fuelEconomy: FuelEconomyMetrics = FuelEconomyMetrics(),
    val userVehicleSetup: UserVehicleSetup = UserVehicleSetup(),
    // Real-time historical buffers for live graphs
    val rpmHistory: List<Float> = emptyList(),
    val speedHistory: List<Float> = emptyList(),
    val o2History: List<Float> = emptyList()
) : UiState

sealed interface DashboardIntent : UiIntent {
    data object ToggleConnection : DashboardIntent
    data class SetConnectionType(val type: ConnectionType) : DashboardIntent
    data object ToggleHudMode : DashboardIntent
    data object ToggleChartMode : DashboardIntent
    data object OpenBluetoothDialog : DashboardIntent
    data object CloseBluetoothDialog : DashboardIntent
    data object OpenSensorPicker : DashboardIntent
    data object CloseSensorPicker : DashboardIntent
    data class AddWidget(val widget: DashboardWidgetConfig) : DashboardIntent
    data class RemoveWidget(val widgetId: String) : DashboardIntent
    data class ConnectToDevice(val device: DiscoveredBluetoothDevice) : DashboardIntent
    data object TriggerBtScan : DashboardIntent
    data class SaveOnboarding(val setup: UserVehicleSetup) : DashboardIntent
}

sealed interface DashboardEffect : UiEffect {
    data class ShowMessage(val text: String) : DashboardEffect
}

class DashboardViewModel(
    private val observeTelemetryUseCase: ObserveTelemetryUseCase,
    private val connectUseCase: ConnectUseCase,
    private val disconnectUseCase: DisconnectUseCase,
    private val appPreferencesManager: AppPreferencesManager,
    private val bluetoothScanner: com.sarani.carobd.core.connectivity.BluetoothDeviceScanner
) : MviViewModel<DashboardState, DashboardIntent, DashboardEffect>(
    DashboardState(widgets = appPreferencesManager.getDefaultWidgets())
) {

    init {
        // Observe preferences & onboarding state
        viewModelScope.launch {
            appPreferencesManager.isOnboardingDone.collect { done ->
                setState { copy(showOnboarding = !done) }
            }
        }
        viewModelScope.launch {
            appPreferencesManager.userVehicleSetup.collect { setup ->
                setState { copy(userVehicleSetup = setup) }
            }
        }
        viewModelScope.launch {
            observeTelemetryUseCase.telemetry.collect { tele ->
                val maxBuffer = 35
                val newRpmHistory = (uiState.value.rpmHistory + tele.rpm.toFloat()).takeLast(maxBuffer)
                val newSpeedHistory = (uiState.value.speedHistory + tele.speedKmh.toFloat()).takeLast(maxBuffer)
                val newO2History = (uiState.value.o2History + tele.oxygenSensor1Volt).takeLast(maxBuffer)

                val fuelEco = FuelEconomyEngine.calculate(
                    telemetry = tele,
                    tankCapacityLiters = uiState.value.userVehicleSetup.tankCapacityLiters,
                    fuelPricePerLiterTomans = uiState.value.userVehicleSetup.fuelPriceTomans
                )

                setState {
                    copy(
                        telemetry = tele,
                        fuelEconomy = fuelEco,
                        rpmHistory = newRpmHistory,
                        speedHistory = newSpeedHistory,
                        o2History = newO2History
                    )
                }
            }
        }
        viewModelScope.launch {
            observeTelemetryUseCase.connectionState.collect { conn ->
                setState { copy(connectionState = conn) }
            }
        }
        viewModelScope.launch {
            observeTelemetryUseCase.activeProfile.collect { prof ->
                setState { copy(activeProfile = prof) }
            }
        }
    }

    override fun onIntent(intent: DashboardIntent) {
        when (intent) {
            is DashboardIntent.ToggleConnection -> {
                viewModelScope.launch {
                    if (uiState.value.connectionState is ConnectionState.Connected) {
                        disconnectUseCase()
                    } else {
                        connectUseCase(uiState.value.selectedConnectionType, "")
                    }
                }
            }
            is DashboardIntent.SetConnectionType -> {
                setState { copy(selectedConnectionType = intent.type) }
                if (intent.type == ConnectionType.BLUETOOTH_CLASSIC || intent.type == ConnectionType.BLUETOOTH_LE) {
                    onIntent(DashboardIntent.OpenBluetoothDialog)
                }
            }
            is DashboardIntent.ToggleHudMode -> {
                setState { copy(isHudMode = !isHudMode) }
            }
            is DashboardIntent.ToggleChartMode -> {
                setState { copy(isChartMode = !isChartMode) }
            }
            is DashboardIntent.OpenSensorPicker -> {
                setState { copy(showSensorPicker = true) }
            }
            is DashboardIntent.CloseSensorPicker -> {
                setState { copy(showSensorPicker = false) }
            }
            is DashboardIntent.AddWidget -> {
                setState { copy(widgets = widgets + intent.widget, showSensorPicker = false) }
            }
            is DashboardIntent.RemoveWidget -> {
                setState { copy(widgets = widgets.filter { it.id != intent.widgetId }) }
            }
            is DashboardIntent.OpenBluetoothDialog -> {
                val paired = bluetoothScanner.getPairedDevices()
                setState {
                    copy(
                        showBluetoothDialog = true,
                        discoveredDevices = paired
                    )
                }
            }
            is DashboardIntent.CloseBluetoothDialog -> {
                setState { copy(showBluetoothDialog = false) }
            }
            is DashboardIntent.ConnectToDevice -> {
                viewModelScope.launch {
                    setState { copy(showBluetoothDialog = false) }
                    connectUseCase(ConnectionType.BLUETOOTH_CLASSIC, intent.device.address)
                }
            }
            is DashboardIntent.TriggerBtScan -> {
                setState { copy(isScanningBt = true) }
                viewModelScope.launch {
                    try {
                        bluetoothScanner.scanNearbyDevices().collect { devices ->
                            setState { copy(discoveredDevices = devices) }
                        }
                    } catch (e: Exception) {
                        setEffect { DashboardEffect.ShowMessage("خطا در اسکن بلوتوث: ${e.message}") }
                    } finally {
                        setState { copy(isScanningBt = false) }
                    }
                }
            }
            is DashboardIntent.SaveOnboarding -> {
                viewModelScope.launch {
                    appPreferencesManager.saveVehicleSetup(intent.setup)
                    setState { copy(showOnboarding = false, userVehicleSetup = intent.setup) }
                }
            }
        }
    }
}
