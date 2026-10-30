package com.sarani.carobd.core.model

import org.junit.Assert.*
import org.junit.Test

class TelemetryGraphHistoryTest {
    @Test fun gapsStaleValuesAndLongMissingPeriodsNeverJoinLines() {
        val points = listOf(TelemetryGraphPoint(0, -4f), TelemetryGraphPoint(1, 0f),
            TelemetryGraphPoint(2, null), TelemetryGraphPoint(3, 5f), TelemetryGraphPoint(20_000, 6f),
            TelemetryGraphPoint(20_001, Float.NaN), TelemetryGraphPoint(20_002, 7f))
        assertEquals(listOf(listOf(-4f, 0f), listOf(5f), listOf(6f), listOf(7f)),
            TelemetryGraphHistory.segments(points).map { it.map { p -> p.value } })
    }
    @Test fun historyIsBoundedAndDuplicateObservationDoesNotCreateAnotherSample() {
        var history = emptyList<TelemetryGraphPoint>()
        repeat(1000) { history = TelemetryGraphHistory.append(history, TelemetryGraphPoint(it.toLong(), it.toFloat())) }
        assertEquals(120, history.size)
        val duplicate = TelemetryGraphHistory.append(history, history.last())
        assertEquals(history, duplicate)
        assertEquals(880L, history.first().timeMs)
    }
    @Test fun rollbackStartsNewAxisAndNonFiniteIsGapRatherThanZero() {
        val history = listOf(TelemetryGraphPoint(100, 20f))
        assertEquals(listOf(TelemetryGraphPoint(10, null)), TelemetryGraphHistory.append(history, TelemetryGraphPoint(10, Float.POSITIVE_INFINITY)))
        assertTrue(TelemetryGraphHistory.segments(listOf(TelemetryGraphPoint(0, null))).isEmpty())
    }
}
