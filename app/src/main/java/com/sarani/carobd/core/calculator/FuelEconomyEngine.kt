package com.sarani.carobd.core.calculator

import com.sarani.carobd.core.model.LiveTelemetry

data class FuelEconomyMetrics(
    val instantLitersPer100Km: Float = 0f,
    val instantLitersPerHour: Float = 0f,
    val instantKmPerLiter: Float = 0f,
    val tripAverageLitersPer100Km: Float = 7.8f,
    val remainingRangeKm: Int = 450,
    val fuelCostPerKmTomans: Float = 240f
)

object FuelEconomyEngine {

    /**
     * Calculates instant and average fuel metrics using air/fuel mass calculations.
     * Stoichiometric ratio for gasoline: 14.7 grams air per 1 gram fuel.
     * Density of gasoline: ~740 grams / liter.
     */
    fun calculate(
        telemetry: LiveTelemetry,
        tankCapacityLiters: Float = 50f,
        fuelPricePerLiterTomans: Float = 3000f,
        averageTripLitersPer100km: Float = 7.5f
    ): FuelEconomyMetrics {
        val speed = telemetry.speedKmh
        val rpm = telemetry.rpm
        val maf = telemetry.mafGramsPerSec
        val fuelLevel = telemetry.fuelLevelPercent

        // If MAF is zero, estimate from MAP & RPM & Engine Displacement (e.g. 1.6L)
        val airGramsPerSec = if (maf > 0.5f) {
            maf
        } else {
            // Speed-Density formula: (RPM * MAP / 2) * (Displacement / 60) * VE / (R * T)
            val estGrams = (rpm.toFloat() * telemetry.intakeManifoldPressureKpa.toFloat() * 1.6f) / 12000f
            estGrams.coerceIn(1.5f, 180f)
        }

        // Fuel grams per sec = air / 14.7 (adjusted by lambda if available)
        val fuelGramsPerSec = airGramsPerSec / 14.7f
        val fuelLitersPerSec = fuelGramsPerSec / 740f
        val fuelLitersPerHour = fuelLitersPerSec * 3600f

        val instantL100km = if (speed > 5) {
            (fuelLitersPerHour / speed.toFloat()) * 100f
        } else {
            0f
        }

        val instantKmL = if (instantL100km > 0.1f) {
            100f / instantL100km
        } else {
            0f
        }

        // Remaining range: (current fuel in liters / average consumption) * 100
        val remainingFuelLiters = (fuelLevel / 100f) * tankCapacityLiters
        val avgConsumption = averageTripLitersPer100km.coerceAtLeast(4.0f)
        val estRangeKm = ((remainingFuelLiters / avgConsumption) * 100f).toInt().coerceIn(0, 1200)

        // Cost per km
        val costPerKm = (avgConsumption / 100f) * fuelPricePerLiterTomans

        return FuelEconomyMetrics(
            instantLitersPer100Km = instantL100km.coerceIn(0f, 35f),
            instantLitersPerHour = fuelLitersPerHour.coerceIn(0f, 25f),
            instantKmPerLiter = instantKmL.coerceIn(0f, 40f),
            tripAverageLitersPer100Km = averageTripLitersPer100km,
            remainingRangeKm = estRangeKm,
            fuelCostPerKmTomans = costPerKm
        )
    }
}
