package com.sarani.carobd.data.transport

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Production Android USB Host (OTG) OBD-II transport for wired USB scanners and cables.
 * Supports FTDI (FT232R), Silicon Labs (CP2102), WCH (CH340), Prolific (PL2303) and standard CDC-ACM devices.
 *
 * Permission model: USB host access requires a one-time user grant. When the device is not yet
 * permitted, connect() suspends on a dynamically-registered ACTION_USB_PERMISSION receiver until
 * the user responds or the 15s timeout elapses. No manifest receiver is required.
 */
class UsbSerialTransport(
    private val context: Context,
    private val selection: com.sarani.carobd.core.model.TransportConfig.Usb,
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

    @Volatile private var retired = false
    @Volatile private var usbConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null
    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null
    private var readerThread: Thread? = null

    // One-shot USB permission plumbing: a deferred is parked while the system permission
    // dialog is shown and completed by the ACTION_USB_PERMISSION broadcast receiver.
    @Volatile
    private var permissionReceiver: BroadcastReceiver? = null
    @Volatile
    private var pendingPermissionResult: CompletableDeferred<Boolean>? = null

    companion object {
        val KNOWN_SERIAL_VIDS = setOf(
            0x0403, // FTDI
            0x10C4, // Silicon Labs CP210x
            0x1A86, // QinHeng Electronics CH340 / CH341
            0x067B  // Prolific PL2303
        )

        private const val PERMISSION_REQUEST_CODE = 0
        private const val PERMISSION_TIMEOUT_MS = 15_000L

        // ACTION_USB_PERMISSION was hidden from the public UsbManager API surface on
        // Android 12 (API 31)+, but the system still broadcasts this action value carrying
        // EXTRA_PERMISSION_GRANTED / EXTRA_DEVICE via the PendingIntent overload. Using the
        // literal keeps the requestPermission(PendingIntent) flow compiling and working on
        // API 24 through 37.
        private const val ACTION_USB_PERMISSION = "android.hardware.usb.action.USB_PERMISSION"
    }

    override suspend fun connect() = withContext(ioDispatcher) {
        check(!retired) { "USB generation retired" }
        if (_isConnected) return@withContext

        val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw ConnectionFailedException("USB Host service is not available on this device")

        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            throw ConnectionFailedException("هیچ کابل یا دانگل USB دیاگ به درگاه گوشی متصل نشده است. اتصال کابل OTG را بررسی فرمایید.")
        }

        val identity = com.sarani.carobd.core.model.UsbTargetSelection.select(selection,
            deviceList.values.map { com.sarani.carobd.core.model.UsbDeviceIdentity(it.deviceId, it.vendorId, it.productId, it.interfaceCount) })
        val candidateDevice = deviceList.values.single { it.deviceId == identity.deviceId }

        if (!usbManager.hasPermission(candidateDevice)) {
            // Ask the user for USB host permission and suspend until a result arrives.
            requestPermissionSuspending(usbManager, candidateDevice)
        }

        check(!retired) { "USB generation retired" }
        val conn = usbManager.openDevice(candidateDevice)
            ?: throw ConnectionFailedException("امکان باز کردن درگاه ارتباطی USB وجود ندارد.")

        usbConnection = conn
        if (retired) { conn.close(); throw ConnectionFailedException("USB generation retired") }
        // Find interface with IN and OUT endpoints
        var targetIface: UsbInterface? = null
        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null

        for (i in listOf(requireNotNull(selection.portIndex))) {
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
            val acknowledged = conn.controlTransfer(0x21, 0x20, 0, targetIface.id, baudBytes, baudBytes.size, 1000)
            check(acknowledged == baudBytes.size) { "USB_SERIAL_DRIVER_UNAVAILABLE: selected interface did not accept CDC line coding" }
        } catch (e: Exception) {
            conn.close()
            throw ConnectionFailedException("USB line configuration failed: ${e.message}", e)
        }

        if (retired) { conn.close(); throw ConnectionFailedException("USB generation retired") }
        usbInterface = targetIface
        endpointIn = inEp
        endpointOut = outEp
        _isConnected = true

        startReaderLoop(conn, inEp, candidateDevice.deviceId)
    }

    private fun startReaderLoop(conn: UsbDeviceConnection, inEp: UsbEndpoint, selectedDeviceId: Int) {
        readerThread = Thread({
            val buffer = ByteArray(1024)
            while (_isConnected && !Thread.currentThread().isInterrupted) {
                val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                if (manager.deviceList.values.none { it.deviceId == selectedDeviceId }) {
                    _isConnected = false
                    break
                }
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

    /**
     * Requests USB host permission for [device] and suspends until the user responds.
     * Uses a dynamically-registered ACTION_USB_PERMISSION receiver (no manifest entry needed),
     * completed via a one-shot [pendingPermissionResult] deferred. Fails fast on timeout or
     * denial so the caller surfaces a clear error instead of hanging.
     */
    private suspend fun requestPermissionSuspending(
        usbManager: UsbManager,
        device: UsbDevice
    ) {
        val deferred = CompletableDeferred<Boolean>()
        pendingPermissionResult = deferred

        val permissionIntent = PendingIntent.getBroadcast(
            context,
            PERMISSION_REQUEST_CODE,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action != ACTION_USB_PERMISSION) return
                @Suppress("DEPRECATION")
                val extraDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (extraDevice?.deviceId != device.deviceId) return
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                if (!deferred.isCompleted) deferred.complete(granted)
            }
        }
        permissionReceiver = receiver
        val appContext = context.applicationContext
        // Android 13/U+ requires an explicit exported flag for dynamically-registered
        // receivers on non-system broadcasts. This receiver only handles our own
        // ACTION_USB_PERMISSION result, so it must NOT be exported.
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        try {
            usbManager.requestPermission(device, permissionIntent)
            val granted = try {
                withTimeout(PERMISSION_TIMEOUT_MS) { deferred.await() }
            } catch (e: TimeoutCancellationException) {
                throw ConnectionFailedException("درخواست مجوز دسترسی USB منقضی شده است. مجدداً تلاش کنید.")
            }
            if (!granted) {
                throw ConnectionFailedException("دسترسی به درگاه USB توسط کاربر رد شده است.")
            }
        } finally {
            pendingPermissionResult = null
            permissionReceiver?.let {
                try { appContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
            }
            permissionReceiver = null
        }
    }

    override suspend fun disconnect() = withContext(ioDispatcher + kotlinx.coroutines.NonCancellable) {
        retired = true
        _isConnected = false
        // Cancel any in-flight permission request and release its receiver.
        pendingPermissionResult?.let { if (!it.isCompleted) it.complete(false) }
        pendingPermissionResult = null
        permissionReceiver?.let {
            try { context.applicationContext.unregisterReceiver(it) } catch (_: IllegalArgumentException) {}
        }
        permissionReceiver = null
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
        if (result != data.size) {
            disconnect()
            throw TransportDisconnectedException("USB write failed: bulkTransfer returned $result")
        }
    }
}