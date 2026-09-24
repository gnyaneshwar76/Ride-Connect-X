package com.eshwar.rideconnectx.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eshwar.rideconnectx.domain.model.AccentColor
import com.eshwar.rideconnectx.domain.model.SurfaceStyle
import com.eshwar.rideconnectx.domain.model.AppSettings
import com.eshwar.rideconnectx.domain.model.DistanceUnit
import com.eshwar.rideconnectx.domain.model.FontSize
import com.eshwar.rideconnectx.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettings: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Everything Screen 20 (Settings) and Screen 22 (Appearance) persist.
 *
 * One store, because the two screens edit one set of preferences and the design
 * has Appearance opening *from* Settings. Every write lands immediately — the
 * design's rule is that a toggle saves the moment it moves.
 *
 * Enums are stored by name so adding a case never renumbers the existing ones,
 * and an unreadable value falls back to the default rather than throwing.
 */
@Singleton
class AppSettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        // General
        val UNITS = stringPreferencesKey("distance_unit")
        val SURFACE_STYLE = stringPreferencesKey("surface_style")
        val AUTO_START_NAV = booleanPreferencesKey("auto_start_navigation")

        // Bluetooth
        val AUTO_CONNECT = booleanPreferencesKey("ble_auto_connect")

        // Notifications
        val RIDE_NOTIFICATIONS = booleanPreferencesKey("notify_rides")
        val CONNECTION_ALERTS = booleanPreferencesKey("notify_connection")

        // Appearance
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent_color")
        val FONT_SIZE = stringPreferencesKey("font_size")
    }

    val settings: Flow<AppSettings> = context.appSettings.data.map { p ->
        AppSettings(
            distanceUnit = p[UNITS].toEnum(DistanceUnit.KM),
            surfaceStyle = p[SURFACE_STYLE].toEnum(SurfaceStyle.FLAT),
            autoStartNavigation = p[AUTO_START_NAV] ?: true,
            autoConnect = p[AUTO_CONNECT] ?: true,
            rideNotifications = p[RIDE_NOTIFICATIONS] ?: true,
            connectionAlerts = p[CONNECTION_ALERTS] ?: true,
            themeMode = p[THEME_MODE].toEnum(ThemeMode.SYSTEM),
            accentColor = p[ACCENT].toEnum(AccentColor.BLUE),
            fontSize = p[FONT_SIZE].toEnum(FontSize.MEDIUM),
        )
    }

    /** Read on their own by the theme, which exists before any screen does. */
    val themeMode: Flow<ThemeMode> =
        context.appSettings.data.map { it[THEME_MODE].toEnum(ThemeMode.SYSTEM) }

    val accentColor: Flow<AccentColor> =
        context.appSettings.data.map { it[ACCENT].toEnum(AccentColor.BLUE) }

    val fontSize: Flow<FontSize> =
        context.appSettings.data.map { it[FONT_SIZE].toEnum(FontSize.MEDIUM) }

    suspend fun setDistanceUnit(unit: DistanceUnit) = put(UNITS, unit.name)

    suspend fun setSurfaceStyle(style: SurfaceStyle) = put(SURFACE_STYLE, style.name)
    suspend fun setAutoStartNavigation(on: Boolean) = put(AUTO_START_NAV, on)

    suspend fun setAutoConnect(on: Boolean) = put(AUTO_CONNECT, on)

    suspend fun setRideNotifications(on: Boolean) = put(RIDE_NOTIFICATIONS, on)
    suspend fun setConnectionAlerts(on: Boolean) = put(CONNECTION_ALERTS, on)

    suspend fun setThemeMode(mode: ThemeMode) = put(THEME_MODE, mode.name)
    suspend fun setAccentColor(accent: AccentColor) = put(ACCENT, accent.name)
    suspend fun setFontSize(size: FontSize) = put(FONT_SIZE, size.name)

    private suspend fun <T> put(key: Preferences.Key<T>, value: T) {
        context.appSettings.edit { it[key] = value }
    }
}

/** Stored name → enum, falling back to [fallback] on anything unrecognised. */
private inline fun <reified E : Enum<E>> String?.toEnum(fallback: E): E =
    this?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: fallback
