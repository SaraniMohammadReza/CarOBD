package com.sarani.carobd.core.protocol

object StandardPids {

    // Common AT Initialization Commands
    const val CMD_RESET = "ATZ"
    const val CMD_ECHO_OFF = "ATE0"
    const val CMD_LINEFEED_OFF = "ATL0"
    const val CMD_SPACES_ON = "ATS1"
    const val CMD_SPACES_OFF = "ATS0"
    const val CMD_HEADERS_ON = "ATH1"
    const val CMD_HEADERS_OFF = "ATH0"
    const val CMD_READ_VOLTAGE = "ATRV"
    const val CMD_ADAPTIVE_TIMING_AUTO = "ATAT1"
    const val CMD_TIMEOUT_DEFAULT = "ATST32" // ~200ms timeout

    // Mode 01 Diagnostic Telemetry PIDs
    const val PID_SUPPORTED_00 = "0100"
    const val PID_ENGINE_LOAD = "0104"
    const val PID_COOLANT_TEMP = "0105"
    const val PID_INTAKE_PRESSURE = "010B"
    const val PID_ENGINE_RPM = "010C"
    const val PID_VEHICLE_SPEED = "010D"
    const val PID_TIMING_ADVANCE = "010E"
    const val PID_INTAKE_AIR_TEMP = "010F"
    const val PID_MAF_AIR_FLOW = "0110"
    const val PID_THROTTLE_POS = "0111"
    const val PID_OXYGEN_SENSOR_1 = "0114"
    const val PID_FUEL_LEVEL = "012F"
    const val PID_CONTROL_MODULE_VOLTAGE = "0142"

    // Diagnostics Services
    const val CMD_REQUEST_DTCS = "03"
    const val CMD_CLEAR_DTCS = "04"
    const val CMD_REQUEST_PENDING_DTCS = "07"

    /**
     * Parses Engine RPM from Mode 01 PID 0C response ("41 0C XX YY").
     * Formula: ((A * 256) + B) / 4
     */
    fun parseRpm(bytes: List<Int>): Int? {
        val index = findPidDataIndex(bytes, 0x0C)
        if (index != -1 && index + 1 < bytes.size) {
            val a = bytes[index]
            val b = bytes[index + 1]
            return ((a * 256) + b) / 4
        }
        return null
    }

    /**
     * Parses Vehicle Speed (km/h) from Mode 01 PID 0D response ("41 0D XX").
     * Formula: A
     */
    fun parseSpeed(bytes: List<Int>): Int? {
        val index = findPidDataIndex(bytes, 0x0D)
        if (index != -1 && index < bytes.size) {
            return bytes[index]
        }
        return null
    }

    /**
     * Parses Coolant Temperature (°C) from Mode 01 PID 05 response ("41 05 XX").
     * Formula: A - 40
     */
    fun parseCoolantTemp(bytes: List<Int>): Int? {
        val index = findPidDataIndex(bytes, 0x05)
        if (index != -1 && index < bytes.size) {
            return bytes[index] - 40
        }
        return null
    }

    /**
     * Parses Throttle Position (%) from Mode 01 PID 11 response ("41 11 XX").
     * Formula: (A * 100) / 255
     */
    fun parseThrottlePosition(bytes: List<Int>): Float? {
        val index = findPidDataIndex(bytes, 0x11)
        if (index != -1 && index < bytes.size) {
            return (bytes[index] * 100f) / 255f
        }
        return null
    }

    /**
     * Parses Engine Load (%) from Mode 01 PID 04 response ("41 04 XX").
     * Formula: (A * 100) / 255
     */
    fun parseEngineLoad(bytes: List<Int>): Float? {
        val index = findPidDataIndex(bytes, 0x04)
        if (index != -1 && index < bytes.size) {
            return (bytes[index] * 100f) / 255f
        }
        return null
    }

    /**
     * Parses Intake Air Temp (°C) from Mode 01 PID 0F.
     * Formula: A - 40
     */
    fun parseIntakeAirTemp(bytes: List<Int>): Int? {
        val index = findPidDataIndex(bytes, 0x0F)
        if (index != -1 && index < bytes.size) {
            return bytes[index] - 40
        }
        return null
    }

    /**
     * Parses Fuel Tank Level (%) from Mode 01 PID 2F.
     * Formula: (A * 100) / 255
     */
    fun parseFuelLevel(bytes: List<Int>): Float? {
        val index = findPidDataIndex(bytes, 0x2F)
        if (index != -1 && index < bytes.size) {
            return (bytes[index] * 100f) / 255f
        }
        return null
    }

    /**
     * Parses Battery Voltage from ATRV response string e.g. "12.6V".
     */
    fun parseVoltage(raw: String): Float? {
        val cleaned = raw.replace("V", "").replace("v", "").trim()
        return cleaned.toFloatOrNull()
    }

    private fun findPidDataIndex(bytes: List<Int>, expectedPid: Int): Int {
        for (i in 0 until bytes.size - 1) {
            if (bytes[i] == 0x41 && bytes[i + 1] == expectedPid) {
                return i + 2
            }
        }
        // If response is raw without mode byte:
        if (bytes.isNotEmpty() && bytes[0] == expectedPid) {
            return 1
        }
        return -1
    }
}
