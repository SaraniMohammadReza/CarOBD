package com.sarani.carobd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OwnedVehiclePersistenceInstrumentedTest {
    @Test fun nullableOwnerVehicleAndSelectionSurviveRecreationWithoutTouchingLegacyGarage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "owned-vehicle-${java.util.UUID.randomUUID()}.db"
        val vehicle = OwnedVehicle(
            id = "owner-1",
            displayName = "Family car",
            manufacturer = "IKCO",
            model = null,
            year = null,
            currentOdometerKm = null,
            tankCapacityLiters = null,
            createdAtMs = 10,
            updatedAtMs = 10,
        )
        try {
            CarObdDatabaseHelper(context, name).use { db ->
                db.saveOwnedVehicle(vehicle)
                assertTrue(db.selectOwnedVehicle(vehicle.id))
                assertFalse(db.selectOwnedVehicle("missing"))
                assertEquals(vehicle, db.ownedVehicles().single())
                assertEquals(vehicle.id, db.activeOwnedVehicleId())
                assertEquals(0L, db.readableDatabase.compileStatement("SELECT COUNT(*) FROM garage_vehicles").simpleQueryForLong())
                db.saveOwnedVehicle(vehicle.copy(displayName = "Edited", notes = "owner note", updatedAtMs = 20))
                db.saveOwnedVehicle(vehicle.copy(id = "owner-2", displayName = "Second", createdAtMs = 30, updatedAtMs = 30))
                assertEquals(listOf("owner-2", "owner-1"), db.ownedVehicles().map { it.id })
                assertEquals("Edited", db.ownedVehicles().first { it.id == vehicle.id }.displayName)
                assertEquals(vehicle.id, db.activeOwnedVehicleId())
            }
            CarObdDatabaseHelper(context, name).use { db ->
                assertEquals(2, db.ownedVehicles().size)
                assertEquals("Edited", db.ownedVehicles().first { it.id == vehicle.id }.displayName)
                assertEquals(vehicle.id, db.activeOwnedVehicleId())
                assertTrue(db.deleteOwnedVehicle(vehicle.id))
                assertEquals(listOf("owner-2"), db.ownedVehicles().map { it.id })
                assertNull(db.activeOwnedVehicleId())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
