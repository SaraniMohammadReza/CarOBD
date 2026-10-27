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

/** §9: common J1979 / ISO 14229 Negative Response Codes. */
enum class ObdNrc(val code: Int, val descriptionEn: String, val descriptionFa: String) {
    GENERAL_REJECT(0x10, "General Reject", "رد درخواست"),
    SERVICE_NOT_SUPPORTED(0x11, "Service Not Supported", "سرویس پشتیبانی نمی‌شود"),
    SUB_FUNCTION_NOT_SUPPORTED(0x12, "Sub-function Not Supported", "زیرتابع پشتیبانی نمی‌شود"),
    INCORRECT_MESSAGE_LENGTH(0x13, "Incorrect Message Length", "طول پیام صحیح نیست"),
    CONDITIONS_NOT_CORRECT(0x22, "Conditions Not Correct", "شرایط مناسب نیست"),
    REQUEST_OUT_OF_RANGE(0x31, "Request Out Of Range", "درخواست خارج از بازه"),
    INSUFFICIENT_MEMORY(0x33, "Insufficient Memory", "حافظه کافی نیست"),
    RESPONSE_PENDING(0x78, "Response Pending", "پاسخ در انتظار است"),
    VEHICLE_NOT_IN_DRIVE_CYCLE(0x99, "Vehicle Not In Drive Cycle", "وسیله در چرخه رانندگی نیست");

    companion object {
        fun fromCode(code: Int): ObdNrc? = entries.firstOrNull { it.code == code }
    }
}

/**
 * §9: structured OBD-II / ISO 14229 negative response (7F <service> <NRC>).
 * Represents "ECU explicitly rejected the request" — distinct from "ECU did not
 * respond" (which surfaces as NotConnectedException / timeout elsewhere). Unknown
 * NRCs are retained numerically ([nrcCode]); [nrc] is null only when unrecognized.
 */
data class NegativeResponse(
    val requestedService: Int,
    val nrc: ObdNrc?,
    val nrcCode: Int,
    val rawLine: String,
    val responder: String? = null
)
