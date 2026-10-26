package com.sarani.carobd.data.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.sarani.carobd.core.adapter.transport.ConnectionFailedException
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.adapter.transport.TransportDisconnectedException
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Production Bluetooth Classic (RFCOMM / SPP) OBD-II transport.
 * Supports standard UUID 00001101-0000-1000-8000-00805F9B34FB.
 */
class BluetoothClassicTransport(
    private val deviceAddress: String,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ObdTransport {

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    override val transportType: TransportType = TransportType.BLUETOOTH_CLASSIC

    @Volatile
    private var _isConnected: Boolean = false
    override val isConnected: Boolean
        get() = _isConnected

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readerThread: Thread? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect() = withContext(ioDispatcher) {
        if (_isConnected) return@withContext

        val adapter = bluetoothAdapter ?: throw ConnectionFailedException("Bluetooth adapter is not available on this device")
        if (!adapter.isEnabled) {
            throw ConnectionFailedException("Bluetooth is turned off")
        }

        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            throw ConnectionFailedException("Invalid Bluetooth hardware address: $deviceAddress", e)
        }

        // Cancel active discovery to guarantee full RFCOMM bandwidth
        try {
            adapter.cancelDiscovery()
        } catch (_: Exception) {}

        try {
            val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
            sock.connect()
            socket = sock
            inputStream = sock.inputStream
            outputStream = sock.outputStream
            _isConnected = true

            startReaderLoop(sock.inputStream)
        } catch (e: Exception) {
            disconnect()
            throw ConnectionFailedException("Failed to establish RFCOMM connection to $deviceAddress: ${e.message}", e)
        }
    }

    override suspend fun disconnect() = withContext(ioDispatcher) {
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
        if (!_isConnected) throw TransportDisconnectedException("Cannot write: transport is disconnected")
        val out = outputStream ?: throw TransportDisconnectedException("Output stream is closed")
        try {
            out.write(data)
            out.flush()
        } catch (e: Exception) {
            disconnect()
            throw TransportDisconnectedException("Write failed: ${e.message}")
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
        }, "BT-OBD-Reader").apply {
            isDaemon = true
            start()
        }
    }
}
