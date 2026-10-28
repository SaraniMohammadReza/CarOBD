package com.sarani.carobd.core.adapter.profile

import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** §20 staged test-connection state machine tests. */
class ConnectionWizardTest {

    @Test
    fun disconnectedAllStagesPendingWithTransportEvidence() {
        val s = ConnectionWizard.evaluate(TransportType.BLUETOOTH_CLASSIC, "AA:BB", listOf(ConnectionState.Disconnected))
        assertEquals(6, s.size)
        assertTrueAll(s, ConnectionWizard.StageState.PENDING)
        assertEquals("Bluetooth Classic AA:BB", s[0].evidence)
        assertNull(s[1].evidence)
    }

    private fun assertTrueAll(s: List<ConnectionWizard.StageStatus>, st: ConnectionWizard.StageState) {
        s.forEach { assertEquals(it.stage.title, st, it.state) }
    }

    @Test
    fun connectedAllStagesPass() {
        val trace = listOf(
            ConnectionState.TransportConnected("socket"),
            ConnectionState.AdapterResetting,
            ConnectionState.AdapterIdentifying("ATI"),
            ConnectionState.ProtocolDetecting("ISO15765-4 CAN 11/500", 1, 3),
            ConnectionState.EcuDiscovering("ISO15765-4 CAN 11/500"),
            ConnectionState.CapabilityDiscovering(12),
            ConnectionState.Connected("ELM327 v1.5", adapterType = "STN1110", protocolName = "CAN 11/500",
                elmVersion = "1.5", batteryVoltage = 12.6f, responderCount = 2, isSimulator = false)
        )
        val s = ConnectionWizard.evaluate(TransportType.BLUETOOTH_CLASSIC, "AA:BB", trace)
        s.forEach { assertEquals(it.stage.title, ConnectionWizard.StageState.PASS, it.state) }
        // Evidence accumulated from earlier trace entries across stages.
        assertEquals("STN1110", evidenceFor(s, ConnectionWizard.Stage.IDENTITY))
        assertEquals("CAN 11/500", evidenceFor(s, ConnectionWizard.Stage.PROTOCOL))
        assertEquals("41 00", evidenceFor(s, ConnectionWizard.Stage.ECU))
        assertEquals("2 responder(s)", evidenceFor(s, ConnectionWizard.Stage.CAPABILITY))
    }

    @Test
    fun adapterIdentifyingThenProtocolDetectingShowsIdentityPassInProgressProtocol() {
        // Identity evidence (probeStage "ATI") observed; now at ProtocolDetecting.
        val trace = listOf(
            ConnectionState.TransportConnected("socket"),
            ConnectionState.AdapterIdentifying("ATRV"),
            ConnectionState.ProtocolDetecting("KWP fast", 1, 1)
        )
        val s = ConnectionWizard.evaluate(TransportType.BLUETOOTH_LE, "AA:BB:00:01", trace)
        assertEquals(ConnectionWizard.StageState.PASS, s[0].state) // TRANSPORT
        assertEquals(ConnectionWizard.StageState.PASS, s[1].state) // ADAPTER
        assertEquals(ConnectionWizard.StageState.PASS, s[2].state) // IDENTITY (evidence retained)
        assertEquals(ConnectionWizard.StageState.IN_PROGRESS, s[3].state) // PROTOCOL
        assertEquals("ATRV", evidenceFor(s, ConnectionWizard.Stage.IDENTITY))
        assertEquals("KWP fast", evidenceFor(s, ConnectionWizard.Stage.PROTOCOL))
        assertEquals(ConnectionWizard.Stage.PROTOCOL, ConnectionWizard.currentStage(s))
    }

    @Test
    fun failureAtEcuMarksEcuFailOthersPending() {
        val trace = listOf(
            ConnectionState.TransportConnected("socket"),
            ConnectionState.AdapterIdentifying("ATI"),
            ConnectionState.ProtocolDetecting("ISO15765-4 CAN 11/500", 1, 2),
            ConnectionState.Failure(stage = "ECU", humanMessageFa = "ECU silent", technicalMessage = "no 41 00", isRecoverable = true)
        )
        val s = ConnectionWizard.evaluate(TransportType.WIFI_TCP, "192.168.0.5", trace)
        assertEquals(ConnectionWizard.StageState.PASS, s[0].state) // TRANSPORT
        assertEquals(ConnectionWizard.StageState.PASS, s[1].state) // ADAPTER
        assertEquals(ConnectionWizard.StageState.PASS, s[2].state) // IDENTITY
        assertEquals(ConnectionWizard.StageState.PASS, s[3].state) // PROTOCOL
        assertEquals(ConnectionWizard.StageState.FAIL, s[4].state)   // ECU
        assertEquals(ConnectionWizard.StageState.PENDING, s[5].state) // CAPABILITY
    }

    @Test
    fun transportLabelPerType() {
        assertEquals("Simulator", ConnectionWizard.evaluate(TransportType.SIMULATOR, null, listOf(ConnectionState.Disconnected))[0].evidence)
        assertEquals("Wi-Fi 192.168.0.10", ConnectionWizard.evaluate(TransportType.WIFI_TCP, "192.168.0.10", listOf(ConnectionState.Disconnected))[0].evidence)
        assertEquals("USB null", ConnectionWizard.evaluate(TransportType.USB_SERIAL, null, listOf(ConnectionState.Disconnected))[0].evidence)
    }

    private fun evidenceFor(s: List<ConnectionWizard.StageStatus>, stage: ConnectionWizard.Stage): String? =
        s.first { it.stage == stage }.evidence
}
