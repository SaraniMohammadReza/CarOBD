package com.sarani.carobd.core.adapter.safety

import com.sarani.carobd.core.model.CommandSafetyVerdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandSafetyClassifierTest {

    private val c = CommandSafetyClassifier()

    @Test
    fun allowedReadOnlyCommands() {
        c.classify("0100").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
        c.classify("01 0C").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
        c.classify("03").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
        c.classify("09 02").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
        c.classify("0A").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
        c.classify("02 02 01").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)   // freeze-frame request (read)
        c.classify("05 00").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)     // O2 sensor monitor (read)
        c.classify("06 01").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)     // monitor result (read)
        c.classify("22 F1 90").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY) // UDS ReadDataById (read)
        // Whitespace/oddities are normalized.
        c.classify("  0100  ").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
        c.classify("01  00").assertIs(CommandSafetyVerdict.ALLOWED_READ_ONLY)
    }

    @Test
    fun allowedAdapterIdentityCommands() {
        // §11: safe identity / read-only AT commands.
        c.classify("ATI").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATRV").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATDP").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATDPN").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATE0").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATE1").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATH0").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATH1").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATS0").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATS1").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATL0").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATL1").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATZ").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATAT0").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATAT1").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("AT@1").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("AT@2").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
    }

    @Test
    fun blockedRoutingAndProtocolATCommands() {
        // §11: routing / filtering / protocol commands block the terminal.
        c.classify("ATSH 7E8").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATSH")
        c.classify("ATSH7E8").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATSH compact")
        c.classify("ATCRA 7E8").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCRA")
        c.classify("ATSP0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATSP")
        c.classify("ATSP 6").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATSP with arg")
        c.classify("ATTP 6").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATTP")
        c.classify("ATCAF1").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCAF")
        c.classify("ATCFC0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCFC")
        c.classify("ATAF0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATAF")
        c.classify("ATEXST0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATEXST")
        c.classify("ATST50").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATST")
        c.classify("ATCM0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCM")
        c.classify("ATCS0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCS")
        c.classify("ATCF0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCF")
        c.classify("ATCSM0").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATCSM")
        c.classify("ATAL").assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, "ATAL")
        // None of these are terminal-safe.
        assertFalse(c.isSafeForTerminal("ATSH 7E8"))
        assertFalse(c.isSafeForTerminal("ATSP0"))
        assertFalse(c.isSafeForTerminal("ATCAF1"))
    }

    @Test
    fun unknownATCommandsAreBlockedByDefault() {
        // §11: unknown AT commands must NOT be blindly allowed.
        c.classify("ATXYZ").assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, "ATXYZ")
        c.classify("ATXYZ999").assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, "ATXYZ999")
        c.classify("ATX").assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, "ATX")
        c.classify("ATBRK").assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, "ATBRK")
        c.classify("AT0").assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, "AT0")
        // Just "AT" with no mnemonic → unknown.
        c.classify("AT").assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, "AT alone")
        assertFalse(c.isSafeForTerminal("ATXYZ"))
    }

    @Test
    fun mode04RequiresDedicatedFlow() {
        // Raw terminal must NOT send 04; the dedicated confirmed clear flow does.
        c.classify("04").assertIs(CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW)
        c.classify("04 00").assertIs(CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW)
        assertFalse(c.isSafeForTerminal("04"))
    }

    @Test
    fun blockedUnsafeCommands() {
        for (cmd in listOf("08", "27 01", "2E F1 90 01", "2F 01", "31 01 02",
            "34 00 01", "35 47", "36 01", "37 00")) {
            c.classify(cmd).assertIs(CommandSafetyVerdict.BLOCKED_UNSAFE, cmd)
            assertFalse(c.isSafeForTerminal(cmd))
        }
    }

    @Test
    fun unknownCommandsAreBlockedByDefault() {
        for (cmd in listOf("", "  ", "ZZ", "99", "FE", "10 01")) {
            c.classify(cmd).assertIs(CommandSafetyVerdict.UNKNOWN_BLOCKED, cmd)
            assertFalse(c.isSafeForTerminal(cmd))
        }
    }

    @Test
    fun readCommandsAreSafeForTerminal() {
        assertTrue(c.isSafeForTerminal("0100"))
        assertTrue(c.isSafeForTerminal("ATI"))
        assertTrue(c.isSafeForTerminal("ATRV"))
        assertTrue(c.isSafeForTerminal("03"))
    }

    private fun CommandSafetyVerdict.assertIs(expected: CommandSafetyVerdict, label: String = "") {
        assertEquals("$label :: expected $expected but was $this", expected, this)
    }
}
