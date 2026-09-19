package com.eshwar.rideconnectx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.data.local.UserPreferencesStore
import com.eshwar.rideconnectx.domain.model.LegalDoc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tracks acceptance of the Terms and the Privacy Policy.
 *
 * Acceptance is stamped with a version, so a future revision of either document
 * can require the user to accept again.
 */
@HiltViewModel
class LegalViewModel @Inject constructor(
    private val prefs: UserPreferencesStore,
) : ViewModel() {

    val termsAccepted: StateFlow<Boolean> = prefs.termsAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val privacyAccepted: StateFlow<Boolean> = prefs.privacyAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun accept(doc: LegalDoc) = viewModelScope.launch {
        when (doc) {
            LegalDoc.TERMS -> prefs.acceptTerms()
            LegalDoc.PRIVACY -> prefs.acceptPrivacy()
            // The combined page covers both documents at once.
            LegalDoc.BOTH -> {
                prefs.acceptTerms()
                prefs.acceptPrivacy()
            }
        }
    }
}
