package com.sarani.carobd.domain.repository

import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.core.model.ConnectionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ObdRepository {
    val activeConnectionProfile: StateFlow<ConnectionProfile?> get() = kotlinx.coroutines.flow.MutableStateFlow(null)
    val connectingProfile: StateFlow<ConnectionProfile?> get() = kotlinx.coroutines.flow.MutableStateFlow(null)
    val historyWarning: StateFlow<String?> get() = kotlinx.coroutines.flow.MutableStateFlow(null)
    val foregroundSessionId: StateFlow<String?> get() = kotlinx.coroutines.flow.MutableStateFlow(null)
    fun foregroundServiceReady(sessionId: String) {}
    fun foregroundServiceFailed(sessionId: String, reason: String) {}
    val connectionState: StateFlow<ConnectionState>
    val activeVehicleProfile: StateFlow<VehicleProfile?>
    val liveTelemetry: StateFlow<LiveTelemetry>

    suspend fun connect(type: ConnectionType, target: String): Result<Unit>

    /**
     * §11 profile-driven connection: builds the transport via the transport
     * factory from a persisted [ConnectionProfile] (never a misrouted type).
     * Implementations MUST use [com.sarani.carobd.data.transport.TransportFactory].
     */
    suspend fun connect(profile: ConnectionProfile): Result<Unit>
    /** Takes exclusive session ownership, executes only controlled initialization/reads,
     * retains stage evidence, and closes the test session before returning. */
    suspend fun testConnection(
        profile: ConnectionProfile,
        onProgress: (com.sarani.carobd.core.model.ConnectionTestReport) -> Unit = {},
    ): com.sarani.carobd.core.model.ConnectionTestReport =
        com.sarani.carobd.core.model.ConnectionTestReport(profile.transport.type == com.sarani.carobd.core.model.TransportType.SIMULATOR)
            .finishFailure("Connection testing not implemented")
    suspend fun disconnect()
    suspend fun setVehicleProfile(profile: VehicleProfile)
    suspend fun readEvidence(command: String, expectedSessionId: String? = null): Result<List<com.sarani.carobd.core.model.DiagnosticWireResponse>> = Result.failure(UnsupportedOperationException("Evidence reads unavailable"))
    suspend fun recentDiagnosticReports(): Result<List<com.sarani.carobd.core.model.DiagnosticSessionReport>> = Result.success(emptyList())
    suspend fun diagnosticHistory(): Result<List<com.sarani.carobd.core.model.DtcHistorySession>> = Result.success(emptyList())
    suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>>
    suspend fun clearDiagnosticTroubleCodes(): Result<Unit>
    suspend fun sendRawCommand(command: String): Result<String>
    fun startTelemetryLoop(): Flow<LiveTelemetry>
    fun stopTelemetryLoop()
}
