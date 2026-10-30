package com.sarani.carobd

import com.sarani.carobd.core.logger.TripLogger
import com.sarani.carobd.core.model.LiveTelemetry
import org.junit.Assert.*
import org.junit.Test

class TripTruthfulnessTest {
    @Test fun unknownAndStaleTelemetryCannotBecomeTripMeasurements() {
        val logger = TripLogger(); logger.startTrip(1000)
        logger.logSample(LiveTelemetry(speedKmh = 120, rpm = 5000, timestamp = 1000), 1000)
        assertEquals(0, logger.tripState.value.samplesCount)
        logger.logSample(LiveTelemetry(speedKmh = 120, measuredPids = setOf("0D"), timestamp = 1000), 20_000)
        assertEquals(0, logger.tripState.value.samplesCount)
        assertNull(logger.tripState.value.estimatedFuelUsedLiters)
    }
    @Test fun cadenceAndMissingSamplesDoNotFabricateDistance() {
        val logger = TripLogger(); logger.startTrip(1000)
        fun sample(time: Long) = LiveTelemetry(speedKmh = 36, measuredPids = setOf("0D"), timestamp = time)
        logger.logSample(sample(1000), 1000); logger.logSample(sample(3000), 3000)
        assertEquals(0.02, logger.tripState.value.distanceKm, 0.000001)
        logger.logSample(LiveTelemetry(timestamp = 4000), 4000)
        logger.logSample(sample(5000), 5000)
        assertEquals(0.02, logger.tripState.value.distanceKm, 0.000001)
        assertTrue(logger.tripState.value.coverageLimited)
        assertTrue(logger.exportToCsv().contains(",,36.0,"))
    }
}
