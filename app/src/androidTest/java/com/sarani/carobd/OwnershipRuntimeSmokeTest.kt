package com.sarani.carobd

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import com.sarani.carobd.core.model.*
import com.sarani.carobd.core.storage.*
import com.sarani.carobd.data.db.CarObdDatabaseHelper
import com.sarani.carobd.data.repository.*
import com.sarani.carobd.core.vehicle.catalog.DefaultVehicleCatalogRepository
import com.sarani.carobd.core.vehicle.parts.DefaultPartsRepository
import com.sarani.carobd.ui.screens.home.HomeViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test

/** Emulator-only owned-data flow, never opens a diagnostic transport. */
class OwnershipRuntimeSmokeTest {
    private val ins=InstrumentationRegistry.getInstrumentation()
    private val auto get()=ins.uiAutomation
    private lateinit var activity:Activity
    private val locale get()=InstrumentationRegistry.getArguments().getString("qaLocale","en")
    private fun label(id:Int)=activity.getString(id)
    private fun nodes(n:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(n==null) emptyList() else listOf(n)+(0 until n.childCount).flatMap{nodes(n.getChild(it))}
    private fun all():List<AccessibilityNodeInfo> { auto.clearCache();return nodes(auto.rootInActiveWindow) }
    private fun await(text:String):AccessibilityNodeInfo {
        val end=SystemClock.uptimeMillis()+15_000
        while(SystemClock.uptimeMillis()<end) {
            all().lastOrNull{it.text?.toString()==text && it.isVisibleToUser}?.let{return it}
            all().lastOrNull{it.text?.toString()==text}?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
            all().firstOrNull{it.isScrollable}?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(200)
        }
        error("Missing $text; visible="+all().mapNotNull{it.text}.joinToString(" | "))
    }
    private fun tap(text:String) {
        val n=await(text);var p:AccessibilityNodeInfo?=n
        while(p!=null && !p.isClickable) p=p.parent
        if(p?.performAction(AccessibilityNodeInfo.ACTION_CLICK)!=true) {
            val rect=Rect();n.getBoundsInScreen(rect);val now=SystemClock.uptimeMillis()
            for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)) {
                val event=MotionEvent.obtain(now,SystemClock.uptimeMillis(),action,rect.exactCenterX(),rect.exactCenterY(),0)
                assertTrue(auto.injectInputEvent(event,true));event.recycle()
            }
        }
        SystemClock.sleep(350)
    }
    private fun field(id:Int,value:String) {
        val text=label(id);await(text)
        val node=all().firstOrNull{it.isEditable && (it.hintText?.toString()==text || nodes(it).any{child->child.text?.toString()==text})}
            ?:error("Missing editable $text")
        assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)}))
        SystemClock.sleep(200)
    }
    private fun shot(name:String) {
        auto.executeShellCommand("mkdir -p /sdcard/Download/carobd-home-qa").use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()}
        auto.executeShellCommand("screencap -p /sdcard/Download/carobd-home-qa/$locale-$name.png").use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()}
    }
    private fun launch() { activity=ins.startActivitySync(Intent(ins.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }

    @Test fun canonicalOwnershipFlow_unknownEngineColdStartAndFitment()=runBlocking {
        assertTrue("This test only runs on an emulator",android.os.Build.MODEL.contains("sdk_gphone"))
        val prefs=AppPreferencesManager(ins.targetContext)
        prefs.setLanguage(if(locale=="fa") AppLanguage.PERSIAN else AppLanguage.ENGLISH)
        prefs.setOnboardingDone(false)
        val owners=org.koin.java.KoinJavaComponent.get<OwnedVehicleRepository>(OwnedVehicleRepository::class.java)
        owners.refresh();assertTrue(owners.vehicles.value.isEmpty())
        val obd=org.koin.java.KoinJavaComponent.get<com.sarani.carobd.domain.repository.ObdRepository>(com.sarani.carobd.domain.repository.ObdRepository::class.java)
        val routing=obd.activeVehicleProfile.value
        launch();tap(label(R.string.onboarding_btn_skip));await(label(R.string.qa_more))
        assertTrue(owners.vehicles.value.isEmpty());assertNull(owners.activeVehicleId.value)
        prefs.setOnboardingDone(false)
        tap(label(R.string.onboarding_btn_next));tap(label(R.string.onboarding_add_owned_vehicle))
        field(R.string.my_vehicle_name,"QA 206 unknown")
        field(R.string.my_vehicle_manufacturer,"IKCO");field(R.string.my_vehicle_model,"206")
        tap(label(R.string.onboarding_btn_next));tap(label(R.string.onboarding_btn_get_started))
        await("QA 206 unknown");shot("home-unknown")
        val v=owners.vehicles.value.single();assertNull(v.currentOdometerKm);assertNull(v.engineCode);assertNull(v.tankCapacityLiters)
        val legacy=prefs.userVehicleSetup.first();assertNull(legacy.odometerKm);assertNull(legacy.fuelPriceTomans);assertNull(legacy.engineDisplacementLiters)
        assertEquals(routing,obd.activeVehicleProfile.value)
        // New repository + Home VM exercises real cold hydration, not a prefilled fake flow.
        CarObdDatabaseHelper(ins.targetContext).use { db ->
            val fresh=SQLiteOwnedVehicleRepository(db)
            assertTrue(fresh.vehicles.value.isEmpty())
            lateinit var home:HomeViewModel
            ins.runOnMainSync { home=HomeViewModel(fresh,SQLiteMaintenanceRepository(db),DefaultPartsRepository(),DefaultVehicleCatalogRepository(),obd) }
            val end=SystemClock.uptimeMillis()+5000
            while(home.uiState.value.activeVehicle==null && SystemClock.uptimeMillis()<end) SystemClock.sleep(100)
            assertEquals(v.id,home.uiState.value.activeVehicle?.id);assertNull(home.uiState.value.nextServiceRemainingKm)
            ins.runOnMainSync { androidx.lifecycle.ViewModelStore().apply { put("home",home);clear() } }
        }
        tap(label(R.string.nav_maintenance));await(label(R.string.maintenance_init_default_schedules))
        tap(label(R.string.maintenance_init_default_schedules));SystemClock.sleep(400)
        val maintenance=org.koin.java.KoinJavaComponent.get<MaintenanceRepository>(MaintenanceRepository::class.java)
        assertTrue(maintenance.getSchedules(v.id).isEmpty());shot("maintenance-ambiguous")
        tap(label(R.string.qa_more));tap(label(R.string.nav_parts));await(label(R.string.parts_no_compatible_found))
        val search=all().first{it.isEditable}
        assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"timing")}))
        await(label(R.string.parts_no_compatible_found));shot("parts-unknown")
        // Explicit known variant, still no physical identification/connection implied.
        owners.save(v.copy(engineCode="TU3",updatedAtMs=System.currentTimeMillis())).getOrThrow()
        tap(label(R.string.nav_my_vehicle));await("QA 206 unknown");shot("owned-selected")
        tap(label(R.string.qa_more));tap(label(R.string.nav_parts));SystemClock.sleep(500)
        val partsVm=com.sarani.carobd.ui.screens.parts.PartsCenterViewModel(owners,DefaultPartsRepository(),DefaultVehicleCatalogRepository())
        val wait=SystemClock.uptimeMillis()+5000
        while(partsVm.uiState.value.resolvedMatch==null && SystemClock.uptimeMillis()<wait) SystemClock.sleep(100)
        partsVm.onIntent(com.sarani.carobd.ui.screens.parts.PartsCenterIntent.UpdateSearchQuery("timing"))
        assertTrue(partsVm.uiState.value.compatibleParts.isNotEmpty())
        assertTrue(partsVm.uiState.value.compatibleParts.all{it.fitment.engineCode?.contains("TU3")==true})
        shot("parts-tu3")
        ins.runOnMainSync { androidx.lifecycle.ViewModelStore().apply { put("parts",partsVm);clear() } }
        assertEquals(routing,obd.activeVehicleProfile.value)
        assertTrue(obd.connectionState.value is ConnectionState.Disconnected)
        ins.runOnMainSync{activity.finish()}
    }
}
