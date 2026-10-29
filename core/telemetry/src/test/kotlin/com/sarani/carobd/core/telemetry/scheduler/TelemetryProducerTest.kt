package com.sarani.carobd.core.telemetry.scheduler

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.TelemetryPollMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §5 single-producer telemetry: exactly ONE coroutine polls for a session;
 * consumers observe a hot StateFlow and never start their own loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryProducerTest {

    // "0100" advertises PIDs 01..20 EXCEPT 0x20 (byte3 = 0x00) -> no 0120 probe.
    private val responses = mapOf(
        "0100" to "41 00 FF FF FF 00",
        "010C" to "41 0C 1A F8",   // RPM 1726
        "010D" to "41 0D 55",      // speed 85
        "0105" to "41 05 82",      // coolant 90
        "0104" to "41 04 80",      // engine load ~50.2
        "010B" to "41 0B 32",      // MAP 50
        "0110" to "41 10 02 87",   // MAF 6.47
        "0111" to "41 11 80",      // throttle
    )

    @Test
    fun twoObserversStillOneCommandSequence() = runTest {
        val calls = mutableListOf<String>()
        var connected = true
        val exec: suspend (String, Long) -> String? = { cmd, _ ->
            if (!connected) null
            else {
                calls.add(cmd)
                // The sample callback ends the session after one complete cycle.
                responses[cmd]
            }
        }
        val mode = MutableStateFlow(TelemetryPollMode.NORMAL)
        val prod = TelemetryProducer(mode, exec, { connected }, onTelemetry = { connected = false })

        // Two observers attach BEFORE the producer starts. StateFlow emits its
        // current value on collect, so each collector first sees the initial
        // value, then the produced sample. `take(2)` makes the collectors
        // FINITE (they complete) so they cannot orphan-suspend at teardown.
        val obs1 = mutableListOf<LiveTelemetry>()
        val obs2 = mutableListOf<LiveTelemetry>()
        launch { prod.liveTelemetry.take(2).collect { obs1.add(it) } }
        launch { prod.liveTelemetry.take(2).collect { obs2.add(it) } }

        prod.startIn(this)
        advanceUntilIdle()

        // One cycle = 1 capability probe (0100) + 7 capability-gated polls = 8.
        // Two observers must NOT double the command stream.
        assertEquals(8, calls.size)
        assertTrue("observer 1 received the sample", obs1.size >= 2) // initial + produced
        assertTrue("observer 2 received the same sample", obs2.size >= 2)
    }

    @Test
    fun disconnectedBeforeStartCancelsPoll() = runTest {
        val calls = mutableListOf<String>()
        val exec: suspend (String, Long) -> String? = { cmd, _ -> calls.add(cmd); responses[cmd] }
        val mode = MutableStateFlow(TelemetryPollMode.NORMAL)
        val prod = TelemetryProducer(mode, exec, isConnected = { false })
        prod.startIn(this)
        advanceUntilIdle()
        assertEquals(0, calls.size)
        assertFalse(prod.isRunning)
    }

    @Test
    fun reconnectCancelsPreviousProducer() = runTest {
        val calls = mutableListOf<String>()
        var connected = true
        val exec: suspend (String, Long) -> String? = { cmd, _ ->
            if (!connected) null else { calls.add(cmd); responses[cmd] }
        }
        val mode = MutableStateFlow(TelemetryPollMode.NORMAL)
        val prod = TelemetryProducer(mode, exec, { connected })

        val old = prod.startIn(this)
        // Run exactly one cycle, then suspend at the cycle delay.
        advanceTimeBy(50)
        assertEquals(8, calls.size)            // one cycle (no more yet)

        val new = prod.startIn(this)           // reconnect -> cancels old producer
        assertTrue("old producer must be cancelled on reconnect", old.isCancelled)
        assertTrue("new producer must be active", new.isActive)
        assertSame(new, prod.activeJob)

        advanceTimeBy(50)                      // new producer runs exactly one more cycle
        assertEquals(16, calls.size)         // 8 (old) + 8 (new single cycle)
        prod.stop()
    }

    @Test
    fun pollModeDrivesPacingNotDuplicated() = runTest {
        // §7: changing the persisted mode is read each cycle. With two observers
        // and a FAST mode, one cycle still produces exactly one command sequence.
        val calls = mutableListOf<String>()
        var connected = true
        val exec: suspend (String, Long) -> String? = { cmd, _ ->
            if (!connected) null
            else {
                calls.add(cmd)
                                responses[cmd]
            }
        }
        val mode = MutableStateFlow(TelemetryPollMode.FAST)
        val prod = TelemetryProducer(mode, exec, { connected }, onTelemetry = { connected = false })
        launch { prod.liveTelemetry.take(2).collect { } }
        launch { prod.liveTelemetry.take(2).collect { } }
        prod.startIn(this)
        advanceUntilIdle()
        assertEquals(8, calls.size)
    }

    @Test
    fun producer_disconnectMidCycleStopsFurtherDispatchOrEmission() = runTest {
        val calls = mutableListOf<String>()
        var connected = true
        var emissions = 0
        val prod = TelemetryProducer(MutableStateFlow(TelemetryPollMode.NORMAL), { cmd, _ ->
            calls.add(cmd)
            if (cmd == "0100") connected = false
            responses[cmd]
        }, { connected }, onTelemetry = { emissions++ })
        prod.startIn(this)
        advanceUntilIdle()
        assertEquals(listOf("0100"), calls)
        assertEquals(0, emissions)
        assertFalse(prod.isRunning)
    }
}
