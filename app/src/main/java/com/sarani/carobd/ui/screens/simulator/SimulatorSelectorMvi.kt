package com.sarani.carobd.ui.screens.simulator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.adapter.profile.ConnectionProfileRepository
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.data.transport.SimulatorScenario
import com.sarani.carobd.data.transport.simulatorScenarioFromId
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * §17 P1 REAL SIMULATOR PRODUCT FLOW — state + intent + effect.
 *
 * Lets the user pick a deterministic [SimulatorScenario], persists a Simulator
 * [ConnectionProfile] for it (so the choice survives restart), then connects. The
 * selected scenario flows: Profile → [TransportFactory] → IsolatedSimulatorTransport,
 * closing the §17 placebo gap (scenarioId is no longer ignored).
 */
data class SimulatorSelectorState(
    val scenarios: List<SimulatorScenario> = SimulatorScenario.values().toList(),
    val selectedScenarioId: String = SimulatorScenario.HEALTHY_VEHICLE.id,
    val connectionState: ConnectionState = ConnectionState.Idle,
)

sealed interface SimulatorSelectorIntent {
    data class SelectScenario(val id: String) : SimulatorSelectorIntent
    data object Connect : SimulatorSelectorIntent
    data object ClearSelection : SimulatorSelectorIntent
}

sealed interface SimulatorSelectorEffect {
    data class Message(val text: String) : SimulatorSelectorEffect
}

class SimulatorSelectorViewModel(
    private val obdRepository: ObdRepository,
    private val profiles: ConnectionProfileRepository,
) : ViewModel() {

    private val _effect = Channel<SimulatorSelectorEffect>(Channel.BUFFERED)
    val effect: Flow<SimulatorSelectorEffect> = _effect.receiveAsFlow()

    private val selectedScenarioId = MutableStateFlow(SimulatorScenario.HEALTHY_VEHICLE.id)

    val uiState: StateFlow<SimulatorSelectorState> = combine(
        selectedScenarioId,
        obdRepository.connectionState,
    ) { id, conn ->
        SimulatorSelectorState(selectedScenarioId = id, connectionState = conn)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SimulatorSelectorState())

    fun onIntent(intent: SimulatorSelectorIntent) {
        when (intent) {
            is SimulatorSelectorIntent.SelectScenario -> selectedScenarioId.value = intent.id
            is SimulatorSelectorIntent.Connect -> connect()
            is SimulatorSelectorIntent.ClearSelection -> {
                viewModelScope.launch {
                    val defaultId = profiles.defaultProfile().id
                    profiles.select(defaultId)
                }
            }
        }
    }

    private fun connect() {
        viewModelScope.launch {
            val id = selectedScenarioId.value
            val scenario = simulatorScenarioFromId(id)
            val profile = ConnectionProfile.newProfile(
                name = "Simulator · ${scenario.labelEn}",
                transport = TransportConfig.Simulator(id),
            )
            val savedId = profiles.saveAndSelect(profile)
            if (savedId == null) {
                _effect.trySend(SimulatorSelectorEffect.Message("Invalid simulator profile"))
                return@launch
            }
            val res = obdRepository.connect(profile.copy(id = savedId))
            if (res.isFailure) {
                _effect.trySend(
                    SimulatorSelectorEffect.Message(
                        res.exceptionOrNull()?.localizedMessage ?: "Connection failed"
                    )
                )
            }
        }
    }
}
