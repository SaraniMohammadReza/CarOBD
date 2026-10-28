package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.CommandSafetyViolation
import com.sarani.carobd.core.model.CommandSafetyVerdict
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * P0 safety (§4): SendRawCommandUseCase is a defence-in-depth gate. Blocked
 * commands (Mode 04 clear, write/program/sec-access services, unknown) must
 * FAIL FAST and never reach the adapter. The dedicated Mode 04 clear flow
 * calls `adapter.executeCommand("04")` directly and is unaffected.
 */
class SendRawCommandUseCaseTest {

    private class FakeRepo : ObdRepository {
        var lastCommand: String? = null
        var rawInvocations = 0
        private val state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        private val veh = MutableStateFlow<VehicleProfile?>(null)
        private val tel = MutableStateFlow(LiveTelemetry())
        override val connectionState: StateFlow<ConnectionState> = state.asStateFlow()
        override val activeVehicleProfile: StateFlow<VehicleProfile?> = veh.asStateFlow()
        override val liveTelemetry: StateFlow<LiveTelemetry> = tel.asStateFlow()
        override suspend fun connect(type: ConnectionType, target: String) = Result.success(Unit)
        override suspend fun disconnect() {}
        override suspend fun setVehicleProfile(profile: VehicleProfile) {}
        override suspend fun scanDiagnosticTroubleCodes() = Result.success(emptyList<DiagnosticCode>())
        override suspend fun clearDiagnosticTroubleCodes() = Result.success(Unit)
        override suspend fun sendRawCommand(command: String): Result<String> {
            rawInvocations++
            lastCommand = command
            return Result.success("ok: $command")
        }
        override fun startTelemetryLoop() = kotlinx.coroutines.flow.emptyFlow<LiveTelemetry>()
        override fun stopTelemetryLoop() {}
    }

    @Test
    fun mode04ClearIsBlockedInRawTerminal() = runTest {
        val repo = FakeRepo()
        val uc = SendRawCommandUseCase(repo)
        val r = uc("04")
        assertTrue(r.isFailure)
        val e = r.exceptionOrNull() as CommandSafetyViolation
        assertEquals(CommandSafetyVerdict.REQUIRES_DEDICATED_CONFIRMED_FLOW, e.verdict)
        // Never reached the adapter.
        assertEquals(0, repo.rawInvocations)
        assertNull(repo.lastCommand)
    }

    @Test
    fun writeAndSecAccessServicesAreBlocked() = runTest {
        val repo = FakeRepo()
        val uc = SendRawCommandUseCase(repo)
        for (cmd in listOf("27 01", "2E F190 01", "2F 01", "31 01 02",
            "34 00 01", "35 47", "36 01", "37 00", "08")) {
            val r = uc(cmd)
            assertTrue("$cmd should be blocked", r.isFailure)
            assertTrue(r.exceptionOrNull() is CommandSafetyViolation)
        }
        assertEquals(0, repo.rawInvocations)
    }

    @Test
    fun unknownCommandsAreBlocked() = runTest {
        val repo = FakeRepo()
        val uc = SendRawCommandUseCase(repo)
        val r = uc("ZZ")
        assertTrue(r.isFailure)
        assertEquals(0, repo.rawInvocations)
    }

    @Test
    fun readOnlyAndAdapterConfigCommandsDelegate() = runTest {
        val repo = FakeRepo()
        val uc = SendRawCommandUseCase(repo)
        assertTrue(uc("0100").isSuccess)
        assertTrue(uc("03").isSuccess)
        assertTrue(uc("09 02").isSuccess)
        assertTrue(uc("ATI").isSuccess)
        assertTrue(uc("ATRV").isSuccess)
        assertEquals(5, repo.rawInvocations)
    }

    @Test
    fun terminalLogShowsFailureForBlockedCommand() = runTest {
        val repo = FakeRepo()
        val uc = SendRawCommandUseCase(repo)
        val r = uc("2E F190 01")
        assertFalse(r.isSuccess)
        // Error message surfaces to TerminalLog.isSuccess = false path.
        assertTrue(r.exceptionOrNull()!!.message!!.isNotEmpty())
    }
}
