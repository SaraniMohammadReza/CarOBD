package com.sarani.carobd.core.model

enum class FeatureMaturity {
    PRODUCTION_IMPLEMENTED,
    IMPLEMENTED_NEEDS_HARDWARE_VALIDATION,
    EXPERIMENTAL,
    SIMULATOR_ONLY,
    PROFILE_DATA_MISSING,
    NOT_IMPLEMENTED
}

enum class ConfidenceLevel {
    VERIFIED_STANDARD,
    VERIFIED_OEM,
    HIGH_CONFIDENCE,
    COMMUNITY_CONFIRMED,
    EXPERIMENTAL,
    UNKNOWN
}

enum class ProvenanceSourceType {
    OEM_MANUAL,
    OEM_PARTS_CATALOG,
    AUTHORIZED_SERVICE_BULLETIN,
    RELIABLE_WORKSHOP_SECONDARY,
    COMMUNITY_DATA,
    USER_INPUT,
    UNKNOWN
}

data class DataSourceReference(
    val title: String,
    val urlOrDocument: String,
    val manufacturer: String? = null,
    val standard: String? = null,
    val revision: String? = null,
    val accessedAt: String? = null,
    val notes: String = ""
)

data class Provenanced<T>(
    val data: T,
    val confidence: ConfidenceLevel,
    val sources: List<DataSourceReference> = emptyList(),
    val maturity: FeatureMaturity = FeatureMaturity.PRODUCTION_IMPLEMENTED
)

/**
 * Field-level provenance and audit metadata.
 *
 * Ensures each individual specification value (e.g. oil capacity vs spark plug gap vs belt teeth)
 * independently tracks its source, confidence, inference status, verification date, and safety
 * for automatic recommendations. Never promotes an entire specification record uniformly.
 */
data class FieldProvenance<T>(
    val value: T,
    val source: DataSourceReference? = null,
    val sourceType: ProvenanceSourceType = ProvenanceSourceType.UNKNOWN,
    val confidence: ConfidenceLevel = ConfidenceLevel.UNKNOWN,
    val isInferred: Boolean = false,
    val verifiedDate: String? = null,
    val isSafeForAutomaticRecommendation: Boolean = false
) {
    fun isOemVerified(): Boolean =
        confidence == ConfidenceLevel.VERIFIED_OEM &&
        source != null &&
        source.title.isNotBlank() &&
        (sourceType == ProvenanceSourceType.OEM_MANUAL || sourceType == ProvenanceSourceType.OEM_PARTS_CATALOG)

    companion object {
        fun <T> oemVerified(
            value: T,
            source: DataSourceReference,
            sourceType: ProvenanceSourceType = ProvenanceSourceType.OEM_MANUAL,
            verifiedDate: String = "2026-10-31"
        ): FieldProvenance<T> = FieldProvenance(
            value = value,
            source = source,
            sourceType = sourceType,
            confidence = ConfidenceLevel.VERIFIED_OEM,
            isInferred = false,
            verifiedDate = verifiedDate,
            isSafeForAutomaticRecommendation = true
        )

        fun <T> communityConfirmed(
            value: T,
            source: DataSourceReference? = null,
            sourceType: ProvenanceSourceType = ProvenanceSourceType.COMMUNITY_DATA,
            verifiedDate: String = "2026-10-31",
            safeForRecommendation: Boolean = false
        ): FieldProvenance<T> = FieldProvenance(
            value = value,
            source = source,
            sourceType = sourceType,
            confidence = ConfidenceLevel.COMMUNITY_CONFIRMED,
            isInferred = false,
            verifiedDate = verifiedDate,
            isSafeForAutomaticRecommendation = safeForRecommendation
        )

        fun <T> unverified(
            value: T,
            notes: String = ""
        ): FieldProvenance<T> = FieldProvenance(
            value = value,
            source = if (notes.isNotBlank()) DataSourceReference(title = notes, urlOrDocument = "") else null,
            sourceType = ProvenanceSourceType.UNKNOWN,
            confidence = ConfidenceLevel.UNKNOWN,
            isInferred = true,
            isSafeForAutomaticRecommendation = false
        )
    }
}
