package com.sarani.carobd.core.model

data class DrivingStyleMetrics(
    val hardAccelerationsCount: Int = 0,
    val hardBrakingCount: Int = 0,
    val highRpmDurationSeconds: Long = 0L,
    val idleDurationSeconds: Long = 0L,
    val smoothnessScore: Int = 85, // 0 to 100
    val ecoScore: Int = 80         // 0 to 100
)

data class TripRecord(
    val id: String,
    val vehicleId: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSeconds: Long,
    val distanceKm: Double,
    val maxSpeedKmh: Int,
    val maxRpm: Int,
    val averageSpeedKmh: Double,
    val estimatedFuelUsedLiters: Double,
    val drivingStyle: DrivingStyleMetrics = DrivingStyleMetrics(),
    val sampleCount: Int = 0
)
