package com.sarani.carobd.data.repository

import android.bluetooth.BluetoothManager
import android.content.Context
import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.command.DtcScanValidator
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.safety.CommandSafetyClassifier
import com.sarani.carobd.core.model.CommandSafetyVerdict
import com.sarani.carobd.core.model.CommandSafetyViolation
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.EcuRejectionException
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.PidCapabilityMap
import com.sarani.carobd.core.model.NotConnectedException
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.protocol.dtc.DtcLookupEngine
import com.sarani.carobd.core.protocol.dtc.DtcParser
import com.sarani.carobd.core.protocol.dtc.DtcFrame
import com.sarani.carobd.core.protocol.obd2.PidCodec
import com.sarani.carobd.core.protocol.obd2.PidPollingPlan
import com.sarani.carobd.core.telemetry.fuel.FuelEconomyCalculator
import com.sarani.carobd.core.telemetry.trip.TripComputer
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.data.transport.BleTransport
import com.sarani.carobd.data.transport.BluetoothClassicTransport
import com.sarani.carobd.data.transport.IsolatedSimulatorTransport
import com.sarani.carobd.data.transport.WifiTcpTransport
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Enterprise-grade OBD Repository implementation.
 * Integrates ELM327 protocol engine, real hardware transports (Classic BT, BLE, Wi-Fi TCP, Isolated Simulator),
 * physical FuelEconomyCalculator, TripComputer, and SQLite database persistence.
 */
class ObdRepositoryImpl(
    private val context: Context,
    private val activeVehicleRepository: ActiveVehicleRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) : ObdRepository {

    val database = CarObdDatabaseHelper(context)

    private var activeAdapter: ObdAdapter? = null
    private var currentTripComputer: TripComputer? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // §B3: single persisted source of truth — restored from ActiveVehicleRepository (DataStore).
    // Never a fabricated PRESET_VEHICLES.first() default; null until the user selects a vehicle.
    private val _activeVehicleProfile = MutableStateFlow<VehicleProfile?>(null)
    override val activeVehicleProfile: StateFlow<VehicleProfile?> = _activeVehicleProfile.asStateFlow()

    private val _liveTelemetry = MutableStateFlow(LiveTelemetry())
    override val liveTelemetry: StateFlow<LiveTelemetry> = _liveTelemetry.asStateFlow()

    private var pollingJob: Job? = null
    private var relayerJob: Job? = null

    init {
        // §B3: restore the persisted active vehicle from the single source of truth on startup.
        scope.launch {
            activeVehicleRepository.activeVehicle.collect { profile ->
                _activeVehicleProfile.value = profile
            }
        }
    }

    override suspend fun connect(type: ConnectionType, target: String): Result<Unit> = withContext(Dispatchers.IO) {
        stopTelemetryLoop()
        disconnect()

        // §27: Bluetooth OFF must be surfaced truthfully — never allowed to look connected.
        if (type == ConnectionType.BLUETOOTH_CLASSIC || type == ConnectionType.BLUETOOTH_LE) {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val btAdapter = btManager?.adapter
            if (btAdapter == null || !btAdapter.isEnabled) {
                _connectionState.value = ConnectionState.Failure(
                    stage = "BLUETOOTH",
                    humanMessageFa = "بلوتوث خاموش است",
                    technicalMessage = "BluetoothAdapter disabled or unavailable",
                    isRecoverable = true
                )
                return@withContext Result.failure(Exception("بلوتوث خاموش است"))
            }
        }

        val transport = when (type) {
            ConnectionType.VIRTUAL_SIMULATOR -> {
                IsolatedSimulatorTransport()
            }
            ConnectionType.BLUETOOTH_CLASSIC -> {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                BluetoothClassicTransport(target, btManager?.adapter)
            }
            ConnectionType.BLUETOOTH_LE -> {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                BleTransport(context, target, btManager?.adapter)
            }
            ConnectionType.WIFI_TCP -> {
                val parts = target.split(":")
                val host = parts.getOrNull(0) ?: "192.168.0.10"
                val port = parts.getOrNull(1)?.toIntOrNull() ?: 35000
                WifiTcpTransport(host, port)
            }
            ConnectionType.USB_SERIAL -> {
                com.sarani.carobd.data.transport.UsbSerialTransport(context)
            }
        }

        val elm = Elm327Adapter(transport, scope)
        activeAdapter = elm

        // §26: single source of truth — relay the adapter's ECU-verified StateFlow instead of
        // synthesizing connection state. Elm327Adapter.initialize() requires a valid Mode 01
        // PID 00 response before emitting Connected, so this can never report "connected"
        // while the ECU is silent.
        relayerJob = scope.launch {
            elm.state.collect { _connectionState.value = it }
        }

        val initResult = elm.initialize()
        if (initResult.isSuccess) {
            // Start Trip tracking
            currentTripComputer = TripComputer(
                tripId = UUID.randomUUID().toString(),
                vehicleId = _activeVehicleProfile.value?.id ?: "unselected"
            )

            startTelemetryLoop()
            try {
                com.sarani.carobd.service.ConnectionForegroundService.start(context)
            } catch (_: Exception) {}

            Result.success(Unit)
        } else {
            val err = initResult.errorMessage ?: "خطا در اتصال به خودرو"
            Result.failure(Exception(err))
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            com.sarani.carobd.service.ConnectionForegroundService.stop(context)
        } catch (_: Exception) {}

        stopTelemetryLoop()

        // Finalize active trip if any
        currentTripComputer?.let { tc ->
            val record = tc.getTripRecord()
            if (record.distanceKm > 0.05 || record.durationSeconds > 30) {
                database.insertTripRecord(record)
            }
        }
        currentTripComputer = null

        relayerJob?.cancel()
        relayerJob = null
        activeAdapter?.disconnect()
        activeAdapter = null
        _connectionState.value = ConnectionState.Disconnected
        _liveTelemetry.value = LiveTelemetry()
    }

    override suspend fun setVehicleProfile(profile: VehicleProfile) {
        _activeVehicleProfile.value = profile
        activeVehicleRepository.selectVehicle(profile)
        // If profile defines custom target ECU header, apply it to the active adapter
        val adapter = activeAdapter
        if (adapter != null) {
            val headerCmd = profile.ecuProtocol.initSequence.firstOrNull { it.startsWith("AT SH", ignoreCase = true) }
            if (headerCmd != null) {
                val hex = headerCmd.removePrefix("AT SH").removePrefix("ATSH").trim()
                adapter.setHeader(hex)
            }
        }
    }

    override suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(NotConnectedException("آداپتور متصل نیست"))

        val dtcList = mutableListOf<DiagnosticCode>()
        // §2 protocol-aware framing: CAN carries the J1979 count byte after the echo;
        // non-CAN classic (J1850/ISO-9141/KWP) emits DTC pairs directly (no count byte).
        val dtcFrame = if (adapter.isCanTransport) DtcFrame.CAN_WITH_COUNT else DtcFrame.NON_CAN_PAIRS

        // 1. Request Mode 03 (Stored DTCs).
        val mode03Resp = adapter.executeCommand("03", 4000L)
        // §28 / §4.4 / §4.3 truth gate: Mode 03 is the canonical "ECU is answering"
        // signal. An unreachable ECU surfaces as NotConnected; a 7F rejection
        // surfaces as EcuRejectionException. Neither may collapse into an empty
        // "no codes found" success.
        DtcScanValidator.scanFailure(mode03Resp)?.let { return@withContext Result.failure(it) }
        if (mode03Resp is AdapterResponse.Success) {
            // §4.3: parseMode03Payload validates the 0x43 echo + count byte explicitly;
            // identity (category/responder/ecuSource) is preserved via addAll.
            dtcList.addAll(
                DtcParser.parseMode03Payload(
                    PidCodec.cleanResponseHex(mode03Resp.rawResponse), DtcCategory.STORED, frame = dtcFrame
                )
            )
        }

        // 2. Request Mode 07 (Pending DTCs). A 7F here is an explicit ECU rejection
        //    (this responder does not answer pending-DTC requests) and MUST surface
        //    as a failure — never silently become an empty "no pending codes" success.
        val mode07Resp = adapter.executeCommand("07", 4000L)
        if (mode07Resp is AdapterResponse.Success) {
            val clean7 = PidCodec.cleanResponseHex(mode07Resp.rawResponse)
            if (PidCodec.isNegativeResponse(clean7)) {
                PidCodec.parseNegativeResponse(clean7, rawLine = mode07Resp.rawResponse)
                    ?.let { return@withContext Result.failure(EcuRejectionException(it)) }
            } else {
                dtcList.addAll(
                    DtcParser.parseMode03Payload(clean7, DtcCategory.PENDING, frame = dtcFrame)
                )
            }
        }

        // 3. Request Mode 0A (Permanent DTCs). Same 7F-rejection semantics as Mode 07.
        val mode0aResp = adapter.executeCommand("0A", 4000L)
        if (mode0aResp is AdapterResponse.Success) {
            val cleanA = PidCodec.cleanResponseHex(mode0aResp.rawResponse)
            if (PidCodec.isNegativeResponse(cleanA)) {
                PidCodec.parseNegativeResponse(cleanA, rawLine = mode0aResp.rawResponse)
                    ?.let { return@withContext Result.failure(EcuRejectionException(it)) }
            } else {
                dtcList.addAll(
                    DtcParser.parseMode03Payload(cleanA, DtcCategory.PERMANENT, frame = dtcFrame)
                )
            }
        }

        // Save scan session to database
        if (dtcList.isNotEmpty()) {
            database.saveDtcScan(
                sessionId = UUID.randomUUID().toString(),
                vehicleId = _activeVehicleProfile.value?.id ?: "unselected",
                codes = dtcList
            )
        }

        Result.success(dtcList)
    }

    override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(Exception("Adapter not connected"))

        val response = adapter.executeCommand("04", 5000L)
        val cleanHex = response.let {
            if (it is AdapterResponse.Success) PidCodec.cleanResponseHex(it.rawResponse) else byteArrayOf()
        }
        return@withContext when {
            // §8.4 / §13: a real 0x44 positive echo is required for success.
            response is AdapterResponse.Success && PidCodec.verifyClearDtcResponse(cleanHex)
                -> Result.success(Unit)
            // §13/#3 & §4.4: a 7F on a clear is an explicit ECU rejection — never success.
            response is AdapterResponse.Success && PidCodec.isNegativeResponse(cleanHex) -> {
                val neg = PidCodec.parseNegativeResponse(cleanHex, rawLine = response.rawResponse)
                Result.failure(
                    neg?.let { EcuRejectionException(it) }
                        ?: Exception("ECU sent 7F rejection to DTC clear")
                )
            }
            else -> Result.failure(Exception("ECU did not acknowledge DTC clear request (no 0x44 positive response)"))
        }
    }

    override suspend fun sendRawCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(NotConnectedException("آداپتور متصل نیست"))
        // §4 P0: the raw terminal is NOT a safety boundary — the repository is.
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
                            "Clear-DTC (Mode 04) is only available via the Diagnostics clear flow"
                        CommandSafetyVerdict.BLOCKED_UNSAFE ->
                            "Write/program/security-access command blocked in raw terminal"
                        else -> "Unknown command blocked in raw terminal"
                    }
                )
            )
        }
        val resp = adapter.executeCommand(command, 2500L)
        when (resp) {
            is AdapterResponse.Success -> Result.success(resp.rawResponse)
            is AdapterResponse.Error -> Result.failure(Exception(resp.error.messageFa))
        }
    }

    override fun startTelemetryLoop(): Flow<LiveTelemetry> {
        pollingJob?.cancel()

        val flowStream = flow {
            var current = _liveTelemetry.value
            // §8 TRUTHFULNESS: vehicle metadata (engine displacement, volumetric
            // efficiency) is NOT carried on LiveTelemetry / VehicleProfile, so we
            // do NOT fabricate a TU5/1.6/1.587L default or assume VE=0.80. Fuel
            // economy is computed ONLY from a measured MAF reading (PID 10); when
            // MAF is absent the rate is unavailable and the banner renders "—".
            var cycleCount = 0
            // §9 / §22: probe the ECU's supported-PID bitmap once per session
            // (Mode 01 PID 00/20/40/60/80/A0/C0/E0). Discovery failure degrades to
            // the full baseline plan, so an unreadable bus never reduces coverage.
            var capabilityMap: PidCapabilityMap? = null
            while (currentCoroutineContext().isActive && _connectionState.value is ConnectionState.Connected) {
                val adapter = activeAdapter ?: break

                try {
                    val measured = current.measuredPids.toMutableSet()

                    if (capabilityMap == null) {
                        capabilityMap = runCatching {
                            PidPollingPlan.discover { cmd ->
                                val r = adapter.executeCommand(cmd, 2000L)
                                if (r is AdapterResponse.Success) r.rawResponse else null
                            }
                        }.getOrNull()
                    }
                    val pollPlan = PidPollingPlan.supported(capabilityMap)

                    // Periodic battery voltage reading (every 15 cycles ~ 2-3 sec)
                    if (cycleCount % 15 == 0) {
                        val voltResp = adapter.executeCommand("ATRV", 500L)
                        if (voltResp is AdapterResponse.Success) {
                            val vStr = voltResp.rawResponse.replace("V", "", ignoreCase = true).trim()
                            val v = vStr.toFloatOrNull()
                            if (v != null && v > 0f) {
                                measured.add("ATRV")
                                current = current.copy(batteryVoltage = v)
                            }
                        }
                    }
                    cycleCount++

                    // §9 / §22 capability-gated polling: only PIDs the ECU advertised
                    // as supported are queried; unsupported PIDs are never repeatedly
                    // requested (§21). Unknown/empty map degrades to full baseline.
                    for (spec in pollPlan) {
                        val resp = adapter.executeCommand(spec.command, 600L)
                        if (resp is AdapterResponse.Success) {
                            val bytes = PidCodec.cleanResponseHex(resp.rawResponse)
                            PidPollingPlan.decodePoll(spec, bytes, current)?.let {
                                measured.add(spec.pidHex)
                                current = it
                            }
                        }
                    }

                    // 7. §8 TRUTHFULNESS: fuel rate ONLY from measured inputs.
                    //     Previously fabricated displacement (TU5 fallback) + VE(0.80)
                    //     + IAT(0, never queried) -> fake L/h fed to TripComputer.
                    val fuelRateLitersPerHour = FuelEconomyCalculator.fuelRateIfMeasurable(
                        mafGramsPerSec = if ("10" in measured) current.mafGramsPerSec.toDouble() else null,
                        rpm = if ("0C" in measured && current.rpm > 0) current.rpm.toDouble() else null,
                        mapKpa = if ("0B" in measured && current.intakeManifoldPressureKpa > 0) current.intakeManifoldPressureKpa.toDouble() else null,
                        iatCelsius = if ("0F" in measured) current.intakeAirTempCelsius.toDouble() else null,
                        displacementLiters = null, // §8: not on VehicleProfile -> never assume TU5/1.6
                        volumetricEfficiency = null // §8: unknown -> never assume 0.80
                    )

                    // Feed Trip Computer — 0.0 when unmeasured (NO fabricated fuel).
                    currentTripComputer?.processSample(
                        speedKmh = current.speedKmh,
                        rpm = current.rpm,
                        fuelRateLitersPerHour = fuelRateLitersPerHour ?: 0.0,
                        timestampMs = System.currentTimeMillis()
                    )

                    current = current.copy(
                        measuredPids = measured,
                        timestamp = System.currentTimeMillis()
                    )
                    _liveTelemetry.value = current
                    emit(current)

                    delay(60) // Smooth refresh rate
                } catch (_: Exception) {
                    delay(300)
                }
            }
        }

        pollingJob = scope.launch {
            flowStream.collect { /* collect for live state */ }
        }

        return flowStream
    }

    override fun stopTelemetryLoop() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
