package com.sarani.carobd.domain.reminder

import com.sarani.carobd.core.model.DueCalculationResult
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle

/**
 * Modern Android maintenance reminder scheduling interface.
 *
 * Guarantees per-vehicle and per-schedule identity, cancellation on edits/deletions,
 * practical snooze semantics without altering canonical service history, and
 * prevents duplicate scheduling or phantom background odometer progression.
 */
interface MaintenanceReminderScheduler {
    suspend fun scheduleReminder(
        vehicle: OwnedVehicle,
        schedule: MaintenanceSchedule,
        dueResult: DueCalculationResult
    )

    suspend fun cancelScheduleReminder(vehicleId: String, scheduleId: String)

    suspend fun cancelAllVehicleReminders(vehicleId: String)

    suspend fun snoozeReminder(
        vehicleId: String,
        scheduleId: String,
        duration: SnoozeDuration
    )

    suspend fun reevaluateAllRemindersForVehicle(vehicle: OwnedVehicle)
}
