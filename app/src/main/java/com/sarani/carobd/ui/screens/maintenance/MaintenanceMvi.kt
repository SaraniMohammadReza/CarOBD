package com.sarani.carobd.ui.screens.maintenance

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.database.IranianCarSpecsDatabase
import com.sarani.carobd.core.model.ServiceReminderItem
import com.sarani.carobd.core.model.ServiceType
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.model.VehicleSpecSheet
import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import com.sarani.carobd.domain.usecase.ObserveTelemetryUseCase
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.launch

data class MaintenanceState(
    val activeProfile: VehicleProfile? = null,
    val specSheet: VehicleSpecSheet? = null,
    val currentOdometerKm: Int = 115000,
    val reminders: List<ServiceReminderItem> = emptyList()
) : UiState

sealed interface MaintenanceIntent : UiIntent {
    data class ResetService(val serviceId: String) : MaintenanceIntent
    data class UpdateOdometer(val km: Int) : MaintenanceIntent
}

sealed interface MaintenanceEffect : UiEffect

class MaintenanceViewModel(
    private val observeTelemetryUseCase: ObserveTelemetryUseCase
) : MviViewModel<MaintenanceState, MaintenanceIntent, MaintenanceEffect>(
    MaintenanceState(
        reminders = listOf(
            ServiceReminderItem(
                id = "oil",
                type = ServiceType.ENGINE_OIL,
                lastChangedKm = 111500,
                intervalKm = 6000,
                currentKm = 115000,
                notes = "روغن ۱۰W-40 نیمه سنتتیک"
            ),
            ServiceReminderItem(
                id = "filter",
                type = ServiceType.OIL_FILTER,
                lastChangedKm = 111500,
                intervalKm = 6000,
                currentKm = 115000
            ),
            ServiceReminderItem(
                id = "timing_belt",
                type = ServiceType.TIMING_BELT,
                lastChangedKm = 70000,
                intervalKm = 60000,
                currentKm = 115000,
                notes = "تسمه تایم گیتس پاور گریپ"
            ),
            ServiceReminderItem(
                id = "plugs",
                type = ServiceType.SPARK_PLUGS,
                lastChangedKm = 95000,
                intervalKm = 30000,
                currentKm = 115000,
                notes = "شمع بوش دو پلاتین"
            ),
            ServiceReminderItem(
                id = "brake_pads",
                type = ServiceType.BRAKE_PADS,
                lastChangedKm = 90000,
                intervalKm = 35000,
                currentKm = 115000,
                notes = "لنت تکستار اصلی"
            ),
            ServiceReminderItem(
                id = "gearbox",
                type = ServiceType.GEARBOX_OIL,
                lastChangedKm = 85000,
                intervalKm = 40000,
                currentKm = 115000,
                notes = "واسکازین ۷۵W-۸۰"
            )
        )
    )
) {

    init {
        viewModelScope.launch {
            observeTelemetryUseCase.activeProfile.collect { profile ->
                val specs = profile?.let { IranianCarSpecsDatabase.getSpecSheet(it.id) }
                setState { copy(activeProfile = profile, specSheet = specs) }
            }
        }
    }

    override fun onIntent(intent: MaintenanceIntent) {
        when (intent) {
            is MaintenanceIntent.ResetService -> {
                val updated = uiState.value.reminders.map { item ->
                    if (item.id == intent.serviceId) {
                        item.copy(lastChangedKm = uiState.value.currentOdometerKm)
                    } else item
                }
                setState { copy(reminders = updated) }
            }
            is MaintenanceIntent.UpdateOdometer -> {
                val updated = uiState.value.reminders.map { item ->
                    item.copy(currentKm = intent.km)
                }
                setState { copy(currentOdometerKm = intent.km, reminders = updated) }
            }
        }
    }
}
