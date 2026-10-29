package com.sarani.carobd.data.repository

import android.bluetooth.BluetoothManager
import android.content.Context
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.command.toWireEnvelopes
import com.sarani.carobd.core.adapter.profile.ConnectionProfileRepository
import com.sarani.carobd.core.adapter.safety.CommandSafetyClassifier
import com.sarani.carobd.core.model.CommandSafetyVerdict
import com.sarani.carobd.core.model.CommandSafetyViolation
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ConnectionState
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
    pollModeFlow: Flow<TelemetryPollMode> = requireNotNull(appPreferencesManager).telemetryPollMode,
    private val createTransport: (TransportConfig) -> com.sarani.carobd.core.adapter.transport.ObdTransport = {
        TransportFactory.createTransport(it, requireNotNull(context))
    },
    private val startService: () -> Unit = { com.sarani.carobd.service.ConnectionForegroundService.start(requireNotNull(context)) },
    private val stopService: () -> Unit = { com.sarani.carobd.service.ConnectionForegroundService.stop(requireNotNull(context)) },
) : ObdRepository {

    val database by lazy { CarObdDatabaseHelper(requireNotNull(context)) }

    @Volatile private var activeSession: DiagnosticSession? = null
    private val sessionMutex = Mutex()
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
    private val pollModeState: StateFlow<TelemetryPollMode> =
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

    override suspend fun connect(type: ConnectionType, target: String): Result<Unit> =
        connect(profileFor(type, target))

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
            val host = parts.getOrNull(0) ?: "192.168.0.10"
            val port = parts.getOrNull(1)?.toIntOrNull() ?: 35000
            ConnectionProfile(id = "wifi", name = "$host:$port", transport = TransportConfig.Wifi(host, port))
        }
        ConnectionType.USB_SERIAL ->
            ConnectionProfile(id = "usb", name = "USB", transport = TransportConfig.Usb())
    }

    override suspend fun connect(profile: ConnectionProfile): Result<Unit> = withContext(Dispatchers.IO) {
        sessionMutex.withLock {
            disconnectLocked()
            var created: DiagnosticSession? = null
            try {
                require(profile.validate().isEmpty()) { "Invalid connection profile: ${profile.validate()}" }
                val forced = when (profile.protocolMode) {
                    ProtocolMode.AUTO -> null
                    ProtocolMode.MANUAL -> requireNotNull(profile.manualProtocolCode?.let { protocolFromManualCode(it) }) { "Unsupported manual protocol" }
                    ProtocolMode.VEHICLE_PROFILE -> error("Vehicle-profile protocol initialization is disabled pending validation; choose AUTO or MANUAL")
                }
                val tp = profile.transport.type
                if (tp == TransportType.BLUETOOTH_CLASSIC || tp == TransportType.BLUETOOTH_LE) {
                    val bt = (requireNotNull(context).getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                    require(bt != null && bt.isEnabled) { "Bluetooth disabled or unavailable" }
                }
                val transport = createTransport(profile.transport)
                check(transport.transportType == profile.transport.type) { "Transport identity mismatch" }
                lateinit var session: DiagnosticSession
                session = DiagnosticSession(transport, scope,
                    onState = { state -> created?.let { if (activeSession === it) _connectionState.value = state } },
                    onTelemetry = { sample -> created?.let { if (activeSession === it && it.isOpen) _liveTelemetry.value = sample } },
                    onLoss = {
                        val lost = created
                        scope.launch { sessionMutex.withLock { if (lost != null && activeSession === lost) disconnectLocked() } }
                    })
                created = session
                activeSession = session
                val result = session.execute { it.initialize(forced) }
                check(result.isSuccess) { result.errorMessage ?: "Initialization failed" }
                currentTripComputer = TripComputer(UUID.randomUUID().toString(), _activeVehicleProfile.value?.id ?: "unselected")
                _connectionState.value = session.adapter.state.value
                startTelemetryLoop()
                try { startService() } catch (_: Exception) {
                    error("Foreground diagnostics unavailable; session stopped")
                }
                connectionProfiles.save(profile.copy(lastSuccessfulAt = System.currentTimeMillis()))
                Result.success(Unit)
            } catch (e: CancellationException) {
                if (activeSession === created) activeSession = null
                created?.close()
                _connectionState.value = ConnectionState.Disconnected
                throw e
            } catch (e: Exception) {
                if (activeSession === created) activeSession = null
                created?.close()
                _liveTelemetry.value = LiveTelemetry()
                _connectionState.value = ConnectionState.Failure("CONNECTION", "اتصال برقرار نشد", e.message.orEmpty(), true)
                Result.failure(e)
            }
        }
    }

    override suspend fun disconnect() {
        val captured = activeSession
        captured?.invalidate() // Stop dispatch before waiting for connection serialization.
        captured?.close() // Release a blocking physical connect/write before acquiring the mutex.
        withContext(Dispatchers.IO) {
            sessionMutex.withLock { disconnectLocked() }
        }
    }

    private suspend fun disconnectLocked() {
        val old = activeSession
        activeSession = null // Generation guard closes BEFORE cancellation/join.
        old?.close()
        currentTripComputer?.getTripRecord()?.let { record ->
            if (record.distanceKm > 0.05 || record.durationSeconds > 30) runCatching { database.insertTripRecord(record) }
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

    override suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>> = withContext(Dispatchers.IO) {
        val session = activeSession?.takeIf { it.isOpen } ?: return@withContext Result.failure(NotConnectedException("Adapter not connected"))
        val report = session.execute { adapter ->
            com.sarani.carobd.core.adapter.command.DiagnosticScanRunner(adapter) {
                activeSession === session && session.isOpen && _connectionState.value is ConnectionState.Connected
            }.scan()
        }
        if (activeSession !== session || !session.isOpen) return@withContext Result.failure(NotConnectedException("Session changed"))
        if (report.completeness != com.sarani.carobd.core.model.ScanCompleteness.COMPLETE) {
            return@withContext Result.failure(com.sarani.carobd.core.model.DiagnosticScanException(report))
        }
        // Persistence must not invent MIL/category/protocol facts. Durable scan storage
        // remains off until the existing database schema can retain those facts.
        Result.success(report.codes)
    }

    override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = withContext(Dispatchers.IO) {
        val session = activeSession?.takeIf { it.isOpen } ?: return@withContext Result.failure(Exception("Adapter not connected"))

        if (_connectionState.value !is ConnectionState.Connected ||
            (_connectionState.value as? ConnectionState.Connected)?.isSimulator != true) {
            return@withContext Result.failure(IllegalStateException("Physical DTC clearing is disabled by read-only safety policy"))
        }
        val response = session.execute { it.clearSimulatorDiagnosticState() }
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

    override suspend fun sendRawCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
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
        val resp = session.execute { it.executeCommand(command, 2500L) }
        if (activeSession !== session || !session.isOpen) return@withContext Result.failure(NotConnectedException("Session changed"))
        when (resp) {
            is AdapterResponse.Success -> Result.success(resp.rawResponse)
            is AdapterResponse.Error -> Result.failure(Exception(resp.error.messageFa))
        }
    }

    override fun startTelemetryLoop(): Flow<LiveTelemetry> {
        activeSession?.startTelemetry(pollModeState, currentTripComputer)
        return liveTelemetry
    }

    override fun stopTelemetryLoop() { activeSession?.stopTelemetry(); _liveTelemetry.value = LiveTelemetry() }
}
