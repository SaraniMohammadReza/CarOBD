package com.sarani.carobd.core.telemetry

import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.telemetry.trip.TripComputer
import org.junit.Assert.*
import org.junit.Test

class TripEvidenceTest {
    private fun TripRecord.metric(m: TripMetric) = evidence.single { it.metric == m }
    @Test fun unavailableFuel_notZeroAndMissingSpeedRpmAreIndependent() {
        val t = TripComputer("id", "v", 0)
        t.processSample(0, null, null, 0)
        t.processSample(0, null, null, 1000)
        val r = t.getTripRecord(1000)
        assertEquals(0.0, r.distanceKm!!, 0.0)
        assertNull(r.estimatedFuelUsedLiters); assertNull(r.maxRpm)
        assertEquals(MetricProvenance.UNAVAILABLE, r.metric(TripMetric.FUEL_USED).provenance)
        assertEquals(2, r.metric(TripMetric.MAX_SPEED).sampleCount)
        assertEquals("010D", r.metric(TripMetric.DISTANCE).source)
    }
    @Test fun derivedFuel_markedDerivedWithAssumptionsAndCoverage() {
        val t = TripComputer("id", "v", 0, simulator = true)
        t.processSample(36, 1000, 3.6, 0)
        t.processSample(36, 2000, 3.6, 1000)
        val r = t.getTripRecord(2000)
        assertEquals(0.001, r.estimatedFuelUsedLiters!!, 1e-9)
        val e = r.metric(TripMetric.FUEL_USED)
        assertEquals(MetricProvenance.DERIVED, e.provenance)
        assertEquals(MetricQuality.PARTIAL, e.quality)
        assertEquals(1000L, e.coveredMs)
        assertEquals("SIMULATOR:0110", e.source)
        assertTrue(e.method!!.contains("AFR14.7"))
        assertFalse(r.legacyUnverified)
    }
    @Test fun gapsAndRollbackDoNotBridgeOrInventCoverage() {
        val t = TripComputer("id", "v", 0)
        t.processSample(36, 1000, null, 0)
        t.processSample(36, 1000, null, 1000)
        t.processSample(null, 1000, null, 2000)
        t.processSample(36, 1000, null, 3000)
        t.processSample(36, 1000, null, 30_000)
        t.processSample(36, 1000, null, 5000) // rollback invalidates adjacency
        t.processSample(36, 1000, null, 31_000)
        val r = t.getTripRecord(31_000)
        assertEquals(0.01, r.distanceKm!!, 1e-9)
        assertEquals(1000L, r.metric(TripMetric.DISTANCE).coveredMs)
        assertEquals(MetricQuality.PARTIAL, r.metric(TripMetric.DISTANCE).quality)
        assertEquals(31_000L, r.metric(TripMetric.MAX_RPM).lastSampleMs)
    }
    @Test fun oneSampleHasMaxButNoIntegratedDistanceOrFuel() {
        val t = TripComputer("id", "v", 0)
        t.processSample(50, 1500, 5.0, 100)
        val r = t.getTripRecord(200)
        assertNull(r.distanceKm); assertNull(r.estimatedFuelUsedLiters)
        assertEquals(50, r.maxSpeedKmh)
        assertEquals(0L, r.metric(TripMetric.MAX_SPEED).coveredMs)
    }
    @Test fun mismatchedCoverageCannotBecomeWholeTripEconomy() {
        val t = TripComputer("id", "v", 0)
        t.processSample(36, 1000, null, 0)
        t.processSample(36, 1000, 3.6, 1000)
        t.processSample(36, 1000, 3.6, 2000)
        assertNull(t.getTripRecord(2000).metric(TripMetric.FUEL_ECONOMY).value)
    }
    @Test fun legacyTrip_remainsUnverified() {
        val legacy = TripRecord("old", "v", 0, 1, 1, 1.0, 1, 1, 1.0, 0.0)
        assertTrue(legacy.legacyUnverified)
        val e = TripMetricEvidence.legacy(TripMetric.FUEL_USED, 0.0)
        assertEquals(MetricProvenance.LEGACY_UNVERIFIED, e.provenance)
        assertNull(e.source); assertEquals(0L, e.coveredMs)
    }
}
