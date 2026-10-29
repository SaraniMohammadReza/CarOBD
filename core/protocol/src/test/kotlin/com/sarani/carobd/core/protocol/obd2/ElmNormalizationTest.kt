package com.sarani.carobd.core.protocol.obd2
import com.sarani.carobd.core.model.DiagnosticResult
import org.junit.Assert.*
import org.junit.Test
class ElmNormalizationTest {
    private fun parse(request: String, raw: String) = ElmResponseEnvelopeParser.parse(request, listOf(raw), 0, 0).single()
    @Test fun envelope_acceptsCompactHeaderlessResponse() {
        assertTrue(parse("03", "430201330300").result is DiagnosticResult.Positive)
        assertTrue(parse("010C", "410C1AF8").result is DiagnosticResult.Positive)
        assertNull(parse("010C", "410C1AF8").responderId)
    }
    @Test fun envelope_acceptsCompactCanResponse() {
        for (raw in listOf("7E806430201330300", "7E8 06 43 02 01 33 03 00")) {
            val e = parse("03", raw)
            assertEquals("7E8", e.responderId)
            assertArrayEquals(byteArrayOf(2,1,0x33,3,0), e.payload)
        }
        assertEquals("18DAF110", parse("010C", "18DAF11004410C1AF8").responderId)
    }
    @Test fun envelope_acceptsSpacedResponse() { assertTrue(parse("010C", "41 0C 1A F8").result is DiagnosticResult.Positive) }
    @Test fun envelope_rejectsOversizedTokenWithoutThrow() {
        for (raw in listOf("43 FFFFFFFF", "43 100", "410", "41 GG 00", "7E8 07 43 00", "7E8 10 14 49 02 00", "41 0C", "41 0D 55"))
            assertTrue(raw, parse("010C", raw).result is DiagnosticResult.Malformed)
    }
}
