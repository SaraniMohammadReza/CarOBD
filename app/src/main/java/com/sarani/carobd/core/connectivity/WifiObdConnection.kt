package com.sarani.carobd.core.connectivity

import com.sarani.carobd.core.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class WifiObdConnection : ObdConnection {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override suspend fun connect(target: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting

            // Default OBD2 Wi-Fi is 192.168.0.10:35000
            val parts = target.split(":")
            val host = if (parts.isNotEmpty() && parts[0].isNotBlank()) parts[0] else "192.168.0.10"
            val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 35000 else 35000

            _connectionState.value = ConnectionState.Initializing("Connecting to $host:$port...")

            val sock = Socket()
            sock.connect(InetSocketAddress(host, port), 5000)
            socket = sock
            inputStream = sock.getInputStream()
            outputStream = sock.getOutputStream()

            _connectionState.value = ConnectionState.Connected(
                deviceName = "Wi-Fi OBD-II Adapter ($host)",
                protocolName = "Wi-Fi TCP Socket",
                elmVersion = "ELM327 Wi-Fi"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect to Wi-Fi OBD")
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {}
        socket = null
        inputStream = null
        outputStream = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun sendCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val out = outputStream ?: return@withContext Result.failure(IllegalStateException("Socket not connected"))
            val input = inputStream ?: return@withContext Result.failure(IllegalStateException("Socket not connected"))

            val formattedCmd = (command.trim() + "\r").toByteArray(Charsets.US_ASCII)
            out.write(formattedCmd)
            out.flush()

            val buffer = StringBuilder()
            val byteBuffer = ByteArray(256)
            var bytesRead: Int

            while (true) {
                bytesRead = input.read(byteBuffer)
                if (bytesRead == -1) break
                val chunk = String(byteBuffer, 0, bytesRead, Charsets.US_ASCII)
                buffer.append(chunk)
                if (buffer.contains(">")) {
                    break
                }
            }

            Result.success(buffer.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean {
        return socket?.isConnected == true && socket?.isClosed == false && _connectionState.value is ConnectionState.Connected
    }
}
