package com.sarani.carobd.ui.screens.diagnostics

import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.repository.ObdRepository
import com.sarani.carobd.domain.usecase.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticsCancellationTest {
    private class CanceledRepository : ObdRepository {
        override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val activeVehicleProfile = MutableStateFlow<VehicleProfile?>(null)
        override val liveTelemetry = MutableStateFlow(LiveTelemetry())
        override suspend fun connect(type: ConnectionType, target: String) = Result.success(Unit)
        override suspend fun connect(profile: ConnectionProfile) = Result.success(Unit)
        override suspend fun disconnect() {}
        override suspend fun setVehicleProfile(profile: VehicleProfile) {}
        override suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>> = throw CancellationException("Session disconnected")
        override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = throw CancellationException("Session disconnected")
        override suspend fun sendRawCommand(command: String) = Result.failure<String>(IllegalStateException("Disconnected"))
        override fun startTelemetryLoop() = emptyFlow<LiveTelemetry>()
        override fun stopTelemetryLoop() {}
    }
    @Test fun canceledOperationsNeverLeaveLoadingOrSuccessfulEmptyScan() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = CanceledRepository()
            val vm = DiagnosticsViewModel(ScanDtcUseCase(repository), ClearDtcUseCase(repository))
            vm.onIntent(DiagnosticsIntent.ScanCodes); runCurrent()
            assertFalse(vm.uiState.value.isScanning)
            assertFalse(vm.uiState.value.scanCompletedSuccessfully)
            assertTrue(vm.uiState.value.lastScanMessage!!.contains("canceled"))
            vm.onIntent(DiagnosticsIntent.ConfirmClearCodes); runCurrent()
            assertFalse(vm.uiState.value.isClearing)
        } finally { Dispatchers.resetMain() }
    }
}
