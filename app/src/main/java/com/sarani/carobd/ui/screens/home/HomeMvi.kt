package com.sarani.carobd.ui.screens.home

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.vehicle.catalog.VehicleCatalogRepository
import com.sarani.carobd.core.vehicle.parts.PartsRepository
import com.sarani.carobd.data.repository.MaintenanceRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.domain.repository.ObdRepository
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HomeState(
    val activeVehicle: OwnedVehicle? = null,
    val vehicles: List<OwnedVehicle> = emptyList(),
    val totalSchedulesCount: Int = 0,
    val overdueCount: Int = 0,
    val dueSoonCount: Int = 0,
    val nextServiceTitle: String? = null,
    val nextServiceRemainingKm: Long? = null,
    val compatiblePartsCount: Int = 0,
    val partsConfidence: ConfidenceLevel = ConfidenceLevel.UNKNOWN,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val showVehicleSelector: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed interface HomeIntent : UiIntent {
    data object Refresh : HomeIntent
    data class SelectVehicle(val vehicleId: String) : HomeIntent
    data object ToggleVehicleSelector : HomeIntent
    data object DismissVehicleSelector : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data class NavigateTo(val route: String) : HomeEffect
}

class HomeViewModel(
    private val ownedVehicleRepository: OwnedVehicleRepository,
    private val maintenanceRepository: MaintenanceRepository,
    private val partsRepository: PartsRepository,
    private val catalogRepository: VehicleCatalogRepository? = null,
    private val obdRepository: ObdRepository
) : MviViewModel<HomeState, HomeIntent, HomeEffect>(HomeState()) {

    init {
        viewModelScope.launch {
            obdRepository.connectionState.collectLatest { conn ->
                setState { copy(connectionState = conn) }
            }
        }

        viewModelScope.launch {
            combine(
                ownedVehicleRepository.vehicles,
                ownedVehicleRepository.activeVehicleId
            ) { vehicles, activeId ->
                val selected = vehicles.firstOrNull { it.id == activeId } ?: vehicles.firstOrNull()
                vehicles to selected
            }.collectLatest { (vehicles, selected) ->
                handleVehicleChanged(vehicles, selected)
            }
        }
    }

    override fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.Refresh -> viewModelScope.launch {
                val vehicles = ownedVehicleRepository.vehicles.value
                val activeId = ownedVehicleRepository.activeVehicleId.value
                val selected = vehicles.firstOrNull { it.id == activeId } ?: vehicles.firstOrNull()
                handleVehicleChanged(vehicles, selected)
            }
            is HomeIntent.SelectVehicle -> viewModelScope.launch {
                ownedVehicleRepository.select(intent.vehicleId)
                setState { copy(showVehicleSelector = false) }
            }
            is HomeIntent.ToggleVehicleSelector -> setState {
                copy(showVehicleSelector = !showVehicleSelector)
            }
            is HomeIntent.DismissVehicleSelector -> setState {
                copy(showVehicleSelector = false)
            }
        }
    }

    private suspend fun handleVehicleChanged(vehicles: List<OwnedVehicle>, selected: OwnedVehicle?) {
        if (selected == null) {
            setState {
                copy(
                    vehicles = vehicles,
                    activeVehicle = null,
                    totalSchedulesCount = 0,
                    overdueCount = 0,
                    dueSoonCount = 0,
                    nextServiceTitle = null,
                    nextServiceRemainingKm = null,
                    compatiblePartsCount = 0,
                    partsConfidence = ConfidenceLevel.UNKNOWN
                )
            }
            return
        }

        // 1. Maintenance Status Calculation
        val currentOdo = selected.currentOdometerKm ?: 0L
        val schedules = maintenanceRepository.getSchedules(selected.id, currentOdo)
        val overdue = schedules.count { it.dueState == DueState.OVERDUE }
        val dueSoon = schedules.count { it.dueState == DueState.DUE_SOON || it.dueState == DueState.DUE }

        val upcoming = schedules
            .filter { (it.nextDueKm ?: 0L) > currentOdo }
            .minByOrNull { (it.nextDueKm ?: Long.MAX_VALUE) - currentOdo }

        val nextTitle = upcoming?.let { it.titleFa.ifBlank { it.titleEn } }
        val remainingKm = upcoming?.nextDueKm?.minus(currentOdo)

        // 2. Parts Compatibility Calculation
        val make = selected.manufacturer
        val model = selected.model
        val match = if (catalogRepository != null && !make.isNullOrBlank() && !model.isNullOrBlank()) {
            catalogRepository.findMatchingVariants(
                makeQuery = make,
                modelQuery = model,
                modelYear = selected.year,
                engineQuery = selected.engineCode
            ).firstOrNull()
        } else null

        val parts = partsRepository.findCompatibleParts(
            vehicleMakeId = match?.make?.id ?: (make?.lowercase()?.trim() ?: ""),
            vehicleModelId = match?.model?.id ?: (model?.lowercase()?.trim() ?: ""),
            generationId = match?.generation?.id,
            engineId = match?.engine?.id,
            engineCode = match?.engine?.code ?: selected.engineCode,
            engineFamily = match?.engine?.family,
            modelYear = selected.year
        )

        val partsConfidence = if (parts.isNotEmpty()) {
            if (parts.any { it.isOemVerified }) ConfidenceLevel.VERIFIED_OEM else ConfidenceLevel.COMMUNITY_CONFIRMED
        } else {
            ConfidenceLevel.UNKNOWN
        }

        setState {
            copy(
                vehicles = vehicles,
                activeVehicle = selected,
                totalSchedulesCount = schedules.size,
                overdueCount = overdue,
                dueSoonCount = dueSoon,
                nextServiceTitle = nextTitle,
                nextServiceRemainingKm = remainingKm,
                compatiblePartsCount = parts.size,
                partsConfidence = partsConfidence
            )
        }
    }
}
