package com.sarani.carobd

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceActionType
import com.sarani.carobd.core.model.MaintenanceIntervalProvenance
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.data.repository.SQLiteMaintenanceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MaintenancePersistenceInstrumentedTest {

    @Test
    fun testMaintenanceFoundationSchedulesAndHistoryLifecycle() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dbName = "maint-test-${UUID.randomUUID()}.db"
        try {
            val helper = CarObdDatabaseHelper(context, dbName)
            val repo = SQLiteMaintenanceRepository(helper)
            for (id in listOf("veh-1", "veh-2")) helper.saveOwnedVehicle(com.sarani.carobd.core.model.OwnedVehicle(id,id,createdAtMs=1L,updatedAtMs=1L))
            val now = System.currentTimeMillis()

            val sched1 = MaintenanceSchedule(
                id = "sched-oil-1",
                vehicleId = "veh-1",
                itemKey = "engine_oil",
                titleEn = "Engine Oil",
                titleFa = "روغن موتور",
                actionType = MaintenanceActionType.REPLACE,
                intervalKm = 10000,
                intervalMonths = 6,
                lastServiceKm = 100000L,
                lastServiceDateMs = now,
                nextDueKm = 110000L,
                nextDueDateMs = 1715000000000L,
                provenance = MaintenanceIntervalProvenance.OEM_RECOMMENDED,
                dueState = DueState.OK,
                isCustom = false
            )

            val sched2 = MaintenanceSchedule(
                id = "sched-plugs-2",
                vehicleId = "veh-2",
                itemKey = "spark_plugs",
                titleEn = "Spark Plugs",
                titleFa = "شمع‌های موتور",
                actionType = MaintenanceActionType.REPLACE,
                intervalKm = 30000,
                intervalMonths = 24,
                lastServiceKm = 40000L,
                lastServiceDateMs = now,
                nextDueKm = 70000L,
                nextDueDateMs = 1740000000000L,
                provenance = MaintenanceIntervalProvenance.OEM_RECOMMENDED,
                dueState = DueState.OK,
                isCustom = false
            )

            repo.saveSchedule(sched1)
            repo.saveSchedule(sched2)

            // Vehicle isolation check
            val veh1Schedules = repo.getSchedules("veh-1", currentOdometerKm = 105000L)
            assertEquals(1, veh1Schedules.size)
            assertEquals("sched-oil-1", veh1Schedules.first().id)
            assertEquals(DueState.OK, veh1Schedules.first().dueState)

            val veh2Schedules = repo.getSchedules("veh-2", currentOdometerKm = 45000L)
            assertEquals(1, veh2Schedules.size)
            assertEquals("sched-plugs-2", veh2Schedules.first().id)

            // Test due calculation dynamic update: at 109,500 km, oil schedule becomes DUE_SOON
            val veh1DueSoon = repo.getSchedules("veh-1", currentOdometerKm = 109500L)
            assertEquals(DueState.DUE_SOON, veh1DueSoon.first().dueState)

            // Record completed service
            val record = repo.recordCompletedService(
                scheduleId = "sched-oil-1",
                performedKm = 110000L,
                performedDateMs = now,
                costTomans = 850000L,
                serviceCenter = "IKCO Central",
                partsBrand = "Total 7000",
                partNumber = "10W40-SL",
                notes = "Oil + filter replaced"
            ).getOrThrow()

            assertNotNull(record.id)
            assertEquals("veh-1", record.vehicleId)
            assertEquals("engine_oil", record.itemKey)
            assertEquals(110000L, record.performedKm)
            assertEquals(850000L, record.costTomans)

            // Verify history log
            val history = repo.getRecords("veh-1")
            assertEquals(1, history.size)
            assertEquals("Total 7000", history.first().partsBrand)

            // Verify schedule was updated to new interval (110,000 + 10,000 = 120,000)
            val updatedSchedules = repo.getSchedules("veh-1", currentOdometerKm = 110000L)
            val updatedOil = updatedSchedules.first()
            assertEquals(110000L, updatedOil.lastServiceKm)
            assertEquals(120000L, updatedOil.nextDueKm)
            assertEquals(DueState.OK, updatedOil.dueState)

            // Legacy table untouched
            val legacyCount = helper.readableDatabase.compileStatement("SELECT COUNT(*) FROM maintenance_records").simpleQueryForLong()
            assertEquals(0L, legacyCount)

            helper.close()
        } finally {
            context.deleteDatabase(dbName)
        }
    }
}
