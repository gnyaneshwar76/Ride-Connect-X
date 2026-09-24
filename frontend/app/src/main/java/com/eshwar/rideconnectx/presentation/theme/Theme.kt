package com.eshwar.rideconnectx.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.eshwar.rideconnectx.domain.model.AccentColor

/**
 * Provides the active [RcxColors] to the whole tree.
 * Read it from any composable with `Rcx.colors`.
 */
val LocalRcxColors = staticCompositionLocalOf { RcxDarkColors }

/**
 * Entry point for design tokens.
 *
 *     Text("Hello", color = Rcx.colors.text, style = RcxType.Body)
 */
object Rcx {
    val colors: RcxColors
        @Composable @ReadOnlyComposable get() = LocalRcxColors.current
}

private fun materialSchemeFrom(c: RcxColors) = if (c.isDark) {
    darkColorScheme(
        primary = c.blue,
        onPrimary = DarkText,
        secondary = c.cyan,
        background = c.bg,
        onBackground = c.text,
        surface = c.card,
        onSurface = c.text,
        surfaceVariant = c.card2,
        onSurfaceVariant = c.muted,
        error = c.red,
        outline = c.border,
    )
} else {
    lightColorScheme(
        primary = c.blue,
        onPrimary = LightCard,
        secondary = c.cyan,
        background = c.bg,
        onBackground = c.text,
        surface = c.card,
        onSurface = c.text,
        surfaceVariant = c.card2,
        onSurfaceVariant = c.muted,
        error = c.red,
        outline = c.border,
    )
}

/**
 * Wraps the app in the RideConnectX design system.
 *
 * @param darkTheme when true the DARK palette is used. Defaults to the system
 *        setting; Screen 22 (Appearance) passes the rider's saved preference.
 * @param fontScale multiplies the *existing* density font scale rather than
 *        replacing it, so the rider's Android accessibility text size is
 *        respected instead of being overridden — the design calls for
 *        "preserve accessibility scaling".
 */
@Composable
fun RcxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontScale: Float = 1f,
    accent: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) RcxDarkColors else RcxLightColors
    // The accent replaces `blue` everywhere, which is what every screen tints
    // itself with. Borders and the glow are derived from it too, or a green
    // accent would still sit inside blue-tinted card outlines.
    val accentColor = Color(if (darkTheme) accent.darkArgb else accent.lightArgb)
    val colors = if (accent == AccentColor.BLUE) base else base.copy(
        blue = accentColor,
        border = accentColor.copy(alpha = if (darkTheme) 0.15f else 0.16f),
        glow = accentColor.copy(alpha = if (darkTheme) 0.30f else 0.09f),
    )
    val density = LocalDensity.current
    val scaled = Density(
        density = density.density,
        fontScale = density.fontScale * fontScale,
    )

    CompositionLocalProvider(
        LocalRcxColors provides colors,
        LocalDensity provides scaled,
    ) {
        MaterialTheme(
            colorScheme = materialSchemeFrom(colors),
            typography = RcxTypography,
            content = content,
        )
    }
}
