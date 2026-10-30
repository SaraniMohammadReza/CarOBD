package com.sarani.carobd.core.model

/** Provenance describes origin, not confidence or vehicle health. */
enum class MetricProvenance { OBSERVED, DERIVED, LEGACY_UNVERIFIED, UNAVAILABLE }
enum class MetricQuality { AVAILABLE, PARTIAL, LEGACY_UNVERIFIED, UNAVAILABLE }
enum class TripMetric(val unit: String) {
    DURATION("s"), DISTANCE("km"), MAX_SPEED("km/h"), AVERAGE_SPEED("km/h"), MAX_RPM("rpm"),
    FUEL_USED("L"), FUEL_ECONOMY("L/100km"), MAX_COOLANT("°C"), AVERAGE_LOAD("%")
}

data class TripMetricEvidence(
    val metric: TripMetric,
    val value: Double?,
    val provenance: MetricProvenance,
    val quality: MetricQuality,
    val source: String?,
    val firstSampleMs: Long? = null,
    val lastSampleMs: Long? = null,
    val sampleCount: Int = 0,
    /** Sum of valid adjacent observed intervals, not an assumed whole-drive percentage. */
    val coveredMs: Long = 0,
    val method: String? = null,
) {
    init {
        require(value == null || value.isFinite())
        require(sampleCount >= 0 && coveredMs >= 0)
        require(firstSampleMs == null || lastSampleMs != null && lastSampleMs >= firstSampleMs)
        require(provenance != MetricProvenance.UNAVAILABLE || value == null)
        require(quality != MetricQuality.UNAVAILABLE || value == null)
    }
    companion object {
        fun unavailable(metric: TripMetric) = TripMetricEvidence(metric, null, MetricProvenance.UNAVAILABLE, MetricQuality.UNAVAILABLE, null)
        fun legacy(metric: TripMetric, value: Double?) = TripMetricEvidence(metric, value, MetricProvenance.LEGACY_UNVERIFIED, MetricQuality.LEGACY_UNVERIFIED, null)
    }
}
