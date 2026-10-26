package com.sarani.carobd

import com.sarani.carobd.core.connectivity.VirtualCarSimulator
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.protocol.FrameParser
import com.sarani.carobd.core.protocol.StandardPids
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualCarSimulatorTest {

    @Test
    fun testSimulatorHandshakeAndRpmQuery() = runBlocking {
        val simulator = VirtualCarSimulator()
        val connectResult = simulator.connect("SIMULATOR")

        assertTrue(connectResult.isSuccess)
        assertTrue(simulator.isConnected())
        assertTrue(simulator.connectionState.value is ConnectionState.Connected)

        // Query ATZ
        val atz = simulator.sendCommand("ATZ").getOrNull()
        assertNotNull(atz)
        assertTrue(atz!!.contains("ELM327"))

        // Query Engine RPM PID 010C
        val rpmRaw = simulator.sendCommand("010C").getOrNull()
        assertNotNull(rpmRaw)
        val bytes = FrameParser.extractHexBytes(rpmRaw!!)
        val rpm = StandardPids.parseRpm(bytes)
        assertNotNull(rpm)
        assertTrue("RPM should be within realistic idle/driving range", rpm!! in 800..6500)

        // Query DTCs
        val dtcRaw = simulator.sendCommand("03").getOrNull()
        assertNotNull(dtcRaw)
        val dtcs = FrameParser.parseDtcResponse(dtcRaw!!)
        assertTrue("Simulator should return initial fault codes", dtcs.isNotEmpty())

        // Clear DTCs
        val clearRaw = simulator.sendCommand("04").getOrNull()
        assertNotNull(clearRaw)

        // Re-query DTCs (should be empty now)
        val dtcAfterClear = simulator.sendCommand("03").getOrNull()
        val dtcsEmpty = FrameParser.parseDtcResponse(dtcAfterClear!!)
        assertTrue("DTCs should be cleared", dtcsEmpty.isEmpty())

        simulator.disconnect()
        assertTrue(!simulator.isConnected())
    }
}
