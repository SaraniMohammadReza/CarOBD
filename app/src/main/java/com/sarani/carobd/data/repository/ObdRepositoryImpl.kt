package com.sarani.carobd.data.repository

import android.bluetooth.BluetoothManager
import android.content.Context
import com.sarani.carobd.core.adapter.ObdAdapter
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.command.DtcScanValidator
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.DtcCategory
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.NotConnectedException
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.protocol.dtc.DtcLookupEngine
import com.sarani.carobd.core.protocol.dtc.DtcParser
import com.sarani.carobd.core.protocol.obd2.PidCodec
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

        // 1. Request Mode 03 (Stored DTCs)
        val mode03Resp = adapter.executeCommand("03", 4000L)
        // Truthful gate (§28): Mode 03 is the canonical "ECU is answering" signal. A
        // non-success / absent response means the ECU is unreachable and MUST surface as
        // NotConnected — never as an empty "no codes found" result.
        DtcScanValidator.notConnectedError(mode03Resp)?.let { return@withContext Result.failure(it) }
        if (mode03Resp is AdapterResponse.Success) {
            val cleanHex = PidCodec.cleanResponseHex(mode03Resp.rawResponse)
            val stored = DtcParser.parseMode03Payload(cleanHex, com.sarani.carobd.core.model.DtcCategory.STORED)
            for (c in stored) {
                dtcList.add(
                    DiagnosticCode(
                        code = c.code,
                        titleEn = c.titleEn,
                        titleFa = c.titleFa,
                        descriptionEn = c.descriptionEn,
                        descriptionFa = c.descriptionFa,
                        ecuSource = c.ecuSource
                    )
                )
            }
        }

        // 2. Request Mode 07 (Pending DTCs)
        val mode07Resp = adapter.executeCommand("07", 4000L)
        if (mode07Resp is AdapterResponse.Success) {
            val cleanHex = PidCodec.cleanResponseHex(mode07Resp.rawResponse)
            val pending = DtcParser.parseMode03Payload(cleanHex, com.sarani.carobd.core.model.DtcCategory.PENDING)
            for (c in pending) {
                dtcList.add(
                    DiagnosticCode(
                        code = c.code,
                        titleEn = c.titleEn,
                        titleFa = c.titleFa,
                        descriptionEn = c.descriptionEn,
                        descriptionFa = c.descriptionFa,
                        ecuSource = c.ecuSource
                    )
                )
            }
        }

        // 3. Request Mode 0A (Permanent DTCs)
        val mode0aResp = adapter.executeCommand("0A", 4000L)
        if (mode0aResp is AdapterResponse.Success) {
            val cleanHex = PidCodec.cleanResponseHex(mode0aResp.rawResponse)
            val permanent = DtcParser.parseMode03Payload(cleanHex, com.sarani.carobd.core.model.DtcCategory.PERMANENT)
            for (c in permanent) {
                dtcList.add(
                    DiagnosticCode(
                        code = c.code,
                        titleEn = c.titleEn,
                        titleFa = c.titleFa,
                        descriptionEn = c.descriptionEn,
                        descriptionFa = c.descriptionFa,
                        ecuSource = c.ecuSource
                    )
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
        if (response is AdapterResponse.Success) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("ECU did not acknowledge DTC clear request"))
        }
    }

    override suspend fun sendRawCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        val adapter = activeAdapter ?: return@withContext Result.failure(Exception("Adapter not connected"))
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
            val engineName = _activeVehicleProfile.value?.engineName ?: "Unknown"
            val displacement = when {
                engineName.contains("XU7", ignoreCase = true) -> 1.761
                engineName.contains("EF7", ignoreCase = true) -> 1.645
                engineName.contains("M15", ignoreCase = true) -> 1.503
                engineName.contains("M13", ignoreCase = true) -> 1.323
                engineName.contains("TU3", ignoreCase = true) -> 1.360
                else -> 1.587 // TU5
            }

            var cycleCount = 0
            while (currentCoroutineContext().isActive && _connectionState.value is ConnectionState.Connected) {
                val adapter = activeAdapter ?: break

                try {
                    val measured = current.measuredPids.toMutableSet()

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

                    // 1. Query RPM (PID 0C)
                    val rpmResp = adapter.executeCommand("010C", 600L)
                    if (rpmResp is AdapterResponse.Success) {
                        val bytes = PidCodec.cleanResponseHex(rpmResp.rawResponse)
                        if (bytes.size >= 4 && bytes[0].toInt() == 0x41 && bytes[1].toInt() == 0x0C) {
                            val a = bytes[2].toInt() and 0xFF
                            val b = bytes[3].toInt() and 0xFF
                            measured.add("0C")
                            current = current.copy(rpm = PidCodec.decodeEngineRpm(a, b).toInt())
                        }
                    }

                    // 2. Query Vehicle Speed (PID 0D)
                    val speedResp = adapter.executeCommand("010D", 600L)
                    if (speedResp is AdapterResponse.Success) {
                        val bytes = PidCodec.cleanResponseHex(speedResp.rawResponse)
                        if (bytes.size >= 3 && bytes[0].toInt() == 0x41 && bytes[1].toInt() == 0x0D) {
                            val a = bytes[2].toInt() and 0xFF
                            measured.add("0D")
                            current = current.copy(speedKmh = PidCodec.decodeVehicleSpeed(a).toInt())
                        }
                    }

                    // 3. Query Throttle Position (PID 11)
                    val throttleResp = adapter.executeCommand("0111", 600L)
                    if (throttleResp is AdapterResponse.Success) {
                        val bytes = PidCodec.cleanResponseHex(throttleResp.rawResponse)
                        if (bytes.size >= 3 && bytes[0].toInt() == 0x41 && bytes[1].toInt() == 0x11) {
                            val a = bytes[2].toInt() and 0xFF
                            measured.add("11")
                            current = current.copy(throttlePositionPercent = PidCodec.decodeThrottlePosition(a).toFloat())
                        }
                    }

                    // 4. Query Coolant Temp (PID 05)
                    val coolantResp = adapter.executeCommand("0105", 600L)
                    if (coolantResp is AdapterResponse.Success) {
                        val bytes = PidCodec.cleanResponseHex(coolantResp.rawResponse)
                        if (bytes.size >= 3 && bytes[0].toInt() == 0x41 && bytes[1].toInt() == 0x05) {
                            val a = bytes[2].toInt() and 0xFF
                            measured.add("05")
                            current = current.copy(coolantTempCelsius = PidCodec.decodeStandardTemperature(a).toInt())
                        }
                    }

                    // 5. Query Engine Load (PID 04)
                    val loadResp = adapter.executeCommand("0104", 600L)
                    if (loadResp is AdapterResponse.Success) {
                        val bytes = PidCodec.cleanResponseHex(loadResp.rawResponse)
                        if (bytes.size >= 3 && bytes[0].toInt() == 0x41 && bytes[1].toInt() == 0x04) {
                            val a = bytes[2].toInt() and 0xFF
                            measured.add("04")
                            current = current.copy(engineLoadPercent = PidCodec.decodeEngineLoad(a).toFloat())
                        }
                    }

                    // 6. Query Intake Manifold Pressure (MAP - PID 0B)
                    val mapResp = adapter.executeCommand("010B", 600L)
                    if (mapResp is AdapterResponse.Success) {
                        val bytes = PidCodec.cleanResponseHex(mapResp.rawResponse)
                        if (bytes.size >= 3 && bytes[0].toInt() == 0x41 && bytes[1].toInt() == 0x0B) {
                            val a = bytes[2].toInt() and 0xFF
                            measured.add("0B")
                            current = current.copy(intakeManifoldPressureKpa = PidCodec.decodeMapPressure(a).toInt())
                        }
                    }

                    // 7. Calculate physical fuel rate (Speed-Density or MAF)
                    val fuelRateLitersPerHour = FuelEconomyCalculator.calculateFuelRateFromSpeedDensity(
                        mapKpa = current.intakeManifoldPressureKpa.toDouble(),
                        iatCelsius = current.intakeAirTempCelsius.toDouble(),
                        rpm = current.rpm.toDouble(),
                        displacementLiters = displacement,
                        volumetricEfficiency = 0.80
                    )

                    // Feed Trip Computer
                    currentTripComputer?.processSample(
                        speedKmh = current.speedKmh,
                        rpm = current.rpm,
                        fuelRateLitersPerHour = fuelRateLitersPerHour,
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
