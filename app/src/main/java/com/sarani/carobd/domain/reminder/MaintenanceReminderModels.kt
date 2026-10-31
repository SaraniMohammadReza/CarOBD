package com.sarani.carobd.domain.reminder

import com.sarani.carobd.core.model.DueCalculationResult
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SnoozeDuration(val days: Int, val labelEn: String, val labelFa: String) {
    ONE_DAY(1, "1 Day", "۱ روز"),
    THREE_DAYS(3, "3 Days", "۳ روز"),
    ONE_WEEK(7, "1 Week", "۱ هفته")
}

object ReminderIdentity {
    const val TAG_MAINTENANCE_REMINDER = "maintenance_reminder"
    const val NOTIFICATION_CHANNEL_ID = "maintenance_reminders"

    fun workName(vehicleId: String, scheduleId: String): String =
        "schedule_reminder_${vehicleId}_${scheduleId}"

    fun vehicleTag(vehicleId: String): String =
        "vehicle_reminders_${vehicleId}"

    fun notificationId(scheduleId: String): Int =
        scheduleId.hashCode()
}

data class ReminderContent(
    val titleEn: String,
    val titleFa: String,
    val bodyEn: String,
    val bodyFa: String,
    val dueState: DueState
)

object MaintenanceNotificationFormatter {

    fun format(
        vehicle: OwnedVehicle,
        schedule: MaintenanceSchedule,
        dueResult: DueCalculationResult
    ): ReminderContent {
        val vehicleName = vehicle.displayName.ifBlank { "Vehicle" }
        val itemEn = schedule.titleEn.ifBlank { schedule.itemKey }
        val itemFa = schedule.titleFa.ifBlank { itemEn }

        val titleEn = "$vehicleName: $itemEn Service Alert"
        val titleFa = "هشدار سرویس $vehicleName: $itemFa"

        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

        val detailEn = buildDetailString(
            dueResult.remainingKm,
            dueResult.nextDueKm,
            dueResult.nextDueDateMs,
            dateFormat,
            isFa = false
        )

        val detailFa = buildDetailString(
            dueResult.remainingKm,
            dueResult.nextDueKm,
            dueResult.nextDueDateMs,
            dateFormat,
            isFa = true
        )

        val (bodyEn, bodyFa) = when (dueResult.state) {
            DueState.OVERDUE -> {
                "Service is overdue ($detailEn). Please service as soon as possible." to
                "موعد سرویس منقضی شده است ($detailFa). لطفاً در اسرع وقت اقدام کنید."
            }
            DueState.DUE -> {
                "Service is due now ($detailEn)." to
                "موعد انجام سرویس فرارسیده است ($detailFa)."
            }
            DueState.DUE_SOON -> {
                "Service is due soon ($detailEn)." to
                "موعد سرویس نزدیک است ($detailFa)."
            }
            DueState.OK -> {
                "Service is up to date ($detailEn)." to
                "وضعیت سرویس مناسب است ($detailFa)."
            }
            DueState.UNKNOWN -> {
                "No previous service history recorded for $itemEn." to
                "هیچ سابقه‌ای برای سرویس $itemFa ثبت نشده است."
            }
        }

        return ReminderContent(
            titleEn = titleEn,
            titleFa = titleFa,
            bodyEn = bodyEn,
            bodyFa = bodyFa,
            dueState = dueResult.state
        )
    }

    private fun buildDetailString(
        remainingKm: Long?,
        nextDueKm: Long?,
        nextDueDateMs: Long?,
        dateFormat: SimpleDateFormat,
        isFa: Boolean
    ): String {
        val parts = mutableListOf<String>()

        if (remainingKm != null) {
            val kmText = if (remainingKm < 0) {
                if (isFa) "${-remainingKm} کیلومتر گذشته" else "${-remainingKm} km overdue"
            } else {
                if (isFa) "$remainingKm کیلومتر باقی‌مانده" else "$remainingKm km remaining"
            }
            parts.add(kmText)
        } else if (nextDueKm != null) {
            val dueKmText = if (isFa) "در $nextDueKm کیلومتر" else "at $nextDueKm km"
            parts.add(dueKmText)
        }

        if (nextDueDateMs != null) {
            val dateStr = dateFormat.format(Date(nextDueDateMs))
            val dateText = if (isFa) "تا تاریخ $dateStr" else "by $dateStr"
            parts.add(dateText)
        }

        return if (parts.isEmpty()) {
            if (isFa) "بر اساس دوره تعریف‌شده" else "per scheduled interval"
        } else {
            parts.joinToString(" • ")
        }
    }
}
