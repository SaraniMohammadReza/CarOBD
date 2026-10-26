package com.sarani.carobd.core.storage

import android.app.Activity
import android.content.Context
import java.util.Locale

/**
 * Runtime locale application. The authoritative language is persisted by
 * [AppPreferencesManager]; this helper applies the persisted tag to the running
 * process and recreates the foreground Activity so RTL/LTR and string resources
 * refresh app-wide (§Phase C — language must apply app-wide, restart-safe).
 */
object LocaleHelper {

    fun localeFor(lang: AppLanguage): Locale =
        if (lang == AppLanguage.PERSIAN) Locale("fa") else Locale.ENGLISH

    /** Best-effort current language read from the active configuration. */
    fun currentLocale(context: Context): AppLanguage {
        val tag = context.resources.configuration.locales[0]?.language
        return if (tag == "fa") AppLanguage.PERSIAN else AppLanguage.ENGLISH
    }

    /** Apply locale to the active process. Does NOT recreate; caller decides refresh. */
    fun apply(activity: Activity, lang: AppLanguage) {
        val locale = localeFor(lang)
        Locale.setDefault(locale)
        val cfg = activity.resources.configuration
        cfg.setLocale(locale)
        cfg.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(cfg, activity.resources.displayMetrics)
    }

    /** Apply locale then recreate so Compose re-reads resources with the new locale. */
    fun applyAndRecreate(activity: Activity, lang: AppLanguage) {
        apply(activity, lang)
        activity.recreate()
    }
}
