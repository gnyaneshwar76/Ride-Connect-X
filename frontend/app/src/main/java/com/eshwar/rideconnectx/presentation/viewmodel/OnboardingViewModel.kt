package com.eshwar.rideconnectx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.domain.model.OnboardingStatus
import com.eshwar.rideconnectx.domain.repository.BleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val bleRepository: BleRepository
) : ViewModel() {

    val onboardingStatus: StateFlow<OnboardingStatus> = bleRepository.onboardingStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = OnboardingStatus.NOT_STARTED
        )

    fun completeWelcome() {
        viewModelScope.launch {
            bleRepository.updateOnboardingStatus(OnboardingStatus.WELCOME_COMPLETED)
        }
    }

    fun grantPermissions() {
        viewModelScope.launch {
            bleRepository.updateOnboardingStatus(OnboardingStatus.PERMISSION_GRANTED)
        }
    }

    fun completePairing() {
        viewModelScope.launch {
            bleRepository.updateOnboardingStatus(OnboardingStatus.PAIRING_COMPLETED)
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            bleRepository.updateOnboardingStatus(OnboardingStatus.FINISHED)
        }
    }
}
