package com.sarani.carobd.data.repository

import com.sarani.carobd.core.adapter.profile.InMemoryConnectionProfileRepository
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
        val connectEntered = kotlinx.coroutines.CompletableDeferred<Unit>()
        override suspend fun connect() { connectEntered.complete(Unit); if (blockConnect) kotlinx.coroutines.awaitCancellation(); isConnected = true }
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
    private fun repository(scope: kotlinx.coroutines.CoroutineScope, physical: Physical) = ObdRepositoryImpl(
        context = null,
        activeVehicleRepository = ActiveVehicleRepository(MutableStateFlow(null), {}, {}),
        appPreferencesManager = null,
        connectionProfiles = InMemoryConnectionProfileRepository(),
        scope = scope,
        ioDispatcher = scope.coroutineContext[kotlin.coroutines.ContinuationInterceptor] as kotlinx.coroutines.CoroutineDispatcher,
        pollModeFlow = MutableStateFlow(TelemetryPollMode.NORMAL),
        createTransport = { physical }, startService = {}, stopService = {},
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

}
