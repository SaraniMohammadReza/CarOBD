package com.sarani.carobd.domain.reminder

import com.sarani.carobd.core.model.*
import org.junit.Assert.*
import org.junit.Test

class ReminderPlanTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L
    @Test fun snoozeSurvivesRepeatedPlanningAndRestartedCaller() {
        val due = DueCalculationResult(DueState.OVERDUE, 100L, null, -10L, null)
        val until = now + 3 * day
        assertEquals(until, ReminderPlan.nextRunAt(now, due, until))
        assertEquals(until, ReminderPlan.nextRunAt(now + day, due, until))
        assertEquals(now + 4 * day, ReminderPlan.nextRunAt(now + 4 * day, due, until))
    }
    @Test fun completedServiceSchedulesFutureTimeWarning() {
        val due = MaintenanceDueEngine.calculateDueState(80_000L, now, 80_000L, now, 10_000, 6)
        assertEquals(DueState.OK, due.state)
        assertEquals(due.nextDueDateMs!! - 30 * day, ReminderPlan.nextRunAt(now, due, null))
        assertTrue(ReminderPlan.nextRunAt(now, due, null)!! > now)
    }
    @Test fun mileageOnlyNeedsExplicitOdometerUpdateNotImaginaryProgress() {
        val ok = MaintenanceDueEngine.calculateDueState(80_000L, now, 80_000L, null, 10_000, null)
        assertNull(ReminderPlan.nextRunAt(now, ok, null))
        val warning = MaintenanceDueEngine.calculateDueState(89_000L, now, 80_000L, null, 10_000, null)
        assertEquals(now, ReminderPlan.nextRunAt(now, warning, null))
        assertNull(ReminderPlan.nextRunAt(now, DueCalculationResult(DueState.UNKNOWN,null,null,null,null), null))
    }
}
