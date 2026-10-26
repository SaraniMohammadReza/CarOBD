package com.sarani.carobd.core.telemetry.trip

import com.sarani.carobd.core.model.DrivingStyleMetrics
import com.sarani.carobd.core.model.TripRecord

/**
 * Real-time automotive trip computer and driving behavior analyzer.
 */
class TripComputer(
    val tripId: String,
    val vehicleId: String,
    val startTimeMs: Long = System.currentTimeMillis()
) {

    private var lastSampleTimeMs: Long = startTimeMs
    private var lastSpeedKmh: Int = 0

    private var accumulatedDistanceKm: Double = 0.0
    private var accumulatedFuelLiters: Double = 0.0

    private var maxSpeedKmh: Int = 0
    private var maxRpm: Int = 0
    private var totalSpeedSamples: Long = 0L
    private var speedSampleSum: Double = 0.0

    private var hardAccelerations: Int = 0
    private var hardBrakings: Int = 0
    private var highRpmDurationSeconds: Long = 0L
    private var idleDurationSeconds: Long = 0L
    private var sampleCount: Int = 0

    /**
     * Ingests a live telemetry sample snapshot.
     * @param speedKmh Current speed in km/h
     * @param rpm Current engine RPM
     * @param fuelRateLitersPerHour Current instantaneous fuel consumption in L/h
     * @param timestampMs Sample timestamp
     */
    @Synchronized
    fun processSample(
        speedKmh: Int,
        rpm: Int,
        fuelRateLitersPerHour: Double,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        val dtMs = (timestampMs - lastSampleTimeMs).coerceAtLeast(0L)
        val dtHours = dtMs / (1000.0 * 3600.0)
        val dtSeconds = dtMs / 1000.0

        if (speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh
        if (rpm > maxRpm) maxRpm = rpm

        // Distance integration: avg speed during interval * dt
        val avgIntervalSpeed = (speedKmh + lastSpeedKmh) / 2.0
        accumulatedDistanceKm += avgIntervalSpeed * dtHours

        // Fuel integration: fuelRate * dt
        accumulatedFuelLiters += fuelRateLitersPerHour * dtHours

        // Driving behavior metrics:
        if (dtSeconds in 0.2..3.0) {
            val deltaSpeedKmhPerSec = (speedKmh - lastSpeedKmh) / dtSeconds
            if (deltaSpeedKmhPerSec > 11.0) { // > ~3.0 m/s^2 acceleration
                hardAccelerations++
            } else if (deltaSpeedKmhPerSec < -13.0) { // < ~-3.6 m/s^2 braking
                hardBrakings++
            }
        }

        if (rpm > 4500) {
            highRpmDurationSeconds += dtSeconds.toLong()
        }

        if (rpm > 400 && speedKmh == 0) {
            idleDurationSeconds += dtSeconds.toLong()
        }

        speedSampleSum += speedKmh
        totalSpeedSamples++
        sampleCount++

        lastSpeedKmh = speedKmh
        lastSampleTimeMs = timestampMs
    }

    /**
     * Generates a frozen snapshot of the current trip record.
     */
    @Synchronized
    fun getTripRecord(nowMs: Long = System.currentTimeMillis()): TripRecord {
        val durationSeconds = ((nowMs - startTimeMs) / 1000L).coerceAtLeast(1L)
        val avgSpeed = if (totalSpeedSamples > 0) speedSampleSum / totalSpeedSamples else 0.0

        // Calculate scores:
        val smoothnessPenalty = (hardAccelerations * 4 + hardBrakings * 5).coerceAtMost(50)
        val smoothnessScore = (100 - smoothnessPenalty).coerceIn(10, 100)

        val highRpmRatio = (highRpmDurationSeconds.toDouble() / durationSeconds).coerceIn(0.0, 1.0)
        val ecoPenalty = ((highRpmRatio * 40) + (hardAccelerations * 3)).toInt().coerceAtMost(60)
        val ecoScore = (100 - ecoPenalty).coerceIn(10, 100)

        return TripRecord(
            id = tripId,
            vehicleId = vehicleId,
            startTimeMs = startTimeMs,
            endTimeMs = nowMs,
            durationSeconds = durationSeconds,
            distanceKm = accumulatedDistanceKm,
            maxSpeedKmh = maxSpeedKmh,
            maxRpm = maxRpm,
            averageSpeedKmh = avgSpeed,
            estimatedFuelUsedLiters = accumulatedFuelLiters,
            drivingStyle = DrivingStyleMetrics(
                hardAccelerationsCount = hardAccelerations,
                hardBrakingCount = hardBrakings,
                highRpmDurationSeconds = highRpmDurationSeconds,
                idleDurationSeconds = idleDurationSeconds,
                smoothnessScore = smoothnessScore,
                ecoScore = ecoScore
            ),
            sampleCount = sampleCount
        )
    }
}
