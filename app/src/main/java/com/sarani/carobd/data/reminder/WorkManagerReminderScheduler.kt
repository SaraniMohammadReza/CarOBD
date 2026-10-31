package com.sarani.carobd.data.reminder

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sarani.carobd.core.model.*
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.domain.reminder.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class WorkManagerReminderScheduler(
    private val context: Context,
    private val database: CarObdDatabaseHelper,
    private val workManager: WorkManager = WorkManager.getInstance(context)
) : MaintenanceReminderScheduler {
    override suspend fun scheduleReminder(vehicle: OwnedVehicle, schedule: MaintenanceSchedule, dueResult: DueCalculationResult) = withContext(Dispatchers.IO) {
        val persistedVehicle = database.ownedVehicles().find { it.id == vehicle.id }
        val current = database.getMaintenanceSchedulesForVehicle(vehicle.id, persistedVehicle?.currentOdometerKm).find { it.id == schedule.id }
        if (persistedVehicle == null || current == null) {
            cancelScheduleReminder(vehicle.id, schedule.id)
            return@withContext
        }
        // Re-read persisted basis so a stale ViewModel cannot resurrect work for old service facts.
        val due = due(persistedVehicle, current)
        val now = System.currentTimeMillis()
        val snooze = database.reminderSnoozeUntil(vehicle.id, current.id, current.updatedAtMs)
        val target = ReminderPlan.nextRunAt(now, due, snooze)
        if (target == null) {
            cancelScheduleReminder(vehicle.id, current.id)
            return@withContext
        }
        val request = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>()
            .setInputData(Data.Builder().putString(MaintenanceReminderWorker.KEY_VEHICLE_ID, vehicle.id)
                .putString(MaintenanceReminderWorker.KEY_SCHEDULE_ID, current.id).build())
            .setInitialDelay(target - now, TimeUnit.MILLISECONDS)
            .addTag(ReminderIdentity.TAG_MAINTENANCE_REMINDER)
            .addTag(ReminderIdentity.vehicleTag(vehicle.id)).build()
        workManager.enqueueUniqueWork(ReminderIdentity.workName(vehicle.id, current.id), ExistingWorkPolicy.REPLACE, request).result.get()
    }

    override suspend fun cancelScheduleReminder(vehicleId: String, scheduleId: String) = withContext(Dispatchers.IO) {
        workManager.cancelUniqueWork(ReminderIdentity.workName(vehicleId, scheduleId)).result.get()
        NotificationManagerCompat.from(context).cancel(ReminderIdentity.notificationId(scheduleId))
    }

    override suspend fun cancelAllVehicleReminders(vehicleId: String) = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(ReminderIdentity.vehicleTag(vehicleId)).result.get()
        database.getMaintenanceSchedulesForVehicle(vehicleId).forEach {
            NotificationManagerCompat.from(context).cancel(ReminderIdentity.notificationId(it.id))
        }
    }

    override suspend fun snoozeReminder(vehicleId: String, scheduleId: String, duration: SnoozeDuration) = withContext(Dispatchers.IO) {
        val vehicle = database.ownedVehicles().find { it.id == vehicleId } ?: return@withContext
        val schedule = database.getMaintenanceSchedulesForVehicle(vehicleId, vehicle.currentOdometerKm).find { it.id == scheduleId } ?: return@withContext
        database.replaceReminderSnooze(vehicleId, scheduleId, schedule.updatedAtMs, System.currentTimeMillis() + duration.days.toLong() * 86_400_000L)
        NotificationManagerCompat.from(context).cancel(ReminderIdentity.notificationId(scheduleId))
        scheduleReminder(vehicle, schedule, due(vehicle, schedule))
    }

    override suspend fun reevaluateAllRemindersForVehicle(vehicle: OwnedVehicle) = withContext(Dispatchers.IO) {
        database.getMaintenanceSchedulesForVehicle(vehicle.id, vehicle.currentOdometerKm).forEach { scheduleReminder(vehicle, it, due(vehicle, it)) }
    }

    private fun due(vehicle: OwnedVehicle, schedule: MaintenanceSchedule) = MaintenanceDueEngine.calculateDueState(
        vehicle.currentOdometerKm, System.currentTimeMillis(), schedule.lastServiceKm, schedule.lastServiceDateMs, schedule.intervalKm, schedule.intervalMonths)
}
