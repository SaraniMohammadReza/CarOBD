package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.DiagnosticResult
import com.sarani.carobd.core.model.DiagnosticService
import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.IsoTpFraming
import com.sarani.carobd.core.protocol.dtc.DtcFrame
import com.sarani.carobd.core.protocol.dtc.DtcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WireEnvelopeGateTest {

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun success(vararg lines: String) = AdapterResponse.Success(
        rawResponse = lines.joinToString("\r"),
        cleanedLines = lines.toList(),
        executionTimeMs = 42L
    )

    private fun error(err: AdapterError, raw: String = "") =
        AdapterResponse.Error(err, rawResponse = raw, executionTimeMs = 42L)

    // ── Success path ─────────────────────────────────────────────────────

    @Test
    fun success_positiveResponse_becomes_Positive_envelope() {
        val resp = success("41 00 BE 1F A8 13")
        val env = resp.toWireEnvelopes("01 00").single()
        assertTrue(env.result is DiagnosticResult.Positive)
        val payload = (env.result as DiagnosticResult.Positive).payload
        // payload is the tokens AFTER the response byte (0x41)
        assertEquals(5, payload.size)
        assertEquals(0x41, env.service.positiveResponseByte)
        assertEquals(0x01, env.service.modeByte)
        assertEquals(DiagnosticService.MODE_01, env.service)
        assertEquals(IsoTpFraming.RAW, env.framing)
        assertEquals(listOf("41 00 BE 1F A8 13"), env.rawLines)
    }

    @Test
    fun success_multiLine_parsesEachLineAsSeparateEnvelope() {
        // Multi-ECU: 7E8 + 7E9 each respond
        val resp = success("7E8 43 01 33", "7E9 43 02 10")
        val envelopes = resp.toWireEnvelopes("03")
        assertEquals(2, envelopes.size)
        val responders = envelopes.map { it.responderId }.toSet()
        assertEquals(setOf("7E8", "7E9"), responders)
        for (env in envelopes) {
            assertTrue(env.result is DiagnosticResult.Positive)
            assertEquals(DiagnosticService.MODE_03, env.service)
        }
    }

    // ── NoData ───────────────────────────────────────────────────────────

    @Test
    fun success_noDataLine_becomes_NoData_envelope() {
        val resp = success("NO DATA")
        val env = resp.toWireEnvelopes("01 0C").single()
        assertTrue(env.result is DiagnosticResult.NoData)
    }

    // ── Negative response ────────────────────────────────────────────────

    @Test
    fun success_sevenF_becomes_NegativeResponse_envelope() {
        val resp = success("7F 03 22")
        val env = resp.toWireEnvelopes("03").single()
        val r = env.result as DiagnosticResult.NegativeResponse
        assertEquals(DiagnosticService.MODE_03, r.service)
        assertEquals(0x22, r.nrc)
    }

    @Test
    fun success_sevenF_preservesNrc() {
        val resp = success("7F 01 12")
        val env = resp.toWireEnvelopes("01 00").single()
        val r = env.result as DiagnosticResult.NegativeResponse
        assertEquals(0x12, r.nrc)
    }

    @Test
    fun success_sevenF_withResponder_preservesResponderId() {
        val resp = success("7E9 7F 01 22")
        val env = resp.toWireEnvelopes("01 00").single()
        val r = env.result as DiagnosticResult.NegativeResponse
        assertEquals("7E9", env.responderId)
        assertEquals(DiagnosticService.MODE_01, r.service)
        assertEquals(0x22, r.nrc)
    }

    // ── Malformed ────────────────────────────────────────────────────────

    @Test
    fun success_unparseableHex_becomes_Malformed() {
        val resp = success("XYZ")
        val env = resp.toWireEnvelopes("01 00").single()
        assertTrue("expected Malformed, got ${env.result}", env.result is DiagnosticResult.Malformed)
    }

    // ── Error path ───────────────────────────────────────────────────────

    @Test
    fun error_timeout_withNoRaw_becomes_Timeout_envelope() {
        val resp = error(AdapterError.Timeout(4000L), raw = "")
        val env = resp.toWireEnvelopes("03").single()
        assertTrue(env.result is DiagnosticResult.Timeout)
    }

    @Test
    fun error_noDataToken_becomes_NoData_envelope() {
        val resp = error(AdapterError.NoData, raw = "NO DATA")
        val env = resp.toWireEnvelopes("03").single()
        assertTrue(env.result is DiagnosticResult.NoData)
    }

    @Test
    fun error_canError_becomes_AdapterFailure_envelope() {
        val resp = error(AdapterError.CanError, raw = "CAN ERROR")
        val env = resp.toWireEnvelopes("03").single()
        assertTrue(env.result is DiagnosticResult.AdapterFailure)
    }

    @Test
    fun error_unableToConnect_withNoRaw_becomes_AdapterFailure_envelope() {
        val resp = error(AdapterError.UnableToConnect, raw = "")
        val env = resp.toWireEnvelopes("03").single()
        val r = env.result
        assertTrue("expected AdapterFailure, got $r", r is DiagnosticResult.AdapterFailure)
    }

    @Test
    fun error_timeout_withPartialBuffer_parsesAsMalformed_or_timeout() {
        // Timeout with partial buffer content: bytes may be incomplete → Malformed
        val resp = error(AdapterError.Timeout(600L), raw = "41 0C")
        val env = resp.toWireEnvelopes("01 0C").single()
        // "41 0C" has a valid hex structure but incomplete payload.
        // The envelope parser creates a Positive with empty payload.
        assertTrue(env.result is DiagnosticResult.Timeout)
    }

    // ── Preservation ─────────────────────────────────────────────────────

    @Test
    fun success_preservesTimestampAndElapsed() {
        val ts = System.currentTimeMillis()
        val resp = success("41 00 BE 1F A8 13")
        val env = resp.toWireEnvelopes("01 00")
        assertEquals(42L, env[0].elapsedMs)
        assertTrue(env[0].timestampMs >= ts)
    }

    @Test
    fun success_emptyCleanedLines_produces_emptyEnvelopes() {
        val resp = success()
        val envelopes = resp.toWireEnvelopes("03")
        assertTrue(envelopes.isEmpty())
    }

    @Test
    fun unknownResponderNotAssumed7E8() {
        // A line with no CAN header → responderId stays null (NOT "7E8")
        val resp = success("41 00 BE 1F A8 13")
        val env = resp.toWireEnvelopes("01 00").single()
        assertEquals(null, env.responderId)
        assertTrue(env.result is DiagnosticResult.Positive)
    }

    // ── §10 P0-C: multi-ECU DTC scanning integration ──────────────────────

    @Test
    fun multiEcuMode03_dtcCodesTaggedWithCorrectResponder() {
        // Two ECUs respond to Mode 03: 7E8 has one DTC, 7E9 has another.
        // 43 <count> <DTC hi> <DTC lo> <padding>  (CAN_WITH_COUNT framing)
        // P0300 = 03 00, P0171 = 01 71
        val resp = success(
            "7E8 43 01 03 00 00 00",   // 7E8: P0300
            "7E9 43 01 01 71 00 00"    // 7E9: P0171
        )
        val envelopes = resp.toWireEnvelopes("03")
        assertEquals(2, envelopes.size)

        val allDtc = envelopes.mapNotNull { env ->
            val r = env.result
            if (r is DiagnosticResult.Positive) {
                // §7: envelope parser strips the echo byte; reconstitute it so
                // DtcParser.parseMode03Payload() can validate the positive echo.
                val fullPayload = byteArrayOf(r.service.positiveResponseByte.toByte()) + r.payload
                DtcParser.parseMode03Payload(
                    fullPayload, DtcCategory.STORED,
                    responderAddressHex = env.responderId,
                    frame = DtcFrame.CAN_WITH_COUNT
                )
            } else null
        }.flatten()

        // Two distinct codes from two distinct responders.
        assertEquals(2, allDtc.size)
        val byResponder = allDtc.associate { it.responderAddressHex to it.code }
        assertEquals("P0300", byResponder["7E8"])
        assertEquals("P0171", byResponder["7E9"])

        // ecuSource should reflect the responder, not a fabricated module name.
        val ecu7E8 = allDtc.first { it.responderAddressHex == "7E8" }
        assertEquals("ECU 7E8", ecu7E8.ecuSource)
        val ecu7E9 = allDtc.first { it.responderAddressHex == "7E9" }
        assertEquals("ECU 7E9", ecu7E9.ecuSource)
    }

    @Test
    fun multiEcuMode03_positiveAndNegativeFromDifferentResponders() {
        // Functional request: 7E8 answers positively, 7EA NACKs (7F 03 22).
        val resp = success(
            "7E8 43 00 00 00 00 00 00 00",   // 7E8: no DTCs
            "7EA 7F 03 22"                   // 7EA: rejection, NRC 0x22
        )
        val envelopes = resp.toWireEnvelopes("03")
        assertEquals(2, envelopes.size)

        // Positive from 7E8 — parser yields no DTCs (count = 0).
        val pos = envelopes.first { it.responderId == "7E8" }
        assertTrue(pos.result is DiagnosticResult.Positive)
        val fullPayload = byteArrayOf((pos.result as DiagnosticResult.Positive).service.positiveResponseByte.toByte()) +
            (pos.result as DiagnosticResult.Positive).payload
        val dtcs = DtcParser.parseMode03Payload(
            fullPayload,
            DtcCategory.STORED,
            responderAddressHex = "7E8",
            frame = DtcFrame.CAN_WITH_COUNT
        )
        assertTrue(dtcs.isEmpty())

        // Negative from 7EA — must NOT be collapsed into "no codes found".
        val neg = envelopes.first { it.responderId == "7EA" }
        val n = neg.result as DiagnosticResult.NegativeResponse
        assertEquals(0x22, n.nrc)
        assertEquals(DiagnosticService.MODE_03, n.service)
    }
}
