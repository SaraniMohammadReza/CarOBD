package com.sarani.carobd.core.adapter.command

import com.sarani.carobd.core.model.NotConnectedException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtcScanValidatorTest {

    private fun success(): AdapterResponse.Success = AdapterResponse.Success(
        rawResponse = "43 00 00 00 00 00 00",
        cleanedLines = listOf("43 00 00 00 00 00 00"),
        executionTimeMs = 12
    )

    @Test
    fun successMode03MeansEcuResponded() {
        assertNull(DtcScanValidator.notConnectedError(success()))
    }

    @Test
    fun errorMode03MeansNotConnected() {
        val err = DtcScanValidator.notConnectedError(
            AdapterResponse.Error(AdapterError.Timeout(4000L), "", 4000L)
        )
        assertNotNull(err)
        assertTrue(err is NotConnectedException)
        assertEquals("پاسخ در زمان مجاز دریافت نشد (4000 میلی‌ثانیه)", err!!.message)
    }

    @Test
    fun errorMode03NoDataMeansNotConnected() {
        val err = DtcScanValidator.notConnectedError(
            AdapterResponse.Error(AdapterError.NoData, "", 4000L)
        )
        assertNotNull(err)
        assertTrue(err is NotConnectedException)
    }

    @Test
    fun nullMode03ResponseMeansNotConnected() {
        val err = DtcScanValidator.notConnectedError(null)
        assertNotNull(err)
        assertTrue(err is NotConnectedException)
    }

    @Test
    fun negativeMode03ResponseIsFailureNotNoCodes() {
        // §4.4/§9: a 7F <svc> <NRC> Success payload is an explicit rejection,
        // NOT "no trouble codes found". Must surface as a failure.
        val sevenF = AdapterResponse.Success(
            rawResponse = "7F 03 22",
            cleanedLines = listOf("7F 03 22"),
            executionTimeMs = 20
        )
        val err = DtcScanValidator.notConnectedError(sevenF)
        assertNotNull("7F must NOT pass the truth gate as a valid Mode 03 response", err)
        assertTrue(err is NotConnectedException)
        assertTrue("Failure message must record the explicit ECU rejection", err?.message?.contains("rejected Mode 03") ?: false)
    }
}
