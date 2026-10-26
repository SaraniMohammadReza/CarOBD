package com.sarani.carobd.core.model

enum class DtcSeverity {
    HIGH,
    MEDIUM,
    LOW
}

enum class DtcSystem {
    POWERTRAIN,   // P codes
    CHASSIS,      // C codes
    BODY,         // B codes
    NETWORK       // U codes
}

data class DiagnosticCode(
    val code: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val severity: DtcSeverity = DtcSeverity.MEDIUM,
    val system: DtcSystem = DtcSystem.POWERTRAIN,
    val ecuSource: String = "Engine ECU"
)
