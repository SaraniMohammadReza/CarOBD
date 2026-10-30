package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class EvidenceAdmissionTest {
    private class EmptyEvidence : ObdRepository {
        val writes = mutableListOf<String>()
        override val foregroundSessionId = MutableStateFlow<String?>("session")
        override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Connected("bench", "ELM", ProtocolStandard.ISO_15765_4_CAN_11_500.displayName, "", 0f))
        override val activeVehicleProfile = MutableStateFlow<VehicleProfile?>(null)
        override val liveTelemetry = MutableStateFlow(LiveTelemetry())
        override suspend fun readEvidence(command: String, expectedSessionId: String?): Result<List<DiagnosticWireResponse>> { writes += command; return Result.success(emptyList()) }
        override suspend fun connect(type: ConnectionType, target: String) = error("No physical transport")
        override suspend fun connect(profile: ConnectionProfile) = error("No physical transport")
        override suspend fun disconnect() {}
        override suspend fun setVehicleProfile(profile: VehicleProfile) {}
        override suspend fun scanDiagnosticTroubleCodes() = error("Not used")
        override suspend fun clearDiagnosticTroubleCodes() = error("Disabled")
        override suspend fun sendRawCommand(command: String) = error("No bypass")
        override fun startTelemetryLoop() = emptyFlow<LiveTelemetry>()
        override fun stopTelemetryLoop() {}
    }
    @Test fun emptyDiscoveryIsMalformedAndNeverTriggersSpeculativeReads() = runTest {
        val r = EmptyEvidence()
        assertEquals(ReadingState.Malformed, FreezeFrameUseCase(r)(7).single().state)
        assertEquals(listOf("020007"), r.writes); r.writes.clear()
        assertEquals(ReadingState.Malformed, VehicleInformationUseCase(r)().single().reading.state)
        assertEquals(listOf("0900"), r.writes); r.writes.clear()
        assertEquals(ReadingState.Malformed, MonitorTestsUseCase(r)(1).single().state)
        assertEquals(listOf("0600"), r.writes)
    }
    @Test fun canSubstringDoesNotAuthorizeUnverifiedMonitorLayout() = runTest {
        val r = EmptyEvidence()
        for (protocol in listOf("UNKNOWN CAN", ProtocolStandard.SAE_J1939_CAN.displayName, ProtocolStandard.ISO_14229_UDS_CAN.displayName)) {
            r.connectionState.value = (r.connectionState.value as ConnectionState.Connected).copy(protocolName = protocol)
            assertEquals(ReadingState.Unsupported, MonitorTestsUseCase(r)(1).single().state)
            assertTrue(r.writes.isEmpty())
        }
    }
}
