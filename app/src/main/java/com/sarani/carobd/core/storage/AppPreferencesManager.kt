package com.sarani.carobd.core.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sarani.carobd.core.model.DashboardWidgetConfig
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

data class UserVehicleSetup(
    val make: String = "ایران خودرو",
    val model: String = "پژو ۲۰۶ تیپ ۵",
    val fuelType: String = "بنزینی",
    val engineDisplacementLiters: Float = 1.6f,
    val tankCapacityLiters: Float = 50f,
    val odometerKm: Int = 120000,
    val fuelPriceTomans: Float = 3000f,
    val isMotorcycle: Boolean = false
)

class AppPreferencesManager(private val context: Context) {

    companion object {
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
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    val themeMode: Flow<AppThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME_MODE] ?: AppThemeMode.AMOLED_BLACK.name
        try { AppThemeMode.valueOf(name) } catch (_: Exception) { AppThemeMode.AMOLED_BLACK }
    }

    val unitSystem: Flow<UnitSystem> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_UNIT_SYSTEM] ?: UnitSystem.METRIC.name
        try { UnitSystem.valueOf(name) } catch (_: Exception) { UnitSystem.METRIC }
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_LANGUAGE] ?: AppLanguage.PERSIAN.name
        try { AppLanguage.valueOf(name) } catch (_: Exception) { AppLanguage.PERSIAN }
    }

    val userVehicleSetup: Flow<UserVehicleSetup> = context.dataStore.data.map { prefs ->
        UserVehicleSetup(
            make = prefs[KEY_VEHICLE_MAKE] ?: "ایران خودرو",
            model = prefs[KEY_VEHICLE_MODEL] ?: "پژو ۲۰۶ تیپ ۵ (TU5)",
            fuelType = prefs[KEY_FUEL_TYPE] ?: "بنزینی",
            engineDisplacementLiters = prefs[KEY_DISPLACEMENT] ?: 1.6f,
            tankCapacityLiters = prefs[KEY_TANK_CAPACITY] ?: 50f,
            odometerKm = prefs[KEY_ODOMETER] ?: 120000,
            fuelPriceTomans = prefs[KEY_FUEL_PRICE] ?: 3000f,
            isMotorcycle = prefs[KEY_IS_MOTORCYCLE] ?: false
        )
    }

    /** PERSISTED active vehicle id (§B3 single source of truth). Null when none selected. */
    val activeVehicleId: Flow<String?> = context.dataStore.data.map { prefs ->
        val id = prefs[KEY_ACTIVE_VEHICLE_ID]
        id?.takeIf { it.isNotBlank() }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.dataStore.edit { it[KEY_UNIT_SYSTEM] = system.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.name }
    }

    suspend fun saveVehicleSetup(setup: UserVehicleSetup) {
        context.dataStore.edit {
            it[KEY_VEHICLE_MAKE] = setup.make
            it[KEY_VEHICLE_MODEL] = setup.model
            it[KEY_FUEL_TYPE] = setup.fuelType
            it[KEY_DISPLACEMENT] = setup.engineDisplacementLiters
            it[KEY_TANK_CAPACITY] = setup.tankCapacityLiters
            it[KEY_ODOMETER] = setup.odometerKm
            it[KEY_FUEL_PRICE] = setup.fuelPriceTomans
            it[KEY_IS_MOTORCYCLE] = setup.isMotorcycle
            it[KEY_ONBOARDING_DONE] = true
        }
    }

    suspend fun setActiveVehicleId(id: String) {
        context.dataStore.edit { it[KEY_ACTIVE_VEHICLE_ID] = id }
    }

    suspend fun clearActiveVehicleId() {
        context.dataStore.edit { it.remove(KEY_ACTIVE_VEHICLE_ID) }
    }

    // Default widget layout
    fun getDefaultWidgets(): List<DashboardWidgetConfig> = listOf(
        DashboardWidgetConfig("w_rpm", "rpm", WidgetType.CIRCULAR_GAUGE, WidgetSize.MEDIUM),
        DashboardWidgetConfig("w_speed", "speed", WidgetType.CIRCULAR_GAUGE, WidgetSize.MEDIUM),
        DashboardWidgetConfig("w_coolant", "coolant_temp", WidgetType.DIGITAL_CARD, WidgetSize.SMALL),
        DashboardWidgetConfig("w_throttle", "throttle_pos", WidgetType.DIGITAL_CARD, WidgetSize.SMALL),
        DashboardWidgetConfig("w_voltage", "module_voltage", WidgetType.DIGITAL_CARD, WidgetSize.SMALL),
        DashboardWidgetConfig("w_fuel", "fuel_level", WidgetType.BAR_GAUGE, WidgetSize.SMALL),
        DashboardWidgetConfig("w_o2", "o2_b1s1", WidgetType.OSCILLOSCOPE_GRAPH, WidgetSize.LARGE),
        DashboardWidgetConfig("w_stepper", "stepper_steps", WidgetType.DIGITAL_CARD, WidgetSize.SMALL)
    )
}
