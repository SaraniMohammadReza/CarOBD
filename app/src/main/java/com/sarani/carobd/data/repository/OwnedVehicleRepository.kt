package com.sarani.carobd.data.repository

import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Owner-garage persistence. This repository never reads or writes ECU routing selection. */
class OwnedVehicleRepository(
    private val database: CarObdDatabaseHelper,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _vehicles = MutableStateFlow<List<OwnedVehicle>>(emptyList())
    val vehicles: StateFlow<List<OwnedVehicle>> = _vehicles.asStateFlow()
    private val _activeVehicleId = MutableStateFlow<String?>(null)
    val activeVehicleId: StateFlow<String?> = _activeVehicleId.asStateFlow()

    suspend fun refresh() = withContext(io) {
        _vehicles.value = database.ownedVehicles()
        _activeVehicleId.value = database.activeOwnedVehicleId()
    }

    suspend fun save(vehicle: OwnedVehicle): Result<Unit> = runCatching {
        withContext(io) { database.saveOwnedVehicle(vehicle) }
        refresh()
    }

    suspend fun select(id: String?): Boolean = withContext(io) { database.selectOwnedVehicle(id) }.also {
        if (it) refresh()
    }

    suspend fun delete(id: String): Boolean = withContext(io) { database.deleteOwnedVehicle(id) }.also {
        if (it) refresh()
    }
}
