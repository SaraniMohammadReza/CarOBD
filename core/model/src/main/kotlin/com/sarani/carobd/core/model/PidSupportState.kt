package com.sarani.carobd.core.model

/**
 * §6 P0-A: explicit, truthful capability state for live PID polling.
 *
 * Replaces the previous "empty/unknown capability -> BASELINE" fallback, which
 * silently polled known-unsupported PIDs forever and presented unknown support
 * as supported. A failed discovery handshake does NOT imply baseline support.
 *
 * - [NotDiscovered]: discovery has not been attempted this session yet.
 * - [Unknown]: the Mode 01 PID 00 handshake could not be positively answered
 *   (NO DATA / 7F / malformed / timeout / disconnect). Capability is genuinely
 *   undetermined — never treat it as supported.
 * - [Known]: the ECU answered; [map] holds its advertised PIDs and may be empty
 *   (the ECU supports none of the baseline set) — equivalent to "unsupported".
 */
sealed interface PidSupportState {
    data object NotDiscovered : PidSupportState
    data class Unknown(val reason: String? = null) : PidSupportState
    data class Known(val map: PidCapabilityMap) : PidSupportState
}
