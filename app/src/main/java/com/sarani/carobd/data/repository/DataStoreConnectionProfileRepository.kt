package com.sarani.carobd.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sarani.carobd.core.adapter.profile.ConnectionProfileJson
import com.sarani.carobd.core.adapter.profile.ConnectionProfileRepository
import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.ProtocolMode
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

private val Context.connectionProfilesDataStore by preferencesDataStore(name = "connection_profiles")

/**
 * §11 P1 REAL CONNECTIONPROFILE — DataStore-backed implementation.
 *
 * Persists the full set of [ConnectionProfile]s plus the selected id so selection
 * survives restart. Profiles are serialized to JSON strings via
 * [ConnectionProfileJson] (stable identifiers only; no Android framework objects
 * — §11 rule). Writes are mutex-guarded to avoid read-modify-write clobbering.
 */
class DataStoreConnectionProfileRepository(
    private val context: Context,
) : ConnectionProfileRepository {

    companion object {
        private val KEY_PROFILES = stringSetPreferencesKey("profiles_json")
        private val KEY_SELECTED = stringPreferencesKey("selected_id")
    }

    private val writeLock = Mutex()

    override val allProfiles: Flow<List<ConnectionProfile>> =
        context.connectionProfilesDataStore.data.map { prefs ->
            (prefs[KEY_PROFILES] ?: emptySet()).mapNotNull { raw ->
                runCatching { ConnectionProfileJson.decode(raw) }.getOrNull()
            }
        }

    override val selectedProfileId: Flow<String?> =
        context.connectionProfilesDataStore.data.map { prefs ->
            val id = prefs[KEY_SELECTED]; if (id.isNullOrEmpty()) null else id
        }

    override suspend fun save(profile: ConnectionProfile): String? {
        if (profile.validate().isNotEmpty()) return null
        writeLock.withLock {
            context.connectionProfilesDataStore.edit { prefs ->
                val set = (prefs[KEY_PROFILES] ?: emptySet()).toMutableSet()
                set.removeAll { profileMatchesId(it, profile.id) }
                set.add(ConnectionProfileJson.encode(profile))
                prefs[KEY_PROFILES] = set
            }
        }
        return profile.id
    }

    override suspend fun saveAndSelect(profile: ConnectionProfile): String? {
        if (profile.validate().isNotEmpty()) return null
        writeLock.withLock {
            context.connectionProfilesDataStore.edit { prefs ->
                val set = (prefs[KEY_PROFILES] ?: emptySet()).toMutableSet()
                set.removeAll { profileMatchesId(it, profile.id) }
                set.add(ConnectionProfileJson.encode(profile))
                prefs[KEY_PROFILES] = set
                prefs[KEY_SELECTED] = profile.id
            }
        }
        return profile.id
    }

    override suspend fun select(id: String): Boolean {
        var ok = false
        writeLock.withLock {
            context.connectionProfilesDataStore.edit { prefs ->
                val exists = (prefs[KEY_PROFILES] ?: emptySet()).any { profileMatchesId(it, id) }
                if (exists) { prefs[KEY_SELECTED] = id; ok = true }
            }
        }
        return ok
    }

    override suspend fun delete(id: String): Boolean {
        var removed = false
        writeLock.withLock {
            context.connectionProfilesDataStore.edit { prefs ->
                val set = (prefs[KEY_PROFILES] ?: emptySet()).toMutableSet()
                removed = set.removeAll { profileMatchesId(it, id) }
                prefs[KEY_PROFILES] = set
                if (removed && prefs[KEY_SELECTED] == id) prefs[KEY_SELECTED] = ""
            }
        }
        return removed
    }

    override suspend fun selectedProfile(): ConnectionProfile? {
        val id = selectedProfileId.first() ?: return null
        return allProfiles.first().firstOrNull { it.id == id }
    }

    override suspend fun defaultProfile(): ConnectionProfile {
        // Migration guard: ensure at least a simulator default exists and is selected.
        val existing = allProfiles.first().firstOrNull { it.transport.type == TransportType.SIMULATOR }
        if (existing != null) { select(existing.id); return existing }
        val d = ConnectionProfile(
            id = UUID.randomUUID().toString(),
            name = "Default Simulator",
            transport = TransportConfig.Simulator("healthy"),
            protocolMode = ProtocolMode.AUTO,
        )
        saveAndSelect(d)
        return d
    }

    private fun profileMatchesId(json: String, id: String): Boolean =
        runCatching { ConnectionProfileJson.decode(json).id == id }.getOrDefault(false)
}
