package com.sarani.carobd.core.protocol.obd2

/**
 * Standard SAE J1979 Diagnostic Service IDs (Modes).
 */
enum class ObdMode(val requestServiceId: Int, val responseServiceId: Int) {
    MODE_01_LIVE_DATA(0x01, 0x41),
    MODE_02_FREEZE_FRAME(0x02, 0x42),
    MODE_03_STORED_DTCS(0x03, 0x43),
    MODE_04_CLEAR_DTCS(0x04, 0x44),
    MODE_05_O2_MONITOR_TESTS(0x05, 0x45),
    MODE_06_ONBOARD_MONITORING(0x06, 0x46),
    MODE_07_PENDING_DTCS(0x07, 0x47),
    MODE_08_CONTROL_OPERATION(0x08, 0x48),
    MODE_09_VEHICLE_INFO(0x09, 0x49),
    MODE_0A_PERMANENT_DTCS(0x0A, 0x4A);

    companion object {
        fun fromResponseServiceId(id: Int): ObdMode? =
            entries.firstOrNull { it.responseServiceId == id }

        fun fromRequestServiceId(id: Int): ObdMode? =
            entries.firstOrNull { it.requestServiceId == id }
    }
}

/**
 * Encapsulates an outgoing diagnostic command string and expected response format.
 */
data class DiagnosticCommand(
    val rawCommand: String,
    val description: String,
    val expectedResponseServiceId: Int? = null,
    val timeoutMs: Long = 2000L
) {
    companion object {
        // Standard OBD-II commands
        fun mode01(pid: Int): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = String.format("01%02X", pid and 0xFF),
                description = "Mode 01 PID 0x%02X".format(pid),
                expectedResponseServiceId = 0x41
            )

        fun mode02(pid: Int, frameIndex: Int = 0): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = String.format("02%02X%02X", pid and 0xFF, frameIndex and 0xFF),
                description = "Mode 02 Freeze Frame PID 0x%02X Frame %d".format(pid, frameIndex),
                expectedResponseServiceId = 0x42
            )

        fun requestStoredDtcs(): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = "03",
                description = "Request Confirmed DTCs (Mode 03)",
                expectedResponseServiceId = 0x43,
                timeoutMs = 4000L
            )

        fun clearDtcs(): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = "04",
                description = "Clear Diagnostic Trouble Codes (Mode 04)",
                expectedResponseServiceId = 0x44,
                timeoutMs = 5000L
            )

        fun requestPendingDtcs(): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = "07",
                description = "Request Pending DTCs (Mode 07)",
                expectedResponseServiceId = 0x47,
                timeoutMs = 4000L
            )

        fun mode09(pid: Int): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = String.format("09%02X", pid and 0xFF),
                description = "Mode 09 Vehicle Info PID 0x%02X".format(pid),
                expectedResponseServiceId = 0x49,
                timeoutMs = 5000L
            )

        fun requestPermanentDtcs(): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = "0A",
                description = "Request Permanent DTCs (Mode 0A)",
                expectedResponseServiceId = 0x4A,
                timeoutMs = 4000L
            )

        /** §10: command factories for diagnostic services that had no command builder. */
        fun mode05(): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = "05",
                description = "Mode 05 O2 Sensor Monitor Tests",
                expectedResponseServiceId = 0x45,
                timeoutMs = 4000L
            )

        fun mode06(pid: Int): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = String.format("06%02X", pid and 0xFF),
                description = "Mode 06 Onboard Monitoring PID 0x%02X".format(pid),
                expectedResponseServiceId = 0x46,
                timeoutMs = 4000L
            )

        fun mode08(pid: Int): DiagnosticCommand =
            DiagnosticCommand(
                rawCommand = String.format("08%02X", pid and 0xFF),
                description = "Mode 08 Control Operation PID 0x%02X".format(pid),
                expectedResponseServiceId = 0x48,
                timeoutMs = 4000L
            )
    }
}
