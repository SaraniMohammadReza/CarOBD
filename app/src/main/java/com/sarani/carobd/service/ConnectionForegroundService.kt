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
 * Owns visible foreground notification state; service loss retires its bound session.
 */
class ConnectionForegroundService : Service(), KoinComponent {

    private val preferences: com.sarani.carobd.core.storage.AppPreferencesManager by inject()
    private var stringsContext: Context? = null
    private fun text(id: Int, vararg args: Any): String = (stringsContext ?: this).getString(id, *args)
    private val repository: ObdRepository by inject()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var sessionId: String? = null
    private var observer: Job? = null

    companion object {
        const val CHANNEL_ID = "carobd_telemetry_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.sarani.carobd.service.ACTION_START"
        const val ACTION_STOP = "com.sarani.carobd.service.ACTION_STOP"
        const val ACTION_DISCONNECT = "com.sarani.carobd.service.ACTION_DISCONNECT"

        fun start(context: Context, sessionId: String) {
            val intent = Intent(context, ConnectionForegroundService::class.java).apply {
                action = ACTION_START
                putExtra("sessionId", sessionId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ConnectionForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch { preferences.language.collect { language -> stringsContext = com.sarani.carobd.core.storage.LocaleHelper.wrapContext(this@ConnectionForegroundService, language) } }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                if (intent.getStringExtra("sessionId") != repository.foregroundSessionId.value) return START_NOT_STICKY
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
                val token = intent.getStringExtra("sessionId")
                if (token == null || token != repository.foregroundSessionId.value) {
                    stopSelf(startId)
                    return START_NOT_STICKY
                }
                if (sessionId != null && sessionId != token) observer?.cancel()
                sessionId = token
                try {
                    com.sarani.carobd.core.permissions.AndroidAccess.require(this, com.sarani.carobd.core.model.RuntimeAccess.Notifications)
                    startForegroundWithNotification()
                    repository.foregroundServiceReady(token)
                    if (observer?.isActive != true) observeConnectionAndTelemetry()
                } catch (e: Exception) {
                    repository.foregroundServiceFailed(token, "Foreground notification unavailable: ${e.javaClass.simpleName}")
                    stopServiceSafely()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val initialNotification = buildNotification(text(R.string.notify_initial_title), text(R.string.notify_initial_body))
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
        observer = serviceScope.launch {
            try {
            combine(
                repository.connectionState,
                repository.liveTelemetry,
                repository.activeVehicleProfile,
                kotlinx.coroutines.flow.flow { while (true) { emit(System.currentTimeMillis()); kotlinx.coroutines.delay(1000) } }
            ) { conn, tele, profile, _ ->
                Triple(conn, tele, profile)
            }.collect { (conn, tele, profile) ->
                when (conn) {
                    is ConnectionState.Connected -> {
                        val title = "${profile?.modelNameFa ?: "خودرو"} • ${conn.deviceName}"
                        val speed = tele.signal("010D").takeIf { it.quality == com.sarani.carobd.core.model.SignalQuality.GOOD }?.value?.toString() ?: "—"
                        val rpm = tele.signal("010C").takeIf { it.quality == com.sarani.carobd.core.model.SignalQuality.GOOD }?.value?.toString() ?: "—"
                        val coolant = tele.signal("0105").takeIf { it.quality == com.sarani.carobd.core.model.SignalQuality.GOOD }?.value?.toString() ?: "—"
                        val voltage = tele.signal("ATRV").takeIf { it.quality == com.sarani.carobd.core.model.SignalQuality.GOOD }?.value?.toString() ?: "—"
                        val content = if (tele.isMeasured("010D") || tele.isMeasured("010C")) {
                            text(R.string.notify_measurements, speed, rpm, coolant)
                        } else {
                            text(R.string.notify_voltage, conn.protocolName, voltage)
                        }
                        updateNotification(title, content)
                    }
                    is ConnectionState.Failure -> {
                        updateNotification(text(R.string.notify_failure), conn.technicalMessage)
                    }
                    is ConnectionState.Disconnecting -> {
                        updateNotification(text(R.string.notify_disconnecting), text(R.string.notify_disconnecting_body))
                    }
                    is ConnectionState.AdapterIdentifying -> {
                        updateNotification(text(R.string.notify_adapter_identity), conn.probeStage)
                    }
                    is ConnectionState.AdapterConfiguring -> {
                        updateNotification(text(R.string.notify_adapter_config), conn.setting)
                    }
                    is ConnectionState.ProtocolDetecting -> {
                        updateNotification(text(R.string.notify_protocol), "${conn.candidateName} (${conn.attemptIndex}/${conn.totalCandidates})")
                    }
                    is ConnectionState.Disconnected -> {
                        stopServiceSafely()
                    }
                    else -> {
                        updateNotification(text(R.string.notify_connecting), text(R.string.notify_connecting_body))
                    }
                }
            }
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { sessionId?.let { repository.foregroundServiceFailed(it, "Notification update failed") }; stopServiceSafely() }
        }
    }

    private fun updateNotification(title: String, content: String) {
        com.sarani.carobd.core.permissions.AndroidAccess.require(this, com.sarani.carobd.core.model.RuntimeAccess.Notifications)
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
            putExtra("sessionId", sessionId)
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
                text(R.string.notify_disconnect),
                disconnectPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                text(R.string.notify_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = text(R.string.notify_channel_description)
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
        sessionId?.let { repository.foregroundServiceFailed(it, "Foreground service destroyed") }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}