package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.DiagnosticResult
import com.sarani.carobd.core.model.DiagnosticService
import com.sarani.carobd.core.model.IsoTpFraming
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §10 responder-aware wire envelope tests.
 *
 * Each responder line must survive as its OWN envelope — no merging, no
 * fabricated 7E8 when the wire provides no source header.
 */
class ElmResponseEnvelopeParserTest {

    private val ts = 1_000L
    private val el = 12L

    private fun b(vararg hex: String) = hex.map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun singlePositiveWithoutHeaderHasNullResponder() {
        val r = ElmResponseEnvelopeParser.parse("01 0C", listOf("41 0C 1A F8"), ts, el)
        assertEquals(1, r.size)
        val e = r[0]
        assertNull("§10: never fabricate 7E8 when no source header is present", e.responderId)
        assertEquals(DiagnosticService.MODE_01, e.service)
        assertEquals(IsoTpFraming.RAW, e.framing)
        assertArrayEquals(b("0C", "1A", "F8"), e.payload)
        assertTrue(e.result is DiagnosticResult.Positive)
    }

    @Test
    fun explicit7E8HeaderIsPreserved() {
        val r = ElmResponseEnvelopeParser.parse("01 0C", listOf("7E8 41 0C 1A F8"), ts, el)
        assertEquals(1, r.size)
        val e = r[0]
        assertEquals("7E8", e.responderId)
        assertEquals(IsoTpFraming.SINGLE_FRAME, e.framing)
        assertArrayEquals(b("0C", "1A", "F8"), e.payload)
        assertTrue(e.result is DiagnosticResult.Positive)
        assertEquals("7E8", (e.result as DiagnosticResult.Positive).responderId)
    }

    @Test
    fun twoDistinctPositiveRespondersDoNotMerge() {
        val r = ElmResponseEnvelopeParser.parse("01 0C",
            listOf("7E8 41 0C 1A F8", "7EA 41 0C 00 10"), ts, el)
        assertEquals(2, r.size)
        assertEquals("7E8", r[0].responderId)
        assertEquals("7EA", r[1].responderId)
        assertArrayEquals(b("0C", "1A", "F8"), r[0].payload)
        assertArrayEquals(b("0C", "00", "10"), r[1].payload)
        assertTrue(r.all { it.result is DiagnosticResult.Positive })
    }

    @Test
    fun positiveAndNegativeFromDifferentRespondersBothSurvive() {
        val r = ElmResponseEnvelopeParser.parse("01 03",
            listOf("7E8 43 12 34", "7E9 7F 03 12"), ts, el)
        assertEquals(2, r.size)
        assertEquals("7E8", r[0].responderId)
        assertTrue(r[0].result is DiagnosticResult.Positive)
        assertEquals(DiagnosticService.MODE_03, r[0].service)
        assertArrayEquals(b("12", "34"), r[0].payload)

        assertEquals("7E9", r[1].responderId)
        val neg = r[1].result as DiagnosticResult.NegativeResponse
        assertEquals(DiagnosticService.MODE_03, neg.service)
        assertEquals(0x12, neg.nrc)
    }

    @Test
    fun sameServiceFromTwoRespondersStaysDistinct() {
        val r = ElmResponseEnvelopeParser.parse("01 03",
            listOf("7E8 43 12 34", "7EA 43 56 78"), ts, el)
        assertEquals(2, r.size)
        assertEquals(setOf("7E8", "7EA"), r.map { it.responderId }.toSet())
        assertArrayEquals(b("12", "34"), r[0].payload)
        assertArrayEquals(b("56", "78"), r[1].payload)
    }

    @Test
    fun outOfOrderRespondersPreserveArrivalOrder() {
        val r = ElmResponseEnvelopeParser.parse("01 0C",
            listOf("7EA 41 0C 00 10", "7E8 41 0C 1A F8"), ts, el)
        assertEquals(2, r.size)
        assertEquals("7EA", r[0].responderId)
        assertEquals("7E8", r[1].responderId)
    }

    @Test
    fun twentyNineBitIdentityArchitecture() {
        val r = ElmResponseEnvelopeParser.parse("01 0C",
            listOf("18DAF110 41 0C 1A F8"), ts, el)
        assertEquals(1, r.size)
        assertEquals("18DAF110", r[0].responderId)
        assertEquals(DiagnosticService.MODE_01, r[0].service)
        assertTrue(r[0].result is DiagnosticResult.Positive)
    }

    @Test
    fun negativeResponseWithoutHeader() {
        val r = ElmResponseEnvelopeParser.parse("01 03", listOf("7F 03 12"), ts, el)
        assertEquals(1, r.size)
        assertNull(r[0].responderId)
        val neg = r[0].result as DiagnosticResult.NegativeResponse
        assertEquals(DiagnosticService.MODE_03, neg.service)
        assertEquals(0x12, neg.nrc)
    }

    @Test
    fun noDataLineYieldsNoDataEnvelope() {
        val r = ElmResponseEnvelopeParser.parse("01 0C", listOf("NO DATA"), ts, el)
        assertEquals(1, r.size)
        assertTrue(r[0].result is DiagnosticResult.NoData)
    }

    @Test
    fun emptyLinesAreIgnored() {
        val r = ElmResponseEnvelopeParser.parse("01 0C", listOf("", "  ", "\t"), ts, el)
        assertTrue(r.isEmpty())
    }

    @Test
    fun adapterErrorStringBecomesAdapterFailure() {
        val r = ElmResponseEnvelopeParser.parse("ATRV", listOf("UNABLE TO CONNECT"), ts, el)
        assertEquals(1, r.size)
        assertTrue(r[0].result is DiagnosticResult.AdapterFailure)
    }

    @Test
    fun malformedHexLineBecomesMalformed() {
        val r = ElmResponseEnvelopeParser.parse("01 0C", listOf("41 GG 00"), ts, el)
        assertEquals(1, r.size)
        assertTrue(r[0].result is DiagnosticResult.Malformed)
    }

    @Test
    fun unknownPositiveServiceIsUnsupported() {
        // 0x52 positive response for an unmodeled service.
        val r = ElmResponseEnvelopeParser.parse("2E F1 90", listOf("7E8 52 01"), ts, el)
        assertEquals(1, r.size)
        assertTrue(r[0].result is DiagnosticResult.Unsupported)
        assertEquals(DiagnosticService.UNKNOWN, r[0].service)
    }

    @Test
    fun requestAndTimingArePropagated() {
        val r = ElmResponseEnvelopeParser.parse("01 0C", listOf("7E8 41 0C 1A F8"), ts, el)
        val e = r[0]
        assertEquals("01 0C", e.request)
        assertEquals(ts, e.timestampMs)
        assertEquals(el, e.elapsedMs)
        assertEquals(listOf("7E8 41 0C 1A F8"), e.rawLines)
    }

    @Test
    fun noEnvelopeDoesNotCollapseIntoEmptySuccess() {
        // A request with no decodable response line yields nothing; the session
        // layer maps emptiness to timeout/NoData — the parser never fabricates a
        // zero/positive result from nothing.
        assertTrue(ElmResponseEnvelopeParser.parse("01 0C", emptyList(), ts, el).isEmpty())
    }
}
