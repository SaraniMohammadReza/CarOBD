package com.sarani.carobd.core.model

enum class VehicleType {
    CAR,
    MOTORCYCLE,
    HEAVY_DUTY,
    EV_HYBRID
}

data class EcuProfile(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val family: String, // e.g. "Siemens VDO", "Bosch ME7.4.4", "SSAT", "Valeo S2000", "EasyU 2.5", "Delphi MT05"
    val protocolConfig: EcuProtocolConfig,
    val primaryAddressHex: String = "11",
    val secondaryAddressesHex: List<String> = emptyList(),
    val supportedServices: List<String> = listOf("01", "02", "03", "04", "07", "09"),
    val notesFa: String = "",
    val confidence: ConfidenceLevel = ConfidenceLevel.HIGH_CONFIDENCE,
    val sources: List<DataSourceReference> = emptyList()
)

data class EngineProfile(
    val code: String, // e.g. "TU5 JP4", "EF7", "XU7 L3", "M15"
    val displacementLiters: Float,
    val cylinderCount: Int,
    val valveCount: Int,
    val fuelType: String,
    val isTurbocharged: Boolean = false,
    val isBiFuelCng: Boolean = false
)

data class TransmissionProfile(
    val type: String, // "Manual", "Automatic AL4", "CVT", "DCT", "6AT DAE"
    val speedCount: Int,
    val fluidType: String,
    val capacityLiters: Float
)

data class VehicleProfile(
    val id: String,
    val type: VehicleType = VehicleType.CAR,
    val manufacturer: String,
    val modelNameEn: String,
    val modelNameFa: String,
    val modelYears: String,
    val engine: EngineProfile,
    val transmission: TransmissionProfile? = null,
    val primaryEcu: EcuProfile,
    val alternativeEcus: List<EcuProfile> = emptyList(),
    val maturity: FeatureMaturity = FeatureMaturity.IMPLEMENTED_NEEDS_HARDWARE_VALIDATION,
    val confidence: ConfidenceLevel = ConfidenceLevel.HIGH_CONFIDENCE,
    val sources: List<DataSourceReference> = emptyList()
)

data class GarageVehicle(
    val id: String,
    val profileId: String,
    val nickname: String,
    val manufacturer: String,
    val model: String,
    val year: Int,
    val engineCode: String,
    val fuelType: String,
    val licensePlate: String? = null,
    val vin: String? = null,
    val odometerKm: Int = 100000,
    val tankCapacityLiters: Float = 50f,
    val preferredAdapterAddress: String? = null,
    val preferredProtocolId: String? = null,
    val isDefault: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis()
)
