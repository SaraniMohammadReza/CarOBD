package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class ReplayFidelityTest {
    private fun event(d: Direction, time: Long, raw: String, meta: Map<String, String> = emptyMap()) = DiagnosticEvent(d, time, time, if (d == Direction.TX) raw.removeSuffix("\r") else null, raw, protocolMetadata = meta)
    private fun transcript(vararg e: DiagnosticEvent) = DiagnosticTranscript(TranscriptMetadata("record", TransportType.WIFI_TCP), e.toList())
    @Test fun replayCommandCorrelationAndNoPhysicalTx() = runTest {
        val t = transcript(event(Direction.TX, 0, "0100\r"), event(Direction.RX, 10, "410000000000\r>"))
        val replay = ReplayTransport(t, parentScope = backgroundScope)
        replay.connect()
        assertEquals(TransportType.SIMULATOR, replay.transportType)
        try { replay.write("03\r".toByteArray()); fail("Mismatched TX must fail") } catch (_: IllegalStateException) {}
        val frames = mutableListOf<ByteArray>()
        backgroundScope.launch { replay.incomingBytes.collect { frames += it } }
        advanceTimeBy(100); runCurrent()
        assertTrue("RX cannot precede TX", frames.isEmpty())
        replay.write("0100\r".toByteArray()); advanceTimeBy(10); runCurrent()
        assertEquals("410000000000\r>", frames.single().toString(Charsets.US_ASCII))
        replay.disconnect()
    }
    @Test fun replayLatePromptAndTimeoutQuarantine() = runTest {
        val t = transcript(event(Direction.TX, 0, "010C\r"), event(Direction.RX, 150, "410C1AF8\r>"),
            event(Direction.TX, 200, "03\r"), event(Direction.RX, 220, "430000\r>"))
        val r = TranscriptReplayRunner().run(t, 100)
        assertEquals(2, r.size)
        assertEquals(DiagnosticResult.Timeout, r[0].evidence.single().result)
        assertTrue(r[1].evidence.single().result is DiagnosticResult.Positive)
    }
    @Test fun replayTimeoutCannotBecomeNoCodes() = runTest {
        val r = TranscriptReplayRunner().run(transcript(event(Direction.TX, 0, "03\r")), 100)
        assertEquals(DiagnosticResult.Timeout, r.single().evidence.single().result)
    }
    @Test fun replayMultiResponder() = runTest {
        val r = TranscriptReplayRunner().run(transcript(event(Direction.TX, 0, "03\r"), event(Direction.RX, 10, "7E8 430133\r7E9 7F 03 12\r>")))
        assertEquals(listOf("7E8", "7E9"), r.single().evidence.map { it.responderId })
    }
    @Test fun replayDisconnect() = runTest {
        val r = TranscriptReplayRunner().run(transcript(event(Direction.TX, 0, "03\r"), event(Direction.ERR, 10, "", mapOf("event" to "disconnect"))))
        assertTrue(r.single().evidence.single().result is DiagnosticResult.AdapterFailure)
        assertEquals("Replay disconnected", r.single().detail)
    }
    @Test fun recordingStoreRoundTripAndPathSafety() {
        val dir = Files.createTempDirectory("transcript-store").toFile()
        try {
            val store = FileTranscriptStore(dir)
            val t = transcript(event(Direction.TX, 0, "03\r"), event(Direction.RX, 10, "430000\r>"))
            store.save(t)
            assertEquals(t, FileTranscriptStore(dir).read(t.metadata.sessionId))
            assertEquals(1, store.list().size)
            assertThrows(IllegalArgumentException::class.java) { store.read("../secret") }
            store.save(t.copy(events = t.events.take(1)))
            assertEquals(2, store.read(t.metadata.sessionId).events.size)
            assertTrue(store.delete(t.metadata.sessionId))
        } finally { dir.deleteRecursively() }
    }
}
