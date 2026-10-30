package com.sarani.carobd.ui.screens.maintenance

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.database.IranianCarSpecsDatabase
import com.sarani.carobd.core.model.ServiceReminderItem
import com.sarani.carobd.core.model.ServiceType
import com.sarani.carobd.core.model.UNKNOWN_LAST_CHANGED
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.model.VehicleSpecSheet
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class MaintenanceState(
    val activeProfile: VehicleProfile? = null,
    val specSheet: VehicleSpecSheet? = null,
    val currentOdometerKm: Int? = null,
    val reminders: List<ServiceReminderItem> = emptyList()
) : UiState

sealed interface MaintenanceIntent : UiIntent {
    data class ResetService(val serviceId: String) : MaintenanceIntent
    data class UpdateOdometer(val km: Int) : MaintenanceIntent
}

sealed interface MaintenanceEffect : UiEffect

class MaintenanceViewModel(
    private val observeTelemetryUseCase: ObserveTelemetryUseCase,
    private val appPreferencesManager: AppPreferencesManager
) : MviViewModel<MaintenanceState, MaintenanceIntent, MaintenanceEffect>(MaintenanceState()) {

    init {
        // Single persisted source: active vehicle profile + user odometer + per-service last changed.
        // Rebuilds reminders whenever any of these changes so selecting a different vehicle
        // (e.g. Dena) immediately repopulates the correct odometer/intervals (§B3 single source).
        viewModelScope.launch {
            combine(
                observeTelemetryUseCase.activeProfile,
                appPreferencesManager.userVehicleSetup,
                appPreferencesManager.serviceLastChangedKm
            ) { profile, setup, lastChanged ->
                val specs = profile?.let { IranianCarSpecsDatabase.getSpecSheet(it.id) }
                MaintenanceState(
                    activeProfile = profile,
                    specSheet = specs,
                    currentOdometerKm = setup.odometerKm,
                    reminders = setup.odometerKm?.let { buildReminders(it, specs, lastChanged) } ?: emptyList()
                )
            }.collect { newState -> setState { newState } }
        }
    }

    private fun buildReminders(
        currentKm: Int,
        specSheet: VehicleSpecSheet?,
        lastChanged: Map<String, Int>
    ): List<ServiceReminderItem> = listOf(
        item("oil", ServiceType.ENGINE_OIL, ServiceType.ENGINE_OIL.defaultIntervalKm, currentKm, "", lastChanged),
        item("filter", ServiceType.OIL_FILTER, ServiceType.OIL_FILTER.defaultIntervalKm, currentKm, "", lastChanged),
        item("timing_belt", ServiceType.TIMING_BELT, specSheet?.timingBeltIntervalKm ?: ServiceType.TIMING_BELT.defaultIntervalKm, currentKm, "", lastChanged),
        item("plugs", ServiceType.SPARK_PLUGS, ServiceType.SPARK_PLUGS.defaultIntervalKm, currentKm, "", lastChanged),
        item("brake_pads", ServiceType.BRAKE_PADS, ServiceType.BRAKE_PADS.defaultIntervalKm, currentKm, "", lastChanged),
        item("gearbox", ServiceType.GEARBOX_OIL, ServiceType.GEARBOX_OIL.defaultIntervalKm, currentKm, "", lastChanged)
    )

    private fun item(
        id: String,
        type: ServiceType,
        intervalKm: Int,
        currentKm: Int,
        notes: String,
        lastChanged: Map<String, Int>
    ) = ServiceReminderItem(
        id = id,
        type = type,
        lastChangedKm = lastChanged[id] ?: UNKNOWN_LAST_CHANGED,
        intervalKm = intervalKm,
        currentKm = currentKm,
        notes = notes
    )

    override fun onIntent(intent: MaintenanceIntent) {
        when (intent) {
            is MaintenanceIntent.ResetService -> viewModelScope.launch {
                // Persist last-service so it survives restart (§18 reminder-state persistence).
                uiState.value.currentOdometerKm?.let { appPreferencesManager.setServiceLastChangedKm(intent.serviceId, it) }
            }
            is MaintenanceIntent.UpdateOdometer -> viewModelScope.launch {
                appPreferencesManager.setOdometerKm(intent.km)
            }
        }
    }
}
