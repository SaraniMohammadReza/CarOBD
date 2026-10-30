package com.sarani.carobd.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sarani.carobd.core.model.DashboardWidgetConfig
import com.sarani.carobd.core.model.TelemetryPollMode
import com.sarani.carobd.core.model.WidgetSize
import com.sarani.carobd.core.model.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "car_obd_prefs")

enum class AppThemeMode(val titleFa: String) {
    AMOLED_BLACK("مشکی خالص آمولد (AMOLED Pure Black)"),
    NEON_CYBERPUNK("نئون تیره (Cyberpunk Neon)"),
    RACING_RED("قرمز مسابقه‌ای (Racing Red)"),
    LIGHT_THEME("روشن روز (Daytime High Contrast)")
}

enum class UnitSystem(val titleFa: String) {
    METRIC("سیستم متری (کیلومتر/ساعت، سانتی‌گراد، بار)"),
    IMPERIAL("سیستم ایمپریال (مایل/ساعت، فارنهایت، PSI)")
}

/**
 * Persisted application language. The tag is the canonical source for locale
 * switching; [LocaleHelper] applies it at runtime. Restart-safe via DataStore (§Phase C).
 */
enum class AppLanguage(val tag: String, val titleFa: String) {
    PERSIAN("fa", "فارسی"),
    ENGLISH("en", "English")
}

/** Optional user metadata. Absence never selects a vehicle or supplies a fuel assumption. */
data class UserVehicleSetup(
    val make: String = "",
    val model: String = "",
    val fuelType: String = "",
    val engineDisplacementLiters: Float? = null,
    val tankCapacityLiters: Float? = null,
    val odometerKm: Int? = null,
    val fuelPriceTomans: Float? = null,
    val isMotorcycle: Boolean = false
)

class AppPreferencesManager(private val dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>) {
    constructor(context: Context) : this(context.dataStore)


    companion object {
        private val KEY_DASHBOARD_LAYOUT = stringPreferencesKey("dashboard_layout_v1")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_UNIT_SYSTEM = stringPreferencesKey("unit_system")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_VEHICLE_MAKE = stringPreferencesKey("vehicle_make")
        private val KEY_VEHICLE_MODEL = stringPreferencesKey("vehicle_model")
        private val KEY_FUEL_TYPE = stringPreferencesKey("fuel_type")
        private val KEY_DISPLACEMENT = floatPreferencesKey("engine_disp")
        private val KEY_TANK_CAPACITY = floatPreferencesKey("tank_capacity")
        private val KEY_ODOMETER = intPreferencesKey("odometer_km")
        private val KEY_FUEL_PRICE = floatPreferencesKey("fuel_price")
        private val KEY_IS_MOTORCYCLE = booleanPreferencesKey("is_motorcycle")
        private val KEY_ACTIVE_VEHICLE_ID = stringPreferencesKey("active_vehicle_id")
        private val KEY_SERVICE_LAST_CHANGED = stringSetPreferencesKey("service_last_changed")
        private val KEY_TELEMETRY_POLL_MODE = stringPreferencesKey("telemetry_poll_mode")
    }

    val isOnboardingDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val themeMode: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME_MODE] ?: AppThemeMode.AMOLED_BLACK.name
        try { AppThemeMode.valueOf(name) } catch (_: Exception) { AppThemeMode.AMOLED_BLACK }
    }

    val unitSystem: Flow<UnitSystem> = dataStore.data.map { prefs ->
        val name = prefs[KEY_UNIT_SYSTEM] ?: UnitSystem.METRIC.name
        try { UnitSystem.valueOf(name) } catch (_: Exception) { UnitSystem.METRIC }
    }

    val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        val name = prefs[KEY_LANGUAGE] ?: AppLanguage.PERSIAN.name
        try { AppLanguage.valueOf(name) } catch (_: Exception) { AppLanguage.PERSIAN }
    }

    val userVehicleSetup: Flow<UserVehicleSetup> = dataStore.data.map { prefs ->
        UserVehicleSetup(
            make = prefs[KEY_VEHICLE_MAKE] ?: "",
            model = prefs[KEY_VEHICLE_MODEL] ?: "",
            fuelType = prefs[KEY_FUEL_TYPE] ?: "",
            engineDisplacementLiters = prefs[KEY_DISPLACEMENT],
            tankCapacityLiters = prefs[KEY_TANK_CAPACITY],
            odometerKm = prefs[KEY_ODOMETER],
            fuelPriceTomans = prefs[KEY_FUEL_PRICE],
            isMotorcycle = prefs[KEY_IS_MOTORCYCLE] ?: false
        )
    }

    /** PERSISTED active vehicle id (§B3 single source of truth). Null when none selected. */
    val activeVehicleId: Flow<String?> = dataStore.data.map { prefs ->
        val id = prefs[KEY_ACTIVE_VEHICLE_ID]
        id?.takeIf { it.isNotBlank() }
    }

    /**
     * §7: persisted telemetry polling cadence (ECO/NORMAL/FAST). The producer
     * scheduler reads this each cycle; the placebo "refresh rate" UI is gone.
     * Unknown/corrupt stored values degrade safely to [TelemetryPollMode.NORMAL]
     * via [TelemetryPollMode.fromStored] (never crashes, never spams).
     */
    val telemetryPollMode: Flow<TelemetryPollMode> = dataStore.data.map { prefs ->
        TelemetryPollMode.fromStored(prefs[KEY_TELEMETRY_POLL_MODE])
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        dataStore.edit { it[KEY_UNIT_SYSTEM] = system.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    /** §7: persist the selected telemetry polling cadence. */
    suspend fun setTelemetryPollMode(mode: TelemetryPollMode) {
        dataStore.edit { it[KEY_TELEMETRY_POLL_MODE] = mode.name }
    }

    suspend fun saveVehicleSetup(setup: UserVehicleSetup) {
        dataStore.edit {
            it[KEY_VEHICLE_MAKE] = setup.make
            it[KEY_VEHICLE_MODEL] = setup.model
            it[KEY_FUEL_TYPE] = setup.fuelType
            setup.engineDisplacementLiters?.takeIf { it.isFinite() && it > 0 }?.let { value -> it[KEY_DISPLACEMENT] = value } ?: it.remove(KEY_DISPLACEMENT)
            setup.tankCapacityLiters?.takeIf { it.isFinite() && it > 0 }?.let { value -> it[KEY_TANK_CAPACITY] = value } ?: it.remove(KEY_TANK_CAPACITY)
            setup.odometerKm?.takeIf { it >= 0 }?.let { value -> it[KEY_ODOMETER] = value } ?: it.remove(KEY_ODOMETER)
            setup.fuelPriceTomans?.takeIf { it.isFinite() && it >= 0 }?.let { value -> it[KEY_FUEL_PRICE] = value } ?: it.remove(KEY_FUEL_PRICE)
            it[KEY_IS_MOTORCYCLE] = setup.isMotorcycle
            it[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun setActiveVehicleId(id: String) {
        dataStore.edit { it[KEY_ACTIVE_VEHICLE_ID] = id }
    }

    suspend fun clearActiveVehicleId() {
        dataStore.edit { it.remove(KEY_ACTIVE_VEHICLE_ID) }
    }

    /** Per-service last-changed odometer (§Phase C maintenance persistence). */
    val serviceLastChangedKm: Flow<Map<String, Int>> = dataStore.data.map { prefs ->
        prefs[KEY_SERVICE_LAST_CHANGED]?.associate { entry ->
            val k = entry.substringBefore(":")
            val v = entry.substringAfter(":").toIntOrNull() ?: 0
            k to v
        } ?: emptyMap()
    }

    suspend fun setServiceLastChangedKm(id: String, km: Int) {
        dataStore.edit { prefs ->
            val current = (prefs[KEY_SERVICE_LAST_CHANGED] ?: emptySet()).toMutableSet()
            current.removeAll { it.substringBefore(':') == id }
            current.add("$id:$km")
            prefs[KEY_SERVICE_LAST_CHANGED] = current
        }
    }

    suspend fun setOdometerKm(km: Int) {
        dataStore.edit { it[KEY_ODOMETER] = km }
    }

    val dashboardWidgets: Flow<List<DashboardWidgetConfig>> = dataStore.data.map { prefs ->
        prefs[KEY_DASHBOARD_LAYOUT]?.let(DashboardLayoutCodec::decode) ?: getDefaultWidgets()
    }

    suspend fun saveDashboardWidgets(widgets: List<DashboardWidgetConfig>) {
        val encoded = DashboardLayoutCodec.encode(widgets)
        dataStore.edit { it[KEY_DASHBOARD_LAYOUT] = encoded }
    }

    // A layout preference never establishes ECU support; unknown cards still display —.
    fun getDefaultWidgets(): List<DashboardWidgetConfig> = listOf(
        DashboardWidgetConfig("w_throttle", "throttle_pos", WidgetType.DIGITAL_CARD, WidgetSize.MEDIUM),
        DashboardWidgetConfig("w_maf", "maf", WidgetType.DIGITAL_CARD, WidgetSize.MEDIUM)
    )
}
