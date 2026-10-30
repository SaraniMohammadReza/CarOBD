package com.sarani.carobd.domain.usecase

import com.sarani.carobd.core.adapter.safety.CommandSafetyClassifier
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.DiagnosticCode
import com.sarani.carobd.core.model.LiveTelemetry
import com.sarani.carobd.core.model.VehicleProfile
import com.sarani.carobd.domain.repository.ObdRepository
import com.sarani.carobd.core.model.CommandSafetyViolation
import kotlinx.coroutines.flow.StateFlow

class ObserveTelemetryUseCase(private val repository: ObdRepository) {
    val telemetry: StateFlow<LiveTelemetry> = repository.liveTelemetry
    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val sessionToken = repository.foregroundSessionId
    val activeConnectionProfile = repository.activeConnectionProfile
    val connectingProfile = repository.connectingProfile
    val activeProfile: StateFlow<VehicleProfile?> = repository.activeVehicleProfile
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
    private val safety = CommandSafetyClassifier()

    suspend operator fun invoke(command: String): Result<String> {
        // §4 P0: defence-in-depth. The repository is the authoritative boundary,
        // but fail fast here so the UI never initiates a dangerous raw command.
        if (!safety.isSafeForTerminal(command)) {
            return Result.failure(
                CommandSafetyViolation(
                    verdict = safety.classify(command),
                    rawCommand = command,
                    reasonFa = "این دستور از ترمینال مسدود شده است. برای واضح‌سازی دستگاه از منوی Diagnostics استفاده کنید.",
                    reasonEn = "Blocked in raw terminal; use the Diagnostics clear flow for Mode 04."
                )
            )
        }
        return repository.sendRawCommand(command)
    }
}

class SetVehicleProfileUseCase(private val repository: ObdRepository) {
    suspend operator fun invoke(profile: VehicleProfile) {
        repository.setVehicleProfile(profile)
    }
}
