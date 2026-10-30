package com.sarani.carobd

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.flow.first

/** Platform automation avoids Espresso's removed InputManager reflection on API 37. No physical connection. */
class RuntimeNavigationInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private lateinit var activity: Activity
    private val locale get() = InstrumentationRegistry.getArguments().getString("qaLocale", "fa")
    private fun root(): AccessibilityNodeInfo {
        if (android.os.Build.VERSION.SDK_INT >= 33) automation.clearCache()
        val deadline = SystemClock.uptimeMillis() + 5000
        do {
            automation.rootInActiveWindow?.let { return it }
            SystemClock.sleep(100)
        } while (SystemClock.uptimeMillis() < deadline)
        error("No active accessibility window after transition")
    }
    private fun label(id: Int) = activity.getString(id)
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }
    private fun find(text: String) = nodes(root()).lastOrNull { it.text?.toString() == text && it.isVisibleToUser }
    private fun await(text: String): AccessibilityNodeInfo {
        val end = SystemClock.uptimeMillis() + 15_000
        var scrollAttempt = 0
        while (SystemClock.uptimeMillis() < end) {
            find(text)?.let { return it }
            swipe((scrollAttempt++ / 4) % 2 == 0)
            SystemClock.sleep(300)
        }
        error("Visible text not found: $text; visible: " + nodes(root()).mapNotNull { it.text }.joinToString(" | "))
    }
    private fun swipe(up: Boolean) {
        val bounds = Rect(); root().getBoundsInScreen(bounds)
        val x = bounds.exactCenterX()
        val from = bounds.top + bounds.height() * (if (up) 0.75f else 0.35f)
        val to = bounds.top + bounds.height() * (if (up) 0.35f else 0.75f)
        val start = SystemClock.uptimeMillis()
        for (step in 0..12) {
            val action = if (step == 0) MotionEvent.ACTION_DOWN else if (step == 12) MotionEvent.ACTION_UP else MotionEvent.ACTION_MOVE
            val event = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, x, from + (to - from) * step / 12, 0)
            automation.injectInputEvent(event, true); event.recycle(); SystemClock.sleep(20)
        }
    }
    private fun tap(text: String) {
        await(text).performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        SystemClock.sleep(500)
        tapNode(await(text))
    }
    private fun tapNode(node: AccessibilityNodeInfo) {
        var target: AccessibilityNodeInfo? = node
        while (target != null && !target.isClickable) target = target.parent
        if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) { SystemClock.sleep(800); return }
        val rect = Rect(); node.getBoundsInScreen(rect)
        val now = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, rect.exactCenterX(), rect.exactCenterY(), 0)
            assertTrue(automation.injectInputEvent(event, true)); event.recycle()
        }
        SystemClock.sleep(800)
    }
    private fun permissionButton(id: String): AccessibilityNodeInfo {
        val end = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < end) {
            nodes(root()).firstOrNull { it.viewIdResourceName?.endsWith(":id/$id") == true }?.let { return it }
            SystemClock.sleep(200)
        }
        error("Permission dialog button not found: $id")
    }
    private fun permissionDenialAndRecovery() {
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        tap(label(R.string.access_notifications) + ": " + label(R.string.access_not_requested))
        shot("notification-system-request")
        tapNode(permissionButton("permission_deny_button"))
        assertTrue(instrumentation.targetContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
        shot("notification-denied")
        tap(label(R.string.access_notifications) + ": " + label(R.string.access_denied))
        tapNode(permissionButton("permission_allow_button"))
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED, instrumentation.targetContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS))
        shot("notification-granted")
        tap(label(R.string.access_scan) + ": " + label(R.string.access_not_requested))
        shot("bluetooth-system-request")
        tapNode(permissionButton("permission_deny_button"))
        assertTrue(instrumentation.targetContext.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED)
        shot("bluetooth-denied")
        tap(label(R.string.access_scan) + ": " + label(R.string.access_denied))
        tapNode(permissionButton("permission_allow_button"))
        assertEquals(android.content.pm.PackageManager.PERMISSION_GRANTED, instrumentation.targetContext.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN))
        shot("bluetooth-granted")
    }
    private fun pickerStates() {
        tap(label(R.string.ui_add))
        tap(label(R.string.connection_bt)); await(label(R.string.qa_scan_idle)); shot("classic-picker-idle")
        tap(label(R.string.ui_scan_classic))
        val end = SystemClock.uptimeMillis() + 12_000
        while (find(label(R.string.qa_scan_timeout)) == null && find(label(R.string.qa_scan_failed)) == null && SystemClock.uptimeMillis() < end) SystemClock.sleep(200)
        await(label(R.string.qa_scan_timeout))
        shot("classic-picker-result")
        tap(label(R.string.ui_scan_classic)); tap(label(R.string.qa_stop_scan)); await(label(R.string.qa_scan_canceled)); shot("classic-picker-canceled")
        tap(label(R.string.ui_cancel))
        tap(label(R.string.ui_add)); tap(label(R.string.connection_ble)); await(label(R.string.qa_scan_idle)); shot("ble-picker-idle"); tap(label(R.string.ui_cancel))
        tap(label(R.string.ui_add)); tap(label(R.string.connection_wifi)); await(label(R.string.ui_host)); shot("wifi-profile"); tap(label(R.string.ui_cancel))
        tap(label(R.string.ui_add)); tap(label(R.string.connection_usb)); await(label(R.string.ui_usb_help)); shot("usb-profile"); tap(label(R.string.ui_cancel))
    }
    private fun readConnectedEvidence() {
        for ((destination, name) in listOf<Pair<Int?, String>>(null to "readiness-loaded", R.string.vehicle_information to "mode09-loaded", R.string.freeze_frame to "freeze-frame-loaded", R.string.monitor_tests to "mode06-loaded")) {
            nav(R.string.nav_diagnostics); tap(label(R.string.open_evidence))
            destination?.let { tap(label(it)) }
            tap(label(R.string.read_evidence))
            val deadline = SystemClock.uptimeMillis() + 12_000
            var successful = false
            while (SystemClock.uptimeMillis() < deadline) {
                successful = nodes(root()).any { it.text?.toString()?.contains(label(R.string.ui_read_success)) == true }
                if (successful) break
                swipe(true); SystemClock.sleep(200)
            }
            assertTrue("No decoded success for $name", successful)
            shot(name)
        }
    }
    private fun shot(name: String) {
        automation.executeShellCommand("mkdir -p /sdcard/Download/carobd-runtime-qa").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        automation.executeShellCommand("screencap -p /sdcard/Download/carobd-runtime-qa/${locale}-$name.png").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
    }
    private fun nav(id: Int, more: Boolean = false) {
        if (more) tap(label(R.string.qa_more))
        tap(label(id))
    }
    private fun back() { automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK); SystemClock.sleep(800) }
    @Test fun everyDestination_opensWithoutPhysicalConnection() {
        kotlinx.coroutines.runBlocking {
            com.sarani.carobd.core.storage.AppPreferencesManager(instrumentation.targetContext).setLanguage(
                if (locale == "en") com.sarani.carobd.core.storage.AppLanguage.ENGLISH else com.sarani.carobd.core.storage.AppLanguage.PERSIAN)
        }
        automation.executeShellCommand("cmd statusbar collapse").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        await(label(R.string.ui_onboarding_save))
        shot("onboarding"); tap(label(R.string.ui_onboarding_save))
        await(label(R.string.qa_more))
        shot("dashboard-disconnected")
        nav(R.string.nav_connection_center); await(label(R.string.ui_connection_title)); shot("connection-center")
        permissionDenialAndRecovery()
        pickerStates()
        automation.executeShellCommand("cmd bluetooth_manager disable").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        SystemClock.sleep(1000)
        nav(R.string.nav_settings); nav(R.string.nav_connection_center)
        await(label(R.string.access_scan) + ": " + label(R.string.access_disabled)); shot("bluetooth-disabled")
        automation.executeShellCommand("cmd bluetooth_manager enable").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        SystemClock.sleep(1000)
        nav(R.string.nav_settings); nav(R.string.nav_connection_center)
        await(label(R.string.access_scan) + ": " + label(R.string.access_granted))
        nav(R.string.nav_diagnostics); tap(label(R.string.open_evidence)); await(label(R.string.readiness_title)); shot("evidence")
        for ((id, name) in listOf(R.string.monitor_tests to "monitor-tests", R.string.dtc_history to "dtc-history", R.string.recordings_title to "recordings", R.string.freeze_frame to "freeze-frame", R.string.vehicle_information to "vehicle-information")) {
            nav(R.string.nav_diagnostics); tap(label(R.string.open_evidence)); tap(label(id)); shot(name)
        }
        nav(R.string.nav_settings); shot("settings")
        tap(label(R.string.ui_theme_light)); shot("settings-daylight")
        assertEquals(com.sarani.carobd.core.storage.AppThemeMode.LIGHT_THEME, kotlinx.coroutines.runBlocking { com.sarani.carobd.core.storage.AppPreferencesManager(instrumentation.targetContext).themeMode.first() })
        tap(label(R.string.ui_theme_amoled))
        for ((id, name) in listOf(R.string.nav_maintenance to "maintenance", R.string.nav_trip to "trip", R.string.nav_ecu_profiles to "vehicle-catalogue", R.string.nav_terminal to "terminal", R.string.nav_simulator to "simulator-selector")) { nav(id, true); shot(name) }
        tapNode(nodes(root()).first { it.text?.toString() == label(R.string.action_connect) && it.isVisibleToUser })
        val repo = org.koin.java.KoinJavaComponent.get<com.sarani.carobd.domain.repository.ObdRepository>(com.sarani.carobd.domain.repository.ObdRepository::class.java)
        val deadline = SystemClock.uptimeMillis() + 20_000
        while (repo.connectionState.value !is com.sarani.carobd.core.model.ConnectionState.Connected && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(200)
        assertTrue(repo.connectionState.value.toString(), (repo.connectionState.value as? com.sarani.carobd.core.model.ConnectionState.Connected)?.isSimulator == true)
        nav(R.string.nav_dashboard); await(label(R.string.simulator_warning)); shot("dashboard-simulation")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
        SystemClock.sleep(1000)
        assertTrue((repo.connectionState.value as? com.sarani.carobd.core.model.ConnectionState.Connected)?.isSimulator == true)
        val notifications = instrumentation.targetContext.getSystemService(android.app.NotificationManager::class.java)
        assertTrue(notifications.activeNotifications.any { it.id == com.sarani.carobd.service.ConnectionForegroundService.NOTIFICATION_ID })
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS); SystemClock.sleep(500); shot("background-foreground-notification")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        automation.executeShellCommand("cmd statusbar collapse").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        automation.executeShellCommand("am start -n com.sarani.carobd/.MainActivity").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        SystemClock.sleep(800)
        nav(R.string.nav_diagnostics); tap(label(R.string.dtc_scan_button)); SystemClock.sleep(1000); shot("diagnostics-simulation")
        nav(R.string.nav_diagnostics); tap(label(R.string.open_evidence)); tap(label(R.string.dtc_history))
        await(label(R.string.diagnosis_scope)); shot("recent-diagnosis")
        tap(label(R.string.diagnosis_scope)); await(label(R.string.diagnosis_adapter_reported)); shot("diagnosis-details")
        readConnectedEvidence()
        nav(R.string.nav_connection_center); tap(label(R.string.ui_disconnect_cancel)); shot("disconnected-after-simulation")
        assertFalse(repo.connectionState.value is com.sarani.carobd.core.model.ConnectionState.Connected)
        nav(R.string.nav_simulator, true)
        val stopped = com.sarani.carobd.data.transport.SimulatorScenario.ENGINE_STOPPED
        tap(if (locale == "fa") stopped.labelFa else stopped.labelEn)
        tapNode(nodes(root()).first { it.text?.toString() == label(R.string.action_connect) && it.isVisibleToUser })
        val stoppedDeadline = SystemClock.uptimeMillis() + 20_000
        while (repo.connectionState.value !is com.sarani.carobd.core.model.ConnectionState.Connected && SystemClock.uptimeMillis() < stoppedDeadline) SystemClock.sleep(200)
        assertTrue((repo.connectionState.value as? com.sarani.carobd.core.model.ConnectionState.Connected)?.isSimulator == true)
        nav(R.string.nav_dashboard); await("0 RPM"); await("0 km/h"); shot("simulation-engine-stopped")
        nav(R.string.nav_connection_center); tap(label(R.string.ui_disconnect_cancel))
        nav(R.string.nav_settings); tap(label(R.string.settings_adapter_inspector)); shot("adapter-inspector")
        nav(R.string.nav_settings); await(label(R.string.settings_language))
        tap(label(if (locale == "en") R.string.language_persian_label else R.string.language_english_label))
        val nextLanguage = if (locale == "fa") com.sarani.carobd.core.storage.AppLanguage.ENGLISH else com.sarani.carobd.core.storage.AppLanguage.PERSIAN
        val config = android.content.res.Configuration(activity.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(nextLanguage.tag)) }
        await(activity.createConfigurationContext(config).getString(R.string.qa_more))
        assertEquals(nextLanguage, kotlinx.coroutines.runBlocking { com.sarani.carobd.core.storage.AppPreferencesManager(instrumentation.targetContext).language.first() })
        shot("language-changed-and-recreated")
    }
}
