package com.sarani.carobd.ui.screens.owned

import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.After
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OwnedVehiclesViewModelTest {
    @After fun tearDown() = Dispatchers.resetMain()

    private fun runWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler)); block()
    }

    @Test fun localizedOptionalValuesSaveAndEditWithoutDefaults() = runWithMain {
            val repo = FakeOwnedVehicles()
            val vm = OwnedVehiclesViewModel(repo)
            vm.add(); vm.update(OwnedVehicleDraft(displayName = "My car", year = "۲۰۲۰", odometer = "١۲3۴", tankCapacity = "۵۰٫۵")); vm.save()
            advanceUntilIdle()
            val saved = repo.vehicles.value.single()
            assertEquals(2020, saved.year); assertEquals(1234L, saved.currentOdometerKm); assertEquals(50.5, saved.tankCapacityLiters!!, 0.0)
            assertNull(saved.engine); assertNull(saved.model)
            vm.edit(saved); vm.update(OwnedVehicleDraft.from(saved).copy(displayName = "Renamed", year = "")); vm.save(); advanceUntilIdle()
            assertEquals(saved.id, repo.vehicles.value.single().id)
            assertNull(repo.vehicles.value.single().year)
    }

    @Test fun invalidNumberDoesNotSaveOrBecomeZero() = runWithMain {
            val repo = FakeOwnedVehicles(); val vm = OwnedVehiclesViewModel(repo)
            vm.add(); vm.update(OwnedVehicleDraft(displayName = "Car", odometer = "۱٬۰۰۰")); vm.save(); advanceUntilIdle()
            assertTrue(repo.vehicles.value.isEmpty())
            // Invalid localized grouping is retained in the editor and never persisted as zero.
    }

    @Test fun selectionAndDeletionAreIsolatedPerVehicle() = runWithMain {
            val a = vehicle("a"); val b = vehicle("b"); val repo = FakeOwnedVehicles(listOf(a, b)); val vm = OwnedVehiclesViewModel(repo)
            advanceUntilIdle()
            vm.select("a"); advanceUntilIdle(); assertEquals("a", repo.activeVehicleId.value)
            vm.requestDelete(b); vm.confirmDelete(); advanceUntilIdle()
            assertEquals(listOf("a"), repo.vehicles.value.map { it.id }); assertEquals("a", repo.activeVehicleId.value)
            vm.requestDelete(a); vm.confirmDelete(); advanceUntilIdle()
            assertTrue(repo.vehicles.value.isEmpty()); assertNull(repo.activeVehicleId.value)
    }

    private fun vehicle(id: String) = OwnedVehicle(id, id, createdAtMs = 1, updatedAtMs = 1)
}

private class FakeOwnedVehicles(initial: List<OwnedVehicle> = emptyList()) : OwnedVehicleRepository {
    override val vehicles = MutableStateFlow(initial)
    override val activeVehicleId = MutableStateFlow<String?>(null)
    override suspend fun refresh() = Unit
    override suspend fun save(vehicle: OwnedVehicle): Result<Unit> { vehicles.value = vehicles.value.filterNot { it.id == vehicle.id } + vehicle; return Result.success(Unit) }
    override suspend fun select(id: String?): Boolean { if (id != null && vehicles.value.none { it.id == id }) return false; activeVehicleId.value = id; return true }
    override suspend fun delete(id: String): Boolean { val found = vehicles.value.any { it.id == id }; vehicles.value = vehicles.value.filterNot { it.id == id }; if (activeVehicleId.value == id) activeVehicleId.value = null; return found }
}
