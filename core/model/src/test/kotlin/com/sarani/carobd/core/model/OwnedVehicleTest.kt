package com.sarani.carobd.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnedVehicleTest {
    @Test fun optionalFactsRemainUnknownAndWhitespaceNormalizesToNull() {
        val vehicle = OwnedVehicle("v1", "  My car  ", manufacturer = "  ", vin = " abc123 ", createdAtMs = 1, updatedAtMs = 1).normalized()
        assertEquals("My car", vehicle.displayName)
        assertNull(vehicle.manufacturer)
        assertEquals("ABC123", vehicle.vin)
        assertNull(vehicle.year)
        assertNull(vehicle.currentOdometerKm)
        assertTrue(vehicle.validationErrors().isEmpty())
    }

    @Test fun invalidOwnerFactsAreRejectedWithoutGuessing() {
        val vehicle = OwnedVehicle("v1", "Car", year = 1700, currentOdometerKm = -1, tankCapacityLiters = Double.NaN, createdAtMs = 2, updatedAtMs = 1)
        assertEquals(4, vehicle.validationErrors().size)
    }
}
