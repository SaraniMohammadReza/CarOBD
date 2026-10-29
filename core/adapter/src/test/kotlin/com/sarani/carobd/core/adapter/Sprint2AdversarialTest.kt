package com.sarani.carobd.core.adapter

import com.sarani.carobd.core.adapter.command.*
import com.sarani.carobd.core.adapter.elm327.*
import com.sarani.carobd.core.adapter.stn.StnAdapter
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class Sprint2AdversarialTest {
    private class Wire : ObdTransport {
        override val transportType = TransportType.WIFI_TCP
        override var isConnected = true
        val rx = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
        override val incomingBytes = rx.asSharedFlow()
        val attempts = mutableListOf<String>()
        override suspend fun connect() { isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) { attempts.add(data.toString(Charsets.US_ASCII)) }
    }
    @Test fun unsafeCorpusAndStnHelpersNeverWrite() = runTest {
        val t = Wire(); val a = StnAdapter(t, backgroundScope)
        val unsafe = listOf("04", "08", "2701", "2EF19001", "2F0100", "3101", "3400", "3500", "3601", "37", "14FFFFFF", "1101", "1002", "220101", "ATSH7E0", "ATSP6", "STPX h:7E0,d:04", "", "0", "010", "GG", "01"+"00".repeat(10000))
        val separators = listOf("\r", "\n", "\r\n", "\u0000", "\t", ";", "\u00a0", "\u2003") + (0..31).map { it.toChar().toString() }
        for (c in unsafe + separators.flatMap { sep -> listOf("ATI${sep}04", "0902${sep}2E F1 90 01", "ATI${sep}2701") }) {
            assertTrue(c, a.executeCommand(c) is AdapterResponse.Error)
        }
        assertFalse(a.setHeader("7E0\r04")); assertFalse(a.selectProtocol(ProtocolStandard.AUTO))
        assertFalse(a.resetCanFilters()); assertFalse(a.addCanFilter("7E0", "7FF"))
        assertFalse(a.setHighSpeedBaud(115200))
        assertTrue(a.transmitMultiFrame("7E0", "04") is AdapterResponse.Error)
        assertTrue(a.clearSimulatorDiagnosticState() is AdapterResponse.Error)
        assertTrue(t.attempts.isEmpty())
        a.disconnect()
    }
    @Test fun completeMixedBufferRetainsPositiveAndErrorResponders() {
        val r = Elm327BufferParser.parseBuffer("7E8 43 01 01 33\r7E9 43 01 01 33\r7EA NO DATA\r>", "03", 1)
        val envelopes = r.toWireEnvelopes("03")
        assertEquals(3, envelopes.size)
        assertEquals(2, envelopes.count { it.result is DiagnosticResult.Positive })
        assertTrue(envelopes.any { it.responderId == "7EA" && it.result is DiagnosticResult.NoData })
    }
    @Test fun cancellationAtPartialResponseAndNewGeneration() = runTest {
        for (partial in listOf("", "41 0C", "41 0C 1A F8\r")) {
            val old = Wire(); val a = Elm327Adapter(old, backgroundScope)
            val pending = launch { a.executeCommand("010C", 1000) }
            runCurrent()
            old.rx.emit(partial.toByteArray()); runCurrent()
            pending.cancelAndJoin()
            assertTrue(a.executeCommand("010D") is AdapterResponse.Error)
            val fresh = Wire(); val b = Elm327Adapter(fresh, backgroundScope)
            val next = async { b.executeCommand("010D") }; runCurrent()
            old.rx.emit("41 0C 1A F8\r>".toByteArray()); runCurrent()
            assertFalse(next.isCompleted)
            fresh.rx.emit("41 0D 55\r>".toByteArray()); runCurrent()
            assertEquals("41 0D 55", (next.await() as AdapterResponse.Success).rawResponse)
            assertEquals(listOf("010C\r"), old.attempts)
            a.disconnect(); b.disconnect()
        }
    }
    @Test fun concurrentReadsAreSerializedWithoutCrossCompletion() = runTest {
        val t = Wire(); val adapter = Elm327Adapter(t, backgroundScope)
        val requests = listOf("010C", "010D", "03", "ATI")
        val jobs = requests.map { c -> async { adapter.executeCommand(c) } }
        val responses = listOf("41 0C 1A F8", "41 0D 55", "43 00", "ELM327")
        for (index in requests.indices) {
            advanceTimeBy(60); runCurrent()
            assertEquals(index + 1, t.attempts.size)
            t.rx.emit((responses[index] + "\r>").toByteArray()); runCurrent()
            assertEquals(responses[index], (jobs[index].await() as AdapterResponse.Success).rawResponse)
        }
        assertEquals(requests.map { it + "\r" }, t.attempts)
        adapter.disconnect()
    }

    @Test fun completedInboundStreamRetiresEvenIfTransportFlagWasNotCleared() = runTest {
        val wire = Wire()
        val ended = object : ObdTransport by wire { override val incomingBytes = emptyFlow<ByteArray>() }
        val adapter = Elm327Adapter(ended, backgroundScope)
        runCurrent()
        assertFalse(wire.isConnected)
        assertTrue(adapter.executeCommand("0100") is AdapterResponse.Error)
        assertTrue(wire.attempts.isEmpty())
        adapter.disconnect()
    }

}
