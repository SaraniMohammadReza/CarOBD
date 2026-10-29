package com.sarani.carobd.core.adapter
import com.sarani.carobd.core.adapter.elm327.Elm327Adapter
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
class ManualProtocolSafetyTest {
    private class Transport(val readback: String, val reject: Boolean = false) : ObdTransport {
        override val transportType = TransportType.WIFI_TCP
        override var isConnected = true
        val rx = MutableSharedFlow<ByteArray>(extraBufferCapacity = 16)
        override val incomingBytes = rx.asSharedFlow()
        val writes = mutableListOf<String>()
        override suspend fun connect() { isConnected = true }
        override suspend fun disconnect() { isConnected = false }
        override suspend fun write(data: ByteArray) {
            val c = data.toString(Charsets.US_ASCII).trim(); writes.add(c)
            val r = when {
                c.startsWith("ATSP") -> if (reject) "?" else "OK"
                c == "ATZ" -> "ELM327"
                c == "ATI" -> "ELM327"
                c == "ATDPN" -> readback
                c == "ATRV" -> "12.0V"
                c == "0100" -> "4100FFFFFFFF"
                else -> "OK"
            }
            rx.emit((r + "\r>").toByteArray())
        }
    }
    @Test fun manualProtocolAUsesCorrectCommand() = runTest {
        val t = Transport("A"); val a = Elm327Adapter(t, backgroundScope)
        assertTrue(a.initialize(protocolFromManualCode("A")).isSuccess)
        assertTrue(t.writes.contains("ATSPA")); assertFalse(t.writes.contains("ATSPB"))
        assertEquals(ProtocolStandard.SAE_J1939_CAN, a.currentProtocol)
        a.disconnect()
    }
    @Test fun manualProtocolRejectedFailsInitialization() = runTest {
        val t = Transport("6", true); val a = Elm327Adapter(t, backgroundScope)
        assertFalse(a.initialize(protocolFromManualCode("6")).isSuccess)
        assertFalse(t.writes.contains("0100"))
        a.disconnect()
    }
    @Test fun protocolUnknownReadbackRemainsUnknown() = runTest {
        val t = Transport("Z"); val a = Elm327Adapter(t, backgroundScope)
        assertFalse(a.initialize().isSuccess); assertNull(a.currentProtocol)
        a.disconnect()
    }
    @Test fun manualProtocolMismatchFailsClosed() = runTest {
        val t = Transport("7"); val a = Elm327Adapter(t, backgroundScope)
        assertFalse(a.initialize(protocolFromManualCode("6")).isSuccess)
        a.disconnect()
    }
}
