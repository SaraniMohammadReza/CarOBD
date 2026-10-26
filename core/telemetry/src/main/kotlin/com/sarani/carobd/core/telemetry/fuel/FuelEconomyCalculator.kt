package com.sarani.carobd.core.telemetry.fuel

/**
 * Physically validated fuel economy calculation engine.
 * Computes instantaneous consumption using either direct MAF sensor readings
 * or the Speed-Density ideal gas law for MAP-based engines (TU5, XU7, M15, Pride).
 */
object FuelEconomyCalculator {

    const val GASOLINE_DENSITY_G_PER_L = 745.0 // grams per liter at 15°C
    const val STOICHIOMETRIC_AFR = 14.7
    const val R_SPECIFIC_AIR = 0.287058 // kPa * L / (g * K)

    /**
     * Calculates instantaneous fuel rate in Liters per Hour using MAF (Mass Air Flow).
     * @param mafGramsPerSec Air mass flow rate in g/s (Mode 01 PID 10)
     * @param equivalenceRatio Lambda equivalence ratio (default 1.0 for stoichiometric)
     */
    fun calculateFuelRateFromMaf(
        mafGramsPerSec: Double,
        equivalenceRatio: Double = 1.0
    ): Double {
        if (mafGramsPerSec <= 0.0) return 0.0
        val actualAfr = STOICHIOMETRIC_AFR * (if (equivalenceRatio > 0.5) equivalenceRatio else 1.0)
        val fuelGramsPerSec = mafGramsPerSec / actualAfr
        val fuelGramsPerHour = fuelGramsPerSec * 3600.0
        return fuelGramsPerHour / GASOLINE_DENSITY_G_PER_L
    }

    /**
     * Calculates instantaneous fuel rate using Speed-Density method for MAP-equipped vehicles.
     * @param mapKpa Intake Manifold Absolute Pressure in kPa (PID 0B)
     * @param iatCelsius Intake Air Temperature in Celsius (PID 0F)
     * @param rpm Engine RPM (PID 0C)
     * @param displacementLiters Engine displacement (e.g. 1.587 for TU5, 1.645 for EF7, 1.761 for XU7, 1.503 for M15)
     * @param volumetricEfficiency Volumetric efficiency (typically 0.75 to 0.85)
     */
    fun calculateFuelRateFromSpeedDensity(
        mapKpa: Double,
        iatCelsius: Double,
        rpm: Double,
        displacementLiters: Double,
        volumetricEfficiency: Double = 0.80
    ): Double {
        if (rpm <= 0.0 || mapKpa <= 0.0 || displacementLiters <= 0.0) return 0.0

        val tempKelvin = (iatCelsius + 273.15).coerceAtLeast(220.0)
        // Air mass per intake stroke: m = (P * V_cyl * VE) / (R * T)
        // In a 4-stroke engine, displacement volume is aspirated every 2 revolutions (rpm / (2 * 60) per sec)
        val airVolumeFlowPerSec = (displacementLiters * (rpm / 120.0)) * volumetricEfficiency
        val airMassGramsPerSec = (mapKpa * airVolumeFlowPerSec) / (R_SPECIFIC_AIR * tempKelvin)

        return calculateFuelRateFromMaf(airMassGramsPerSec)
    }

    /**
     * Computes final instantaneous fuel consumption.
     * When vehicle is moving (speed > 3 km/h), returns Liters / 100km.
     * When vehicle is stopped/idling (speed <= 3 km/h), returns Liters / hour.
     */
    fun calculateInstantaneousEconomy(
        fuelRateLitersPerHour: Double,
        speedKmh: Double
    ): InstantaneousConsumption {
        if (fuelRateLitersPerHour <= 0.0) {
            return InstantaneousConsumption(0.0, FuelUnit.LITERS_PER_100KM, isIdling = speedKmh <= 3.0)
        }

        return if (speedKmh > 3.0) {
            val lPer100km = (fuelRateLitersPerHour / speedKmh) * 100.0
            InstantaneousConsumption(
                value = lPer100km.coerceIn(0.0, 99.9),
                unit = FuelUnit.LITERS_PER_100KM,
                isIdling = false
            )
        } else {
            InstantaneousConsumption(
                value = fuelRateLitersPerHour.coerceIn(0.0, 25.0),
                unit = FuelUnit.LITERS_PER_HOUR,
                isIdling = true
            )
        }
    }
}

enum class FuelUnit(val symbolEn: String, val symbolFa: String) {
    LITERS_PER_100KM("L/100km", "لیتر در ۱۰۰ کیلومتر"),
    LITERS_PER_HOUR("L/h", "لیتر در ساعت")
}

data class InstantaneousConsumption(
    val value: Double,
    val unit: FuelUnit,
    val isIdling: Boolean
)
