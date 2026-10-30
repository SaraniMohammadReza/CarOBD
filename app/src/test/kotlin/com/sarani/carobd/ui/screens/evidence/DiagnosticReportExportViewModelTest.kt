package com.sarani.carobd.ui.screens.evidence

import com.sarani.carobd.core.model.*
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiagnosticReportExportViewModelTest {
    @After fun after() { Dispatchers.resetMain() }
    private val report = DiagnosticSessionReport("saved", "old-session", 10, 20, null, null, null, null, true, null, null,
        listOf(DiagnosticReportOutcome("03", null, ScanOutcome.MALFORMED, rawLines = listOf("bad evidence"))))
    private inner class Repository : ObdRepository {
        override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val activeVehicleProfile = MutableStateFlow<VehicleProfile?>(null)
        override val liveTelemetry = MutableStateFlow(LiveTelemetry())
        override suspend fun recentDiagnosticReports() = Result.success(listOf(report))
        override suspend fun connect(type: ConnectionType, target: String): Result<Unit> = error("No hardware path allowed")
        override suspend fun connect(profile: ConnectionProfile): Result<Unit> = error("No hardware path allowed")
        override suspend fun disconnect() = error("No hardware path allowed")
        override suspend fun setVehicleProfile(profile: VehicleProfile) = error("No profile mutation allowed")
        override suspend fun scanDiagnosticTroubleCodes(): Result<List<DiagnosticCode>> = error("No command path allowed")
        override suspend fun clearDiagnosticTroubleCodes(): Result<Unit> = error("No command path allowed")
        override suspend fun sendRawCommand(command: String): Result<String> = error("No command path allowed")
        override fun startTelemetryLoop() = error("No producer allowed")
        override fun stopTelemetryLoop() = error("No producer mutation allowed")
    }
    @Test fun savedReportExportsWithoutConnectionAndDuplicateActionWritesOnce() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val vm = DtcHistoryViewModel(Repository(), dispatcher); advanceUntilIdle()
        var writes = 0; var json = ""
        vm.export("saved") { writes++; json = it }
        vm.export("saved") { writes++ }
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals(DiagnosticReportExport.toJson(report), json)
        assertEquals(ReportExportStatus.SAVED, vm.exportStatus.value)
        vm.resetExportStatus(); assertEquals(ReportExportStatus.NONE, vm.exportStatus.value)
    }
    @Test fun documentWriteFailureIsNotReportedSavedOrRetried() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val vm = DtcHistoryViewModel(Repository(), dispatcher); advanceUntilIdle()
        var attempts = 0
        vm.export("saved") { attempts++; throw java.io.IOException("Destination unavailable") }
        advanceUntilIdle()
        assertEquals(1, attempts)
        assertEquals(ReportExportStatus.FAILED, vm.exportStatus.value)
        assertEquals("Destination unavailable", vm.error.value)
        assertEquals(listOf(report), vm.reports.value)
    }
    @Test fun missingSelectionNeverWritesAnotherReport() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
        val vm = DtcHistoryViewModel(Repository(), dispatcher); advanceUntilIdle()
        var writes = 0
        vm.export("missing") { writes++ }; advanceUntilIdle()
        assertEquals(0, writes); assertEquals(ReportExportStatus.FAILED, vm.exportStatus.value)
    }
}
