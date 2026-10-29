package com.sarani.carobd.core.adapter.safety

/** Exactly one ASCII ELM command. Controls are rejected before any normalization. */
object SingleCommandParser {
    const val MAX_LENGTH = 64
    fun canonicalize(raw: String): String? {
        if (raw.isEmpty() || raw.length > MAX_LENGTH || raw.any { it.code !in 32..126 }) return null
        val text = raw.trim().uppercase()
        if (text.isEmpty() || text.any { !it.isLetterOrDigit() && it != ' ' && it != '@' }) return null
        val parts = text.split(' ').filter { it.isNotEmpty() }
        // Hex parameters must be whole bytes when separated; AT spelling is allowlisted below.
        if (!text.startsWith("AT") && text != "STI" && parts.size > 1 && parts.any { it.length != 2 }) return null
        val canonical = parts.joinToString("")
        return canonical.takeIf { it.isNotEmpty() }
    }
}

enum class CommandRisk { SAFE_READ, ADAPTER_IDENTITY_READ, INTERNAL_SESSION_CONFIGURATION,
    PRIVILEGED_STATE_CHANGE, ACTUATOR_CONTROL, SECURITY_PROGRAMMING, UNKNOWN }

/** Constructor cannot be used to smuggle a string past the policy. */
class AuthorizedDiagnosticCommand private constructor(val canonical: String, val risk: CommandRisk) {
    companion object {
        private val identity = setOf("ATI", "ATRV", "ATDP", "ATDPN")
        private val internalConfiguration = setOf("ATZ", "ATE0", "ATL0", "ATS0", "ATAT1", "ATCAF1", "ATCSM0") +
            ("0123456789A".map { "ATSP$it" })

        fun readOnly(raw: String): AuthorizedDiagnosticCommand? {
            val c = SingleCommandParser.canonicalize(raw) ?: return null
            if (c in identity) return AuthorizedDiagnosticCommand(c, CommandRisk.ADAPTER_IDENTITY_READ)
            if (!c.matches(Regex("[0-9A-F]+")) || c.length % 2 != 0) return null
            val allowed = when (c.take(2)) {
                "01", "06", "09" -> c.length == 4
                "02" -> c.length == 6
                "03", "07", "0A" -> c.length == 2
                else -> false // No arbitrary DID/manufacturer/session/clear/control requests.
            }
            return if (allowed) AuthorizedDiagnosticCommand(c, CommandRisk.SAFE_READ) else null
        }

        // Module-internal and only used by the adapter's private initialization path.
        internal fun initialization(raw: String): AuthorizedDiagnosticCommand? {
            readOnly(raw)?.let { return it }
            val c = SingleCommandParser.canonicalize(raw) ?: return null
            if (c in setOf("AT@1", "STI")) return AuthorizedDiagnosticCommand(c, CommandRisk.ADAPTER_IDENTITY_READ)
            return if (c in internalConfiguration) AuthorizedDiagnosticCommand(c, CommandRisk.INTERNAL_SESSION_CONFIGURATION) else null
        }

        internal fun simulatorClear(): AuthorizedDiagnosticCommand =
            AuthorizedDiagnosticCommand("04", CommandRisk.PRIVILEGED_STATE_CHANGE)
    }
}
