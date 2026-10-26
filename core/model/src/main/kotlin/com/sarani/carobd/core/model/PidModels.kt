package com.sarani.carobd.core.model

enum class PollingPriority {
    FAST,       // e.g. RPM, Speed (every 50-100ms)
    MEDIUM,     // e.g. Throttle, MAP, MAF, O2 (every 200-500ms)
    SLOW,       // e.g. Coolant, Fuel level, Ambient temp (every 1-3s)
    STATIC      // e.g. VIN, CALID (queried once on connect)
}

enum class PidCategory(val titleFa: String, val titleEn: String) {
    ENGINE("موتور و احتراق", "Engine & Combustion"),
    FUEL_AIR("سوخت و هوای ورودی", "Fuel & Air Intake"),
    TEMPERATURE("دماها و حرارت", "Temperatures"),
    ELECTRICAL("برق و باتری", "Electrical & Voltage"),
    EMISSIONS("آلایندگی و اگزوز", "Emissions & Exhaust"),
    TRANSMISSION("گیربکس و انتقال قدرت", "Transmission & Drivetrain"),
    IRANIAN_ECU("اختصاصی خودروهای ایرانی", "Iranian ECU Proprietary"),
    MOTORCYCLE("موتورسیکلت و خودروهای چینی", "Motorcycle & Chinese ECUs")
}

data class PidDefinition(
    val id: String,
    val serviceModeHex: String = "01",
    val pidHex: String,
    val nameEn: String,
    val nameFa: String,
    val descriptionEn: String = "",
    val descriptionFa: String = "",
    val byteCount: Int = 1,
    val minValue: Float = 0f,
    val maxValue: Float = 100f,
    val unitMetric: String,
    val unitImperial: String,
    val category: PidCategory = PidCategory.ENGINE,
    val priority: PollingPriority = PollingPriority.MEDIUM,
    val confidence: ConfidenceLevel = ConfidenceLevel.VERIFIED_STANDARD,
    val formulaDescription: String = ""
)

/**
 * Runtime capability map built dynamically from Mode 01 PID 00, 20, 40, 60, 80, A0
 * bitmap responses from the ECU. Only truly supported parameters are polled.
 */
class PidCapabilityMap(
    val supportedPids: Set<String> = emptySet(),
    val rawBitmapResponses: Map<String, String> = emptyMap()
) {
    fun isSupported(pidHex: String): Boolean {
        val normalized = pidHex.uppercase().removePrefix("01").trim()
        return supportedPids.contains(normalized)
    }

    companion object {
        /**
         * Decodes standard SAE J1979 32-bit bitmap responses.
         * Example: PID 00 response "BE 3E B8 11"
         */
        fun parseFromBitmaps(bitmapMap: Map<String, List<Int>>): PidCapabilityMap {
            val supported = mutableSetOf<String>()
            val rawStrings = mutableMapOf<String, String>()

            for ((basePidHex, bytes) in bitmapMap) {
                val baseInt = basePidHex.toIntOrNull(16) ?: continue
                rawStrings[basePidHex] = bytes.joinToString(" ") { String.format(java.util.Locale.US, "%02X", it) }

                for (byteIndex in 0 until minOf(bytes.size, 4)) {
                    val b = bytes[byteIndex]
                    for (bit in 7 downTo 0) {
                        val isSet = (b and (1 shl bit)) != 0
                        if (isSet) {
                            val pidOffset = (byteIndex * 8) + (7 - bit) + 1
                            val calculatedPid = baseInt + pidOffset
                            supported.add(String.format(java.util.Locale.US, "%02X", calculatedPid))
                        }
                    }
                }
            }
            return PidCapabilityMap(supported, rawStrings)
        }
    }
}

/**
 * Inspection/Maintenance (I/M) Readiness Monitor states per SAE J1979
 */
data class ReadinessMonitor(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val isSupported: Boolean,
    val isComplete: Boolean
)

data class ReadinessStatus(
    val milActive: Boolean,
    val dtcCount: Int,
    val monitors: List<ReadinessMonitor>
)
