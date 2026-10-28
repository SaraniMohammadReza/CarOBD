package com.sarani.carobd.ui.screens.simulator

import com.sarani.carobd.core.adapter.profile.InMemoryConnectionProfileRepository
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.data.transport.SimulatorScenario
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** §17: verifier that the chosen scenario is persisted + routed to the transport. */
@OptIn(ExperimentalCoroutinesApi::class)
class SimulatorSelectorViewModelTest {

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeObdRepository : ObdRepository {
        val connectCalls = mutableListOf<ConnectionProfile>()
        private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val connectionState: StateFlow<ConnectionState> = _state
        override val activeVehicleProfile: StateFlow<VehicleProfile?> = MutableStateFlow(null)
        override val liveTelemetry: StateFlow<LiveTelemetry> = MutableStateFlow(LiveTelemetry())

        override suspend fun connect(type: com.sarani.carobd.core.model.ConnectionType,
                                     target: String): Result<Unit> = Result.success(Unit)
        override suspend fun connect(profile: ConnectionProfile): Result<Unit> {
            connectCalls.add(profile)
            return Result.success(Unit)
        }
        override suspend fun disconnect() {}
        override suspend fun setVehicleProfile(profile: VehicleProfile) {}
        override suspend fun scanDiagnosticTroubleCodes(): Result<List<com.sarani.carobd.core.model.DiagnosticCode>> =
            Result.success(emptyList())
        override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = Result.success(Unit)
        override suspend fun sendRawCommand(command: String): Result<String> = Result.success("")
        override fun startTelemetryLoop(): kotlinx.coroutines.flow.Flow<LiveTelemetry> =
            kotlinx.coroutines.flow.emptyFlow()
        override fun stopTelemetryLoop() {}
    }

    private fun runWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    @Test
    fun initialStateExposesAllScenariosAndHealthyDefault() = runWithMain {
        val vm = SimulatorSelectorViewModel(FakeObdRepository(), InMemoryConnectionProfileRepository())
        advanceUntilIdle()
        val s = vm.uiState.value
        assertEquals(SimulatorScenario.values().toList(), s.scenarios)
        assertEquals(SimulatorScenario.HEALTHY_VEHICLE.id, s.selectedScenarioId)
    }

    @Test
    fun selectScenarioUpdatesState() = runWithMain {
        val vm = SimulatorSelectorViewModel(FakeObdRepository(), InMemoryConnectionProfileRepository())
        advanceUntilIdle()
        vm.onIntent(SimulatorSelectorIntent.SelectScenario(SimulatorScenario.BUFFER_FULL.id))
        advanceUntilIdle()
        assertEquals(SimulatorScenario.BUFFER_FULL.id, vm.uiState.value.selectedScenarioId)
    }

    @Test
    fun connectPersistsAndRoutesSelectedScenario() = runWithMain {
        val repo = FakeObdRepository()
        val profiles = InMemoryConnectionProfileRepository()
        val vm = SimulatorSelectorViewModel(repo, profiles)
        advanceUntilIdle()

        vm.onIntent(SimulatorSelectorIntent.SelectScenario(SimulatorScenario.NO_DATA.id))
        vm.onIntent(SimulatorSelectorIntent.Connect)
        advanceUntilIdle()

        // The profile persisted via saveAndSelect is a Simulator transport carrying the scenario.
        val selected = profiles.selectedProfile()
        assertTrue(selected?.transport is TransportConfig.Simulator)
        assertEquals(
            SimulatorScenario.NO_DATA.id,
            (selected!!.transport as TransportConfig.Simulator).scenarioId
        )
        // and the same scenario reaches the adapter-bound connect(profile) call.
        assertTrue(repo.connectCalls.isNotEmpty())
        assertEquals(
            SimulatorScenario.NO_DATA.id,
            (repo.connectCalls.last().transport as TransportConfig.Simulator).scenarioId
        )
    }
}
