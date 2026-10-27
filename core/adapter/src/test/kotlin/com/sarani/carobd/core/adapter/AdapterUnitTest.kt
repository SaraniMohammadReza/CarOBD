package com.sarani.carobd.core.adapter

import com.sarani.carobd.core.adapter.command.AdapterError
import com.sarani.carobd.core.adapter.command.AdapterResponse
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.elm327.Elm327BufferParser
import com.sarani.carobd.core.adapter.elm327.Elm327Prober
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.AdapterQuality
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.core.model.ProtocolStandard
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AdapterUnitTest {

    @Test
    fun testBufferParserNormalResponse() {
        val raw = "010C\r41 0C 1A F8\r\r>"
        val parsed = Elm327BufferParser.parseBuffer(raw, "010C", 45L)

        assertTrue(parsed is AdapterResponse.Success)
        val success = parsed as AdapterResponse.Success
        assertEquals(1, success.cleanedLines.size)
        assertEquals("41 0C 1A F8", success.cleanedLines[0])
        assertEquals(45L, success.executionTimeMs)
    }

    @Test
    fun testBufferParserNoData() {
        val raw = "0155\rNO DATA\r\r>"
        val parsed = Elm327BufferParser.parseBuffer(raw, "0155", 30L)

        assertTrue(parsed is AdapterResponse.Error)
        val err = (parsed as AdapterResponse.Error).error
        assertTrue(err is AdapterError.NoData)
    }

    @Test
    fun testBufferParserUnableToConnect() {
        val raw = "0100\rSEARCHING...\rUNABLE TO CONNECT\r\r>"
        val parsed = Elm327BufferParser.parseBuffer(raw, "0100", 2500L)

        assertTrue(parsed is AdapterResponse.Error)
        val err = (parsed as AdapterResponse.Error).error
        assertTrue(err is AdapterError.UnableToConnect)
    }

    @Test
    fun testBufferParserCanError() {
        val raw = "010C\rCAN ERROR\r\r>"
        val parsed = Elm327BufferParser.parseBuffer(raw, "010C", 100L)

        assertTrue(parsed is AdapterResponse.Error)
        val err = (parsed as AdapterResponse.Error).error
        assertTrue(err is AdapterError.CanError)
    }

    @Test
    fun testBufferParserUnknownCommand() {
        val raw = "ATINVALID\r?\r\r>"
        val parsed = Elm327BufferParser.parseBuffer(raw, "ATINVALID", 15L)

        assertTrue(parsed is AdapterResponse.Error)
        val err = (parsed as AdapterResponse.Error).error
        assertTrue(err is AdapterError.UnknownCommand)
    }

    @Test
    fun testHardwareProberCloneDetection() = runBlocking {
        val mockExecutor: suspend (String, Long) -> AdapterResponse = { cmd, _ ->
            when (cmd) {
                "ATI" -> AdapterResponse.Success("ELM327 v2.1", listOf("ELM327 v2.1"), 10L)
                "AT@1" -> AdapterResponse.Success("OBDII to RS232 Interpreter", listOf("OBDII to RS232 Interpreter"), 10L)
                "ATRV" -> AdapterResponse.Success("12.4V", listOf("12.4V"), 10L)
                "ATAT1" -> AdapterResponse.Error(AdapterError.UnknownCommand, "?", 10L)
                "ATCAF1" -> AdapterResponse.Error(AdapterError.UnknownCommand, "?", 10L)
                "ATCSM0" -> AdapterResponse.Error(AdapterError.UnknownCommand, "?", 10L)
                "STI" -> AdapterResponse.Error(AdapterError.UnknownCommand, "?", 10L)
                else -> AdapterResponse.Success("OK", listOf("OK"), 10L)
            }
        }

        val prober = Elm327Prober(mockExecutor)
        val result = prober.probeHardware()

        assertEquals("ELM327 v2.1", result.firmwareVersion)
        assertEquals(12.4f, result.voltage, 0.01f)
        assertTrue(result.isClone)
        assertEquals(AdapterQuality.SUSPICIOUS_CLONE, result.quality)
    }

    @Test
    fun testElm327AdapterFullHandshake() = runBlocking {
        val mockTransport = MockObdTransport()
        val adapter = testAdapter(mockTransport)

        // Queue mock hardware responses
        mockTransport.queueResponse("ATZ", "ELM327 v1.5\r\r>")
        mockTransport.queueResponse("ATE0", "OK\r\r>")
        mockTransport.queueResponse("ATL0", "OK\r\r>")
        mockTransport.queueResponse("ATS0", "OK\r\r>")
        mockTransport.queueResponse("ATAT1", "OK\r\r>")
        mockTransport.queueResponse("ATI", "ELM327 v1.5\r\r>")
        mockTransport.queueResponse("AT@1", "OBDLink SX\r\r>")
        mockTransport.queueResponse("ATRV", "13.8V\r\r>")
        mockTransport.queueResponse("ATCAF1", "OK\r\r>")
        mockTransport.queueResponse("ATCSM0", "OK\r\r>")
        mockTransport.queueResponse("STI", "?\r\r>")
        mockTransport.queueResponse("ATSP0", "OK\r\r>")
        mockTransport.queueResponse("0100", "41 00 BE 3E B8 11\r\r>")
        mockTransport.queueResponse("ATDPN", "6\r\r>") // 6 = ISO 15765-4 CAN 11-bit 500k

        val initResult = adapter.initialize()

        assertTrue(initResult.isSuccess)
        assertEquals("ELM327 v1.5", initResult.firmwareVersion)
        assertEquals(13.8f, initResult.batteryVoltage, 0.01f)
        assertEquals(ProtocolStandard.ISO_15765_4_CAN_11_500, adapter.currentProtocol)
        assertTrue(adapter.state.value is ConnectionState.Connected)

        // Test normal PID execution
        mockTransport.queueResponse("010C", "41 0C 1A F8\r\r>")
        val pidResp = adapter.executeCommand("010C")
        assertTrue(pidResp is AdapterResponse.Success)
        val success = pidResp as AdapterResponse.Success
        assertEquals("41 0C 1A F8", success.cleanedLines[0])
    }

    @Test
    fun testElm327AdapterFailsOnNoDataFromEcu() = runBlocking {
        val mockTransport = MockObdTransport()
        val adapter = testAdapter(mockTransport)

        mockTransport.queueResponse("ATZ", "ELM327 v1.5\r\r>")
        mockTransport.queueResponse("ATE0", "OK\r\r>")
        mockTransport.queueResponse("ATL0", "OK\r\r>")
        mockTransport.queueResponse("ATS0", "OK\r\r>")
        mockTransport.queueResponse("ATAT1", "OK\r\r>")
        mockTransport.queueResponse("ATI", "ELM327 v1.5\r\r>")
        mockTransport.queueResponse("AT@1", "OBDII to RS232 Interpreter\r\r>")
        mockTransport.queueResponse("ATRV", "13.8V\r\r>")
        mockTransport.queueResponse("ATCAF1", "OK\r\r>")
        mockTransport.queueResponse("ATCSM0", "OK\r\r>")
        mockTransport.queueResponse("STI", "?\r\r>")
        mockTransport.queueResponse("ATSP0", "OK\r\r>")
        mockTransport.queueResponse("0100", "NO DATA\r\r>")

        val initResult = adapter.initialize()

        assertFalse("Init must fail when ECU returns NO DATA", initResult.isSuccess)
        val state = adapter.state.value
        assertTrue("State must be a Failure", state is ConnectionState.Failure)
        assertEquals("ECU_HANDSHAKE", (state as ConnectionState.Failure).stage)
    }

    @Test
    fun testElm327AdapterFailsOnMalformed0100() = runBlocking {
        val mockTransport = MockObdTransport()
        val adapter = testAdapter(mockTransport)

        mockTransport.queueResponse("ATZ", "ELM327 v1.5\r\r>")
        mockTransport.queueResponse("ATE0", "OK\r\r>")
        mockTransport.queueResponse("ATL0", "OK\r\r>")
        mockTransport.queueResponse("ATS0", "OK\r\r>")
        mockTransport.queueResponse("ATAT1", "OK\r\r>")
        mockTransport.queueResponse("ATI", "ELM327 v1.5\r\r>")
        mockTransport.queueResponse("AT@1", "OBDII to RS232 Interpreter\r\r>")
        mockTransport.queueResponse("ATRV", "13.8V\r\r>")
        mockTransport.queueResponse("ATCAF1", "OK\r\r>")
        mockTransport.queueResponse("ATCSM0", "OK\r\r>")
        mockTransport.queueResponse("STI", "?\r\r>")
        mockTransport.queueResponse("ATSP0", "OK\r\r>")
        // 0100 returning an RPM frame is not a valid Mode 01 PID 00 response
        mockTransport.queueResponse("0100", "41 0C 1A F8\r\r>")

        val initResult = adapter.initialize()

        assertFalse("Init must fail when 0100 is not a valid PID 00 frame", initResult.isSuccess)
        val state = adapter.state.value
        assertTrue("State must be a Failure", state is ConnectionState.Failure)
        assertEquals("ECU_HANDSHAKE", (state as ConnectionState.Failure).stage)
    }

    // --- B4: connection & active-vehicle truthfulness regressions ---

    @Test
    fun testSimulatorCannotMasqueradeAsPhysicalTransport() {
        // B4 #3: a simulator transport exposes TransportType.SIMULATOR, never a physical type.
        val sim = MockObdTransport(TransportType.SIMULATOR)
        assertEquals(TransportType.SIMULATOR, sim.transportType)
        assertTrue(
            "Simulator must not masquerade as BLUETOOTH_CLASSIC/BLE/WIFI_TCP/USB_SERIAL",
            sim.transportType != TransportType.BLUETOOTH_CLASSIC &&
                sim.transportType != TransportType.BLUETOOTH_LE &&
                sim.transportType != TransportType.WIFI_TCP &&
                sim.transportType != TransportType.USB_SERIAL
        )
    }

    @Test
    fun testSimulatorHandshakeReportsIsSimulatorTrue() = runBlocking {
        val sim = MockObdTransport(TransportType.SIMULATOR)
        val adapter = testAdapter(sim)
        queueFullHandshake(sim)
        assertTrue(adapter.initialize().isSuccess)
        val state = adapter.state.value
        assertTrue(state is ConnectionState.Connected)
        assertTrue(
            "Simulator Connected must carry isSimulator = true (B4 #4)",
            (state as ConnectionState.Connected).isSimulator
        )
    }

    @Test
    fun testPhysicalHandshakeReportsIsSimulatorFalse() = runBlocking {
        val phy = MockObdTransport() // BLUETOOTH_CLASSIC by default
        val adapter = testAdapter(phy)
        queueFullHandshake(phy)
        assertTrue(adapter.initialize().isSuccess)
        val state = adapter.state.value
        assertTrue(state is ConnectionState.Connected)
        assertFalse(
            "Physical connection must not carry isSimulator = true (B4 #5)",
            (state as ConnectionState.Connected).isSimulator
        )
    }

    // ---- §12 ELM327 / STN robustness coverage (failures must never become success) ----

    @Test
    fun testBufferParserStopped() {
        val parsed = Elm327BufferParser.parseBuffer("010C\rSTOPPED\r\r>", "010C", 100L)
        assertTrue(parsed is AdapterResponse.Error)
        assertEquals(AdapterError.Stopped, (parsed as AdapterResponse.Error).error)
    }

    @Test
    fun testBufferParserBusInitError() {
        val parsed = Elm327BufferParser.parseBuffer("0100\rSEARCHING...\rBUS INIT: ERROR\r\r>", "0100", 2500L)
        assertTrue(parsed is AdapterResponse.Error)
        assertEquals(AdapterError.BusInitFailed, (parsed as AdapterResponse.Error).error)
    }

    @Test
    fun testBufferParserBufferFull() {
        val parsed = Elm327BufferParser.parseBuffer("0100\rBUFFER FULL\r\r>", "0100", 100L)
        assertTrue(parsed is AdapterResponse.Error)
        assertEquals(AdapterError.BufferOverflow, (parsed as AdapterResponse.Error).error)
    }

    @Test
    fun testBufferParserStripsCommandEcho() {
        val parsed = Elm327BufferParser.parseBuffer("010C\r41 0C 1A F8\r\r>", "010C", 50L)
        assertTrue(parsed is AdapterResponse.Success)
        val lines = (parsed as AdapterResponse.Success).cleanedLines
        assertEquals(1, lines.size)
        assertEquals("41 0C 1A F8", lines[0])
        assertFalse("Command echo must be stripped from cleaned lines", lines.any { it == "010C" })
    }

    @Test
    fun testBufferParserMultilineStripsPromptAndCrlf() {
        val parsed = Elm327BufferParser.parseBuffer("41 0C 1A F8\r41 0D 55\r\r>", "010C", 50L)
        assertTrue(parsed is AdapterResponse.Success)
        val success = parsed as AdapterResponse.Success
        assertEquals(2, success.cleanedLines.size)
        assertTrue("Prompt '>' must be stripped", !success.rawResponse.contains(">"))
        assertEquals("41 0C 1A F8", success.cleanedLines[0])
        assertEquals("41 0D 55", success.cleanedLines[1])
    }

    // ---- §5 ELM command lifecycle: serialization + late-prompt isolation ----

    @Test
    fun testLatePromptDoesNotContaminateNextCommand() = runBlocking {
        // §5 headline: A times out because its response arrives late; the late A
        // payload + late '>' must be quarantined by the post-timeout resync and
        // never leak into B. B must succeed with its OWN bytes only.
        val mockTransport = MockObdTransport()
        val adapter = testAdapter(mockTransport)
        mockTransport.queueDelayedResponse("010C", 400, "010C\r41 0C 1A F8\r\r>")
        mockTransport.queueResponse("010D", "010D\r41 0D 55\r\r>")

        val a = adapter.executeCommand("010C", 300L)
        assertTrue("A must time out", a is AdapterResponse.Error)
        assertTrue("A must be a Timeout", (a as AdapterResponse.Error).error is AdapterError.Timeout)

        val b = adapter.executeCommand("010D", 2000L)
        assertTrue("B must succeed independently after resync", b is AdapterResponse.Success)
        val bSuccess = b as AdapterResponse.Success
        // §5 invariant: B must NEVER receive A's response bytes.
        assertFalse("B must not contain A's data", bSuccess.rawResponse.contains("41 0C"))
        assertEquals(1, bSuccess.cleanedLines.size)
        assertEquals("41 0D 55", bSuccess.cleanedLines[0])
    }

    @Test
    fun testTwoSimultaneousCallersSerializeOnMutex() = runBlocking {
        // §5: concurrent callers must serialize (one command in flight). Both must
        // receive their own clean response with no interleaving.
        val mockTransport = MockObdTransport()
        val adapter = testAdapter(mockTransport)
        mockTransport.queueResponse("010C", "41 0C 1A F8\r\r>")
        mockTransport.queueResponse("010D", "41 0D 55\r\r>")

        val results = java.util.Collections.synchronizedList(mutableListOf<AdapterResponse>())
        val j1 = launch { results.add(adapter.executeCommand("010C", 2000L)) }
        val j2 = launch { results.add(adapter.executeCommand("010D", 2000L)) }
        joinAll(j1, j2)
        assertEquals(2, results.size)
        assertTrue("both must succeed (serialized, not interleaved)", results.all { it is AdapterResponse.Success })
        val codes = results.map { (it as AdapterResponse.Success).cleanedLines.first() }.toSet()
        assertEquals(setOf("41 0C 1A F8", "41 0D 55"), codes)
    }

    @Test
    fun testElmFailureTokensNeverBecomeSuccess() = runBlocking {
        // §5 transcript: each ELM failure token must surface as Error at the
        // executeCommand boundary, never silently become Success.
        val cases = listOf(
            "NO DATA" to AdapterError.NoData,
            "STOPPED" to AdapterError.Stopped,
            "CAN ERROR" to AdapterError.CanError,
            "BUFFER FULL" to AdapterError.BufferOverflow,
            "UNABLE TO CONNECT" to AdapterError.UnableToConnect,
            "BUS INIT: ERROR" to AdapterError.BusInitFailed
        )
        for ((text, expected) in cases) {
            val mockTransport = MockObdTransport()
            val adapter = testAdapter(mockTransport)
            mockTransport.queueResponse("010C", "$text\r\r>")
            val resp = adapter.executeCommand("010C", 1000L)
            assertTrue("$text must be Error", resp is AdapterResponse.Error)
            assertEquals("$text must map to $expected", expected, (resp as AdapterResponse.Error).error)
        }
    }

    @Test
    fun testUnknownCommandMarkerBecomesError() = runBlocking {
        val mockTransport = MockObdTransport()
        val adapter = testAdapter(mockTransport)
        mockTransport.queueResponse("ATINVALID", "ATINVALID\r?\r\r>")
        val resp = adapter.executeCommand("ATINVALID", 1000L)
        assertTrue("? must be UnknownCommand Error", resp is AdapterResponse.Error)
        assertEquals(AdapterError.UnknownCommand, (resp as AdapterResponse.Error).error)
    }

    @Test
    fun testReconnectAfterStreamCompromiseRestoresCommandPath() = runBlocking {
        // §5: when a command times out and resynchronisation cannot be proven
        // (no late prompt ever arrives), the stream is marked compromised and
        // subsequent commands are refused until a (re)initialisation resets it.
        val deadTransport = MockObdTransport()
        val deadAdapter = testAdapter(deadTransport)
        val a = deadAdapter.executeCommand("010C", 200L)
        assertTrue("A must time out", a is AdapterResponse.Error)
        // Dead transport never sends A's late prompt -> resync cannot prove a boundary.
        val b = deadAdapter.executeCommand("010D", 300L)
        assertTrue("compromised stream must refuse subsequent commands", b is AdapterResponse.Error)

        // Reconnect: a fresh adapter + full handshake resets session state.
        val newTransport = MockObdTransport()
        val newAdapter = testAdapter(newTransport)
        queueFullHandshake(newTransport)
        assertTrue(newAdapter.initialize().isSuccess)
        newTransport.queueResponse("010D", "41 0D 55\r\r>")
        val c = newAdapter.executeCommand("010D", 2000L)
        assertTrue("after reconnect, command succeeds on its own bytes", c is AdapterResponse.Success)
        assertEquals("41 0D 55", (c as AdapterResponse.Success).cleanedLines[0])
    }
}

/**
 * Deterministic test adapter. Injects a Dispatchers.Unconfined-backed scope so the
 * inbound reader suspends synchronously in collect() during construction and is
 * resumed inline on the calling (runBlocking) thread by emit(). This removes the
 * Dispatchers.IO scheduling race that otherwise starves the receive loop and
 * produces spurious command timeouts under JUnit. Production code is untouched
 * (Elm327Adapter defaults to Dispatchers.IO).
 */
private fun testAdapter(transport: ObdTransport): Elm327Adapter =
    Elm327Adapter(transport, CoroutineScope(Dispatchers.Unconfined))

private fun queueFullHandshake(t: MockObdTransport) {
    t.queueResponse("ATZ", "ELM327 v1.5\r\r>")
    t.queueResponse("ATE0", "OK\r\r>")
    t.queueResponse("ATL0", "OK\r\r>")
    t.queueResponse("ATS0", "OK\r\r>")
    t.queueResponse("ATAT1", "OK\r\r>")
    t.queueResponse("ATI", "ELM327 v1.5\r\r>")
    t.queueResponse("AT@1", "OBDII to RS232 Interpreter\r\r>")
    t.queueResponse("ATRV", "13.8V\r\r>")
    t.queueResponse("ATCAF1", "OK\r\r>")
    t.queueResponse("ATCSM0", "OK\r\r>")
    t.queueResponse("STI", "?\r\r>")
    t.queueResponse("ATSP0", "OK\r\r>")
    t.queueResponse("0100", "41 00 BE 3E B8 11\r\r>")
    t.queueResponse("ATDPN", "6\r\r>")
}

class MockObdTransport(
    override val transportType: TransportType = TransportType.BLUETOOTH_CLASSIC
) : ObdTransport {
    override var isConnected: Boolean = false

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private val responseMap = mutableMapOf<String, String>()
    private val delayedResponses = mutableMapOf<String, Pair<Long, String>>()
    private val emitScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun queueResponse(cmd: String, resp: String) {
        responseMap[cmd.trim().uppercase()] = resp
    }

    /** Queues a response emitted only after [delayMs], modelling a late prompt. */
    fun queueDelayedResponse(cmd: String, delayMs: Long, resp: String) {
        delayedResponses[cmd.trim().uppercase()] = (delayMs to resp)
    }

    override suspend fun connect() {
        isConnected = true
    }

    override suspend fun disconnect() {
        isConnected = false
    }

    override suspend fun write(data: ByteArray) {
        val cmdString = String(data, Charsets.US_ASCII).trim().uppercase()
        val delayed = delayedResponses.remove(cmdString)
        val canned = responseMap[cmdString]
        // No fabricated "OK\r\r>" ack: an unqueued command models a no-response
        // transport, so emit nothing and let the receive loop time out cleanly.
        if (delayed != null) {
            // Late response: emit asynchronously so bytes arrive AFTER the command's
            // timeout, exercising the post-timeout resync path (never synchronously).
            emitScope.launch {
                delay(delayed.first)
                _incomingBytes.emit(delayed.second.toByteArray(Charsets.US_ASCII))
            }
        } else if (canned != null) {
            _incomingBytes.emit(canned.toByteArray(Charsets.US_ASCII))
        }
    }
}
