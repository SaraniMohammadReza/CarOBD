package com.sarani.carobd.data.repository

import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import com.sarani.carobd.core.storage.AppPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Single, persisted, observable source of truth for the active vehicle (§B3).
 *
 * The selected vehicle id is persisted in [AppPreferencesManager]'s DataStore, so the
 * selection survives process restart. The id is resolved against the catalogue on read,
 * yielding a nullable [StateFlow] — a value is only emitted when the user has actually
 * selected a vehicle, never a fabricated PRESET_VEHICLES.first() default.
 *
 * No screen initializes its own private vehicle; every consumer reads
 * [ObdRepository.activeVehicleProfile], which is fed by this repository.
 */
class ActiveVehicleRepository(
    private val activeVehicleId: kotlinx.coroutines.flow.Flow<String?>,
    private val persistId: suspend (String) -> Unit,
    private val clearId: suspend () -> Unit
) {
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** The user-selected vehicle, or null when none has been selected. */
    val activeVehicle: StateFlow<VehicleProfile?> = activeVehicleId
        .map { id -> id?.let { IranianEcuRegistry.PRESET_VEHICLES.find { p -> p.id == id } } }
        .stateIn(ioScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Persist the selected vehicle so it survives process restart. */
    suspend fun selectVehicle(profile: VehicleProfile) {
        persistId(profile.id)
    }

    /** Persist selection by id. */
    suspend fun selectVehicleById(id: String) {
        persistId(id)
    }

    /** Clear any selected vehicle. */
    suspend fun clearVehicle() {
        clearId()
    }

    /** No-op: [activeVehicle] restores from DataStore on creation. Kept for the §B3 contract. */
    suspend fun restore() { /* no-op — stateIn already restores */ }
}
