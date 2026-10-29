package com.sarani.carobd.core.model
import org.junit.Assert.*
import org.junit.Test
class BleReadinessTest {
    @Test fun linkAndDescriptorSubmissionAreNotReady() {
        val gate = BleReadiness()
        assertTrue(gate.linkConnected()); assertFalse(gate.isReady)
        assertTrue(gate.beginSubscription()); assertFalse(gate.isReady)
        assertTrue(gate.acknowledgeSubscription(true)); assertTrue(gate.isReady)
    }
    @Test fun failedOrUnrequestedCccdCannotBecomeReady() {
        val gate = BleReadiness(); gate.linkConnected()
        assertFalse(gate.acknowledgeSubscription(true))
        gate.beginSubscription(); assertFalse(gate.acknowledgeSubscription(false))
    }
    @Test fun disconnectAndLateCallbacksCannotReviveGeneration() {
        val gate = BleReadiness(); gate.linkConnected(); gate.beginSubscription()
        gate.disconnect()
        assertFalse(gate.acknowledgeSubscription(true)); assertFalse(gate.linkConnected())
        assertFalse(gate.beginSubscription()); assertFalse(gate.isReady)
    }
}
