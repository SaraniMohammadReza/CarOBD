package com.sarani.carobd.core.adapter.profile

import com.sarani.carobd.core.model.ConnectionProfile
import com.sarani.carobd.core.model.TransportConfig
import com.sarani.carobd.core.model.TransportType
import kotlinx.coroutines.flow.Flow

/**
 * §11 P1 REAL CONNECTIONPROFILE — repository contract.
 *
 * Persisted, restart-safe connection profiles. The DataStore-backed
 * implementation lives in the app layer (it needs a DataStore Context); an
 * in-memory implementation is provided for tests + non-persistent fallback.
 *
 * Required operations (§11): create, update, select, delete, default profile,
 * selected profile, survive restart, migration/default.
 */
interface ConnectionProfileRepository {
    /** All persisted profiles, emitted on every mutation. */
    val allProfiles: Flow<List<ConnectionProfile>>

    /** The currently selected profile id (null = none selected). */
    val selectedProfileId: Flow<String?>

    /** Persists [profile] (replacing any existing profile with the same id) and
     *  returns the saved id. Returns null if [profile] is invalid. */
    suspend fun save(profile: ConnectionProfile): String?

    /** Persists [profile] and selects it as the active profile. */
    suspend fun saveAndSelect(profile: ConnectionProfile): String?

    /** Selects the profile with [id]. Returns false if it does not exist. */
    suspend fun select(id: String): Boolean

    /** Deletes the profile with [id]; clears selection if it was active. */
    suspend fun delete(id: String): Boolean

    /** The selected profile, or null. */
    suspend fun selectedProfile(): ConnectionProfile?

    /** Ensures a default profile exists (migration guard) and returns it. */
    suspend fun defaultProfile(): ConnectionProfile
}

/**
 * In-memory implementation: mirrors the DataStore contract for JVM tests and as
 * a non-persistent fallback. Not restart-safe by itself — the DataStore impl is.
 */
class InMemoryConnectionProfileRepository(
    private val defaultProfileSupplier: suspend () -> ConnectionProfile = { ConnectionProfile.newProfile("Default", TransportConfig.Simulator("healthy")) },
) : ConnectionProfileRepository {

    private val store = LinkedHashMap<String, ConnectionProfile>()
    private var selected: String? = null
    private val _all = kotlinx.coroutines.flow.MutableStateFlow<List<ConnectionProfile>>(emptyList())
    private val _sel = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override val allProfiles: Flow<List<ConnectionProfile>> get() = _all
    override val selectedProfileId: Flow<String?> get() = _sel

    private fun emit() {
        _all.value = store.values.toList()
        _sel.value = selected
    }

    override suspend fun save(profile: ConnectionProfile): String? {
        if (profile.validate().isNotEmpty()) return null
        store[profile.id] = profile
        emit()
        return profile.id
    }

    override suspend fun saveAndSelect(profile: ConnectionProfile): String? {
        if (profile.validate().isNotEmpty()) return null
        store[profile.id] = profile
        selected = profile.id
        emit()
        return profile.id
    }

    override suspend fun select(id: String): Boolean {
        if (!store.containsKey(id)) return false
        selected = id
        emit()
        return true
    }

    override suspend fun delete(id: String): Boolean {
        val removed = store.remove(id) != null
        if (removed && selected == id) selected = null
        if (removed) emit()
        return removed
    }

    override suspend fun selectedProfile(): ConnectionProfile? = store[selected]

    override suspend fun defaultProfile(): ConnectionProfile {
        // Migration guard: ensure a usable simulator default exists.
        val existing = store.values.firstOrNull { it.transport.type == com.sarani.carobd.core.model.TransportType.SIMULATOR }
        if (existing != null) { selected = existing.id; emit(); return existing }
        val d = defaultProfileSupplier()
        store[d.id] = d
        selected = d.id
        emit()
        return d
    }
}
