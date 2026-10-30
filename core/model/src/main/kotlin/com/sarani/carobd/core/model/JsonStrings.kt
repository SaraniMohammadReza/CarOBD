package com.sarani.carobd.core.model

internal fun jsonStringLiteral(value: String?): String = value?.let { "\"${jsonEscaped(it)}\"" } ?: "null"
internal fun jsonEscaped(value: String): String = buildString {
    value.forEach { c -> when (c) {
        '\\' -> append("\\\\")
        '"' -> append("\\\"")
        else -> if (c.code < 0x20) append("\\u%04X".format(java.util.Locale.ROOT, c.code)) else append(c)
    } }
}
