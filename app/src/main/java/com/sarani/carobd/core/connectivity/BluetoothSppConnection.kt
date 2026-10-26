package com.sarani.carobd.core.connectivity

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.sarani.carobd.core.model.ConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothSppConnection : ObdConnection {

    companion object {
        // Standard SPP UUID for ELM327 Bluetooth adapters
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect(target: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return@withContext Result.failure(IllegalStateException("Bluetooth not available on this device"))

            if (!adapter.isEnabled) {
                _connectionState.value = ConnectionState.Error("Bluetooth is turned off")
                return@withContext Result.failure(IllegalStateException("Bluetooth is turned off"))
            }

            val device: BluetoothDevice = if (BluetoothAdapter.checkBluetoothAddress(target)) {
                adapter.getRemoteDevice(target)
            } else {
                adapter.bondedDevices?.firstOrNull { it.name.equals(target, ignoreCase = true) }
                    ?: return@withContext Result.failure(IllegalStateException("Device '$target' not found in paired devices"))
            }

            _connectionState.value = ConnectionState.Initializing("Opening RFCOMM socket...")
            adapter.cancelDiscovery()

            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()

            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream

            _connectionState.value = ConnectionState.Connected(
                deviceName = device.name ?: "ELM327",
                protocolName = "OBD-II Bluetooth SPP",
                elmVersion = "ELM327"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            _connectionState.value = ConnectionState.Error(e.message ?: "Failed to connect to Bluetooth")
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
        return socket?.isConnected == true && _connectionState.value is ConnectionState.Connected
    }
}
