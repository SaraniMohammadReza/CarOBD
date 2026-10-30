package com.sarani.carobd.core.model

/** Missing/stale/invalid observations are gaps, not zero-valued measurements. */
data class TelemetryGraphPoint(val timeMs: Long, val value: Float?)

object TelemetryGraphHistory {
    const val MAX_POINTS = 120
    const val MAX_GAP_MS = 10_000L

    fun append(history: List<TelemetryGraphPoint>, point: TelemetryGraphPoint): List<TelemetryGraphPoint> {
        val valid = point.copy(value = point.value?.takeIf { it.isFinite() })
        val previous = history.lastOrNull()
        // Wall-clock rollback starts a new time axis; never draw backwards through old data.
        if (previous != null && valid.timeMs < previous.timeMs) return listOf(valid)
        if (previous?.timeMs == valid.timeMs) return (history.dropLast(1) + valid).takeLast(MAX_POINTS)
        return (history + valid).takeLast(MAX_POINTS)
    }

    fun segments(history: List<TelemetryGraphPoint>): List<List<TelemetryGraphPoint>> {
        val out = mutableListOf<List<TelemetryGraphPoint>>()
        var current = mutableListOf<TelemetryGraphPoint>()
        fun flush() { if (current.isNotEmpty()) out += current.toList(); current = mutableListOf() }
        for (point in history) {
            val previous = current.lastOrNull()
            if (point.value == null || !point.value.isFinite()) { flush(); continue }
            if (previous != null && (point.timeMs <= previous.timeMs || point.timeMs - previous.timeMs > MAX_GAP_MS)) flush()
            current += point
        }
        flush()
        return out
    }
}
