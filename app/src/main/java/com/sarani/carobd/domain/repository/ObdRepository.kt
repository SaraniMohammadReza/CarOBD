package com.sarani.carobd.domain.repository

import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ConnectionType
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.VehicleProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ObdRepository {
    val connectionState: StateFlow<ConnectionState>
    val activeVehicleProfile: StateFlow<VehicleProfile?>
    val liveTelemetry: StateFlow<LiveTelemetry>

    suspend fun connect(type: ConnectionType, target: String): Result<Unit>
    suspend fun disconnect()
    suspend fun setVehicleProfile(profile: VehicleProfile)
    suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>>
    suspend fun clearDiagnosticTroubleCodes(): Result<Unit>
    suspend fun sendRawCommand(command: String): Result<String>
    fun startTelemetryLoop(): Flow<LiveTelemetry>
    fun stopTelemetryLoop()
}
