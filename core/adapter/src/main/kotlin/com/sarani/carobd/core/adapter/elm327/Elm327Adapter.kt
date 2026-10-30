package com.sarani.carobd.core.adapter.elm327

import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.adapter.command.AdapterError
import com.sarani.carobd.core.adapter.command.AdapterInitializationResult
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ProtocolStandard
import com.sarani.carobd.core.protocol.obd2.PidCodec
import com.sarani.carobd.core.adapter.safety.AuthorizedDiagnosticCommand
import com.sarani.carobd.core.adapter.command.toWireEnvelopes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Robust ELM327 / STN communication adapter.
 * Handles state machine synchronization, buffer parsing, timeout policies,
 * protocol negotiation, and hardware health monitoring.
 */
open class Elm327Adapter(
    private val transport: ObdTransport,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val maxCommandTimeoutMs: Long = 30_000L,
    private val onEvidence: (ConnectionTestEvidence) -> Unit = {},
    private val onResponse: (String, AdapterResponse) -> Unit = { _, _ -> }
) : ObdAdapter {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var _currentProtocol: ProtocolStandard? = null
    override val currentProtocol: ProtocolStandard?
        get() = _currentProtocol

    private var _batteryVoltage: Float = 0.0f
    override val batteryVoltage: Float
        get() = _batteryVoltage

    // §2: expose CAN-family transport status on the adapter abstraction so the
    // repository can select DTC framing without resolving the model-layer
    // ProtocolStandard enum (which is shadowed by an app-module duplicate).
    // Elm327Adapter resolves core/model's real ProtocolStandard here in core:adapter.
    override val isCanTransport: Boolean
        get() = _currentProtocol?.isCanBus == true

    private val commandMutex = Mutex()
    private val admission = Semaphore(8) // Bound callers waiting for the wire mutex.
    @Volatile private var retired = false
    private var lastDispatchNanos = 0L
    // §5 bounded buffering (was UNLIMITED): caps memory so a flooded/stuck adapter
    // cannot grow the inbound channel without limit.
    private val incomingCharsChannel = Channel<Char>(4096)
    private var readerJob: Job? = null

    // §5: set true when a post-timeout resynchronisation window expires WITHOUT
    // observing the terminating '>' prompt — the stream is unverified until reset.
    private var resyncCompromised = false

    init {
        startIncomingReader()
    }

    private fun startIncomingReader() {
        readerJob?.cancel()
        readerJob = scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            try {
                transport.incomingBytes.collect { chunk ->
                    for (b in chunk) {
                        incomingCharsChannel.send(b.toInt().toChar())
                    }
                }
                retire("Incoming stream ended")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                retire("Stream reader failed: ${e.message}")
                _currentProtocol = null
                _state.value = ConnectionState.Failure(
                    stage = "STREAM_READ",
                    humanMessageFa = "خطا در دریافت جریان داده از آداپتور",
                    technicalMessage = e.message ?: "Unknown stream fault",
                    isRecoverable = true
                )
            }
        }
    }

    override suspend fun initialize(forcedProtocol: ProtocolStandard?): AdapterInitializationResult =
        commandMutex.withLock {
            // A retired stream may never be reused. Reconnect requires a new adapter/transport.
            try {
                check(!retired) { "Session retired; create a new adapter" }
                evidence(ConnectionTestStage.TRANSPORT, ConnectionTestStatus.RUNNING, transport.transportType.name)
                _state.value = ConnectionState.TransportConnecting(transport.transportType.name)
                if (!transport.isConnected) {
                    transport.connect()
                }

                _state.value = ConnectionState.TransportConnected(transport.transportType.name)
                evidence(ConnectionTestStage.TRANSPORT, ConnectionTestStatus.PASS, "Transport open: ${transport.transportType}")
                evidence(ConnectionTestStage.ADAPTER, ConnectionTestStatus.RUNNING, "ATZ and controlled configuration")
                _state.value = ConnectionState.AdapterResetting
                drainPendingBuffer()

                // 1. Reset chip: ATZ
                check(executeInternal("ATZ", 2500L) is AdapterResponse.Success) { "Adapter reset failed" }
                kotlinx.coroutines.delay(200)
                drainPendingBuffer()

                // 2. Disable echo: ATE0
                checkOk("ATE0", 1000L)

                // 3. Disable linefeeds: ATL0
                checkOk("ATL0", 1000L)

                // 4. Disable spaces for faster transmission: ATS0
                checkOk("ATS0", 1000L)

                // 5. Adaptive timing auto 1: ATAT1
                checkOk("ATAT1", 1000L)

                evidence(ConnectionTestStage.ADAPTER, ConnectionTestStatus.PASS, "ATZ response; formatting commands acknowledged OK")
                evidence(ConnectionTestStage.IDENTITY, ConnectionTestStatus.RUNNING, "ATI / ATRV")
                _state.value = ConnectionState.AdapterIdentifying("ATI / ATRV")
                // 6. Run hardware probe (ATI, ATRV, Clone check)
                val prober = Elm327Prober { cmd, timeout -> executeInternal(cmd, timeout) }
                val probeResult = prober.probeHardware()
                check(probeResult.firmwareVersion != "Unknown") { "Adapter identity read failed" }
                _batteryVoltage = probeResult.voltage
                evidence(ConnectionTestStage.IDENTITY, ConnectionTestStatus.PASS, "${probeResult.firmwareVersion}; ATRV=${probeResult.voltage}V (0 means unavailable)")
                evidence(ConnectionTestStage.PROTOCOL, ConnectionTestStatus.RUNNING, "Controlled ATSP selection")

                // 7. Negotiate or force protocol
                _state.value = ConnectionState.ProtocolDetecting(
                    candidateName = forcedProtocol?.displayName ?: "Auto Search",
                    attemptIndex = 1,
                    totalCandidates = 1
                )
                if (forcedProtocol != null) {
                    val atspCmd = getElmProtocolCommand(forcedProtocol)
                    checkOk(atspCmd, 1500L)
                    _currentProtocol = forcedProtocol
                } else {
                    checkOk("ATSP0", 1500L)
                }

                // 8. Issue first query (0100) to trigger vehicle handshake.
                // Require real, service-correlated ECU evidence: only a valid Mode 01 PID 00
                // response (0x41 0x00 followed by >=4 supported-PID data bytes) may advance
                // to Connected. Any adapter/ECU error, timeout, or malformed frame must fail
                // initialization; receiving the prompt alone is not proof of communication.
                evidence(ConnectionTestStage.ECU, ConnectionTestStatus.RUNNING, "0100")
                _state.value = ConnectionState.EcuDiscovering(forcedProtocol?.displayName ?: "AUTO")
                val init0100 = executeInternal("0100", 4000L)
                when {
                    init0100 is AdapterResponse.Error -> {
                        val err = init0100.error
                        evidence(ConnectionTestStage.ECU, ConnectionTestStatus.FAIL, init0100.rawResponse.ifBlank { err.messageEn })
                        retire("ECU handshake failed")
                        _currentProtocol = null
                        _state.value = ConnectionState.Failure(
                            stage = "ECU_HANDSHAKE",
                            humanMessageFa = "ارتباط با کامپیوتر خودرو برقرار نشد",
                            technicalMessage = "0100 failed: ${err.messageEn}",
                            isRecoverable = err !is AdapterError.NoData && err !is AdapterError.UnknownCommand,
                            rawResponse = init0100.rawResponse
                        )
                        return AdapterInitializationResult(
                            isSuccess = false,
                            firmwareVersion = probeResult.firmwareVersion,
                            detectedProtocol = null,
                            batteryVoltage = probeResult.voltage,
                            capabilities = probeResult.capabilities,
                            quality = probeResult.quality,
                            errorMessage = "Vehicle ECU did not respond to 0100 (${err.messageEn})"
                        )
                    }
                    init0100 is AdapterResponse.Success -> {
                        val replies = init0100.toWireEnvelopes("0100")
                        val validResponse = replies.isNotEmpty() && replies.all { it.result is com.sarani.carobd.core.model.DiagnosticResult.Positive }
                        if (!validResponse) {
                            evidence(ConnectionTestStage.ECU, ConnectionTestStatus.FAIL, "Malformed 0100: ${init0100.rawResponse}")
                            retire("Malformed ECU handshake")
                            _currentProtocol = null
                            _state.value = ConnectionState.Failure(
                                stage = "ECU_HANDSHAKE",
                                humanMessageFa = "پاسخٔ ECU برای 0100 معتبر نیست",
                                technicalMessage = "0100 response was not a valid Mode 01 PID 00 frame: ${init0100.rawResponse}",
                                isRecoverable = false,
                                rawResponse = init0100.rawResponse
                            )
                            return AdapterInitializationResult(
                                isSuccess = false,
                                firmwareVersion = probeResult.firmwareVersion,
                                detectedProtocol = null,
                                batteryVoltage = probeResult.voltage,
                                capabilities = probeResult.capabilities,
                                quality = probeResult.quality,
                                errorMessage = "Vehicle ECU 0100 response was malformed"
                            )
                        }
                    }
                }

                evidence(ConnectionTestStage.ECU, ConnectionTestStatus.PASS, (init0100 as AdapterResponse.Success).rawResponse)
                evidence(ConnectionTestStage.CAPABILITIES, ConnectionTestStatus.RUNNING, "Validating PID 00 bitmap from ECU response")
                _state.value = ConnectionState.CapabilityDiscovering(0)
                val capabilityFrames = init0100.toWireEnvelopes("0100")
                evidence(ConnectionTestStage.CAPABILITIES, ConnectionTestStatus.PASS,
                    capabilityFrames.joinToString("; ") { "${it.responderId ?: "unknown responder"}: " + it.payload.joinToString("") { b -> "%02X".format(b) } })

                // 9. Read back detected protocol using ATDPN
                val dpnResp = executeInternal("ATDPN", 1000L)
                val readback = (dpnResp as? AdapterResponse.Success)?.cleanedLines?.singleOrNull()?.trim()?.uppercase()
                val code = readback?.let { if (it.length == 2 && it.startsWith("A")) it.drop(1) else it }
                _currentProtocol = code?.let { mapElmProtocolNumber(it) }
                check(_currentProtocol != null && _currentProtocol != ProtocolStandard.AUTO) { "Unknown protocol readback: $readback" }
                if (forcedProtocol != null && forcedProtocol != ProtocolStandard.AUTO) {
                    check(_currentProtocol == forcedProtocol) { "Manual protocol mismatch: requested $forcedProtocol, observed $_currentProtocol" }
                }

                check(!retired && transport.isConnected) { "Transport lost during initialization" }
                evidence(ConnectionTestStage.PROTOCOL, ConnectionTestStatus.PASS, "ATDPN=$readback; ${_currentProtocol?.displayName}")
                _state.value = ConnectionState.Connected(
                    deviceName = probeResult.deviceDescription.ifEmpty { "OBD-II Dongle" },
                    adapterType = probeResult.quality.name,
                    protocolName = _currentProtocol?.displayName ?: "OBD-II Standard",
                    elmVersion = probeResult.firmwareVersion,
                    batteryVoltage = probeResult.voltage,
                    isSimulator = transport.transportType == com.sarani.carobd.core.model.TransportType.SIMULATOR
                )

                AdapterInitializationResult(
                    isSuccess = true,
                    firmwareVersion = probeResult.firmwareVersion,
                    detectedProtocol = _currentProtocol,
                    batteryVoltage = probeResult.voltage,
                    capabilities = probeResult.capabilities,
                    quality = probeResult.quality
                )

            } catch (e: CancellationException) {
                retire("Initialization canceled")
                _currentProtocol = null
                throw e
            } catch (e: Exception) {
                retire("Initialization failed: ${e.message}")
                _currentProtocol = null
                _state.value = ConnectionState.Failure(
                    stage = "INITIALIZATION",
                    humanMessageFa = "خطا در آماده‌سازی آداپتور عیب‌یابی",
                    technicalMessage = e.message ?: "Initialization exception",
                    isRecoverable = true
                )
                AdapterInitializationResult(
                    isSuccess = false,
                    firmwareVersion = "Unknown",
                    detectedProtocol = null,
                    batteryVoltage = 0.0f,
                    capabilities = com.sarani.carobd.core.model.AdapterCapabilities(),
                    quality = com.sarani.carobd.core.model.AdapterQuality.COMPATIBLE,
                    errorMessage = e.message
                )
            }
        }

    override suspend fun executeCommand(command: String, timeoutMs: Long): AdapterResponse {
        val authorized = AuthorizedDiagnosticCommand.readOnly(command) ?: return deniedCommand()
        if (!admission.tryAcquire()) return AdapterResponse.Error(AdapterError.BusBusy, "", 0L)
        try {
            return commandMutex.withLock { dispatch(authorized, timeoutMs) }
        } finally { admission.release() }
    }

    // Routing changes require a new verified session; arbitrary header strings are disabled.
    override suspend fun setHeader(headerHex: String): Boolean = false
    override suspend fun selectProtocol(protocol: ProtocolStandard): Boolean = false

    override suspend fun clearSimulatorDiagnosticState(): AdapterResponse = commandMutex.withLock {
        if (transport.transportType != com.sarani.carobd.core.model.TransportType.SIMULATOR) return@withLock deniedCommand()
        dispatch(AuthorizedDiagnosticCommand.simulatorClear(), 1000L)
    }

    private fun deniedCommand(): AdapterResponse = AdapterResponse.Error(
        AdapterError.Other("Command disabled by read-only safety policy"), "", 0L)

    override suspend fun disconnect() {
        retired = true // Prevent queued commands before waiting for the in-flight owner.
        withContext(NonCancellable) {
            transport.disconnect()
            readerJob?.cancelAndJoin()
            commandMutex.withLock { drainPendingBuffer() }
            _state.value = ConnectionState.Disconnected
        }
    }

    private suspend fun retire(reason: String) {
        retired = true
        resyncCompromised = true
        _state.value = ConnectionState.Failure("COMMAND_OWNERSHIP", "نشست ارتباطی متوقف شد", reason, false)
        val reader = readerJob
        val retiringReaderItself = reader === kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        withContext(NonCancellable) {
            runCatching { transport.disconnect() }
            if (!retiringReaderItself) reader?.cancelAndJoin()
            drainPendingBuffer()
        }
    }

    private suspend fun executeInternal(command: String, timeoutMs: Long): AdapterResponse {
        val authorized = AuthorizedDiagnosticCommand.initialization(command) ?: return deniedCommand()
        return dispatch(authorized, timeoutMs)
    }

    private suspend fun dispatch(authorized: AuthorizedDiagnosticCommand, timeoutMs: Long): AdapterResponse {
        val response = dispatchInternal(authorized, timeoutMs)
        runCatching { onResponse(authorized.canonical, response) }
        return response
    }

    private suspend fun dispatchInternal(authorized: AuthorizedDiagnosticCommand, timeoutMs: Long): AdapterResponse {
        val command = authorized.canonical
        val startTime = System.currentTimeMillis()
        // §5: a prior timeout that could not be resynchronised leaves the inbound
        // stream unverified — refuse to issue until (re)initialisation resets it.
        if (retired || resyncCompromised || !transport.isConnected) {
            return AdapterResponse.Error(
                AdapterError.Timeout(timeoutMs),
                "unverified stream after prior timeout; re-initialise adapter",
                System.currentTimeMillis() - startTime
            )
        }
        try {
            drainPendingBuffer()
            // Global per-adapter minimum spacing, including manual reads and scans.
            val waitMs = 50L - java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastDispatchNanos)
            if (waitMs > 0) delay(waitMs)
            if (retired || !transport.isConnected) return AdapterResponse.Error(AdapterError.TransportFault("Disconnected"), "", 0L)
            lastDispatchNanos = System.nanoTime()
            withTimeoutOrNull(timeoutMs.coerceIn(100L, maxCommandTimeoutMs.coerceIn(100L, 30_000L))) {
                transport.write((command + "\r").toByteArray(Charsets.US_ASCII))
                true
            } ?: run {
                retire("Write timeout; response ownership unknown")
                return AdapterResponse.Error(AdapterError.Timeout(timeoutMs), "", System.currentTimeMillis() - startTime)
            }
            val sb = StringBuilder()
            val receivedPrompt = withTimeoutOrNull(timeoutMs.coerceIn(1L, maxCommandTimeoutMs.coerceIn(100L, 30_000L))) {
                while (true) {
                    val c = incomingCharsChannel.receive()
                    sb.append(c)
                    if (sb.length > 16_384) error("Response exceeds bounded buffer")
                    if (c == Elm327BufferParser.PROMPT_CHAR) return@withTimeoutOrNull true
                }
                @Suppress("UNREACHABLE_CODE") false
            }
            val elapsed = System.currentTimeMillis() - startTime
            if (receivedPrompt == true) return Elm327BufferParser.parseBuffer(sb.toString(), command, elapsed)
            // Ordinary timeout retains the existing bounded late-prompt quarantine.
            resyncAfterTimeout(500L)
            if (resyncCompromised) retire("Timeout without a verified prompt boundary")
            return AdapterResponse.Error(AdapterError.Timeout(timeoutMs), sb.toString(), elapsed)
        } catch (e: CancellationException) {
            // No next command can reuse this stream, regardless of when late bytes arrive.
            retire("Command canceled; response ownership invalidated")
            throw e
        } catch (e: Exception) {
            retire("Transport/reader failure: ${e.message}")
            return AdapterResponse.Error(AdapterError.TransportFault(e.message ?: "Transport failure"), "", System.currentTimeMillis() - startTime)
        }
    }

    /**
     * §5 post-timeout quarantine: consume bytes still in flight from the timed-out
     * command until its terminating '>' prompt is observed, proving a clean prompt
     * boundary before the next command. Bounded by [resyncWindowMs]; if no prompt
     * arrives the stream cannot be resynchronised and is marked compromised.
     */
    private suspend fun resyncAfterTimeout(resyncWindowMs: Long) {
        val prompt = withTimeoutOrNull(resyncWindowMs) {
            while (true) {
                if (incomingCharsChannel.receive() == Elm327BufferParser.PROMPT_CHAR) return@withTimeoutOrNull true
            }
            @Suppress("UNREACHABLE_CODE") false
        }
        resyncCompromised = prompt != true
    }

    private fun drainPendingBuffer() {
        while (true) {
            val item = incomingCharsChannel.tryReceive().getOrNull() ?: break
        }
    }

    private fun evidence(stage: ConnectionTestStage, status: ConnectionTestStatus, raw: String) {
        onEvidence(ConnectionTestEvidence(stage, status, raw.take(4096)))
    }

    private suspend fun checkOk(command: String, timeoutMs: Long) {
        val r = executeInternal(command, timeoutMs)
        check(r is AdapterResponse.Success && r.cleanedLines.singleOrNull()?.trim()?.uppercase() == "OK") {
            "Adapter rejected $command"
        }
    }

    private fun getElmProtocolCommand(standard: ProtocolStandard): String {
        require(standard in setOf(ProtocolStandard.AUTO, ProtocolStandard.SAE_J1850_PWM, ProtocolStandard.SAE_J1850_VPW,
            ProtocolStandard.ISO_9141_2, ProtocolStandard.ISO_14230_4_KWP_5BAUD, ProtocolStandard.ISO_14230_4_KWP_FAST,
            ProtocolStandard.ISO_15765_4_CAN_11_500, ProtocolStandard.ISO_15765_4_CAN_29_500,
            ProtocolStandard.ISO_15765_4_CAN_11_250, ProtocolStandard.ISO_15765_4_CAN_29_250, ProtocolStandard.SAE_J1939_CAN)) {
            "Unsupported protocol selection"
        }
        return standard.elmCommand.replace(" ", "")
    }

    private fun mapElmProtocolNumber(code: String): ProtocolStandard? =
        com.sarani.carobd.core.model.protocolFromManualCode(code)
}
