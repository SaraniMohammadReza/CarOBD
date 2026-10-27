package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.PidCapabilityMap
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PidPollingPlanTest {

    private fun hex(hex: String): ByteArray = PidCodec.cleanResponseHex(hex)
    private fun spec(h: String) = TelemetryPollSpec("01$h", h)
    private fun decode(h: String, bytesHex: String): LiveTelemetry? =
        PidPollingPlan.decodePoll(spec(h), hex(bytesHex), LiveTelemetry())

    // ---- §22 discovery scenarios ----

    @Test
    fun discoverParsesPage00AndAdvertisesCorePids() = runTest {
        val caps = PidPollingPlan.discover { cmd -> if (cmd == "0100") "41 00 BE 3E B8 11" else null }
        // 0x3E byte advertises pids 11-15 (incl. 0C/0D/0B); 0xB8 advertises 17/19/20/21.
        assertTrue(caps.isSupported("0C"))
        assertTrue(caps.isSupported("0D"))
        assertTrue(caps.isSupported("0B"))
        assertTrue(caps.isSupported("05"))
        assertTrue(caps.isSupported("04"))
        assertTrue(caps.isSupported("11"))
        // 0x3E omits bit0 -> pid 16 (MAF) is NOT advertised by this bitmap.
        assertFalse(caps.isSupported("10"))
        assertEquals("BE 3E B8 11", caps.rawBitmapResponses["00"])
    }

    @Test
    fun discoverMultiPageAggregatesAcrossBitmaps() = runTest {
        val caps = PidPollingPlan.discover { cmd ->
            when (cmd) {
                "0100" -> "41 00 BE 3E B8 11"
                "0120" -> "41 20 80 05 B0 11" // byte0 0x80 -> pid 0x21 (33)
                else -> null
            }
        }
        assertTrue(caps.isSupported("0C"))   // from page 00
        assertTrue(caps.isSupported("21"))   // from page 20
    }

    @Test
    fun discoverExtensionBitPage0160() = runTest {
        val caps = PidPollingPlan.discover { cmd -> if (cmd == "0160") "41 60 80 00 00 00" else null }
        // base 0x60 = 96; byte0 0x80 (bit7) -> pid 97 = 0x61
        assertTrue(caps.isSupported("61"))
    }

    @Test
    fun discoverAllPagesAbsentYieldsEmptyMap() = runTest {
        val caps = PidPollingPlan.discover { null }
        assertTrue(caps.supportedPids.isEmpty())
    }

    @Test
    fun discoverNegativeResponse7FYieldsEmptyMap() = runTest {
        // 7F is an ECU rejection, not a capability denial; the page is skipped.
        val caps = PidPollingPlan.discover { cmd -> if (cmd == "0100") "7F 00 7F 12 13 14" else null }
        assertTrue(caps.supportedPids.isEmpty())
    }

    @Test
    fun discoverMalformedShortFrameIsSkipped() = runTest {
        val caps = PidPollingPlan.discover { cmd -> if (cmd == "0100") "41 00 BE" else null }
        assertTrue(caps.supportedPids.isEmpty())
    }

    // ---- §9 gating ----

    @Test
    fun supportedGatingDropsUnsupportedPids() {
        // Bitmap advertises every baseline PID EXCEPT pid 16 (MAF).
        val caps = PidCapabilityMap.parseFromBitmaps(
            mapOf("00" to listOf(0xBE, 0x3E, 0xB8, 0x11))
        )
        val plan = PidPollingPlan.supported(caps)
        assertFalse("MAF (pid 10) not advertised -> must be gated out (§21)", plan.any { it.pidHex == "10" })
        assertTrue("RPM (pid 0C) advertised -> must remain", plan.any { it.pidHex == "0C" })
        assertEquals("all baseline PIDs except MAF", PidPollingPlan.BASELINE.size - 1, plan.size)
    }

    @Test
    fun supportedFallsBackToBaselineWhenEmptyOrUnknown() {
        assertEquals(PidPollingPlan.BASELINE, PidPollingPlan.supported(null))
        assertEquals(PidPollingPlan.BASELINE, PidPollingPlan.supported(PidCapabilityMap()))
    }

    @Test
    fun supportedEmptyMapFallsBackToBaseline() {
        // An ECU that answered 0100 but advertised nothing still gets the
        // baseline set so telemetry is never silently dropped below prior behaviour.
        val caps = PidCapabilityMap.parseFromBitmaps(mapOf("00" to listOf(0, 0, 0, 0)))
        assertTrue(caps.supportedPids.isEmpty())
        assertEquals(PidPollingPlan.BASELINE, PidPollingPlan.supported(caps))
    }

    // ---- decodePoll round-trips & truthfulness (no 0 from unknown) ----

    @Test
    fun decodePollRoundTripsEachPid() {
        assertEquals(1726, decode("0C", "41 0C 1A F8")!!.rpm)
        assertEquals(85, decode("0D", "41 0D 55")!!.speedKmh)
        assertEquals(50.196f, decode("11", "41 11 80")!!.throttlePositionPercent, 0.001f)
        assertEquals(90, decode("05", "41 05 82")!!.coolantTempCelsius)
        assertEquals(50.196f, decode("04", "41 04 80")!!.engineLoadPercent, 0.001f)
        assertEquals(50, decode("0B", "41 0B 32")!!.intakeManifoldPressureKpa)
        assertEquals(6.47f, decode("10", "41 10 02 87")!!.mafGramsPerSec, 0.001f)
    }

    @Test
    fun decodePollRejectsPidMismatchAndShortFrames() {
        // PID mismatch (0D where 0C expected) -> null (unmeasured, never 0).
        assertNull(decode("0C", "41 0D 55"))
        // Too short for header -> null.
        assertNull(decode("0C", "41 0C"))
        // Non-positive (7F) is not 0x41 -> null.
        assertNull(decode("0C", "7F 0C 12 13"))
    }

    @Test
    fun decodePollDoesNotMutateMeasuredField() {
        val out = decode("0C", "41 0C 1A F8")
        assertNotNull(out)
        assertTrue(out!!.measuredPids.isEmpty()) // decode does not fabricate measurements
    }
}
