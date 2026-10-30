package com.sarani.carobd.data.transport

import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.FeatureMaturity
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.math.sin

/**
 * Isolated deterministic vehicle simulator transport.
 * Explicitly labeled as [FeatureMaturity.SIMULATOR_ONLY].
 * Allows testing full UI, gauges, charts, DTC clearance, and PID workflows
 * without requiring physical car hardware or an adapter connected.
 *
 * [scenario] selects the synthetic ECU behaviour (§39). The default
 * [SimulatorScenario.HEALTHY_VEHICLE] preserves the original behaviour.
 */
class IsolatedSimulatorTransport(
    val scenario: SimulatorScenario = SimulatorScenario.HEALTHY_VEHICLE,
    parentScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val latePromptDelayMs: Long = 750L,
) : ObdTransport {

    val maturity: FeatureMaturity = FeatureMaturity.SIMULATOR_ONLY

    override val transportType: TransportType = TransportType.SIMULATOR

    @Volatile
    private var _isConnected: Boolean = false
    override val isConnected: Boolean
        get() = _isConnected

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private val owner = SupervisorJob(parentScope.coroutineContext[Job])
    private val scope = CoroutineScope(parentScope.coroutineContext + owner)
    private var simulationStep = 0.0
    private var isDtcsCleared = false

    override suspend fun connect() {
        check(owner.isActive) { "Simulator retired; create a new session" }
        _isConnected = true
        isDtcsCleared = false
    }

    override suspend fun disconnect() {
        _isConnected = false
        owner.cancelAndJoin()
    }

    override suspend fun write(data: ByteArray) {
        if (!_isConnected) return

        val cmd = String(data, Charsets.US_ASCII).trim().uppercase()
        if (!cmd.startsWith("AT") && (scenario == SimulatorScenario.TIMEOUT || scenario == SimulatorScenario.ENGINE_OFF)) return
        if (scenario == SimulatorScenario.DISCONNECT && cmd == "010C") { _isConnected = false; return }
        val response = processCommand(cmd)
        val bytes = "$response\r\r>".toByteArray(Charsets.US_ASCII)
        if (scenario == SimulatorScenario.LATE_PROMPT && cmd == "010C") {
            // write returns now. The adapter's receive deadline runs before this
            // independently owned response/prompt arrives.
            scope.launch { delay(latePromptDelayMs); if (_isConnected) _incomingBytes.emit(bytes) }
        } else _incomingBytes.emit(bytes)
    }

    private fun processCommand(cmd: String): String {
        simulationStep += 0.1

        // 1. AT Command Handling
        if (cmd.startsWith("AT")) {
            return when {
                cmd == "ATZ" -> "ELM327 v1.5 (SIMULATOR)"
                cmd == "ATE0" || cmd == "ATE1" -> "OK"
                cmd == "ATL0" || cmd == "ATL1" -> "OK"
                cmd == "ATS0" || cmd == "ATS1" -> "OK"
                cmd == "ATH0" || cmd == "ATH1" -> "OK"
                cmd == "ATAT1" || cmd == "ATAT2" -> "OK"
                cmd == "ATI" -> "ELM327 v1.5"
                cmd == "AT@1" -> "CarObd Virtual Simulator"
                cmd == "AT@2" -> "SIM-IKCO-TU5-2026"
                cmd == "ATRV" -> "13.9V"
                cmd.startsWith("ATSP") -> "OK"
                cmd == "ATDP" -> "ISO 15765-4 (CAN 11/500)"
                cmd == "ATDPN" -> "6" // ISO 15765-4 CAN 11-bit 500k
                cmd.startsWith("ATSH") -> "OK"
                cmd == "ATCSM0" || cmd == "ATCSM1" -> "OK"
                cmd == "ATCAF1" || cmd == "ATCAF0" -> "OK"
                else -> "OK"
            }
        }

        // 2. OBD Diagnostic Modes Handling — scenario-routed (§11, §39).
        // AT commands are handled above so adapter identity is always answerable
        // regardless of the simulated ECU state.
        return when (scenario) {
            SimulatorScenario.NO_DATA -> "NO DATA"
            SimulatorScenario.BUS_INIT_ERROR -> "BUS INIT: ERROR"
            SimulatorScenario.CAN_ERROR -> "CAN ERROR"
            SimulatorScenario.NEGATIVE_RESPONSE -> "7F ${cmd.take(2)} 12" // ECU responded and rejected (mode 01, NRC 0x12)
            SimulatorScenario.TIMEOUT, SimulatorScenario.ENGINE_OFF -> "" // no wire response; see write()
            SimulatorScenario.MALFORMED -> if (cmd == "03") "43 FFFFFFFF" else respondOod(cmd)
            SimulatorScenario.MULTI_ECU -> if (cmd == "03") "7E8 43 01 01 33\r7E9 43 01 01 33\r7EA 7F 03 12" else respondOod(cmd)
            SimulatorScenario.STORED_DTC, SimulatorScenario.PENDING_DTC, SimulatorScenario.PERMANENT_DTC,
            SimulatorScenario.DISCONNECT, SimulatorScenario.HEALTHY_VEHICLE, SimulatorScenario.LATE_PROMPT -> respondOod(cmd)
            SimulatorScenario.BUFFER_FULL -> "BUFFER FULL"
        }
    }

    /** Healthy-vehicle synthetic responses for Mode 01/03/04/07/09 (§39 baseline). */
    private fun respondOod(cmd: String): String = when {
        cmd == "0600" -> "46 00 80 00 00 00"
        cmd == "0601" -> "46 01 01 FE 00 10 00 00 FF FF"
        cmd == "020000" -> "42 00 00 00 18 00 00"
        cmd == "020C00" -> "42 0C 00 1A F8"
        cmd == "020D00" -> "42 0D 00 55"
        // Mode 01 PID 00 (Supported PIDs 01-20)
        cmd == "0100" -> "41 00 BE 3F B8 11"

        // Mode 01 PID 20 (Supported PIDs 21-40)
        cmd == "0120" -> "41 20 80 05 B0 11"

        // Mode 01 PID 40 (Supported PIDs 41-60)
        cmd == "0140" -> "41 40 7A D0 80 00"

        // Mode 01 PID 01 (Monitor status)
        cmd == "0101" -> if (scenario == SimulatorScenario.STORED_DTC && !isDtcsCleared) "41 01 82 07 65 00" else "41 01 00 07 65 00"

        // Mode 01 PID 0C (RPM: 800 - 3200 with sine oscillation)
        cmd == "010C" -> {
            val rpm = 1800 + (1000 * sin(simulationStep)).toInt()
            val encoded = (rpm * 4)
            val a = (encoded ushr 8) and 0xFF
            val b = encoded and 0xFF
            String.format("41 0C %02X %02X", a, b)
        }

        // Mode 01 PID 0D (Speed: 0 - 110 km/h)
        cmd == "010D" -> {
            val speed = (55 + (45 * sin(simulationStep * 0.7))).toInt().coerceIn(0, 160)
            String.format("41 0D %02X", speed)
        }

        // Mode 01 PID 05 (Coolant Temp: 88 - 93 °C)
        cmd == "0105" -> {
            val tempC = 90 + (2 * sin(simulationStep * 0.2)).toInt()
            val raw = tempC + 40
            String.format("41 05 %02X", raw)
        }

        // Mode 01 PID 11 (Throttle: 15% - 65%)
        cmd == "0111" -> {
            val throttlePercent = 35 + (20 * sin(simulationStep)).toFloat()
            val raw = (throttlePercent * 255.0 / 100.0).toInt().coerceIn(0, 255)
            String.format("41 11 %02X", raw)
        }

        // Mode 01 PID 04 (Engine Load: 25% - 70%)
        cmd == "0104" -> {
            val loadPercent = 40 + (25 * sin(simulationStep)).toFloat()
            val raw = (loadPercent * 255.0 / 100.0).toInt().coerceIn(0, 255)
            String.format("41 04 %02X", raw)
        }

        // Mode 01 PID 0B (MAP: 35 - 75 kPa)
        cmd == "010B" -> {
            val mapKpa = (50 + (20 * sin(simulationStep))).toInt().coerceIn(20, 105)
            String.format("41 0B %02X", mapKpa)
        }

        // Mode 01 PID 0F (Intake Temp: 32 °C)
        cmd == "010F" -> {
            val raw = 32 + 40
            String.format("41 0F %02X", raw)
        }

        // Mode 01 PID 10 (MAF: 6.5 g/s)
        cmd == "0110" -> {
            val maf = 6.5 + (3.0 * sin(simulationStep))
            val raw = (maf * 100.0).toInt().coerceIn(0, 65535)
            String.format("41 10 %02X %02X", (raw ushr 8) and 0xFF, raw and 0xFF)
        }

        // Mode 01 PID 42 (Control Module Voltage: 13.8V - 14.1V)
        cmd == "0142" -> {
            val v = 13.9 + (0.2 * sin(simulationStep * 0.5))
            val raw = (v * 1000.0).toInt()
            String.format("41 42 %02X %02X", (raw ushr 8) and 0xFF, raw and 0xFF)
        }

        // Mode 01 PID 2F (Fuel Level: 68%)
        cmd == "012F" -> {
            val raw = (68.0 * 255.0 / 100.0).toInt()
            String.format("41 2F %02X", raw)
        }

        // Mode 01 PID 0E (Timing Advance: 14°)
        cmd == "010E" -> {
            val raw = (14.0 * 2.0 + 128.0).toInt()
            String.format("41 0E %02X", raw)
        }

        cmd == "03" -> if (scenario == SimulatorScenario.STORED_DTC && !isDtcsCleared) "43 02 01 33 03 00" else "43 00"
        cmd == "07" -> if (scenario == SimulatorScenario.PENDING_DTC && !isDtcsCleared) "47 01 01 15" else "47 00"
        cmd == "0A" -> if (scenario == SimulatorScenario.PERMANENT_DTC) "4A 01 01 33" else "4A 00"

        // Mode 04 (Clear DTCs)
        cmd == "04" -> {
            isDtcsCleared = true
            "44"
        }

        cmd == "0900" -> "49 00 FC C0 00 00"
        cmd in setOf("0901", "0903", "0905", "0909") -> "49 ${cmd.drop(2)} 01"
        cmd == "0904" -> "490401" + "SIM-CAL".padEnd(16, '\u0000').toByteArray().joinToString("") { "%02X".format(it) }
        cmd == "0906" -> "49 06 01 12 34 AB CD"
        cmd == "090A" -> "490A01" + "SIMULATED ECU".padEnd(20, '\u0000').toByteArray().joinToString("") { "%02X".format(it) }
        // Mode 09 PID 02 (synthetic fixture; no vehicle provenance claim)
        cmd == "0902" -> {
            // Synthetic VIN-shaped test data only.
            "49 02 01 56 46 33 32 41 4B 46 57 46 31 32 33 34 35 36 37 38"
        }
        else -> "NO DATA"
    }
}
