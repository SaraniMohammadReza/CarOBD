package com.sarani.carobd.core.adapter.transport

import com.sarani.carobd.core.model.DiagnosticTranscript
import com.sarani.carobd.core.model.TranscriptMetadata
import java.io.File
import java.io.FileOutputStream

/** App-private atomic snapshots. Call on IO; IDs never become arbitrary paths. */
class FileTranscriptStore(private val directory: File) {
    private fun file(id: String): File { require(id.matches(Regex("[A-Za-z0-9-]{1,80}"))); return File(directory, "$id.json") }
    @Synchronized fun save(transcript: DiagnosticTranscript) {
        check(directory.isDirectory || directory.mkdirs())
        val json = transcript.toJson()
        DiagnosticTranscript.fromJson(json) // Validate the persisted schema and limits.
        val destination = file(transcript.metadata.sessionId)
        if (destination.exists() && runCatching { read(transcript.metadata.sessionId).events.size > transcript.events.size }.getOrDefault(false)) return
        val temporary = File.createTempFile("capture-", ".tmp", directory)
        try {
            FileOutputStream(temporary).use { out -> out.write(json.toByteArray(Charsets.UTF_8)); out.fd.sync() }
            check(temporary.renameTo(destination)) { "Atomic transcript replacement failed" }
        } finally { temporary.delete() }
    }
    @Synchronized fun read(id: String): DiagnosticTranscript {
        val source = file(id)
        require(source.length() <= 16 * 1024 * 1024) { "Transcript too large" }
        return DiagnosticTranscript.fromJson(source.readText(Charsets.UTF_8))
    }
    /** Pages are ordered by file update time, then ID. Only this page's bounded files are parsed. */
    @Synchronized fun list(offset: Int = 0, limit: Int = 50): List<TranscriptMetadata> {
        require(offset >= 0 && limit in 1..100)
        return directory.listFiles()?.filter { it.isFile && it.extension == "json" }
            ?.sortedWith(compareByDescending<File> { it.lastModified() }.thenBy { it.name })
            ?.drop(offset)?.take(limit)?.map { read(it.nameWithoutExtension).metadata } ?: emptyList()
    }
    @Synchronized fun delete(id: String): Boolean = file(id).delete()
}
