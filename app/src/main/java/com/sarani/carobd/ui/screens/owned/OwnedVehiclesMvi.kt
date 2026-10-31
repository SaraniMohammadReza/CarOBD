package com.sarani.carobd.ui.screens.owned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.storage.VehicleSetupNumbers
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class OwnedVehicleDraft(
    val id: String? = null, val displayName: String = "", val manufacturer: String = "", val model: String = "",
    val generation: String = "", val year: String = "", val variant: String = "", val engine: String = "",
    val engineCode: String = "", val transmission: String = "", val fuelType: String = "", val market: String = "",
    val vin: String = "", val licensePlate: String = "", val odometer: String = "", val tankCapacity: String = "",
    val notes: String = "", val createdAtMs: Long? = null,
) {
    companion object {
        fun from(v: OwnedVehicle) = OwnedVehicleDraft(v.id, v.displayName, v.manufacturer.orEmpty(), v.model.orEmpty(),
            v.generation.orEmpty(), v.year?.toString().orEmpty(), v.variant.orEmpty(), v.engine.orEmpty(),
            v.engineCode.orEmpty(), v.transmission.orEmpty(), v.fuelType.orEmpty(), v.market.orEmpty(), v.vin.orEmpty(),
            v.licensePlate.orEmpty(), v.currentOdometerKm?.toString().orEmpty(), v.tankCapacityLiters?.toString().orEmpty(),
            v.notes.orEmpty(), v.createdAtMs)
    }
}

enum class OwnedVehicleEditorError { NAME_REQUIRED, YEAR_INVALID, ODOMETER_INVALID, TANK_INVALID, SAVE_FAILED }
data class OwnedVehiclesState(
    val vehicles: List<OwnedVehicle> = emptyList(), val selectedId: String? = null,
    val editor: OwnedVehicleDraft? = null, val deleteCandidate: OwnedVehicle? = null,
    val error: OwnedVehicleEditorError? = null,
)

class OwnedVehiclesViewModel(
    private val repository: OwnedVehicleRepository,
    private val catalog: com.sarani.carobd.core.vehicle.catalog.VehicleCatalogRepository = com.sarani.carobd.core.vehicle.catalog.DefaultVehicleCatalogRepository()
) : ViewModel() {
    private val editor = kotlinx.coroutines.flow.MutableStateFlow<OwnedVehicleDraft?>(null)
    private val deleting = kotlinx.coroutines.flow.MutableStateFlow<OwnedVehicle?>(null)
    private val error = kotlinx.coroutines.flow.MutableStateFlow<OwnedVehicleEditorError?>(null)
    val state = combine(repository.vehicles, repository.activeVehicleId, editor, deleting, error) { vehicles, selected, edit, delete, err ->
        OwnedVehiclesState(vehicles, selected, edit, delete, err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OwnedVehiclesState())

    init { viewModelScope.launch { repository.refresh() } }
    fun add() { error.value = null; editor.value = OwnedVehicleDraft() }
    fun edit(vehicle: OwnedVehicle) { error.value = null; editor.value = OwnedVehicleDraft.from(vehicle) }
    fun update(draft: OwnedVehicleDraft) { error.value = null; editor.value = draft }
    fun cancelEdit() { error.value = null; editor.value = null }
    fun select(id: String) { viewModelScope.launch { repository.select(id) } }
    fun requestDelete(vehicle: OwnedVehicle) { deleting.value = vehicle }
    fun cancelDelete() { deleting.value = null }
    fun confirmDelete() { val id = deleting.value?.id ?: return; viewModelScope.launch { repository.delete(id); deleting.value = null } }

    fun findCatalogMatches(make: String, model: String, year: String? = null, engine: String? = null): List<com.sarani.carobd.core.model.ResolvedVehicleMatch> {
        if (make.isBlank() || model.isBlank()) return emptyList()
        val parsedYear = year?.takeIf { it.isNotBlank() }?.let { VehicleSetupNumbers.modelYear(it) }
        return catalog.findMatchingVariants(make, model, parsedYear, engine)
    }

    fun applyCatalogSelection(match: com.sarani.carobd.core.model.ResolvedVehicleMatch) {
        val current = editor.value ?: return
        val tankCap = match.variant.specifications?.fuelTankCapacityLiters?.let {
            if (current.tankCapacity.isBlank()) it.toString() else current.tankCapacity
        } ?: current.tankCapacity
        editor.value = current.copy(
            manufacturer = match.make.name,
            model = match.model.name,
            generation = match.generation.name,
            variant = match.variant.trim,
            engine = match.engine.code,
            engineCode = match.engine.code,
            transmission = match.transmission?.type?.name ?: current.transmission,
            fuelType = match.engine.fuelType.name,
            market = match.generation.market.name,
            tankCapacity = tankCap
        )
    }

    fun save() {
        val d = editor.value ?: return
        val parsed = d.parse()
        val vehicle = parsed.vehicle ?: run { error.value = parsed.error; return }
        viewModelScope.launch {
            repository.save(vehicle).onSuccess { editor.value = null; error.value = null }
                .onFailure { error.value = OwnedVehicleEditorError.SAVE_FAILED }
        }
    }

    private fun fail(failure: OwnedVehicleEditorError): Nothing? { error.value = failure; return null }
}
