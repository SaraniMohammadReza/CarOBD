package com.sarani.carobd.core.vehicle.catalog

import com.sarani.carobd.core.model.OwnedVehicle
import org.junit.Assert.*
import org.junit.Test

class OwnedVehicleResolutionTest {
    private val catalog = DefaultVehicleCatalogRepository()
    @Test fun missingEngineNeverResolves206ToFirstCandidate() {
        val result = catalog.resolveOwnedVehicle(OwnedVehicle(createdAtMs=1L, updatedAtMs=1L, id="v", displayName= "206", manufacturer="IKCO", model="206"))
        assertEquals(2, result.candidates.size)
        assertEquals(VehicleResolutionStatus.ENGINE_REQUIRED, result.status)
        assertNull(result.exactMatch)
    }
    @Test fun blankIdentityDoesNotResolve() {
        assertTrue(catalog.findMatchingVariants("", "").isEmpty())
        assertNull(catalog.resolveOwnedVehicle(OwnedVehicle(createdAtMs=1L, updatedAtMs=1L, id="v", displayName= "My car")).exactMatch)
    }
    @Test fun specifiedEngineResolvesOnlyItsVariant() {
        val match = catalog.resolveOwnedVehicle(OwnedVehicle(createdAtMs=1L, updatedAtMs=1L, id="v", displayName= "206", manufacturer="IKCO", model="206", engineCode="TU5")).exactMatch
        assertNotNull(match)
        assertEquals("var_206_type5", match!!.variant.id)
    }
}
