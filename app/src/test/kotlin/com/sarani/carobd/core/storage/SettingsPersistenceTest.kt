package com.sarani.carobd.core.storage

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class SettingsPersistenceTest {
    @Test fun languageUnitsAndThemeSurviveStoreRecreation() = runBlocking {
        val dir = Files.createTempDirectory("settings-recreation").toFile()
        val file = dir.resolve("settings.preferences_pb")
        val first = SupervisorJob()
        try {
            val p = AppPreferencesManager(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + first), produceFile = { file }))
            p.setLanguage(AppLanguage.ENGLISH); p.setUnitSystem(UnitSystem.IMPERIAL); p.setThemeMode(AppThemeMode.RACING_RED)
        } finally { first.cancelAndJoin() }
        val second = SupervisorJob()
        try {
            val p = AppPreferencesManager(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + second), produceFile = { file }))
            assertEquals(AppLanguage.ENGLISH, p.language.first())
            assertEquals(UnitSystem.IMPERIAL, p.unitSystem.first())
            assertEquals(AppThemeMode.RACING_RED, p.themeMode.first())
            assertEquals("en", LocaleHelper.localeFor(p.language.first()).language)
        } finally { second.cancelAndJoin(); dir.deleteRecursively() }
    }
}
