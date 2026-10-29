package com.sarani.carobd.core.protocol.obd2
import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test
class Sprint2EnvelopeAdversarialTest {
    @Test fun compactSpacedColonAnd29BitUseSamePayload() {
        val fixtures = listOf("430201330300", "43 02 01 33 03 00", "7E806430201330300", "7E8 06 43 02 01 33 03 00", "7E8:06430201330300", "18DAF11006430201330300", "18DAF110 06 43 02 01 33 03 00")
        for (raw in fixtures) {
            val env = ElmResponseEnvelopeParser.parse("03", listOf(raw), 0, 0).single()
            assertTrue(raw, env.result is DiagnosticResult.Positive)
            assertArrayEquals(byteArrayOf(2, 1, 0x33, 3, 0), env.payload)
        }
    }
    @Test fun malformedAndAdapterErrorsNeverPositiveOrThrow() {
        val fixtures = listOf("43 FFFFFFFF", "43 100", "430", "ZZ", "7E8 06 43 01", "7E8 10 43 01", "STOPPED", "CAN ERROR", "BUS INIT...ERROR", "BUFFER FULL", "NO DATA")
        for (raw in fixtures) assertFalse(raw, ElmResponseEnvelopeParser.parse("03", listOf(raw), 0, 0).single().result is DiagnosticResult.Positive)
        for (c in 0..255) ElmResponseEnvelopeParser.parse("03", listOf("43" + c.toChar() + "100"), 0, 0)
    }
}
