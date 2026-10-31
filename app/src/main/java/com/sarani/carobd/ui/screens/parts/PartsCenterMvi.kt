package com.sarani.carobd.ui.screens.parts

import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.CompatiblePartItem
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.PartCategory
import com.sarani.carobd.core.model.ResolvedVehicleMatch
import com.sarani.carobd.core.vehicle.catalog.VehicleCatalogRepository
import com.sarani.carobd.core.vehicle.parts.PartsRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.ui.mvi.MviViewModel
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PartsCenterState(
    val vehicles: List<OwnedVehicle> = emptyList(),
    val activeVehicle: OwnedVehicle? = null,
    val resolvedMatch: ResolvedVehicleMatch? = null,
    val categories: List<PartCategory> = emptyList(),
    val selectedCategory: PartCategory? = null,
    val searchQuery: String = "",
    val compatibleParts: List<CompatiblePartItem> = emptyList(),
    val isLoading: Boolean = false,
    val toastMessage: String? = null
) : UiState

sealed interface PartsCenterIntent : UiIntent {
    data class SelectCategory(val category: PartCategory?) : PartsCenterIntent
    data class UpdateSearchQuery(val query: String) : PartsCenterIntent
    data class CopyToClipboard(val text: String) : PartsCenterIntent
    data object ClearToast : PartsCenterIntent
    data object Refresh : PartsCenterIntent
}

sealed interface PartsCenterEffect : UiEffect {
    data class ShowToast(val message: String) : PartsCenterEffect
    data class CopyText(val text: String) : PartsCenterEffect
}

class PartsCenterViewModel(
    private val ownedVehicleRepository: OwnedVehicleRepository,
    private val partsRepository: PartsRepository,
    private val catalogRepository: VehicleCatalogRepository? = null
) : MviViewModel<PartsCenterState, PartsCenterIntent, PartsCenterEffect>(PartsCenterState()) {

    init {
        setState { copy(categories = partsRepository.getCategories()) }
        viewModelScope.launch {
            combine(
                ownedVehicleRepository.vehicles,
                ownedVehicleRepository.activeVehicleId
            ) { vehicles, activeId ->
                val selected = vehicles.firstOrNull { it.id == activeId } ?: vehicles.firstOrNull()
                vehicles to selected
            }.collectLatest { (vehicles, selected) ->
                handleVehicleSelection(vehicles, selected)
            }
        }
    }

    override fun onIntent(intent: PartsCenterIntent) {
        when (intent) {
            is PartsCenterIntent.SelectCategory -> {
                setState { copy(selectedCategory = intent.category) }
                queryParts()
            }
            is PartsCenterIntent.UpdateSearchQuery -> {
                setState { copy(searchQuery = intent.query) }
                queryParts()
            }
            is PartsCenterIntent.CopyToClipboard -> {
                viewModelScope.launch {
                    setEffect { PartsCenterEffect.CopyText(intent.text) }
                }
                setState { copy(toastMessage = intent.text) }
            }
            is PartsCenterIntent.ClearToast -> {
                setState { copy(toastMessage = null) }
            }
            is PartsCenterIntent.Refresh -> {
                queryParts()
            }
        }
    }

    private suspend fun handleVehicleSelection(vehicles: List<OwnedVehicle>, selected: OwnedVehicle?) {
        if (selected == null) {
            setState {
                copy(
                    vehicles = vehicles,
                    activeVehicle = null,
                    resolvedMatch = null,
                    compatibleParts = emptyList()
                )
            }
            return
        }

        val catalogMatch = catalogRepository?.resolveOwnedVehicle(selected)?.exactMatch

        setState {
            copy(
                vehicles = vehicles,
                activeVehicle = selected,
                resolvedMatch = catalogMatch
            )
        }
        queryParts()
    }

    private fun queryParts() {
        val s = uiState.value
        val vehicle = s.activeVehicle
        val match = s.resolvedMatch

        if (vehicle == null || match == null) {
            setState { copy(compatibleParts = emptyList()) }
            return
        }

        val makeId = match?.make?.id ?: (vehicle.manufacturer?.lowercase()?.trim() ?: "")
        val modelId = match?.model?.id ?: (vehicle.model?.lowercase()?.trim() ?: "")

        val parts = partsRepository.findCompatibleParts(
            vehicleMakeId = makeId,
            vehicleModelId = modelId,
            generationId = match?.generation?.id,
            engineId = match?.engine?.id,
            engineCode = match?.engine?.code ?: vehicle.engineCode,
            engineFamily = match?.engine?.family,
            modelYear = vehicle.year,
            category = s.selectedCategory
        )

        val matches = if (s.searchQuery.isBlank()) parts else {
            val searchIds = partsRepository.searchParts(s.searchQuery).map { it.fitment.id }.toSet()
            parts.filter { it.fitment.id in searchIds }
        }
        setState { copy(compatibleParts = matches) }
    }
}
