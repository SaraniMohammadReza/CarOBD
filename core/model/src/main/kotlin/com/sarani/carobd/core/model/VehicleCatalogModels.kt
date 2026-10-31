package com.sarani.carobd.core.model

enum class VehicleMarket {
    IRAN,
    EUROPE,
    NORTH_AMERICA,
    ASIA,
    GLOBAL,
    OTHER
}

enum class FuelType {
    PETROL,
    DIESEL,
    CNG,
    LPG,
    HYBRID_PETROL,
    ELECTRIC,
    FLEX_FUEL,
    UNKNOWN
}

enum class AspirationType {
    NATURALLY_ASPIRATED,
    TURBOCHARGED,
    SUPERCHARGED,
    TWIN_TURBO,
    UNKNOWN
}

enum class TransmissionType {
    MANUAL,
    AUTOMATIC_TORQUE_CONVERTER,
    CONTINUOUSLY_VARIABLE_CVT,
    DUAL_CLUTCH_DCT,
    AUTOMATED_MANUAL_AMT,
    DIRECT_DRIVE_EV,
    UNKNOWN
}

enum class DriveLayout {
    FRONT_WHEEL_DRIVE_FWD,
    REAR_WHEEL_DRIVE_RWD,
    ALL_WHEEL_DRIVE_AWD,
    FOUR_WHEEL_DRIVE_4WD,
    UNKNOWN
}

data class VehicleMake(
    val id: String,
    val name: String,
    val countryOfOrigin: String? = null,
    val aliases: List<String> = emptyList()
)

data class VehicleModel(
    val id: String,
    val makeId: String,
    val name: String,
    val bodyStyle: String? = null,
    val aliases: List<String> = emptyList()
)

data class VehicleGeneration(
    val id: String,
    val modelId: String,
    val name: String,
    val startYear: Int,
    val endYear: Int? = null,
    val chassisCode: String? = null,
    val market: VehicleMarket = VehicleMarket.GLOBAL,
    val aliases: List<String> = emptyList()
)

data class EngineVariant(
    val id: String,
    val code: String,
    val family: String? = null,
    val displacementCc: Int? = null,
    val cylinderCount: Int? = null,
    val valvesPerCylinder: Int? = null,
    val fuelType: FuelType = FuelType.PETROL,
    val aspiration: AspirationType = AspirationType.NATURALLY_ASPIRATED,
    val powerHp: Int? = null,
    val torqueNm: Int? = null,
    val sources: List<DataSourceReference> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_OEM
)

data class TransmissionVariant(
    val id: String,
    val type: TransmissionType,
    val gearCount: Int? = null,
    val code: String? = null,
    val manufacturer: String? = null
)

data class VehicleSpecification(
    val engineOilGrade: String? = null,
    val engineOilCapacityLiters: Float? = null,
    val gearboxOilGrade: String? = null,
    val gearboxCapacityLiters: Float? = null,
    val sparkPlugModel: String? = null,
    val sparkPlugGapMm: Float? = null,
    val timingBeltIntervalKm: Int? = null,
    val timingBeltTeeth: Int? = null,
    val brakeFluidType: String? = null,
    val coolantCapacityLiters: Float? = null,
    val tireFront: String? = null,
    val tireRear: String? = null,
    val batterySpec: String? = null,
    val fuelTankCapacityLiters: Float? = null,
    val sources: List<DataSourceReference> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_OEM
)

data class VehicleVariant(
    val id: String,
    val generationId: String,
    val trim: String,
    val modelYearStart: Int,
    val modelYearEnd: Int? = null,
    val engineId: String,
    val transmissionId: String? = null,
    val driveLayout: DriveLayout = DriveLayout.FRONT_WHEEL_DRIVE_FWD,
    val specifications: VehicleSpecification? = null,
    val aliases: List<String> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_OEM
)

data class ResolvedVehicleMatch(
    val make: VehicleMake,
    val model: VehicleModel,
    val generation: VehicleGeneration,
    val variant: VehicleVariant,
    val engine: EngineVariant,
    val transmission: TransmissionVariant?,
    val confidence: ConfidenceLevel
)
