package com.sarani.carobd.data.transport

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the deterministic §39 simulator scenarios respond per their contract
 * (and never masquerade as a physical ECU). The healthy path preserves the
 * original synthetic responses, including the corrected MAF-bit 0100 bitmap.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class IsolatedSimulatorTransportTest {

    private suspend fun IsolatedSimulatorTransport.collectResponse(
        scope: TestScope,
        command: String
    ): String {
        val collected = mutableListOf<ByteArray>()
        // Attach the collector first so it is suspended at collect() awaiting a
        // frame; write()'s tryEmit then delivers without suspending, and a second
        // advanceUntilIdle runs the collector body. SharedFlow(replay=0) does not
        // retain frames for collectors that attach later.
        val job = scope.launch { incomingBytes.collect { collected.add(it) } }
        scope.advanceUntilIdle()
        write(command.toByteArray(Charsets.US_ASCII))
        scope.advanceUntilIdle()
        job.cancel()
        return collected.joinToString("") { String(it, Charsets.US_ASCII) }
    }

    @Test
    fun healthySimulatesPid00Bitmap() = runTest {
        val t = IsolatedSimulatorTransport()
        t.connect()
        val out = t.collectResponse(this, "0100")
        assertTrue("out=[$out]", out.contains("41 00 BE 3F B8 11"))
    }

    @Test
    fun noDataScenarioReturnsNoData() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.NO_DATA)
        t.connect()
        assertTrue(t.collectResponse(this, "0100").contains("NO DATA"))
    }

    @Test
    fun busInitErrorScenario() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.BUS_INIT_ERROR)
        t.connect()
        assertTrue(t.collectResponse(this, "0100").contains("BUS INIT: ERROR"))
    }

    @Test
    fun canErrorScenario() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.CAN_ERROR)
        t.connect()
        assertTrue(t.collectResponse(this, "0100").contains("CAN ERROR"))
    }

    @Test
    fun negativeResponseScenario() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.NEGATIVE_RESPONSE)
        t.connect()
        // 7F <svc> <NRC>: a real ECU rejection, distinct from NO DATA / not-connected.
        assertTrue(t.collectResponse(this, "010C").contains("7F 01 12"))
    }

    @Test
    fun storedDtcScenario() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.STORED_DTC)
        t.connect()
        assertTrue(t.collectResponse(this, "03").contains("43 02 01 33 03 00"))
    }

    @Test
    fun pendingDtcScenario() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.PENDING_DTC)
        t.connect()
        assertTrue(t.collectResponse(this, "07").contains("47 01 01 15"))
    }

    @Test
    fun engineOffScenarioEmitsNothing() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.ENGINE_OFF)
        t.connect()
        assertEquals("", t.collectResponse(this, "0100"))
    }

    @Test
    fun timeoutScenarioEmitsNothing() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.TIMEOUT)
        t.connect()
        assertEquals("", t.collectResponse(this, "0100"))
    }

    @Test
    fun latePromptStillDeliversResponse() = runTest {
        val t = IsolatedSimulatorTransport(SimulatorScenario.LATE_PROMPT)
        t.connect()
        assertTrue(t.collectResponse(this, "0100").contains("41 00 BE 3F B8 11"))
    }
}
