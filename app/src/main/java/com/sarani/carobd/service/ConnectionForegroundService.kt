package com.sarani.carobd.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sarani.carobd.MainActivity
import com.sarani.carobd.R
import com.sarani.carobd.core.model.ConnectionState
import com.sarani.carobd.domain.repository.ObdRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Android 14+ Foreground Service for continuous automotive diagnostic communication and trip telemetry.
 * Ensures OS low-memory killer does not disrupt live ECU polling while driving or running background diagnostics.
 */
class ConnectionForegroundService : Service(), KoinComponent {

    private val repository: ObdRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    companion object {
        const val CHANNEL_ID = "carobd_telemetry_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.sarani.carobd.service.ACTION_START"
        const val ACTION_STOP = "com.sarani.carobd.service.ACTION_STOP"
        const val ACTION_DISCONNECT = "com.sarani.carobd.service.ACTION_DISCONNECT"

        fun start(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                serviceScope.launch {
                    repository.disconnect()
                    stopServiceSafely()
                }
                return START_NOT_STICKY
            }
            ACTION_STOP -> {
                stopServiceSafely()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForegroundWithNotification()
                observeConnectionAndTelemetry()
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val initialNotification = buildNotification("در حال اتصال به خودرو...", "پایش زنده پارامترهای موتور")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                initialNotification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    private fun observeConnectionAndTelemetry() {
        serviceScope.launch {
            combine(
                repository.connectionState,
                repository.liveTelemetry,
                repository.activeVehicleProfile
            ) { conn, tele, profile ->
                Triple(conn, tele, profile)
            }.collect { (conn, tele, profile) ->
                when (conn) {
                    is ConnectionState.Connected -> {
                        val title = "${profile.modelNameFa} • ${conn.deviceName}"
                        val content = if (tele.speedKmh > 0 || tele.rpm > 0) {
                            "سرعت: ${tele.speedKmh} km/h | دور موتور: ${tele.rpm} RPM | آب: ${tele.coolantTempCelsius}°C"
                        } else {
                            "ایسیو آنلاین (${conn.protocolName}) | ولتاژ: ${String.format(java.util.Locale.US, "%.1f", tele.batteryVoltage)}V"
                        }
                        updateNotification(title, content)
                    }
                    is ConnectionState.Connecting -> {
                        updateNotification("در حال اتصال به دانگل دیاگ...", "ارسال سیگنال‌های آغازین")
                    }
                    is ConnectionState.Initializing -> {
                        updateNotification("هماهنگ‌سازی با ایسیو...", conn.step)
                    }
                    is ConnectionState.Disconnected -> {
                        stopServiceSafely()
                    }
                    is ConnectionState.Error -> {
                        updateNotification("خطا در ارتباط", conn.message)
                    }
                }
            }
        }
    }

    private fun updateNotification(title: String, content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    @SuppressLint("LaunchActivityFromNotification")
    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, ConnectionForegroundService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "قطع اتصال (Disconnect)",
                disconnectPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "پایش تله‌متری دیاگ خودرو",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش زنده سرعت، دور موتور و وضعیت اتصال به ایسیو در پس‌زمینه"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stopServiceSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}