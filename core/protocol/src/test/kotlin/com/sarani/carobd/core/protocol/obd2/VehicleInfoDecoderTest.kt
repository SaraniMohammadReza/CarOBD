package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test

class VehicleInfoDecoderTest {
    private val vin = "1D4GP00R55B123456"
    private fun hex(s: String) = s.toByteArray().joinToString("") { "%02X".format(it) }
    private fun read(pid: Int, raw: String) = VehicleInfoDecoder.decode(pid, ElmResponseEnvelopeParser.parse("09%02X".format(pid), raw.lines(), 0, 0))
    @Test fun singleVin() { assertEquals(listOf(vin), read(2, "490201" + hex(vin)).single().value!!.values) }
    @Test fun multiFrameVin() {
        val raw = "014\n0: 49 02 01 31 44 34\n1: 47 50 30 30 52 35 35\n2: 42 31 32 33 34 35 36"
        val r = read(2, raw).single()
        assertEquals(ReadingState.Success, r.state)
        assertEquals(listOf(vin), r.value!!.values)
        assertEquals(4, r.evidence.single().rawLines.size)
        assertNull(r.evidence.single().responderId)
        assertEquals(ReadingState.Malformed, read(2, raw.replace("1:", "2:")).single().state)
    }
    @Test fun nonCanVinRequiresSequenceAndCount() {
        val raw = listOf("49020100000031", "49020244344750", "49020330305235", "49020435423132", "49020533343536")
        val env = ElmResponseEnvelopeParser.parse("0902", raw, 0, 0)
        assertEquals(listOf(vin), VehicleInfoDecoder.decode(2, env, mapOf(null to 5)).single().value!!.values)
        assertEquals(ReadingState.Malformed, VehicleInfoDecoder.decode(2, env, mapOf(null to 4)).single().state)
        assertEquals(ReadingState.Malformed, VehicleInfoDecoder.decode(2, env + env, mapOf(null to 5)).single().state)
    }
    @Test fun invalidVinCount() { assertEquals(ReadingState.Malformed, read(2, "490202" + hex(vin)).single().state) }
    @Test fun partialVin() { val r = read(2, "490201" + hex(vin.dropLast(1))).single(); assertEquals(ReadingState.Partial, r.state); assertNull(r.value) }
    @Test fun malformedVinNeverDropsGarbage() {
        assertEquals(ReadingState.Malformed, read(2, "490201" + hex(vin + "X")).single().state)
        assertEquals(ReadingState.Malformed, read(2, "490201" + hex("I" + vin.drop(1))).single().state)
        assertNull(PidCodec.decodeVin("PRINTABLE GARBAGE".toByteArray()))
    }
    @Test fun multipleCalid() { assertEquals(listOf("CAL-A", "CAL-B"), read(4, "490402" + hex("CAL-A".padEnd(16, '\u0000') + "CAL-B".padEnd(16, '\u0000'))).single().value!!.values) }
    @Test fun multipleCvnAndTrailingPartial() {
        assertEquals(listOf("1234ABCD", "01020304"), read(6, "4906021234ABCD01020304").single().value!!.values)
        assertEquals(ReadingState.Partial, read(6, "4906021234ABCD0102").single().state)
        val legacy = PidCodec.decodeCvn(byteArrayOf(1, 2, 3, 4, 5))
        assertEquals(ReadingState.Partial, legacy.quality)
        assertEquals(listOf(5), legacy.trailingBytes)
    }
    @Test fun ecuName() { assertEquals(listOf("Engine ECU"), read(10, "490A01" + hex("Engine ECU".padEnd(20, '\u0000'))).single().value!!.values) }
    @Test fun multipleRespondersNeverMerged() {
        val r = read(2, "7E8 490201" + hex(vin) + "\n7E9 7F 09 12")
        assertEquals(listOf(ReadingState.Success, ReadingState.NegativeResponse), r.map { it.state })
        assertEquals(listOf("7E8", "7E9"), r.map { it.evidence.single().responderId })
    }
    @Test fun noDataNegativeMalformed() {
        assertEquals(ReadingState.NoData, read(2, "NO DATA").single().state)
        assertEquals(ReadingState.NegativeResponse, read(2, "7F 09 12").single().state)
        assertEquals(ReadingState.Malformed, read(2, "garbage").single().state)
    }
}
