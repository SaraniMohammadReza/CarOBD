package com.sarani.carobd.ui.screens.parts

import com.sarani.carobd.core.model.ConfidenceLevel
import com.sarani.carobd.core.model.OwnedVehicle
import com.sarani.carobd.core.model.PartCategory
import com.sarani.carobd.core.vehicle.catalog.DefaultVehicleCatalogRepository
import com.sarani.carobd.core.vehicle.parts.DefaultPartsRepository
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PartsCenterViewModelTest {

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun runWithMain(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        block()
    }

    private fun testVehicle(
        id: String = "v1",
        displayName: String = "My Tondar 90",
        make: String = "Renault",
        model: String = "Tondar 90",
        generation: String = "Tondar 90",
        year: Int = 2012,
        engineCode: String = "K4M"
    ) = OwnedVehicle(
        id = id,
        displayName = displayName,
        manufacturer = make,
        model = model,
        generation = generation,
        year = year,
        engine = "1.6 16V",
        engineCode = engineCode,
        transmission = "Manual 5-speed",
        vin = null,
        licensePlate = "12-345-67",
        currentOdometerKm = 120_000L,
        tankCapacityLiters = 50.0,
        fuelType = "Petrol",
        notes = null,
        createdAtMs = 1000L,
        updatedAtMs = 1000L
    )

    private class FakeOwnedVehicles(
        initialList: List<OwnedVehicle> = emptyList(),
        activeId: String? = null
    ) : OwnedVehicleRepository {
        private val _vehicles = MutableStateFlow(initialList)
        override val vehicles: StateFlow<List<OwnedVehicle>> = _vehicles.asStateFlow()

        private val _activeVehicleId = MutableStateFlow(activeId)
        override val activeVehicleId: StateFlow<String?> = _activeVehicleId.asStateFlow()

        override suspend fun refresh() {}
        override suspend fun select(id: String?): Boolean {
            _activeVehicleId.value = id
            return true
        }
        override suspend fun save(vehicle: OwnedVehicle): Result<Unit> {
            _vehicles.value = _vehicles.value.filterNot { it.id == vehicle.id } + vehicle
            return Result.success(Unit)
        }
        override suspend fun delete(id: String): Boolean {
            val existed = _vehicles.value.any { it.id == id }
            _vehicles.value = _vehicles.value.filterNot { it.id == id }
            return existed
        }
    }

    @Test
    fun initialStateLoadsCategoriesAndCompatiblePartsForActiveVehicle() = runWithMain {
        val vehicle = testVehicle()
        val ownedRepo = FakeOwnedVehicles(listOf(vehicle), activeId = "v1")
        val partsRepo = DefaultPartsRepository()
        val catalogRepo = DefaultVehicleCatalogRepository()

        val viewModel = PartsCenterViewModel(
            ownedVehicleRepository = ownedRepo,
            partsRepository = partsRepo,
            catalogRepository = catalogRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(vehicle, state.activeVehicle)
        assertFalse(state.categories.isEmpty())
        assertFalse(state.compatibleParts.isEmpty())

        // Verified OEM Tondar 90 parts
        val sparkPlug = state.compatibleParts.firstOrNull { it.definition.category == PartCategory.SPARK_PLUG }
        assertNotNull(sparkPlug)
        assertEquals(ConfidenceLevel.VERIFIED_OEM, sparkPlug!!.confidence)
        assertTrue(sparkPlug.fitment.oemNumbers.any { it.partNumber == "77 00 500 168" })
    }

    @Test
    fun categoryFilterFiltersCompatibleParts() = runWithMain {
        val vehicle = testVehicle()
        val ownedRepo = FakeOwnedVehicles(listOf(vehicle), activeId = "v1")
        val partsRepo = DefaultPartsRepository()
        val catalogRepo = DefaultVehicleCatalogRepository()

        val viewModel = PartsCenterViewModel(
            ownedVehicleRepository = ownedRepo,
            partsRepository = partsRepo,
            catalogRepository = catalogRepo
        )
        advanceUntilIdle()

        // Select Timing Belt category
        viewModel.onIntent(PartsCenterIntent.SelectCategory(PartCategory.TIMING_BELT))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(PartCategory.TIMING_BELT, state.selectedCategory)
        assertTrue(state.compatibleParts.isNotEmpty())
        assertTrue(state.compatibleParts.all { it.definition.category == PartCategory.TIMING_BELT })
        assertEquals("132", state.compatibleParts.first().fitment.specifications.first { it.key == "teeth" }.value)
    }

    @Test
    fun searchAcrossOemPartNumbersAndBrands() = runWithMain {
        val vehicle = testVehicle()
        val ownedRepo = FakeOwnedVehicles(listOf(vehicle), activeId = "v1")
        val partsRepo = DefaultPartsRepository()
        val catalogRepo = DefaultVehicleCatalogRepository()

        val viewModel = PartsCenterViewModel(
            ownedVehicleRepository = ownedRepo,
            partsRepository = partsRepo,
            catalogRepository = catalogRepo
        )
        advanceUntilIdle()

        // Search by OEM number
        viewModel.onIntent(PartsCenterIntent.UpdateSearchQuery("77 00 500 168"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("77 00 500 168", state.searchQuery)
        assertTrue(state.compatibleParts.isNotEmpty())
        assertEquals("part_spark_plug", state.compatibleParts.first().definition.id)

        // Search by aftermarket brand
        viewModel.onIntent(PartsCenterIntent.UpdateSearchQuery("Purflux"))
        advanceUntilIdle()

        val brandState = viewModel.uiState.value
        assertTrue(brandState.compatibleParts.isNotEmpty())
        assertTrue(brandState.compatibleParts.any { part ->
            part.fitment.aftermarketReferences.any { it.brand.contains("Purflux", ignoreCase = true) }
        })
    }

    @Test
    fun noActiveVehicleYieldsEmptyCompatibleParts() = runWithMain {
        val ownedRepo = FakeOwnedVehicles(emptyList(), activeId = null)
        val partsRepo = DefaultPartsRepository()

        val viewModel = PartsCenterViewModel(
            ownedVehicleRepository = ownedRepo,
            partsRepository = partsRepo,
            catalogRepository = null
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.activeVehicle)
        assertTrue(state.compatibleParts.isEmpty())
    }

    @Test
    fun copyToClipboardTriggersEffectAndSetsToast() = runWithMain {
        val vehicle = testVehicle()
        val ownedRepo = FakeOwnedVehicles(listOf(vehicle), activeId = "v1")
        val partsRepo = DefaultPartsRepository()

        val viewModel = PartsCenterViewModel(
            ownedVehicleRepository = ownedRepo,
            partsRepository = partsRepo,
            catalogRepository = null
        )
        advanceUntilIdle()

        viewModel.onIntent(PartsCenterIntent.CopyToClipboard("77 00 500 168"))
        advanceUntilIdle()

        assertEquals("77 00 500 168", viewModel.uiState.value.toastMessage)

        viewModel.onIntent(PartsCenterIntent.ClearToast)
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.toastMessage)
    }

    @Test fun searchNeverEscapesSelectedEngineOrManufacturer() = runWithMain {
        val tu3 = testVehicle(make="IKCO", model="206", year=2012, engineCode="TU3")
        val vm = PartsCenterViewModel(FakeOwnedVehicles(listOf(tu3), "v1"), DefaultPartsRepository(), DefaultVehicleCatalogRepository())
        advanceUntilIdle()
        vm.onIntent(PartsCenterIntent.UpdateSearchQuery("timing"))
        assertTrue(vm.uiState.value.compatibleParts.isNotEmpty())
        assertTrue(vm.uiState.value.compatibleParts.all { it.fitment.engineCode?.contains("TU3") == true })
        val k4m = PartsCenterViewModel(FakeOwnedVehicles(listOf(testVehicle()), "v1"), DefaultPartsRepository(), DefaultVehicleCatalogRepository())
        advanceUntilIdle()
        k4m.onIntent(PartsCenterIntent.UpdateSearchQuery("5960 F3"))
        assertTrue(k4m.uiState.value.compatibleParts.isEmpty())
    }
    @Test fun unknownEngineCannotAcquireCompatibilityBySearching() = runWithMain {
        val unknown = testVehicle(make="IKCO", model="206", year=2012, engineCode="")
        val vm = PartsCenterViewModel(FakeOwnedVehicles(listOf(unknown), "v1"), DefaultPartsRepository(), DefaultVehicleCatalogRepository())
        advanceUntilIdle()
        vm.onIntent(PartsCenterIntent.UpdateSearchQuery("timing"))
        assertTrue(vm.uiState.value.compatibleParts.isEmpty())
    }
}
