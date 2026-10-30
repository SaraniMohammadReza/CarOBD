package com.sarani.carobd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.MetricProvenance
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Device execution required: actual SQLiteOpenHelper v1→v4 upgrade and domain history reader. */
@RunWith(AndroidJUnit4::class)
class DtcHistoryMigrationInstrumentedTest {
    @Test fun versionOneRowsSurviveWithUnknownObservationFields() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "legacy-history-${java.util.UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { db ->
                db.execSQL("CREATE TABLE diagnostic_sessions (id TEXT PRIMARY KEY, vehicle_id TEXT, start_time INTEGER, end_time INTEGER, dtc_count INTEGER, mil_active INTEGER, ecu_name TEXT, protocol TEXT)")
                db.execSQL("CREATE TABLE dtc_records (id TEXT PRIMARY KEY, session_id TEXT, code TEXT, title_en TEXT, title_fa TEXT, description_en TEXT, description_fa TEXT, severity TEXT, system TEXT, category TEXT, is_mil INTEGER, ecu_source TEXT, timestamp INTEGER)")
                db.execSQL("CREATE TABLE trip_records(id TEXT PRIMARY KEY, vehicle_id TEXT, start_time INTEGER, end_time INTEGER, duration_sec INTEGER, distance_km REAL, max_speed INTEGER, max_rpm INTEGER, avg_speed REAL, fuel_liters REAL, hard_accel INTEGER, hard_brake INTEGER, high_rpm_sec INTEGER, idle_sec INTEGER, smoothness INTEGER, eco_score INTEGER, sample_count INTEGER)")
                db.execSQL("INSERT INTO diagnostic_sessions VALUES ('legacy','vehicle',1,2,3,1,'Engine ECU','Standard OBD-II')")
                for ((index, category) in listOf("STORED", "PENDING", "PERMANENT").withIndex()) {
                    db.execSQL("INSERT INTO dtc_records VALUES (?,?, 'P0133','','','','','MODERATE','POWERTRAIN',?,1,'Engine ECU',2)", arrayOf("old$index", "legacy", category))
                }
                db.execSQL("INSERT INTO trip_records VALUES ('old-trip','vehicle',1,2,1,2.5,80,3000,45,0,0,0,0,0,85,80,1)")
                db.version = 1
            }
            CarObdDatabaseHelper(context, name).use { helper ->
                val history = helper.diagnosticHistory().single()
                assertTrue(history.legacy); assertNull(history.mil); assertNull(history.protocol)
                assertEquals(3, history.codes.size)
                assertTrue(history.codes.all { it.code == "P0133" && it.legacy && it.category == null && it.mil == null && it.protocol == null && it.responder == null })
                val trip = helper.getTripsForVehicle("vehicle").single()
                assertTrue(trip.legacyUnverified)
                assertEquals(2.5, trip.distanceKm!!, 0.0)
                assertTrue(trip.evidence.all { it.provenance == MetricProvenance.LEGACY_UNVERIFIED })
            }
            CarObdDatabaseHelper(context, name).use { helper ->
                assertEquals(4, helper.readableDatabase.version)
                assertEquals(3, helper.diagnosticHistory().single().codes.size)
            }
        } finally { context.deleteDatabase(name) }
    }
}
