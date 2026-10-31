package com.sarani.carobd.data.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sarani.carobd.core.model.DueCalculationResult
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceDueEngine
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.domain.reminder.MaintenanceReminderScheduler
import com.sarani.carobd.domain.reminder.ReminderIdentity
import com.sarani.carobd.domain.reminder.SnoozeDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WorkManagerReminderScheduler(
    private val context: Context,
    private val database: CarObdDatabaseHelper,
    private val workManager: WorkManager = WorkManager.getInstance(context)
) : MaintenanceReminderScheduler {

    override suspend fun scheduleReminder(
        vehicle: OwnedVehicle,
        schedule: MaintenanceSchedule,
        dueResult: DueCalculationResult
    ) = withContext(Dispatchers.IO) {
        // Unknown or OK schedules without due dates do not need active background alarm workers
        if (dueResult.state == DueState.UNKNOWN || (dueResult.state == DueState.OK && dueResult.nextDueDateMs == null)) {
            cancelScheduleReminder(vehicle.id, schedule.id)
            return@withContext
        }

        val initialDelayMs = calculateInitialDelayMs(dueResult)

        val inputData = Data.Builder()
            .putString(MaintenanceReminderWorker.KEY_VEHICLE_ID, vehicle.id)
            .putString(MaintenanceReminderWorker.KEY_SCHEDULE_ID, schedule.id)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .addTag(ReminderIdentity.TAG_MAINTENANCE_REMINDER)
            .addTag(ReminderIdentity.vehicleTag(vehicle.id))
            .build()

        workManager.enqueueUniqueWork(
            ReminderIdentity.workName(vehicle.id, schedule.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override suspend fun cancelScheduleReminder(vehicleId: String, scheduleId: String) =
        withContext(Dispatchers.IO) {
            workManager.cancelUniqueWork(ReminderIdentity.workName(vehicleId, scheduleId))
            Unit
        }

    override suspend fun cancelAllVehicleReminders(vehicleId: String) =
        withContext(Dispatchers.IO) {
            workManager.cancelAllWorkByTag(ReminderIdentity.vehicleTag(vehicleId))
            Unit
        }

    override suspend fun snoozeReminder(
        vehicleId: String,
        scheduleId: String,
        duration: SnoozeDuration
    ) = withContext(Dispatchers.IO) {
        val delayMs = duration.days.toLong() * 86_400_000L

        val inputData = Data.Builder()
            .putString(MaintenanceReminderWorker.KEY_VEHICLE_ID, vehicleId)
            .putString(MaintenanceReminderWorker.KEY_SCHEDULE_ID, scheduleId)
            .putBoolean(MaintenanceReminderWorker.KEY_IS_SNOOZE, true)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
            .setInputData(inputData)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(ReminderIdentity.TAG_MAINTENANCE_REMINDER)
            .addTag(ReminderIdentity.vehicleTag(vehicleId))
            .build()

        workManager.enqueueUniqueWork(
            ReminderIdentity.workName(vehicleId, scheduleId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Unit
    }

    override suspend fun reevaluateAllRemindersForVehicle(vehicle: OwnedVehicle) =
        withContext(Dispatchers.IO) {
            val schedules = database.getMaintenanceSchedulesForVehicle(
                vehicleId = vehicle.id,
                currentOdometerKm = vehicle.currentOdometerKm
            )

            for (schedule in schedules) {
                val dueResult = MaintenanceDueEngine.calculateDueState(
                    currentKm = vehicle.currentOdometerKm,
                    currentDateMs = System.currentTimeMillis(),
                    lastKm = schedule.lastServiceKm,
                    lastDateMs = schedule.lastServiceDateMs,
                    intervalKm = schedule.intervalKm,
                    intervalMonths = schedule.intervalMonths
                )
                scheduleReminder(vehicle, schedule, dueResult)
            }
        }

    private fun calculateInitialDelayMs(dueResult: DueCalculationResult): Long {
        val now = System.currentTimeMillis()
        return when (dueResult.state) {
            DueState.OVERDUE, DueState.DUE -> 0L
            DueState.DUE_SOON -> {
                // If already due soon, trigger prompt notification
                0L
            }
            DueState.OK -> {
                val nextDueDate = dueResult.nextDueDateMs
                if (nextDueDate != null) {
                    val leadTimeMs = MaintenanceDueEngine.DEFAULT_LEAD_DAYS * 86_400_000L
                    val targetNotificationDate = nextDueDate - leadTimeMs
                    maxOf(0L, targetNotificationDate - now)
                } else {
                    0L
                }
            }
            DueState.UNKNOWN -> 0L
        }
    }
}
