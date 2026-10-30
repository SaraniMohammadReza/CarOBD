package com.sarani.carobd.data.repository

import android.bluetooth.BluetoothManager
import android.content.Context
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.command.toWireEnvelopes
import com.sarani.carobd.core.adapter.profile.ConnectionProfileRepository
import com.sarani.carobd.core.adapter.safety.CommandSafetyClassifier
import com.sarani.carobd.core.model.CommandSafetyVerdict
import com.sarani.carobd.core.model.CommandSafetyViolation
import com.sarani.carobd.core.model.ConnectionTestReport
import com.sarani.carobd.core.model.ConnectionTestEvidence
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.BenchPhase
import com.sarani.carobd.core.model.ConnectionTestStage
import com.sarani.carobd.core.model.ConnectionTestStatus
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DiagnosticResult
import com.sarani.carobd.core.model.DiagnosticService
import com.sarani.carobd.core.model.EcuRejectionException
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.NegativeResponse
import com.sarani.carobd.core.model.NotConnectedException
import com.sarani.carobd.core.model.ObdNrc
import com.sarani.carobd.core.model.ProtocolMode
import com.sarani.carobd.core.model.TelemetryPollMode
import com.sarani.carobd.core.model.protocolFromManualCode
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.storage.AppPreferencesManager
import com.sarani.carobd.core.telemetry.trip.TripComputer
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.data.transport.TransportFactory
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import com.sarani.carobd.data.session.DiagnosticSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CancellationException

/**
 * Enterprise-grade OBD Repository implementation.
 * Integrates ELM327 protocol engine, real hardware transports (Classic BT, BLE, Wi-Fi TCP, Isolated Simulator),
 * physical FuelEconomyCalculator, TripComputer, and SQLite database persistence.
 */
class ObdRepositoryImpl(
    private val context: Context?,
    private val activeVehicleRepository: ActiveVehicleRepository,
    appPreferencesManager: AppPreferencesManager?,
    private val connectionProfiles: ConnectionProfileRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()),
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    pollModeFlow: Flow<TelemetryPollMode> = requireNotNull(appPreferencesManager).telemetryPollMode,
    private val createTransport: (TransportConfig) -> com.sarani.carobd.core.adapter.transport.ObdTransport = {
        TransportFactory.createTransport(it, requireNotNull(context))
    },
    private val historySink: ((com.sarani.carobd.core.model.DtcHistorySession, List<DiagnosticCode>) -> Unit)? = null,
    private val recordings: com.sarani.carobd.data.recording.RecordingController? = null,
    private val admitTransport: (TransportConfig) -> Unit = { if (it is TransportConfig.BluetoothClassic || it is TransportConfig.Ble) com.sarani.carobd.core.permissions.AndroidAccess.requireTransport(requireNotNull(context), it) },
    private val admitBackground: () -> Unit = { context?.let { com.sarani.carobd.core.permissions.AndroidAccess.require(it, com.sarani.carobd.core.model.RuntimeAccess.Notifications) } },
    private val startService: (String) -> Unit = { com.sarani.carobd.service.ConnectionForegroundService.start(requireNotNull(context), it) },
    private val stopService: () -> Unit = { com.sarani.carobd.service.ConnectionForegroundService.stop(requireNotNull(context)) },
) : ObdRepository {

    val database by lazy { CarObdDatabaseHelper(requireNotNull(context)) }

    private val _activeConnectionProfile = MutableStateFlow<ConnectionProfile?>(null)
    override val activeConnectionProfile = _activeConnectionProfile.asStateFlow()
    private val _connectingProfile = MutableStateFlow<ConnectionProfile?>(null)
    override val connectingProfile = _connectingProfile.asStateFlow()
    private val _historyWarning = MutableStateFlow<String?>(null)
    override val historyWarning = _historyWarning.asStateFlow()
    private val _foregroundSessionId = MutableStateFlow<String?>(null)
    override val foregroundSessionId: StateFlow<String?> = _foregroundSessionId.asStateFlow()
    @Volatile private var serviceAcknowledged: String? = null
    override fun foregroundServiceReady(sessionId: String) { if (_foregroundSessionId.value == sessionId) serviceAcknowledged = sessionId }
    override fun foregroundServiceFailed(sessionId: String, reason: String) {
        scope.launch { sessionMutex.withLock {
            if (_foregroundSessionId.value == sessionId) {
                disconnectLocked()
                _connectionState.value = ConnectionState.Failure("FOREGROUND_SERVICE", "پایش پس‌زمینه متوقف شد", reason, true, issue = com.sarani.carobd.core.model.ConnectionIssue.ForegroundServiceUnavailable)
            }
        } }
    }
    @Volatile private var activeSession: DiagnosticSession? = null
    private val sessionMutex = Mutex()
    private val connectionAdmission = kotlinx.coroutines.sync.Semaphore(1)
    private val connecting = java.util.concurrent.atomic.AtomicReference<kotlinx.coroutines.Job?>(null)
    private var activeCommandTimeoutMs = 2500L
    private var currentTripComputer: TripComputer? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // §B3: single persisted source of truth — restored from ActiveVehicleRepository (DataStore).
    // Never a fabricated PRESET_VEHICLES.first() default; null until the user selects a vehicle.
    private val _activeVehicleProfile = MutableStateFlow<VehicleProfile?>(null)
    override val activeVehicleProfile: StateFlow<VehicleProfile?> = _activeVehicleProfile.asStateFlow()

    private val _liveTelemetry = MutableStateFlow(LiveTelemetry())
    override val liveTelemetry: StateFlow<LiveTelemetry> = _liveTelemetry.asStateFlow()

    /** §7: persisted polling cadence, observed by the producer each cycle. */
    internal val pollModeState: StateFlow<TelemetryPollMode> =
        pollModeFlow.stateIn(scope, SharingStarted.Eagerly, TelemetryPollMode.NORMAL)

    // Reader, producer and in-flight requests belong to activeSession, not repository scope.

    init {
        // §B3: restore the persisted active vehicle from the single source of truth on startup.
        scope.launch {
            activeVehicleRepository.activeVehicle.collect { profile ->
                _activeVehicleProfile.value = profile
            }
        }
    }

    override suspend fun connect(type: ConnectionType, target: String): Result<Unit> = try {
        connect(profileFor(type, target))
    } catch (e: CancellationException) { throw e }
      catch (e: Exception) { Result.failure(e) }

    /** §11: build a [ConnectionProfile] from the legacy (type, target) pair so the
     *  legacy path is driven by the same factory as the profile path. */
    private fun profileFor(type: ConnectionType, target: String): ConnectionProfile = when (type) {
        ConnectionType.VIRTUAL_SIMULATOR ->
            ConnectionProfile(id = "sim", name = "Simulator", transport = TransportConfig.Simulator())
        ConnectionType.BLUETOOTH_CLASSIC ->
            ConnectionProfile(id = "bt", name = target, transport = TransportConfig.BluetoothClassic(target))
        ConnectionType.BLUETOOTH_LE ->
            ConnectionProfile(id = "ble", name = target, transport = TransportConfig.Ble(target))
        ConnectionType.WIFI_TCP -> {
            val parts = target.split(":")
            require(parts.size == 2) { "Explicit Wi-Fi host:port required" }
            val host = parts[0]
            val port = parts[1].toIntOrNull() ?: error("Explicit valid Wi-Fi port required")
            ConnectionProfile(id = "wifi", name = "$host:$port", transport = TransportConfig.Wifi(host, port))
        }
        ConnectionType.USB_SERIAL ->
            ConnectionProfile(id = "usb", name = "USB", transport = TransportConfig.Usb())
    }

    override suspend fun connect(profile: ConnectionProfile): Result<Unit> = openConnection(profile)

    override suspend fun testConnection(profile: ConnectionProfile, onProgress: (ConnectionTestReport) -> Unit): ConnectionTestReport {
        var report = ConnectionTestReport(profile.transport.type == TransportType.SIMULATOR)
        onProgress(report)
        try {
            val result = openConnection(profile, testOnly = true) { event ->
                report = report.record(event)
                onProgress(report)
            }
            if (result.isFailure) report = report.finishFailure(result.exceptionOrNull()?.message ?: "Connection failed")
        } catch (e: CancellationException) {
            onProgress(report.finishFailure("Canceled; session closed"))
            throw e
        }
        onProgress(report)
        return report
    }

    private suspend fun openConnection(profile: ConnectionProfile, testOnly: Boolean = false, onEvidence: (ConnectionTestEvidence) -> Unit = {}): Result<Unit> = withContext(ioDispatcher) {
        if (!connectionAdmission.tryAcquire()) return@withContext Result.failure(IllegalStateException("Connection already in progress"))
        val connectionJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
        connecting.set(connectionJob)
        try {
            sessionMutex.withLock {
                disconnectLocked()
                var created: DiagnosticSession? = null
                _connectingProfile.value = profile
                try {
                    require(profile.validate().isEmpty()) { "Invalid connection profile: ${profile.validate()}" }
                    val forced = when (profile.protocolMode) {
                        ProtocolMode.AUTO -> null
                        ProtocolMode.MANUAL -> requireNotNull(profile.manualProtocolCode?.let { protocolFromManualCode(it) }) { "Unsupported manual protocol" }
                        ProtocolMode.VEHICLE_PROFILE -> error("Vehicle-profile protocol initialization is disabled pending validation; choose AUTO or MANUAL")
                    }
                    recordings?.session(profile)
                    recordings?.bench(BenchPhase.PermissionSnapshot, "AdmissionPending")
                    admitTransport(profile.transport)
                    if (!testOnly) admitBackground()
                    recordings?.bench(BenchPhase.PermissionSnapshot, if (testOnly) "TransportAdmissionPassed;ForegroundNotRequiredForTest" else "TransportAndForegroundAdmissionPassed")
                    val rawTransport = createTransport(profile.transport)
                    val transport = if (recordings != null) com.sarani.carobd.core.adapter.transport.RecordingTransport(rawTransport, { recordings.builder }, recordings::failure) else rawTransport
                    check(transport.transportType == profile.transport.type) { "Transport identity mismatch" }
                    lateinit var session: DiagnosticSession
                    session = DiagnosticSession(transport, scope,
                        onState = { state -> created?.let { if (activeSession === it) {
                            _connectionState.value = state
                            if (state is ConnectionState.Failure) recordings?.bench(BenchPhase.TransportLost, state.issue?.name ?: "Unknown")
                        } } },
                        onTelemetry = { sample -> created?.let { if (activeSession === it && it.isOpen) _liveTelemetry.value = sample } },
                        onLoss = {
                            val lost = created
                            scope.launch { sessionMutex.withLock { if (lost != null && activeSession === lost) {
                                val failure = _connectionState.value as? ConnectionState.Failure
                                disconnectLocked()
                                if (failure != null) _connectionState.value = failure
                            } } }
                        }, connectTimeoutMs = profile.connectTimeoutMs,
                        commandTimeoutMs = profile.commandTimeoutMs, onEvidence = { event ->
                            recordings?.bench(when (event.stage) {
                                ConnectionTestStage.TRANSPORT -> if (event.status == ConnectionTestStatus.PASS) BenchPhase.TransportConnected else BenchPhase.TransportConnecting
                                ConnectionTestStage.ADAPTER, ConnectionTestStage.IDENTITY -> if (event.status == ConnectionTestStatus.PASS) BenchPhase.AdapterResponding else BenchPhase.AdapterInitializing
                                ConnectionTestStage.PROTOCOL -> if (event.status == ConnectionTestStatus.PASS) BenchPhase.ProtocolObserved else BenchPhase.ProtocolSelecting
                                ConnectionTestStage.ECU -> if (event.status == ConnectionTestStatus.PASS) BenchPhase.EcuResponding else BenchPhase.EcuHandshake
                                ConnectionTestStage.CAPABILITIES -> BenchPhase.CapabilitiesDiscovered
                            }, event.status.name)
                            onEvidence(event)
                        }, onResponse = { command, response -> recordings?.parsed(command, response) })
                    activeCommandTimeoutMs = profile.commandTimeoutMs
                    created = session
                    activeSession = session
                    val result = session.execute { it.initialize(forced) }
                    if (!result.isSuccess) throw com.sarani.carobd.core.model.ConnectionIssueException(result.failureIssue ?: com.sarani.carobd.core.model.ConnectionIssue.Unknown, result.errorMessage ?: "Initialization failed")
                    connectionProfiles.save(profile.copy(lastSuccessfulAt = System.currentTimeMillis()))
                    if (testOnly) {
                        disconnectLocked()
                    } else {
                        _activeConnectionProfile.value = profile
                        currentTripComputer = TripComputer(UUID.randomUUID().toString(), _activeVehicleProfile.value?.id ?: "unselected", simulator = profile.transport.type == TransportType.SIMULATOR)
                        (session.adapter.state.value as? ConnectionState.Connected)?.let { recordings?.identity(it) }
                        _connectionState.value = session.adapter.state.value
                        startTelemetryLoop()
                        val serviceId = UUID.randomUUID().toString()
                        _foregroundSessionId.value = serviceId
                        serviceAcknowledged = null
                        try { startService(serviceId) } catch (_: Exception) { error("Foreground diagnostics unavailable; session stopped") }
                        scope.launch {
                            kotlinx.coroutines.delay(5000)
                            if (_foregroundSessionId.value == serviceId && serviceAcknowledged != serviceId)
                                foregroundServiceFailed(serviceId, "Foreground service did not acknowledge startup")
                        }
                    }
                    Result.success(Unit)
                } catch (e: CancellationException) {
                    recordings?.bench(BenchPhase.ConnectionFailed, "Canceled")
                    recordings?.disconnected()
                    _activeConnectionProfile.value = null
                    _foregroundSessionId.value = null
                    if (activeSession === created) activeSession = null
                    created?.close()
                    _liveTelemetry.value = LiveTelemetry()
                    currentTripComputer = null
                    runCatching { stopService() }
                    _connectionState.value = ConnectionState.Disconnected
                    throw e
                } catch (e: Exception) {
                    if (e is com.sarani.carobd.core.model.AccessDeniedException)
                        recordings?.bench(BenchPhase.PermissionDenied, "${e.access}:${e.state}")
                    recordings?.bench(BenchPhase.ConnectionFailed, com.sarani.carobd.core.model.connectionIssue(e).name)
                    recordings?.disconnected()
                    _activeConnectionProfile.value = null
                    _foregroundSessionId.value = null
                    if (activeSession === created) activeSession = null
                    created?.close()
                    _liveTelemetry.value = LiveTelemetry()
                    currentTripComputer = null
                    runCatching { stopService() }
                    _connectionState.value = ConnectionState.Failure("CONNECTION", "اتصال برقرار نشد", e.message.orEmpty(), true, issue = com.sarani.carobd.core.model.connectionIssue(e))
                    Result.failure(e)
                }
            }
        } finally {
            _connectingProfile.value = null
            connecting.compareAndSet(connectionJob, null)
            connectionAdmission.release()
        }
    }

    override suspend fun disconnect() {
        recordings?.bench(BenchPhase.DisconnectRequested)
        connecting.get()?.cancel(CancellationException("User disconnected"))
        val captured = activeSession
        captured?.invalidate() // Stop dispatch before waiting for connection serialization.
        captured?.close() // Release a blocking physical connect/write before acquiring the mutex.
        withContext(ioDispatcher + kotlinx.coroutines.NonCancellable) {
            sessionMutex.withLock { disconnectLocked() }
        }
    }

    private suspend fun disconnectLocked() {
        _activeConnectionProfile.value = null
        _connectingProfile.value = null
        _foregroundSessionId.value = null
        serviceAcknowledged = null
        val old = activeSession
        activeSession = null // Generation guard closes BEFORE cancellation/join.
        old?.close()
        recordings?.disconnected()
        currentTripComputer?.getTripRecord()?.let { record ->
            if ((record.distanceKm ?: 0.0) > 0.05 || record.durationSeconds > 30) runCatching { database.insertTripRecord(record) }.onFailure { _historyWarning.value = "Trip evidence could not be saved: ${it.javaClass.simpleName}" }
        }
        currentTripComputer = null
        _connectionState.value = ConnectionState.Disconnected
        _liveTelemetry.value = LiveTelemetry()
        try { stopService() } catch (_: Exception) {}
    }

    override suspend fun setVehicleProfile(profile: VehicleProfile) {
        _activeVehicleProfile.value = profile
        activeVehicleRepository.selectVehicle(profile)
        // Changing vehicle metadata never changes live routing. A new verified
        // connection is required for any protocol configuration.
    }

    override suspend fun diagnosticHistory(): Result<List<com.sarani.carobd.core.model.DtcHistorySession>> = withContext(ioDispatcher) {
        runCatching { database.diagnosticHistory() }
    }

    override suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>> = withContext(ioDispatcher) {
        val session = activeSession?.takeIf { it.isOpen } ?: return@withContext Result.failure(NotConnectedException("Adapter not connected"))
        val report = sessionResult(session) { adapter ->
            com.sarani.carobd.core.adapter.command.DiagnosticScanRunner(adapter) {
                activeSession === session && session.isOpen && _connectionState.value is ConnectionState.Connected
            }.scan()
        }.getOrElse { return@withContext Result.failure(it) }
        if (activeSession !== session || !session.isOpen) return@withContext Result.failure(NotConnectedException("Session changed"))
        if (report.completeness != com.sarani.carobd.core.model.ScanCompleteness.COMPLETE) {
            return@withContext Result.failure(com.sarani.carobd.core.model.DiagnosticScanException(report))
        }
        val observedAt = System.currentTimeMillis()
        val protocol = (_connectionState.value as? ConnectionState.Connected)?.protocolName
        val history = com.sarani.carobd.core.model.DtcHistorySession(UUID.randomUUID().toString(), _foregroundSessionId.value,
            _activeVehicleProfile.value?.id, observedAt, protocol, null,
            report.codes.map { com.sarani.carobd.core.model.DtcHistoryCode.capture(it, protocol, null, observedAt) })
        _historyWarning.value = null
        runCatching {
            if (historySink != null) historySink.invoke(history, report.codes)
            else if (context != null) database.saveDtcScan(history, report.codes)
        }.onFailure { _historyWarning.value = "Scan completed, but history could not be saved: ${it.message}" }
        Result.success(report.codes)
    }

    override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = withContext(ioDispatcher) {
        val session = activeSession?.takeIf { it.isOpen } ?: return@withContext Result.failure(Exception("Adapter not connected"))

        if (_connectionState.value !is ConnectionState.Connected ||
            (_connectionState.value as? ConnectionState.Connected)?.isSimulator != true) {
            return@withContext Result.failure(IllegalStateException("Physical DTC clearing is disabled by read-only safety policy"))
        }
        val response = sessionResult(session) { it.clearSimulatorDiagnosticState() }.getOrElse { return@withContext Result.failure(it) }
        // §7: canonical envelope path — classify through ElmResponseEnvelopeParser
        if (activeSession !== session || !session.isOpen) return@withContext Result.failure(NotConnectedException("Session changed"))
        val envelopes = response.toWireEnvelopes("04")

        // §8.4: a real 0x44 positive echo is required for success.
        val positiveClear = envelopes.any {
            it.result is DiagnosticResult.Positive && it.service == DiagnosticService.MODE_04
        }

        // §13 §4.4: a 7F on a clear is an explicit ECU rejection — never success.
        val rejection = envelopes.firstOrNull { it.result is DiagnosticResult.NegativeResponse }

        return@withContext when {
            positiveClear && envelopes.all { it.result is DiagnosticResult.Positive && it.service == DiagnosticService.MODE_04 } -> Result.success(Unit)
            rejection != null -> {
                val r = rejection.result as DiagnosticResult.NegativeResponse
                val nrcCode = r.nrc ?: 0
                Result.failure(
                    EcuRejectionException(
                        NegativeResponse(
                            requestedService = 0x04,
                            nrc = ObdNrc.fromCode(nrcCode),
                            nrcCode = nrcCode,
                            rawLine = rejection.rawLines.joinToString(" "),
                            responder = rejection.responderId
                        )
                    )
                )
            }
            else -> Result.failure(Exception("ECU did not acknowledge DTC clear request (no 0x44 positive response)"))
        }
    }

    override suspend fun readEvidence(command: String, expectedSessionId: String?): Result<List<com.sarani.carobd.core.model.DiagnosticWireResponse>> = withContext(ioDispatcher) {
        if (com.sarani.carobd.core.adapter.safety.AuthorizedDiagnosticCommand.readOnly(command) == null)
            return@withContext Result.failure(IllegalArgumentException("Read-only command required"))
        val session = activeSession?.takeIf { it.isOpen } ?: return@withContext Result.failure(NotConnectedException("Disconnected"))
        if (expectedSessionId != null && _foregroundSessionId.value != expectedSessionId)
            return@withContext Result.failure(NotConnectedException("Session changed"))
        val response = sessionResult(session) { it.executeCommand(command, activeCommandTimeoutMs) }
            .getOrElse { return@withContext Result.failure(it) }
        if (activeSession !== session || !session.isOpen) return@withContext Result.failure(NotConnectedException("Session changed"))
        Result.success(response.toWireEnvelopes(command))
    }

    override suspend fun sendRawCommand(command: String): Result<String> = withContext(ioDispatcher) {
        val session = activeSession?.takeIf { it.isOpen } ?: return@withContext Result.failure(NotConnectedException("Adapter not connected"))
        // Repository feedback mirrors the mandatory adapter authorization boundary.
        // Mode 04 (clear) must use the dedicated confirmed Diagnostics flow; write /
        // program / security-access services (08, 27, 2E, 2F, 31, 34, 35, 36, 37) and
        // unknown commands are refused before reaching the adapter.
        val verdict = CommandSafetyClassifier().classify(command)
        if (verdict != CommandSafetyVerdict.ALLOWED_READ_ONLY &&
            verdict != CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION) {
            return@withContext Result.failure(
                CommandSafetyViolation(
                    verdict = verdict,
                    rawCommand = command,
                    reasonFa = when (verdict) {
                        CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW ->
                            "دستور واضح‌سازی (Mode 04) فقط از طریق صفحهٔ Diagnostics قابل انجام است"
                        CommandSafetyVerdict.BLOCKED_UNSAFE ->
                            "این دستور نوشتاری/برنامه‌ریزی/دسترسی امنیتی است؛ از ترمینال مسدود شده"
                        else -> "دستور ناشناخته در ترمینال مسدود شده"
                    },
                    reasonEn = when (verdict) {
                        CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW ->
                            "Physical DTC clearing is disabled; simulator clearing requires the dedicated flow"
                        CommandSafetyVerdict.BLOCKED_UNSAFE ->
                            "Write/program/security-access command blocked in raw terminal"
                        else -> "Unknown command blocked in raw terminal"
                    }
                )
            )
        }
        val resp = sessionResult(session) { it.executeCommand(command, activeCommandTimeoutMs) }.getOrElse { return@withContext Result.failure(it) }
        if (activeSession !== session || !session.isOpen) return@withContext Result.failure(NotConnectedException("Session changed"))
        when (resp) {
            is AdapterResponse.Success -> Result.success(resp.rawResponse)
            is AdapterResponse.Error -> Result.failure(Exception(resp.error.messageFa))
        }
    }

    private suspend fun <T> sessionResult(session: DiagnosticSession, block: suspend (com.sarani.carobd.core.adapter.ObdAdapter) -> T): Result<T> =
        try { Result.success(session.execute(block)) }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { Result.failure(e) }

    override fun startTelemetryLoop(): Flow<LiveTelemetry> {
        activeSession?.startTelemetry(pollModeState, currentTripComputer)
        return liveTelemetry
    }

    override fun stopTelemetryLoop() { activeSession?.stopTelemetry(); _liveTelemetry.value = LiveTelemetry() }
}
