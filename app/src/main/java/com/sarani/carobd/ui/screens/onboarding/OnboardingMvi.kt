package com.sarani.carobd.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarani.carobd.data.repository.OwnedVehicleRepository
import com.sarani.carobd.ui.screens.owned.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val owners: OwnedVehicleRepository, private val finish: suspend () -> Unit) : ViewModel() {
    private val saving = MutableStateFlow(false)
    val busy = saving.asStateFlow()
    private val failure = MutableStateFlow<OwnedVehicleEditorError?>(null)
    val error = failure.asStateFlow()
    fun complete(draft: OwnedVehicleDraft?) {
        if (saving.value) return
        val parsed = draft?.parse()
        if (parsed?.error != null) { failure.value = parsed.error; return }
        saving.value = true
        failure.value = null
        viewModelScope.launch {
            try {
                parsed?.vehicle?.let { vehicle ->
                    owners.save(vehicle).getOrThrow()
                    check(owners.select(vehicle.id)) { "Owner selection failed" }
                }
                finish()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { failure.value = OwnedVehicleEditorError.SAVE_FAILED }
            finally { saving.value = false }
        }
    }
}
