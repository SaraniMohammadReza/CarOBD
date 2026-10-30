package com.sarani.carobd.core.database

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.protocol.obd2.PidPollingPlan

/** Selection requires both a canonical decoder/poll path and session evidence of support. */
enum class DashboardSensorAvailability { AVAILABLE, UNKNOWN, UNSUPPORTED, NOT_POLLED }

fun dashboardSensorAvailability(sensor: SensorDefinition, telemetry: LiveTelemetry): DashboardSensorAvailability {
    if (sensor.pid == "ATRV") return if (telemetry.measurementTimesMs.containsKey("ATRV") && telemetry.signal("ATRV").value != null)
        DashboardSensorAvailability.AVAILABLE else DashboardSensorAvailability.UNKNOWN
    if (PidPollingPlan.BASELINE.none { it.command == sensor.pid }) return DashboardSensorAvailability.NOT_POLLED
    val support = telemetry.pidSupport as? PidSupportState.Known ?: return DashboardSensorAvailability.UNKNOWN
    return if (support.map.isSupported(sensor.pid.removePrefix("01"))) DashboardSensorAvailability.AVAILABLE else DashboardSensorAvailability.UNSUPPORTED
}
