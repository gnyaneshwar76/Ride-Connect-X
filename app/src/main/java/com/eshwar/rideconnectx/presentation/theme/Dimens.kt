package com.eshwar.rideconnectx.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing and shape values from the design spec.
 * Screens should reference these instead of literal dp values.
 */
object Dimens {
    /** Standard horizontal screen padding — used on every screen. */
    val ScreenPadding = 24.dp

    /** Vertical gap between cards / sections. */
    val CardGap = 16.dp

    /** Tighter gap — notification rows, settings rows. */
    val RowGap = 12.dp

    /** Card corner radius. */
    val CardRadius = 20.dp

    /** Primary button corner radius. */
    val ButtonRadius = 16.dp

    /** Inner padding inside a card. */
    val CardPadding = 16.dp

    /** Height of the primary full-width CTA. */
    val ButtonHeight = 56.dp

    /** Square ghost / back button. */
    val IconButtonSize = 56.dp

    /** Card border thickness. */
    val BorderWidth = 1.dp
}
