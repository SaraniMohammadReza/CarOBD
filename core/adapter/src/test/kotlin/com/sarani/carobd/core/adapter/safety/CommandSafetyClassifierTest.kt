package com.sarani.carobd.core.adapter.safety

import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CommandSafetyClassifierTest {
    private val classifier = CommandSafetyClassifier()
    @Test fun dispatch_rejectsEmbeddedCrLfAndControls() {
        for (c in listOf("ATI\r04", "0100\r04", "ATI\r\n04", "0100\r\r04", "ATI\n04", "ATI\u000004", "ATI\t04", "ATI;04", "09 02\r08", "ATI\r27 01", "ATI\r2E F1 90 01", "ATI\r31 01", "\rATI", "AT\u00a0I"))
            assertFalse(c, classifier.isSafeForTerminal(c))
    }
    @Test fun dispatch_rejectsOversizedCommand() { assertFalse(classifier.isSafeForTerminal("01" + "00".repeat(10000))) }
    @Test fun dispatch_deniesUnknownService() {
        for (c in listOf("99", "220101", "AT@3text", "AT S H 7E0", "ATZ", "ATE0", "ATH1", "ATS0", "ATSP6", "STPX h:7E0,d:04"))
            assertFalse(c, classifier.isSafeForTerminal(c))
    }
    @Test fun canonicalReadAllowlist() {
        for (c in listOf("ATI", "ati", "ATRV", "ATDP", "AT DPN", "0100", "09 02", "03", "07", "0A")) assertTrue(c, classifier.isSafeForTerminal(c))
        assertEquals("0902", SingleCommandParser.canonicalize("09 02"))
        assertEquals("220101", SingleCommandParser.canonicalize("220101")) // grammatical, not authorized
    }
    private class Physical : ObdTransport {
        override val transportType = TransportType.WIFI_TCP
        override var isConnected = true
        override val incomingBytes = emptyFlow<ByteArray>()
        val writes = mutableListOf<ByteArray>()
        override suspend fun connect() { isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) { writes.add(data) }
    }
    private fun assertDenied(vararg commands: String) = runTest {
        val transport = Physical()
        val adapter = Elm327Adapter(transport, this)
        for (c in commands) adapter.executeCommand(c)
        adapter.clearSimulatorDiagnosticState()
        assertTrue(transport.writes.isEmpty())
        adapter.disconnect()
    }
    @Test fun physicalDispatch_deniesMode04() = assertDenied("04")
    @Test fun physicalDispatch_deniesMode08() = assertDenied("08", "0800")
    @Test fun physicalDispatch_deniesUdsWritesProgrammingSecurity() = assertDenied("1002", "11", "14FFFFFF", "2701", "2EF19001", "2F0100", "3101", "3400", "3500", "3601", "37", "3B0100", "8501")
    @Test fun rawTerminal_cannotInjectSecondCommand() = assertDenied("ATI\r04", "0902\r08", "0100\r2701")
}
