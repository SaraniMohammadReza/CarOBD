package com.sarani.carobd.core.calculator

import com.sarani.carobd.core.storage.UnitSystem
import java.util.Locale

/**
 * Pure, side-effect-free unit conversions at the presentation/domain boundary.
 *
 * Raw telemetry stays canonical (metric) internally; callers convert at display.
 * Unknown / non-finite values render as an em dash ("—"), never a fabricated 0.
 *
 * Conversion factors:
 * - 1 mile = 1.609344 km (international)
 * - 1 psi  = 6.8947572931 kPa (ISO 1 bar ≈ 14.5038 psi)
 */
object UnitConverter {

    private const val KM_PER_MILE = 1.609344
    private const val KPA_PER_PSI = 6.8947572931

    // ---- raw value conversions ----
    fun kmhToMph(kmh: Double) = kmh / KM_PER_MILE
    fun mphToKmh(mph: Double) = mph * KM_PER_MILE
    fun kmToMiles(km: Double) = km / KM_PER_MILE
    fun milesToKm(miles: Double) = miles * KM_PER_MILE
    fun celsiusToFahrenheit(c: Double) = c * 9.0 / 5.0 + 32.0
    fun fahrenheitToCelsius(f: Double) = (f - 32.0) * 5.0 / 9.0
    fun kpaToPsi(kpa: Double) = kpa / KPA_PER_PSI
    fun psiToKpa(psi: Double) = psi * KPA_PER_PSI

    // ---- presentation helpers (value + unit label per system) ----
    fun formatSpeed(valueKmh: Double, system: UnitSystem): Pair<String, String> =
        formatted(if (system == UnitSystem.IMPERIAL) kmhToMph(valueKmh) else valueKmh) to
            (if (system == UnitSystem.IMPERIAL) "mph" else "km/h")

    fun formatDistance(valueKm: Double, system: UnitSystem): Pair<String, String> =
        formatted(if (system == UnitSystem.IMPERIAL) kmToMiles(valueKm) else valueKm) to
            (if (system == UnitSystem.IMPERIAL) "mi" else "km")

    fun formatTemperature(valueCelsius: Double, system: UnitSystem): Pair<String, String> =
        formatted(if (system == UnitSystem.IMPERIAL) celsiusToFahrenheit(valueCelsius) else valueCelsius) to
            (if (system == UnitSystem.IMPERIAL) "°F" else "°C")

    fun formatPressure(valueKpa: Double, system: UnitSystem): Pair<String, String> =
        formatted(if (system == UnitSystem.IMPERIAL) kpaToPsi(valueKpa) else valueKpa) to
            (if (system == UnitSystem.IMPERIAL) "psi" else "kPa")

    private fun formatted(value: Double): String =
        if (value.isNaN() || value.isInfinite()) "—" else String.format(Locale.US, "%.1f", value)
}
