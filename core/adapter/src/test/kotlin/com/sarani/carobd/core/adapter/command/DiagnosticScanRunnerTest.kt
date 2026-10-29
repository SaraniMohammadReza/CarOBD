package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DiagnosticScanRunnerTest {
    private class Adapter(val can: Boolean, val lines: Map<String, List<String>>) : ObdAdapter {
        override val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val currentProtocol: ProtocolStandard? = null
        override val batteryVoltage = 0f
        override val isCanTransport = can
        val requests = mutableListOf<String>()
        override suspend fun initialize(forcedProtocol: ProtocolStandard?) = error("unused")
        override suspend fun executeCommand(command: String, timeoutMs: Long): AdapterResponse {
            requests.add(command)
            val raw = lines[command].orEmpty()
            return AdapterResponse.Success(raw.joinToString("\r"), raw, 1)
        }
        override suspend fun setHeader(headerHex: String) = false
        override suspend fun selectProtocol(protocol: ProtocolStandard) = false
        override suspend fun disconnect() {}
    }
    private fun nonCan() = Adapter(false, mapOf("03" to listOf("43 01 33 00 00 00 00"), "07" to listOf("47 01 33 00 00 00 00"), "0A" to listOf("4A 01 33 00 00 00 00")))
    @Test fun repository_nonCanStoredP0133SurvivesEnvelope() = runTest {
        val r = DiagnosticScanRunner(nonCan()).scan()
        assertEquals(ScanCompleteness.COMPLETE, r.completeness)
        assertEquals("P0133", r.codes.single { it.category == DtcCategory.STORED }.code)
    }
    @Test fun repository_pendingP0133PreservesCategory() = runTest { assertEquals("P0133", DiagnosticScanRunner(nonCan()).scan().codes.single { it.category == DtcCategory.PENDING }.code) }
    @Test fun repository_permanentP0133PreservesCategory() = runTest { assertEquals("P0133", DiagnosticScanRunner(nonCan()).scan().codes.single { it.category == DtcCategory.PERMANENT }.code) }
    @Test fun repository_malformedNeverBecomesNoCodes() = runTest {
        for (raw in listOf("43", "43 02 01", "43 FFFFFFFF", "410C1AF8", "NO DATA")) {
            val r = DiagnosticScanRunner(Adapter(true, mapOf("03" to listOf(raw)))).scan()
            assertNotEquals(raw, ScanCompleteness.COMPLETE, r.completeness)
        }
    }
    @Test fun scan_multiResponderPreservesPositiveAndNegativeEvidence() = runTest {
        val r = DiagnosticScanRunner(Adapter(true, mapOf("03" to listOf("7E8 06 43 02 01 33 03 00", "7E9 43 01 01 71", "7EA 7F 03 12"), "07" to listOf("4700"), "0A" to listOf("4A00")))).scan()
        assertEquals(ScanCompleteness.PARTIAL, r.completeness)
        assertEquals(3, r.codes.size)
        assertTrue(r.responders.any { it.responder == "7EA" && it.outcome == ScanOutcome.NEGATIVE_RESPONSE })
    }
    @Test fun scan_sameDtcDifferentRespondersRemainDistinct() = runTest {
        val r = DiagnosticScanRunner(Adapter(true, mapOf("03" to listOf("7E8 43 01 01 33", "7E9 43 01 01 33"), "07" to listOf("4700"), "0A" to listOf("4A00")))).scan()
        assertEquals(ScanCompleteness.COMPLETE, r.completeness)
        assertEquals(setOf("7E8", "7E9"), r.codes.map { it.responderAddressHex }.toSet())
        assertTrue(r.codes.all { it.code == "P0133" })
    }
    @Test fun scan_validZeroCodesIsComplete() = runTest {
        val r = DiagnosticScanRunner(Adapter(true, mapOf("03" to listOf("4300"), "07" to listOf("4700"), "0A" to listOf("4A00")))).scan()
        assertEquals(ScanCompleteness.COMPLETE, r.completeness)
        assertTrue(r.codes.isEmpty())
    }
}
