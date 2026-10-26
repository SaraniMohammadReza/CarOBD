package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure due-calculation tests for ServiceReminderItem (§Phase C maintenance correctness).
 * Covers OK / DUE_SOON / OVERDUE semantics without Android instrumentation.
 */
class MaintenanceDueCalculationTest {

    private fun reminder(
        lastChangedKm: Int,
        currentKm: Int,
        intervalKm: Int = 6000,
        type: ServiceType = ServiceType.ENGINE_OIL,
        id: String = "oil"
    ) = ServiceReminderItem(
        id = id,
        type = type,
        lastChangedKm = lastChangedKm,
        intervalKm = intervalKm,
        currentKm = currentKm
    )

    // nextDueKm = lastChangedKm + intervalKm
    @Test
    fun nextDueKm_isLastChangedPlusInterval() {
        val item = reminder(lastChangedKm = 100000, currentKm = 100000, intervalKm = 6000)
        assertEquals(106000, item.nextDueKm)
    }

    // just after service: progressPercent is remaining-fraction = 1.0 (full interval ahead); not overdue
    @Test
    fun remainingFullAtLastService() {
        val item = reminder(lastChangedKm = 100000, currentKm = 100000, intervalKm = 6000)
        assertEquals(1f, item.progressPercent, 0.001f)
        assertEquals(6000, item.remainingKm)
        assertFalse(item.isOverdue)
    }

    // halfway through interval
    @Test
    fun progress_halfway() {
        val item = reminder(lastChangedKm = 100000, currentKm = 103000, intervalKm = 6000)
        assertEquals(0.5f, item.progressPercent, 0.001f)
        assertEquals(3000, item.remainingKm)
        assertFalse(item.isOverdue)
    }

    // exactly at due km
    @Test
    fun overdue_atDueKm() {
        val item = reminder(lastChangedKm = 100000, currentKm = 106000, intervalKm = 6000)
        assertEquals(0f, item.progressPercent, 0.001f)
        assertEquals(0, item.remainingKm)
        assertTrue(item.isOverdue)
    }

    // past due
    @Test
    fun overdue_pastDueKm() {
        val item = reminder(lastChangedKm = 100000, currentKm = 107500, intervalKm = 6000)
        assertEquals(0f, item.progressPercent, 0.001f)
        assertEquals(-1500, item.remainingKm)
        assertTrue(item.isOverdue)
    }

    // currentKm never regresses below lastChanged -> progress clamped to 1, not overdue
    @Test
    fun progress_clampedWhenOverServiced() {
        val item = reminder(lastChangedKm = 106000, currentKm = 100000, intervalKm = 6000)
        assertEquals(1f, item.progressPercent, 0.001f)
        assertFalse(item.isOverdue)
    }

    // catalog default intervals are the source of truth (no invented numbers)
    @Test
    fun serviceType_defaultIntervalsAreCatalogValues() {
        assertEquals(6000, ServiceType.ENGINE_OIL.defaultIntervalKm)
        assertEquals(6000, ServiceType.OIL_FILTER.defaultIntervalKm)
        assertEquals(10000, ServiceType.AIR_FILTER.defaultIntervalKm)
        assertEquals(30000, ServiceType.SPARK_PLUGS.defaultIntervalKm)
        assertEquals(60000, ServiceType.TIMING_BELT.defaultIntervalKm)
        assertEquals(35000, ServiceType.BRAKE_PADS.defaultIntervalKm)
        assertEquals(40000, ServiceType.GEARBOX_OIL.defaultIntervalKm)
    }
}
