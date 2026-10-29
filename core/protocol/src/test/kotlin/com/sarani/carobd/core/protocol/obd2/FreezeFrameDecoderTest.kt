package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test

class FreezeFrameDecoderTest {
    private fun read(pid: Int, frame: Int, raw: String) = ElmResponseEnvelopeParser.parse(FreezeFrameDecoder.command(pid, frame), raw.lines(), 0, 0).map { FreezeFrameDecoder.decode(pid, frame, it) }
    @Test fun frame0_knownPid() { val r = read(12, 0, "420C001AF8").single(); assertEquals(1726.0, r.value!!.value!!, 0.0); assertEquals("rpm", r.value!!.unit) }
    @Test fun nonzeroFrameIndex() { assertEquals(7, read(13, 7, "420D0755").single().value!!.frameIndex) }
    @Test fun multipleResponderFreezeFrame() { assertEquals(listOf("7E8", "7E9"), read(13, 0, "7E8 420D0055\n7E9 420D0040").map { it.evidence.single().responderId }) }
    @Test fun negativeResponse() { assertEquals(ReadingState.NegativeResponse, read(13, 0, "7F 02 12").single().state) }
    @Test fun noData() { assertEquals(ReadingState.NoData, read(13, 0, "NO DATA").single().state) }
    @Test fun timeout() {
        val e = ElmResponseEnvelopeParser.parse("020D00", listOf("NO DATA"), 0, 0).single().copy(result = DiagnosticResult.Timeout)
        assertEquals(ReadingState.Timeout, FreezeFrameDecoder.decode(13, 0, e).state)
    }
    @Test fun malformed() {
        for (raw in listOf("420C001A", "420C011AF8", "410C1AF8", "420D005500")) {
            val pid = if (raw.startsWith("420D")) 13 else 12
            assertEquals(raw, ReadingState.Malformed, read(pid, 0, raw).single().state)
        }
    }
    @Test fun unsupportedPidPreservesRaw() { val r = read(0x15, 2, "4215021234").single(); assertEquals(ReadingState.Unsupported, r.state); assertNull(r.value!!.value); assertEquals("1234", r.value!!.rawHex) }
}
