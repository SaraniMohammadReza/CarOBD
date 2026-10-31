package com.sarani.carobd.data.repository

import android.content.ContentValues
import com.sarani.carobd.core.model.MaintenanceDueEngine
import com.sarani.carobd.core.model.MaintenanceIntervalProvenance
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.VehicleMaintenanceRecord
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

interface MaintenanceRepository {
    val changes: kotlinx.coroutines.flow.Flow<Long> get() = kotlinx.coroutines.flow.flowOf(0L)
    suspend fun getSchedules(vehicleId: String, currentOdometerKm: Long? = null): List<MaintenanceSchedule>
    suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit>
    suspend fun deleteSchedule(scheduleId: String): Boolean

    suspend fun getRecords(vehicleId: String): List<VehicleMaintenanceRecord>
    suspend fun saveRecord(record: VehicleMaintenanceRecord): Result<Unit>
    suspend fun deleteRecord(recordId: String): Boolean

    suspend fun recordCompletedService(
        scheduleId: String,
        performedKm: Long,
        performedDateMs: Long,
        costTomans: Long = 0L,
        serviceCenter: String? = null,
        partsBrand: String? = null,
        partNumber: String? = null,
        notes: String? = null
    ): Result<VehicleMaintenanceRecord>
}

class SQLiteMaintenanceRepository(
    private val dbHelper: CarObdDatabaseHelper
) : MaintenanceRepository {
    private val revision = kotlinx.coroutines.flow.MutableStateFlow(0L)
    override val changes: kotlinx.coroutines.flow.Flow<Long> get() = revision
    private fun changed() { revision.value += 1 }
    private fun <T> transaction(block: () -> T): T {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        return try { val value = block(); db.setTransactionSuccessful(); value } finally { db.endTransaction() }
    }

    override suspend fun getSchedules(vehicleId: String, currentOdometerKm: Long?): List<MaintenanceSchedule> =
        withContext(Dispatchers.IO) {
            dbHelper.getMaintenanceSchedulesForVehicle(vehicleId, currentOdometerKm)
        }

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { transaction { dbHelper.saveMaintenanceSchedule(schedule) }; changed() }
        }

    override suspend fun deleteSchedule(scheduleId: String): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.deleteMaintenanceSchedule(scheduleId).also { if(it) changed() }
        }

    override suspend fun getRecords(vehicleId: String): List<VehicleMaintenanceRecord> =
        withContext(Dispatchers.IO) {
            dbHelper.getVehicleMaintenanceRecords(vehicleId)
        }

    override suspend fun saveRecord(record: VehicleMaintenanceRecord): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                transaction {
                    val old = dbHelper.writableDatabase.rawQuery("SELECT vehicle_id,item_key,schedule_id FROM vehicle_maintenance_records WHERE id=?", arrayOf(record.id)).use { c ->
                        if(c.moveToFirst()) Triple(c.getString(0),c.getString(1),if(c.isNull(2)) null else c.getString(2)) else null
                    }
                    if(old != null) require(old.first == record.vehicleId && old.second == record.itemKey && old.third == record.scheduleId) { "History identity is immutable" }
                    record.scheduleId?.let { id ->
                        check(dbHelper.getMaintenanceSchedulesForVehicle(record.vehicleId).any { it.id == id && it.itemKey == record.itemKey }) { "Schedule mismatch" }
                    }
                    dbHelper.saveVehicleMaintenanceRecord(record)
                    record.scheduleId?.let(dbHelper::reconcileScheduleHistory)
                }
                changed()
            }
        }

    override suspend fun deleteRecord(recordId: String): Boolean =
        withContext(Dispatchers.IO) {
            transaction {
                val scheduleId = dbHelper.writableDatabase.rawQuery("SELECT schedule_id FROM vehicle_maintenance_records WHERE id=?", arrayOf(recordId)).use { if(it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }
                dbHelper.deleteVehicleMaintenanceRecord(recordId).also { deleted ->
                    if(deleted) { scheduleId?.let(dbHelper::reconcileScheduleHistory); changed() }
                }
            }
        }

    override suspend fun recordCompletedService(
        scheduleId: String,
        performedKm: Long,
        performedDateMs: Long,
        costTomans: Long,
        serviceCenter: String?,
        partsBrand: String?,
        partNumber: String?,
        notes: String?
    ): Result<VehicleMaintenanceRecord> = withContext(Dispatchers.IO) {
        runCatching {
          val completed = transaction {
            val db = dbHelper.writableDatabase
            val cursor = db.query(
                CarObdDatabaseHelper.TABLE_VEHICLE_MAINTENANCE_SCHEDULES,
                null,
                "id = ?",
                arrayOf(scheduleId),
                null,
                null,
                null
            )
            val schedule = cursor.use { c ->
                if (!c.moveToNext()) error("Schedule not found: $scheduleId")
                val intervalKm = if (c.isNull(c.getColumnIndexOrThrow("interval_km"))) null else c.getInt(c.getColumnIndexOrThrow("interval_km"))
                val intervalMonths = if (c.isNull(c.getColumnIndexOrThrow("interval_months"))) null else c.getInt(c.getColumnIndexOrThrow("interval_months"))
                val itemKey = c.getString(c.getColumnIndexOrThrow("item_key"))
                val vehicleId = c.getString(c.getColumnIndexOrThrow("vehicle_id"))
                Triple(vehicleId, itemKey, Pair(intervalKm, intervalMonths))
            }

            val (vehicleId, itemKey, intervals) = schedule
            val (intervalKm, intervalMonths) = intervals

            val record = VehicleMaintenanceRecord(
                scheduleId = scheduleId,
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                itemKey = itemKey,
                performedKm = performedKm,
                performedDateMs = performedDateMs,
                costTomans = costTomans,
                serviceCenter = serviceCenter,
                partsBrand = partsBrand,
                partNumber = partNumber,
                notes = notes,
                provenance = MaintenanceIntervalProvenance.USER_CUSTOM,
                createdAtMs = System.currentTimeMillis()
            )
            dbHelper.saveVehicleMaintenanceRecord(record)

            dbHelper.reconcileScheduleHistory(scheduleId)
            check(db.update("owned_vehicles", ContentValues().apply {
                val previous = dbHelper.ownedVehicles().first { it.id == vehicleId }
                put("current_odometer_km", maxOf(previous.currentOdometerKm ?: performedKm, performedKm))
                put("updated_at", System.currentTimeMillis())
            }, "id=? AND archived=0", arrayOf(vehicleId)) == 1)

            record
          }
          changed()
          completed
        }
    }
}
