package com.sarani.carobd.core.model

/**
 * Comprehensive real-time vehicle telemetry metrics supporting both standard OBD-II
 * and extended parameters (fuel trims, O2 voltages, manifold pressures, temperatures).
 */
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

    // Fuel Trim & Air-Fuel Ratio (Vital for tuning & diagnosing Iranian ECUs)
    val shortTermFuelTrimPercent: Float = 0f, // STFT (-100% to +99.2%)
    val longTermFuelTrimPercent: Float = 0f,  // LTFT
    val oxygenSensor1Volt: Float = 0.45f,     // O2 sensor upstream (0.1V lean - 0.9V rich)
    val oxygenSensor2Volt: Float = 0.45f,     // O2 sensor downstream (catalyst health)
    val fuelRailPressureKpa: Float = 350f,    // Common rail / fuel pressure
    val catalystTempCelsius: Float = 450f,

    // Specialized Iranian ECU parameters (Siemens/SSAT/Bosch)
    val stepperMotorSteps: Int = 45,          // استپر موتور
    val knockRetardDegrees: Float = 0f,       // ریتارد ناشی از ضربه ناک
    val fanLowSpeedActive: Boolean = false,   // رله فن دور کند
    val fanHighSpeedActive: Boolean = false,  // رله فن دور تند
    val cngPressureBar: Float = 0f,           // فشار مخزن CNG در خودروهای دوگانه‌سوز EF7 و پژو

    val timestamp: Long = System.currentTimeMillis()
)
