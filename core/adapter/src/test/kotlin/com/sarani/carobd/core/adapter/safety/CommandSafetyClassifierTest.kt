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
    fun allowedAdapterConfiguration() {
        c.classify("ATI").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATRV").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATDP").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATDPN").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("ATE0").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
        c.classify("AT SH 7E8").assertIs(CommandSafetyVerdict.ALLOWED_ADAPTER_CONFIGURATION)
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
        assertEquals("$label :: ", expected, this)
    }
}
