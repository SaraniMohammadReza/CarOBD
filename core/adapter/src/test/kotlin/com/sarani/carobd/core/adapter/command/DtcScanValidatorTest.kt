package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.EcuRejectionException
import com.sarani.carobd.core.model.NotConnectedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcScanValidatorTest {

    private fun success(rawResponse: String = "43 00 00 00 00 00 00"): AdapterResponse.Success =
        AdapterResponse.Success(
            rawResponse = rawResponse,
            cleanedLines = listOf(rawResponse),
            executionTimeMs = 12
        )

    @Test
    fun successMode03MeansEcuResponded() {
        // A clean positive Mode-03 acknowledgement (43 00) passes the truth gate.
        assertNull(DtcScanValidator.scanFailure(success()))
    }

    @Test
    fun errorMode03MeansNotConnected() {
        val err = DtcScanValidator.scanFailure(
            AdapterResponse.Error(AdapterError.Timeout(4000L), "", 4000L)
        )
        assertNotNull(err)
        assertTrue(err is NotConnectedException)
        assertEquals("پاسخ در زمان مجاز دریافت نشد (4000 میلی‌ثانیه)", err!!.message)
    }

    @Test
    fun errorMode03NoDataMeansNotConnected() {
        val err = DtcScanValidator.scanFailure(
            AdapterResponse.Error(AdapterError.NoData, "", 4000L)
        )
        assertNotNull(err)
        assertTrue(err is NotConnectedException)
    }

    @Test
    fun nullMode03ResponseMeansNotConnected() {
        val err = DtcScanValidator.scanFailure(null)
        assertNotNull(err)
        assertTrue(err is NotConnectedException)
    }

    @Test
    fun negativeMode03ResponseIsEcuRejectionNotNotConnected() {
        // §4.4/§9: "7F 03 22" is an ECU RESPONSE — reachable but NACK'd (NRC 0x22).
        // Must surface as EcuRejectionException, NOT NotConnectedException and NOT
        // "no codes found" success.
        val err = DtcScanValidator.scanFailure(success("7F 03 22"))
        assertNotNull("7F must NOT pass the truth gate as a valid Mode 03 response", err)
        assertTrue("7F must be EcuRejection, not NotConnected", err is EcuRejectionException)
        val rejection = err as EcuRejectionException
        assertEquals(0x22, rejection.negativeResponse.nrcCode)
        assertEquals(0x03, rejection.negativeResponse.requestedService)
        assertTrue("failure message must record the explicit rejection", rejection.message?.contains("rejected", ignoreCase = true) == true)
    }

    @Test
    fun sevenFIsRejectionNotConnectionLoss() {
        // Distinctness invariant: 7F is EcuRejectionException, never NotConnected.
        val err = DtcScanValidator.scanFailure(success("7F 03 12"))
        assertTrue(err is EcuRejectionException)
        assertTrue("must not be reported as a connection error", err !is NotConnectedException)
    }

    @Test
    fun sevenFRejectionPreservesRequestedServiceAndNrc() {
        // §4.4: 7F must preserve requested service + NRC across services.
        val cases = listOf("7F 03 22" to 0x03, "7F 01 11" to 0x01, "7F 09 12" to 0x09, "7F 0A 13" to 0x0A)
        for ((raw, svc) in cases) {
            val err = DtcScanValidator.scanFailure(success(raw))
            assertNotNull(err)
            assertTrue(err is EcuRejectionException)
            assertEquals(svc, (err as EcuRejectionException).negativeResponse.requestedService)
        }
    }

    @Test
    fun nonNegativeSuccessPassesGate() {
        // A genuine positive Mode-03 frame (43 00 = 0 codes) must pass the gate.
        assertNull(DtcScanValidator.scanFailure(success("43 00 00 00 00 00 00")))
    }
}
