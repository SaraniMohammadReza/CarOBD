package com.sarani.carobd.core.connectivity

import com.sarani.carobd.core.model.ConnectionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

class VirtualCarSimulator : ObdConnection {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var simulatedTick = 0L
    private val activeFaultCodes = mutableListOf("P0300", "P0115", "P0505")
    private var echoEnabled = false
    private var currentProtocolName = "ISO 14230-4 KWP (Fast Init)"

    override suspend fun connect(target: String): Result<Unit> {
        _connectionState.value = ConnectionState.Connecting
        delay(300)
        _connectionState.value = ConnectionState.Initializing("ELM327 v1.5 Handshake...")
        delay(400)
        _connectionState.value = ConnectionState.Initializing("Negotiating Protocol ($currentProtocolName)...")
        delay(400)
        _connectionState.value = ConnectionState.Connected(
            deviceName = "Virtual ELM327 OBD-II Simulator",
            protocolName = currentProtocolName,
            elmVersion = "ELM327 v1.5 (Simulated Engine)"
        )
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendCommand(command: String): Result<String> {
        if (!isConnected()) {
            return Result.failure(IllegalStateException("Simulator not connected"))
        }

        // Simulate transmission and processing delay of real ELM327 adapter
        delay(25)
        simulatedTick++

        val cleanCmd = command.trim().uppercase()
        val response = processCommand(cleanCmd)
        return Result.success(response)
    }

    override fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    private fun processCommand(cmd: String): String {
        return when {
            cmd == "ATZ" || cmd == "AT WS" -> "ELM327 v1.5\r\r>"
            cmd == "ATE0" -> {
                echoEnabled = false
                "OK\r\r>"
            }
            cmd == "ATE1" -> {
                echoEnabled = true
                "OK\r\r>"
            }
            cmd == "ATL0" || cmd == "ATL1" || cmd == "ATS0" || cmd == "ATS1" || cmd == "ATH1" || cmd == "ATH0" -> "OK\r\r>"
            cmd.startsWith("AT SP") -> {
                currentProtocolName = "User Configured (${cmd})"
                "OK\r\r>"
            }
            cmd.startsWith("AT SH") || cmd.startsWith("AT WM") || cmd.startsWith("AT SW") || cmd.startsWith("AT IB") || cmd.startsWith("AT ST") || cmd.startsWith("AT AT") -> "OK\r\r>"
            cmd == "ATDP" -> "$currentProtocolName\r\r>"
            cmd == "ATRV" -> {
                val voltage = 14.1f + (sin(simulatedTick * 0.1).toFloat() * 0.15f)
                String.format(java.util.Locale.US, "%.1fV\r\r>", voltage)
            }

            // Mode 01 Live Telemetry PIDs
            cmd == "0100" -> "41 00 BE 3E B8 11\r\r>" // Supported PIDs 01-20
            cmd == "010C" -> {
                // Simulated Engine RPM: oscillates between 850 (idle) and 3400 (accelerating)
                val base = 1600.0
                val variance = 1200.0 * sin(simulatedTick * 0.08)
                val rpm = (base + variance).toInt().coerceIn(820, 6200)
                val rawValue = rpm * 4
                val a = (rawValue ushr 8) and 0xFF
                val b = rawValue and 0xFF
                String.format(java.util.Locale.US, "41 0C %02X %02X\r\r>", a, b)
            }
            cmd == "010D" -> {
                // Simulated Speed: 0 to 115 km/h
                val speed = ((sin(simulatedTick * 0.05) + 1.0) * 45.0).toInt().coerceIn(0, 140)
                String.format(java.util.Locale.US, "41 0D %02X\r\r>", speed)
            }
            cmd == "0105" -> {
                // Coolant Temperature: warms up to ~89°C
                val temp = (89 - (10.0 / (simulatedTick * 0.05 + 1.0))).toInt().coerceIn(40, 93)
                val rawA = temp + 40
                String.format(java.util.Locale.US, "41 05 %02X\r\r>", rawA)
            }
            cmd == "0111" -> {
                // Throttle Position %
                val throttle = ((sin(simulatedTick * 0.08) + 1.0) * 25.0).toInt().coerceIn(0, 85)
                val rawA = ((throttle * 255) / 100).coerceIn(0, 255)
                String.format(java.util.Locale.US, "41 11 %02X\r\r>", rawA)
            }
            cmd == "0104" -> {
                // Engine Load %
                val load = ((sin(simulatedTick * 0.08) + 1.0) * 30.0).toInt().coerceIn(12, 90)
                val rawA = ((load * 255) / 100).coerceIn(0, 255)
                String.format(java.util.Locale.US, "41 04 %02X\r\r>", rawA)
            }
            cmd == "010F" -> {
                // Intake Air Temp (around 32°C)
                val tempA = 32 + 40
                String.format(java.util.Locale.US, "41 0F %02X\r\r>", tempA)
            }
            cmd == "012F" -> {
                // Fuel Tank Level (around 62%)
                val rawA = ((62 * 255) / 100)
                String.format(java.util.Locale.US, "41 2F %02X\r\r>", rawA)
            }

            // Mode 03: Read Stored Trouble Codes
            cmd == "03" -> {
                if (activeFaultCodes.isEmpty()) {
                    "43 00 00 00 00 00 00\r\r>"
                } else {
                    buildDtcResponse()
                }
            }

            // Mode 04: Clear Trouble Codes
            cmd == "04" -> {
                activeFaultCodes.clear()
                "44\r\r>"
            }

            else -> "41 ${cmd.takeLast(2)} 00 00\r\r>"
        }
    }

    private fun buildDtcResponse(): String {
        val sb = StringBuilder("43 ")
        for (code in activeFaultCodes) {
            val hex1 = code.substring(1, 3)
            val hex2 = code.substring(3, 5)
            sb.append("$hex1 $hex2 ")
        }
        sb.append("\r\r>")
        return sb.toString()
    }
}
