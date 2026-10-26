package com.sarani.carobd.core.logger

import com.sarani.carobd.core.model.LiveTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TripSummary(
    val isRecording: Boolean = false,
    val startTimeMs: Long = 0L,
    val durationSeconds: Long = 0L,
    val distanceKm: Double = 0.0,
    val maxSpeedKmh: Int = 0,
    val maxRpm: Int = 0,
    val averageSpeedKmh: Double = 0.0,
    val hardAccelerationsCount: Int = 0,
    val hardBrakingCount: Int = 0,
    val estimatedFuelUsedLiters: Double = 0.0,
    val samplesCount: Int = 0
)

data class TelemetrySample(
    val timestampMs: Long,
    val rpm: Int,
    val speedKmh: Int,
    val coolantTemp: Int,
    val throttlePercent: Float,
    val voltage: Float,
    val engineLoad: Float
)

class TripLogger {

    private val _tripState = MutableStateFlow(TripSummary())
    val tripState: StateFlow<TripSummary> = _tripState.asStateFlow()

    private val recordedSamples = mutableListOf<TelemetrySample>()
    private var lastSpeed = 0
    private var speedSum = 0L

    fun startTrip() {
        recordedSamples.clear()
        speedSum = 0L
        lastSpeed = 0
        _tripState.value = TripSummary(
            isRecording = true,
            startTimeMs = System.currentTimeMillis()
        )
    }

    fun stopTrip() {
        _tripState.value = _tripState.value.copy(isRecording = false)
    }

    fun logSample(telemetry: LiveTelemetry) {
        val current = _tripState.value
        if (!current.isRecording) return

        val now = System.currentTimeMillis()
        val durationSec = (now - current.startTimeMs) / 1000

        val speed = telemetry.speedKmh
        val rpm = telemetry.rpm
        speedSum += speed
        val sampleCount = current.samplesCount + 1

        // Speed delta for aggressive driving detection
        var hardAcc = current.hardAccelerationsCount
        var hardBrake = current.hardBrakingCount
        val speedDelta = speed - lastSpeed

        if (speedDelta > 15 && telemetry.throttlePositionPercent > 70f) {
            hardAcc++
        } else if (speedDelta < -20) {
            hardBrake++
        }
        lastSpeed = speed

        // Integrate distance: speed (km/h) * (time in hours)
        // Polling interval ~0.1s = 0.1 / 3600 hours
        val deltaDistanceKm = (speed.toDouble() * 0.1) / 3600.0
        val totalDistance = current.distanceKm + deltaDistanceKm

        // Rough fuel consumption estimation (grams/sec from load and RPM or MAF)
        val deltaFuelLiters = (telemetry.engineLoadPercent * 0.0001)

        val sample = TelemetrySample(
            timestampMs = now,
            rpm = rpm,
            speedKmh = speed,
            coolantTemp = telemetry.coolantTempCelsius,
            throttlePercent = telemetry.throttlePositionPercent,
            voltage = telemetry.batteryVoltage,
            engineLoad = telemetry.engineLoadPercent
        )
        recordedSamples.add(sample)

        _tripState.value = current.copy(
            durationSeconds = durationSec,
            distanceKm = totalDistance,
            maxSpeedKmh = maxOf(current.maxSpeedKmh, speed),
            maxRpm = maxOf(current.maxRpm, rpm),
            averageSpeedKmh = if (sampleCount > 0) speedSum.toDouble() / sampleCount else 0.0,
            hardAccelerationsCount = hardAcc,
            hardBrakingCount = hardBrake,
            estimatedFuelUsedLiters = current.estimatedFuelUsedLiters + deltaFuelLiters,
            samplesCount = sampleCount
        )
    }

    fun exportToCsv(): String {
        val sb = StringBuilder()
        sb.append("Timestamp,RPM,Speed_kmh,Coolant_C,Throttle_pct,Voltage_V,EngineLoad_pct\n")
        for (s in recordedSamples) {
            sb.append("${s.timestampMs},${s.rpm},${s.speedKmh},${s.coolantTemp},${s.throttlePercent},${s.voltage},${s.engineLoad}\n")
        }
        return sb.toString()
    }
}
