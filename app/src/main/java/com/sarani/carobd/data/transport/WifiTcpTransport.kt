package com.sarani.carobd.data.transport

import com.sarani.carobd.core.adapter.transport.ConnectionFailedException
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.adapter.transport.TransportDisconnectedException
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Production Wi-Fi TCP OBD-II transport for Wi-Fi dongles (e.g. ELM327 Wi-Fi / Vgate Wi-Fi).
 * Standard default endpoint: 192.168.0.10:35000.
 */
class WifiTcpTransport(
    private val host: String = "192.168.0.10",
    private val port: Int = 35000,
    private val connectTimeoutMs: Int = 5000,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ObdTransport {

    override val transportType: TransportType = TransportType.WIFI_TCP

    @Volatile
    private var _isConnected: Boolean = false
    override val isConnected: Boolean
        get() = _isConnected

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    @Volatile private var closed = false
    @Volatile private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readerThread: Thread? = null

    override suspend fun connect() = withContext(ioDispatcher) {
        check(!closed) { "Transport retired; create a new transport" }
        if (_isConnected) return@withContext

        try {
            val sock = Socket()
            sock.tcpNoDelay = true // Immediate packet delivery without Nagle buffering
            sock.soTimeout = 0 // Infinite read timeout (managed by adapter coroutine timeouts)
            socket = sock
            check(!closed) { "Transport retired" }
            sock.connect(InetSocketAddress(host, port), connectTimeoutMs)
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            check(!closed) { "Transport retired" }
            inputStream = sock.getInputStream()
            outputStream = sock.getOutputStream()
            _isConnected = true

            startReaderLoop(sock.getInputStream())
        } catch (e: Exception) {
            disconnect()
            throw ConnectionFailedException("Could not connect to Wi-Fi OBD adapter at $host:$port: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(ioDispatcher + kotlinx.coroutines.NonCancellable) {
        closed = true
        _isConnected = false
        try {
            readerThread?.interrupt()
            readerThread = null
        } catch (_: Exception) {}

        try {
            inputStream?.close()
        } catch (_: Exception) {}

        try {
            outputStream?.close()
        } catch (_: Exception) {}

        try {
            socket?.close()
        } catch (_: Exception) {}

        inputStream = null
        outputStream = null
        socket = null
    }

    override suspend fun write(data: ByteArray) = withContext(ioDispatcher) {
        if (!_isConnected) throw TransportDisconnectedException("Cannot write: Wi-Fi transport is disconnected")
        val out = outputStream ?: throw TransportDisconnectedException("Output stream is closed")
        try {
            out.write(data)
            out.flush()
        } catch (e: Exception) {
            disconnect()
            throw TransportDisconnectedException("Wi-Fi write failed: ${e.message}")
        }
    }

    private fun startReaderLoop(inStream: InputStream) {
        readerThread = Thread({
            val buffer = ByteArray(512)
            while (_isConnected && !Thread.currentThread().isInterrupted) {
                try {
                    val bytesRead = inStream.read(buffer)
                    if (bytesRead > 0) {
                        val chunk = buffer.copyOf(bytesRead)
                        _incomingBytes.tryEmit(chunk)
                    } else if (bytesRead == -1) {
                        break
                    }
                } catch (e: Exception) {
                    break
                }
            }
            _isConnected = false
        }, "WiFi-OBD-Reader").apply {
            isDaemon = true
            start()
        }
    }
}
