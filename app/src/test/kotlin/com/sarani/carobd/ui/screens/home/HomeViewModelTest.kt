package com.sarani.carobd.ui.screens.home

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DueState
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.MaintenanceIntervalProvenance
import com.sarani.carobd.core.model.MaintenanceSchedule
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.VehicleMaintenanceRecord
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.vehicle.catalog.DefaultVehicleCatalogRepository
import com.sarani.carobd.core.vehicle.parts.DefaultPartsRepository
import com.sarani.carobd.data.repository.MaintenanceRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun runWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun testVehicle(
        id: String = "v1",
        displayName: String = "My Tondar 90",
        make: String = "Renault",
        model: String = "Tondar 90",
        year: Int = 2012,
        engineCode: String = "K4M",
        odo: Long? = 100_000L
    ) = OwnedVehicle(
        id = id,
        displayName = displayName,
        manufacturer = make,
        model = model,
        generation = "Tondar 90",
        year = year,
        engine = "1.6 16V",
        engineCode = engineCode,
        transmission = "Manual",
        vin = null,
        licensePlate = "12-345-67",
        currentOdometerKm = odo,
        tankCapacityLiters = 50.0,
        fuelType = "Petrol",
        notes = null,
        createdAtMs = 1000L,
        updatedAtMs = 1000L
    )

    private class FakeOwnedVehicles(
        private val initialList: List<OwnedVehicle> = emptyList(),
        private val activeId: String? = null,
        private val hydrateOnRefresh: Boolean = false
    ) : OwnedVehicleRepository {
        private val _vehicles = MutableStateFlow(if (hydrateOnRefresh) emptyList() else initialList)
        override val vehicles: StateFlow<List<OwnedVehicle>> = _vehicles.asStateFlow()

        private val _activeVehicleId = MutableStateFlow(if (hydrateOnRefresh) null else activeId)
        override val activeVehicleId: StateFlow<String?> = _activeVehicleId.asStateFlow()

        override suspend fun refresh() { if(hydrateOnRefresh) { _vehicles.value=initialList; _activeVehicleId.value=activeId } }
        override suspend fun select(id: String?): Boolean {
            _activeVehicleId.value = id
            return true
        }
        override suspend fun save(vehicle: OwnedVehicle): Result<Unit> {
            _vehicles.value = _vehicles.value.filterNot { it.id == vehicle.id } + vehicle
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Boolean {
            val existed = _vehicles.value.any { it.id == id }
            _vehicles.value = _vehicles.value.filterNot { it.id == id }
            return existed
        }
    }

    private class FakeMaintenanceRepository(
        private val schedules: MutableList<MaintenanceSchedule> = mutableListOf()
    ) : MaintenanceRepository {
        override suspend fun getSchedules(vehicleId: String, currentOdometerKm: Long?): List<MaintenanceSchedule> =
            schedules.filter { it.vehicleId == vehicleId }

        override suspend fun saveSchedule(schedule: MaintenanceSchedule): Result<Unit> {
            schedules.removeAll { it.id == schedule.id }
            schedules.add(schedule)
            return Result.success(Unit)
        }

        override suspend fun deleteSchedule(scheduleId: String): Boolean =
            schedules.removeAll { it.id == scheduleId }

        override suspend fun getRecords(vehicleId: String): List<VehicleMaintenanceRecord> = emptyList()
        override suspend fun saveRecord(record: VehicleMaintenanceRecord): Result<Unit> = Result.success(Unit)
        override suspend fun deleteRecord(recordId: String): Boolean = false
        override suspend fun recordCompletedService(
            scheduleId: String,
            performedKm: Long,
            performedDateMs: Long,
            costTomans: Long,
            serviceCenter: String?,
            partsBrand: String?,
            partNumber: String?,
            notes: String?
        ): Result<VehicleMaintenanceRecord> = error("Not needed in test")
    }

    private class FakeObdRepository(
        initialState: ConnectionState = ConnectionState.Disconnected
    ) : ObdRepository {
        private val _state = MutableStateFlow(initialState)
        override val connectionState: StateFlow<ConnectionState> = _state.asStateFlow()

        fun updateState(state: ConnectionState) {
            _state.value = state
        }

        override val activeVehicleProfile: StateFlow<VehicleProfile?> = MutableStateFlow(null)
        override val liveTelemetry: StateFlow<LiveTelemetry> = MutableStateFlow(LiveTelemetry())
        override suspend fun connect(type: ConnectionType, target: String): Result<Unit> = Result.success(Unit)
        override suspend fun connect(profile: ConnectionProfile): Result<Unit> = Result.success(Unit)
        override suspend fun disconnect() {}
        override suspend fun setVehicleProfile(profile: VehicleProfile) {}
        override suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>> = Result.success(emptyList())
        override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = Result.success(Unit)
        override suspend fun sendRawCommand(command: String): Result<String> = Result.success("OK")
        override fun startTelemetryLoop(): Flow<LiveTelemetry> = emptyFlow()
        override fun stopTelemetryLoop() {}
    }

    @Test
    fun initialStateWithNoVehicles_emitsEmptyActiveVehicleAndZeroSchedules() = runWithMain {
        val ownedRepo = FakeOwnedVehicles()
        val maintRepo = FakeMaintenanceRepository()
        val partsRepo = DefaultPartsRepository()
        val obdRepo = FakeObdRepository()

        val vm = HomeViewModel(
            ownedVehicleRepository = ownedRepo,
            maintenanceRepository = maintRepo,
            partsRepository = partsRepo,
            catalogRepository = DefaultVehicleCatalogRepository(),
            obdRepository = obdRepo
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNull(state.activeVehicle)
        assertTrue(state.vehicles.isEmpty())
        assertEquals(0, state.totalSchedulesCount)
        assertEquals(0, state.overdueCount)
        assertEquals(0, state.dueSoonCount)
        assertEquals(0, state.compatiblePartsCount)
        assertEquals(ConfidenceLevel.UNKNOWN, state.partsConfidence)
    }

    @Test
    fun initialStateWithVehicleHavingNoSchedules_reflectsTotalSchedulesZero() = runWithMain {
        val v1 = testVehicle()
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()
        val partsRepo = DefaultPartsRepository()
        val obdRepo = FakeObdRepository()

        val vm = HomeViewModel(
            ownedVehicleRepository = ownedRepo,
            maintenanceRepository = maintRepo,
            partsRepository = partsRepo,
            catalogRepository = DefaultVehicleCatalogRepository(),
            obdRepository = obdRepo
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertNotNull(state.activeVehicle)
        assertEquals("v1", state.activeVehicle?.id)
        // Truthful empty state: schedules count is 0
        assertEquals(0, state.totalSchedulesCount)
        assertEquals(0, state.overdueCount)
        assertEquals(0, state.dueSoonCount)
        assertNull(state.nextServiceTitle)
    }

    @Test
    fun vehicleWithOverdueAndDueSoonSchedules_calculatesCountsAndNextService() = runWithMain {
        val v1 = testVehicle(odo = 100_000L)
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")

        val s1 = MaintenanceSchedule(
            id = "s1",
            vehicleId = "v1",
            itemKey = "engine_oil",
            titleEn = "Engine Oil",
            titleFa = "روغن موتور",
            nextDueKm = 98_000L,
            dueState = DueState.OVERDUE
        )
        val s2 = MaintenanceSchedule(
            id = "s2",
            vehicleId = "v1",
            itemKey = "air_filter",
            titleEn = "Air Filter",
            titleFa = "فیلتر هوا",
            nextDueKm = 100_500L,
            dueState = DueState.DUE_SOON
        )
        val s3 = MaintenanceSchedule(
            id = "s3",
            vehicleId = "v1",
            itemKey = "spark_plugs",
            titleEn = "Spark Plugs",
            titleFa = "شمع موتور",
            nextDueKm = 110_000L,
            dueState = DueState.OK
        )

        val maintRepo = FakeMaintenanceRepository(mutableListOf(s1, s2, s3))
        val partsRepo = DefaultPartsRepository()
        val obdRepo = FakeObdRepository()

        val vm = HomeViewModel(
            ownedVehicleRepository = ownedRepo,
            maintenanceRepository = maintRepo,
            partsRepository = partsRepo,
            catalogRepository = DefaultVehicleCatalogRepository(),
            obdRepository = obdRepo
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(3, state.totalSchedulesCount)
        assertEquals(1, state.overdueCount)
        assertEquals(1, state.dueSoonCount)
        // Next upcoming due km > 100_000 is s2 (100_500 km, remaining 500 km)
        assertEquals("فیلتر هوا", state.nextServiceTitle)
        assertEquals(500L, state.nextServiceRemainingKm)
    }

    @Test
    fun vehicleWithCompatibleParts_populatesPartsCountAndConfidence() = runWithMain {
        val v1 = testVehicle(make = "Renault", model = "Tondar 90", year = 2012, engineCode = "K4M")
        val ownedRepo = FakeOwnedVehicles(listOf(v1), activeId = "v1")
        val maintRepo = FakeMaintenanceRepository()
        val partsRepo = DefaultPartsRepository()
        val catalogRepo = DefaultVehicleCatalogRepository()
        val obdRepo = FakeObdRepository()

        val vm = HomeViewModel(
            ownedVehicleRepository = ownedRepo,
            maintenanceRepository = maintRepo,
            partsRepository = partsRepo,
            catalogRepository = catalogRepo,
            obdRepository = obdRepo
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue("Compatible parts should be populated for Tondar 90 K4M", state.compatiblePartsCount > 0)
        assertEquals(ConfidenceLevel.VERIFIED_OEM, state.partsConfidence)
    }

    @Test
    fun intentToggleAndDismissVehicleSelector_updatesShowVehicleSelector() = runWithMain {
        val ownedRepo = FakeOwnedVehicles()
        val maintRepo = FakeMaintenanceRepository()
        val partsRepo = DefaultPartsRepository()
        val obdRepo = FakeObdRepository()

        val vm = HomeViewModel(
            ownedVehicleRepository = ownedRepo,
            maintenanceRepository = maintRepo,
            partsRepository = partsRepo,
            catalogRepository = DefaultVehicleCatalogRepository(),
            obdRepository = obdRepo
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showVehicleSelector)

        vm.onIntent(HomeIntent.ToggleVehicleSelector)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.showVehicleSelector)

        vm.onIntent(HomeIntent.DismissVehicleSelector)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showVehicleSelector)
    }

    @Test
    fun obdConnectionStateUpdates_reflectsInHomeState() = runWithMain {
        val ownedRepo = FakeOwnedVehicles()
        val maintRepo = FakeMaintenanceRepository()
        val partsRepo = DefaultPartsRepository()
        val obdRepo = FakeObdRepository(ConnectionState.Disconnected)

        val vm = HomeViewModel(
            ownedVehicleRepository = ownedRepo,
            maintenanceRepository = maintRepo,
            partsRepository = partsRepo,
            catalogRepository = DefaultVehicleCatalogRepository(),
            obdRepository = obdRepo
        )
        advanceUntilIdle()

        assertEquals(ConnectionState.Disconnected, vm.uiState.value.connectionState)

        val connected = ConnectionState.Connected(
            deviceName = "OBDII",
            adapterType = "ELM327 v1.5",
            protocolName = "ISO 15765-4 (CAN 11/500)",
            elmVersion = "ELM327 v1.5",
            batteryVoltage = 12.6f
        )
        obdRepo.updateState(connected)
        advanceUntilIdle()

        assertEquals(connected, vm.uiState.value.connectionState)
    }

    @Test fun coldStartHydratesPersistedSelectionAndNeverInventsOdometer() = runWithMain {
        for(odo in listOf<Long?>(null,0L,80_000L)) {
            val vehicle=testVehicle(odo=odo)
            val owners=FakeOwnedVehicles(listOf(vehicle), "v1", hydrateOnRefresh=true)
            val schedule=MaintenanceSchedule("s","v1","oil","Oil","روغن",nextDueKm=90_000L)
            val vm=HomeViewModel(owners,FakeMaintenanceRepository(mutableListOf(schedule)),DefaultPartsRepository(),DefaultVehicleCatalogRepository(),FakeObdRepository())
            advanceUntilIdle()
            assertEquals(vehicle,vm.uiState.value.activeVehicle)
            assertEquals(1,vm.uiState.value.unknownSchedulesCount)
            assertEquals(odo,vm.uiState.value.activeVehicle!!.currentOdometerKm)
            if(odo==null) assertNull(vm.uiState.value.nextServiceRemainingKm)
            else assertEquals(90_000L-odo,vm.uiState.value.nextServiceRemainingKm)
        }
    }
    @Test fun clearedSelectionDoesNotSilentlyChooseAnotherVehicle() = runWithMain {
        val vm=HomeViewModel(FakeOwnedVehicles(listOf(testVehicle())),FakeMaintenanceRepository(),DefaultPartsRepository(),DefaultVehicleCatalogRepository(),FakeObdRepository())
        advanceUntilIdle();assertNull(vm.uiState.value.activeVehicle)
    }
}
