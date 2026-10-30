package com.sarani.carobd.core.model

enum class SignalQuality {
    GOOD,
    STALE,
    ESTIMATED,
    INVALID,
    UNAVAILABLE
}

data class TelemetrySample(
    val timestampMs: Long,
    val pidHex: String,
    val value: Float,
    val unit: String,
    val rawHex: String,
    val sourceEcu: String? = null,
    val quality: SignalQuality = SignalQuality.GOOD
)

data class LiveTelemetry(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempCelsius: Int = 0,
    val throttlePositionPercent: Float = 0f,
    /** Adapter supply voltage measured by ATRV, never PID 0142. */
    val batteryVoltage: Float = 0f,
    val controlModuleVoltage: Float? = null,
    val intakeAirTempCelsius: Int = 0,
    val engineLoadPercent: Float = 0f,
    val fuelLevelPercent: Float = 0f,
    val mafGramsPerSec: Float = 0f,
    val intakeManifoldPressureKpa: Int = 0, // MAP sensor
    val timingAdvanceDegrees: Float = 0f,

    // Fuel Trim & Air-Fuel Ratio
    val shortTermFuelTrimPercent: Float = 0f,
    val longTermFuelTrimPercent: Float = 0f,
    val oxygenSensor1Volt: Float = 0f,
    val oxygenSensor2Volt: Float = 0f,
    val fuelRailPressureKpa: Float = 0f,
    val catalystTempCelsius: Float = 0f,

    // Specialized Iranian ECU parameters
    val stepperMotorSteps: Int = 0,
    val knockRetardDegrees: Float = 0f,
    val fanLowSpeedActive: Boolean = false,
    val fanHighSpeedActive: Boolean = false,
    val cngPressureBar: Float = 0f,

    val measuredPids: Set<String> = emptySet(),
    val measurementTimesMs: Map<String, Long> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun signal(pidHex: String, nowMs: Long = System.currentTimeMillis()): TelemetrySignal {
        val pid = pidHex.uppercase().replace(" ", "").removePrefix("01")
        val observed = measurementTimesMs[pid] ?: timestamp.takeIf { isMeasured(pidHex) }
        if (observed == null) return TelemetrySignal(null, SignalQuality.UNAVAILABLE)
        val value = when (pid) {
            "0C" -> rpm.toFloat(); "0D" -> speedKmh.toFloat(); "05" -> coolantTempCelsius.toFloat()
            "11" -> throttlePositionPercent; "ATRV" -> batteryVoltage; "42" -> controlModuleVoltage
            "04" -> engineLoadPercent; "2F" -> fuelLevelPercent; "0F" -> intakeAirTempCelsius.toFloat()
            "10" -> mafGramsPerSec; "0B" -> intakeManifoldPressureKpa.toFloat()
            "14" -> oxygenSensor1Volt; "15" -> oxygenSensor2Volt
            else -> null
        }
        if (value == null || !value.isFinite()) return TelemetrySignal(null, SignalQuality.UNAVAILABLE)
        return TelemetrySignal(value, if (!isMeasured(pidHex) || nowMs < observed || nowMs - observed > 10_000) SignalQuality.STALE else SignalQuality.GOOD)
    }
    fun isMeasured(pidHex: String): Boolean = measuredPids.contains(pidHex.uppercase().removePrefix("01").trim())
}

data class TelemetrySignal(val value: Float?, val quality: SignalQuality)
