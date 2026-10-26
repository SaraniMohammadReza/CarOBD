package com.sarani.carobd.core.model

enum class DtcSeverity {
    CRITICAL,
    MODERATE,
    INFORMATIONAL
}

enum class DtcSystem {
    POWERTRAIN,   // P codes
    CHASSIS,      // C codes
    BODY,         // B codes
    NETWORK       // U codes
}

enum class DtcCategory {
    STORED,       // Mode 03 / Confirmed
    PENDING,      // Mode 07 / Continuous monitor failure
    PERMANENT,    // Mode 0A / Cleared only by ECU after driving cycle
    HISTORY       // OEM specific historical log
}

data class FreezeFrameData(
    val dtc: String,
    val frameNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sensorValues: Map<String, String> = emptyMap() // e.g. "010C" -> "2450 RPM", "0105" -> "102 °C"
)

data class DiagnosticCode(
    val code: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val severity: DtcSeverity = DtcSeverity.MODERATE,
    val system: DtcSystem = DtcSystem.POWERTRAIN,
    val category: DtcCategory = DtcCategory.STORED,
    val likelyCausesFa: List<String> = emptyList(),
    val diagnosticStepsFa: List<String> = emptyList(),
    val ecuSource: String = "Engine ECU",
    val responderAddressHex: String = "7E8",
    val freezeFrame: FreezeFrameData? = null,
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_STANDARD
)
