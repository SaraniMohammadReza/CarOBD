package com.sarani.carobd.core.protocol

import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.DtcSystem
import com.sarani.carobd.core.model.Mode09Decoded
import com.sarani.carobd.core.protocol.dtc.DtcLookupEngine
import com.sarani.carobd.core.protocol.dtc.DtcParser
import com.sarani.carobd.core.protocol.dtc.DtcFrame
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
        // Mode 43 positive response WITH echo byte: 43 [count=02] P0115 P0300
        val payload = byteArrayOf(0x43, 0x02, 0x01, 0x15, 0x03, 0x00)
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

    // ---- §11 ISO-TP Network Layer coverage (Network → Transport Layer reassembly) ----

    @Test
    fun testIsoTpFirstFrameStartsAssembly() {
        val reassembler = IsoTpReassembler()
        // First Frame: PCI 0x10 (FF), total length 0x0011 = 17 bytes, 6 data bytes follow.
        val ff = byteArrayOf(0x10, 0x11, 0x49, 0x44, 0x00, 0x00, 0x00, 0x00)
        assertNull("First Frame must not complete the message", reassembler.processFrame(ff))
        assertFalse(reassembler.isComplete)
        assertTrue("Assembly progress must advance past zero", reassembler.progress > 0f)
    }

    @Test
    fun testIsoTpConsecutiveFrameCompletesReassembly() {
        val payload = "VF32AKFWF12345678".toByteArray() // 17-byte Mode 09 VIN payload
        val frames = IsoTpReassembler.segmentPayload(payload, 8)
        assertTrue("Multi-frame payload must yield a First + Consecutive frames", frames.size >= 2)
        val reassembler = IsoTpReassembler()
        var result: ByteArray? = null
        for (frame in frames) {
            result = reassembler.processFrame(frame) ?: result
        }
        assertNotNull("Reassembly must complete on the final Consecutive Frame", result)
        assertArrayEquals(payload, result!!)
    }

    @Test
    fun testIsoTpSequenceRollover() {
        // Payload large enough to require > 16 Consecutive Frames, exercising the 4-bit
        // sequence-number rollover (1..F then 0..).
        val payload = ByteArray(200) { (it % 251).toByte() }
        val frames = IsoTpReassembler.segmentPayload(payload, 8)
        assertTrue("Payload must need more than 16 Consecutive Frames for rollover", frames.size > 17)
        val reassembler = IsoTpReassembler()
        var result: ByteArray? = null
        for (frame in frames) {
            result = reassembler.processFrame(frame) ?: result
        }
        assertNotNull(result)
        assertArrayEquals(payload, result!!)
    }

    @Test
    fun testIsoTpWrongSequenceResets() {
        val reassembler = IsoTpReassembler()
        // First Frame declaring a 13-byte message
        val ff = byteArrayOf(0x10, 0x0D, 0x41, 0x0C, 0x00, 0x00, 0x00, 0x00)
        assertNull(reassembler.processFrame(ff))
        // Consecutive Frame with WRONG sequence number (2 instead of expected 1)
        val wrongSeq = byteArrayOf(0x22, 0x41, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        assertNull("Wrong sequence number must not complete assembly", reassembler.processFrame(wrongSeq))
        // A later correctly-sequenced CF must NOT resurrect a dead assembly (state was reset)
        val cf1 = byteArrayOf(0x21, 0x41, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        assertNull("Reset state must reject stray Consecutive Frame", reassembler.processFrame(cf1))
    }

    @Test
    fun testIsoTpMalformedAndEmptyFramesAreIgnored() {
        val reassembler = IsoTpReassembler()
        assertNull("Empty frame must be ignored", reassembler.processFrame(ByteArray(0)))
        // Invalid PCI type nibble (0xF) -> not a valid ISO-TP frame type
        assertNull(
            "Invalid PCI must be ignored",
            reassembler.processFrame(byteArrayOf(0xF0.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00))
        )
    }

    @Test
    fun testIsoTpFlowControlFrameIsIgnoredByReceiver() {
        val reassembler = IsoTpReassembler()
        // First Frame declaring an 8-byte message (6 bytes initial + need CF)
        val ff = byteArrayOf(0x10, 0x08, 0x41, 0x05, 0x00, 0x00, 0x00, 0x00)
        assertNull(reassembler.processFrame(ff))
        // Flow Control frame (PCI 0x30) is a sender-side directive; receiver ignores it
        val fc = byteArrayOf(0x30, 0x00, 0x00, 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte())
        assertNull(reassembler.processFrame(fc))
        assertFalse("Flow Control must not complete receiver assembly", reassembler.isComplete)
    }

    @Test
    fun testIsoTpLengthMismatchStaysIncomplete() {
        val reassembler = IsoTpReassembler()
        // First Frame declares 20 bytes; only one partial Consecutive Frame arrives
        val ff = byteArrayOf(0x10, 0x14, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06) // total 20
        assertNull(reassembler.processFrame(ff))
        val partialCf = byteArrayOf(0x21, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D)
        assertNull("Partial Consecutive Frame must not complete assembly", reassembler.processFrame(partialCf))
        assertFalse(reassembler.isComplete)
    }

    // ---- §10 Mode coverage: parsing / validation ----

    @Test
    fun testMode09VinDecoding() {
        val vin = "VF32AKFWF12345678"
        val decoded = PidCodec.decodeVin(vin.toByteArray())
        assertEquals(vin, decoded)
    }

    @Test
    fun testMode07PendingDtcCarriesPendingCategory() {
        // Mode 07 positive response WITH echo: 47 [count=01] P0300.
        val payload = byteArrayOf(0x47, 0x01, 0x03, 0x00)
        val dtcs = DtcParser.parseMode03Payload(payload, category = DtcCategory.PENDING)
        assertEquals(1, dtcs.size)
        assertEquals("P0300", dtcs[0].code)
        assertEquals(DtcCategory.PENDING, dtcs[0].category)
    }

    @Test
    fun testClearDtcResponseValidation() {
        // Mode 04 positive response echoes service id 0x44.
        assertTrue(PidCodec.verifyClearDtcResponse(byteArrayOf(0x44)))
        assertTrue(PidCodec.verifyClearDtcResponse(byteArrayOf(0x44, 0x00)))
        assertFalse("Empty response must not validate as success", PidCodec.verifyClearDtcResponse(byteArrayOf()))
        assertFalse("Wrong SID must not validate", PidCodec.verifyClearDtcResponse(byteArrayOf(0x41, 0x0C)))
    }

    // ---- §13 responder-identity preservation (multi-ECU honest framing) ----

    @Test
    fun testDtcParserPreservesResponderAddress() {
        val payload = byteArrayOf(0x43, 0x01, 0x03, 0x00) // 43 echo + count=01 + P0300
        val dtcs = DtcParser.parseMode03Payload(payload, responderAddressHex = "7E9")
        assertEquals(1, dtcs.size)
        assertEquals("7E9", dtcs[0].responderAddressHex) // identity preserved, not hardcoded 7E8
        assertEquals("Engine ECU", dtcs[0].ecuSource)
    }

    @Test
    fun testDtcParserNeutralEcuNameForUnknownResponder() {
        val payload = byteArrayOf(0x43, 0x01, 0x03, 0x00) // 43 echo + count=01 + P0300
        val dtcs = DtcParser.parseMode03Payload(payload, responderAddressHex = "720")
        assertEquals(1, dtcs.size)
        assertEquals("720", dtcs[0].responderAddressHex)
        assertEquals("ECU 720", dtcs[0].ecuSource) // not a fabricated ABS/SRS/BCM name
    }

    // ---- §9 structured negative-response model (7F <svc> <NRC>) ----

    @Test
    fun testIsNegativeResponseDetects7f() {
        assertTrue(PidCodec.isNegativeResponse(byteArrayOf(0x7F, 0x03, 0x12)))
        assertFalse(PidCodec.isNegativeResponse(byteArrayOf(0x41, 0x0C, 0x1A.toByte(), 0xF8.toByte())))
        assertFalse(PidCodec.isNegativeResponse(byteArrayOf()))
    }

    @Test
    fun testParseNegativeResponseCommonNrc() {
        val neg = PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x03, 0x22), responder = "7E8", rawLine = "7F 03 22")
        assertNotNull(neg)
        assertEquals(0x03, neg!!.requestedService)
        assertEquals(0x22, neg.nrcCode)
        assertNotNull("known NRC must resolve", neg.nrc)
        assertEquals("7E8", neg.responder)
        assertEquals("7F 03 22", neg.rawLine)
    }

    @Test
    fun testParseNegativeResponseUnknownNrcRetainedNumerically() {
        val neg = PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x01, 0xAB.toByte()))
        assertNotNull(neg)
        assertEquals(0x01, neg!!.requestedService)
        assertNull("unknown NRC must not be faked as a known one", neg.nrc)
        assertEquals(0xAB, neg.nrcCode)
    }

    @Test
    fun testParseNegativeResponseMalformed() {
        assertNull("too short to be a negative response", PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x03)))
        assertNull("positive response is not a negative response", PidCodec.parseNegativeResponse(byteArrayOf(0x41, 0x0C)))
    }

    @Test
    fun testParseNegativeResponseAcrossRequestedServices() {
        // §9 case set: 7F 01 xx, 7F 03 xx, 7F 09 xx, 7F 0A xx
        assertEquals(0x01, PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x01, 0x11))!!.requestedService)
        assertEquals(0x03, PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x03, 0x12))!!.requestedService)
        assertEquals(0x09, PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x09, 0x12))!!.requestedService)
        assertEquals(0x0A, PidCodec.parseNegativeResponse(byteArrayOf(0x7F, 0x0A, 0x13))!!.requestedService)
    }

    // ---- §10 Mode 09 PID-00 supported-PID discovery ----

    @Test
    fun testDecodeSupportedMode09PidsBitmap() {
        // byte0 bit6 -> PID 02 (VIN); byte0 bit2 -> PID 06 (CVN); byte1 bit6 -> PID 0x0A (ECU name)
        val bitmap = byteArrayOf(0x44, 0x40, 0x00, 0x00)
        assertEquals(setOf(0x02, 0x06, 0x0A), PidCodec.decodeSupportedMode09Pids(bitmap))
    }

    @Test
    fun testDecodeSupportedMode09PidsMalformed() {
        assertTrue(PidCodec.decodeSupportedMode09Pids(byteArrayOf(0x44, 0x40, 0x00)).isEmpty()) // < 4 bytes
        assertTrue(PidCodec.decodeSupportedMode09Pids(byteArrayOf()).isEmpty())
    }

    @Test
    fun testDtcParserRejectsNegativeResponseAsDtc() {
        // A 7F payload must NOT be parsed into fabricated DTC codes (e.g. P0312).
        val sevenF = byteArrayOf(0x7F, 0x03, 0x12) // 7F 03 12 = Mode 03 rejected, NRC 0x12
        assertTrue("7F 03 xx must yield no DTCs", DtcParser.parseMode03Payload(sevenF, DtcCategory.STORED).isEmpty())
        assertTrue("7F 07 xx must yield no pending DTCs", DtcParser.parseMode03Payload(byteArrayOf(0x7F, 0x07, 0x22), DtcCategory.PENDING).isEmpty())
    }

    @Test
    fun testMode0APermanentDtcCategoryIsModeled() {
        // §8.3: Mode 0A positive response: 4A echo + count=01 + P0300.
        val dtcs = DtcParser.parseMode03Payload(byteArrayOf(0x4A, 0x01, 0x03, 0x00), DtcCategory.PERMANENT)
        assertEquals(1, dtcs.size)
        assertEquals(DtcCategory.PERMANENT, dtcs[0].category)
    }

    // ---- §4.3 framing regression: no fabricated DTCs, no parity heuristic ----

    @Test
    fun testMode03EchoByteIsNeverParsedAsDtc() {
        // Canonical J1979: "43 02 01 15 03 00" must yield exactly [P0115, P0300].
        // The old parity heuristic mis-parsed the 0x43 echo as a bogus "C0302".
        val dtcs = DtcParser.parseMode03Payload(byteArrayOf(0x43, 0x02, 0x01, 0x15, 0x03, 0x00), DtcCategory.STORED)
        assertEquals(2, dtcs.size)
        assertTrue(dtcs.none { it.code.startsWith("C0") || it.code.startsWith("C3") })
    }

    @Test
    fun testMode03ZeroCountIsHealthyEmptyNotFabrication() {
        // 43 00 = scan succeeded, zero codes — truthfully empty, NOT a failure.
        assertTrue(DtcParser.parseMode03Payload(byteArrayOf(0x43, 0x00), DtcCategory.STORED).isEmpty())
    }

    @Test
    fun testMode03MismatchedEchoIsRejectedNotMisParsed() {
        // 0x41 is a Mode-01 echo, not a Mode-03 echo → malformed framing → no DTCs.
        // Must not be mis-parsed as (0x41,0x02)->"P0102", (0x15,0x03)->"C1503".
        val dtcs = DtcParser.parseMode03Payload(byteArrayOf(0x41, 0x02, 0x01, 0x15, 0x03, 0x00), DtcCategory.STORED)
        assertTrue("mismatched echo must not fabricate DTCs", dtcs.isEmpty())
    }

    @Test
    fun testMode03MalformedOddTailDoesNotFabricate() {
        // count claims 02, but only one complete pair precedes a dangling 0x03.
        // The dangling byte must be dropped, not zero-padded or mis-paired into a code.
        val dtcs = DtcParser.parseMode03Payload(byteArrayOf(0x43, 0x02, 0x01, 0x15, 0x03), DtcCategory.STORED)
        assertEquals(1, dtcs.size)
        assertEquals("P0115", dtcs[0].code)
    }

    // ---- §2 protocol-aware framing: non-CAN classic has NO count byte ----

    @Test
    fun testMode03NonCanClassicParsesPairsNotCount() {
        // §2 critical regression: non-CAN classic "43 01 33 00 00 00 00" — byte[1]=0x01
        // is the HIGH byte of P0133, NOT a count. Must yield [P0133], not P3300 or count=1.
        val dtcs = DtcParser.parseMode03Payload(
            byteArrayOf(0x43, 0x01, 0x33, 0x00, 0x00, 0x00, 0x00),
            DtcCategory.STORED, frame = DtcFrame.NON_CAN_PAIRS
        )
        assertEquals(1, dtcs.size)
        assertEquals("P0133", dtcs[0].code)
        assertTrue(dtcs.none { it.code.startsWith("P33") })
    }

    @Test
    fun testMode03NonCanPaddingZeroPairsAreDropped() {
        // 43 01 33 03 00 00 00 -> pairs (01,33)=P0133, (03,00)=P0300, (00,00) padding
        val dtcs = DtcParser.parseMode03Payload(
            byteArrayOf(0x43, 0x01, 0x33, 0x03, 0x00, 0x00, 0x00),
            DtcCategory.STORED, frame = DtcFrame.NON_CAN_PAIRS
        )
        assertEquals(listOf("P0133", "P0300"), dtcs.map { it.code })
    }

    @Test
    fun testMode03NonCanZeroCodesIsEmpty() {
        // 43 00 00 -> non-CAN: (00,00) is padding, no DTCs.
        assertTrue(
            DtcParser.parseMode03Payload(
                byteArrayOf(0x43, 0x00, 0x00), DtcCategory.STORED, frame = DtcFrame.NON_CAN_PAIRS
            ).isEmpty()
        )
    }

    @Test
    fun testMode03CanAndNonCanDifferOnSameBytes() {
        // Same bytes "43 01 33 00" mean different things by framing:
        //  CAN (count byte): count=1 -> pair(33,00) = P3300
        //  Non-CAN (no count): pair(01,33) = P0133
        val can = DtcParser.parseMode03Payload(
            byteArrayOf(0x43, 0x01, 0x33, 0x00), DtcCategory.STORED, frame = DtcFrame.CAN_WITH_COUNT
        )
        val nonCan = DtcParser.parseMode03Payload(
            byteArrayOf(0x43, 0x01, 0x33, 0x00), DtcCategory.STORED, frame = DtcFrame.NON_CAN_PAIRS
        )
        assertEquals(listOf("P3300"), can.map { it.code })
        assertEquals(listOf("P0133"), nonCan.map { it.code })
    }

    @Test
    fun testMode03NonCanMalformedTailDoesNotFabricate() {
        // 43 01 33 -> one complete pair (01,33)=P0133; no padding; nothing fabricated.
        val dtcs = DtcParser.parseMode03Payload(
            byteArrayOf(0x43, 0x01, 0x33), DtcCategory.STORED, frame = DtcFrame.NON_CAN_PAIRS
        )
        assertEquals(listOf("P0133"), dtcs.map { it.code })
    }

    // ---- §10 Mode 09 typed vehicle-information decoders (CALID / CVN / ECU name) ----

    @Test
    fun testMode09AsciiDecodersForCalidAndEcuName() {
        // Calibration ID (0904) and ECU name (090A) are textual ASCII identifiers;
        // decodeAsciiString preserves printable chars (0x20..0x7E) and drops padding.
        assertEquals("F12345678", PidCodec.decodeAsciiString("F12345678".toByteArray()))
        assertEquals("ABC-12345 XYZ", PidCodec.decodeAsciiString("ABC-12345 XYZ".toByteArray()))
    }

    @Test
    fun testDecodeCvnAsTypedBinaryValues() {
        // §7: CVN (Mode 09 PID 06) is 4-byte binary — NOT ASCII text.
        val single = PidCodec.decodeCvn(byteArrayOf(0x12, 0x34, 0xAB.toByte(), 0xCD.toByte()))
        assertEquals(listOf("1234ABCD"), single.values)

        // Multiple CVNs: two complete 4-byte values.
        val multi = PidCodec.decodeCvn(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xFF.toByte(), 0xEE.toByte(), 0xDD.toByte(), 0xCC.toByte()))
        assertEquals(listOf("01020304", "FFEEDDCC"), multi.values)

        // Trailing partial group (< 4 bytes) is malformed — dropped, not fabricated/padded.
        val partial = PidCodec.decodeCvn(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xAA.toByte(), 0xBB.toByte()))
        assertEquals(listOf("01020304"), partial.values)
    }

    @Test
    fun testDecodeCvnRejectsInvalidLength() {
        // A Cvn chunk shorter than 4 bytes must not be emitted as a CVN (would be fabrication).
        val only = PidCodec.decodeCvn(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        assertEquals(emptyList<String>(), only.values)
    }

    @Test
    fun testMode09TypedResultsPreserveRawAndResponder() {
        val vin = PidCodec.decodeVinResult(
            payload = "VF32AKFWF12345678".toByteArray(), responder = "7E8",
            messageCount = 1, multiFrameComplete = true
        )
        assertEquals(0x02, vin.pid)
        assertEquals("7E8", vin.responder)
        assertTrue("raw bytes preserved verbatim", vin.rawBytes.contentEquals("VF32AKFWF12345678".toByteArray()))
        assertEquals(1, vin.messageCount)
        assertTrue(vin.multiFrameComplete == true)
        assertTrue(vin.decoded is Mode09Decoded.Vin)
        assertEquals("VF32AKFWF12345678", (vin.decoded as Mode09Decoded.Vin).value)

        val cvn = PidCodec.decodeCvnResult(
            payload = byteArrayOf(0x12, 0x34, 0xAB.toByte(), 0xCD.toByte(), 0x01, 0x02, 0x03, 0x04), responder = "7E9"
        )
        assertEquals(0x06, cvn.pid)
        assertEquals("7E9", cvn.responder)
        assertTrue(cvn.multiFrameComplete == null)
        assertTrue(cvn.decoded is Mode09Decoded.Cvn)
        assertEquals(listOf("1234ABCD", "01020304"), (cvn.decoded as Mode09Decoded.Cvn).cvn.values)
    }

    @Test
    fun testMode09EcuNameAndCalidTypedResultsDistinctByPid() {
        val ecuName = PidCodec.decodeEcuNameResult("Bosch ME7.4".toByteArray(), responder = "7EA")
        assertEquals(0x0A, ecuName.pid)
        assertTrue(ecuName.decoded is Mode09Decoded.EcuName)
        assertEquals("Bosch ME7.4", (ecuName.decoded as Mode09Decoded.EcuName).value)

        val calid = PidCodec.decodeCalibrationIdResult("F12345678".toByteArray(), responder = "7E8")
        assertEquals(0x04, calid.pid)
        assertTrue(calid.decoded is Mode09Decoded.CalibrationId)
        assertEquals("F12345678", (calid.decoded as Mode09Decoded.CalibrationId).value)
    }
}
