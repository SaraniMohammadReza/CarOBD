package com.sarani.carobd.core.model

/**
 * High-level automotive replacement part categories.
 */
enum class PartCategory {
    ENGINE_OIL,
    OIL_FILTER,
    AIR_FILTER,
    CABIN_FILTER,
    FUEL_FILTER,
    SPARK_PLUG,
    TIMING_BELT,
    BRAKE_PADS,
    BATTERY,
    COOLANT,
    TRANSMISSION_FLUID,
    ACCESSORY_BELT,
    WIPER_BLADES
}

/**
 * Definition of a replacement part item type.
 */
data class PartDefinition(
    val id: String,
    val category: PartCategory,
    val canonicalNameEn: String,
    val canonicalNameFa: String,
    val descriptionEn: String = "",
    val descriptionFa: String = "",
    val isSafetyCritical: Boolean = false,
    val iconKey: String = ""
)

/**
 * Original Equipment Manufacturer (OEM) part number reference with provenance.
 */
data class OemPartNumber(
    val partNumber: String,
    val manufacturer: String,
    val isDirectOem: Boolean = true,
    val source: DataSourceReference? = null,
    val notes: String = ""
)

/**
 * Compatible aftermarket part brand reference.
 */
data class AftermarketPartReference(
    val brand: String,
    val partNumber: String,
    val series: String? = null,
    val barcode: String? = null,
    val notes: String = "",
    val source: DataSourceReference? = null
)

/**
 * Technical specification for a replacement part (e.g. spark plug gap, belt tooth count).
 */
data class PartSpecification(
    val key: String,
    val labelEn: String,
    val labelFa: String,
    val value: String,
    val unit: String? = null,
    val confidence: ConfidenceLevel = ConfidenceLevel.UNKNOWN,
    val source: DataSourceReference? = null
)

/**
 * Vehicle fitment rule establishing compatibility between a part and a specific vehicle/engine.
 *
 * Fitment MUST be constrained by exact vehicle make, model, generation, engine family,
 * engine code, and production year boundaries. It must never be inferred from model name alone.
 */
data class VehicleFitment(
    val id: String,
    val partDefinitionId: String,
    val vehicleMakeId: String,
    val vehicleModelId: String,
    val vehicleGenerationId: String? = null,
    val engineId: String? = null,
    val engineCode: String? = null,
    val engineFamily: String? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val notesEn: String? = null,
    val notesFa: String? = null,
    val confidence: ConfidenceLevel = ConfidenceLevel.COMMUNITY_CONFIRMED,
    val source: DataSourceReference? = null,
    val oemNumbers: List<OemPartNumber> = emptyList(),
    val aftermarketReferences: List<AftermarketPartReference> = emptyList(),
    val specifications: List<PartSpecification> = emptyList()
)

/**
 * Aggregated compatible part item for domain consumption and UI rendering.
 */
data class CompatiblePartItem(
    val definition: PartDefinition,
    val fitment: VehicleFitment,
    val confidence: ConfidenceLevel = fitment.confidence
) {
    val isSafetyCritical: Boolean get() = definition.isSafetyCritical
    val isOemVerified: Boolean get() = confidence == ConfidenceLevel.VERIFIED_OEM
    val primaryOemNumber: String? get() = fitment.oemNumbers.firstOrNull()?.partNumber
}
