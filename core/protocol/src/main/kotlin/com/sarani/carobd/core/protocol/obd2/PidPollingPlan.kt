package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.PidCapabilityMap

/**
 * §9 / §22: canonical, capability-gated SAE J1979 Mode 01 telemetry polling plan.
 *
 * The polling set is derived from the ECU's advertised supported-PID bitmap
 * (Mode 01 PID 00/20/40/60/80/A0/C0/E0). Only PIDs the ECU declares supported are
 * queried, so the live loop never repeatedly requests known-unsupported PIDs
 * (§21/§22 truthfulness). When the capability map is unavailable or unreadable,
 * [supported] degrades to the full [BASELINE] set — no regression vs.
 * pre-capability polling.
 *
 * Decode logic ([decodePoll]) is isolated here as a pure function so the
 * telemetry truthfulness rules are unit-tested independently of the transport.
 */
data class TelemetryPollSpec(
    val command: String, // wire command, e.g. "010C"
    val pidHex: String   // canonical PID, e.g. "0C"
)

object PidPollingPlan {

    /** Baseline live telemetry set — universally supported OBD-II PIDs. */
    val BASELINE: List<TelemetryPollSpec> = listOf(
        TelemetryPollSpec("010C", "0C"), // RPM
        TelemetryPollSpec("010D", "0D"), // Vehicle speed
        TelemetryPollSpec("0111", "11"), // Throttle position
        TelemetryPollSpec("0105", "05"), // Coolant temperature
        TelemetryPollSpec("0104", "04"), // Engine load
        TelemetryPollSpec("010B", "0B"), // Intake manifold pressure (MAP)
        TelemetryPollSpec("0110", "10"), // Mass air flow (sole fuel-economy input)
    )

    /**
     * Capability-gated plan. Returns the baseline subset that the ECU supports.
     * A null or empty map yields the full baseline (graceful fallback).
     */
    fun supported(caps: PidCapabilityMap?): List<TelemetryPollSpec> {
        if (caps == null || caps.supportedPids.isEmpty()) return BASELINE
        return BASELINE.filter { caps.isSupported(it.pidHex) }
    }

    /**
     * Probes Mode 01 PID 00/20/40/60/80/A0/C0/E0 and builds a [PidCapabilityMap].
     * Each page is asked via [respond]; malformed, non-positive (7F / NO DATA) or
     * absent pages are skipped. A fully unreadable bus yields an empty map (the
     * caller's [supported] then falls back to [BASELINE]).
     */
    suspend fun discover(
        respond: suspend (command: String) -> String?
    ): PidCapabilityMap {
        val pages = listOf("0100", "0120", "0140", "0160", "0180", "01A0", "01C0", "01E0")
        val raw = mutableMapOf<String, List<Int>>()
        for (p in pages) {
            val resp = respond(p) ?: continue
            val bytes = PidCodec.cleanResponseHex(resp)
            // Need at least: 0x41, PID echo, 4 data bytes (6 bytes total).
            if (bytes.size < 6) continue
            val mode = bytes[0].toInt() and 0xFF
            val pid = bytes[1].toInt() and 0xFF
            val expectedPid = p.substring(2).toInt(16) // "0100" -> 0x00 ...
            if (mode != 0x41 || pid != expectedPid) continue // 7F / NO DATA / wrong PID
            val data = listOf(
                bytes[2].toInt() and 0xFF,
                bytes[3].toInt() and 0xFF,
                bytes[4].toInt() and 0xFF,
                bytes[5].toInt() and 0xFF,
            )
            raw[p.substring(2)] = data
        }
        return PidCapabilityMap.parseFromBitmaps(raw)
    }

    /**
     * Decodes one positive Mode 01 response into a new [LiveTelemetry].
     * Returns null when [bytes] do not match [spec] (malformed / PID mismatch),
     * so the caller leaves the field unmeasured — rendered as "—", never 0.
     */
    fun decodePoll(spec: TelemetryPollSpec, bytes: ByteArray, current: LiveTelemetry): LiveTelemetry? {
        val pid = spec.pidHex.toInt(16)
        if (bytes.size < 3) return null
        if ((bytes[0].toInt() and 0xFF) != 0x41) return null
        if ((bytes[1].toInt() and 0xFF) != pid) return null
        return when (spec.pidHex) {
            "0C" -> { // RPM (2-byte)
                if (bytes.size < 4) return null
                current.copy(rpm = PidCodec.decodeEngineRpm(
                    bytes[2].toInt() and 0xFF, bytes[3].toInt() and 0xFF
                ).toInt())
            }
            "10" -> { // MAF (2-byte)
                if (bytes.size < 4) return null
                current.copy(mafGramsPerSec = PidCodec.decodeMafAirFlow(
                    bytes[2].toInt() and 0xFF, bytes[3].toInt() and 0xFF
                ).toFloat())
            }
            "0D" -> current.copy(speedKmh = PidCodec.decodeVehicleSpeed(bytes[2].toInt() and 0xFF).toInt())
            "11" -> current.copy(throttlePositionPercent = PidCodec.decodeThrottlePosition(bytes[2].toInt() and 0xFF).toFloat())
            "05" -> current.copy(coolantTempCelsius = PidCodec.decodeStandardTemperature(bytes[2].toInt() and 0xFF).toInt())
            "04" -> current.copy(engineLoadPercent = PidCodec.decodeEngineLoad(bytes[2].toInt() and 0xFF).toFloat())
            "0B" -> current.copy(intakeManifoldPressureKpa = PidCodec.decodeMapPressure(bytes[2].toInt() and 0xFF).toInt())
            else -> null
        }
    }
}
