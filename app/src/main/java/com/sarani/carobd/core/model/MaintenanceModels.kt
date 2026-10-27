package com.sarani.carobd.core.model

enum class ServiceType(val titleFa: String, val titleEn: String, val defaultIntervalKm: Int) {
    ENGINE_OIL("روغن موتور", "Engine Oil", 6000),
    OIL_FILTER("فیلتر روغن", "Oil Filter", 6000),
    AIR_FILTER("فیلتر هوا", "Air Filter", 10000),
    CABIN_FILTER("فیلتر اتاق (کابین)", "Cabin Air Filter", 15000),
    SPARK_PLUGS("شمع‌های موتور", "Spark Plugs", 30000),
    TIMING_BELT("تسمه تایم", "Timing Belt", 60000),
    ACCESSORY_BELT("تسمه دینام و هیدرولیک", "Serpentine / Accessory Belt", 45000),
    BRAKE_PADS("لنت ترمز چرخ‌های جلو", "Front Brake Pads", 35000),
    GEARBOX_OIL("روغن گیربکس (واسکازین)", "Transmission Fluid", 40000),
    COOLANT("مایع ضد یخ و خنک‌کننده", "Engine Coolant", 40000),
    BRAKE_FLUID("روغن ترمز", "Brake Fluid", 40000)
}

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

data class VehicleSpecSheet(
    val vehicleId: String,
    val engineCode: String,
    val engineOilGrade: String,
    val engineOilCapacityLiters: Float,
    val recommendedOilBrands: List<String>,
    val gearboxOilGrade: String,
    val gearboxCapacityLiters: Float,
    val sparkPlugModel: String,
    val sparkPlugGapMm: Float,
    val timingBeltIntervalKm: Int,
    val timingBeltTeeth: Int,
    val brakeFluidType: String,
    val coolantCapacityLiters: Float,
    val maintenanceTipsFa: List<String>
)
