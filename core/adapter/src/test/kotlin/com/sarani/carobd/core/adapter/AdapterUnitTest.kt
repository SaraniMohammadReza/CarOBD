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
import kotlinx.coroutines.flow.asSharedFlow
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
        val adapter = Elm327Adapter(mockTransport)

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
}

class MockObdTransport : ObdTransport {
    override val transportType: TransportType = TransportType.BLUETOOTH_CLASSIC
    override var isConnected: Boolean = false

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private val responseMap = mutableMapOf<String, String>()

    fun queueResponse(cmd: String, resp: String) {
        responseMap[cmd.trim().uppercase()] = resp
    }

    override suspend fun connect() {
        isConnected = true
    }

    override suspend fun disconnect() {
        isConnected = false
    }

    override suspend fun write(data: ByteArray) {
        val cmdString = String(data, Charsets.US_ASCII).trim().uppercase()
        val canned = responseMap[cmdString] ?: "OK\r\r>"
        _incomingBytes.emit(canned.toByteArray(Charsets.US_ASCII))
    }
}
