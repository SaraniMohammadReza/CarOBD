package com.sarani.carobd.ui.screens.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.core.adapter.profile.ConnectionProfileRepository
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionTestReport
import kotlinx.coroutines.flow.MutableStateFlow
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.domain.repository.ObdRepository
import com.sarani.carobd.ui.mvi.UiEffect
import com.sarani.carobd.ui.mvi.UiIntent
import com.sarani.carobd.ui.mvi.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * §12 P1 REAL CONNECTION CENTER — state + intent.
 *
 * The dashboard keeps a compact quick-connect shortcut (§12 allows this); the
 * full connection management lives here as a normal navigation destination.
 */
data class ConnectionCenterState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val profiles: List<ProfileSummary> = emptyList(),
    val selectedProfileId: String? = null,
    val fullProfiles: List<ConnectionProfile> = emptyList(),
    val testReport: ConnectionTestReport? = null,
) : UiState {
    val selectedProfile: ProfileSummary? get() =
        profiles.find { it.id == selectedProfileId }
    val isConnected: Boolean get() = connectionState is ConnectionState.Connected
    val isConnecting: Boolean get() = isConnectingState(connectionState)
}

data class ProfileSummary(
    val id: String,
    val name: String,
    val transportType: String,
    val target: String,
    val protocol: String,
    val autoReconnect: Boolean,
    val lastSuccessfulAt: Long?,
)

sealed interface ConnectionCenterIntent : UiIntent {
    data object Connect : ConnectionCenterIntent
    data object Disconnect : ConnectionCenterIntent
    data object CreateNewProfile : ConnectionCenterIntent
    data class SaveProfile(val profile: ConnectionProfile) : ConnectionCenterIntent
    data class Select(val profileId: String) : ConnectionCenterIntent
    data class ConnectSpecific(val profileId: String) : ConnectionCenterIntent
    data class Delete(val profileId: String) : ConnectionCenterIntent
    data class TestConnection(val profileId: String) : ConnectionCenterIntent
}

sealed interface ConnectionCenterEffect : UiEffect {
    data class ShowMessage(val text: String) : ConnectionCenterEffect
}

fun ConnectionProfile.summarize(): ProfileSummary = ProfileSummary(
    id = id,
    name = name,
    transportType = transport.type.toString(),
    target = transport.connectionTarget ?: "—",
    protocol = protocolMode.name,
    autoReconnect = autoReconnect,
    lastSuccessfulAt = lastSuccessfulAt,
)

private fun isConnectingState(state: ConnectionState): Boolean = when (state) {
    is ConnectionState.TransportConnecting,
    is ConnectionState.TransportConnected,
    is ConnectionState.AdapterResetting,
    is ConnectionState.AdapterIdentifying,
    is ConnectionState.AdapterConfiguring,
    is ConnectionState.ProtocolDetecting,
    is ConnectionState.EcuDiscovering,
    is ConnectionState.CapabilityDiscovering,
    is ConnectionState.Retrying -> true
    else -> false
}

class ConnectionCenterViewModel(
    private val obdRepository: ObdRepository,
    private val profiles: ConnectionProfileRepository,
) : ViewModel() {

    private val testReport = MutableStateFlow<ConnectionTestReport?>(null)
    private var testJob: kotlinx.coroutines.Job? = null
    private val _effect = Channel<ConnectionCenterEffect>(Channel.BUFFERED)
    val effect: Flow<ConnectionCenterEffect> = _effect.receiveAsFlow()

    val uiState = combine(
        obdRepository.connectionState,
        profiles.allProfiles,
        profiles.selectedProfileId,
        testReport,
    ) { state, profileList, selId, report ->
        ConnectionCenterState(
            connectionState = state,
            profiles = profileList.map { it.summarize() },
            selectedProfileId = selId,
            fullProfiles = profileList,
            testReport = report,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionCenterState())

    fun onIntent(intent: ConnectionCenterIntent) {
        when (intent) {
            ConnectionCenterIntent.Connect -> {
                val p = uiState.value.selectedProfile ?: return
                connect(p.id)
            }
            ConnectionCenterIntent.Disconnect -> viewModelScope.launch { obdRepository.disconnect() }
            ConnectionCenterIntent.CreateNewProfile -> createNewProfile()
            is ConnectionCenterIntent.SaveProfile -> viewModelScope.launch {
                try {
                    if (profiles.saveAndSelect(intent.profile) == null) _effect.trySend(ConnectionCenterEffect.ShowMessage("Invalid profile: ${intent.profile.validate()}"))
                } catch (e: Exception) { _effect.trySend(ConnectionCenterEffect.ShowMessage(e.message ?: "Could not save profile")) }
            }
            is ConnectionCenterIntent.Select -> select(intent.profileId)
            is ConnectionCenterIntent.ConnectSpecific -> connect(intent.profileId)
            is ConnectionCenterIntent.Delete -> delete(intent.profileId)
            is ConnectionCenterIntent.TestConnection -> testConnection(intent.profileId)
        }
    }

    private fun connect(profileId: String) {
        viewModelScope.launch {
            val profile = profiles.allProfiles
                .map { list -> list.find { it.id == profileId } }
                .first()
            if (profile == null) {
                _effect.trySend(ConnectionCenterEffect.ShowMessage("Profile not found"))
                return@launch
            }
            val res = obdRepository.connect(profile)
            if (res.isFailure) {
                _effect.trySend(
                    ConnectionCenterEffect.ShowMessage(
                        res.exceptionOrNull()?.localizedMessage ?: "Connection failed"
                    )
                )
            }
        }
    }

    private fun select(profileId: String) {
        viewModelScope.launch { profiles.select(profileId) }
    }

    private fun createNewProfile() {
        viewModelScope.launch {
            val id = profiles.saveAndSelect(
                ConnectionProfile.newProfile("New connection", TransportConfig.Simulator("healthy"))
            )
            if (id == null) _effect.trySend(ConnectionCenterEffect.ShowMessage("Invalid profile"))
        }
    }

    private fun delete(profileId: String) {
        viewModelScope.launch { profiles.delete(profileId) }
    }

    private fun testConnection(profileId: String) {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            val profile = profiles.allProfiles.first().find { it.id == profileId } ?: return@launch
            obdRepository.testConnection(profile) { testReport.value = it }
        }
    }
}
