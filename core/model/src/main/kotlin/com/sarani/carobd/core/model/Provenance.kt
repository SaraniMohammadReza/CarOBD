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
