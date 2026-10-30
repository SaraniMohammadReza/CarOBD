package com.sarani.carobd.core.telemetry

import com.sarani.carobd.core.telemetry.buffer.BoundedRingBuffer
import com.sarani.carobd.core.telemetry.fuel.FuelEconomyCalculator
import com.sarani.carobd.core.telemetry.fuel.FuelUnit
import com.sarani.carobd.core.telemetry.scheduler.PollingTier
import com.sarani.carobd.core.telemetry.scheduler.TelemetryScheduler
import com.sarani.carobd.core.telemetry.trip.TripComputer
import org.junit.Assert.*
import org.junit.Test

class TelemetryUnitTest {

    @Test
    fun testBoundedRingBuffer() {
        val buffer = BoundedRingBuffer(capacity = 5)
        assertEquals(0, buffer.size)

        buffer.add(10f)
        buffer.add(20f)
        buffer.add(30f)
        assertEquals(3, buffer.size)
        assertEquals(30f, buffer.latest())

        buffer.add(40f)
        buffer.add(50f)
        assertEquals(5, buffer.size)
        assertTrue(buffer.isFull)

        // Adding 6th element drops the oldest (10f)
        buffer.add(60f)
        assertEquals(5, buffer.size)
        assertEquals(60f, buffer.latest())

        val list = buffer.toList()
        assertEquals(listOf(20f, 30f, 40f, 50f, 60f), list)

        val stats = buffer.getStatistics()
        assertEquals(20f, stats.min, 0.01f)
        assertEquals(60f, stats.max, 0.01f)
        assertEquals(40f, stats.average, 0.01f)
        assertEquals(40f, stats.delta, 0.01f) // 60 - 20
    }

    @Test
    fun testFuelEconomyMaf() {
        // MAF = 3.5 g/s at idle
        val idleFuelLitersPerHour = FuelEconomyCalculator.calculateFuelRateFromMaf(mafGramsPerSec = 3.5)
        // Fuel = 3.5 / 14.7 = 0.238 g/s -> * 3600 = 857.14 g/h -> / 745 = 1.15 L/h
        assertEquals(1.15, idleFuelLitersPerHour, 0.05)

        // Test instantaneous display when idling
        val idleEconomy = FuelEconomyCalculator.calculateInstantaneousEconomy(idleFuelLitersPerHour, speedKmh = 0.0)
        assertEquals(FuelUnit.LITERS_PER_HOUR, idleEconomy.unit)
        assertTrue(idleEconomy.isIdling)
        assertEquals(1.15, idleEconomy.value, 0.05)

        // Test instantaneous display when cruising at 100 km/h with 7.0 L/h consumption
        val cruiseEconomy = FuelEconomyCalculator.calculateInstantaneousEconomy(fuelRateLitersPerHour = 7.0, speedKmh = 100.0)
        assertEquals(FuelUnit.LITERS_PER_100KM, cruiseEconomy.unit)
        assertFalse(cruiseEconomy.isIdling)
        assertEquals(7.0, cruiseEconomy.value, 0.05)
    }

    @Test
    fun testFuelEconomySpeedDensityTu5() {
        // TU5 1.587L at idle: MAP 35 kPa, IAT 30°C, RPM 850, VE 0.75
        val fuelRateIdle = FuelEconomyCalculator.calculateFuelRateFromSpeedDensity(
            mapKpa = 35.0,
            iatCelsius = 30.0,
            rpm = 850.0,
            displacementLiters = 1.587,
            volumetricEfficiency = 0.75
        )

        // Real TU5 idle fuel consumption is typically 0.8 to 1.1 L/h
        assertTrue("Idle fuel rate should be between 0.6 and 1.3 L/h", fuelRateIdle in 0.6..1.3)

        // TU5 cruising at 100 km/h: MAP 60 kPa, IAT 35°C, RPM 3000, VE 0.82
        val fuelRateCruise = FuelEconomyCalculator.calculateFuelRateFromSpeedDensity(
            mapKpa = 60.0,
            iatCelsius = 35.0,
            rpm = 3000.0,
            displacementLiters = 1.587,
            volumetricEfficiency = 0.82
        )
        val lPer100km = (fuelRateCruise / 100.0) * 100.0
        // Real TU5 highway cruise at 100 km/h is typically 6.0 to 7.5 L/100km
        assertTrue("Cruise fuel rate should be between 5.5 and 8.0 L/100km", lPer100km in 5.5..8.0)
    }

    @Test
    fun testTripComputerIntegration() {
        val trip = TripComputer("trip_001", "ikco_206_tu5", startTimeMs = 1000000L)

        // Initial state at t = 1000000ms: speed 0
        trip.processSample(speedKmh = 0, rpm = 850, fuelRateLitersPerHour = 1.0, timestampMs = 1000000L)

        // 10 seconds later: accelerated to 60 km/h (speed went 0 -> 60 in 10s -> avg speed 30 km/h)
        // Distance = 30 * (10 / 3600) = 0.0833 km
        trip.processSample(speedKmh = 60, rpm = 2500, fuelRateLitersPerHour = 5.0, timestampMs = 1000000L + 10000L)

        val record = trip.getTripRecord(nowMs = 1000000L + 10000L)
        assertEquals(60, record.maxSpeedKmh)
        assertEquals(2500, record.maxRpm)
        assertTrue(requireNotNull(record.distanceKm) > 0.07 && requireNotNull(record.distanceKm) < 0.10)
        assertTrue(requireNotNull(record.estimatedFuelUsedLiters) > 0.0)
    }

    @Test
    fun testTelemetrySchedulerPacing() {
        val scheduler = TelemetryScheduler()

        // Verify tier cycles
        assertTrue(scheduler.shouldPoll(PollingTier.FAST)) // cycle 0
        assertTrue(scheduler.shouldPoll(PollingTier.MEDIUM))
        assertTrue(scheduler.shouldPoll(PollingTier.SLOW))

        scheduler.advanceCycle() // cycle 1
        assertTrue(scheduler.shouldPoll(PollingTier.FAST))
        assertFalse(scheduler.shouldPoll(PollingTier.MEDIUM))
        assertFalse(scheduler.shouldPoll(PollingTier.SLOW))

        // Latency adaptation
        scheduler.recordRtt(20L)
        scheduler.recordRtt(15L)
        scheduler.recordRtt(10L)
        assertEquals(10L, scheduler.recommendedPacingDelayMs)

        scheduler.recordRtt(180L)
        scheduler.recordRtt(250L)
        assertTrue(scheduler.recommendedPacingDelayMs >= 25L)

        // State update
        scheduler.updateMetric("010C", 2200.0) // RPM
        assertEquals(2200, scheduler.liveTelemetry.value.rpm)

        scheduler.updateMetric("010D", 75.0) // Speed
        assertEquals(75, scheduler.liveTelemetry.value.speedKmh)
    }

    @Test
    fun testFuelRateTruthfulnessGate() {
        // §8: MAF path — a measured MAF needs no vehicle metadata.
        val fromMaf = FuelEconomyCalculator.fuelRateIfMeasurable(mafGramsPerSec = 3.5)
        assertNotNull(fromMaf)
        assertEquals(1.15, fromMaf!!, 0.05) // 3.5 g/s -> ~1.15 L/h (matches testFuelEconomyMaf)

        // §8: MAF unmeasured -> null (no fabricated displacement/VE/IAT defaults).
        assertNull(FuelEconomyCalculator.fuelRateIfMeasurable(mafGramsPerSec = null))

        // §8: speed-density gate rejects partial/invented inputs:
        assertNull(FuelEconomyCalculator.fuelRateIfMeasurable(
            rpm = 2800.0, mapKpa = 65.0, iatCelsius = 30.0,
            displacementLiters = null, volumetricEfficiency = 0.80)) // unknown engine
        assertNull(FuelEconomyCalculator.fuelRateIfMeasurable(
            rpm = 2800.0, mapKpa = 65.0, iatCelsius = 30.0,
            displacementLiters = 1.587, volumetricEfficiency = null)) // unknown VE
        assertNull(FuelEconomyCalculator.fuelRateIfMeasurable(
            rpm = 2800.0, mapKpa = 65.0, iatCelsius = null,
            displacementLiters = 1.587, volumetricEfficiency = 0.80)) // unmeasured IAT
        assertNull(FuelEconomyCalculator.fuelRateIfMeasurable(
            rpm = 0.0, mapKpa = 65.0, iatCelsius = 30.0,
            displacementLiters = 1.587, volumetricEfficiency = 0.80)) // engine not running

        // All SD inputs genuinely known -> a real, non-fabricated rate.
        val fromSd = FuelEconomyCalculator.fuelRateIfMeasurable(
            rpm = 2800.0, mapKpa = 65.0, iatCelsius = 30.0,
            displacementLiters = 1.587, volumetricEfficiency = 0.75)
        assertNotNull(fromSd)
        assertTrue(fromSd!! > 0.0)
    }
}
