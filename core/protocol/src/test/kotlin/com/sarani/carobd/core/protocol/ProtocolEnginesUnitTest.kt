package com.sarani.carobd.core.protocol

import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.DtcSystem
import com.sarani.carobd.core.protocol.dtc.DtcLookupEngine
import com.sarani.carobd.core.protocol.dtc.DtcParser
import com.sarani.carobd.core.protocol.isotp.FlowStatus
import com.sarani.carobd.core.protocol.isotp.IsoTpReassembler
import com.sarani.carobd.core.protocol.j1939.J1939CanId
import com.sarani.carobd.core.protocol.j1939.J1939Engine
import com.sarani.carobd.core.protocol.kwp2000.Kwp2000Engine
import com.sarani.carobd.core.protocol.kwp2000.KwpNrc
import com.sarani.carobd.core.protocol.kwp2000.KwpResponse
import com.sarani.carobd.core.protocol.obd2.PidCodec
import com.sarani.carobd.core.protocol.uds.UdsEngine
import com.sarani.carobd.core.protocol.uds.UdsNrc
import com.sarani.carobd.core.protocol.uds.UdsResponse
import com.sarani.carobd.core.protocol.uds.UdsStandardDid
import org.junit.Assert.*
import org.junit.Test

class ProtocolEnginesUnitTest {

    @Test
    fun testIsoTpSingleFrameReassembly() {
        val reassembler = IsoTpReassembler()
        // Single Frame: PCI 0x03 (length 3), payload 0x41 0x0C 0x1A
        val rawFrame = byteArrayOf(0x03, 0x41, 0x0C, 0x1A, 0x00, 0x00, 0x00, 0x00)
        val result = reassembler.processFrame(rawFrame)

        assertNotNull(result)
        assertEquals(3, result!!.size)
        assertEquals(0x41.toByte(), result[0])
        assertEquals(0x0C.toByte(), result[1])
        assertEquals(0x1A.toByte(), result[2])
    }

    @Test
    fun testIsoTpMultiFrameSegmentationAndReassembly() {
        val reassembler = IsoTpReassembler()
        // Create 20-byte payload (e.g. Mode 09 VIN payload)
        val originalPayload = "VF32AKFWF12345678".toByteArray() // 17 bytes
        val segments = IsoTpReassembler.segmentPayload(originalPayload, maxFrameSize = 8)

        // 17 bytes: First Frame has 6 data bytes, remaining 11 bytes need 2 Consecutive Frames (7 + 4)
        assertEquals(3, segments.size)

        var finalResult: ByteArray? = null
        for (frame in segments) {
            val step = reassembler.processFrame(frame)
            if (step != null) {
                finalResult = step
            }
        }

        assertNotNull("Reassembly should succeed after all consecutive frames", finalResult)
        assertEquals(17, finalResult!!.size)
        assertArrayEquals(originalPayload, finalResult)
    }

    @Test
    fun testDtcParserObd2() {
        // P0133: 0x01, 0x33
        val dtc1 = DtcParser.parseStandardObd2Dtc(0x01.toByte(), 0x33.toByte())
        assertNotNull(dtc1)
        assertEquals("P0133", dtc1!!.code)
        assertEquals(DtcSystem.POWERTRAIN, dtc1.system)
        assertTrue(dtc1.descriptionFa.contains("اکسیژن"))

        // C0301: 0x43, 0x01
        val dtc2 = DtcParser.parseStandardObd2Dtc(0x43.toByte(), 0x01.toByte())
        assertNotNull(dtc2)
        assertEquals("C0301", dtc2!!.code)
        assertEquals(DtcSystem.CHASSIS, dtc2.system)

        // B0100: 0x81, 0x00
        val dtc3 = DtcParser.parseStandardObd2Dtc(0x81.toByte(), 0x00.toByte())
        assertNotNull(dtc3)
        assertEquals("B0100", dtc3!!.code)
        assertEquals(DtcSystem.BODY, dtc3.system)

        // U0100: 0xC1, 0x00
        val dtc4 = DtcParser.parseStandardObd2Dtc(0xC1.toByte(), 0x00.toByte())
        assertNotNull(dtc4)
        assertEquals("U0100", dtc4!!.code)
        assertEquals(DtcSystem.NETWORK, dtc4.system)
    }

    @Test
    fun testDtcMode03PayloadParsing() {
        // Mode 43 payload with DTC count byte (2 codes: P0115 and P0300)
        val payload = byteArrayOf(0x02, 0x01, 0x15, 0x03, 0x00)
        val dtcs = DtcParser.parseMode03Payload(payload, DtcCategory.STORED)

        assertEquals(2, dtcs.size)
        assertEquals("P0115", dtcs[0].code)
        assertEquals("P0300", dtcs[1].code)
    }

    @Test
    fun testUdsDtcParsingWithFailureTypeByte() {
        // P0100 with FTB 0x11 (Circuit short to ground)
        val dtc = DtcParser.parseUdsDtc(0x01.toByte(), 0x00.toByte(), 0x11.toByte(), statusMask = 0x08)
        assertNotNull(dtc)
        assertEquals("P0100-11", dtc!!.code)
        assertTrue(dtc.descriptionEn.contains("Circuit short to ground"))
        assertTrue(dtc.descriptionFa.contains("اتصال کوتاه"))
    }

    @Test
    fun testPidCalculations() {
        // RPM: ((256 * 0x1A) + 0xF8) / 4 = ((26 * 256) + 248) / 4 = 6904 / 4 = 1726.0 rpm
        val rpm = PidCodec.decodeEngineRpm(0x1A, 0xF8)
        assertEquals(1726.0, rpm, 0.01)

        // Speed: 0x55 = 85 km/h
        val speed = PidCodec.decodeVehicleSpeed(0x55)
        assertEquals(85.0, speed, 0.01)

        // Coolant Temp: 0x82 = 130 - 40 = 90 °C
        val ect = PidCodec.decodeStandardTemperature(0x82)
        assertEquals(90.0, ect, 0.01)

        // Throttle: 0x80 = 128 * 100 / 255 = 50.196%
        val throttle = PidCodec.decodeThrottlePosition(0x80)
        assertEquals(50.196, throttle, 0.01)
    }

    @Test
    fun testSupportedPidBitmaskDecoding() {
        // Bitmask: 0xBE, 0x1F, 0xA8, 0x13
        // 0xBE = 1011 1110 -> PIDs 1, 3, 4, 5, 6, 7
        val bytes = byteArrayOf(0xBE.toByte(), 0x1F.toByte(), 0xA8.toByte(), 0x13.toByte())
        val supported = PidCodec.decodeSupportedPids(bytes, basePid = 0x00)

        assertTrue(supported.contains(0x01)) // Supported
        assertFalse(supported.contains(0x02)) // Not supported
        assertTrue(supported.contains(0x03))
        assertTrue(supported.contains(0x04))
        assertTrue(supported.contains(0x05))
        assertTrue(supported.contains(0x0C)) // RPM is bit 12
    }

    @Test
    fun testKwp2000FrameBuildingAndVerification() {
        val kwp = Kwp2000Engine(targetAddress = 0x10, sourceAddress = 0xF1)
        // Request StartCommunication (SID 0x81)
        val frame = kwp.buildFrame(0x81)

        // Format byte: 0x81 (Physical addressing, 1 byte data)
        assertEquals(0x81.toByte(), frame[0])
        assertEquals(0x10.toByte(), frame[1])
        assertEquals(0xF1.toByte(), frame[2])
        assertEquals(0x81.toByte(), frame[3])

        // Verify checksum is sum modulo 256
        val sum = (0x81 + 0x10 + 0xF1 + 0x81) and 0xFF
        assertEquals(sum.toByte(), frame.last())

        // Simulate ECU positive response frame
        // Echo SID: 0xC1, Key bytes: 0xEA, 0x8F
        val responseBytes = byteArrayOf(0x83.toByte(), 0xF1.toByte(), 0x10.toByte(), 0xC1.toByte(), 0xEA.toByte(), 0x8F.toByte())
        val respSum = responseBytes.sumOf { it.toInt() and 0xFF } and 0xFF
        val completeResp = responseBytes + byteArrayOf(respSum.toByte())

        val parsed = kwp.parseFrame(completeResp)
        assertTrue(parsed is KwpResponse.Positive)
        val pos = parsed as KwpResponse.Positive
        assertEquals(0xC1, pos.serviceId)
        assertEquals(2, pos.payload.size)
        assertEquals(0xEA.toByte(), pos.payload[0])
        assertEquals(0x8F.toByte(), pos.payload[1])
    }

    @Test
    fun testKwp2000NegativeResponse() {
        val kwp = Kwp2000Engine()
        // 0x7F [Rejected SID 0x21] [NRC 0x22 ConditionsNotCorrect]
        val frameNoCs = byteArrayOf(0x83.toByte(), 0xF1.toByte(), 0x10.toByte(), 0x7F.toByte(), 0x21.toByte(), 0x22.toByte())
        val cs = frameNoCs.sumOf { it.toInt() and 0xFF } and 0xFF
        val fullFrame = frameNoCs + byteArrayOf(cs.toByte())

        val parsed = kwp.parseFrame(fullFrame)
        assertTrue(parsed is KwpResponse.Negative)
        val neg = parsed as KwpResponse.Negative
        assertEquals(0x21, neg.rejectedServiceId)
        assertEquals(0x22, neg.nrcCode)
        assertEquals(KwpNrc.CONDITIONS_NOT_CORRECT, neg.nrc)
    }

    @Test
    fun testUdsEngineReadDid() {
        val uds = UdsEngine()
        val payload = uds.buildReadDidPayload(UdsStandardDid.VIN_DATA_IDENTIFIER)
        assertEquals(3, payload.size)
        assertEquals(0x22.toByte(), payload[0])
        assertEquals(0xF1.toByte(), payload[1])
        assertEquals(0x90.toByte(), payload[2])

        // Simulate response: [0x62, 0xF1, 0x90, 'V', 'I', 'N', '1', '2', '3']
        val response = byteArrayOf(0x62.toByte(), 0xF1.toByte(), 0x90.toByte()) + "VIN123".toByteArray()
        val didData = uds.parseReadDidResponse(response)
        assertNotNull(didData)
        assertEquals(UdsStandardDid.VIN_DATA_IDENTIFIER, didData!!.did)
        assertEquals("VIN123", didData.toAscii())
    }

    @Test
    fun testJ1939CanIdParsing() {
        // Example: Priority 6, PGN 65226 (0xFECA), SA 0x00 -> 29-bit CAN ID 0x18FECA00
        val canId = 0x18FECA00L
        val parsed = J1939CanId.parse(canId)

        assertEquals(6, parsed.priority)
        assertEquals(65226, parsed.pgn)
        assertEquals(0, parsed.sourceAddress)
        assertFalse(parsed.isPdu1Format) // PF = 0xFE = 254 >= 240 -> PDU2 broadcast
    }

    @Test
    fun testJ1939Dm1Parsing() {
        // DM1 payload: 2 lamp bytes + 4 bytes DTC
        // Lamp byte 1: 0x40 (MIL active)
        // SPN: 190 (Engine Speed) = 0x00BE -> Byte 1: 0xBE, Byte 2: 0x00, Byte 3: 0x02 (FMI 2: Erratic)
        // Byte 4: Occurrence Count 5
        val payload = byteArrayOf(
            0x40.toByte(), 0x00.toByte(),
            0xBE.toByte(), 0x00.toByte(), 0x02.toByte(), 0x05.toByte()
        )

        val (lampStatus, dtcs) = J1939Engine.parseDmMessage(payload)
        assertTrue(lampStatus.milLamp)
        assertEquals(1, dtcs.size)
        val dtc = dtcs[0]
        assertEquals(190, dtc.spn)
        assertEquals(2, dtc.fmi)
        assertEquals(5, dtc.occurrenceCount)
        assertTrue(dtc.spnDescription.contains("Engine Speed"))
    }
}
