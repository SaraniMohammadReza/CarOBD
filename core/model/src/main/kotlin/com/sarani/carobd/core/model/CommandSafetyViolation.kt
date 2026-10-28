package com.sarani.carobd.core.model

/**
 * Safety verdict for a raw command attempted through the terminal path.
 *
 * P0 boundary (§4): the UI is NOT the security boundary — the repository /
 * use-case consults [CommandSafetyClassifier] before any command reaches the
 * adapter. A verdict here is the single source of truth for "is this raw
 * command safe to send as-is".
 */
enum class CommandSafetyVerdict {
    /** Safe read-only OBD diagnostic request (Mode 01/02/03/05/06/07/09/0A, UDS 22). */
    ALLOWED_READ_ONLY,
    /** Non-destructive adapter configuration / identification (AT* commands). */
    ALLOWED_ADAPTER_CONFIGURATION,
    /**
     * A clear/write that is handled only by a dedicated, confirmed product flow
     * (Mode 04 clear-DTC). The raw terminal refuses and directs the user to that
     * flow; it is NOT blocked outright so the dedicated flow still works.
     */
    REQUIRES_DEDICATED_CONFIRMED_FLOW,
    /** Explicitly blocked: Mode 08, SecurityAccess, WriteData, IOControl,
     * RoutineControl, RequestDownload/RequestUpload, TransferData. */
    BLOCKED_UNSAFE,
    /** Unknown / unclassifiable — denied by default in the raw terminal. */
    UNKNOWN_BLOCKED,
}

/**
 * Thrown (and surfaced to the user) when a raw command is refused by the
 * CommandSafetyClassifier at the repository / use-case boundary.
 */
class CommandSafetyViolation(
    val verdict: CommandSafetyVerdict,
    val rawCommand: String,
    val reasonFa: String,
    val reasonEn: String = reasonFa,
) : Exception(reasonEn)
