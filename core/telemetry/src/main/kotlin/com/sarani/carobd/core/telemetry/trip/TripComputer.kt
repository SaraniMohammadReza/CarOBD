package com.sarani.carobd.core.telemetry.trip

import com.sarani.carobd.core.model.*

/** Bounded aggregates only. Gaps, missing inputs and clock rollback never imply coverage. */
class TripComputer(val tripId: String, val vehicleId: String, val startTimeMs: Long = System.currentTimeMillis(),
    private val simulator: Boolean = false) {
    private class Samples {
        var first: Long? = null; var last: Long? = null; var previous: Pair<Long, Double>? = null
        var count = 0; var covered = 0L; var integral = 0.0; var max: Double? = null; var sum = 0.0
        fun add(value: Double?, time: Long) {
            if (value == null || !value.isFinite()) { previous = null; return }
            if (last != null && time <= last!!) { previous = null; return }
            first = first ?: time; last = time; count++; sum += value; max = max?.let { maxOf(it, value) } ?: value
            previous?.let { (t, v) ->
                val dt = time - t
                if (dt in 1..10_000) { covered += dt; integral += (v + value) / 2 * dt / 3_600_000 }
            }
            previous = time to value
        }
    }
    private val speed = Samples(); private val rpm = Samples(); private val fuel = Samples()
    private val coolant = Samples(); private val load = Samples()
    private var samples = 0; private var lastTime: Long? = null
    private var matchedFuelSpeed = true

    @Synchronized fun processSample(speedKmh: Int?, rpm: Int?, fuelRateLitersPerHour: Double?,
        timestampMs: Long = System.currentTimeMillis(), coolantCelsius: Double? = null, engineLoadPercent: Double? = null) {
        if (timestampMs < startTimeMs || lastTime?.let { timestampMs <= it } == true) {
            speed.previous = null; this.rpm.previous = null; fuel.previous = null
            coolant.previous = null; load.previous = null
            return
        }
        samples++; lastTime = timestampMs
        if (speedKmh == null || speedKmh < 0 || fuelRateLitersPerHour == null || !fuelRateLitersPerHour.isFinite() || fuelRateLitersPerHour < 0) matchedFuelSpeed = false
        speed.add(speedKmh?.takeIf { it >= 0 }?.toDouble(), timestampMs)
        this.rpm.add(rpm?.takeIf { it >= 0 }?.toDouble(), timestampMs)
        fuel.add(fuelRateLitersPerHour?.takeIf { it >= 0 }, timestampMs)
        coolant.add(coolantCelsius, timestampMs); load.add(engineLoadPercent?.takeIf { it in 0.0..100.0 }, timestampMs)
    }

    @Synchronized fun getTripRecord(nowMs: Long = System.currentTimeMillis()): TripRecord {
        val end = maxOf(startTimeMs, lastTime ?: startTimeMs, nowMs)
        val durationMs = end - startTimeMs
        fun evidence(metric: TripMetric, value: Double?, origin: MetricProvenance, source: String, data: Samples, method: String? = null): TripMetricEvidence {
            if (value == null) return TripMetricEvidence.unavailable(metric)
            return TripMetricEvidence(metric, value, origin,
                if (data.covered < durationMs || data.count < 2) MetricQuality.PARTIAL else MetricQuality.AVAILABLE,
                (if (simulator) "SIMULATOR:" else "") + source, data.first, data.last, data.count, data.covered, method)
        }
        val distance = speed.integral.takeIf { speed.covered > 0 }
        val fuelUsed = fuel.integral.takeIf { fuel.covered > 0 }
        // Ratio requires the same interval coverage. Different input gaps cannot be silently mixed.
        val economy = if (matchedFuelSpeed && distance != null && distance > 0 && fuelUsed != null && fuel.covered == speed.covered && fuel.first == speed.first && fuel.last == speed.last)
            fuelUsed / distance * 100 else null
        val ev = listOf(
            TripMetricEvidence(TripMetric.DURATION, durationMs / 1000.0, MetricProvenance.DERIVED, MetricQuality.AVAILABLE,
                "HOST_CLOCK", startTimeMs, end, samples, durationMs, "elapsed session wall time; not engine-running duration"),
            evidence(TripMetric.DISTANCE, distance, MetricProvenance.DERIVED, "010D", speed, "trapezoid over adjacent valid speed samples; gaps >10s excluded"),
            evidence(TripMetric.MAX_SPEED, speed.max, MetricProvenance.DERIVED, "010D", speed, "maximum observed sample"),
            evidence(TripMetric.AVERAGE_SPEED, distance?.let { it * 3_600_000 / speed.covered }, MetricProvenance.DERIVED, "010D", speed, "distance / covered time"),
            evidence(TripMetric.MAX_RPM, rpm.max, MetricProvenance.DERIVED, "010C", rpm, "maximum observed sample"),
            evidence(TripMetric.FUEL_USED, fuelUsed, MetricProvenance.DERIVED, "0110", fuel, "MAF-derived estimate; gasoline AFR14.7 and density745g/L assumed; not ECU fuel-flow measurement"),
            evidence(TripMetric.FUEL_ECONOMY, economy, MetricProvenance.DERIVED, "0110+010D", fuel, "MAF-derived liters / covered distance *100; equal input coverage required"),
            evidence(TripMetric.MAX_COOLANT, coolant.max, MetricProvenance.DERIVED, "0105", coolant, "maximum observed sample"),
            evidence(TripMetric.AVERAGE_LOAD, load.sum.takeIf { load.count > 0 }?.div(load.count), MetricProvenance.DERIVED, "0104", load, "sample arithmetic mean; not time-weighted whole-drive mean")
        )
        return TripRecord(tripId, vehicleId, startTimeMs, end, durationMs / 1000, distance,
            speed.max?.toInt(), rpm.max?.toInt(), ev.first { it.metric == TripMetric.AVERAGE_SPEED }.value, fuelUsed,
            // Legacy score fields are compatibility data, never evidence or a health score.
            drivingStyle = null, sampleCount = samples,
            evidence = ev, legacyUnverified = false)
    }
}
