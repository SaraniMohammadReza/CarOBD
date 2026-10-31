package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaintenanceDueEngineTest {

    private val nowMs = 1_700_000_000_000L // arbitrary fixed reference timestamp
    private val msPerDay = 86_400_000L

    @Test
    fun testDueEngineReturnsUnknownWhenLastServiceMissing() {
        val res = MaintenanceDueEngine.calculateDueState(
            currentKm = 50000L,
            currentDateMs = nowMs,
            lastKm = null,
            lastDateMs = null,
            intervalKm = 10000,
            intervalMonths = 12
        )
        assertEquals(DueState.UNKNOWN, res.state)
        assertNull(res.nextDueKm)
        assertNull(res.nextDueDateMs)
        assertNull(res.remainingKm)
        assertNull(res.remainingDays)
    }

    @Test
    fun testMileageBasedCalculationLifecycle() {
        val lastKm = 100_000L
        val interval = 10_000

        // 1. Far from due (5000 km remaining) -> OK
        val okRes = MaintenanceDueEngine.calculateDueState(
            currentKm = 105_000L,
            currentDateMs = nowMs,
            lastKm = lastKm,
            lastDateMs = null,
            intervalKm = interval,
            intervalMonths = null,
            leadKm = 1000L
        )
        assertEquals(DueState.OK, okRes.state)
        assertEquals(110_000L, okRes.nextDueKm)
        assertEquals(5000L, okRes.remainingKm)

        // 2. Within lead window (500 km remaining) -> DUE_SOON
        val soonRes = MaintenanceDueEngine.calculateDueState(
            currentKm = 109_500L,
            currentDateMs = nowMs,
            lastKm = lastKm,
            lastDateMs = null,
            intervalKm = interval,
            intervalMonths = null,
            leadKm = 1000L
        )
        assertEquals(DueState.DUE_SOON, soonRes.state)
        assertEquals(500L, soonRes.remainingKm)

        // 3. Exactly at due (0 km remaining) -> DUE
        val dueRes = MaintenanceDueEngine.calculateDueState(
            currentKm = 110_000L,
            currentDateMs = nowMs,
            lastKm = lastKm,
            lastDateMs = null,
            intervalKm = interval,
            intervalMonths = null,
            leadKm = 1000L
        )
        assertEquals(DueState.DUE, dueRes.state)
        assertEquals(0L, dueRes.remainingKm)

        // 4. Exceeded (1500 km overdue) -> OVERDUE
        val overdueRes = MaintenanceDueEngine.calculateDueState(
            currentKm = 111_500L,
            currentDateMs = nowMs,
            lastKm = lastKm,
            lastDateMs = null,
            intervalKm = interval,
            intervalMonths = null,
            leadKm = 1000L
        )
        assertEquals(DueState.OVERDUE, overdueRes.state)
        assertEquals(-1500L, overdueRes.remainingKm)
    }

    @Test
    fun testWorstEvidenceStateWins() {
        // Case: Mileage is still OK (5000 km left), but Date is OVERDUE (past next due date)
        val lastDatePast = nowMs - (200 * msPerDay) // ~6.5 months ago
        val res = MaintenanceDueEngine.calculateDueState(
            currentKm = 105_000L,
            currentDateMs = nowMs,
            lastKm = 100_000L,
            lastDateMs = lastDatePast,
            intervalKm = 10_000,
            intervalMonths = 6, // 6 months was due ~182 days ago
            leadKm = 1000L,
            leadDays = 30L
        )
        assertEquals("Date overdue must make the overall state OVERDUE even if mileage is OK", DueState.OVERDUE, res.state)

        // Case: Date is OK (90 days left), but Mileage is DUE_SOON (500 km left)
        val lastDateRecent = nowMs - (30 * msPerDay)
        val res2 = MaintenanceDueEngine.calculateDueState(
            currentKm = 109_500L,
            currentDateMs = nowMs,
            lastKm = 100_000L,
            lastDateMs = lastDateRecent,
            intervalKm = 10_000,
            intervalMonths = 6,
            leadKm = 1000L,
            leadDays = 30L
        )
        assertEquals("Mileage DUE_SOON must make overall state DUE_SOON even if date is OK", DueState.DUE_SOON, res2.state)
    }
}
