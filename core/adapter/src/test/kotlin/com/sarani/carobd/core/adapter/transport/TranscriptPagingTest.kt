package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class TranscriptPagingTest {
    @Test fun pagesDoNotParseUnselectedFilesAndPreserveAllCaptures() {
        val dir = Files.createTempDirectory("paged-captures").toFile()
        try {
            val store = FileTranscriptStore(dir)
            repeat(3) { i ->
                store.save(DiagnosticTranscript(TranscriptMetadata("p$i", TransportType.WIFI_TCP, sessionId = "id$i"), emptyList()))
                check(java.io.File(dir, "id$i.json").setLastModified((i + 1) * 1000L))
            }
            val corrupt = java.io.File(dir, "old.json"); corrupt.writeText("corrupt"); check(corrupt.setLastModified(1))
            assertEquals(listOf("id2", "id1"), store.list(0, 2).map { it.sessionId })
            assertEquals(listOf("id0"), store.list(2, 1).map { it.sessionId })
            assertEquals(4, dir.listFiles()!!.size)
            assertThrows(IllegalArgumentException::class.java) { store.list(-1, 2) }
            assertThrows(IllegalArgumentException::class.java) { store.list(0, 101) }
            // Corruption is actionable when its page is selected, never silently skipped.
            assertThrows(Exception::class.java) { store.list(3, 1) }
        } finally { dir.deleteRecursively() }
    }
}
