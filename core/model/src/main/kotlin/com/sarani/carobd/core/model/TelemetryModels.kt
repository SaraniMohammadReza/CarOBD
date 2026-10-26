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
    val sourceEcu: String = "7E8",
    val quality: SignalQuality = SignalQuality.GOOD
)

data class LiveTelemetry(
    val rpm: Int = 0,
    val speedKmh: Int = 0,
    val coolantTempCelsius: Int = 0,
    val throttlePositionPercent: Float = 0f,
    val batteryVoltage: Float = 12.6f,
    val intakeAirTempCelsius: Int = 25,
    val engineLoadPercent: Float = 0f,
    val fuelLevelPercent: Float = 50f,
    val mafGramsPerSec: Float = 0f,
    val intakeManifoldPressureKpa: Int = 101, // MAP sensor
    val timingAdvanceDegrees: Float = 10f,

    // Fuel Trim & Air-Fuel Ratio
    val shortTermFuelTrimPercent: Float = 0f,
    val longTermFuelTrimPercent: Float = 0f,
    val oxygenSensor1Volt: Float = 0.45f,
    val oxygenSensor2Volt: Float = 0.45f,
    val fuelRailPressureKpa: Float = 350f,
    val catalystTempCelsius: Float = 450f,

    // Specialized Iranian ECU parameters
    val stepperMotorSteps: Int = 45,
    val knockRetardDegrees: Float = 0f,
    val fanLowSpeedActive: Boolean = false,
    val fanHighSpeedActive: Boolean = false,
    val cngPressureBar: Float = 0f,

    val timestamp: Long = System.currentTimeMillis()
)
