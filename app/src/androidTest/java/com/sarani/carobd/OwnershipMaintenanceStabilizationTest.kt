package com.sarani.carobd

import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.*
import com.sarani.carobd.data.db.*
import com.sarani.carobd.data.repository.*
import com.sarani.carobd.data.reminder.WorkManagerReminderScheduler
import com.sarani.carobd.domain.reminder.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class OwnershipMaintenanceStabilizationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun owner(id: String) = OwnedVehicle(id, "QA owner", currentOdometerKm=80_000L, createdAtMs=1L, updatedAtMs=1L)
    private fun schedule(id: String, vehicle: String) = MaintenanceSchedule(id,vehicle,"engine_oil","Engine Oil","روغن موتور", intervalKm=10_000, intervalMonths=6, lastServiceKm=70_000L, lastServiceDateMs=System.currentTimeMillis(), provenance=MaintenanceIntervalProvenance.USER_CUSTOM)
    private fun isolated(block: suspend (CarObdDatabaseHelper) -> Unit) = runBlocking {
        val name = "stabilization-${UUID.randomUUID()}.db"
        try { CarObdDatabaseHelper(context,name).use { block(it) } } finally { context.deleteDatabase(name) }
    }

    @Test fun completionRollsBackHistoryBaselineAndMileageOnInjectedUpdateFailure() = isolated { db ->
        db.saveOwnedVehicle(owner("v")); db.saveMaintenanceSchedule(schedule("s","v"))
        val repo = SQLiteMaintenanceRepository(db)
        db.writableDatabase.execSQL("CREATE TRIGGER reject_schedule BEFORE UPDATE ON vehicle_maintenance_schedules BEGIN SELECT RAISE(ABORT,'injected'); END")
        assertTrue(repo.recordCompletedService("s",90_000L,System.currentTimeMillis()).isFailure)
        assertTrue(repo.getRecords("v").isEmpty())
        assertEquals(70_000L,repo.getSchedules("v").single().lastServiceKm)
        assertEquals(80_000L,db.ownedVehicles().single().currentOdometerKm)
        db.writableDatabase.execSQL("DROP TRIGGER reject_schedule")
        val record = repo.recordCompletedService("s",90_000L,System.currentTimeMillis()).getOrThrow()
        assertEquals("s",record.scheduleId)
        assertEquals(90_000L,repo.getSchedules("v").single().lastServiceKm)
        assertEquals(90_000L,db.ownedVehicles().single().currentOdometerKm)
    }

    @Test fun linkedHistoryEditsAndDeletesReconcileWithoutGuessingLegacyLinks() = isolated { db ->
        db.saveOwnedVehicle(owner("v"));db.saveMaintenanceSchedule(schedule("s","v"))
        val repo=SQLiteMaintenanceRepository(db);val now=System.currentTimeMillis()
        val first=repo.recordCompletedService("s",81_000L,now-2000).getOrThrow()
        val second=repo.recordCompletedService("s",85_000L,now-1000).getOrThrow()
        repo.saveRecord(second.copy(performedKm=84_000L)).getOrThrow()
        assertEquals(84_000L,repo.getSchedules("v").single().lastServiceKm)
        repo.saveRecord(first.copy(performedDateMs=now)).getOrThrow()
        assertEquals(81_000L,repo.getSchedules("v").single().lastServiceKm)
        assertTrue(repo.deleteRecord(first.id));assertEquals(84_000L,repo.getSchedules("v").single().lastServiceKm)
        assertTrue(repo.deleteRecord(second.id));assertEquals(70_000L,repo.getSchedules("v").single().lastServiceKm)
        val unlinked=second.copy(id="legacy",scheduleId=null,performedKm=999_000L)
        repo.saveRecord(unlinked).getOrThrow()
        assertEquals(70_000L,repo.getSchedules("v").single().lastServiceKm)
        assertTrue(repo.saveRecord(unlinked.copy(scheduleId="s")).isFailure)
    }

    @Test fun snoozeSurvivesDatabaseReopenAndNormalReloadButNotServiceCompletion() = runBlocking {
        val name="snooze-${UUID.randomUUID()}.db";val now=System.currentTimeMillis();val until=now+3*86_400_000L
        try {
            CarObdDatabaseHelper(context,name).use { db ->
                db.saveOwnedVehicle(owner("v"));db.saveMaintenanceSchedule(schedule("s","v"))
                val s=db.getMaintenanceSchedulesForVehicle("v",80_000L).single()
                db.replaceReminderSnooze("v","s",s.updatedAtMs,until)
            }
            CarObdDatabaseHelper(context,name).use { db ->
                val s=db.getMaintenanceSchedulesForVehicle("v",80_000L).single()
                val due=MaintenanceDueEngine.calculateDueState(80_000L,now,s.lastServiceKm,s.lastServiceDateMs,s.intervalKm,s.intervalMonths)
                assertEquals(until,ReminderPlan.nextRunAt(now,due,db.reminderSnoozeUntil("v","s",s.updatedAtMs)))
                SQLiteMaintenanceRepository(db).recordCompletedService("s",80_000L,now).getOrThrow()
                val next=db.getMaintenanceSchedulesForVehicle("v",80_000L).single()
                assertNull(db.reminderSnoozeUntil("v","s",next.updatedAtMs))
                val nextDue=MaintenanceDueEngine.calculateDueState(80_000L,now,next.lastServiceKm,next.lastServiceDateMs,next.intervalKm,next.intervalMonths)
                assertTrue(ReminderPlan.nextRunAt(now,nextDue,null)!! > now)
            }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun archiveStopsRealWorkRetainsHistoryAndCannotResurrectFromStaleEdit() = isolated { db ->
        val id=UUID.randomUUID().toString();val sid=UUID.randomUUID().toString();val v=owner(id)
        db.saveOwnedVehicle(v);db.selectOwnedVehicle(id);db.saveMaintenanceSchedule(schedule(sid,id))
        val repo=SQLiteMaintenanceRepository(db);repo.recordCompletedService(sid,80_000L,System.currentTimeMillis()).getOrThrow()
        val scheduler=WorkManagerReminderScheduler(context,db)
        scheduler.reevaluateAllRemindersForVehicle(v)
        val work=androidx.work.WorkManager.getInstance(context)
        assertTrue(work.getWorkInfosForUniqueWork(ReminderIdentity.workName(id,sid)).get().any { !it.state.isFinished })
        val owners=SQLiteOwnedVehicleRepository(db,reminders=scheduler)
        assertTrue(owners.delete(id))
        assertNull(db.activeOwnedVehicleId());assertTrue(db.ownedVehicles().isEmpty())
        assertFalse(db.selectOwnedVehicle(id));assertTrue(owners.save(v).isFailure)
        assertEquals(1,repo.getRecords(id).size)
        assertTrue(work.getWorkInfosForUniqueWork(ReminderIdentity.workName(id,sid)).get().all { it.state.isFinished })
    }

    @Test fun publishedV7UpgradeRetainsUnknownsHistoryAndManualBaseline() {
        val name="upgrade-v7-${UUID.randomUUID()}.db";val path=context.getDatabasePath(name);path.parentFile!!.mkdirs()
        try {
            android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path,null).use { db ->
                (OwnedVehicleMigration.version5+OwnedVehicleDetailsMigration.version6+MaintenanceFoundationMigration.version7).forEach(db::execSQL)
                db.execSQL("INSERT INTO owned_vehicles(id,display_name,created_at,updated_at) VALUES('v','Legacy owner',1,1)")
                db.execSQL("INSERT INTO vehicle_maintenance_schedules(id,vehicle_id,item_key,title_en,title_fa,action_type,last_service_km,provenance,created_at,updated_at) VALUES('s','v','oil','Oil','روغن','REPLACE',70000,'USER_CUSTOM',1,1)")
                db.execSQL("INSERT INTO vehicle_maintenance_records(id,vehicle_id,item_key,performed_km,performed_date,provenance,created_at) VALUES('old','v','oil',80000,2,'USER_CUSTOM',2)")
                db.version=7
            }
            CarObdDatabaseHelper(context,name).use { db ->
                assertEquals(8,db.readableDatabase.version)
                assertNull(db.ownedVehicles().single().currentOdometerKm)
                assertNull(db.getVehicleMaintenanceRecords("v").single().scheduleId)
                assertEquals(70_000L,db.getMaintenanceSchedulesForVehicle("v").single().lastServiceKm)
                assertNull(db.activeOwnedVehicleId())
            }
        } finally { context.deleteDatabase(name) }
    }
}
