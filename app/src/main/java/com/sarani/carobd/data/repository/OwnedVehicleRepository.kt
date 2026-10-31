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
interface OwnedVehicleRepository {
    val vehicles: StateFlow<List<OwnedVehicle>>
    val activeVehicleId: StateFlow<String?>
    suspend fun refresh()
    suspend fun save(vehicle: OwnedVehicle): Result<Unit>
    suspend fun select(id: String?): Boolean
    suspend fun delete(id: String): Boolean
}

class SQLiteOwnedVehicleRepository(
    private val database: CarObdDatabaseHelper,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : OwnedVehicleRepository {
    private val _vehicles = MutableStateFlow<List<OwnedVehicle>>(emptyList())
    override val vehicles: StateFlow<List<OwnedVehicle>> = _vehicles.asStateFlow()
    private val _activeVehicleId = MutableStateFlow<String?>(null)
    override val activeVehicleId: StateFlow<String?> = _activeVehicleId.asStateFlow()

    override suspend fun refresh() = withContext(io) {
        _vehicles.value = database.ownedVehicles()
        _activeVehicleId.value = database.activeOwnedVehicleId()
    }

    override suspend fun save(vehicle: OwnedVehicle): Result<Unit> = runCatching {
        withContext(io) { database.saveOwnedVehicle(vehicle) }
        refresh()
    }

    override suspend fun select(id: String?): Boolean = withContext(io) { database.selectOwnedVehicle(id) }.also {
        if (it) refresh()
    }

    override suspend fun delete(id: String): Boolean = withContext(io) { database.deleteOwnedVehicle(id) }.also {
        if (it) refresh()
    }
}
