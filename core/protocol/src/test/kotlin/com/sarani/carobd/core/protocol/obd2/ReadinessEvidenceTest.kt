package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ReadinessEvidenceTest {
    private fun read(raw: String) = EvidenceDecoders.readiness(ElmResponseEnvelopeParser.parse("0101", listOf(raw), 0, 0)).single()
    @Test fun validSparkReadiness() {
        val r = read("41 01 83 07 65 00")
        assertEquals(ReadingState.Success, r.state)
        assertEquals(IgnitionType.Spark, r.value!!.ignitionType)
        assertTrue(r.value!!.milActive)
        assertEquals(3, r.value!!.dtcCount)
        assertTrue(r.value!!.monitors.any { !it.isSupported })
    }
    @Test fun validCompressionReadiness() {
        val r = read("41 01 00 0F 6B 20")
        assertEquals(IgnitionType.Compression, r.value!!.ignitionType)
        assertTrue(r.value!!.monitors.any { it.id == "EXHAUST_GAS" && !it.isComplete })
    }
    @Test fun shortPayload_isMalformed() {
        for (raw in listOf("41 01", "41 01 00", "41 01 00 00 00", "41 01 00 00 00 00 00")) {
            val r = read(raw)
            assertEquals(raw, ReadingState.Malformed, r.state)
            assertNull("Malformed must never render all healthy", r.value)
        }
    }
    @Test fun emptyPayload_isMalformedOrUnavailable() { assertEquals(ReadingState.Malformed, read("").state) }
    @Test fun wrongSid_isMalformed() { assertEquals(ReadingState.Malformed, read("49 01 00 00 00 00").state) }
    @Test fun negativeResponse_preserved() { val r = read("7F 01 12"); assertEquals(ReadingState.NegativeResponse, r.state); assertEquals("12", r.detail) }
    @Test fun noData_preserved() { assertEquals(ReadingState.NoData, read("NO DATA").state) }
    @Test fun timeout_preserved() {
        val env = ElmResponseEnvelopeParser.parse("0101", listOf("NO DATA"), 0, 0).single().copy(result = DiagnosticResult.Timeout)
        assertEquals(ReadingState.Timeout, EvidenceDecoders.readiness(env).state)
    }
}
