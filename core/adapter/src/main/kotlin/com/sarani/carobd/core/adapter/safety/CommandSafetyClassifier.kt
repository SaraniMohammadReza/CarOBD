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
 * - adapter identity / read-only formatting AT commands (ATI, ATRV, ATDP,
 *   ATDPN, ATE0/1, ATH0/1, ATS0/1, AL0/1, ATZ, ATAT) → ALLOWED_ADAPTER_CONFIGURATION
 * - routing / filtering / protocol reconfiguration AT commands (ATSH, ATCRA,
 *   ATSP, ATTP, ATCAF, ATCFC, ATAF, ATEXST, ATST, ATCM, ATCS, ATCF, ATCSM)
 *   → BLOCKED_UNSAFE (§11: these can poison the active diagnostic session)
 * - Mode 04 (clear DTC) → REQUIRES_DEDICATED_CONFIRMED_FLOW (use the dedicated
 *   confirmed clear flow; never the raw terminal)
 * - Mode 08, UDS 27/2E/2F/31/34/35/36/37 → BLOCKED_UNSAFE
 * - any AT command not in the safe or blocked catalog → UNKNOWN_BLOCKED
 * - anything else unknown → UNKNOWN_BLOCKED (deny by default)
 *
 * The first two hex characters of the normalized command are treated as the
 * service byte; payloads/params are ignored so e.g. `2E F1 90 01` is still
 * classified as the write service 2E. For AT commands the full mnemonic is
 * inspected to distinguish safe identity/read commands from dangerous routing.
 */
class CommandSafetyClassifier {

    /**
     * Returns the verdict for [rawCommand]. The command is **not** sent to the
     * adapter by this call — the caller uses the verdict to decide.
     */
    fun classify(rawCommand: String): CommandSafetyVerdict {
        val s = normalize(rawCommand)
        if (s.isEmpty()) return CommandSafetyVerdict.UNKNOWN_BLOCKED

        if (s.startsWith("AT")) {
            // §11: AT commands are NOT uniformly safe. Classify by mnemonic:
            // identity/read-only → ALLOWED, routing/protocol → BLOCKED,
            // anything unrecognized → UNKNOWN_BLOCKED (deny by default).
            val atParam = s.substring(2).trim()
            val mnemonic = atParam.takeWhile { it.isLetter() }.uppercase()
            return when {
                atParam.startsWith("@") -> CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION
                mnemonic in SAFE_AT_MNEMONICS -> CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION
                mnemonic in BLOCKED_AT_MNEMONICS -> CommandSafetyVerdict.BLOCKED_UNSAFE
                else -> CommandSafetyVerdict.UNKNOWN_BLOCKED
            }
        }

        val service = if (s.length >= 2) s.substring(0, 2) else s
        return when {
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

        /**
         * §11: AT mnemonics that are terminal-safe (identity / read-only
         * formatting / read commands).
         */
        private val SAFE_AT_MNEMONICS = setOf(
            "I",     // ATI  — adapter identification
            "RV",    // ATRV — read voltage
            "DP",    // ATDP — describe protocol
            "DPN",   // ATDPN — describe protocol number
            "E",     // ATE0/1 — echo off/on
            "H",     // ATH0/1 — headers off/on
            "S",     // ATS0/1 — spaces off/on
            "L",     // ATL0/1 — linefeeds off/on
            "Z",     // ATZ — reset adapter
            "AT",    // ATAT0/1 — automatic timing on/off
        )

        /**
         * §11: AT mnemonics that reconfigure routing, filtering, or protocol
         * parameters — must NOT be available in the raw terminal because they
         * can poison the active diagnostic session.
         */
        private val BLOCKED_AT_MNEMONICS = setOf(
            "SH",    // ATSH   — set header
            "CRA",   // ATCRA  — CAN receive address filter
            "SP",    // ATSP   — set protocol
            "TP",    // ATTP   — set protocol parameters
            "CAF",   // ATCAF  — CAN automatic framing
            "CFC",   // ATCFC  — CAN fractional counts
            "AF",    // ATAF   — auto framing
            "EXST",  // ATEXST — extended addressing
            "ST",    // ATST   — set timeout
            "CM",    // ATCM   — CAN mask
            "CS",    // ATCS   — CAN filter set
            "CF",    // ATCF   — CAN filter clear
            "CSM",   // ATCSM  — CAN service mode
            "AL",    // ATAL   — allow long messages
            "BD",    // ATBD   — set baud rate
            "BR",    // ATBR   — baud rate
        )
    }
}
