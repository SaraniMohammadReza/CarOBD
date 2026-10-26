package com.sarani.carobd.core.telemetry.scheduler

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.SignalQuality
import com.sarani.carobd.core.model.TelemetrySample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Polling priority levels for sensor query scheduling.
 */
enum class PollingTier(val intervalCycles: Int) {
    FAST(1),      // Polled every cycle (RPM, Speed, Throttle)
    MEDIUM(3),    // Polled every 3 cycles (Engine Load, MAP, MAF, Voltage)
    SLOW(10)      // Polled every 10 cycles (Coolant, Fuel Level, Ambient Temp)
}

data class ScheduledPid(
    val pidHex: String,
    val tier: PollingTier,
    val description: String
)

/**
 * Priority-aware telemetry scheduler with dynamic latency adaptation.
 */
class TelemetryScheduler {

    private val _liveTelemetry = MutableStateFlow(LiveTelemetry())
    val liveTelemetry: StateFlow<LiveTelemetry> = _liveTelemetry.asStateFlow()

    private var cycleCounter = 0L
    private var rollingAverageRttMs = 50L

    /**
     * Updates rolling round-trip latency measurement to dynamically adjust polling pace.
     */
    fun recordRtt(rttMs: Long) {
        val clamped = rttMs.coerceIn(5L, 2000L)
        rollingAverageRttMs = ((rollingAverageRttMs * 7) + (clamped * 3)) / 10
    }

    /**
     * Recommended delay in milliseconds before dispatching the next PID query.
     */
    val recommendedPacingDelayMs: Long
        get() = when {
            rollingAverageRttMs < 30 -> 10L
            rollingAverageRttMs < 80 -> 25L
            rollingAverageRttMs < 150 -> 50L
            else -> 100L
        }

    /**
     * Determines whether a given PID should be polled in the current cycle.
     */
    fun shouldPoll(tier: PollingTier): Boolean {
        return cycleCounter % tier.intervalCycles == 0L
    }

    /**
     * Advances to the next polling cycle.
     */
    fun advanceCycle() {
        cycleCounter++
    }

    /**
     * Updates the consolidated live telemetry state with a decoded PID value.
     */
    fun updateMetric(pidHex: String, value: Double) {
        val current = _liveTelemetry.value
        val normalizedPid = pidHex.uppercase().removePrefix("01").trim()

        val updated = when (normalizedPid) {
            "0C" -> current.copy(rpm = value.toInt(), timestamp = System.currentTimeMillis())
            "0D" -> current.copy(speedKmh = value.toInt(), timestamp = System.currentTimeMillis())
            "05" -> current.copy(coolantTempCelsius = value.toInt(), timestamp = System.currentTimeMillis())
            "11" -> current.copy(throttlePositionPercent = value.toFloat(), timestamp = System.currentTimeMillis())
            "42" -> current.copy(batteryVoltage = value.toFloat(), timestamp = System.currentTimeMillis())
            "0F" -> current.copy(intakeAirTempCelsius = value.toInt(), timestamp = System.currentTimeMillis())
            "04" -> current.copy(engineLoadPercent = value.toFloat(), timestamp = System.currentTimeMillis())
            "2F" -> current.copy(fuelLevelPercent = value.toFloat(), timestamp = System.currentTimeMillis())
            "10" -> current.copy(mafGramsPerSec = value.toFloat(), timestamp = System.currentTimeMillis())
            "0B" -> current.copy(intakeManifoldPressureKpa = value.toInt(), timestamp = System.currentTimeMillis())
            "0E" -> current.copy(timingAdvanceDegrees = value.toFloat(), timestamp = System.currentTimeMillis())
            "06" -> current.copy(shortTermFuelTrimPercent = value.toFloat(), timestamp = System.currentTimeMillis())
            "07" -> current.copy(longTermFuelTrimPercent = value.toFloat(), timestamp = System.currentTimeMillis())
            "23" -> current.copy(fuelRailPressureKpa = value.toFloat(), timestamp = System.currentTimeMillis())
            else -> current
        }

        _liveTelemetry.value = updated
    }
}
