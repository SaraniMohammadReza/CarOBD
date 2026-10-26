package com.sarani.carobd.data.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.sarani.carobd.core.adapter.transport.ConnectionFailedException
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.adapter.transport.TransportDisconnectedException
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Production Bluetooth Low Energy (BLE) OBD-II transport.
 * Supports Vgate iCar Pro, Viecar, OBDLink CX, and Nordic UART Service (NUS).
 */
class BleTransport(
    private val context: Context,
    private val deviceAddress: String,
    private val bluetoothAdapter: BluetoothAdapter?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ObdTransport {

    companion object {
        // Nordic UART Service (NUS)
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_RX_WRITE_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_TX_NOTIFY_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")

        // Standard FFF0 Chinese BLE OBD Service
        val FFF0_SERVICE_UUID: UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB")
        val FFF1_NOTIFY_UUID: UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB")
        val FFF2_WRITE_UUID: UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB")

        val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }

    override val transportType: TransportType = TransportType.BLUETOOTH_LE

    @Volatile
    private var _isConnected: Boolean = false
    override val isConnected: Boolean
        get() = _isConnected

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var connectionDeferred: CompletableDeferred<Boolean>? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect() = withContext(ioDispatcher) {
        if (_isConnected) return@withContext

        val adapter = bluetoothAdapter ?: throw ConnectionFailedException("Bluetooth adapter is unavailable")
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            throw ConnectionFailedException("Invalid BLE device address: $deviceAddress", e)
        }

        connectionDeferred = CompletableDeferred()

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gattClient: BluetoothGatt, status: Int, newState: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    gattClient.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    _isConnected = false
                    connectionDeferred?.complete(false)
                }
            }

            override fun onServicesDiscovered(gattClient: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val resolved = resolveCharacteristics(gattClient)
                    if (resolved) {
                        _isConnected = true
                        connectionDeferred?.complete(true)
                    } else {
                        connectionDeferred?.complete(false)
                    }
                } else {
                    connectionDeferred?.complete(false)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gattClient: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val value = characteristic.value ?: return
                _incomingBytes.tryEmit(value)
            }
        }

        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

        val connected = withTimeoutOrNull(8000L) {
            connectionDeferred?.await()
        }

        if (connected != true) {
            disconnect()
            throw ConnectionFailedException("Failed to establish and configure GATT connection to $deviceAddress")
        }
    }

    @SuppressLint("MissingPermission")
    private fun resolveCharacteristics(gattClient: BluetoothGatt): Boolean {
        // Try NUS service first
        val nusService = gattClient.getService(NUS_SERVICE_UUID)
        if (nusService != null) {
            writeCharacteristic = nusService.getCharacteristic(NUS_RX_WRITE_UUID)
            val notifyChar = nusService.getCharacteristic(NUS_TX_NOTIFY_UUID)
            if (writeCharacteristic != null && notifyChar != null) {
                enableNotifications(gattClient, notifyChar)
                return true
            }
        }

        // Try FFF0 service
        val fff0Service = gattClient.getService(FFF0_SERVICE_UUID)
        if (fff0Service != null) {
            writeCharacteristic = fff0Service.getCharacteristic(FFF2_WRITE_UUID)
                ?: fff0Service.getCharacteristic(FFF1_NOTIFY_UUID)
            val notifyChar = fff0Service.getCharacteristic(FFF1_NOTIFY_UUID)
            if (writeCharacteristic != null && notifyChar != null) {
                enableNotifications(gattClient, notifyChar)
                return true
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gattClient: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gattClient.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_DESCRIPTOR_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gattClient.writeDescriptor(descriptor)
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() = withContext(ioDispatcher) {
        _isConnected = false
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {}
        gatt = null
        writeCharacteristic = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun write(data: ByteArray) = withContext(ioDispatcher) {
        if (!_isConnected) throw TransportDisconnectedException("Cannot write: BLE transport is disconnected")
        val char = writeCharacteristic ?: throw TransportDisconnectedException("BLE write characteristic not resolved")
        val gattClient = gatt ?: throw TransportDisconnectedException("GATT is null")

        // BLE standard MTU chunks (max 20 bytes per chunk if not MTU negotiated)
        var offset = 0
        while (offset < data.size) {
            val chunkSize = minOf(20, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            char.value = chunk
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            gattClient.writeCharacteristic(char)
            offset += chunkSize
            if (offset < data.size) {
                kotlinx.coroutines.delay(10)
            }
        }
    }
}
