package com.sarani.carobd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.telemetry.trip.TripComputer
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Prepared for a device: compiling this class does not constitute Android SQLite execution. */
@RunWith(AndroidJUnit4::class)
class TripEvidenceInstrumentedTest {
    @Test fun processRecreation_preservesTripProvenanceAndNullFuel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "trip-evidence-${java.util.UUID.randomUUID()}.db"
        try {
            val computer = TripComputer("new", "vehicle", 1000)
            computer.processSample(36, null, null, 1000)
            computer.processSample(36, 1000, null, 2000)
            val original = computer.getTripRecord(3000)
            CarObdDatabaseHelper(context, name).use { it.insertTripRecord(original) }
            CarObdDatabaseHelper(context, name).use { helper ->
                val restored = helper.getTripsForVehicle("vehicle").single()
                assertEquals(original, restored)
                assertNull(restored.estimatedFuelUsedLiters)
                assertEquals(1000L, restored.evidence.single { it.metric == TripMetric.DISTANCE }.coveredMs)
            }
        } finally { context.deleteDatabase(name) }
    }
    @Test fun versionTwoTrip_remainsUnverifiedAfterUpgradeAndRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "legacy-trip-${java.util.UUID.randomUUID()}.db"
        try {
            context.openOrCreateDatabase(name, 0, null).use { db ->
                db.execSQL("CREATE TABLE dtc_records(id TEXT, session_id TEXT)")
                db.execSQL("CREATE TABLE trip_records(id TEXT PRIMARY KEY, vehicle_id TEXT, start_time INTEGER, end_time INTEGER, duration_sec INTEGER, distance_km REAL, max_speed INTEGER, max_rpm INTEGER, avg_speed REAL, fuel_liters REAL, hard_accel INTEGER, hard_brake INTEGER, high_rpm_sec INTEGER, idle_sec INTEGER, smoothness INTEGER, eco_score INTEGER, sample_count INTEGER)")
                db.execSQL("INSERT INTO trip_records VALUES('old','vehicle',1,2,1,2.5,80,3000,45,0,0,0,0,0,85,80,1)")
                db.version = 2
            }
            CarObdDatabaseHelper(context, name).use { helper ->
                val trip = helper.getTripsForVehicle("vehicle").single()
                assertTrue(trip.legacyUnverified)
                assertEquals(2.5, trip.distanceKm!!, 0.0)
                assertTrue(trip.evidence.all { it.provenance == MetricProvenance.LEGACY_UNVERIFIED && it.source == null && it.coveredMs == 0L })
                assertEquals(0.0, trip.estimatedFuelUsedLiters!!, 0.0) // Retained historical number, never newly verified.
            }
        } finally { context.deleteDatabase(name) }
    }
}
