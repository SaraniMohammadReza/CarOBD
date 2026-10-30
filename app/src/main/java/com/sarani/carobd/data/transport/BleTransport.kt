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

    private val readiness = com.sarani.carobd.core.model.BleReadiness()
    override val isConnected: Boolean get() = readiness.isReady

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private var gatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private var connectionDeferred: CompletableDeferred<Boolean>? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect() = withContext(ioDispatcher) {
        check(!readiness.isClosed) { "BLE generation retired" }
        if (isConnected) return@withContext

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
                    try {
                        if (readiness.linkConnected() && !gattClient.discoverServices()) connectionDeferred?.complete(false)
                    } catch (_: SecurityException) { readiness.disconnect(); connectionDeferred?.complete(false) }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    readiness.disconnect()
                    connectionDeferred?.complete(false)
                }
            }

            override fun onServicesDiscovered(gattClient: BluetoothGatt, status: Int) {
                if (gatt !== gattClient) return
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val resolved = try { resolveCharacteristics(gattClient) } catch (_: SecurityException) { readiness.disconnect(); false }
                    if (!resolved) {
                        connectionDeferred?.complete(false)
                    }
                } else {
                    connectionDeferred?.complete(false)
                }
            }

            override fun onDescriptorWrite(gattClient: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                if (gatt === gattClient && descriptor.uuid == CCCD_DESCRIPTOR_UUID) {
                    connectionDeferred?.complete(readiness.acknowledgeSubscription(status == BluetoothGatt.GATT_SUCCESS))
                }
            }

            override fun onCharacteristicChanged(gattClient: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                if (isConnected && gatt === gattClient) _incomingBytes.tryEmit(value.copyOf())
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gattClient: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                if (!isConnected || gatt !== gattClient) return
                val value = characteristic.value ?: return
                _incomingBytes.tryEmit(value)
            }
        }

        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

        if (readiness.isClosed) { disconnect(); throw ConnectionFailedException("BLE generation retired") }
        val connected = withTimeoutOrNull(8000L) {
            connectionDeferred?.await()
        }

        if (connected != true) {
            disconnect()
            throw com.sarani.carobd.core.model.ConnectionIssueException(com.sarani.carobd.core.model.ConnectionIssue.BleSubscriptionFailed, "BLE service discovery or CCCD acknowledgement failed for $deviceAddress")
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
                return enableNotifications(gattClient, notifyChar)
            }
        }

        // Try FFF0 service
        val fff0Service = gattClient.getService(FFF0_SERVICE_UUID)
        if (fff0Service != null) {
            writeCharacteristic = fff0Service.getCharacteristic(FFF2_WRITE_UUID)
                ?: fff0Service.getCharacteristic(FFF1_NOTIFY_UUID)
            val notifyChar = fff0Service.getCharacteristic(FFF1_NOTIFY_UUID)
            if (writeCharacteristic != null && notifyChar != null) {
                return enableNotifications(gattClient, notifyChar)
            }
        }

        return false
    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gattClient: BluetoothGatt, characteristic: BluetoothGattCharacteristic): Boolean {
        if (!gattClient.setCharacteristicNotification(characteristic, true)) return false
        val descriptor = characteristic.getDescriptor(CCCD_DESCRIPTOR_UUID)
        if (descriptor != null) {
            val value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0)
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            else if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE else return false
            if (!readiness.beginSubscription()) return false
            return if (android.os.Build.VERSION.SDK_INT >= 33)
                gattClient.writeDescriptor(descriptor, value) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            else { descriptor.value = value; gattClient.writeDescriptor(descriptor) }
        }
        return false
    }

    @SuppressLint("MissingPermission")
    override suspend fun disconnect() = withContext(ioDispatcher + kotlinx.coroutines.NonCancellable) {
        readiness.disconnect()
        connectionDeferred?.complete(false)
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {}
        gatt = null
        writeCharacteristic = null
    }

    @SuppressLint("MissingPermission")
    override suspend fun write(data: ByteArray) = withContext(ioDispatcher) {
        if (!isConnected) throw TransportDisconnectedException("Cannot write: BLE transport is disconnected")
        val char = writeCharacteristic ?: throw TransportDisconnectedException("BLE write characteristic not resolved")
        val gattClient = gatt ?: throw TransportDisconnectedException("GATT is null")

        // BLE standard MTU chunks (max 20 bytes per chunk if not MTU negotiated)
        var offset = 0
        while (offset < data.size) {
            if (!isConnected || gatt !== gattClient) throw TransportDisconnectedException("BLE generation closed")
            val chunkSize = minOf(20, data.size - offset)
            val chunk = data.copyOfRange(offset, offset + chunkSize)
            val writeType = if (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0)
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE else BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            val accepted = if (android.os.Build.VERSION.SDK_INT >= 33)
                gattClient.writeCharacteristic(char, chunk, writeType) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            else { char.value = chunk; char.writeType = writeType; gattClient.writeCharacteristic(char) }
            if (!accepted) throw TransportDisconnectedException("BLE write rejected")
            offset += chunkSize
            if (offset < data.size) {
                kotlinx.coroutines.delay(10)
            }
        }
    }
}
