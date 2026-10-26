package com.sarani.carobd.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DrivingStyleMetrics
import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.DtcSeverity
import com.sarani.carobd.core.model.DtcSystem
import com.sarani.carobd.core.model.GarageVehicle
import com.sarani.carobd.core.model.ServiceRecord
import com.sarani.carobd.core.model.ServiceType
import com.sarani.carobd.core.model.TripRecord

/**
 * Production SQLite database helper for automotive persistence.
 * Zero annotation processor dependencies; guaranteed runtime compatibility across all Android versions.
 */
class CarObdDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "car_obd_automotive.db"
        const val DATABASE_VERSION = 1

        // Tables
        const val TABLE_GARAGE = "garage_vehicles"
        const val TABLE_DIAGNOSTIC_SESSIONS = "diagnostic_sessions"
        const val TABLE_DTC_RECORDS = "dtc_records"
        const val TABLE_TRIP_RECORDS = "trip_records"
        const val TABLE_MAINTENANCE = "maintenance_records"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_GARAGE (
                id TEXT PRIMARY KEY,
                profile_id TEXT NOT NULL,
                nickname TEXT NOT NULL,
                manufacturer TEXT NOT NULL,
                model TEXT NOT NULL,
                year INTEGER NOT NULL,
                engine_code TEXT NOT NULL,
                fuel_type TEXT NOT NULL,
                license_plate TEXT,
                vin TEXT,
                odometer_km INTEGER NOT NULL,
                tank_capacity_liters REAL NOT NULL,
                preferred_adapter TEXT,
                preferred_protocol TEXT,
                is_default INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DIAGNOSTIC_SESSIONS (
                id TEXT PRIMARY KEY,
                vehicle_id TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                dtc_count INTEGER NOT NULL,
                mil_active INTEGER NOT NULL,
                ecu_name TEXT NOT NULL,
                protocol TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_DTC_RECORDS (
                id TEXT PRIMARY KEY,
                session_id TEXT NOT NULL,
                code TEXT NOT NULL,
                title_en TEXT NOT NULL,
                title_fa TEXT NOT NULL,
                description_en TEXT NOT NULL,
                description_fa TEXT NOT NULL,
                severity TEXT NOT NULL,
                system TEXT NOT NULL,
                category TEXT NOT NULL,
                is_mil INTEGER NOT NULL,
                ecu_source TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_TRIP_RECORDS (
                id TEXT PRIMARY KEY,
                vehicle_id TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                duration_sec INTEGER NOT NULL,
                distance_km REAL NOT NULL,
                max_speed INTEGER NOT NULL,
                max_rpm INTEGER NOT NULL,
                avg_speed REAL NOT NULL,
                fuel_liters REAL NOT NULL,
                hard_accel INTEGER NOT NULL DEFAULT 0,
                hard_brake INTEGER NOT NULL DEFAULT 0,
                high_rpm_sec INTEGER NOT NULL DEFAULT 0,
                idle_sec INTEGER NOT NULL DEFAULT 0,
                smoothness INTEGER NOT NULL DEFAULT 85,
                eco_score INTEGER NOT NULL DEFAULT 80,
                sample_count INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MAINTENANCE (
                id TEXT PRIMARY KEY,
                vehicle_id TEXT NOT NULL,
                type_key TEXT NOT NULL,
                performed_km INTEGER NOT NULL,
                performed_date INTEGER NOT NULL,
                cost_tomans INTEGER NOT NULL DEFAULT 0,
                parts_brand TEXT NOT NULL,
                notes TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration strategy
    }

    // -------------------------------------------------------------
    // GARAGE VEHICLE OPERATIONS
    // -------------------------------------------------------------

    fun insertGarageVehicle(v: GarageVehicle) {
        val cv = ContentValues().apply {
            put("id", v.id)
            put("profile_id", v.profileId)
            put("nickname", v.nickname)
            put("manufacturer", v.manufacturer)
            put("model", v.model)
            put("year", v.year)
            put("engine_code", v.engineCode)
            put("fuel_type", v.fuelType)
            put("license_plate", v.licensePlate)
            put("vin", v.vin)
            put("odometer_km", v.odometerKm)
            put("tank_capacity_liters", v.tankCapacityLiters)
            put("preferred_adapter", v.preferredAdapterAddress)
            put("preferred_protocol", v.preferredProtocolId)
            put("is_default", if (v.isDefault) 1 else 0)
            put("created_at", v.createdAtMs)
        }
        writableDatabase.insertWithOnConflict(TABLE_GARAGE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllGarageVehicles(): List<GarageVehicle> {
        val list = mutableListOf<GarageVehicle>()
        val cursor = readableDatabase.query(TABLE_GARAGE, null, null, null, null, null, "is_default DESC, created_at DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(cursorToGarageVehicle(c))
            }
        }
        return list
    }

    fun deleteGarageVehicle(id: String) {
        writableDatabase.delete(TABLE_GARAGE, "id = ?", arrayOf(id))
    }

    private fun cursorToGarageVehicle(c: Cursor): GarageVehicle {
        return GarageVehicle(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            profileId = c.getString(c.getColumnIndexOrThrow("profile_id")),
            nickname = c.getString(c.getColumnIndexOrThrow("nickname")),
            manufacturer = c.getString(c.getColumnIndexOrThrow("manufacturer")),
            model = c.getString(c.getColumnIndexOrThrow("model")),
            year = c.getInt(c.getColumnIndexOrThrow("year")),
            engineCode = c.getString(c.getColumnIndexOrThrow("engine_code")),
            fuelType = c.getString(c.getColumnIndexOrThrow("fuel_type")),
            licensePlate = c.getString(c.getColumnIndexOrThrow("license_plate")),
            vin = c.getString(c.getColumnIndexOrThrow("vin")),
            odometerKm = c.getInt(c.getColumnIndexOrThrow("odometer_km")),
            tankCapacityLiters = c.getFloat(c.getColumnIndexOrThrow("tank_capacity_liters")),
            preferredAdapterAddress = c.getString(c.getColumnIndexOrThrow("preferred_adapter")),
            preferredProtocolId = c.getString(c.getColumnIndexOrThrow("preferred_protocol")),
            isDefault = c.getInt(c.getColumnIndexOrThrow("is_default")) == 1,
            createdAtMs = c.getLong(c.getColumnIndexOrThrow("created_at"))
        )
    }

    // -------------------------------------------------------------
    // TRIP RECORD OPERATIONS
    // -------------------------------------------------------------

    fun insertTripRecord(trip: TripRecord) {
        val cv = ContentValues().apply {
            put("id", trip.id)
            put("vehicle_id", trip.vehicleId)
            put("start_time", trip.startTimeMs)
            put("end_time", trip.endTimeMs)
            put("duration_sec", trip.durationSeconds)
            put("distance_km", trip.distanceKm)
            put("max_speed", trip.maxSpeedKmh)
            put("max_rpm", trip.maxRpm)
            put("avg_speed", trip.averageSpeedKmh)
            put("fuel_liters", trip.estimatedFuelUsedLiters)
            put("hard_accel", trip.drivingStyle.hardAccelerationsCount)
            put("hard_brake", trip.drivingStyle.hardBrakingCount)
            put("high_rpm_sec", trip.drivingStyle.highRpmDurationSeconds)
            put("idle_sec", trip.drivingStyle.idleDurationSeconds)
            put("smoothness", trip.drivingStyle.smoothnessScore)
            put("eco_score", trip.drivingStyle.ecoScore)
            put("sample_count", trip.sampleCount)
        }
        writableDatabase.insertWithOnConflict(TABLE_TRIP_RECORDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getTripsForVehicle(vehicleId: String): List<TripRecord> {
        val list = mutableListOf<TripRecord>()
        val cursor = readableDatabase.query(TABLE_TRIP_RECORDS, null, "vehicle_id = ?", arrayOf(vehicleId), null, null, "start_time DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    TripRecord(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        vehicleId = c.getString(c.getColumnIndexOrThrow("vehicle_id")),
                        startTimeMs = c.getLong(c.getColumnIndexOrThrow("start_time")),
                        endTimeMs = c.getLong(c.getColumnIndexOrThrow("end_time")),
                        durationSeconds = c.getLong(c.getColumnIndexOrThrow("duration_sec")),
                        distanceKm = c.getDouble(c.getColumnIndexOrThrow("distance_km")),
                        maxSpeedKmh = c.getInt(c.getColumnIndexOrThrow("max_speed")),
                        maxRpm = c.getInt(c.getColumnIndexOrThrow("max_rpm")),
                        averageSpeedKmh = c.getDouble(c.getColumnIndexOrThrow("avg_speed")),
                        estimatedFuelUsedLiters = c.getDouble(c.getColumnIndexOrThrow("fuel_liters")),
                        drivingStyle = DrivingStyleMetrics(
                            hardAccelerationsCount = c.getInt(c.getColumnIndexOrThrow("hard_accel")),
                            hardBrakingCount = c.getInt(c.getColumnIndexOrThrow("hard_brake")),
                            highRpmDurationSeconds = c.getLong(c.getColumnIndexOrThrow("high_rpm_sec")),
                            idleDurationSeconds = c.getLong(c.getColumnIndexOrThrow("idle_sec")),
                            smoothnessScore = c.getInt(c.getColumnIndexOrThrow("smoothness")),
                            ecoScore = c.getInt(c.getColumnIndexOrThrow("eco_score"))
                        ),
                        sampleCount = c.getInt(c.getColumnIndexOrThrow("sample_count"))
                    )
                )
            }
        }
        return list
    }

    // -------------------------------------------------------------
    // MAINTENANCE OPERATIONS
    // -------------------------------------------------------------

    fun insertMaintenanceRecord(record: ServiceRecord) {
        val cv = ContentValues().apply {
            put("id", record.id)
            put("vehicle_id", record.vehicleId)
            put("type_key", record.type.name)
            put("performed_km", record.performedAtKm)
            put("performed_date", record.performedAtDateMs)
            put("cost_tomans", record.costTomans)
            put("parts_brand", record.partsBrand)
            put("notes", record.notes)
        }
        writableDatabase.insertWithOnConflict(TABLE_MAINTENANCE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getMaintenanceForVehicle(vehicleId: String): List<ServiceRecord> {
        val list = mutableListOf<ServiceRecord>()
        val cursor = readableDatabase.query(TABLE_MAINTENANCE, null, "vehicle_id = ?", arrayOf(vehicleId), null, null, "performed_km DESC")
        cursor.use { c ->
            while (c.moveToNext()) {
                val typeName = c.getString(c.getColumnIndexOrThrow("type_key"))
                val type = runCatching { ServiceType.valueOf(typeName) }.getOrDefault(ServiceType.ENGINE_OIL)
                list.add(
                    ServiceRecord(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        vehicleId = c.getString(c.getColumnIndexOrThrow("vehicle_id")),
                        type = type,
                        performedAtKm = c.getInt(c.getColumnIndexOrThrow("performed_km")),
                        performedAtDateMs = c.getLong(c.getColumnIndexOrThrow("performed_date")),
                        costTomans = c.getLong(c.getColumnIndexOrThrow("cost_tomans")),
                        partsBrand = c.getString(c.getColumnIndexOrThrow("parts_brand")),
                        notes = c.getString(c.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    // -------------------------------------------------------------
    // DTC OPERATIONS
    // -------------------------------------------------------------

    fun saveDtcScan(sessionId: String, vehicleId: String, codes: List<DiagnosticCode>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val sessionCv = ContentValues().apply {
                put("id", sessionId)
                put("vehicle_id", vehicleId)
                put("start_time", System.currentTimeMillis())
                put("end_time", System.currentTimeMillis())
                put("dtc_count", codes.size)
                put("mil_active", if (codes.isNotEmpty()) 1 else 0)
                put("ecu_name", if (codes.isNotEmpty()) codes.first().ecuSource else "Engine ECU")
                put("protocol", "Standard OBD-II")
            }
            db.insertWithOnConflict(TABLE_DIAGNOSTIC_SESSIONS, null, sessionCv, SQLiteDatabase.CONFLICT_REPLACE)

            for ((index, dtc) in codes.withIndex()) {
                val dtcCv = ContentValues().apply {
                    put("id", "${sessionId}_${index}_${dtc.code}")
                    put("session_id", sessionId)
                    put("code", dtc.code)
                    put("title_en", dtc.titleEn)
                    put("title_fa", dtc.titleFa)
                    put("description_en", dtc.descriptionEn)
                    put("description_fa", dtc.descriptionFa)
                    put("severity", dtc.severity.name)
                    put("system", dtc.system.name)
                    put("category", "STORED")
                    put("is_mil", 1)
                    put("ecu_source", dtc.ecuSource)
                    put("timestamp", System.currentTimeMillis())
                }
                db.insertWithOnConflict(TABLE_DTC_RECORDS, null, dtcCv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
