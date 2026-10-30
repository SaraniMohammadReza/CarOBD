package com.sarani.carobd.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sarani.carobd.R
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.permissions.AndroidAccess

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Requests are user initiated. Granting access never retries a connection or command. */
@Composable
fun RuntimeAccessControls(accesses: Set<RuntimeAccess> = RuntimeAccess.entries.toSet()) {
    val recorder = org.koin.compose.koinInject<com.sarani.carobd.data.recording.RecordingController>()
    val context = LocalContext.current
    val activity = context.activity()
    var revision by remember { mutableIntStateOf(0) }
    var requesting by rememberSaveable { mutableStateOf(RuntimeAccess.Connect.name) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        AndroidAccess.markRequested(context, RuntimeAccess.valueOf(requesting))
        val result = AndroidAccess.state(context, RuntimeAccess.valueOf(requesting), activity)
        recorder.bench(if (result == AccessState.Granted) BenchPhase.PermissionGranted else BenchPhase.PermissionDenied, requesting + ":" + result.name)
        revision++
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) revision++ }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    Column {
        accesses.forEach { access ->
            val state = remember(revision, access) { AndroidAccess.state(context, access, activity) }
            val label = stringResource(when (access) {
                RuntimeAccess.Connect -> R.string.access_connect
                RuntimeAccess.Scan -> R.string.access_scan
                RuntimeAccess.Notifications -> R.string.access_notifications
                RuntimeAccess.LocalNetwork -> R.string.qa_network_access
            })
            val status = stringResource(when (state) {
                AccessState.Granted -> R.string.access_granted
                AccessState.NotRequested -> R.string.access_not_requested
                AccessState.Denied -> R.string.access_denied
                AccessState.PermanentlyDenied -> R.string.access_permanent
                AccessState.UnsupportedDevice -> R.string.access_unsupported
                AccessState.BluetoothUnavailable -> R.string.access_unavailable
                AccessState.BluetoothDisabled -> R.string.access_disabled
                AccessState.LocationDisabled -> R.string.qa_location_disabled
            })
            if (state != AccessState.Granted) Text(stringResource(when (access) {
                RuntimeAccess.Connect, RuntimeAccess.Scan -> R.string.qa_bluetooth_reason
                RuntimeAccess.Notifications -> R.string.qa_notifications_reason
                RuntimeAccess.LocalNetwork -> R.string.qa_network_reason
            }), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            if (state == AccessState.Granted || state == AccessState.UnsupportedDevice || state == AccessState.BluetoothUnavailable) Text("$label: $status")
            else TextButton(onClick = {
                when (state) {
                    AccessState.PermanentlyDenied -> context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    AccessState.LocationDisabled -> context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    AccessState.BluetoothDisabled -> context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    AccessState.NotRequested, AccessState.Denied -> { requesting = access.name; recorder.bench(BenchPhase.PermissionRequested, access.name); launcher.launch(AndroidAccess.permissions(access)) }
                    else -> Unit
                }
            }) { Text("$label: $status") }
        }
    }
}
