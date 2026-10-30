package com.sarani.carobd.service

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.storage.UnitSystem
import org.junit.Assert.*
import org.junit.Test

class NotificationMeasurementsTest {
    private val sample = LiveTelemetry(speedKmh = 100, rpm = 2300, coolantTempCelsius = 100, batteryVoltage = 13.7f,
        measuredPids = setOf("0D", "0C", "05", "ATRV"), timestamp = 1000)
    @Test fun metricAndImperialUseTheSameCanonicalSnapshot() {
        val metric = NotificationMeasurements.from(sample, UnitSystem.METRIC, 1000)
        val imperial = NotificationMeasurements.from(sample, UnitSystem.IMPERIAL, 1000)
        assertEquals("100.0 km/h", metric.speed); assertEquals("100.0 °C", metric.coolant)
        assertEquals("62.1 mph", imperial.speed); assertEquals("212.0 °F", imperial.coolant)
        assertEquals("2300", imperial.rpm); assertEquals("13.7", imperial.voltage)
        assertEquals(100, sample.speedKmh)
    }
    @Test fun measuredZeroAndNegativeTemperatureRemainValues() {
        val value = NotificationMeasurements.from(sample.copy(speedKmh = 0, rpm = 0, coolantTempCelsius = -40), UnitSystem.IMPERIAL, 1000)
        assertEquals("0.0 mph", value.speed); assertEquals("0", value.rpm); assertEquals("-40.0 °F", value.coolant)
    }
    @Test fun missingOrStaleOrClockRollbackNeverFabricatesFreshMeasurements() {
        for (value in listOf(NotificationMeasurements.from(LiveTelemetry(), UnitSystem.METRIC, 1000),
            NotificationMeasurements.from(sample, UnitSystem.METRIC, 11001),
            NotificationMeasurements.from(sample, UnitSystem.METRIC, 999))) {
            assertEquals("— km/h", value.speed); assertEquals("—", value.rpm)
            assertEquals("— °C", value.coolant); assertEquals("—", value.voltage)
        }
    }
    @Test fun perPidFreshnessAndNonFiniteVoltageAreHonoured() {
        val value = NotificationMeasurements.from(sample.copy(batteryVoltage = Float.NaN,
            measurementTimesMs = mapOf("0D" to 0)), UnitSystem.IMPERIAL, 11000)
        assertEquals("— mph", value.speed); assertEquals("2300", value.rpm); assertEquals("—", value.voltage)
    }
}
