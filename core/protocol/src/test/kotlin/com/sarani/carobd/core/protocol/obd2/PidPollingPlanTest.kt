package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.PidCapabilityMap
import com.sarani.carobd.core.model.PidSupportState
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

    // ---- §6-P0-A / §22 discovery scenarios ----

    @Test
    fun discoverParsesPage00AndAdvertisesCorePids() = runTest {
        val state = PidPollingPlan.discover { cmd -> if (cmd == "0100") "41 00 BE 3E B8 11" else null }
        assertTrue(state is PidSupportState.Known)
        val caps = (state as PidSupportState.Known).map
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
    fun discover0100SuccessNoExtension() = runTest {
        // 0100 succeeds, 0x20 NOT advertised → no 0120 probe.
        val requested = mutableListOf<String>()
        val state = PidPollingPlan.discover { cmd ->
            requested.add(cmd)
            if (cmd == "0100") "41 00 BE 3E B8 00" else null
        }
        assertEquals(listOf("0100"), requested)
        assertTrue(state is PidSupportState.Known)
    }

    @Test
    fun discoverMultiPageAggregatesAcrossBitmaps() = runTest {
        val state = PidPollingPlan.discover { cmd ->
            when (cmd) {
                "0100" -> "41 00 BE 3E B8 11"
                "0120" -> "41 20 80 05 B0 11" // byte0 0x80 -> pid 0x21 (33)
                else -> null
            }
        }
        assertTrue(state is PidSupportState.Known)
        val caps = (state as PidSupportState.Known).map
        assertTrue(caps.isSupported("0C"))   // from page 00
        assertTrue(caps.isSupported("21"))   // from page 20
    }

    @Test
    fun discoverExtensionBitPage0160() = runTest {
        // §6 lazy extension: each page must advertise its boundary PID (0x20, 0x40,
        // 0x60) for the next page to be requested. byte3 bit0 of each page carries
        // that boundary PID.
        val state = PidPollingPlan.discover { cmd ->
            when (cmd) {
                "0100" -> "41 00 00 00 00 01"   // advertises 0x20 -> next page
                "0120" -> "41 20 00 00 00 01"   // advertises 0x40 -> next page
                "0140" -> "41 40 00 00 00 01"   // advertises 0x60 -> next page
                "0160" -> "41 60 80 00 00 00"   // byte0 0x80 -> pid 0x61
                else -> null
            }
        }
        assertTrue(state is PidSupportState.Known)
        val caps = (state as PidSupportState.Known).map
        // base 0x60; byte0 0x80 (bit7) -> pid 0x61
        assertTrue(caps.isSupported("61"))
    }

    @Test
    fun discoverProbesLazilyAndNeverRequestsUnsupportedPages() = runTest {
        val requested = mutableListOf<String>()
        // 0100 advertises pids 01-08 only (byte0=0xFF); 0x20 NOT supported.
        val state = PidPollingPlan.discover { cmd ->
            requested.add(cmd)
            if (cmd == "0100") "41 00 FF 00 00 00" else null
        }
        // Only the baseline page was asked — 0120/0140.. were NOT probed.
        assertEquals(listOf("0100"), requested)
        assertTrue(state is PidSupportState.Known)
        val caps = (state as PidSupportState.Known).map
        assertTrue(caps.isSupported("01"))
        assertTrue(caps.isSupported("08"))
        assertFalse(caps.isSupported("20"))
        assertFalse(caps.isSupported("0C")) // not in this bitmap
    }

    @Test
    fun discoverStopsWhenExtensionBitAbsent() = runTest {
        val requested = mutableListOf<String>()
        PidPollingPlan.discover { cmd ->
            requested.add(cmd)
            // 0100 does NOT advertise 0x20 (byte3 = 0x00).
            if (cmd == "0100") "41 00 BE 3E B8 00" else null
        }
        assertEquals(listOf("0100"), requested)
    }

    // ---- §6 failure scenarios: result is Unknown, NOT baseline ----

    @Test
    fun discoverNoDataAndTimeoutYieldUnknown() = runTest {
        // NO DATA (null) on baseline page → Unknown, no further pages probed.
        val requested = mutableListOf<String>()
        val state = PidPollingPlan.discover { cmd ->
            requested.add(cmd)
            null  // simulates timeout / NO DATA
        }
        assertEquals(listOf("0100"), requested)
        assertTrue("NO DATA/timeout must yield Unknown, not Known", state is PidSupportState.Unknown)
    }

    @Test
    fun discover7FOn0100YieldsUnknown() = runTest {
        // 7F is an ECU rejection, not a capability denial.
        val state = PidPollingPlan.discover { cmd ->
            if (cmd == "0100") "7F 00 7F 12 13 14" else null
        }
        assertTrue("7F must yield Unknown", state is PidSupportState.Unknown)
    }

    @Test
    fun discoverMalformed0100YieldsUnknown() = runTest {
        val state = PidPollingPlan.discover { cmd ->
            if (cmd == "0100") "41 00 BE" else null  // too short (3 bytes)
        }
        assertTrue("malformed 0100 must yield Unknown", state is PidSupportState.Unknown)
    }

    @Test
    fun discoverWrongSID0100YieldsUnknown() = runTest {
        // 0xC3 is the negative echo for Mode 03, not a valid 0100 response.
        val state = PidPollingPlan.discover { cmd ->
            if (cmd == "0100") "C3 00 00 00 00 00" else null
        }
        assertTrue("wrong-SID 0100 must yield Unknown", state is PidSupportState.Unknown)
    }

    // ---- §6-P0-A gating: capability unknown != baseline supported ----

    @Test
    fun supportedGatingDropsUnsupportedPids() {
        // Bitmap advertises every baseline PID EXCEPT pid 16 (MAF).
        val caps = PidCapabilityMap.parseFromBitmaps(
            mapOf("00" to listOf(0xBE, 0x3E, 0xB8, 0x11))
        )
        val plan = PidPollingPlan.supported(PidSupportState.Known(caps))
        assertFalse("MAF (pid 10) not advertised -> must be gated out (§21)", plan.any { it.pidHex == "10" })
        assertTrue("RPM (pid 0C) advertised -> must remain", plan.any { it.pidHex == "0C" })
        assertEquals("all baseline PIDs except MAF", PidPollingPlan.BASELINE.size - 1, plan.size)
    }

    @Test
    fun supportedUnknownReturnsEmptyNotBaseline() {
        // §6-P0-A: Unknown capability must NOT fall back to BASELINE.
        assertEquals(emptyList<TelemetryPollSpec>(), PidPollingPlan.supported(PidSupportState.Unknown("test")))
    }

    @Test
    fun supportedNotDiscoveredReturnsEmpty() {
        // §6-P0-A: discovery not yet attempted → conservative empty set.
        assertEquals(emptyList<TelemetryPollSpec>(), PidPollingPlan.supported(PidSupportState.NotDiscovered))
    }

    @Test
    fun supportedKnownEmptyMapReturnsEmpty() {
        // An ECU that answered 0100 but advertised nothing (all-zero bitmap).
        val caps = PidCapabilityMap.parseFromBitmaps(mapOf("00" to listOf(0, 0, 0, 0)))
        assertTrue(caps.supportedPids.isEmpty())
        assertEquals(emptyList<TelemetryPollSpec>(), PidPollingPlan.supported(PidSupportState.Known(caps)))
    }

    @Test
    fun supportedKnownFullMapReturnsAllBaseline() {
        // ECU advertises ALL baseline PIDs → full BASELINE set.
        val caps = PidCapabilityMap.parseFromBitmaps(
            mapOf("00" to listOf(0xFF, 0xFF, 0xFF, 0xFF))
        )
        val plan = PidPollingPlan.supported(PidSupportState.Known(caps))
        assertEquals(PidPollingPlan.BASELINE.toSet(), plan.toSet())
        assertEquals(PidPollingPlan.BASELINE.size, plan.size)
    }

    @Test
    fun noRepeatedUnsupportedRequestLoop() = runTest {
        // §6: when capability is Unknown, the producer must NOT poll any PIDs.
        // Simulate a session where 0100 fails: only 1 command sent, 0 polled.
        val requested = mutableListOf<String>()
        val state = PidPollingPlan.discover { cmd ->
            requested.add(cmd)
            null  // 0100 fails → Unknown
        }
        assertEquals(listOf("0100"), requested)
        val plan = PidPollingPlan.supported(state)
        assertTrue("Unknown capability → no polling commands", plan.isEmpty())
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
