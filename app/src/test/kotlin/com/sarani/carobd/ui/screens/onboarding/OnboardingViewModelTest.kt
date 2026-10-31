package com.sarani.carobd.ui.screens.onboarding

import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.ui.screens.owned.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    @After fun tearDown() = Dispatchers.resetMain()
    private class Owners : OwnedVehicleRepository {
        override val vehicles=MutableStateFlow<List<OwnedVehicle>>(emptyList())
        override val activeVehicleId=MutableStateFlow<String?>(null)
        var reject=false
        override suspend fun refresh() {}
        override suspend fun save(vehicle:OwnedVehicle):Result<Unit> {
            if(reject) return Result.failure(IllegalStateException("injected"))
            vehicles.value=listOf(vehicle);return Result.success(Unit)
        }
        override suspend fun select(id:String?):Boolean { activeVehicleId.value=id;return true }
        override suspend fun delete(id:String)=false
    }
    @Test fun skipDoesNotInventOrSelectVehicle()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler));val owners=Owners();var done=false
        OnboardingViewModel(owners){done=true}.complete(null);advanceUntilIdle()
        assertTrue(done);assertTrue(owners.vehicles.value.isEmpty());assertNull(owners.activeVehicleId.value)
    }
    @Test fun createsCanonicalOwnerWithUnknownFactsPreserved()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler));val owners=Owners();var done=false
        OnboardingViewModel(owners){done=true}.complete(OwnedVehicleDraft(id="owned",displayName="My car"));advanceUntilIdle()
        val v=owners.vehicles.value.single();assertTrue(done);assertEquals("owned",owners.activeVehicleId.value)
        assertNull(v.manufacturer);assertNull(v.engineCode);assertNull(v.currentOdometerKm);assertNull(v.tankCapacityLiters);assertNull(v.fuelType)
    }
    @Test fun localizedZeroIsRealButGroupedOrOverflowMileageCannotBeSaved()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler));val owners=Owners();var count=0
        val vm=OnboardingViewModel(owners){count++}
        for(value in listOf("۱٬۰۰۰","-1","1e3","999999999999999999999")) {
            vm.complete(OwnedVehicleDraft(displayName="My car",odometer=value));advanceUntilIdle()
            assertEquals(0,count);assertTrue(owners.vehicles.value.isEmpty())
        }
        vm.complete(OwnedVehicleDraft(displayName="My car",odometer="۰"));advanceUntilIdle()
        assertEquals(0L,owners.vehicles.value.single().currentOdometerKm);assertEquals(1,count)
    }
    @Test fun failedPersistenceDoesNotCompleteSetup()=runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler));val owners=Owners().apply{reject=true};var done=false
        val vm=OnboardingViewModel(owners){done=true};vm.complete(OwnedVehicleDraft(displayName="My car"));advanceUntilIdle()
        assertFalse(done);assertEquals(OwnedVehicleEditorError.SAVE_FAILED,vm.error.value)
    }
}
