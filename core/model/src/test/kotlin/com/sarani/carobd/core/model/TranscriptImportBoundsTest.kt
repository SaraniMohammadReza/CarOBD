package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class TranscriptImportBoundsTest {
    private val md = TranscriptMetadata("import", TransportType.USB_SERIAL, sessionId = "safe-id")
    private fun json(events: List<DiagnosticEvent>) = DiagnosticTranscript(md, events).toJson()
    private fun event(elapsed: Long = 0, raw: String = "") = DiagnosticEvent(Direction.RX, 0, elapsed, rawHex = raw)
    @Test fun byteAndFrameBudgetsFailBeforeReplay() {
        assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson(" ".repeat(16 * 1024 * 1024 + 1)) }
        val e = assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson(json(listOf(event(raw = "X".repeat(65_537))))) }
        assertTrue(e.message.orEmpty().contains("Oversized frame"))
    }
    @Test fun nestingAndDuplicateKeysAreRejectedStructurally() {
        val nested = assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson("[".repeat(14) + "null" + "]".repeat(14)) }
        assertTrue(nested.message.orEmpty().contains("deep"))
        val duplicate = json(emptyList()).replace("\"events\":[]", "\"events\":[],\"events\":[]")
        assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson(duplicate) }
    }
    @Test fun timingAndPathTraversalIdsNeverEnterStoredSessions() {
        for (events in listOf(listOf(event(-1)), listOf(event(10), event(9)), listOf(event(604_800_001)))) {
            assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson(json(events)) }
        }
        val path = DiagnosticTranscript(md.copy(sessionId = "../capture"), emptyList()).toJson()
        assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson(path) }
    }
    @Test fun eventCountBoundIsEnforced() {
        val e = assertThrows(IllegalArgumentException::class.java) { DiagnosticTranscript.fromJson(json(List(50_001) { event() })) }
        assertTrue(e.message.orEmpty().contains("Too many events"))
    }
}
