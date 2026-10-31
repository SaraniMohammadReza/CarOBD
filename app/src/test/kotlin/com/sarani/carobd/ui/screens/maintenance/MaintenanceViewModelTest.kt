package com.sarani.carobd.ui.screens.maintenance

import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.MaintenanceActionType
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.VehicleMaintenanceRecord
import com.sarani.carobd.data.repository.MaintenanceRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceViewModelTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun runWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    @Test
    fun initialStateLoadsVehiclesAndSelectedVehicleSchedules() = runWithMain {
        val v1 = vehicle("v1", "Peugeot 206", odo = 100_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        val schedule1 = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "engine_oil",
            titleEn = "Engine Oil",
            titleFa = "روغن موتور",
            intervalKm = 6000,
            lastServiceKm = 95_000L,
            nextDueKm = 101_000L,
            dueState = DueState.DUE_SOON
        )
        val schedule2 = MaintenanceSchedule(
            id = "s2",
            vehicleId = "v1",
            itemKey = "timing_belt",
            titleEn = "Timing Belt",
            titleFa = "تسمه تایم",
            intervalKm = 60000,
            lastServiceKm = 30_000L,
            nextDueKm = 90_000L,
            dueState = DueState.OVERDUE
        )
        maintRepo.saveSchedule(schedule1)
        maintRepo.saveSchedule(schedule2)

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("v1", state.selectedVehicle?.id)
        assertEquals(2, state.schedules.size)
        assertEquals(1, state.overdueCount)
        assertEquals(1, state.dueSoonCount)
        assertEquals(0, state.okCount)
        assertEquals(0, state.unknownCount)
    }

    @Test
    fun emptyGarageShowsNullSelectedVehicleAndZeroCounts() = runWithMain {
        val ownedRepo = FakeOwnedVehicles(emptyList(), activeId = null)
        val maintRepo = FakeMaintenanceRepository()

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.selectedVehicle)
        assertTrue(state.vehicles.isEmpty())
        assertTrue(state.schedules.isEmpty())
        assertTrue(state.records.isEmpty())
        assertEquals(0, state.overdueCount)
        assertEquals(0, state.dueSoonCount)
    }

    @Test
    fun initializeDefaultSchedulesCreatesRecommendedItemsForVehicle() = runWithMain {
        val v1 = vehicle("v1", "Dena Plus", odo = 25_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertTrue(vm.uiState.value.schedules.isEmpty())

        vm.onIntent(MaintenanceIntent.InitializeDefaultSchedules)
        advanceUntilIdle()

        val schedules = vm.uiState.value.schedules
        assertTrue("Must create at least 8 standard service items", schedules.size >= 8)
        assertTrue(schedules.any { it.itemKey == "engine_oil" })
        assertTrue(schedules.any { it.itemKey == "timing_belt" })
        assertTrue(schedules.any { it.itemKey == "brake_pads" })

        // When initialized with no previous service mileage, state defaults safely to UNKNOWN
        assertTrue(schedules.all { it.dueState == DueState.UNKNOWN })
    }

    @Test
    fun recordCompletedServiceAdvancesScheduleAndUpdatesVehicleOdometer() = runWithMain {
        val v1 = vehicle("v1", "Tara MT", odo = 19_500L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        val schedule = MaintenanceSchedule(
            id = "s_oil",
            vehicleId = "v1",
            itemKey = "engine_oil",
            titleEn = "Engine Oil",
            titleFa = "روغن موتور",
            intervalKm = 6000,
            lastServiceKm = 14_000L,
            nextDueKm = 20_000L,
            dueState = DueState.DUE_SOON
        )
        maintRepo.saveSchedule(schedule)

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        // Submit completed service at 20,000 km
        vm.onIntent(
            MaintenanceIntent.SubmitRecordService(
                RecordServiceDialogModel(
                    scheduleId = "s_oil",
                    itemKey = "engine_oil",
                    title = "روغن موتور",
                    performedKm = "20000",
                    costTomans = "850000",
                    serviceCenter = "IKCO Rep 1024",
                    partsBrand = "Total Quartz 7000",
                    notes = "Changed filter as well"
                )
            )
        )
        advanceUntilIdle()

        // Verify history record created
        val records = vm.uiState.value.records
        assertEquals(1, records.size)
        val record = records.first()
        assertEquals("engine_oil", record.itemKey)
        assertEquals(20_000L, record.performedKm)
        assertEquals(850_000L, record.costTomans)
        assertEquals("IKCO Rep 1024", record.serviceCenter)
        assertEquals("Total Quartz 7000", record.partsBrand)

        // Verify schedule updated with new last service and next due (20000 + 6000 = 26000)
        val updatedSchedule = vm.uiState.value.schedules.first { it.id == "s_oil" }
        assertEquals(20_000L, updatedSchedule.lastServiceKm)
        assertEquals(26_000L, updatedSchedule.nextDueKm)

        // Verify vehicle current mileage advanced to 20,000 km
        val updatedVehicle = ownedRepo.vehicles.value.first { it.id == "v1" }
        assertEquals(20_000L, updatedVehicle.currentOdometerKm)
    }

    @Test
    fun switchingVehiclesIsolatesMaintenanceSchedulesAndHistory() = runWithMain {
        val v1 = vehicle("v1", "Peugeot 206", odo = 80_000L)
        val v2 = vehicle("v2", "Saipa Shahin", odo = 15_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1, v2), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        maintRepo.saveSchedule(
            MaintenanceSchedule(
                id = "s_206",
                vehicleId = "v1",
                itemKey = "plugs_206",
                titleEn = "206 Plugs",
                titleFa = "شمع 206",
                intervalKm = 30000
            )
        )
        maintRepo.saveSchedule(
            MaintenanceSchedule(
                id = "s_shahin",
                vehicleId = "v2",
                itemKey = "turbo_inspection",
                titleEn = "Turbo Inspection",
                titleFa = "بازدید توربو",
                intervalKm = 10000
            )
        )

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        // Initially v1 is selected
        assertEquals("v1", vm.uiState.value.selectedVehicle?.id)
        assertEquals(listOf("s_206"), vm.uiState.value.schedules.map { it.id })

        // Switch to v2
        vm.onIntent(MaintenanceIntent.SelectVehicle("v2"))
        advanceUntilIdle()

        assertEquals("v2", vm.uiState.value.selectedVehicle?.id)
        assertEquals(listOf("s_shahin"), vm.uiState.value.schedules.map { it.id })
    }

    @Test
    fun deleteSchedulePreservesExistingServiceHistory() = runWithMain {
        val v1 = vehicle("v1", "Samand EF7", odo = 60_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        val schedule = MaintenanceSchedule(
            id = "s_custom",
            vehicleId = "v1",
            itemKey = "custom_additive",
            titleEn = "Fuel Additive",
            titleFa = "مکمل سوخت",
            intervalKm = 5000
        )
        maintRepo.saveSchedule(schedule)

        // Record a service for this schedule
        maintRepo.recordCompletedService(
            scheduleId = "s_custom",
            performedKm = 55_000L,
            performedDateMs = System.currentTimeMillis(),
            notes = "BG 44K"
        )

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.schedules.size)
        assertEquals(1, vm.uiState.value.records.size)

        // Delete schedule
        vm.onIntent(MaintenanceIntent.ConfirmDeleteSchedule("s_custom"))
        advanceUntilIdle()

        // Schedule removed, but historical record remains preserved
        assertTrue(vm.uiState.value.schedules.isEmpty())
        assertEquals(1, vm.uiState.value.records.size)
        assertEquals("custom_additive", vm.uiState.value.records.first().itemKey)
    }

    @Test
    fun editExistingRecordUpdatesFieldsAndPreservesIdentity() = runWithMain {
        val v1 = vehicle("v1", "Samand", odo = 80_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        val record = VehicleMaintenanceRecord(
            id = "rec_oil_1",
            vehicleId = "v1",
            itemKey = "engine_oil",
            performedKm = 80_000L,
            performedDateMs = 1700000000000L,
            costTomans = 500_000L,
            serviceCenter = "Workshop Alpha",
            partsBrand = "Total",
            partNumber = "7000-10W40",
            notes = "Replaced oil and filter",
            createdAtMs = 12345L
        )
        maintRepo.saveRecord(record)

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.records.size)

        // Open edit dialog
        vm.onIntent(MaintenanceIntent.OpenEditRecordDialog(record))
        val dialog = vm.uiState.value.editRecordDialog
        assertNotNull(dialog)
        assertEquals("rec_oil_1", dialog?.recordId)
        assertEquals("80000", dialog?.performedKm)
        assertEquals("500000", dialog?.costTomans)
        assertEquals("Workshop Alpha", dialog?.serviceCenter)

        // Submit updated record with new cost, mileage, and notes
        vm.onIntent(
            MaintenanceIntent.SubmitSaveRecord(
                dialog!!.copy(
                    performedKm = "81000",
                    costTomans = "550000",
                    notes = "Updated note: included washer"
                )
            )
        )
        advanceUntilIdle()

        assertNull(vm.uiState.value.editRecordDialog)
        val updatedRecords = maintRepo.getRecords("v1")
        assertEquals(1, updatedRecords.size)
        val updated = updatedRecords.first()
        assertEquals("rec_oil_1", updated.id)
        assertEquals(12345L, updated.createdAtMs)
        assertEquals(81000L, updated.performedKm)
        assertEquals(550000L, updated.costTomans)
        assertEquals("Updated note: included washer", updated.notes)
    }

    @Test
    fun exportServiceHistoryFormatsTruthfulSummary() = runWithMain {
        val v1 = vehicle("v1", "Peugeot 207i", odo = 45_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()

        val record = VehicleMaintenanceRecord(
            id = "rec_spark",
            vehicleId = "v1",
            itemKey = "spark_plugs",
            performedKm = 40_000L,
            performedDateMs = 1710000000000L,
            costTomans = 400_000L,
            serviceCenter = "IKCO Central",
            partsBrand = "Denso",
            partNumber = "K16PR-U11",
            notes = "Torqued to spec"
        )
        maintRepo.saveRecord(record)

        val vm = MaintenanceViewModel(maintRepo, ownedRepo)
        backgroundScope.launch { vm.uiState.collect {} }
        advanceUntilIdle()

        vm.onIntent(MaintenanceIntent.ExportServiceHistory)
        val export = vm.uiState.value.exportText
        assertNotNull(export)
        assertTrue(export!!.contains("Peugeot 207i"))
        assertTrue(export.contains("45000 km"))
        assertTrue(export.contains("SPARK PLUGS"))
        assertTrue(export.contains("40000 km"))
        assertTrue(export.contains("IKCO Central"))
        assertTrue(export.contains("Denso"))

        vm.onIntent(MaintenanceIntent.DismissExportDialog)
        assertNull(vm.uiState.value.exportText)
    }

    private fun vehicle(id: String, name: String, odo: Long? = null) = OwnedVehicle(
        id = id,
        displayName = name,
        currentOdometerKm = odo,
        createdAtMs = 1L,
        updatedAtMs = 1L
    )
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

private class FakeMaintenanceRepository : MaintenanceRepository {
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
