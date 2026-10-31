package com.sarani.carobd.ui.screens.maintenance

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.database.IranianCarSpecsDatabase
import com.sarani.carobd.core.model.ConfidenceLevel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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

data class EditRecordDialogModel(
    val recordId: String,
    val vehicleId: String,
    val itemKey: String,
    val performedKm: String,
    val performedDateMs: Long,
    val costTomans: String = "",
    val serviceCenter: String = "",
    val partsBrand: String = "",
    val partNumber: String = "",
    val notes: String = "",
    val provenance: MaintenanceIntervalProvenance = MaintenanceIntervalProvenance.USER_CUSTOM,
    val createdAtMs: Long = System.currentTimeMillis(),
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
    val editRecordDialog: EditRecordDialogModel? = null,
    val exportText: String? = null,
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

    data class OpenEditRecordDialog(val record: VehicleMaintenanceRecord) : MaintenanceIntent
    data object DismissEditRecordDialog : MaintenanceIntent
    data class UpdateEditRecordDialog(val model: EditRecordDialogModel) : MaintenanceIntent
    data class SubmitSaveRecord(val model: EditRecordDialogModel) : MaintenanceIntent

    data object ExportServiceHistory : MaintenanceIntent
    data object DismissExportDialog : MaintenanceIntent

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
                run {
                    val dueResult = MaintenanceDueEngine.calculateDueState(
                        currentKm = selected.currentOdometerKm,
                        currentDateMs = System.currentTimeMillis(),
                        lastKm = s.lastServiceKm,
                        lastDateMs = s.lastServiceDateMs,
                        intervalKm = s.intervalKm,
                        intervalMonths = s.intervalMonths
                    )
                    reminderScheduler?.scheduleReminder(selected, s, dueResult)
                }
            }
        }
    }

    private fun resolveSpecSheet(vehicle: OwnedVehicle): VehicleSpecSheet? {
        if (catalogRepository != null) {
            val match = catalogRepository.resolveOwnedVehicle(vehicle).exactMatch
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

        return null
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

                val match = catalogRepository?.resolveOwnedVehicle(vehicle)?.exactMatch
                if (match == null) {
                    setEffect { MaintenanceEffect.ShowMessage(com.sarani.carobd.R.string.vehicle_exact_match_required) }
                    return@launch
                }
                for (type in defaultTypes) {
                    // Only the exact field's source may authorize a numeric recommendation.
                    val field = if (type == ServiceType.TIMING_BELT) match.variant.specifications
                        ?.getFieldProvenance<Int>("timingBeltIntervalKm") else null
                    val authorized = field?.isSafeForAutomaticRecommendation == true &&
                        (field.isOemVerified() || (field.source != null &&
                            field.sourceType == com.sarani.carobd.core.model.ProvenanceSourceType.RELIABLE_WORKSHOP_SECONDARY))
                    val intervalKm = field?.value?.takeIf { authorized && it > 0 }
                    val provenance = when {
                        intervalKm == null -> MaintenanceIntervalProvenance.UNVERIFIED
                        field?.isOemVerified() == true -> MaintenanceIntervalProvenance.OEM_RECOMMENDED
                        else -> MaintenanceIntervalProvenance.VERIFIED_SECONDARY
                    }

                    val schedule = MaintenanceSchedule(
                        id = UUID.randomUUID().toString(),
                        vehicleId = vehicle.id,
                        itemKey = type.name.lowercase(),
                        titleEn = type.titleEn,
                        titleFa = type.titleFa,
                        actionType = MaintenanceActionType.REPLACE,
                        intervalKm = intervalKm,
                        intervalMonths = null,
                        lastServiceKm = null,
                        lastServiceDateMs = null,
                        provenance = provenance,
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
                    loadDataForVehicle(ownedVehicleRepository.vehicles.value, ownedVehicleRepository.vehicles.value.find { it.id == currentVehicle?.id })
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
                    provenance = MaintenanceIntervalProvenance.USER_CUSTOM,
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

            is MaintenanceIntent.OpenEditRecordDialog -> {
                val r = intent.record
                setState {
                    copy(
                        editRecordDialog = EditRecordDialogModel(
                            recordId = r.id,
                            vehicleId = r.vehicleId,
                            itemKey = r.itemKey,
                            performedKm = r.performedKm.toString(),
                            performedDateMs = r.performedDateMs,
                            costTomans = if (r.costTomans > 0) r.costTomans.toString() else "",
                            serviceCenter = r.serviceCenter.orEmpty(),
                            partsBrand = r.partsBrand.orEmpty(),
                            partNumber = r.partNumber.orEmpty(),
                            notes = r.notes.orEmpty(),
                            provenance = r.provenance,
                            createdAtMs = r.createdAtMs
                        )
                    )
                }
            }

            is MaintenanceIntent.DismissEditRecordDialog -> setState { copy(editRecordDialog = null) }

            is MaintenanceIntent.UpdateEditRecordDialog -> setState { copy(editRecordDialog = intent.model) }

            is MaintenanceIntent.SubmitSaveRecord -> viewModelScope.launch {
                val vehicle = uiState.value.selectedVehicle ?: return@launch
                val model = intent.model
                val km = model.performedKm.trim().toLongOrNull()
                if (km == null || km < 0L) {
                    setState {
                        copy(editRecordDialog = model.copy(errorMessage = "Invalid mileage"))
                    }
                    return@launch
                }
                val cost = model.costTomans.trim().toLongOrNull() ?: 0L

                if (model.vehicleId != vehicle.id) return@launch
                val updatedRecord = VehicleMaintenanceRecord(
                    scheduleId = uiState.value.records.firstOrNull { it.id == model.recordId }?.scheduleId,
                    id = model.recordId,
                    vehicleId = model.vehicleId,
                    itemKey = model.itemKey,
                    performedKm = km,
                    performedDateMs = model.performedDateMs,
                    costTomans = cost,
                    serviceCenter = model.serviceCenter.trim().ifEmpty { null },
                    partsBrand = model.partsBrand.trim().ifEmpty { null },
                    partNumber = model.partNumber.trim().ifEmpty { null },
                    notes = model.notes.trim().ifEmpty { null },
                    provenance = model.provenance,
                    createdAtMs = model.createdAtMs
                )

                val result = maintenanceRepository.saveRecord(updatedRecord)
                if (result.isSuccess) {
                    val currentKm = vehicle.currentOdometerKm
                    if (currentKm == null || km > currentKm) {
                        ownedVehicleRepository.save(vehicle.copy(currentOdometerKm = km))
                    }
                    setState { copy(editRecordDialog = null) }
                    loadDataForVehicle(ownedVehicleRepository.vehicles.value, ownedVehicleRepository.vehicles.value.find { it.id == vehicle.id })
                } else {
                    setState {
                        copy(editRecordDialog = model.copy(errorMessage = result.exceptionOrNull()?.message ?: "Failed to save record"))
                    }
                }
            }

            is MaintenanceIntent.ExportServiceHistory -> {
                val vehicle = uiState.value.selectedVehicle
                val records = uiState.value.records
                val exportText = buildExportSummary(vehicle, records)
                setState { copy(exportText = exportText) }
            }

            is MaintenanceIntent.DismissExportDialog -> setState { copy(exportText = null) }

            is MaintenanceIntent.SnoozeSchedule -> viewModelScope.launch {
                val vehicle = uiState.value.selectedVehicle ?: return@launch
                reminderScheduler?.snoozeReminder(vehicle.id, intent.scheduleId, intent.duration)
                loadDataForVehicle(uiState.value.vehicles, vehicle)
            }
        }
    }

    private fun buildExportSummary(vehicle: OwnedVehicle?, records: List<VehicleMaintenanceRecord>): String {
        val name = vehicle?.displayName ?: "Vehicle"
        val df = SimpleDateFormat("yyyy/MM/dd", Locale.US)
        return buildString {
            appendLine("=== $name — Service History ===")
            if (vehicle?.currentOdometerKm != null) {
                appendLine("Current Odometer: ${vehicle.currentOdometerKm} km")
            }
            appendLine("Export Date: ${df.format(Date())}")
            appendLine("Total Records: ${records.size}")
            appendLine()
            if (records.isEmpty()) {
                appendLine("No service history recorded.")
            } else {
                records.forEachIndexed { i, r ->
                    appendLine("[${i + 1}] ${r.itemKey.replace('_', ' ').uppercase()}")
                    appendLine("  Date: ${df.format(Date(r.performedDateMs))}")
                    appendLine("  Mileage: ${r.performedKm} km")
                    if (r.costTomans > 0) appendLine("  Cost: ${r.costTomans} Tomans")
                    if (!r.serviceCenter.isNullOrBlank()) appendLine("  Center: ${r.serviceCenter}")
                    if (!r.partsBrand.isNullOrBlank()) appendLine("  Parts: ${r.partsBrand} ${r.partNumber.orEmpty()}".trim())
                    if (!r.notes.isNullOrBlank()) appendLine("  Notes: ${r.notes}")
                    appendLine()
                }
            }
        }
    }
}
