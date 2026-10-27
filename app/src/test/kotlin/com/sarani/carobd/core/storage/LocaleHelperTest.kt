package com.sarani.carobd.core.storage

import com.sarani.carobd.core.storage.AppLanguage.ENGLISH
import com.sarani.carobd.core.storage.AppLanguage.PERSIAN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Locale

/**
 * §3 Checkpoint D — locale resolver contract.
 *
 * These are pure-JVM tests: [LocaleHelper.localeFor] and
 * [LocaleHelper.currentLocale] tag-based resolution are Context-free, so they
 * are exercised here without Robolectric. The Context-dependent
 * [LocaleHelper.wrapContext] path is architecturally covered by
 * [MainActivity.attachBaseContext] applying the persisted language before
 * super; see §3 #3 / #5 / #8.
 */
class LocaleHelperTest {

    // --- localeFor: authoritative tag -> Locale mapping ---

    @Test
    fun `Persian maps to fa locale`() {
        val locale = LocaleHelper.localeFor(PERSIAN)
        assertEquals("fa", locale.language)
        assertNotEquals(Locale.ENGLISH, locale)
    }

    @Test
    fun `English maps to ENGLISH locale`() {
        val locale = LocaleHelper.localeFor(ENGLISH)
        assertEquals(Locale.ENGLISH, locale)
        assertEquals("en", locale.language)
    }

    // --- currentLocale(String?): tag -> AppLanguage ---

    @Test
    fun `fa tag resolves to Persian`() {
        assertEquals(PERSIAN, LocaleHelper.currentLocale("fa"))
    }

    @Test
    fun `en tag resolves to English`() {
        assertEquals(ENGLISH, LocaleHelper.currentLocale("en"))
    }

    @Test
    fun `unknown tag falls back to English`() {
        assertEquals(ENGLISH, LocaleHelper.currentLocale("de"))
        assertEquals(ENGLISH, LocaleHelper.currentLocale("ar"))
    }

    @Test
    fun `null tag falls back to English`() {
        assertEquals(ENGLISH, LocaleHelper.currentLocale(null))
    }

    // --- round-trip: resolver <-> localeFor are mutually consistent ---

    @Test
    fun `Persian round-trips through its language tag`() {
        assertEquals(PERSIAN, LocaleHelper.currentLocale(LocaleHelper.localeFor(PERSIAN).language))
    }

    @Test
    fun `English round-trips through its language tag`() {
        assertEquals(ENGLISH, LocaleHelper.currentLocale(LocaleHelper.localeFor(ENGLISH).language))
    }

    // --- RTL / LTR language separation ---

    @Test
    fun `Persian and English locales are distinct`() {
        assertNotEquals(LocaleHelper.localeFor(PERSIAN), LocaleHelper.localeFor(ENGLISH))
    }

    // fa is a right-to-left script; en is left-to-right. The locale mapping
    // must never collapse them (§3 #7: Persian RTL, English LTR).
    @Test
    fun `Persian tag is fa and English tag is en`() {
        assertEquals("fa", LocaleHelper.localeFor(PERSIAN).language)
        assertEquals("en", LocaleHelper.localeFor(ENGLISH).language)
        assertNotEquals(
            LocaleHelper.localeFor(PERSIAN).language,
            LocaleHelper.localeFor(ENGLISH).language
        )
    }
}
