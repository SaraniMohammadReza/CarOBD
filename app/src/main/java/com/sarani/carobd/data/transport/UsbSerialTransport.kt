package com.sarani.carobd.data.transport

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.sarani.carobd.core.adapter.transport.ConnectionFailedException
import com.sarani.carobd.core.adapter.transport.ObdTransport
import com.sarani.carobd.core.adapter.transport.TransportDisconnectedException
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * Production Android USB Host (OTG) OBD-II transport for wired USB scanners and cables.
 * Supports FTDI (FT232R), Silicon Labs (CP2102), WCH (CH340), Prolific (PL2303) and standard CDC-ACM devices.
 */
class UsbSerialTransport(
    private val context: Context,
    private val baudRate: Int = 38400,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ObdTransport {

    override val transportType: TransportType = TransportType.USB_SERIAL

    @Volatile
    private var _isConnected: Boolean = false
    override val isConnected: Boolean
        get() = _isConnected

    private val _incomingBytes = MutableSharedFlow<ByteArray>(extraBufferCapacity = 128)
    override val incomingBytes: Flow<ByteArray> = _incomingBytes.asSharedFlow()

    private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var readerThread: Thread? = null

    companion object {
        val KNOWN_SERIAL_VIDS = setOf(
            0x0403, // FTDI
            0x10C4, // Silicon Labs CP210x
            0x1A86, // QinHeng Electronics CH340 / CH341
            0x067B  // Prolific PL2303
        )
    }

    override suspend fun connect() = withContext(ioDispatcher) {
        if (_isConnected) return@withContext

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw ConnectionFailedException("USB Host service is not available on this device")

        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            throw ConnectionFailedException("هیچ کابل یا دانگل USB دیاگ به درگاه گوشی متصل نشده است. اتصال کابل OTG را بررسی فرمایید.")
        }

        // Find candidate device
        val candidateDevice: UsbDevice = deviceList.values.firstOrNull { dev ->
            KNOWN_SERIAL_VIDS.contains(dev.vendorId) ||
                    (0 until dev.interfaceCount).any { i ->
                        val iface = dev.getInterface(i)
                        iface.interfaceClass == UsbConstants.USB_CLASS_CDC_DATA ||
                                iface.interfaceClass == UsbConstants.USB_CLASS_COMM ||
                                iface.interfaceClass == UsbConstants.USB_CLASS_VENDOR_SPEC
                    }
        } ?: deviceList.values.first()

        if (!usbManager.hasPermission(candidateDevice)) {
            throw ConnectionFailedException("مجوز دسترسی به درگاه USB دیاگ صادر نشده است. پنجره درخواست مجوز را تایید نمایید.")
        }

        val conn = usbManager.openDevice(candidateDevice)
            ?: throw ConnectionFailedException("امکان باز کردن درگاه ارتباطی USB وجود ندارد.")

        // Find interface with IN and OUT endpoints
        var targetIface: UsbInterface? = null
        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null

        for (i in 0 until candidateDevice.interfaceCount) {
            val iface = candidateDevice.getInterface(i)
            var currentIn: UsbEndpoint? = null
            var currentOut: UsbEndpoint? = null

            for (e in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(e)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN) currentIn = ep
                    if (ep.direction == UsbConstants.USB_DIR_OUT) currentOut = ep
                }
            }

            if (currentIn != null && currentOut != null) {
                targetIface = iface
                inEp = currentIn
                outEp = currentOut
                break
            }
        }

        if (targetIface == null || inEp == null || outEp == null) {
            conn.close()
            throw ConnectionFailedException("پایانه‌های انتقال داده (Bulk Endpoints) روی این دستگاه USB دیاگ یافت نشد.")
        }

        if (!conn.claimInterface(targetIface, true)) {
            conn.close()
            throw ConnectionFailedException("امکان قفل و در اختیار گرفتن اینترفیس USB وجود ندارد.")
        }

        // Set serial line coding (baud rate) via CDC ACM request 0x20 if supported
        try {
            val baudBytes = byteArrayOf(
                (baudRate and 0xFF).toByte(),
                ((baudRate shr 8) and 0xFF).toByte(),
                ((baudRate shr 16) and 0xFF).toByte(),
                ((baudRate shr 24) and 0xFF).toByte(),
                0x00, // 1 stop bit
                0x00, // parity none
                0x08  // 8 data bits
            )
            conn.controlTransfer(0x21, 0x20, 0, 0, baudBytes, baudBytes.size, 1000)
        } catch (_: Exception) {}

        usbConnection = conn
        usbInterface = targetIface
        endpointIn = inEp
        endpointOut = outEp
        _isConnected = true

        startReaderLoop(conn, inEp)
    }

    private fun startReaderLoop(conn: UsbDeviceConnection, inEp: UsbEndpoint) {
        readerThread = Thread({
            val buffer = ByteArray(1024)
            while (_isConnected && !Thread.currentThread().isInterrupted) {
                val bytesRead = conn.bulkTransfer(inEp, buffer, buffer.size, 100)
                if (bytesRead > 0) {
                    val chunk = buffer.copyOf(bytesRead)
                    _incomingBytes.tryEmit(chunk)
                }
            }
        }, "UsbSerial-Reader").apply {
            isDaemon = true
            start()
        }
    }

    override suspend fun disconnect() = withContext(ioDispatcher) {
        _isConnected = false
        try {
            readerThread?.interrupt()
            readerThread = null
        } catch (_: Exception) {}

        try {
            usbInterface?.let { usbConnection?.releaseInterface(it) }
            usbConnection?.close()
        } catch (_: Exception) {}

        usbConnection = null
        usbInterface = null
        endpointIn = null
        endpointOut = null
    }

    override suspend fun write(data: ByteArray) = withContext(ioDispatcher) {
        if (!_isConnected) throw TransportDisconnectedException("Cannot write: USB serial transport is disconnected")
        val conn = usbConnection ?: throw TransportDisconnectedException("USB connection is null")
        val outEp = endpointOut ?: throw TransportDisconnectedException("USB OUT endpoint is null")

        val result = conn.bulkTransfer(outEp, data, data.size, 2000)
        if (result < 0) {
            disconnect()
            throw TransportDisconnectedException("USB write failed: bulkTransfer returned $result")
        }
    }
}