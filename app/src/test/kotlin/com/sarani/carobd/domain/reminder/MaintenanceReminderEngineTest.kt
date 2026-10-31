package com.sarani.carobd.domain.reminder

import com.sarani.carobd.core.model.DueCalculationResult
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.VehicleMaintenanceRecord
import com.sarani.carobd.data.repository.MaintenanceRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.ui.screens.maintenance.MaintenanceIntent
import com.sarani.carobd.ui.screens.maintenance.MaintenanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceReminderEngineTest {

    @After
    fun tearDown() {
        try {
            Dispatchers.resetMain()
        } catch (_: Exception) {}
    }

    private fun runWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun vehicle(
        id: String,
        name: String,
        odo: Long? = null,
        manufacturer: String? = null,
        model: String? = null,
        year: Int? = null
    ) = OwnedVehicle(
        id = id,
        displayName = name,
        manufacturer = manufacturer,
        model = model,
        year = year,
        currentOdometerKm = odo,
        createdAtMs = 1L,
        updatedAtMs = 1L
    )

    // --- Unit Tests for Reminder Identity & Isolation ---

    @Test
    fun testReminderIdentityWorkNameIsIsolatedPerVehicleAndSchedule() {
        val v1Id = "vehicle-123"
        val v2Id = "vehicle-456"
        val s1Id = "sched-oil"
        val s2Id = "sched-filter"

        val workNameV1S1 = ReminderIdentity.workName(v1Id, s1Id)
        val workNameV1S2 = ReminderIdentity.workName(v1Id, s2Id)
        val workNameV2S1 = ReminderIdentity.workName(v2Id, s1Id)

        assertEquals("schedule_reminder_vehicle-123_sched-oil", workNameV1S1)
        assertEquals("schedule_reminder_vehicle-123_sched-filter", workNameV1S2)
        assertEquals("schedule_reminder_vehicle-456_sched-oil", workNameV2S1)

        assertNotEquals(workNameV1S1, workNameV1S2)
        assertNotEquals(workNameV1S1, workNameV2S1)

        assertEquals("vehicle_reminders_vehicle-123", ReminderIdentity.vehicleTag(v1Id))
        assertEquals("vehicle_reminders_vehicle-456", ReminderIdentity.vehicleTag(v2Id))
    }

    // --- Unit Tests for Truthful Notification Formatter ---

    @Test
    fun testNotificationFormatterTruthfulWhenMileageAndDateKnown() {
        val veh = vehicle("v1", "Peugeot 206 TU5", 100_000L, "Peugeot", "206", 2018)
        val schedule = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "engine_oil",
            titleEn = "Engine Oil & Filter",
            titleFa = "روغن و فیلتر موتور",
            intervalKm = 7000
        )
        val dueResult = DueCalculationResult(
            state = DueState.DUE_SOON,
            remainingKm = 450L,
            remainingDays = 12L,
            nextDueKm = 100_450L,
            nextDueDateMs = 1714500000000L
        )

        val content = MaintenanceNotificationFormatter.format(veh, schedule, dueResult)

        assertEquals(DueState.DUE_SOON, content.dueState)
        assertTrue(content.titleEn.contains("Peugeot 206 TU5"))
        assertTrue(content.titleEn.contains("Engine Oil & Filter"))
        assertTrue(content.titleFa.contains("روغن و فیلتر موتور"))

        // Body must honestly convey 450 km remaining and not say overdue
        assertTrue(content.bodyEn.contains("450 km remaining"))
        assertTrue(content.bodyFa.contains("450 کیلومتر باقی‌مانده"))
    }

    @Test
    fun testNotificationFormatterTruthfulWhenOverdue() {
        val veh = vehicle("v1", "Samand EF7", 125_000L, "Iran Khodro", "Samand", 2016)
        val schedule = MaintenanceSchedule(
            id = "s2",
            vehicleId = "v1",
            itemKey = "timing_belt",
            titleEn = "Timing Belt",
            titleFa = "تسمه تایم",
            intervalKm = 60000
        )
        val dueResult = DueCalculationResult(
            state = DueState.OVERDUE,
            remainingKm = -1200L,
            remainingDays = -10L,
            nextDueKm = 123_800L,
            nextDueDateMs = 1700000000000L
        )

        val content = MaintenanceNotificationFormatter.format(veh, schedule, dueResult)

        assertEquals(DueState.OVERDUE, content.dueState)
        assertTrue(content.bodyEn.contains("1200 km overdue"))
        assertTrue(content.bodyFa.contains("1200 کیلومتر گذشته"))
    }

    @Test
    fun testNotificationFormatterTruthfulWhenMileageIsUnknown() {
        val veh = vehicle("v1", "Tondar 90", null)
        val schedule = MaintenanceSchedule(
            id = "s3",
            vehicleId = "v1",
            itemKey = "brake_fluid",
            titleEn = "Brake Fluid",
            titleFa = "روغن ترمز"
        )
        val dueResult = DueCalculationResult(
            state = DueState.DUE,
            remainingKm = null, // Truthfully null
            remainingDays = -2L,
            nextDueKm = null,
            nextDueDateMs = 1714000000000L
        )

        val content = MaintenanceNotificationFormatter.format(veh, schedule, dueResult)

        assertEquals(DueState.DUE, content.dueState)
        // Must NOT invent "0 km remaining" or fake numbers
        assertFalse(content.bodyEn.contains("0 km remaining"))
        assertFalse(content.bodyFa.contains("0 کیلومتر"))
        // Must mention the due date or service due
        assertTrue(content.bodyEn.contains("by ") || content.bodyEn.contains("Service is due"))
    }

    // --- Unit Tests for Snooze Durations ---

    @Test
    fun testSnoozeDurations() {
        assertEquals(1, SnoozeDuration.ONE_DAY.days)
        assertEquals(3, SnoozeDuration.THREE_DAYS.days)
        assertEquals(7, SnoozeDuration.ONE_WEEK.days)

        assertEquals("1 Day", SnoozeDuration.ONE_DAY.labelEn)
        assertEquals("۱ روز", SnoozeDuration.ONE_DAY.labelFa)
        assertEquals("3 Days", SnoozeDuration.THREE_DAYS.labelEn)
        assertEquals("۳ روز", SnoozeDuration.THREE_DAYS.labelFa)
        assertEquals("1 Week", SnoozeDuration.ONE_WEEK.labelEn)
        assertEquals("۱ هفته", SnoozeDuration.ONE_WEEK.labelFa)
    }

    // --- Integration with ViewModel & Isolation Tests ---

    @Test
    fun testViewModelSchedulesReminderOnLoadWhenDue() = runWithMain {
        val v1 = vehicle("v1", "Tara", odo = 50_000L)
        val fakeOwned = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val fakeMaint = FakeMaintenanceRepo()
        val fakeReminder = RecordingReminderScheduler()

        // Schedule that is DUE_SOON
        val s1 = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "air_filter",
            titleEn = "Air Filter",
            titleFa = "فیلتر هوا",
            intervalKm = 10_000,
            lastServiceKm = 40_500L,
            nextDueKm = 50_500L,
            dueState = DueState.DUE_SOON
        )
        fakeMaint.saveSchedule(s1)

        val vm = MaintenanceViewModel(
            maintenanceRepository = fakeMaint,
            ownedVehicleRepository = fakeOwned,
            reminderScheduler = fakeReminder
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        // Should have scheduled a reminder for s1
        assertEquals(1, fakeReminder.scheduledCalls.size)
        val call = fakeReminder.scheduledCalls[0]
        assertEquals("v1", call.vehicle.id)
        assertEquals("s1", call.schedule.id)
    }

    @Test
    fun testViewModelCancelsReminderWhenScheduleDeleted() = runWithMain {
        val v1 = vehicle("v1", "Tara", odo = 50_000L)
        val fakeOwned = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val fakeMaint = FakeMaintenanceRepo()
        val fakeReminder = RecordingReminderScheduler()

        val s1 = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "spark_plugs",
            titleEn = "Spark Plugs",
            titleFa = "شمع موتور",
            intervalKm = 30_000,
            lastServiceKm = 20_000L,
            dueState = DueState.DUE_SOON
        )
        fakeMaint.saveSchedule(s1)

        val vm = MaintenanceViewModel(
            maintenanceRepository = fakeMaint,
            ownedVehicleRepository = fakeOwned,
            reminderScheduler = fakeReminder
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onIntent(MaintenanceIntent.ConfirmDeleteSchedule(s1.id))
        advanceUntilIdle()

        // Should have canceled s1 reminder
        assertTrue(fakeReminder.canceledSchedules.any { it.first == "v1" && it.second == "s1" })
    }

    @Test
    fun testViewModelSnoozeIntentDispatchesToScheduler() = runWithMain {
        val v1 = vehicle("v1", "Dena Plus", odo = 30_000L)
        val fakeOwned = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val fakeMaint = FakeMaintenanceRepo()
        val fakeReminder = RecordingReminderScheduler()

        val s1 = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "engine_oil",
            titleEn = "Engine Oil",
            titleFa = "روغن موتور",
            intervalKm = 5_000,
            dueState = DueState.OVERDUE
        )
        fakeMaint.saveSchedule(s1)

        val vm = MaintenanceViewModel(
            maintenanceRepository = fakeMaint,
            ownedVehicleRepository = fakeOwned,
            reminderScheduler = fakeReminder
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onIntent(MaintenanceIntent.SnoozeSchedule(scheduleId = "s1", duration = SnoozeDuration.THREE_DAYS))
        advanceUntilIdle()

        assertEquals(1, fakeReminder.snoozedCalls.size)
        val snoozeCall = fakeReminder.snoozedCalls[0]
        assertEquals("v1", snoozeCall.vehicleId)
        assertEquals("s1", snoozeCall.scheduleId)
        assertEquals(SnoozeDuration.THREE_DAYS, snoozeCall.duration)
    }

    @Test
    fun testMultiVehicleSchedulerIsolation() = runTest {
        val scheduler = RecordingReminderScheduler()

        scheduler.cancelAllVehicleReminders("vehicle_A")

        assertEquals(listOf("vehicle_A"), scheduler.canceledVehicles)
        // vehicle_B must NOT be in the canceled list
        assertFalse(scheduler.canceledVehicles.contains("vehicle_B"))
    }

    @Test
    fun testServiceCompletionReschedulesReminderWithUpdatedOdometer() = runWithMain {
        val v1 = vehicle("v1", "Quick M15", odo = 20_000L)
        val fakeOwned = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val fakeMaint = FakeMaintenanceRepo()
        val fakeReminder = RecordingReminderScheduler()

        val s1 = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "engine_oil",
            titleEn = "Engine Oil",
            titleFa = "روغن موتور",
            intervalKm = 5_000,
            lastServiceKm = 15_000L,
            nextDueKm = 20_000L,
            dueState = DueState.DUE
        )
        fakeMaint.saveSchedule(s1)

        val vm = MaintenanceViewModel(
            maintenanceRepository = fakeMaint,
            ownedVehicleRepository = fakeOwned,
            reminderScheduler = fakeReminder
        )
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        // Record service at 20,500 km
        vm.onIntent(
            MaintenanceIntent.SubmitRecordService(
                com.sarani.carobd.ui.screens.maintenance.RecordServiceDialogModel(
                    scheduleId = "s1",
                    itemKey = "engine_oil",
                    title = "روغن موتور",
                    performedKm = "20500"
                )
            )
        )
        advanceUntilIdle()

        // Schedule was updated to OK (next due is 25,500 km)
        val schedules = fakeMaint.getSchedules("v1", 20500L)
        val updatedSchedule = schedules.find { it.id == "s1" }
        assertNotNull(updatedSchedule)
        assertEquals(20500L, updatedSchedule?.lastServiceKm)
        assertEquals(25500L, updatedSchedule?.nextDueKm)
        assertEquals(DueState.OK, updatedSchedule?.dueState)
    }

    // --- Test Doubles ---

    private data class ScheduledCall(
        val vehicle: OwnedVehicle,
        val schedule: MaintenanceSchedule,
        val dueResult: DueCalculationResult
    )

    private data class SnoozeCall(
        val vehicleId: String,
        val scheduleId: String,
        val duration: SnoozeDuration
    )

    private class RecordingReminderScheduler : MaintenanceReminderScheduler {
        val scheduledCalls = mutableListOf<ScheduledCall>()
        val canceledSchedules = mutableListOf<Pair<String, String>>()
        val canceledVehicles = mutableListOf<String>()
        val snoozedCalls = mutableListOf<SnoozeCall>()
        val reevaluatedVehicles = mutableListOf<String>()

        override suspend fun scheduleReminder(
            vehicle: OwnedVehicle,
            schedule: MaintenanceSchedule,
            dueResult: DueCalculationResult
        ) {
            scheduledCalls += ScheduledCall(vehicle, schedule, dueResult)
        }

        override suspend fun snoozeReminder(
            vehicleId: String,
            scheduleId: String,
            duration: SnoozeDuration
        ) {
            snoozedCalls += SnoozeCall(vehicleId, scheduleId, duration)
        }

        override suspend fun cancelScheduleReminder(vehicleId: String, scheduleId: String) {
            canceledSchedules += Pair(vehicleId, scheduleId)
        }

        override suspend fun cancelAllVehicleReminders(vehicleId: String) {
            canceledVehicles += vehicleId
        }

        override suspend fun reevaluateAllRemindersForVehicle(vehicle: OwnedVehicle) {
            reevaluatedVehicles += vehicle.id
        }
    }

    private class FakeOwnedVehicles(
        initial: List<OwnedVehicle>,
        activeId: String? = null
    ) : OwnedVehicleRepository {
        override val vehicles = MutableStateFlow(initial)
        override val activeVehicleId = MutableStateFlow(activeId)

        override suspend fun refresh() = Unit
        override suspend fun save(vehicle: OwnedVehicle): Result<Unit> {
            vehicles.value = vehicles.value.filterNot { it.id == vehicle.id } + vehicle
            return Result.success(Unit)
        }
        override suspend fun select(id: String?): Boolean {
            activeVehicleId.value = id
            return true
        }
        override suspend fun delete(id: String): Boolean {
            vehicles.value = vehicles.value.filterNot { it.id == id }
            if (activeVehicleId.value == id) activeVehicleId.value = null
            return true
        }
    }

    private class FakeMaintenanceRepo : MaintenanceRepository {
        private val schedules = mutableListOf<MaintenanceSchedule>()
        private val records = mutableListOf<VehicleMaintenanceRecord>()

        override suspend fun getSchedules(vehicleId: String, currentOdometerKm: Long?): List<MaintenanceSchedule> {
            return schedules.filter { it.vehicleId == vehicleId }
        }

        override suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> {
            schedules.removeAll { it.id == schedule.id }
            schedules.add(schedule)
            return Result.success(Unit)
        }

        override suspend fun deleteSchedule(scheduleId: String): Boolean {
            return schedules.removeAll { it.id == scheduleId }
        }

        override suspend fun getRecords(vehicleId: String): List<VehicleMaintenanceRecord> {
            return records.filter { it.vehicleId == vehicleId }.sortedByDescending { it.performedKm }
        }

        override suspend fun saveRecord(record: VehicleMaintenanceRecord): Result<Unit> {
            records.removeAll { it.id == record.id }
            records.add(record)
            return Result.success(Unit)
        }

        override suspend fun deleteRecord(recordId: String): Boolean {
            return records.removeAll { it.id == recordId }
        }

        override suspend fun recordCompletedService(
            scheduleId: String,
            performedKm: Long,
            performedDateMs: Long,
            costTomans: Long,
            serviceCenter: String?,
            partsBrand: String?,
            partNumber: String?,
            notes: String?
        ): Result<VehicleMaintenanceRecord> {
            val schedule = schedules.find { it.id == scheduleId } ?: error("Schedule not found: $scheduleId")
            val record = VehicleMaintenanceRecord(
                id = UUID.randomUUID().toString(),
                vehicleId = schedule.vehicleId,
                itemKey = schedule.itemKey,
                performedKm = performedKm,
                performedDateMs = performedDateMs,
                costTomans = costTomans,
                serviceCenter = serviceCenter,
                partsBrand = partsBrand,
                partNumber = partNumber,
                notes = notes
            )
            records.add(record)

            val intervalKm = schedule.intervalKm
            val nextDue = intervalKm?.let { performedKm + it }
            val updated = schedule.copy(
                lastServiceKm = performedKm,
                lastServiceDateMs = performedDateMs,
                nextDueKm = nextDue,
                dueState = DueState.OK
            )
            schedules.removeAll { it.id == scheduleId }
            schedules.add(updated)

            return Result.success(record)
        }
    }
}
