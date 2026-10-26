package com.sarani.carobd

import com.sarani.carobd.core.protocol.iranian.IranianEcuRegistry
import com.sarani.carobd.data.repository.ActiveVehicleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ActiveVehicleRepositoryTest {

    private val preset = IranianEcuRegistry.PRESET_VEHICLES
    // Resolve a real catalogue entry; fall back to the first if "Dena" id is absent.
    private val selected = preset.find { it.id.contains("dena", ignoreCase = true) } ?: preset.first()

    // B4 #9: no vehicle selected must NOT silently become PRESET_VEHICLES.first() (Peugeot 206 TU5).
    @Test
    fun noVehicleSelected_emitsNullNotPeugeotDefault() = runBlocking {
        val store = MutableStateFlow<String?>(null)
        val repo = ActiveVehicleRepository(store, { }, { store.value = null })
        val job = launch { repo.activeVehicle.collect { } } // activate stateIn (WhileSubscribed)
        delay(50)
        assertNull(
            "Unselected vehicle must be null, never PRESET_VEHICLES.first()",
            repo.activeVehicle.value
        )
        job.cancel()
    }

    // B4 #9 (negative): an unknown id resolves to null, not the 206 default.
    @Test
    fun unknownVehicleId_resolvesToNull() = runBlocking {
        val store = MutableStateFlow<String?>("no_such_vehicle")
        val repo = ActiveVehicleRepository(store, { }, { store.value = null })
        val job = launch { repo.activeVehicle.collect { } }
        delay(50)
        assertNull(repo.activeVehicle.value)
        job.cancel()
    }

    // B4 #6: selecting a vehicle updates the repository flow immediately and persists the id.
    @Test
    fun selectVehicle_updatesFlowAndPersistsImmediately() = runBlocking {
        val store = MutableStateFlow<String?>(null)
        val persisted = mutableListOf<String>()
        val repo = ActiveVehicleRepository(store, { persisted += it; store.value = it }, { store.value = null })
        val job = launch { repo.activeVehicle.collect { } } // keep stateIn (WhileSubscribed) sharing active
        delay(20)

        assertNull(repo.activeVehicle.value)
        repo.selectVehicle(selected)

        // Poll the shared StateFlow until the selection propagates (robust against dispatch lag).
        var ticks = 0
        while (ticks < 200 && repo.activeVehicle.value?.id != selected.id) { delay(10); ticks++ }
        val after = repo.activeVehicle.value
        assertNotNull("selectVehicle must propagate to the active vehicle flow (B4 #6)", after)
        assertEquals(selected.id, after!!.id)
        assertTrue("selectVehicle must persist the id via the callback (B4 #6)", persisted.contains(selected.id))
        job.cancel()
    }

    // B4 #8: selection survives repository recreation (persisted id is restored).
    @Test
    fun selectionSurvivesRepositoryRecreation() = runBlocking {
        val store = MutableStateFlow<String?>(null)
        val repo = ActiveVehicleRepository(store, { store.value = it }, { store.value = null })
        val job = launch { repo.activeVehicle.collect { } }
        delay(20)
        repo.selectVehicle(selected)
        var ticks = 0
        while (ticks < 200 && repo.activeVehicle.value?.id != selected.id) { delay(10); ticks++ }
        assertEquals(selected.id, repo.activeVehicle.value?.id) // ensure persisted
        job.cancel()

        // Recreate the repository over the same persisted id store.
        val store2 = MutableStateFlow(store.value)
        val repo2 = ActiveVehicleRepository(store2, { store2.value = it }, { store2.value = null })
        val job2 = launch { repo2.activeVehicle.collect { } }
        delay(20)
        var ticks2 = 0
        while (ticks2 < 200 && repo2.activeVehicle.value?.id != selected.id) { delay(10); ticks2++ }
        assertEquals(selected.id, repo2.activeVehicle.value?.id)
        job2.cancel()
    }
}
