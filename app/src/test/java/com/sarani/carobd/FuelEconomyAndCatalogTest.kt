package com.sarani.carobd

import com.sarani.carobd.core.calculator.FuelEconomyEngine
import com.sarani.carobd.core.database.SensorCatalog
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.SensorCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FuelEconomyAndCatalogTest {

    @Test
    fun testSensorCatalogHasExtensivePidsAcrossCategories() {
        val sensors = SensorCatalog.ALL_SENSORS
        assertTrue("Catalog should contain a large number of sensors", sensors.size >= 40)

        val iranianSensors = sensors.filter { it.category == SensorCategory.IRANIAN_ECU }
        assertTrue("Should contain Iranian ECU sensors", iranianSensors.isNotEmpty())
        assertNotNull(SensorCatalog.findById("stepper_steps"))
        assertNotNull(SensorCatalog.findById("cng_tank_press"))
        assertNotNull(SensorCatalog.findById("o2_b1s1"))

        val motorcycleSensors = sensors.filter { it.category == SensorCategory.MOTORCYCLE }
        assertTrue("Should contain motorcycle & Chinese sensors", motorcycleSensors.isNotEmpty())
    }

    @Test
    fun testFuelEconomyEngineCalculatesRangeAndCost() {
        val telemetry = LiveTelemetry(
            speedKmh = 100,
            rpm = 2800,
            mafGramsPerSec = 22.0f,
            fuelLevelPercent = 60f,
            intakeManifoldPressureKpa = 65, measuredPids = setOf("10", "0D", "2F"), timestamp = 1000
        )

        val metrics = FuelEconomyEngine.calculate(
            telemetry = telemetry,
            tankCapacityLiters = 50f,
            fuelPricePerLiterTomans = 3000f,
            averageTripLitersPer100km = 7.2f, nowMs = 1000
        )

        assertTrue("Instant consumption should be positive", requireNotNull(metrics.instantLitersPer100Km) > 0f)
        assertTrue("Km per liter should be positive", requireNotNull(metrics.instantKmPerLiter) > 0f)
        assertTrue("Estimated range should be realistic (around 400km)", requireNotNull(metrics.remainingRangeKm) in 250..650)
        assertTrue("Cost per km should be positive", requireNotNull(metrics.fuelCostPerKmTomans) > 0f)
    }

    @Test
    fun testFuelEconomyUnavailableWhenMafUnmeasured() {
        // §8: no measured MAF -> FuelEconomyEngine must NOT fabricate fuel
        // economy from assumed displacement/VE/IAT. Banner renders "—".
        val telemetry = LiveTelemetry(speedKmh = 100, rpm = 2800, fuelLevelPercent = 60f)
        val metrics = FuelEconomyEngine.calculate(telemetry = telemetry, tankCapacityLiters = 50f)
        org.junit.Assert.assertNull(metrics.instantLitersPer100Km)
        org.junit.Assert.assertNull(metrics.instantKmPerLiter)
    }
    @Test fun stalePositiveMafAndAbsentAverageCannotFabricateFuelOrRange() {
        val t = LiveTelemetry(speedKmh = 100, mafGramsPerSec = 22f, fuelLevelPercent = 60f,
            measuredPids = setOf("10", "0D", "2F"), timestamp = 1000)
        val fresh = FuelEconomyEngine.calculate(t, tankCapacityLiters = 50f, fuelPricePerLiterTomans = 3000f, nowMs = 1000)
        assertTrue(requireNotNull(fresh.instantLitersPer100Km) > 0)
        org.junit.Assert.assertNull(fresh.remainingRangeKm)
        org.junit.Assert.assertNull(fresh.fuelCostPerKmTomans)
        val stale = FuelEconomyEngine.calculate(t, nowMs = 20_000)
        org.junit.Assert.assertNull(stale.instantLitersPer100Km)
        org.junit.Assert.assertNull(stale.source)
        val unknown = FuelEconomyEngine.calculate(t.copy(measuredPids = emptySet()), nowMs = 1000)
        org.junit.Assert.assertNull(unknown.instantLitersPerHour)
    }
}
