package com.eshwar.rideconnectx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.data.local.AppSettingsStore
import com.eshwar.rideconnectx.domain.model.AccentColor
import com.eshwar.rideconnectx.domain.model.FontSize
import com.eshwar.rideconnectx.domain.model.SurfaceStyle
import com.eshwar.rideconnectx.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Screen 22 — Appearance, and the app-wide theme it drives.
 *
 * Also read by `MainActivity`, which is why it is a ViewModel of its own rather
 * than part of `SettingsViewModel`: the theme has to be known before any screen
 * is composed.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val store: AppSettingsStore,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        store.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val fontSize: StateFlow<FontSize> =
        store.fontSize.stateIn(viewModelScope, SharingStarted.Eagerly, FontSize.MEDIUM)

    val accentColor: StateFlow<AccentColor> =
        store.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.BLUE)

    /** Flat or glass. Read app-wide so every surface switches together. */
    val surfaceStyle: StateFlow<SurfaceStyle> =
        store.settings
            .map { it.surfaceStyle }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, SurfaceStyle.FLAT)


    fun setThemeMode(mode: ThemeMode) = appScope.launch { store.setThemeMode(mode) }

    fun setFontSize(size: FontSize) = appScope.launch { store.setFontSize(size) }

    fun setAccentColor(accent: AccentColor) = appScope.launch { store.setAccentColor(accent) }

    fun setSurfaceStyle(style: SurfaceStyle) = appScope.launch { store.setSurfaceStyle(style) }
}
