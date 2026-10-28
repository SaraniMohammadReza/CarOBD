package com.sarani.carobd.core.protocol.obd2

import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.PidCapabilityMap
import com.sarani.carobd.core.model.PidSupportState

/**
 * §9 / §22 / §6-P0-A: canonical, capability-gated SAE J1979 Mode 01 telemetry polling plan.
 *
 * The polling set is derived from the ECU's advertised supported-PID bitmap
 * (Mode 01 PID 00/20/40/60/80/A0/C0/E0). Only PIDs the ECU declares supported are
 * queried, so the live loop never repeatedly requests known-unsupported PIDs
 * (§21/§22 truthfulness).
 *
 * §6-P0-A truthfulness: discovery failure (timeout, NO DATA, 7F, malformed,
 * disconnect) yields [PidSupportState.Unknown] — NOT a fallback to [BASELINE].
 * When capability is unknown or empty, [supported] returns an empty list, so the
 * producer does NOT poll blindly. The ECU must explicitly advertise each PID.
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
     * Capability-gated plan. Returns the baseline subset that the ECU supports,
     * derived from a [PidSupportState]:
     * - [PidSupportState.NotDiscovered]: discovery not yet attempted — conservative empty set.
     * - [PidSupportState.Unknown]: capability undetermined (timeout/NO DATA/7F/malformed) —
     *   conservative empty set. The ECU did NOT prove baseline support.
     * - [PidSupportState.Known]: only PIDs the ECU explicitly advertised are queried.
     *   An empty bitmap (ECU answered but advertises nothing) also yields an empty set.
     */
    fun supported(state: PidSupportState): List<TelemetryPollSpec> = when (state) {
        is PidSupportState.NotDiscovered -> emptyList()
        is PidSupportState.Unknown -> emptyList()
        is PidSupportState.Known -> {
            if (state.map.supportedPids.isEmpty()) emptyList()
            else BASELINE.filter { state.map.isSupported(it.pidHex) }
        }
    }

    /**
     * Probes Mode 01 PID 00/20/40/60/80/A0/C0/E0 and builds a [PidSupportState],
     * following the J1979 extension bit lazily (§6): only request page `0120` if
     * the previous page advertised its boundary PID (`0x20`) as supported, and so
     * on. This avoids hammering unsupported pages.
     *
     * §6-P0-A truthfulness:
     * - 0100 times out (null) / NO DATA / disconnect → [PidSupportState.Unknown].
     * - 0100 returns 7F (NRC rejection) → [PidSupportState.Unknown].
     * - 0100 malformed (wrong SID, too short, PID mismatch) → [PidSupportState.Unknown].
     * - 0100 succeeds → [PidSupportState.Known] with the parsed capability bitmap.
     *
     * A fully unreadable bus does NOT fall back to BASELINE. It returns Unknown,
     * and [supported] returns an empty polling set.
     */
    suspend fun discover(
        respond: suspend (command: String) -> String?
    ): PidSupportState {
        val raw = mutableMapOf<String, List<Int>>()
        var pageBase = "00"                       // page "0100" baseline = PIDs 01..20
        var firstPage = true
        while (pageBase.toInt(16) <= 0xE0) {
            val pageKey = pageBase                    // "00".."E0"
            val cmd = "01$pageKey"                    // "0100".."01E0"
            val resp = respond(cmd)

            if (resp == null) {
                // NO DATA / timeout / disconnect on this page.
                if (firstPage) {
                    return PidSupportState.Unknown(
                        "no response to $cmd (NO DATA/timeout/disconnect)"
                    )
                }
                break  // later page unreadable — keep what we learned from earlier pages
            }

            val bytes = PidCodec.cleanResponseHex(resp)
            val valid = bytes.size >= 6 &&
                (bytes[0].toInt() and 0xFF) == 0x41 &&
                (bytes[1].toInt() and 0xFF) == pageKey.toInt(16)

            when {
                valid -> {
                    raw[pageKey] = listOf(
                        bytes[2].toInt() and 0xFF,
                        bytes[3].toInt() and 0xFF,
                        bytes[4].toInt() and 0xFF,
                        bytes[5].toInt() and 0xFF,
                    )
                }
                firstPage && bytes.isNotEmpty() &&
                    (bytes[0].toInt() and 0xFF) == 0x7F -> {
                    val nrc = bytes.getOrNull(2)?.toInt()?.let { it and 0xFF }
                    return PidSupportState.Unknown(
                        "7F negative response for $cmd (NRC=0x${nrc?.toString(16) ?: "?"})"
                    )
                }
                firstPage -> {
                    return PidSupportState.Unknown(
                        "malformed or invalid 0100 response: ${resp.take(80)}"
                    )
                }
                else -> {
                    // Non-first page malformed/7F — stop probing further pages.
                    break
                }
            }

            firstPage = false

            // Extension bit: the next page exists only if this page's boundary
            // PID (pageBase + 0x20) is supported.
            val nextBoundary = (pageKey.toInt(16) + 0x20)
            val nextPid = "%02X".format(nextBoundary)
            val mapSoFar = PidCapabilityMap.parseFromBitmaps(raw)
            if (!mapSoFar.isSupported(nextPid)) break
            pageBase = nextPid
        }
        return PidSupportState.Known(PidCapabilityMap.parseFromBitmaps(raw))
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
