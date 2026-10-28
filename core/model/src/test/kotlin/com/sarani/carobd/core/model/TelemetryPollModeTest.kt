package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryPollModeTest {

    @Test
    fun storedRoundTripByName() {
        for (mode in TelemetryPollMode.values()) {
            assertEquals(mode, TelemetryPollMode.fromStored(mode.name))
        }
    }

    @Test
    fun unknownNameDegradesToNormal() {
        assertEquals(TelemetryPollMode.NORMAL, TelemetryPollMode.fromStored("BOGUS"))
        assertEquals(TelemetryPollMode.NORMAL, TelemetryPollMode.fromStored(""))
    }

    @Test
    fun pollDelayOrderedEcoToFast() {
        // §7: ECO slowest, FAST fastest; no millisecond spam.
        assertEquals(true, TelemetryPollMode.ECO.delayMs > TelemetryPollMode.NORMAL.delayMs)
        assertEquals(true, TelemetryPollMode.NORMAL.delayMs > TelemetryPollMode.FAST.delayMs)
        assertEquals(500L, TelemetryPollMode.FAST.delayMs)   // 0.5s, not ms-level spam
        assertEquals(2000L, TelemetryPollMode.ECO.delayMs)
    }

    @Test
    fun clampBoundedByEcoAndFast() {
        val delays = TelemetryPollMode.values().map { it.delayMs }
        assertEquals(true, delays.all { it >= 500L && it <= 2000L })
    }
}
