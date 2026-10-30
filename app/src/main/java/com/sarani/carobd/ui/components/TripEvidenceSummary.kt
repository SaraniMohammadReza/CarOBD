package com.sarani.carobd.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sarani.carobd.R
import com.sarani.carobd.core.model.*
import com.sarani.carobd.ui.theme.*

@Composable fun TripEvidenceSummary(evidence: List<TripMetricEvidence>) {
    evidence.forEach { e ->
        val title = stringResource(when(e.metric) {
            TripMetric.DURATION -> R.string.s5_trip_time
            TripMetric.DISTANCE -> R.string.s5_trip_distance
            TripMetric.MAX_SPEED -> R.string.s5_max_speed
            TripMetric.AVERAGE_SPEED -> R.string.s5_avg_speed
            TripMetric.MAX_RPM -> R.string.s5_max_rpm
            TripMetric.FUEL_USED -> R.string.s5_fuel_est
            TripMetric.FUEL_ECONOMY -> R.string.s5_fuel_economy
            TripMetric.MAX_COOLANT -> R.string.s5_coolant
            TripMetric.AVERAGE_LOAD -> R.string.s5_load
        })
        val converted = e.value?.let { displayMeasurement(it, e.metric.unit, LocalUnitSystem.current) }
        Text(stringResource(R.string.s5_coverage, title,
            converted?.value?.takeIf { it.isFinite() }?.let { "%.2f".format(java.util.Locale.US, it) } ?: "—", converted?.unit.orEmpty(), e.sampleCount, e.coveredMs))
        Text(stringResource(when(e.provenance) {
            MetricProvenance.OBSERVED -> R.string.s5_observed
            MetricProvenance.DERIVED -> R.string.s5_derived
            MetricProvenance.LEGACY_UNVERIFIED -> R.string.s5_legacy
            MetricProvenance.UNAVAILABLE -> R.string.s5_unavailable
        }))
        DeveloperEvidence(listOfNotNull(e.source, e.method, e.quality.name, e.firstSampleMs?.toString(), e.lastSampleMs?.toString()).joinToString(" · "))
    }
}
