package com.sarani.carobd.data.session

import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.telemetry.scheduler.TelemetryProducer
import com.sarani.carobd.core.telemetry.trip.TripComputer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/** One ownership generation: no adapter, reader, producer or request outlives this owner. */
class DiagnosticSession(
    val transport: ObdTransport,
    parentScope: CoroutineScope,
    private val onState: (ConnectionState) -> Unit,
    private val onTelemetry: (LiveTelemetry) -> Unit,
    private val onLoss: () -> Unit,
    connectTimeoutMs: Long = 3000L,
    commandTimeoutMs: Long = 2500L,
    onEvidence: (ConnectionTestEvidence) -> Unit = {},
    adapterFactory: (ObdTransport, CoroutineScope) -> ObdAdapter = { t, s -> Elm327Adapter(t, s, commandTimeoutMs, onEvidence) },
) {
    private val owner = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + owner)
    private val closed = AtomicBoolean(false)
    val adapter: ObdAdapter = adapterFactory(transport, scope)
    private val requestAdmission = kotlinx.coroutines.sync.Semaphore(8)
    private var producer: TelemetryProducer? = null
    val isOpen: Boolean get() = !closed.get() && owner.isActive

    init {
        scope.launch {
            adapter.state.collect { state -> if (isOpen) onState(state) }
        }
        scope.launch {
            delay(connectTimeoutMs.coerceIn(100L, 30_000L))
            if (isOpen && !transport.isConnected && adapter.state.value !is ConnectionState.Connected) {
                invalidate()
                withContext(NonCancellable) { transport.disconnect() }
                onState(ConnectionState.Failure("TRANSPORT", "زمان اتصال منقضی شد", "Transport connect deadline exceeded", true))
                onLoss()
            }
        }
        scope.launch {
            var sawLink = false
            while (isActive && isOpen) {
                if (transport.isConnected || adapter.state.value is ConnectionState.Connected) sawLink = true
                if (sawLink && !transport.isConnected) {
                    invalidate()
                    onState(ConnectionState.Failure("TRANSPORT_LOST", "ارتباط قطع شد", "Transport disconnected", true))
                    onLoss()
                    break
                }
                delay(100) // Observes transport status; sends no bus traffic.
            }
        }
    }

    suspend fun <T> execute(block: suspend (ObdAdapter) -> T): T {
        check(isOpen) { "Diagnostic session closed" }
        check(requestAdmission.tryAcquire()) { "Diagnostic session busy (maximum 8 requests)" }
        try {
        val work = scope.async { check(isOpen); block(adapter) }
        try { return work.await() }
        catch (e: CancellationException) {
            withContext(NonCancellable) { work.cancelAndJoin() }
            throw e
        }
        } finally { requestAdmission.release() }
    }

    @Synchronized
    fun startTelemetry(mode: StateFlow<TelemetryPollMode>, trip: TripComputer? = null) {
        if (!isOpen || producer != null) return
        producer = TelemetryProducer(mode, { cmd, timeout ->
            if (!isOpen || !transport.isConnected) null else {
                val r = adapter.executeCommand(cmd, timeout)
                (r as? com.sarani.carobd.core.adapter.command.AdapterResponse.Success)?.rawResponse
            }
        }, { isOpen && transport.isConnected && adapter.state.value is ConnectionState.Connected }, trip,
            { value -> if (isOpen) onTelemetry(value) }).also { it.startIn(scope) }
    }

    @Synchronized fun stopTelemetry() { producer?.stop() }
    fun invalidate() {
        closed.set(true)
        stopTelemetry()
        owner.cancel()
    }
    suspend fun close() {
        invalidate()
        withContext(NonCancellable) {
            // Close hardware first to release any blocking physical read/write.
            runCatching { transport.disconnect() }
            owner.cancelAndJoin()
            adapter.disconnect()
        }
    }
}
