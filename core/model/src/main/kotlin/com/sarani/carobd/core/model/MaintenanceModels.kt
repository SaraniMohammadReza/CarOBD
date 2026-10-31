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

data class VehicleSpecSheet @JvmOverloads constructor(
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

// -------------------------------------------------------------
// NORMALIZED MAINTENANCE FOUNDATION
// -------------------------------------------------------------

enum class MaintenanceCategory {
    ENGINE,
    TRANSMISSION,
    BRAKES,
    FILTERS,
    IGNITION,
    COOLING,
    FLUIDS,
    TIRES_AND_SUSPENSION,
    ELECTRICAL,
    INSPECTION,
    OTHER
}

enum class MaintenanceActionType {
    REPLACE,
    INSPECT,
    ROTATE,
    CLEAN_OR_ADJUST,
    OTHER
}

enum class MaintenanceIntervalProvenance {
    OEM_RECOMMENDED,
    VERIFIED_SECONDARY,
    USER_CUSTOM,
    UNVERIFIED
}

enum class DueState {
    OK,
    DUE_SOON,
    DUE,
    OVERDUE,
    UNKNOWN
}

data class MaintenanceItemDefinition(
    val key: String,
    val category: MaintenanceCategory,
    val actionType: MaintenanceActionType,
    val titleEn: String,
    val titleFa: String,
    val defaultIntervalKm: Int? = null,
    val defaultIntervalMonths: Int? = null,
    val isInspectionOnly: Boolean = (actionType == MaintenanceActionType.INSPECT),
    val notes: String = ""
)

data class MaintenanceSchedule(
    val id: String,
    val vehicleId: String,
    val itemKey: String,
    val titleEn: String,
    val titleFa: String,
    val actionType: MaintenanceActionType = MaintenanceActionType.REPLACE,
    val intervalKm: Int? = null,
    val intervalMonths: Int? = null,
    val lastServiceKm: Long? = null,
    val lastServiceDateMs: Long? = null,
    val nextDueKm: Long? = null,
    val nextDueDateMs: Long? = null,
    val provenance: MaintenanceIntervalProvenance = MaintenanceIntervalProvenance.OEM_RECOMMENDED,
    val dueState: DueState = DueState.UNKNOWN,
    val isCustom: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)

data class VehicleMaintenanceRecord(
    val id: String,
    val vehicleId: String,
    val itemKey: String,
    val performedKm: Long,
    val performedDateMs: Long,
    val costTomans: Long = 0L,
    val serviceCenter: String? = null,
    val partsBrand: String? = null,
    val partNumber: String? = null,
    val notes: String? = null,
    val receiptUri: String? = null,
    val provenance: MaintenanceIntervalProvenance = MaintenanceIntervalProvenance.USER_CUSTOM,
    val createdAtMs: Long = System.currentTimeMillis()
)

data class DueCalculationResult(
    val state: DueState,
    val nextDueKm: Long?,
    val nextDueDateMs: Long?,
    val remainingKm: Long?,
    val remainingDays: Long?
)

object MaintenanceDueEngine {
    const val DEFAULT_LEAD_KM = 1000L
    const val DEFAULT_LEAD_DAYS = 30L
    private const val MS_PER_DAY = 86_400_000L
    private const val DAYS_PER_MONTH_APPROX = 30.4375

    fun calculateDueState(
        currentKm: Long?,
        currentDateMs: Long,
        lastKm: Long?,
        lastDateMs: Long?,
        intervalKm: Int?,
        intervalMonths: Int?,
        leadKm: Long = DEFAULT_LEAD_KM,
        leadDays: Long = DEFAULT_LEAD_DAYS
    ): DueCalculationResult {
        val nextKm = if (lastKm != null && intervalKm != null) lastKm + intervalKm else null
        val nextDate = if (lastDateMs != null && intervalMonths != null) {
            lastDateMs + (intervalMonths * DAYS_PER_MONTH_APPROX * MS_PER_DAY).toLong()
        } else null

        val remKm = if (nextKm != null && currentKm != null) nextKm - currentKm else null
        val remDays = if (nextDate != null) (nextDate - currentDateMs) / MS_PER_DAY else null

        // Determine state from mileage
        val kmState = when {
            remKm == null -> DueState.UNKNOWN
            remKm < 0 -> DueState.OVERDUE
            remKm == 0L -> DueState.DUE
            remKm <= leadKm -> DueState.DUE_SOON
            else -> DueState.OK
        }

        // Determine state from date
        val dateState = when {
            remDays == null -> DueState.UNKNOWN
            remDays < 0 -> DueState.OVERDUE
            remDays == 0L -> DueState.DUE
            remDays <= leadDays -> DueState.DUE_SOON
            else -> DueState.OK
        }

        // Combine conservative evidence: worst severity wins
        val finalState = when {
            kmState == DueState.OVERDUE || dateState == DueState.OVERDUE -> DueState.OVERDUE
            kmState == DueState.DUE || dateState == DueState.DUE -> DueState.DUE
            kmState == DueState.DUE_SOON || dateState == DueState.DUE_SOON -> DueState.DUE_SOON
            kmState == DueState.OK && dateState == DueState.OK -> DueState.OK
            kmState == DueState.OK && dateState == DueState.UNKNOWN -> DueState.OK
            kmState == DueState.UNKNOWN && dateState == DueState.OK -> DueState.OK
            else -> DueState.UNKNOWN
        }

        return DueCalculationResult(
            state = finalState,
            nextDueKm = nextKm,
            nextDueDateMs = nextDate,
            remainingKm = remKm,
            remainingDays = remDays
        )
    }
}
