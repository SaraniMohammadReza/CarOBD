package com.sarani.carobd

import com.sarani.carobd.core.database.DtcDatabase
import com.sarani.carobd.core.protocol.FrameParser
import com.sarani.carobd.core.protocol.StandardPids
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolParserUnitTest {

    @Test
    fun testRpmFormula() {
        // 0x1A * 256 + 0xF8 = 6904. 6904 / 4 = 1726 RPM
        val raw = "41 0C 1A F8\r\r>"
        val bytes = FrameParser.extractHexBytes(raw)
        val rpm = StandardPids.parseRpm(bytes)

        assertEquals(1726, rpm)
    }

    @Test
    fun testSpeedFormula() {
        // 0x5A = 90 km/h
        val raw = "41 0D 5A\r\r>"
        val bytes = FrameParser.extractHexBytes(raw)
        val speed = StandardPids.parseSpeed(bytes)

        assertEquals(90, speed)
    }

    @Test
    fun testCoolantTempFormula() {
        // 0x81 = 129. 129 - 40 = 89°C (Typical running temp for Peugeot TU5 / EF7)
        val raw = "41 05 81\r\r>"
        val bytes = FrameParser.extractHexBytes(raw)
        val temp = StandardPids.parseCoolantTemp(bytes)

        assertEquals(89, temp)
    }

    @Test
    fun testDtcParserExtractsCodes() {
        // 43 03 00 (P0300) 01 15 (P0115) 00 00
        val rawResponse = "43 03 00 01 15 00 00\r\r>"
        val dtcs = FrameParser.parseDtcResponse(rawResponse)

        assertEquals(2, dtcs.size)
        assertEquals("P0300", dtcs[0])
        assertEquals("P0115", dtcs[1])
    }

    @Test
    fun testBilingualDtcDatabaseResolution() {
        val dtc = DtcDatabase.resolveCode("P0300")
        assertNotNull(dtc)
        assertEquals("P0300", dtc.code)
        assertTrue(dtc.titleFa.contains("احتراق ناقص") || dtc.titleFa.contains("میس‌فایر"))
        assertTrue(dtc.titleEn.contains("Misfire"))
    }

    @Test
    fun testFrameCleanerStripsPromptAndNoise() {
        val dirty = "SEARCHING...\r\nBUS INIT: OK\r\n41 0C 10 00\r\r>"
        val cleaned = FrameParser.cleanResponse(dirty)

        assertTrue(!cleaned.contains(">"))
        assertTrue(!cleaned.contains("SEARCHING"))
        assertEquals("41 0C 10 00", cleaned)
    }
}
