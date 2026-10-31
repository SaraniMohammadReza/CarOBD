package com.sarani.carobd.domain.reminder

import com.sarani.carobd.core.model.DueCalculationResult
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceDueEngine

/** Absolute wall-clock target. A mileage-only OK schedule has no predictable wakeup. */
object ReminderPlan {
    fun nextRunAt(now: Long, due: DueCalculationResult, snoozedUntil: Long?): Long? {
        val normal = when (due.state) {
            DueState.UNKNOWN -> return null
            DueState.OK -> due.nextDueDateMs?.minus(MaintenanceDueEngine.DEFAULT_LEAD_DAYS * 86_400_000L) ?: return null
            else -> now
        }
        return maxOf(now, normal, snoozedUntil ?: now)
    }
}
