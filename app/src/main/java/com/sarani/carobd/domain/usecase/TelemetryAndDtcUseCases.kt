package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveTelemetryUseCase(private val repository: ObdRepository) {
    val telemetry: StateFlow<LiveTelemetry> = repository.liveTelemetry
    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val activeProfile: StateFlow<VehicleProfile> = repository.activeVehicleProfile
}

class ScanDtcUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke(): Result<List<DiagnosticCode>> {
        return repository.scanDiagnosticTroubleCodes()
    }
}

class ClearDtcUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.clearDiagnosticTroubleCodes()
    }
}

class SendRawCommandUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke(command: String): Result<String> {
        return repository.sendRawCommand(command)
    }
}

class SetVehicleProfileUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke(profile: VehicleProfile) {
        repository.setVehicleProfile(profile)
    }
}
