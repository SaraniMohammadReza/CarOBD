package com.sarani.carobd

import com.sarani.carobd.core.database.IranianCarSpecsDatabase
import com.sarani.carobd.core.logger.TripLogger
import com.sarani.carobd.core.model.LiveTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripAndSpecsUnitTest {

    @Test
    fun testIranianCarSpecsTu5AndEf7() {
        val tu5Specs = IranianCarSpecsDatabase.getSpecSheet("ikco_206_tu5")
        assertNotNull(tu5Specs)
        assertEquals(3.25f, tu5Specs.engineOilCapacityLiters, 0.01f)
        assertTrue(tu5Specs.engineOilGrade.contains("10W-40"))
        assertEquals(60000, tu5Specs.timingBeltIntervalKm)
        assertEquals(134, tu5Specs.timingBeltTeeth)

        val ef7Specs = IranianCarSpecsDatabase.getSpecSheet("ikco_dena_ef7")
        assertNotNull(ef7Specs)
        assertEquals(5.5f, ef7Specs.engineOilCapacityLiters, 0.01f)
        assertEquals(127, ef7Specs.timingBeltTeeth)
    }

    @Test
    fun testTripLoggerRecordsSamplesAndCalculatesDistance() {
        val logger = TripLogger()
        logger.startTrip(1000)

        assertTrue(logger.tripState.value.isRecording)

        // Log sample driving at 90 km/h
        logger.logSample(LiveTelemetry(speedKmh = 90, rpm = 2500, coolantTempCelsius = 89, measuredPids = setOf("0C", "0D", "05"), timestamp = 1000), nowMs = 1000)
        logger.logSample(LiveTelemetry(speedKmh = 120, rpm = 3400, throttlePositionPercent = 85f, measuredPids = setOf("0C", "0D", "11"), timestamp = 2000), nowMs = 2000)

        val summary = logger.tripState.value
        assertEquals(120, summary.maxSpeedKmh)
        assertEquals(3400, summary.maxRpm)
        assertEquals(105.0 / 3600.0, summary.distanceKm, 0.000001)
        org.junit.Assert.assertNull(summary.estimatedFuelUsedLiters)

        val csv = logger.exportToCsv()
        assertTrue("CSV should contain header", csv.contains("Timestamp,RPM,Speed_kmh"))
        assertTrue("CSV should contain speed 120", csv.contains("120"))

        logger.stopTrip()
        assertTrue(!logger.tripState.value.isRecording)
    }
}
