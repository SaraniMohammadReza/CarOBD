package com.sarani.carobd

import com.sarani.carobd.core.database.*
import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test

class DashboardSensorAvailabilityTest {
    private fun availability(id: String, telemetry: LiveTelemetry) = dashboardSensorAvailability(SensorCatalog.findById(id)!!, telemetry)
    @Test fun pickerRequiresCapabilityMapAndImplementedPollPath() {
        val known = LiveTelemetry(pidSupport = PidSupportState.Known(PidCapabilityMap(setOf("0C", "0E"))))
        assertEquals(DashboardSensorAvailability.AVAILABLE, availability("rpm", known))
        assertEquals(DashboardSensorAvailability.UNSUPPORTED, availability("speed", known))
        assertEquals(DashboardSensorAvailability.NOT_POLLED, availability("timing_advance", known))
        assertEquals(DashboardSensorAvailability.NOT_POLLED, availability("stepper_steps", known))
        assertNull(SensorCatalog.findById("not-a-sensor"))
    }
    @Test fun discoveryFailureAndFreshSessionDoNotGrantSupportFromAValue() {
        val measured = LiveTelemetry(rpm = 1000, measuredPids = setOf("0C"))
        assertEquals(DashboardSensorAvailability.UNKNOWN, availability("rpm", measured))
        assertEquals(DashboardSensorAvailability.UNKNOWN, availability("rpm", measured.copy(pidSupport = PidSupportState.Unknown("timeout"))))
        assertEquals(DashboardSensorAvailability.UNKNOWN, availability("elm_pin16_volt", LiveTelemetry()))
        val observed = LiveTelemetry(batteryVoltage = 12.4f, measurementTimesMs = mapOf("ATRV" to 100))
        assertEquals(DashboardSensorAvailability.AVAILABLE, availability("elm_pin16_volt", observed))
    }
}
