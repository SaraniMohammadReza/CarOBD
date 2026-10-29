package com.sarani.carobd.data.repository

import com.sarani.carobd.core.adapter.profile.InMemoryConnectionProfileRepository
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.*
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
        override suspend fun connect() { isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) {
            val c = data.toString(Charsets.US_ASCII).trim(); writes.add(c)
            val raw = when(c) {
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
        pollModeFlow = MutableStateFlow(TelemetryPollMode.NORMAL),
        createTransport = { physical }, startService = {}, stopService = {},
    )
    private val profile = ConnectionProfile("test", "bench", TransportConfig.Wifi("bench", 35000))
    @Test fun repository_actualScanPathPreservesAllThreeNonCanCategories() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        val result = repo.scanDiagnosticTroubleCodes()
        assertTrue(result.toString(), result.isSuccess)
        assertEquals(setOf(DtcCategory.STORED, DtcCategory.PENDING, DtcCategory.PERMANENT), result.getOrThrow().map { it.category }.toSet())
        assertTrue(result.getOrThrow().all { it.code == "P0133" && it.responderAddressHex == null })
        repo.disconnect()
    }
    @Test fun physicalDedicatedClearAndRawPathsNeverDispatch04() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        assertTrue(repo.clearDiagnosticTroubleCodes().isFailure)
        assertTrue(repo.sendRawCommand("04").isFailure)
        assertTrue(repo.sendRawCommand("ATI\r04").isFailure)
        repo.disconnect()
        assertTrue(repo.connect(profile).isSuccess)
        assertFalse(t.writes.any { it.contains("04") || it.contains('\r') })
        repo.disconnect()
    }
    @Test fun repository_actualMalformedScanIsFailure() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
        t.malformed = true
        val result = repo.scanDiagnosticTroubleCodes()
        assertTrue(result.isFailure)
        assertEquals(ScanCompleteness.PARTIAL, (result.exceptionOrNull() as DiagnosticScanException).report.completeness)
        repo.disconnect()
    }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun repository_repeatedStartTelemetryIsIdempotent() = runTest {
        val t = Physical(); val repo = repository(backgroundScope, t)
        assertTrue(repo.connect(profile).isSuccess)
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
        assertTrue(repo.connect(profile).isSuccess)
        runCurrent()
        t.isConnected = false
        advanceTimeBy(200); runCurrent()
        assertFalse(repo.connectionState.value is ConnectionState.Connected)
        assertTrue(repo.sendRawCommand("ATI").isFailure)
        repo.disconnect()
    }

}
