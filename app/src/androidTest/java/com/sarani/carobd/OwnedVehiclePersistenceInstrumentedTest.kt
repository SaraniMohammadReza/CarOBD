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
            }
            CarObdDatabaseHelper(context, name).use { db ->
                assertEquals(vehicle, db.ownedVehicles().single())
                assertEquals(vehicle.id, db.activeOwnedVehicleId())
                assertTrue(db.deleteOwnedVehicle(vehicle.id))
                assertTrue(db.ownedVehicles().isEmpty())
                assertNull(db.activeOwnedVehicleId())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
