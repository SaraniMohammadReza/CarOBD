package com.sarani.carobd.core.model

const val UNKNOWN_LAST_CHANGED = -1

data class ServiceReminderItem(
    val id: String,
    val type: ServiceType,
    val lastChangedKm: Int,
    val intervalKm: Int,
    val currentKm: Int,
    val notes: String = ""
) {
    /** True when the user has never recorded this service — no fake history. */
    val isUnknownHistory: Boolean get() = lastChangedKm < 0

    val nextDueKm: Int = if (isUnknownHistory) -1 else lastChangedKm + intervalKm
    val remainingKm: Int = if (isUnknownHistory) -1 else nextDueKm - currentKm
    val isOverdue: Boolean = !isUnknownHistory && remainingKm <= 0
    val progressPercent: Float = if (isUnknownHistory || intervalKm <= 0) Float.NaN else {
        val used = currentKm - lastChangedKm
        (1f - (used.toFloat() / intervalKm.toFloat())).coerceIn(0f, 1f)
    }
}
