package com.sarani.carobd.service

import com.sarani.carobd.core.calculator.UnitConverter
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.SignalQuality
import com.sarani.carobd.core.storage.UnitSystem
import java.util.Locale

/** Presentation only: reads the existing shared snapshot, never starts polling. */
internal data class NotificationMeasurements(val speed: String, val rpm: String, val coolant: String, val voltage: String) {
    companion object {
        fun from(telemetry: LiveTelemetry, units: UnitSystem, nowMs: Long): NotificationMeasurements {
            fun fresh(pid: String) = telemetry.signal(pid, nowMs).takeIf { it.quality == SignalQuality.GOOD }?.value?.toDouble()
            fun Pair<String, String>.label() = "$first $second"
            return NotificationMeasurements(
                UnitConverter.formatSpeed(fresh("010D") ?: Double.NaN, units).label(),
                fresh("010C")?.let { String.format(Locale.US, "%.0f", it) } ?: "—",
                UnitConverter.formatTemperature(fresh("0105") ?: Double.NaN, units).label(),
                fresh("ATRV")?.let { String.format(Locale.US, "%.1f", it) } ?: "—"
            )
        }
    }
}
