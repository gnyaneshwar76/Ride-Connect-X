package com.eshwar.rideconnectx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.data.local.UserPreferencesStore
import com.eshwar.rideconnectx.data.repository.VehicleRepository
import com.eshwar.rideconnectx.domain.model.Vehicle
import com.eshwar.rideconnectx.domain.model.VehicleColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the rider's account already holds, ready to be confirmed or replaced. */
data class FoundProfile(
    val riderName: String = "",
    val location: String = "",
    val vehicle: Vehicle? = null,
    val color: VehicleColor? = null,
) {
    val hasSomethingToShow: Boolean get() = riderName.isNotBlank() || vehicle != null
}

/**
 * Backs the screen shown when signing in finds an existing profile.
 *
 * The rider decides whether to carry on with it or start over — the app does not
 * silently adopt old data, and it does not silently discard it either.
 */
@HiltViewModel
class ProfileFoundViewModel @Inject constructor(
    private val prefs: UserPreferencesStore,
    private val vehicles: VehicleRepository,
) : ViewModel() {

    val profile: StateFlow<FoundProfile> =
        combine(prefs.riderName, prefs.riderLocation, vehicles.selection) { name, city, sel ->
            FoundProfile(
                riderName = name,
                location = city,
                vehicle = sel.vehicle,
                color = sel.color,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, FoundProfile())

    /**
     * Clears the restored details so Create Profile genuinely starts blank.
     *
     * The vehicle and colour are cleared too. Leaving them behind meant "set up
     * a new profile" still arrived with the old scooter already chosen, which is
     * not starting over.
     *
     * Only local state is touched. The cloud copy stays until the rider finishes
     * the new profile and it gets written over, so backing out here costs them
     * nothing.
     */
    fun startFresh(onDone: () -> Unit) {
        viewModelScope.launch {
            prefs.saveRiderName("")
            prefs.saveRiderLocation("")
            prefs.saveVehicle("")
            prefs.saveColor("")
            prefs.setProfileCompleted(false)
            onDone()
        }
    }
}
