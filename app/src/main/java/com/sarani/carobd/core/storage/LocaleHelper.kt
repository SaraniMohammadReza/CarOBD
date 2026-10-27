package com.sarani.carobd.core.storage

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Runtime locale application. The authoritative language is persisted by
 * [AppPreferencesManager]; this helper applies the persisted tag to a Context's
 * Configuration BEFORE resources are inflated (§Phase C / §3: one authoritative
 * state, await-persist-before-recreate, locale available before Compose,
 * RTL/LTR, restart-safe).
 *
 * Uses the non-deprecated [Context.createConfigurationContext] API (replaces the
 * deprecated [android.content.res.Resources.updateConfiguration] path).
 */
object LocaleHelper {

    fun localeFor(lang: AppLanguage): Locale =
        if (lang == AppLanguage.PERSIAN) Locale.forLanguageTag("fa") else Locale.ENGLISH

    /**
     * Derive the active language from an already-resolved locale language tag.
     * Tag form (e.g. "fa" / "en") so this is testable on the JVM without a Context.
     */
    fun currentLocale(languageTag: String?): AppLanguage =
        if (languageTag == "fa") AppLanguage.PERSIAN else AppLanguage.ENGLISH

    /** Convenience for callers that have a Context. */
    fun currentLocale(context: Context): AppLanguage =
        currentLocale(context.resources.configuration.locales[0]?.language)

    /**
     * Wrap [context] so its Configuration carries [lang], applied via the
     * non-deprecated [Context.createConfigurationContext] API BEFORE any
     * resources are inflated. Call from
     * [android.app.Activity.attachBaseContext] so the locale is in place for
     * first composition and survives cold launch, process restart, and
     * Activity recreation.
     */
    fun wrapContext(context: Context, lang: AppLanguage): Context {
        val locale = localeFor(lang)
        Locale.setDefault(locale)
        val cfg = Configuration(context.resources.configuration)
        cfg.setLocale(locale)
        cfg.setLayoutDirection(locale)
        return context.createConfigurationContext(cfg)
    }
}
