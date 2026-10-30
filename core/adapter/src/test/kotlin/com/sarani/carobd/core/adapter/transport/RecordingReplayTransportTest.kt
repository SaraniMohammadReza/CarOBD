package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.Direction
import com.sarani.carobd.core.model.DiagnosticEvent
import com.sarani.carobd.core.model.DiagnosticOutcome
import com.sarani.carobd.core.model.DiagnosticTranscript
import com.sarani.carobd.core.model.DiagnosticTranscriptBuilder
import com.sarani.carobd.core.model.TransportType
import com.sarani.carobd.core.model.TranscriptMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §38 infrastructure: a RecordingTransport wraps any ObdTransport and snapshots
 * TX/RX/ERR into a DiagnosticTranscriptBuilder; a ReplayTransport then re-emits a
 * transcript's RX frames (as ASCII wire bytes) so the same parsing stack can
 * validate a recording.
 *
 * The OBD wire (ELM327) is ASCII hex text, so raw frames are carried as the ASCII
 * bytes of the hex string — not decoded binary.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordingReplayTransportTest {

    private class FakeTransport(
        val incoming: List<ByteArray>,
        override val transportType: TransportType = TransportType.SIMULATOR
    ) : ObdTransport {
        val writes = mutableListOf<ByteArray>()
        private var connected = false
        override val isConnected: Boolean get() = connected
        override suspend fun connect() { connected = true }
        override suspend fun disconnect() { connected = false }
        override suspend fun write(data: ByteArray) { writes.add(data) }
        override val incomingBytes: Flow<ByteArray> = flow { for (b in incoming) emit(b) }
    }

    private suspend fun Flow<ByteArray>.drain(scope: TestScope): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        val job = scope.launch { toList(out) }
        scope.runCurrent(); scope.testScheduler.advanceTimeBy(100); scope.runCurrent(); scope.advanceUntilIdle()
        job.cancel()
        return out
    }

    @Test
    fun recordingCapturesTxAndRx() = runTest {
        // FakeTransport emits real ASCII wire bytes (as an ELM would).
        val fake = FakeTransport(incoming = listOf("41 00 BE 3F B8 11".toByteArray(Charsets.US_ASCII)))
        val md = TranscriptMetadata("rec", TransportType.SIMULATOR, target = "sim")
        val builder = DiagnosticTranscriptBuilder(md)
        val rec = RecordingTransport(fake, builder)

        rec.connect()
        rec.incomingBytes.drain(this)                       // collect RX (records it)
        rec.write("0100".toByteArray(Charsets.US_ASCII))     // record TX on the way out
        rec.disconnect()

        val t = builder.toTranscript()
        assertTrue(t.events.any { it.direction == Direction.TX && it.command == "0100" })
        assertTrue(t.events.any { it.direction == Direction.RX && it.rawHex == "41 00 BE 3F B8 11" })
        assertArrayEquals("0100".toByteArray(Charsets.US_ASCII), fake.writes.first())
    }

    @Test
    fun replayEmitsRecordedRxInOrder() = runTest {
        val t = DiagnosticTranscript(
            metadata = TranscriptMetadata("rec", TransportType.BLUETOOTH_CLASSIC, target = "dev"),
            events = listOf(
                DiagnosticEvent(Direction.TX, 1000L, 0L, "01 00", "01 00"),
                DiagnosticEvent(Direction.RX, 1010L, 10L, null, "41 00 BE 3F B8 11", responder = "7E8"),
                DiagnosticEvent(Direction.RX, 1020L, 20L, null, "41 0C 1A F8", responder = "7E8"),
                DiagnosticEvent(Direction.ERR, 1030L, 30L, null, "BUS INIT: ERROR", outcome = DiagnosticOutcome.ADAPTER_FAILURE)
            )
        )
        val replay = ReplayTransport(t, parentScope = backgroundScope)
        replay.connect()

        replay.write("0100\r".toByteArray())
        val collected = replay.incomingBytes.drain(this)
        // Only RX frames are emitted; TX and ERR are not wire returns.
        assertEquals(2, collected.size)
        assertArrayEquals("41 00 BE 3F B8 11".toByteArray(Charsets.US_ASCII), collected[0])
        assertArrayEquals("41 0C 1A F8".toByteArray(Charsets.US_ASCII), collected[1])
        assertEquals("SIMULATOR", replay.transportType.name)
        replay.disconnect()
    }

    @Test
    fun recordExportReplayRoundTrip() = runTest {
        val md = TranscriptMetadata("bt", TransportType.BLUETOOTH_CLASSIC,
            target = "00:1D:A5:68:37:5B", adapterIdentity = "ELM327 v1.5")
        val builder = DiagnosticTranscriptBuilder(md)
        builder.recordConnect()
        builder.recordTx("01 00")
        builder.recordRx("41 00 BE 3F B8 11", responder = "7E8", outcome = DiagnosticOutcome.POSITIVE)
        builder.recordTx("03")
        builder.recordRx("43 02 01 33 03 00", responder = "7E8", outcome = DiagnosticOutcome.POSITIVE)
        val original = builder.toTranscript()

        // Export → reload → replay (same parsing input).
        val reloaded = DiagnosticTranscript.fromJson(original.toJson())
        assertEquals(original.events.size, reloaded.events.size)
        assertEquals(original.events, reloaded.events)

        val replay = ReplayTransport(reloaded, parentScope = backgroundScope)
        replay.connect()
        replay.write("0100\r".toByteArray())
        runCurrent()
        replay.write("03\r".toByteArray())
        val frames = replay.incomingBytes.drain(this)
        assertEquals(2, frames.size)
        assertArrayEquals("41 00 BE 3F B8 11".toByteArray(Charsets.US_ASCII), frames[0])
        assertArrayEquals("43 02 01 33 03 00".toByteArray(Charsets.US_ASCII), frames[1])
        replay.disconnect()
    }
    @Test fun recorderAndFailureHandlerExceptionsDoNotChangeWireBehavior() = runTest {
        val bytes = "41 0D 00\r\n>".toByteArray()
        val delegate = FakeTransport(listOf(bytes), TransportType.WIFI_TCP)
        val recorder = RecordingTransport(delegate, { error("Capture unavailable") }, { error("Observer failed") })
        recorder.connect()
        val command = "010D\r".toByteArray()
        recorder.write(command)
        assertArrayEquals(command, delegate.writes.single())
        assertArrayEquals(bytes, recorder.incomingBytes.toList().single())
        recorder.disconnect()
        assertTrue(!delegate.isConnected)
    }
}
