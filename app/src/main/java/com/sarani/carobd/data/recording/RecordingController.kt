package com.sarani.carobd.data.recording

import com.sarani.carobd.core.adapter.transport.*
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.adapter.command.toWireEnvelopes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/** Memory-only observation on dispatch; periodic IO snapshots happen in a separate coroutine. */
class RecordingController(val store: FileTranscriptStore, private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())) {
    private val _state = MutableStateFlow("Stopped")
    val state = _state.asStateFlow()
    @Volatile var builder: DiagnosticTranscriptBuilder? = null
        private set
    @Volatile private var metadata: TranscriptMetadata? = null
    init {
        scope.launch {
            while (isActive) {
                delay(1000)
                val captured = builder ?: continue
                try { store.save(captured.toTranscript()) }
                catch (e: Exception) { _state.value = "Storage failed: ${e.message}" }
            }
        }
    }
    @Synchronized fun session(profile: ConnectionProfile) {
        builder?.let { old -> val snapshot = old.toTranscript(); scope.launch { runCatching { store.save(snapshot) } } }
        metadata = TranscriptMetadata(profile.name, profile.transport.type, profile.transport.connectionTarget)
        builder = null
        _state.value = "Stopped"
    }
    @Synchronized fun start() {
        check(builder == null) { "Already recording" }
        val md = requireNotNull(metadata) { "Connect a session before starting recording" }
        builder = DiagnosticTranscriptBuilder(md.copy(startedAtMs = System.currentTimeMillis(), sessionId = java.util.UUID.randomUUID().toString())).also { it.recordConnect() }
        _state.value = "Recording"
    }
    @Synchronized fun identity(state: ConnectionState.Connected) {
        metadata = metadata?.copy(adapterIdentity = state.elmVersion, vehicleProtocol = state.protocolName)
        builder?.updateIdentity(state.elmVersion, state.protocolName)
    }
    suspend fun stop() {
        val snapshot = synchronized(this) {
            val current = builder ?: return
            builder = null
            _state.value = "Saving"
            current.toTranscript()
        }
        try { withContext(Dispatchers.IO) { store.save(snapshot) }; _state.value = "Stopped" }
        catch (e: Exception) { _state.value = "Storage failed: ${e.message}"; throw e }
    }
    @Synchronized fun failure(e: Throwable) {
        val snapshot = builder?.toTranscript()
        builder = null
        _state.value = "Capture stopped: ${e.message}"
        if (snapshot != null) scope.launch { runCatching { store.save(snapshot) } }
    }
    @Synchronized fun disconnected() {
        val snapshot = builder?.toTranscript()
        builder = null
        metadata = null
        _state.value = "Stopped"
        if (snapshot != null) scope.launch { runCatching { store.save(snapshot) }.onFailure { _state.value = "Storage failed: ${it.message}" } }
    }
    fun parsed(command: String, response: com.sarani.carobd.core.adapter.command.AdapterResponse) {
        try { response.toWireEnvelopes(command).forEach { builder?.recordParsed(command, it) } }
        catch (e: Exception) { failure(e) }
    }
}

