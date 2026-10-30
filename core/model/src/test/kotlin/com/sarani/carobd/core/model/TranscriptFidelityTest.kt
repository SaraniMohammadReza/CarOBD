package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class TranscriptFidelityTest {
    @Test fun jsonPreservesCrLfAndStructuralCharacters() {
        val raw = "\r\n\\\"{}[] , فارسی \u0000\b\t\u00FF"
        val t = DiagnosticTranscript(TranscriptMetadata(raw, TransportType.SIMULATOR, raw, raw, raw),
            listOf(DiagnosticEvent(Direction.RX, 1, 0, raw, raw, null, DiagnosticOutcome.RAW, mapOf(raw to raw))))
        assertEquals(t, DiagnosticTranscript.fromJson(t.toJson()))
        assertFalse(t.toJson().contains('\u0000'))
        assertFalse(t.toJson().contains('\n'))
        assertEquals(DiagnosticTranscript(t.metadata, emptyList()), DiagnosticTranscript.fromJson(DiagnosticTranscript(t.metadata, emptyList()).toJson()))
    }
    @Test fun jsonRejectsMalformedStructureAndDuplicates() {
        val valid = DiagnosticTranscript(TranscriptMetadata("test", TransportType.SIMULATOR), emptyList()).toJson()
        for (bad in listOf(valid.dropLast(1), valid + "x", "{}", valid.replace("\"events\":[]", "\"events\":[],\"events\":[]"))) {
            assertThrows(Exception::class.java) { DiagnosticTranscript.fromJson(bad) }
        }
    }
    @Test fun csvEscapesNewlineAndQuote() {
        val raw = "x,\"y\r\nz"
        val t = DiagnosticTranscript(TranscriptMetadata("test", TransportType.SIMULATOR), listOf(DiagnosticEvent(Direction.RX, 1, 0, rawHex = raw)))
        assertTrue(t.toCsv().contains("\"x,\"\"y\r\nz\""))
        assertTrue(t.toCsv().contains(t.metadata.sessionId))
    }
    @Test fun recordingConcurrentTxRxSafe() {
        val b = DiagnosticTranscriptBuilder(TranscriptMetadata("test", TransportType.SIMULATOR))
        val threads = (1..4).map { Thread { repeat(500) { if (it % 2 == 0) b.recordTx("0100\r") else b.recordRx("\r\n>") } }.apply { start() } }
        threads.forEach { it.join() }
        val t = b.toTranscript()
        assertEquals(2000, t.events.size)
        assertTrue(t.events.zipWithNext().all { (a, z) -> a.elapsedMs <= z.elapsedMs })
        assertEquals(t, DiagnosticTranscript.fromJson(t.toJson()))
    }
}
