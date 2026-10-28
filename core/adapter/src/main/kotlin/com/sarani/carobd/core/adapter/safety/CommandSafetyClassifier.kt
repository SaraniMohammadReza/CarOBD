package com.sarani.carobd.core.adapter.safety

import com.sarani.carobd.core.model.CommandSafetyVerdict

/**
 * Pure, stateless classifier that decides whether a raw string sent through the
 * diagnostic terminal is safe to execute.
 *
 * P0 safety (§4): the production flow sends OBD service bytes and AT commands.
 * This classifier is consulted at the **repository / use-case boundary** — the
 * UI alone is not the security boundary. It encodes a small, auditable table:
 *
 * - read-only OBD services (01/02/03/05/06/07/09/0A) and UDS 22 (ReadDataById)
 *   → ALLOWED_READ_ONLY
 * - adapter configuration / identification (AT*) → ALLOWED_ADAPTER_CONFIGURATION
 * - Mode 04 (clear DTC) → REQUIRES_DEDICATED_CONFIRMED_FLOW (use the dedicated
 *   confirmed clear flow; never the raw terminal)
 * - Mode 08, UDS 27/2E/2F/31/34/35/36/37 → BLOCKED_UNSAFE
 * - anything else → UNKNOWN_BLOCKED (deny by default)
 *
 * The first two hex characters of the normalized command are treated as the
 * service byte; payloads/params are ignored so e.g. `2E F1 90 01` is still
 * classified as the write service 2E.
 */
class CommandSafetyClassifier {

    /**
     * Returns the verdict for [rawCommand]. The command is **not** sent to the
     * adapter by this call — the caller uses the verdict to decide.
     */
    fun classify(rawCommand: String): CommandSafetyVerdict {
        val s = normalize(rawCommand)
        if (s.isEmpty()) return CommandSafetyVerdict.UNKNOWN_BLOCKED

        val service = if (s.length >= 2) s.substring(0, 2) else s
        return when {
            service.startsWith("AT") -> CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION
            service in READ_ONLY_SERVICES -> CommandSafetyVerdict.ALLOWED_READ_ONLY
            service == CLEAR_DTC -> CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW
            service in BLOCKED_SERVICES -> CommandSafetyVerdict.BLOCKED_UNSAFE
            else -> CommandSafetyVerdict.UNKNOWN_BLOCKED
        }
    }

    /**
     * Returns true iff [rawCommand] is safe to send through the raw terminal
     * (read-only or adapter configuration). Everything else is refused.
     */
    fun isSafeForTerminal(rawCommand: String): Boolean =
        classify(rawCommand).let {
            it == CommandSafetyVerdict.ALLOWED_READ_ONLY ||
                it == CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION
        }

    private fun normalize(raw: String): String =
        raw.trim().uppercase().replace(Regex("\\s+"), " ")

    private companion object {
        private val READ_ONLY_SERVICES = setOf("01", "02", "03", "05", "06", "07", "09", "0A", "22")
        private val BLOCKED_SERVICES = setOf("08", "27", "2E", "2F", "31", "34", "35", "36", "37")
        private const val CLEAR_DTC = "04"
    }
}
