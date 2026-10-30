package com.sarani.carobd.core.permissions

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sarani.carobd.core.model.*

/** The sole platform snapshot/admission policy. No adapter access before permission admission. */
object AndroidAccess {
    fun permissions(access: RuntimeAccess): Array<String> = when (access) {
        RuntimeAccess.Connect -> if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()
        RuntimeAccess.Scan -> if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        RuntimeAccess.Notifications -> if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()
    }
    fun state(context: Context, access: RuntimeAccess, activity: Activity? = null): AccessState {
        val missing = permissions(access).filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        val history = context.getSharedPreferences("runtime_access", Context.MODE_PRIVATE)
        val permission = RuntimeAdmission.evaluate(AccessSnapshot(
            permissionGranted = missing.isEmpty(),
            requested = missing.isNotEmpty() && missing.all { history.getBoolean(it, false) },
            showRationale = missing.any { activity?.shouldShowRequestPermissionRationale(it) == true },
        ))
        if (permission != AccessState.Granted) return permission
        if (access == RuntimeAccess.Notifications) {
            if (!androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) return AccessState.PermanentlyDenied
            if (Build.VERSION.SDK_INT >= 26 && context.getSystemService(android.app.NotificationManager::class.java)?.getNotificationChannel("carobd_telemetry_channel")?.importance == android.app.NotificationManager.IMPORTANCE_NONE)
                return AccessState.PermanentlyDenied
            return AccessState.Granted
        }
        val ble = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (access == RuntimeAccess.Scan && !ble && !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) return AccessState.UnsupportedDevice
        return try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            when { adapter == null -> AccessState.BluetoothUnavailable; !adapter.isEnabled -> AccessState.BluetoothDisabled; else -> AccessState.Granted }
        } catch (_: SecurityException) { AccessState.Denied }
    }
    fun markRequested(context: Context, access: RuntimeAccess) {
        context.getSharedPreferences("runtime_access", Context.MODE_PRIVATE).edit().apply {
            permissions(access).forEach { putBoolean(it, true) }
        }.apply()
    }
    fun require(context: Context, access: RuntimeAccess) {
        val state = state(context, access)
        if (state != AccessState.Granted) throw AccessDeniedException(access, state)
    }
    fun requireTransport(context: Context, config: TransportConfig) {
        if (config is TransportConfig.BluetoothClassic || config is TransportConfig.Ble) require(context, RuntimeAccess.Connect)
        if (config is TransportConfig.Ble && !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            throw AccessDeniedException(RuntimeAccess.Connect, AccessState.UnsupportedDevice)
    }
}
