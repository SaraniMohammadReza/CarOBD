package com.sarani.carobd.core.logger

import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.*

data class TripSummary(
    val isRecording: Boolean = false, val startTimeMs: Long = 0, val durationSeconds: Long = 0,
    val distanceKm: Double = 0.0, val maxSpeedKmh: Int = 0, val maxRpm: Int = 0, val averageSpeedKmh: Double = 0.0,
    val hardAccelerationsCount: Int = 0, val hardBrakingCount: Int = 0,
    val estimatedFuelUsedLiters: Double? = null, val samplesCount: Int = 0,
    val speedSamples: Int = 0, val rpmSamples: Int = 0, val coverageLimited: Boolean = false,
    val limitReached: Boolean = false,
)
data class TelemetrySample(val timestampMs: Long, val rpm: Float?, val speedKmh: Float?, val coolantTemp: Float?,
    val throttlePercent: Float?, val voltage: Float?, val engineLoad: Float?)

/** Captures measured samples. Distance integrates observed time, never an assumed polling interval. */
class TripLogger {
    private val _tripState = MutableStateFlow(TripSummary())
    val tripState = _tripState.asStateFlow()
    private val samples = mutableListOf<TelemetrySample>()
    private var lastSpeed: Pair<Long, Float>? = null
    private var coveredMs = 0L
    @Synchronized fun startTrip(nowMs: Long = System.currentTimeMillis()) {
        samples.clear(); lastSpeed = null; coveredMs = 0
        _tripState.value = TripSummary(isRecording = true, startTimeMs = nowMs)
    }
    @Synchronized fun stopTrip() { _tripState.value = _tripState.value.copy(isRecording = false) }
    @Synchronized fun logSample(telemetry: LiveTelemetry, nowMs: Long = System.currentTimeMillis()) {
        val current = _tripState.value
        if (!current.isRecording || telemetry.timestamp <= (samples.lastOrNull()?.timestampMs ?: Long.MIN_VALUE)) return
        fun measured(pid: String) = telemetry.signal(pid, nowMs).takeIf { it.quality == SignalQuality.GOOD }?.value
        val rpm = measured("010C"); val speed = measured("010D"); val coolant = measured("0105")
        val throttle = measured("0111"); val voltage = measured("ATRV"); val load = measured("0104")
        if (listOf(rpm, speed, coolant, throttle, voltage, load).all { it == null }) {
            lastSpeed = null
            _tripState.value = current.copy(coverageLimited = true)
            return
        }
        if (samples.size >= 20_000) { _tripState.value = current.copy(isRecording = false, limitReached = true); return }
        var distance = current.distanceKm
        var accel = current.hardAccelerationsCount; var braking = current.hardBrakingCount
        var limited = current.coverageLimited
        if (speed != null) {
            val time = telemetry.measurementTimesMs["0D"] ?: telemetry.timestamp
            lastSpeed?.let { (before, previousSpeed) ->
                val dt = time - before
                if (dt in 1..10_000) {
                    distance += (previousSpeed + speed) / 2.0 * dt / 3_600_000
                    coveredMs += dt
                    val changePerSecond = (speed - previousSpeed) * 1000 / dt
                    if (changePerSecond > 15 && throttle != null && throttle > 70) accel++
                    if (changePerSecond < -20) braking++
                } else if (dt > 10_000 || dt < 0) limited = true
            }
            lastSpeed = time to speed
        } else { lastSpeed = null; limited = true }
        samples += TelemetrySample(telemetry.timestamp, rpm, speed, coolant, throttle, voltage, load)
        _tripState.value = current.copy(durationSeconds = ((telemetry.timestamp - current.startTimeMs) / 1000).coerceAtLeast(0),
            distanceKm = distance, maxSpeedKmh = speed?.toInt()?.let { maxOf(current.maxSpeedKmh, it) } ?: current.maxSpeedKmh,
            maxRpm = rpm?.toInt()?.let { maxOf(current.maxRpm, it) } ?: current.maxRpm,
            averageSpeedKmh = if (coveredMs > 0) distance * 3_600_000 / coveredMs else 0.0,
            hardAccelerationsCount = accel, hardBrakingCount = braking, samplesCount = samples.size,
            speedSamples = current.speedSamples + if (speed != null) 1 else 0,
            rpmSamples = current.rpmSamples + if (rpm != null) 1 else 0, coverageLimited = limited)
    }
    @Synchronized fun exportToCsv(): String = buildString {
        append("Timestamp,RPM,Speed_kmh,Coolant_C,Throttle_pct,Adapter_ATRV_V,EngineLoad_pct\n")
        samples.forEach { s -> append(listOf(s.timestampMs, s.rpm, s.speedKmh, s.coolantTemp, s.throttlePercent, s.voltage, s.engineLoad).joinToString(",") { it?.toString() ?: "" }); append('\n') }
    }
}
