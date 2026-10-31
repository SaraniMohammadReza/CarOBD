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

    override suspend fun getSchedules(vehicleId: String, currentOdometerKm: Long?): List<MaintenanceSchedule> =
        withContext(Dispatchers.IO) {
            dbHelper.getMaintenanceSchedulesForVehicle(vehicleId, currentOdometerKm)
        }

    override suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { dbHelper.saveMaintenanceSchedule(schedule) }
        }

    override suspend fun deleteSchedule(scheduleId: String): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.deleteMaintenanceSchedule(scheduleId)
        }

    override suspend fun getRecords(vehicleId: String): List<VehicleMaintenanceRecord> =
        withContext(Dispatchers.IO) {
            dbHelper.getVehicleMaintenanceRecords(vehicleId)
        }

    override suspend fun saveRecord(record: VehicleMaintenanceRecord): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { dbHelper.saveVehicleMaintenanceRecord(record) }
        }

    override suspend fun deleteRecord(recordId: String): Boolean =
        withContext(Dispatchers.IO) {
            dbHelper.deleteVehicleMaintenanceRecord(recordId)
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

            val nextDue = MaintenanceDueEngine.calculateDueState(
                currentKm = performedKm,
                currentDateMs = performedDateMs,
                lastKm = performedKm,
                lastDateMs = performedDateMs,
                intervalKm = intervalKm,
                intervalMonths = intervalMonths
            )

            val updateCv = ContentValues().apply {
                put("last_service_km", performedKm)
                put("last_service_date", performedDateMs)
                put("next_due_km", nextDue.nextDueKm)
                put("next_due_date", nextDue.nextDueDateMs)
                put("updated_at", System.currentTimeMillis())
            }
            db.update(
                CarObdDatabaseHelper.TABLE_VEHICLE_MAINTENANCE_SCHEDULES,
                updateCv,
                "id = ?",
                arrayOf(scheduleId)
            )

            record
        }
    }
}
