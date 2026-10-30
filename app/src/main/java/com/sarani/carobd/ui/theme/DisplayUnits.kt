package com.sarani.carobd.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.sarani.carobd.core.calculator.UnitConverter
import com.sarani.carobd.core.storage.UnitSystem

val LocalUnitSystem = staticCompositionLocalOf { UnitSystem.METRIC }
data class DisplayMeasurement(val value: Double, val unit: String)
fun displayMeasurement(value: Double, unit: String, system: UnitSystem): DisplayMeasurement {
    if (system == UnitSystem.METRIC) return DisplayMeasurement(value, unit)
    return when (unit) {
        "km/h" -> DisplayMeasurement(UnitConverter.kmhToMph(value), "mph")
        "km" -> DisplayMeasurement(UnitConverter.kmToMiles(value), "mi")
        "°C" -> DisplayMeasurement(UnitConverter.celsiusToFahrenheit(value), "°F")
        "kPa" -> DisplayMeasurement(UnitConverter.kpaToPsi(value), "psi")
        "bar" -> DisplayMeasurement(UnitConverter.kpaToPsi(value * 100), "psi")
        else -> DisplayMeasurement(value, unit)
    }
}
