package com.sarani.carobd.ui.screens.maintenance

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.database.IranianCarSpecsDatabase
import com.sarani.carobd.core.model.DueCalculationResult
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceActionType
import com.sarani.carobd.core.model.MaintenanceCategory
import com.sarani.carobd.core.model.MaintenanceDueEngine
import com.sarani.carobd.core.model.MaintenanceIntervalProvenance
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.ServiceType
import com.sarani.carobd.core.model.VehicleMaintenanceRecord
import com.sarani.carobd.core.model.VehicleSpecSheet
import com.sarani.carobd.core.vehicle.catalog.VehicleCatalogRepository
import com.sarani.carobd.data.repository.MaintenanceRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.domain.reminder.MaintenanceReminderScheduler
import com.sarani.carobd.domain.reminder.SnoozeDuration
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.UUID

enum class MaintenanceTab {
    SCHEDULES,
    HISTORY,
    SPECS
}

data class RecordServiceDialogModel(
    val scheduleId: String,
    val itemKey: String,
    val title: String,
    val performedKm: String,
    val performedDateMs: Long = System.currentTimeMillis(),
    val costTomans: String = "",
    val serviceCenter: String = "",
    val partsBrand: String = "",
    val partNumber: String = "",
    val notes: String = "",
    val errorMessage: String? = null
)

data class EditScheduleDialogModel(
    val scheduleId: String? = null,
    val itemKey: String = "",
    val titleEn: String = "",
    val titleFa: String = "",
    val actionType: MaintenanceActionType = MaintenanceActionType.REPLACE,
    val intervalKm: String = "",
    val intervalMonths: String = "",
    val lastServiceKm: String = "",
    val lastServiceDateMs: Long? = null,
    val isCustom: Boolean = true,
    val errorMessage: String? = null
)

data class MaintenanceState(
    val vehicles: List<OwnedVehicle> = emptyList(),
    val selectedVehicle: OwnedVehicle? = null,
    val schedules: List<MaintenanceSchedule> = emptyList(),
    val records: List<VehicleMaintenanceRecord> = emptyList(),
    val specSheet: VehicleSpecSheet? = null,
    val selectedTab: MaintenanceTab = MaintenanceTab.SCHEDULES,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val okCount: Int = 0,
    val unknownCount: Int = 0,
    val recordServiceDialog: RecordServiceDialogModel? = null,
    val editScheduleDialog: EditScheduleDialogModel? = null,
    val deleteScheduleTarget: MaintenanceSchedule? = null,
    val deleteRecordTarget: VehicleMaintenanceRecord? = null,
    val showVehicleSelector: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed interface MaintenanceIntent : UiIntent {
    data class SelectVehicle(val vehicleId: String) : MaintenanceIntent
    data class SelectTab(val tab: MaintenanceTab) : MaintenanceIntent
    data object ToggleVehicleSelector : MaintenanceIntent
    data object DismissVehicleSelector : MaintenanceIntent
    data object InitializeDefaultSchedules : MaintenanceIntent

    data class OpenRecordServiceDialog(val schedule: MaintenanceSchedule) : MaintenanceIntent
    data object DismissRecordServiceDialog : MaintenanceIntent
    data class UpdateRecordServiceDialog(val model: RecordServiceDialogModel) : MaintenanceIntent
    data class SubmitRecordService(val model: RecordServiceDialogModel) : MaintenanceIntent

    data object OpenAddScheduleDialog : MaintenanceIntent
    data class OpenEditScheduleDialog(val schedule: MaintenanceSchedule) : MaintenanceIntent
    data object DismissEditScheduleDialog : MaintenanceIntent
    data class UpdateEditScheduleDialog(val model: EditScheduleDialogModel) : MaintenanceIntent
    data class SubmitSaveSchedule(val model: EditScheduleDialogModel) : MaintenanceIntent

    data class RequestDeleteSchedule(val schedule: MaintenanceSchedule) : MaintenanceIntent
    data object DismissDeleteSchedule : MaintenanceIntent
    data class ConfirmDeleteSchedule(val scheduleId: String) : MaintenanceIntent

    data class RequestDeleteRecord(val record: VehicleMaintenanceRecord) : MaintenanceIntent
    data object DismissDeleteRecord : MaintenanceIntent
    data class ConfirmDeleteRecord(val recordId: String) : MaintenanceIntent

    data class SnoozeSchedule(val scheduleId: String, val duration: SnoozeDuration) : MaintenanceIntent
}

sealed interface MaintenanceEffect : UiEffect {
    data class ShowMessage(val messageResId: Int) : MaintenanceEffect
    data class ShowError(val message: String) : MaintenanceEffect
}

class MaintenanceViewModel(
    private val maintenanceRepository: MaintenanceRepository,
    private val ownedVehicleRepository: OwnedVehicleRepository,
    private val catalogRepository: VehicleCatalogRepository? = null,
    private val reminderScheduler: MaintenanceReminderScheduler? = null
) : MviViewModel<MaintenanceState, MaintenanceIntent, MaintenanceEffect>(MaintenanceState()) {

    init {
        viewModelScope.launch {
            combine(
                ownedVehicleRepository.vehicles,
                ownedVehicleRepository.activeVehicleId
            ) { vehicles, activeId ->
                val selected = vehicles.firstOrNull { it.id == activeId } ?: vehicles.firstOrNull()
                vehicles to selected
            }.collectLatest { (vehicles, selected) ->
                loadDataForVehicle(vehicles, selected)
            }
        }
    }

    private suspend fun loadDataForVehicle(vehicles: List<OwnedVehicle>, selected: OwnedVehicle?) {
        if (selected == null) {
            setState {
                copy(
                    vehicles = vehicles,
                    selectedVehicle = null,
                    schedules = emptyList(),
                    records = emptyList(),
                    specSheet = null,
                    overdueCount = 0,
                    dueSoonCount = 0,
                    okCount = 0,
                    unknownCount = 0,
                    isLoading = false
                )
            }
            return
        }

        val odo = selected.currentOdometerKm
        val schedules = maintenanceRepository.getSchedules(selected.id, odo)
        val records = maintenanceRepository.getRecords(selected.id)

        // Resolve spec sheet from catalog or fallback database
        val specSheet = resolveSpecSheet(selected)

        var overdue = 0
        var dueSoon = 0
        var ok = 0
        var unknown = 0

        for (s in schedules) {
            when (s.dueState) {
                DueState.OVERDUE, DueState.DUE -> overdue++
                DueState.DUE_SOON -> dueSoon++
                DueState.OK -> ok++
                DueState.UNKNOWN -> unknown++
            }
        }

        setState {
            copy(
                vehicles = vehicles,
                selectedVehicle = selected,
                schedules = schedules,
                records = records,
                specSheet = specSheet,
                overdueCount = overdue,
                dueSoonCount = dueSoon,
                okCount = ok,
                unknownCount = unknown,
                isLoading = false
            )
        }

        if (reminderScheduler != null) {
            for (s in schedules) {
                if (s.dueState == DueState.DUE_SOON || s.dueState == DueState.DUE || s.dueState == DueState.OVERDUE) {
                    val dueResult = MaintenanceDueEngine.calculateDueState(
                        currentKm = selected.currentOdometerKm,
                        currentDateMs = System.currentTimeMillis(),
                        lastKm = s.lastServiceKm,
                        lastDateMs = s.lastServiceDateMs,
                        intervalKm = s.intervalKm,
                        intervalMonths = s.intervalMonths
                    )
                    reminderScheduler.scheduleReminder(selected, s, dueResult)
                }
            }
        }
    }

    private fun resolveSpecSheet(vehicle: OwnedVehicle): VehicleSpecSheet? {
        if (catalogRepository != null) {
            val matches = catalogRepository.findMatchingVariants(
                makeQuery = vehicle.manufacturer.orEmpty(),
                modelQuery = vehicle.model.orEmpty(),
                modelYear = vehicle.year,
                engineQuery = vehicle.engine ?: vehicle.engineCode
            )
            val match = matches.firstOrNull()
            val specs = match?.variant?.specifications
            val engine = match?.engine
            if (specs != null && engine != null) {
                return VehicleSpecSheet(
                    vehicleId = vehicle.id,
                    engineCode = engine.code,
                    engineOilGrade = specs.engineOilGrade ?: "—",
                    engineOilCapacityLiters = specs.engineOilCapacityLiters ?: 0f,
                    recommendedOilBrands = emptyList(),
                    gearboxOilGrade = specs.gearboxOilGrade ?: "—",
                    gearboxCapacityLiters = specs.gearboxCapacityLiters ?: 0f,
                    sparkPlugModel = specs.sparkPlugModel ?: "—",
                    sparkPlugGapMm = specs.sparkPlugGapMm ?: 0f,
                    timingBeltIntervalKm = specs.timingBeltIntervalKm ?: 60000,
                    timingBeltTeeth = specs.timingBeltTeeth ?: 0,
                    brakeFluidType = specs.brakeFluidType ?: "—",
                    coolantCapacityLiters = specs.coolantCapacityLiters ?: 0f,
                    maintenanceTipsFa = emptyList(),
                    sources = engine.sources,
                    confidence = match.confidence
                )
            }
        }

        // Fallback to IranianCarSpecsDatabase if name/model matches
        val query = "${vehicle.manufacturer.orEmpty()} ${vehicle.model.orEmpty()} ${vehicle.displayName}".trim()
        val matchId = when {
            query.contains("206", ignoreCase = true) -> "ikco_206_tu5"
            query.contains("207", ignoreCase = true) -> "ikco_207_tu5"
            query.contains("دنا", ignoreCase = true) || query.contains("dena", ignoreCase = true) -> "ikco_dena_plus_turbo"
            query.contains("تارا", ignoreCase = true) || query.contains("tara", ignoreCase = true) -> "ikco_tara"
            query.contains("شاهین", ignoreCase = true) || query.contains("shahin", ignoreCase = true) -> "saipa_shahin"
            query.contains("کوئیک", ignoreCase = true) || query.contains("quick", ignoreCase = true) -> "saipa_quick"
            query.contains("ساینا", ignoreCase = true) || query.contains("saina", ignoreCase = true) -> "saipa_saina"
            query.contains("ال 90", ignoreCase = true) || query.contains("l90", ignoreCase = true) || query.contains("تندر", ignoreCase = true) -> "renault_tondar90"
            else -> null
        }
        return matchId?.let { IranianCarSpecsDatabase.getSpecSheet(it) }
    }

    override fun onIntent(intent: MaintenanceIntent) {
        when (intent) {
            is MaintenanceIntent.SelectVehicle -> viewModelScope.launch {
                ownedVehicleRepository.select(intent.vehicleId)
                setState { copy(showVehicleSelector = false) }
            }
            is MaintenanceIntent.SelectTab -> setState { copy(selectedTab = intent.tab) }
            is MaintenanceIntent.ToggleVehicleSelector -> setState { copy(showVehicleSelector = !showVehicleSelector) }
            is MaintenanceIntent.DismissVehicleSelector -> setState { copy(showVehicleSelector = false) }

            is MaintenanceIntent.InitializeDefaultSchedules -> viewModelScope.launch {
                val vehicle = uiState.value.selectedVehicle ?: return@launch
                val specSheet = uiState.value.specSheet
                val odo = vehicle.currentOdometerKm

                val defaultTypes = listOf(
                    ServiceType.ENGINE_OIL,
                    ServiceType.OIL_FILTER,
                    ServiceType.AIR_FILTER,
                    ServiceType.CABIN_FILTER,
                    ServiceType.SPARK_PLUGS,
                    ServiceType.TIMING_BELT,
                    ServiceType.BRAKE_PADS,
                    ServiceType.GEARBOX_OIL,
                    ServiceType.COOLANT,
                    ServiceType.BRAKE_FLUID
                )

                for (type in defaultTypes) {
                    val intervalKm = if (type == ServiceType.TIMING_BELT && specSheet?.timingBeltIntervalKm != null && specSheet.timingBeltIntervalKm > 0) {
                        specSheet.timingBeltIntervalKm
                    } else {
                        type.defaultIntervalKm
                    }

                    val schedule = MaintenanceSchedule(
                        id = UUID.randomUUID().toString(),
                        vehicleId = vehicle.id,
                        itemKey = type.name.lowercase(),
                        titleEn = type.titleEn,
                        titleFa = type.titleFa,
                        actionType = MaintenanceActionType.REPLACE,
                        intervalKm = intervalKm,
                        intervalMonths = type.defaultIntervalMonths,
                        lastServiceKm = null,
                        lastServiceDateMs = null,
                        provenance = MaintenanceIntervalProvenance.OEM_RECOMMENDED,
                        dueState = DueState.UNKNOWN,
                        isCustom = false
                    )
                    maintenanceRepository.saveSchedule(schedule)
                }

                loadDataForVehicle(uiState.value.vehicles, vehicle)
            }

            is MaintenanceIntent.OpenRecordServiceDialog -> {
                val currentOdo = uiState.value.selectedVehicle?.currentOdometerKm?.toString() ?: ""
                setState {
                    copy(
                        recordServiceDialog = RecordServiceDialogModel(
                            scheduleId = intent.schedule.id,
                            itemKey = intent.schedule.itemKey,
                            title = intent.schedule.titleFa.ifBlank { intent.schedule.titleEn },
                            performedKm = currentOdo
                        )
                    )
                }
            }

            is MaintenanceIntent.DismissRecordServiceDialog -> setState { copy(recordServiceDialog = null) }

            is MaintenanceIntent.UpdateRecordServiceDialog -> setState { copy(recordServiceDialog = intent.model) }

            is MaintenanceIntent.SubmitRecordService -> viewModelScope.launch {
                val model = intent.model
                val km = model.performedKm.toLongOrNull()
                if (km == null || km < 0L) {
                    setState {
                        copy(recordServiceDialog = model.copy(errorMessage = "Invalid mileage"))
                    }
                    return@launch
                }

                val cost = model.costTomans.toLongOrNull() ?: 0L

                val result = maintenanceRepository.recordCompletedService(
                    scheduleId = model.scheduleId,
                    performedKm = km,
                    performedDateMs = model.performedDateMs,
                    costTomans = cost,
                    serviceCenter = model.serviceCenter.trim().ifEmpty { null },
                    partsBrand = model.partsBrand.trim().ifEmpty { null },
                    partNumber = model.partNumber.trim().ifEmpty { null },
                    notes = model.notes.trim().ifEmpty { null }
                )

                if (result.isSuccess) {
                    // Update vehicle mileage if performedKm is greater than current known
                    val currentVehicle = uiState.value.selectedVehicle
                    val currentKm = currentVehicle?.currentOdometerKm
                    if (currentVehicle != null && (currentKm == null || km > currentKm)) {
                        ownedVehicleRepository.save(currentVehicle.copy(currentOdometerKm = km))
                    }

                    setState { copy(recordServiceDialog = null) }
                    loadDataForVehicle(uiState.value.vehicles, currentVehicle)
                } else {
                    setState {
                        copy(recordServiceDialog = model.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to record service"))
                    }
                }
            }

            is MaintenanceIntent.OpenAddScheduleDialog -> {
                setState {
                    copy(
                        editScheduleDialog = EditScheduleDialogModel(
                            scheduleId = null,
                            itemKey = "custom_${System.currentTimeMillis()}",
                            isCustom = true
                        )
                    )
                }
            }

            is MaintenanceIntent.OpenEditScheduleDialog -> {
                val s = intent.schedule
                setState {
                    copy(
                        editScheduleDialog = EditScheduleDialogModel(
                            scheduleId = s.id,
                            itemKey = s.itemKey,
                            titleEn = s.titleEn,
                            titleFa = s.titleFa,
                            actionType = s.actionType,
                            intervalKm = s.intervalKm?.toString() ?: "",
                            intervalMonths = s.intervalMonths?.toString() ?: "",
                            lastServiceKm = s.lastServiceKm?.toString() ?: "",
                            lastServiceDateMs = s.lastServiceDateMs,
                            isCustom = s.isCustom
                        )
                    )
                }
            }

            is MaintenanceIntent.DismissEditScheduleDialog -> setState { copy(editScheduleDialog = null) }

            is MaintenanceIntent.UpdateEditScheduleDialog -> setState { copy(editScheduleDialog = intent.model) }

            is MaintenanceIntent.SubmitSaveSchedule -> viewModelScope.launch {
                val vehicle = uiState.value.selectedVehicle ?: return@launch
                val model = intent.model

                val titleFa = model.titleFa.trim()
                val titleEn = model.titleEn.trim().ifEmpty { titleFa }
                if (titleFa.isEmpty()) {
                    setState {
                        copy(editScheduleDialog = model.copy(errorMessage = "Title is required"))
                    }
                    return@launch
                }

                val intervalKm = model.intervalKm.trim().toIntOrNull()
                val intervalMonths = model.intervalMonths.trim().toIntOrNull()
                val lastServiceKm = model.lastServiceKm.trim().toLongOrNull()

                val schedule = MaintenanceSchedule(
                    id = model.scheduleId ?: UUID.randomUUID().toString(),
                    vehicleId = vehicle.id,
                    itemKey = model.itemKey.ifEmpty { "custom_${System.currentTimeMillis()}" },
                    titleEn = titleEn,
                    titleFa = titleFa,
                    actionType = model.actionType,
                    intervalKm = intervalKm,
                    intervalMonths = intervalMonths,
                    lastServiceKm = lastServiceKm,
                    lastServiceDateMs = model.lastServiceDateMs,
                    provenance = if (model.isCustom) MaintenanceIntervalProvenance.USER_CUSTOM else MaintenanceIntervalProvenance.OEM_RECOMMENDED,
                    isCustom = model.isCustom,
                    updatedAtMs = System.currentTimeMillis()
                )

                maintenanceRepository.saveSchedule(schedule)
                if (reminderScheduler != null) {
                    val dueResult = MaintenanceDueEngine.calculateDueState(
                        currentKm = vehicle.currentOdometerKm,
                        currentDateMs = System.currentTimeMillis(),
                        lastKm = lastServiceKm,
                        lastDateMs = model.lastServiceDateMs,
                        intervalKm = intervalKm,
                        intervalMonths = intervalMonths
                    )
                    reminderScheduler.scheduleReminder(vehicle, schedule, dueResult)
                }
                setState { copy(editScheduleDialog = null) }
                loadDataForVehicle(uiState.value.vehicles, vehicle)
            }

            is MaintenanceIntent.RequestDeleteSchedule -> setState { copy(deleteScheduleTarget = intent.schedule) }
            is MaintenanceIntent.DismissDeleteSchedule -> setState { copy(deleteScheduleTarget = null) }
            is MaintenanceIntent.ConfirmDeleteSchedule -> viewModelScope.launch {
                val vehicle = uiState.value.selectedVehicle
                if (vehicle != null) {
                    reminderScheduler?.cancelScheduleReminder(vehicle.id, intent.scheduleId)
                }
                maintenanceRepository.deleteSchedule(intent.scheduleId)
                setState { copy(deleteScheduleTarget = null) }
                loadDataForVehicle(uiState.value.vehicles, uiState.value.selectedVehicle)
            }

            is MaintenanceIntent.RequestDeleteRecord -> setState { copy(deleteRecordTarget = intent.record) }
            is MaintenanceIntent.DismissDeleteRecord -> setState { copy(deleteRecordTarget = null) }
            is MaintenanceIntent.ConfirmDeleteRecord -> viewModelScope.launch {
                maintenanceRepository.deleteRecord(intent.recordId)
                setState { copy(deleteRecordTarget = null) }
                loadDataForVehicle(uiState.value.vehicles, uiState.value.selectedVehicle)
            }

            is MaintenanceIntent.SnoozeSchedule -> viewModelScope.launch {
                val vehicle = uiState.value.selectedVehicle ?: return@launch
                reminderScheduler?.snoozeReminder(vehicle.id, intent.scheduleId, intent.duration)
                loadDataForVehicle(uiState.value.vehicles, vehicle)
            }
        }
    }
}
