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
import com.sarani.carobd.core.model.*

/**
 * Production SQLite database helper for automotive persistence.
 * Additive evidence migrations; Android runtime behavior requires device validation.
 */
class CarObdDatabaseHelper(context: Context, databaseName: String = DATABASE_NAME) : SQLiteOpenHelper(context, databaseName, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "car_obd_automotive.db"
        const val DATABASE_VERSION = 5

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
            DtcHistoryMigration.version2.forEach(db::execSQL)
        TripEvidenceMigration.version3.forEach(db::execSQL)
        DiagnosticReportMigration.version4.forEach(db::execSQL)
        OwnedVehicleMigration.version5.forEach(db::execSQL)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2 && newVersion >= 2) DtcHistoryMigration.version2.forEach(db::execSQL)
        if (oldVersion < 3 && newVersion >= 3) TripEvidenceMigration.version3.forEach(db::execSQL)
        if (oldVersion < 4 && newVersion >= 4) DiagnosticReportMigration.version4.forEach(db::execSQL)
        if (oldVersion < 5 && newVersion >= 5) OwnedVehicleMigration.version5.forEach(db::execSQL)
    }

    fun saveOwnedVehicle(vehicle: OwnedVehicle) {
        val normalized = vehicle.normalized()
        require(normalized.validationErrors().isEmpty()) { normalized.validationErrors().joinToString() }
        val values = ContentValues().apply {
            put("id", normalized.id); put("display_name", normalized.displayName)
            putNullable("manufacturer", normalized.manufacturer); putNullable("model", normalized.model)
            putNullable("generation", normalized.generation); putNullable("year", normalized.year)
            putNullable("variant", normalized.variant); putNullable("engine", normalized.engine)
            putNullable("engine_code", normalized.engineCode); putNullable("transmission", normalized.transmission)
            putNullable("market", normalized.market); putNullable("vin", normalized.vin)
            putNullable("license_plate", normalized.licensePlate)
            putNullable("current_odometer_km", normalized.currentOdometerKm)
            putNullable("tank_capacity_liters", normalized.tankCapacityLiters)
            put("created_at", normalized.createdAtMs); put("updated_at", normalized.updatedAtMs)
        }
        check(writableDatabase.insertWithOnConflict("owned_vehicles", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }

    fun ownedVehicles(): List<OwnedVehicle> = buildList {
        readableDatabase.query("owned_vehicles", null, null, null, null, null, "updated_at DESC").use { c ->
            while (c.moveToNext()) add(c.toOwnedVehicle())
        }
    }

    fun activeOwnedVehicleId(): String? = readableDatabase.query(
        "owned_vehicle_selection", arrayOf("vehicle_id"), "singleton_id=1", null, null, null, null
    ).use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null }

    fun selectOwnedVehicle(id: String?): Boolean {
        if (id != null && !readableDatabase.query("owned_vehicles", arrayOf("id"), "id=?", arrayOf(id), null, null, null).use { it.moveToFirst() }) return false
        val values = ContentValues().apply { put("singleton_id", 1); putNullable("vehicle_id", id) }
        return writableDatabase.insertWithOnConflict("owned_vehicle_selection", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L
    }

    fun deleteOwnedVehicle(id: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val deleted = db.delete("owned_vehicles", "id=?", arrayOf(id)) > 0
            db.execSQL("UPDATE owned_vehicle_selection SET vehicle_id=NULL WHERE singleton_id=1 AND vehicle_id=?", arrayOf(id))
            db.setTransactionSuccessful()
            deleted
        } finally { db.endTransaction() }
    }

    private fun Cursor.toOwnedVehicle() = OwnedVehicle(
        id = getString(getColumnIndexOrThrow("id")), displayName = getString(getColumnIndexOrThrow("display_name")),
        manufacturer = nullableString("manufacturer"), model = nullableString("model"), generation = nullableString("generation"),
        year = nullableInt("year"), variant = nullableString("variant"), engine = nullableString("engine"),
        engineCode = nullableString("engine_code"), transmission = nullableString("transmission"), market = nullableString("market"),
        vin = nullableString("vin"), licensePlate = nullableString("license_plate"),
        currentOdometerKm = nullableLong("current_odometer_km"), tankCapacityLiters = nullableDouble("tank_capacity_liters"),
        createdAtMs = getLong(getColumnIndexOrThrow("created_at")), updatedAtMs = getLong(getColumnIndexOrThrow("updated_at")),
    )

    private fun Cursor.nullableString(name: String) = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.nullableInt(name: String) = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getInt(it) }
    private fun Cursor.nullableLong(name: String) = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getLong(it) }
    private fun Cursor.nullableDouble(name: String) = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getDouble(it) }

    private fun ContentValues.putNullable(key: String, value: String?) { if (value == null) putNull(key) else put(key, value) }
    private fun ContentValues.putNullable(key: String, value: Int?) { if (value == null) putNull(key) else put(key, value) }
    private fun ContentValues.putNullable(key: String, value: Long?) { if (value == null) putNull(key) else put(key, value) }
    private fun ContentValues.putNullable(key: String, value: Double?) { if (value == null) putNull(key) else put(key, value) }

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
        require(!trip.legacyUnverified && trip.evidence.map { it.metric }.toSet() == TripMetric.entries.toSet()) { "New trips require complete metric provenance" }
        require(trip.evidence.size == TripMetric.entries.size)
        val cv = ContentValues().apply {
            put("id", trip.id)
            put("vehicle_id", trip.vehicleId)
            put("start_time", trip.startTimeMs)
            put("end_time", trip.endTimeMs)
            put("duration_sec", trip.durationSeconds)
            put("distance_km", trip.distanceKm ?: 0.0) // Legacy compatibility column; evidence is authoritative.
            put("max_speed", trip.maxSpeedKmh ?: 0) // Legacy compatibility column; evidence is authoritative.
            put("max_rpm", trip.maxRpm ?: 0) // Legacy compatibility column; evidence is authoritative.
            put("avg_speed", trip.averageSpeedKmh ?: 0.0) // Legacy compatibility column; evidence is authoritative.
            put("fuel_liters", trip.estimatedFuelUsedLiters ?: 0.0) // Legacy compatibility column; evidence is authoritative.
            put("hard_accel", trip.drivingStyle?.hardAccelerationsCount ?: 0)
            put("hard_brake", trip.drivingStyle?.hardBrakingCount ?: 0)
            put("high_rpm_sec", trip.drivingStyle?.highRpmDurationSeconds ?: 0L)
            put("idle_sec", trip.drivingStyle?.idleDurationSeconds ?: 0L)
            put("smoothness", trip.drivingStyle?.smoothnessScore ?: 0)
            put("eco_score", trip.drivingStyle?.ecoScore ?: 0)
            put("sample_count", trip.sampleCount)
        }
        cv.put("provenance", "CAPTURED_WITH_EVIDENCE")
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict(TABLE_TRIP_RECORDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) { "Trip persistence failed" } }
            db.delete("trip_metric_evidence", "trip_id=?", arrayOf(trip.id))
            trip.evidence.forEach { evidence ->
                val values = ContentValues().apply {
                    put("trip_id", trip.id); put("metric", evidence.metric.name); put("value", evidence.value)
                    put("provenance", evidence.provenance.name); put("quality", evidence.quality.name); put("source", evidence.source)
                    put("first_sample_ms", evidence.firstSampleMs); put("last_sample_ms", evidence.lastSampleMs)
                    put("sample_count", evidence.sampleCount); put("covered_ms", evidence.coveredMs); put("method", evidence.method)
                }
                db.insertOrThrow("trip_metric_evidence", null, values)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun getTripsForVehicle(vehicleId: String): List<TripRecord> {
        val list = mutableListOf<TripRecord>()
        val cursor = readableDatabase.query(TABLE_TRIP_RECORDS, null, "vehicle_id = ?", arrayOf(vehicleId), null, null, "start_time DESC", "100")
        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(c.getColumnIndexOrThrow("id"))
                val legacy = c.getString(c.getColumnIndexOrThrow("provenance")) != "CAPTURED_WITH_EVIDENCE"
                val evidence = if (legacy) listOf(
                    TripMetricEvidence.legacy(TripMetric.DURATION, c.getDouble(c.getColumnIndexOrThrow("duration_sec"))),
                    TripMetricEvidence.legacy(TripMetric.DISTANCE, c.getDouble(c.getColumnIndexOrThrow("distance_km"))),
                    TripMetricEvidence.legacy(TripMetric.MAX_SPEED, c.getDouble(c.getColumnIndexOrThrow("max_speed"))),
                    TripMetricEvidence.legacy(TripMetric.MAX_RPM, c.getDouble(c.getColumnIndexOrThrow("max_rpm"))),
                    TripMetricEvidence.legacy(TripMetric.AVERAGE_SPEED, c.getDouble(c.getColumnIndexOrThrow("avg_speed"))),
                    TripMetricEvidence.legacy(TripMetric.FUEL_USED, c.getDouble(c.getColumnIndexOrThrow("fuel_liters")))
                ) + listOf(TripMetric.FUEL_ECONOMY, TripMetric.MAX_COOLANT, TripMetric.AVERAGE_LOAD).map { TripMetricEvidence.legacy(it, null) }
                else readTripEvidence(id)
                fun value(metric: TripMetric) = evidence.firstOrNull { it.metric == metric }?.value
                list.add(
                    TripRecord(
                        id = c.getString(c.getColumnIndexOrThrow("id")),
                        vehicleId = c.getString(c.getColumnIndexOrThrow("vehicle_id")),
                        startTimeMs = c.getLong(c.getColumnIndexOrThrow("start_time")),
                        endTimeMs = c.getLong(c.getColumnIndexOrThrow("end_time")),
                        durationSeconds = c.getLong(c.getColumnIndexOrThrow("duration_sec")),
                        distanceKm = value(TripMetric.DISTANCE),
                        maxSpeedKmh = value(TripMetric.MAX_SPEED)?.toInt(),
                        maxRpm = value(TripMetric.MAX_RPM)?.toInt(),
                        averageSpeedKmh = value(TripMetric.AVERAGE_SPEED),
                        estimatedFuelUsedLiters = value(TripMetric.FUEL_USED),
                        drivingStyle = if (legacy) DrivingStyleMetrics(
                            hardAccelerationsCount = c.getInt(c.getColumnIndexOrThrow("hard_accel")),
                            hardBrakingCount = c.getInt(c.getColumnIndexOrThrow("hard_brake")),
                            highRpmDurationSeconds = c.getLong(c.getColumnIndexOrThrow("high_rpm_sec")),
                            idleDurationSeconds = c.getLong(c.getColumnIndexOrThrow("idle_sec")),
                            smoothnessScore = c.getInt(c.getColumnIndexOrThrow("smoothness")),
                            ecoScore = c.getInt(c.getColumnIndexOrThrow("eco_score"))
                        ) else null,
                        sampleCount = c.getInt(c.getColumnIndexOrThrow("sample_count")), evidence = evidence, legacyUnverified = legacy
                    )
                )
            }
        }
        return list
    }

    private fun readTripEvidence(id: String): List<TripMetricEvidence> {
        val result = mutableListOf<TripMetricEvidence>()
        readableDatabase.query("trip_metric_evidence", null, "trip_id=?", arrayOf(id), null, null, "metric", "20").use { c ->
            fun text(name: String) = c.getColumnIndexOrThrow(name).let { if (c.isNull(it)) null else c.getString(it) }
            fun number(name: String) = c.getColumnIndexOrThrow(name).let { if (c.isNull(it)) null else c.getLong(it) }
            while (c.moveToNext()) {
                result += TripMetricEvidence(TripMetric.valueOf(requireNotNull(text("metric"))),
                    c.getColumnIndexOrThrow("value").let { if (c.isNull(it)) null else c.getDouble(it) },
                    MetricProvenance.valueOf(requireNotNull(text("provenance"))), MetricQuality.valueOf(requireNotNull(text("quality"))),
                    text("source"), number("first_sample_ms"), number("last_sample_ms"),
                    requireNotNull(number("sample_count")).toInt(), requireNotNull(number("covered_ms")), text("method"))
            }
        }
        check(result.map { it.metric }.toSet() == TripMetric.entries.toSet()) { "Incomplete trip evidence" }
        return result.sortedBy { it.metric.ordinal }
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

    fun saveDtcScan(scan: com.sarani.carobd.core.model.DtcHistorySession, codes: List<DiagnosticCode>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val sessionCv = ContentValues().apply {
                put("id", scan.id)
                put("vehicle_id", scan.vehicleId ?: "unselected")
                put("start_time", scan.observedAtMs); put("end_time", scan.observedAtMs)
                put("dtc_count", scan.codes.size)
                put("mil_active", scan.mil?.let { if (it) 1 else 0 } ?: -1)
                put("observed_mil", scan.mil?.let { if (it) 1 else 0 })
                put("ecu_name", "Unknown")
                put("protocol", scan.protocol ?: "Unknown")
                put("observed_protocol", scan.protocol)
                put("connection_session_id", scan.connectionSessionId)
                put("provenance", "OBSERVED_SCAN")
            }
            db.insertOrThrow(TABLE_DIAGNOSTIC_SESSIONS, null, sessionCv)
            for ((index, dtc) in codes.withIndex()) {
                val observed = scan.codes[index]
                val cv = ContentValues().apply {
                    put("id", "${scan.id}_${index}_${dtc.code}"); put("session_id", scan.id)
                    put("code", dtc.code); put("title_en", dtc.titleEn); put("title_fa", dtc.titleFa)
                    put("description_en", dtc.descriptionEn); put("description_fa", dtc.descriptionFa)
                    put("severity", dtc.severity.name); put("system", dtc.system.name)
                    put("category", observed.category?.name ?: "UNKNOWN")
                    put("is_mil", observed.mil?.let { if (it) 1 else 0 } ?: -1)
                    put("observed_mil", observed.mil?.let { if (it) 1 else 0 })
                    put("observed_protocol", observed.protocol); put("responder", observed.responder)
                    put("provenance", "OBSERVED_SCAN"); put("ecu_source", dtc.ecuSource)
                    put("timestamp", observed.observedAtMs)
                }
                db.insertOrThrow(TABLE_DTC_RECORDS, null, cv)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    fun diagnosticHistory(): List<com.sarani.carobd.core.model.DtcHistorySession> {
        val out = mutableListOf<com.sarani.carobd.core.model.DtcHistorySession>()
        val db = readableDatabase
        fun Cursor.nullText(name: String): String? = getColumnIndexOrThrow(name).let { if (isNull(it)) null else getString(it) }
        fun Cursor.mil(): Boolean? = getColumnIndexOrThrow("observed_mil").let { if (isNull(it)) null else when (getInt(it)) { 0 -> false; 1 -> true; else -> null } }
        db.query(TABLE_DIAGNOSTIC_SESSIONS, null, null, null, null, null, "start_time DESC", "100").use { sessions ->
            while (sessions.moveToNext()) {
                val id = sessions.getString(sessions.getColumnIndexOrThrow("id"))
                val legacy = sessions.nullText("provenance") != "OBSERVED_SCAN"
                val records = mutableListOf<com.sarani.carobd.core.model.DtcHistoryCode>()
                db.query(TABLE_DTC_RECORDS, null, "session_id=?", arrayOf(id), null, null, "timestamp ASC").use { rows ->
                    while (rows.moveToNext()) {
                        val old = rows.nullText("provenance") != "OBSERVED_SCAN"
                        records += com.sarani.carobd.core.model.DtcHistoryCode(rows.nullText("code").orEmpty(),
                            if (old) null else runCatching { DtcCategory.valueOf(rows.nullText("category").orEmpty()) }.getOrNull(),
                            rows.nullText("responder"), rows.mil(), rows.nullText("observed_protocol"),
                            rows.getLong(rows.getColumnIndexOrThrow("timestamp")), old)
                    }
                }
                out += com.sarani.carobd.core.model.DtcHistorySession(id, sessions.nullText("connection_session_id"),
                    sessions.nullText("vehicle_id")?.takeUnless { it == "unselected" }, sessions.getLong(sessions.getColumnIndexOrThrow("start_time")),
                    sessions.nullText("observed_protocol"), sessions.mil(), records, legacy)
            }
        }
        return out
    }
    fun saveDiagnosticReport(report: com.sarani.carobd.core.model.DiagnosticSessionReport) = DiagnosticReportStore.save(writableDatabase, report)
    fun recentDiagnosticReports(): List<com.sarani.carobd.core.model.DiagnosticSessionReport> = DiagnosticReportStore.recent(readableDatabase)
}
