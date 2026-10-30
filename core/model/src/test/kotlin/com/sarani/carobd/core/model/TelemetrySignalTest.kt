package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class TelemetrySignalTest {
    @Test fun adapterVoltageDoesNotBecomePid0142() {
        val t = LiveTelemetry(batteryVoltage = 12.4f, measuredPids = setOf("ATRV"), timestamp = 100)
        assertEquals(12.4f, t.signal("ATRV", 100).value!!, 0f)
        assertNull(t.signal("0142", 100).value)
    }
    @Test fun failedPidCannotReceiveAnotherPidsFreshTimestamp() {
        val t = LiveTelemetry(rpm = 1000, speedKmh = 10, measuredPids = setOf("0D"), measurementTimesMs = mapOf("0C" to 100, "0D" to 300), timestamp = 300)
        assertEquals(SignalQuality.STALE, t.signal("010C", 300).quality)
        assertEquals(SignalQuality.GOOD, t.signal("010D", 300).quality)
        assertEquals(SignalQuality.STALE, t.signal("010D", 20_000).quality)
        assertNull(t.signal("0105", 300).value)
    }
    @Test fun actualZeroRemainsMeasuredAndClockRollbackIsStale() {
        val t = LiveTelemetry(measuredPids = setOf("0D"), measurementTimesMs = mapOf("0D" to 100))
        assertEquals(0f, t.signal("010D", 100).value!!, 0f)
        assertEquals(SignalQuality.GOOD, t.signal("010D", 100).quality)
        assertEquals(SignalQuality.STALE, t.signal("010D", 50).quality)
    }
}
