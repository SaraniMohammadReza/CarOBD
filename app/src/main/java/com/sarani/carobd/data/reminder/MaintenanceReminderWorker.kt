package com.sarani.carobd.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sarani.carobd.R
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceDueEngine
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.domain.reminder.MaintenanceNotificationFormatter
import com.sarani.carobd.domain.reminder.ReminderIdentity
import java.util.Locale

class MaintenanceReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_VEHICLE_ID = "vehicle_id"
        const val KEY_SCHEDULE_ID = "schedule_id"
        const val KEY_IS_SNOOZE = "is_snooze"
    }

    override suspend fun doWork(): Result {
        val vehicleId = inputData.getString(KEY_VEHICLE_ID) ?: return Result.success()
        val scheduleId = inputData.getString(KEY_SCHEDULE_ID) ?: return Result.success()

        val dbHelper = CarObdDatabaseHelper(applicationContext)

        val vehicle = dbHelper.ownedVehicles().firstOrNull { it.id == vehicleId }
            ?: return Result.success() // Vehicle deleted, discard obsolete work

        val schedules = dbHelper.getMaintenanceSchedulesForVehicle(
            vehicleId = vehicleId,
            currentOdometerKm = vehicle.currentOdometerKm
        )
        val schedule = schedules.firstOrNull { it.id == scheduleId }
            ?: return Result.success() // Schedule deleted, discard obsolete work

        // Recalculate truthful due state at the moment of execution
        val dueResult = MaintenanceDueEngine.calculateDueState(
            currentKm = vehicle.currentOdometerKm,
            currentDateMs = System.currentTimeMillis(),
            lastKm = schedule.lastServiceKm,
            lastDateMs = schedule.lastServiceDateMs,
            intervalKm = schedule.intervalKm,
            intervalMonths = schedule.intervalMonths
        )

        // Only alert if still due soon, due, or overdue
        if (dueResult.state != DueState.DUE_SOON &&
            dueResult.state != DueState.DUE &&
            dueResult.state != DueState.OVERDUE
        ) {
            return Result.success()
        }

        val content = MaintenanceNotificationFormatter.format(vehicle, schedule, dueResult)

        // Respect Android notification permissions
        if (!areNotificationsAllowed(applicationContext)) {
            return Result.success()
        }

        postNotification(applicationContext, scheduleId, content)
        return Result.success()
    }

    private fun areNotificationsAllowed(context: Context): Boolean {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun postNotification(
        context: Context,
        scheduleId: String,
        content: com.sarani.carobd.domain.reminder.ReminderContent
    ) {
        val channelId = ReminderIdentity.NOTIFICATION_CHANNEL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Vehicle Maintenance Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic notifications for scheduled vehicle maintenance and service due dates"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }

        val isPersian = Locale.getDefault().language.equals("fa", ignoreCase = true)
        val title = if (isPersian) content.titleFa else content.titleEn
        val body = if (isPersian) content.bodyFa else content.bodyEn

        val priority = when (content.dueState) {
            DueState.OVERDUE -> NotificationCompat.PRIORITY_HIGH
            DueState.DUE -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        val notificationId = ReminderIdentity.notificationId(scheduleId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission rejected or revoked at runtime
        }
    }
}
