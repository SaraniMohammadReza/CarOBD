package com.sarani.carobd.data.repository

import com.sarani.carobd.core.adapter.profile.InMemoryConnectionProfileRepository
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class ObdRepositorySafetyTest {
    private class Physical(val can: Boolean = false) : ObdTransport {
        override val transportType = TransportType.WIFI_TCP
        override var isConnected = false
        val rx = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
        override val incomingBytes = rx.asSharedFlow()
        val writes = java.util.Collections.synchronizedList(mutableListOf<String>())
        var malformed = false
        var overrides = emptyMap<String, String>()
        var blockConnect = false
        var connectGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        val connectEntered = kotlinx.coroutines.CompletableDeferred<Unit>()
        override suspend fun connect() { connectEntered.complete(Unit); if (blockConnect) { connectGate?.await() ?: kotlinx.coroutines.awaitCancellation() }; isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) {
            val c = data.toString(Charsets.US_ASCII).trim(); writes.add(c)
            val raw = overrides[c] ?: when(c) {
                "ATZ", "ATI" -> "ELM327"
                "ATRV" -> "12.0V"
                "ATDPN" -> if(can) "6" else "3"
                "0100" -> "410000000000"
                "03" -> if(malformed) "43 FFFFFFFF" else "43 01 33 00 00 00 00"
                "07" -> "47 01 33 00 00 00 00"
                "0A" -> "4A 01 33 00 00 00 00"
                else -> "OK"
            }
            rx.emit((raw + "\r>").toByteArray())
        }
    }
    private fun repository(scope: kotlinx.coroutines.CoroutineScope, physical: Physical, recordings: com.sarani.carobd.data.recording.RecordingController? = null, history: ((DtcHistorySession, List<DiagnosticCode>) -> Unit)? = null, admission: (TransportConfig) -> Unit = {}, reportSink: ((DiagnosticSessionReport) -> Unit)? = null, ongoingAdmission: (() -> Unit)? = null) = ObdRepositoryImpl(
        context = null,
        activeVehicleRepository = ActiveVehicleRepository(MutableStateFlow(null), {}, {}),
        appPreferencesManager = null,
        connectionProfiles = InMemoryConnectionProfileRepository(),
        scope = scope,
        ioDispatcher = scope.coroutineContext[kotlin.coroutines.ContinuationInterceptor] as kotlinx.coroutines.CoroutineDispatcher,
        pollModeFlow = MutableStateFlow(TelemetryPollMode.NORMAL),
        createTransport = { ongoingAdmission?.let { com.sarani.carobd.core.adapter.transport.AdmissionGuardedTransport(physical, it) } ?: physical }, admitTransport = admission, recordings = recordings, historySink = history, diagnosticReportSink = reportSink, startService = {}, stopService = {},
    )
    private val profile = ConnectionProfile("test", "bench", TransportConfig.Wifi("bench", 35000))
    @Test fun repository_actualScanPathPreservesAllThreeNonCanCategories() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        val result = repo.scanDiagnosticTroubleCodes()
        assertTrue(result.toString(), result.isSuccess)
        assertEquals(setOf(DtcCategory.STORED, DtcCategory.PENDING, DtcCategory.PERMANENT), result.getOrThrow().map { it.category }.toSet())
        assertTrue(result.getOrThrow().all { it.code == "P0133" && it.responderAddressHex == null })
        repo.disconnect()
    }
    @Test fun physicalDedicatedClearAndRawPathsNeverDispatch04() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        assertTrue(repo.clearDiagnosticTroubleCodes().isFailure)
        assertTrue(repo.sendRawCommand("04").isFailure)
        assertTrue(repo.sendRawCommand("ATI\r04").isFailure)
        repo.disconnect()
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        assertFalse(t.writes.any { it.contains("04") || it.contains('\r') })
        repo.disconnect()
    }
    @Test fun repository_actualMalformedScanIsFailure() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        t.malformed = true
        val result = repo.scanDiagnosticTroubleCodes()
        assertTrue(result.isFailure)
        assertEquals(ScanCompleteness.PARTIAL, (result.exceptionOrNull() as DiagnosticScanException).report.completeness)
        repo.disconnect()
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun repository_repeatedStartTelemetryIsIdempotent() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        repeat(20) { repo.startTelemetryLoop() }
        advanceTimeBy(1000); runCurrent()
        // One handshake plus one producer capability discovery. Unknown/empty
        // capability bitmap must produce no speculative baseline PID traffic.
        assertEquals(2, t.writes.count { it == "0100" })
        assertFalse(t.writes.any { it.startsWith("01") && it != "0100" })
        repo.disconnect()
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun repository_transportLossInvalidatesSession() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        runCurrent()
        t.isConnected = false
        advanceTimeBy(200); runCurrent()
        assertFalse(repo.connectionState.value is ConnectionState.Connected)
        assertTrue(repo.sendRawCommand("ATI").isFailure)
        repo.disconnect()
    }

    @Test fun repository_mixedCanEvidenceSurvivesActualBufferParser() = runTest {
        val t = Physical(can = true)
        t.overrides = mapOf("03" to "7E8:0443010133\r7E9 43 01 01 33\r7EA NO DATA\r7EB 43 02 01", "07" to "4700", "0A" to "4A00")
        val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        val failure = repo.scanDiagnosticTroubleCodes().exceptionOrNull() as DiagnosticScanException
        assertEquals(ScanCompleteness.PARTIAL, failure.report.completeness)
        assertEquals(setOf("7E8", "7E9"), failure.report.codes.map { it.responderAddressHex }.toSet())
        assertTrue(failure.report.responders.any { it.responder == "7EA" && it.outcome == ScanOutcome.NO_DATA })
        assertTrue(failure.report.responders.any { it.responder == "7EB" && it.outcome == ScanOutcome.MALFORMED })
        repo.disconnect()
    }
    @Test fun concurrentConnectIsRejectedAndDisconnectCancelsPendingConnect() = runTest {
        val t = Physical(); t.blockConnect = true
        val repo = repository(backgroundScope, t)
        val first = kotlinx.coroutines.CoroutineScope(coroutineContext).launch { repo.connect(profile) }
        t.connectEntered.await()
        assertTrue(repo.connect(profile).isFailure)
        repo.disconnect(); first.join()
        assertTrue(first.isCancelled)
        assertFalse(t.isConnected)
        assertTrue(t.writes.isEmpty())
        assertEquals(ConnectionState.Disconnected, repo.connectionState.value)
    }
    @Test fun legacyWifiNeverGuessesMissingOrMalformedPort() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        for (target in listOf("", "bench", "bench:invalid", "bench:35000:ignored", ":35000")) {
            assertTrue(target, repo.connect(ConnectionType.WIFI_TCP, target).isFailure)
        }
        assertTrue(t.writes.isEmpty())
    }

    @Test fun wizardRunsRealReadOnlyHandshakeAndClosesPhysicalSession() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        val progress = mutableListOf<ConnectionTestReport>()
        val report = repo.testConnection(profile) { progress.add(it) }
        assertTrue(report.toString(), report.passed)
        assertFalse(report.isSimulator)
        assertTrue(progress.any { it.stages.any { stage -> stage.status == ConnectionTestStatus.RUNNING } })
        assertTrue(report.stages.first { it.stage == ConnectionTestStage.ECU }.evidence.contains("4100"))
        assertTrue(t.writes.contains("0100"))
        assertFalse(t.writes.any { it == "04" || it.startsWith("08") || it.startsWith("27") })
        assertFalse(t.isConnected)
        assertEquals(ConnectionState.Disconnected, repo.connectionState.value)
    }
    @Test fun wizardUnknownReadbackCannotPassAndRetainsEcuEvidence() = runTest {
        val t = Physical(); t.overrides = mapOf("ATDPN" to "UNKNOWN")
        val repo = repository(backgroundScope, t)
        val report = repo.testConnection(profile) {}
        assertFalse(report.passed)
        assertEquals(ConnectionTestStatus.FAIL, report.stages.first { it.stage == ConnectionTestStage.PROTOCOL }.status)
        assertEquals(ConnectionTestStatus.PASS, report.stages.first { it.stage == ConnectionTestStage.ECU }.status)
        assertFalse(t.isConnected)
    }

    @Test fun bluetoothPermission_deniedDoesNotTouchAdapter() = runTest {
        var created = false
        val repo = ObdRepositoryImpl(null, ActiveVehicleRepository(MutableStateFlow(null), {}, {}), null,
            InMemoryConnectionProfileRepository(), scope = backgroundScope,
            pollModeFlow = MutableStateFlow(TelemetryPollMode.NORMAL),
            createTransport = { created = true; Physical() },
            admitTransport = { throw AccessDeniedException(RuntimeAccess.Connect, AccessState.Denied) },
            startService = {}, stopService = {})
        val result = repo.connect(ConnectionProfile("bt", "exact", TransportConfig.BluetoothClassic("AA:BB:CC:DD:EE:FF")))
        assertTrue(result.exceptionOrNull() is AccessDeniedException)
        assertFalse(created)
        assertTrue(repo.connectionState.value is ConnectionState.Failure)
    }
    @Test fun foregroundServiceFailureInvalidatesVisibleSessionState() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        val old = requireNotNull(repo.foregroundSessionId.value)
        repo.foregroundServiceFailed(old, "platform rejected")
        runCurrent()
        assertFalse(t.isConnected)
        assertTrue(repo.connectionState.value is ConnectionState.Failure)
        assertNull(repo.foregroundSessionId.value)
        repo.connect(profile).let { assertTrue(it.toString(), it.isSuccess) }
        repo.foregroundServiceFailed(old, "late callback")
        runCurrent()
        assertTrue(repo.connectionState.value is ConnectionState.Connected)
        repo.disconnect()
    }

    @Test fun repository_readinessUsesProductionEnvelope() = runTest {
        val t = Physical(); t.overrides = mapOf("0101" to "41 01 83 0F 6B 20")
        val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val read = com.sarani.carobd.domain.usecase.ReadinessUseCase(repo)
        val valid = read().single()
        assertEquals(ReadingState.Success, valid.state)
        assertEquals(IgnitionType.Compression, valid.value!!.ignitionType)
        assertNull(valid.evidence.single().responderId)
        for ((raw, state) in listOf("41 01 00" to ReadingState.Malformed, "7F 01 12" to ReadingState.NegativeResponse, "NO DATA" to ReadingState.NoData)) {
            t.overrides = mapOf("0101" to raw)
            val r = read().single()
            assertEquals(raw, state, r.state)
            assertNull(r.value)
        }
        repo.disconnect()
        assertEquals(ReadingState.Disconnected, read().single().state)
    }

    @Test fun mode09ProductionPathPreservesVinCvnAndNegativePeer() = runTest {
        val t = Physical()
        val vin = "1D4GP00R55B123456"
        fun hex(s: String) = s.toByteArray().joinToString("") { "%02X".format(it) }
        t.overrides = mapOf("0900" to "4900FCC00000", "0901" to "490101", "0902" to "7E8 490201${hex(vin)}\r7E9 7F 09 12",
            "0903" to "490301", "0904" to "490401${hex("CAL-A".padEnd(16, '\u0000'))}", "0905" to "490501",
            "0906" to "4906011234ABCD", "0909" to "490901", "090A" to "490A01${hex("Engine ECU".padEnd(20, '\u0000'))}")
        val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val result = com.sarani.carobd.domain.usecase.VehicleInformationUseCase(repo)()
        assertEquals(listOf(vin), result.first { it.pid == 2 && it.reading.state == ReadingState.Success }.reading.value!!.values)
        assertTrue(result.any { it.pid == 2 && it.reading.state == ReadingState.NegativeResponse && it.reading.evidence.single().responderId == "7E9" })
        assertEquals(listOf("1234ABCD"), result.first { it.pid == 6 }.reading.value!!.values)
        assertFalse(t.writes.any { it == "04" || it.startsWith("08") })
        repo.disconnect()
    }

    @Test fun freezeFrameProductionRepositoryPath() = runTest {
        val t = Physical(); t.overrides = mapOf("020007" to "42000700180000", "020C07" to "7E8 420C071AF8\r7E9 7F 02 12", "020D07" to "420D0755")
        val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val r = com.sarani.carobd.domain.usecase.FreezeFrameUseCase(repo)(7)
        assertTrue(r.any { it.value?.pid == 12 && it.value?.value == 1726.0 && it.value?.frameIndex == 7 })
        assertTrue(r.any { it.state == ReadingState.NegativeResponse && it.evidence.single().responderId == "7E9" })
        assertFalse(t.writes.contains("020C00"))
        assertFalse(t.writes.any { it == "04" || it.startsWith("08") })
        repo.disconnect()
    }

    @Test fun recordingObservesProductionWireWithoutTrimmingAndPersists() = runTest {
        val directory = java.nio.file.Files.createTempDirectory("recording-production").toFile()
        try {
            val store = com.sarani.carobd.core.adapter.transport.FileTranscriptStore(directory)
            val recording = com.sarani.carobd.data.recording.RecordingController(store, backgroundScope)
            val t = Physical(); t.overrides = mapOf("0101" to "41 01 83 07 65 00")
            val repo = repository(backgroundScope, t, recording)
            assertTrue(repo.connect(profile).isSuccess)
            recording.start()
            assertTrue(repo.readEvidence("0101").isSuccess)
            recording.stop()
            val capture = store.read(store.list().single().sessionId)
            assertEquals("0101\r", capture.events.first { it.direction == Direction.TX && it.command == "0101" }.rawHex)
            assertEquals("41 01 83 07 65 00\r>", capture.events.first { it.direction == Direction.RX && it.rawHex.startsWith("41 01") }.rawHex)
            assertTrue(capture.events.any { it.command == "0101" && it.protocolMetadata["event"] == "parsed" && it.outcome == DiagnosticOutcome.POSITIVE })
            assertNull(capture.events.first { it.direction == Direction.RX }.responder)
            assertFalse(capture.metadata.vehicleProtocol.isNullOrBlank())
            repo.disconnect()
        } finally { directory.deleteRecursively() }
    }

    @Test fun completedScanHistoryPreservesEvidenceAndDoesNotInventMil() = runTest {
        var saved: DtcHistorySession? = null
        val repo = repository(backgroundScope, Physical(), history = { scan, _ -> saved = scan })
        assertTrue(repo.connect(profile).isSuccess)
        assertTrue(repo.scanDiagnosticTroubleCodes().isSuccess)
        val h = requireNotNull(saved)
        assertEquals(setOf(DtcCategory.STORED, DtcCategory.PENDING, DtcCategory.PERMANENT), h.codes.map { it.category }.toSet())
        assertNull(h.mil)
        assertTrue(h.codes.all { it.mil == null && it.responder == null && it.protocol == h.protocol })
        assertEquals((repo.connectionState.value as ConnectionState.Connected).protocolName, h.protocol)
        assertEquals(repo.foregroundSessionId.value, h.connectionSessionId)
        repo.disconnect()
    }

    @Test fun mode06ProductionPathKeepsUnknownScalingAndFailedPeers() = runTest {
        val t = Physical(can = true); t.overrides = mapOf("0600" to "7E8 460080000000\r7E9 7F 06 12", "0601" to "7E8 460101FE80010000FFFF")
        val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val r = com.sarani.carobd.domain.usecase.MonitorTestsUseCase(repo)(1)
        assertTrue(r.any { it.state == ReadingState.NegativeResponse && it.evidence.single().responderId == "7E9" })
        val record = r.first { it.value != null }.value!!.single()
        assertEquals(32769, record.rawMeasured); assertNull(record.passed); assertNull(record.measured)
        assertFalse(t.writes.any { it == "04" || it.startsWith("08") || it.startsWith("05") })
        repo.disconnect()
    }
    @Test fun mode06DoesNotApplyCanLayoutToNonCanOrBlindlyReadMonitor() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val r = com.sarani.carobd.domain.usecase.MonitorTestsUseCase(repo)(1)
        assertEquals(ReadingState.Unsupported, r.single().state)
        assertFalse(t.writes.any { it.startsWith("06") })
        repo.disconnect()
    }

    @Test fun benchCaptureIncludesInitializationWithoutAddingCommands() = runTest {
        val directory = java.nio.file.Files.createTempDirectory("bench-production").toFile()
        try {
            val store = com.sarani.carobd.core.adapter.transport.FileTranscriptStore(directory)
            val controller = com.sarani.carobd.data.recording.RecordingController(store, backgroundScope)
            controller.enableBench(true)
            val transport = Physical()
            val repo = repository(backgroundScope, transport, controller)
            val report = repo.testConnection(profile) {}
            assertTrue(report.toString(), report.passed)
            // Final save is asynchronous; explicit capture stop is unnecessary after test session closes.
            runCurrent()
            val captured = store.read(store.list().single().sessionId)
            assertEquals(transport.writes, captured.events.filter { it.direction == Direction.TX }.map { it.command })
            assertTrue(captured.events.any { it.command == "ATI" && it.rawHex == "ATI\r" })
            assertTrue(captured.events.any { it.protocolMetadata["phase"] == "EcuResponding" && it.protocolMetadata["state"] == "PASS" })
            assertTrue(captured.events.any { it.protocolMetadata["phase"] == "SessionEnded" })
            assertFalse(captured.events.any { it.protocolMetadata.values.any { value -> value == "DEVICE_PASS" || value == "VEHICLE_PASS" } })
            assertFalse(transport.writes.any { it == "04" || it.startsWith("08") || it.startsWith("27") || it.startsWith("2E") })
        } finally { directory.deleteRecursively() }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun importedPhysicalMetadataReplayNeverUsesActivePhysicalSession() = runTest {
        val physical = Physical()
        val repo = repository(backgroundScope, physical)
        assertTrue(repo.connect(profile).isSuccess)
        runCurrent()
        val before = physical.writes.toList()
        for (type in listOf(TransportType.BLUETOOTH_CLASSIC, TransportType.BLUETOOTH_LE, TransportType.WIFI_TCP, TransportType.USB_SERIAL)) {
            val transcript = DiagnosticTranscript(TranscriptMetadata("imported", type), listOf(
                DiagnosticEvent(Direction.TX, 0, 0, "010D", "010D\r"),
                DiagnosticEvent(Direction.RX, 0, 0, rawHex = "410D24\r>")
            ))
            val imported = DiagnosticTranscript.fromJson(transcript.toJson())
            val result = com.sarani.carobd.core.adapter.transport.TranscriptReplayRunner().run(imported).single()
            val evidence = result.evidence.single()
            assertTrue(evidence.result is DiagnosticResult.Positive)
            assertEquals(DiagnosticService.MODE_01, evidence.service)
            assertArrayEquals(byteArrayOf(0x0D, 0x24), evidence.payload)
            assertNull(evidence.responderId)
            assertEquals(before, physical.writes)
            assertTrue(physical.isConnected)
        }
        repo.disconnect()
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun deniedBenchAdmissionRecordsFailureWithoutOpeningTransportOrInventingDisconnect() = runTest {
        val directory = java.nio.file.Files.createTempDirectory("bench-denial").toFile()
        try {
            val store = com.sarani.carobd.core.adapter.transport.FileTranscriptStore(directory)
            val recording = com.sarani.carobd.data.recording.RecordingController(store, backgroundScope)
            recording.enableBench(true)
            val transport = Physical()
            val repo = repository(backgroundScope, transport, recording, admission = { throw AccessDeniedException(RuntimeAccess.Connect, AccessState.Denied) })
            assertTrue(repo.connect(profile).isFailure)
            runCurrent()
            assertFalse(transport.connectEntered.isCompleted)
            assertEquals(emptyList<String>(), transport.writes)
            val capture = store.read(store.list().single().sessionId)
            assertTrue(capture.events.any { it.protocolMetadata["phase"] == "PermissionDenied" && it.protocolMetadata["state"] == "Connect:Denied" })
            assertTrue(capture.events.any { it.protocolMetadata["phase"] == "SessionEnded" })
            assertFalse(capture.events.any { it.direction == Direction.TX || it.protocolMetadata["event"] == "disconnect" || it.protocolMetadata["phase"] == "PermissionGranted" })
        } finally { directory.deleteRecursively() }
    }
    @Test fun reportRetainsPartialScanAndCapturedPhysicalIdentityWithoutPromotingMalformedToSuccess() = runTest {
        val t = Physical(can = true)
        t.overrides = mapOf("03" to "7E8:0443010133\r7E9 43 02 01", "07" to "4700", "0A" to "NO DATA")
        val reports = mutableListOf<DiagnosticSessionReport>()
        val repo = repository(backgroundScope, t, reportSink = { reports += it })
        assertTrue(repo.connect(profile).isSuccess)
        assertTrue(repo.scanDiagnosticTroubleCodes().isFailure)
        val saved = reports.single()
        assertFalse(saved.simulator)
        assertEquals("test", saved.profileId)
        assertEquals("bench", saved.profileLabel)
        assertNull(saved.vehicleId)
        assertEquals(ScanCompleteness.PARTIAL, saved.completeness)
        assertEquals(setOf("7E8", "7E9"), saved.responders)
        assertEquals("P0133", saved.codes.single().code)
        assertTrue(saved.outcomes.any { it.outcome == ScanOutcome.MALFORMED && it.rawLines.isNotEmpty() })
        assertTrue(saved.outcomes.any { it.outcome == ScanOutcome.NO_DATA })
        repo.disconnect()
        assertFalse(reports.single().simulator)
    }

    @Test fun reportStorageFailureDoesNotHideSuccessfulScanOrRetryCommands() = runTest {
        val t = Physical()
        val repo = repository(backgroundScope, t, reportSink = { error("disk unavailable") })
        assertTrue(repo.connect(profile).isSuccess)
        assertTrue(repo.scanDiagnosticTroubleCodes().isSuccess)
        assertTrue(repo.historyWarning.value!!.contains("disk unavailable"))
        assertEquals(1, t.writes.count { it == "03" })
        repo.disconnect()
    }
    @Test fun delayedNotificationDisconnectCannotRetireReplacementGeneration() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val old = requireNotNull(repo.foregroundSessionId.value)
        assertTrue(repo.connect(profile.copy(id = "replacement", name = "Replacement")).isSuccess)
        val current = requireNotNull(repo.foregroundSessionId.value)
        assertNotEquals(old, current)
        assertFalse(repo.disconnectIfCurrent(old))
        assertEquals(current, repo.foregroundSessionId.value)
        assertTrue(t.isConnected)
        assertTrue(repo.sendRawCommand("ATI").isSuccess)
        assertTrue(repo.disconnectIfCurrent(current))
        assertFalse(t.isConnected)
        assertNull(repo.foregroundSessionId.value)
        assertEquals(ConnectionState.Disconnected, repo.connectionState.value)
    }

    @Test fun duplicateAndUnknownServiceDisconnectAreInert() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertFalse(repo.disconnectIfCurrent("unknown"))
        assertTrue(repo.connect(profile).isSuccess)
        val current = requireNotNull(repo.foregroundSessionId.value)
        assertFalse(repo.disconnectIfCurrent(""))
        assertTrue(repo.disconnectIfCurrent(current))
        assertFalse(repo.disconnectIfCurrent(current))
        assertNull(repo.foregroundSessionId.value)
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun delayedServiceTeardownWaitsForReplacementLockWithoutCancelingConnect() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val old = requireNotNull(repo.foregroundSessionId.value)
        t.blockConnect = true
        t.connectGate = kotlinx.coroutines.CompletableDeferred()
        val replacement = async { repo.connect(profile.copy(id = "next")) }
        runCurrent()
        val delayedStop = async { repo.disconnectIfCurrent(old) }
        runCurrent()
        assertFalse(delayedStop.isCompleted)
        t.connectGate!!.complete(Unit)
        assertTrue(replacement.await().isSuccess)
        assertFalse(delayedStop.await())
        assertTrue(repo.connectionState.value is ConnectionState.Connected)
        assertNotEquals(old, repo.foregroundSessionId.value)
        repo.disconnect()
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun revokedOngoingAdmissionInvalidatesActualSessionAndNeverRevivesWithoutReconnect() = runTest {
        val t = Physical()
        var allowed = true
        val repo = repository(backgroundScope, t, ongoingAdmission = {
            if (!allowed) throw AccessDeniedException(RuntimeAccess.LocalNetwork, AccessState.Denied)
        })
        assertTrue(repo.connect(profile).isSuccess)
        runCurrent()
        val writesBeforeRevocation = t.writes.size
        allowed = false
        advanceTimeBy(200); runCurrent()
        assertFalse(repo.connectionState.value is ConnectionState.Connected)
        assertFalse(t.isConnected)
        assertNull(repo.foregroundSessionId.value)
        assertTrue(repo.sendRawCommand("ATI").isFailure)
        assertEquals(writesBeforeRevocation, t.writes.size)
        allowed = true
        assertTrue(repo.sendRawCommand("ATI").isFailure)
        assertTrue(repo.connect(profile).isSuccess)
        assertTrue(repo.sendRawCommand("ATI").isSuccess)
        repo.disconnect()
    }
}
