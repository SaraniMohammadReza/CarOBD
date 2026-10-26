package com.sarani.carobd.core.calculator

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.telemetry.fuel.FuelEconomyCalculator

data class FuelEconomyMetrics(
    val instantLitersPer100Km: Float = 0f,
    val instantLitersPerHour: Float = 0f,
    val instantKmPerLiter: Float = 0f,
    val tripAverageLitersPer100Km: Float = 0f,
    val remainingRangeKm: Int = 0,
    val fuelCostPerKmTomans: Float = 0f
)

object FuelEconomyEngine {

    /**
     * Calculates instant and average fuel metrics using physical formulas from FuelEconomyCalculator.
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

        if (rpm <= 0 && speed <= 0) {
            return FuelEconomyMetrics()
        }

        val fuelLitersPerHour = if (maf > 0.1f) {
            FuelEconomyCalculator.calculateFuelRateFromMaf(maf.toDouble()).toFloat()
        } else {
            FuelEconomyCalculator.calculateFuelRateFromSpeedDensity(
                mapKpa = telemetry.intakeManifoldPressureKpa.toDouble(),
                iatCelsius = telemetry.intakeAirTempCelsius.toDouble(),
                rpm = rpm.toDouble(),
                displacementLiters = 1.6,
                volumetricEfficiency = 0.80
            ).toFloat()
        }

        val instantL100km = if (speed > 3) {
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
        val estRangeKm = if (fuelLevel > 0f) {
            ((remainingFuelLiters / avgConsumption) * 100f).toInt().coerceIn(0, 1200)
        } else {
            0
        }

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
