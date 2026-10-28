@file:Suppress("UNCHECKED_CAST")

package com.sarani.carobd.core.adapter.profile

import java.lang.StringBuilder

/**
 * Minimal, dependency-free JSON codec for [ConnectionProfile] serialization to
 * Preferences DataStore. Intentionally small: supports null, bool, number,
 * string, array, and object — exactly what the profile schema needs. No external
 * serialization library is present in the project.
 */
internal object Json {
    fun encode(value: Any?): String = when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is Number -> value.toString()
        is String -> encodeString(value)
        is Map<*, *> -> encodeObject(value)
        is List<*> -> encodeArray(value)
        else -> encodeString(value.toString())
    }

    private fun encodeString(s: String): String {
        val sb = StringBuilder(s.length + 2)
        sb.append('"')
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c.code < 0x20) sb.append("\\u%04x".format(c.code)) else sb.append(c)
        }
        sb.append('"')
        return sb.toString()
    }

    private fun encodeObject(m: Map<*, *>): String {
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((k, v) in m) {
            if (!first) sb.append(',')
            first = false
            sb.append(encodeString(k.toString())).append(':').append(encode(v))
        }
        sb.append('}')
        return sb.toString()
    }

    private fun encodeArray(l: List<*>): String {
        val sb = StringBuilder()
        sb.append('[')
        l.forEachIndexed { i, v -> if (i > 0) sb.append(','); sb.append(encode(v)) }
        sb.append(']')
        return sb.toString()
    }

    fun decode(text: String): Any? = Parser(text).parseValue()

    private class Parser(private val src: String) {
        private var i = 0
        fun parseValue(): Any? {
            skipWs()
            if (i >= src.length) return null
            return when (src[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBool()
                'n' -> parseNull()
                else -> parseNumber()
            }
        }

        private fun skipWs() { while (i < src.length && src[i] in " \t\n\r") i++ }

        private fun parseObject(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++ // {
            skipWs()
            if (src.startsWith("}", i)) { i++; return m }
            while (true) {
                skipWs()
                val k = parseString()
                skipWs()
                if (src[i] != ':') throw IllegalArgumentException("expected ':' at $i in $src")
                i++
                val v = parseValue()
                m[k] = v
                skipWs()
                when (src[i]) {
                    ',' -> i++
                    '}' -> { i++; return m }
                    else -> throw IllegalArgumentException("expected ',' or '}' at $i in $src")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            val l = ArrayList<Any?>()
            i++ // [
            skipWs()
            if (src.startsWith("]", i)) { i++; return l }
            while (true) {
                l.add(parseValue())
                skipWs()
                when (src[i]) {
                    ',' -> i++
                    ']' -> { i++; return l }
                    else -> throw IllegalArgumentException("expected ',' or ']' at $i in $src")
                }
            }
        }

        private fun parseString(): String {
            if (src[i] != '"') throw IllegalArgumentException("expected string at $i in $src")
            i++
            val sb = StringBuilder()
            while (i < src.length && src[i] != '"') {
                val c = src[i]
                if (c == '\\' && i + 1 < src.length) {
                    i++
                    when (src[i]) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = src.substring(i + 1, i + 5)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                        else -> sb.append(src[i])
                    }
                } else sb.append(c)
                i++
            }
            if (src[i] != '"') throw IllegalArgumentException("unterminated string in $src")
            i++
            return sb.toString()
        }

        private fun parseBool(): Boolean {
            if (src.startsWith("true", i)) { i += 4; return true }
            if (src.startsWith("false", i)) { i += 5; return false }
            throw IllegalArgumentException("invalid literal at $i in $src")
        }

        private fun parseNull(): Any? {
            if (src.startsWith("null", i)) { i += 4; return null }
            throw IllegalArgumentException("invalid literal at $i in $src")
        }

        private fun parseNumber(): Number {
            val start = i
            if (src[i] == '-') i++
            while (i < src.length && (src[i].isdigit() || src[i] in ".eE+-")) i++
            val num = src.substring(start, i)
            return if ('.' in num || 'e' in num || 'E' in num) num.toDouble()
            else num.toLong()
        }

        private fun Char.isdigit() = this in '0'..'9'
    }
}
