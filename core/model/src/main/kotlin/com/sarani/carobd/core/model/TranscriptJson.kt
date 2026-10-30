package com.sarani.carobd.core.model

/** Bounded schema reader. Strings, including braces and CR/LF, are never scanned as structure. */
internal object TranscriptJson {
    fun decode(text: String): DiagnosticTranscript {
        require(text.length <= 16 * 1024 * 1024) { "Transcript too large" }
        val root = Parser(text).parse().obj()
        val md = root["metadata"].obj()
        val metadata = TranscriptMetadata(md.string("profileName"), TransportType.valueOf(md.string("transport")),
            md.optional("target"), md.optional("adapterIdentity"), md.optional("vehicleProtocol"), md.number("startedAtMs"),
            md.optional("sessionId") ?: "legacy-${md.number("startedAtMs")}")
        require(metadata.sessionId.matches(Regex("[A-Za-z0-9-]{1,80}"))) { "Invalid session ID" }
        val list = root["events"] as? List<*> ?: error("Missing events")
        require(list.size <= 50_000) { "Too many events" }
        var last = -1L
        val events = list.map { value ->
            val e = value.obj()
            val elapsed = e.number("el")
            require(elapsed in 0..604_800_000L && elapsed >= last) { "Invalid event timing" }
            last = elapsed
            val raw = e.string("raw")
            require(raw.length <= 65_536) { "Oversized frame" }
            val extra = (e["meta"] ?: emptyMap<String, Any?>()).obj().mapValues { (_, v) -> v as? String ?: error("Invalid protocol metadata") }
            DiagnosticEvent(Direction.valueOf(e.string("dir")), e.number("ts"), elapsed, e.optional("cmd"), raw,
                e.optional("responder"), DiagnosticOutcome.valueOf(e.optional("outcome") ?: "RAW"), extra)
        }
        return DiagnosticTranscript(metadata, events)
    }
    private fun Any?.obj(): Map<String, Any?> {
        require(this is Map<*, *> && keys.all { it is String }) { "Expected object" }
        @Suppress("UNCHECKED_CAST") return this as Map<String, Any?>
    }
    private fun Map<String, Any?>.string(key: String) = this[key] as? String ?: error("Missing string $key")
    private fun Map<String, Any?>.optional(key: String): String? = this[key]?.let { it as? String ?: error("Invalid $key") }
    private fun Map<String, Any?>.number(key: String) = this[key] as? Long ?: error("Missing integer $key")
    private class Parser(val text: String) {
        var index = 0
        fun parse(): Any? { val value = value(0); whitespace(); require(index == text.length) { "Trailing JSON" }; return value }
        fun whitespace() { while (index < text.length && text[index] in " \r\n\t") index++ }
        fun consume(c: Char): Boolean { whitespace(); return if (index < text.length && text[index] == c) { index++; true } else false }
        fun value(depth: Int): Any? {
            require(depth <= 12) { "JSON too deep" }; whitespace(); require(index < text.length) { "Truncated JSON" }
            return when (text[index]) {
                '"' -> string()
                '{' -> {
                    index++; val result = linkedMapOf<String, Any?>()
                    if (!consume('}')) do {
                        whitespace(); val key = string(); require(!result.containsKey(key)) { "Duplicate JSON key" }
                        require(consume(':')); result[key] = value(depth + 1)
                        if (consume('}')) break
                        require(consume(','))
                    } while (true)
                    result
                }
                '[' -> {
                    index++; val result = mutableListOf<Any?>()
                    if (!consume(']')) do {
                        require(result.size < 50_001); result += value(depth + 1)
                        if (consume(']')) break
                        require(consume(','))
                    } while (true)
                    result
                }
                'n' -> { require(text.startsWith("null", index)); index += 4; null }
                else -> {
                    val start = index
                    if (text[index] == '-') index++
                    require(index < text.length && text[index].isDigit())
                    if (text[index] == '0') index++ else while (index < text.length && text[index] in '0'..'9') index++
                    text.substring(start, index).toLong()
                }
            }
        }
        fun string(): String {
            require(index < text.length && text[index++] == '"') { "Expected JSON string" }
            val out = StringBuilder()
            while (index < text.length) {
                val c = text[index++]
                if (c == '"') return out.toString()
                require(c.code >= 0x20) { "Unescaped control" }
                if (c != '\\') { out.append(c); continue }
                require(index < text.length)
                when (val escaped = text[index++]) {
                    '"', '\\', '/' -> out.append(escaped)
                    'b' -> out.append('\b')
                    'f' -> out.append('\u000C')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    'u' -> { require(index + 4 <= text.length); out.append(text.substring(index, index + 4).toInt(16).toChar()); index += 4 }
                    else -> error("Invalid JSON escape")
                }
            }
            error("Unterminated string")
        }
    }
}
