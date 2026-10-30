package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test

class MonitorTestDecoderTest {
    private fun read(raw: String) = ElmResponseEnvelopeParser.parse("0601", raw.lines(), 0, 0).map { MonitorTestDecoder.decode(1, it) }
    @Test fun recordsPreserveBitsWithoutInventedScalingOrPassFail() {
        val result = read("460101FE80010000FFFF0102FF000200000064").single()
        assertEquals(ReadingState.Success, result.state)
        val records = result.value!!
        assertEquals(2, records.size)
        assertEquals(32769, records[0].rawMeasured)
        assertEquals(65535, records[0].rawMaximum)
        assertEquals(0xFE, records[0].unitScalingId)
        assertTrue(records.all { it.measured == null && it.minimum == null && it.maximum == null && it.passed == null && it.unit == null && it.componentId == null })
    }
    @Test fun malformedAndUnrelatedRecordsCannotPass() {
        assertEquals(ReadingState.Malformed, read("460101FE80010000").single().state)
        assertEquals(ReadingState.Malformed, read("460201FE80010000FFFF").single().state)
    }
    @Test fun multiResponderNegativeAndNoDataRemainDistinct() {
        val results = read("7E8 460101FE80010000FFFF\n7E9 7F 06 12\n7EA NO DATA")
        assertEquals(listOf(ReadingState.Success, ReadingState.NegativeResponse, ReadingState.NoData), results.map { it.state })
        assertEquals(listOf("7E8", "7E9", "7EA"), results.map { it.evidence.single().responderId })
    }
    @Test fun capabilityAndTimeoutRemainExplicit() {
        val supported = ElmResponseEnvelopeParser.parse("0600", listOf("460080000000"), 0, 0).single()
        assertEquals(setOf(1), MonitorTestDecoder.supported(0, supported))
        assertEquals(ReadingState.Timeout, MonitorTestDecoder.decode(1, supported.copy(result = DiagnosticResult.Timeout)).state)
    }
}
