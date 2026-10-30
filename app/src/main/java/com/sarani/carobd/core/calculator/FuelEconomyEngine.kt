package com.sarani.carobd.core.calculator

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.telemetry.fuel.FuelEconomyCalculator

data class FuelEconomyMetrics(
    val instantLitersPer100Km: Float? = null,
    val instantLitersPerHour: Float? = null,
    val instantKmPerLiter: Float? = null,
    val tripAverageLitersPer100Km: Float? = null,
    val remainingRangeKm: Int? = null,
    val fuelCostPerKmTomans: Float? = null,
    val source: String? = null,
)

object FuelEconomyEngine {
    /** Display estimates only. Missing/stale inputs and absent supplied averages remain null. */
    fun calculate(telemetry: LiveTelemetry, tankCapacityLiters: Float? = null,
        fuelPricePerLiterTomans: Float? = null, averageTripLitersPer100km: Float? = null,
        nowMs: Long = System.currentTimeMillis()): FuelEconomyMetrics {
        fun measured(pid: String) = telemetry.signal(pid, nowMs).takeIf { it.quality == SignalQuality.GOOD }?.value
        val speed = measured("010D")
        val rate = measured("0110")?.takeIf { it.isFinite() && it > 0 }?.let {
            FuelEconomyCalculator.fuelRateIfMeasurable(mafGramsPerSec = it.toDouble())?.toFloat()
        }
        val consumption = if (rate != null && speed != null && speed > 3) rate / speed * 100 else null
        val average = averageTripLitersPer100km?.takeIf { it.isFinite() && it > 0 }
        val capacity = tankCapacityLiters?.takeIf { it.isFinite() && it > 0 }
        val level = measured("012F")?.takeIf { it in 0f..100f }
        val price = fuelPricePerLiterTomans?.takeIf { it.isFinite() && it >= 0 }
        val range = if (average != null && capacity != null && level != null)
            (level / 100 * capacity / average * 100).takeIf { it.isFinite() && it <= Int.MAX_VALUE }?.toInt() else null
        return FuelEconomyMetrics(consumption, rate, consumption?.takeIf { it > 0 }?.let { 100 / it },
            average, range, if (average != null && price != null) average / 100 * price else null,
            if (rate != null) "MAF_DERIVED_GASOLINE_AFR14.7_DENSITY745" else null)
    }
}
