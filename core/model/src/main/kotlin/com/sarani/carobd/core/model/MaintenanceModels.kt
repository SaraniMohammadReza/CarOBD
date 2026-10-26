package com.sarani.carobd.core.model

enum class ServiceType(val titleFa: String, val titleEn: String, val defaultIntervalKm: Int, val defaultIntervalMonths: Int) {
    ENGINE_OIL("روغن موتور", "Engine Oil", 6000, 6),
    OIL_FILTER("فیلتر روغن", "Oil Filter", 6000, 6),
    AIR_FILTER("فیلتر هوا", "Air Filter", 10000, 12),
    CABIN_FILTER("فیلتر اتاق (کابین)", "Cabin Air Filter", 15000, 12),
    SPARK_PLUGS("شمع‌های موتور", "Spark Plugs", 30000, 24),
    TIMING_BELT("تسمه تایم", "Timing Belt", 60000, 48),
    ACCESSORY_BELT("تسمه دینام و هیدرولیک", "Serpentine Belt", 45000, 36),
    BRAKE_PADS("لنت ترمز جلو", "Front Brake Pads", 35000, 24),
    GEARBOX_OIL("روغن گیربکس (واسکازین)", "Transmission Fluid", 40000, 36),
    COOLANT("مایع خنک‌کننده / ضدیخ", "Engine Coolant", 40000, 24),
    BRAKE_FLUID("روغن ترمز", "Brake Fluid", 40000, 24)
}

data class ServiceRecord(
    val id: String,
    val vehicleId: String,
    val type: ServiceType,
    val performedAtKm: Int,
    val performedAtDateMs: Long,
    val costTomans: Long = 0L,
    val partsBrand: String = "",
    val notes: String = ""
)

data class ServiceReminderItem(
    val id: String,
    val vehicleId: String,
    val type: ServiceType,
    val lastChangedKm: Int,
    val intervalKm: Int,
    val currentKm: Int,
    val notes: String = ""
) {
    val nextDueKm: Int = lastChangedKm + intervalKm
    val remainingKm: Int = nextDueKm - currentKm
    val isOverdue: Boolean = remainingKm <= 0
    val progressPercent: Float = if (intervalKm > 0) {
        val used = currentKm - lastChangedKm
        (1f - (used.toFloat() / intervalKm.toFloat())).coerceIn(0f, 1f)
    } else 1f
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
    val maintenanceTipsFa: List<String>,
    val sources: List<DataSourceReference> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_OEM
)
