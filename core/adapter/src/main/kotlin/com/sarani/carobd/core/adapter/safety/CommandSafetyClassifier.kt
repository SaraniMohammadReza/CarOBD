package com.sarani.carobd.core.adapter.safety

import com.sarani.carobd.core.model.CommandSafetyVerdict

/** UI/use-case feedback only. The adapter independently enforces the dispatch policy. */
class CommandSafetyClassifier {
    fun classify(rawCommand: String): CommandSafetyVerdict {
        val c = SingleCommandParser.canonicalize(rawCommand) ?: return CommandSafetyVerdict.UNKNOWN_BLOCKED
        val authorized = AuthorizedDiagnosticCommand.readOnly(rawCommand)
        if (authorized != null) return if (authorized.risk == CommandRisk.SAFE_READ)
            CommandSafetyVerdict.ALLOWED_READ_ONLY else CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION
        if (c == "04") return CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW
        if (c.startsWith("AT") || c.take(2) in setOf("08", "10", "11", "14", "27", "2E", "2F", "30", "31", "34", "35", "36", "37", "3B", "85"))
            return CommandSafetyVerdict.BLOCKED_UNSAFE
        return CommandSafetyVerdict.UNKNOWN_BLOCKED
    }
    fun isSafeForTerminal(rawCommand: String): Boolean = AuthorizedDiagnosticCommand.readOnly(rawCommand) != null
}
