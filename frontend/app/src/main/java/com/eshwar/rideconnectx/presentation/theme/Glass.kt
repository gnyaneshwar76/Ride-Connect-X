package com.eshwar.rideconnectx.presentation.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.HazeState

/**
 * The "Liquid Glass" surface, ported from the Figma export of 16 August 2026.
 *
 * Every number here comes from `GDK` / `GLT` in that file — Figma delivered the
 * token set as explicit values precisely so it could be typed in rather than
 * eyeballed. Do not adjust these by feel; change them in Figma and re-port.
 *
 * ## About the blur
 *
 * **Never use `Modifier.blur` for this.** It blurs the node's *own* content —
 * its text and icons — not what is behind it. Reaching for it produced exactly
 * that: frosted panels with their labels smeared away to nothing.
 *
 * Real backdrop blur comes from Haze: a screen marks its background with
 * [Modifier.hazeBackdrop] and each surface samples it through [hazeChild].
 * That is what gives the Apple-style frosting.
 *
 * `RenderEffect` needs Android 12 (API 31). Below that Haze cannot blur, so the
 * surface falls back to [GlassTokens.fallback] — an opaque colour Figma chose
 * to sit closest to the blurred result, because a transparent panel with no
 * blur over a photograph is unreadable.
 */

/** Which surface style the rider has chosen. Mirrors `StyleMode` in the export. */
enum class StyleMode { FLAT, GLASS }

/** Interaction state of a glass surface. Mirrors `GMode`. */
enum class GlassState { Default, Pressed, Disabled }

data class GlassTokens(
    val blur: Dp,
    val fill: Color,
    val fillAccent: Color,
    val fillPressed: Color,
    val fillDisabled: Color,
    /** Opaque stand-in for the blur on Android 11 and below. */
    val fallback: Color,
    val border: Color,
    val borderAccent: Color,
    val borderDisabled: Color,
    /** The bright hairline along the top edge — what actually sells glass. */
    val highlight: Color,
    /** Soft light pooling at the top of the surface. */
    val specular: Color,
    /** Hairline inner rim - `innerGlow` in the export. */
    val innerGlow: Color,
)

/**
 * Which glass tier a surface uses. Added in the export of 18 August 2026.
 *
 * One fill could not serve both jobs: a panel over a photograph has to stay
 * legible against anything, while a decorative panel over a controlled colour
 * field should let that colour through. Trying to do both with a single 58%
 * fill is what made the whole app look uniformly dim.
 */
enum class GlassTier { HEAVY, LIGHT }

/**
 * `GDK_H` - dark, **heavy**. Panels that must stay legible over photographs or
 * arbitrary backgrounds: Statistics tiles and period tabs, the navigation
 * overlay, sheet bodies.
 */
val GlassDarkHeavy = GlassTokens(
    blur = 20.dp,
    fill = Color(0x9E070D1B),           // rgba(7,13,27,0.62)
    fillAccent = Color(0xB82B7FFF),     // rgba(43,127,255,0.72)
    fillPressed = Color(0xB8070D1B),    // rgba(7,13,27,0.72)
    fillDisabled = Color(0x66070D1B),   // rgba(7,13,27,0.40)
    fallback = Color(0xFF0D1A30),
    border = Color(0x2EFFFFFF),         // rgba(255,255,255,0.18)
    borderAccent = Color(0xB32B7FFF),   // rgba(43,127,255,0.70)
    borderDisabled = Color(0x0FFFFFFF), // rgba(255,255,255,0.06)
    highlight = Color(0x4DFFFFFF),      // rgba(255,255,255,0.30)
    specular = Color(0x17FFFFFF),       // rgba(255,255,255,0.09)
    innerGlow = Color(0x17FFFFFF),      // rgba(255,255,255,0.09)
)

/**
 * `GDK_L` - dark, **light**. Decorative panels over controlled colour: the
 * Dashboard quick-action tiles, icon chips, the Appearance card. The colour
 * behind bleeds through and lights the tile from within, which is the point.
 */
val GlassDarkLight = GlassTokens(
    blur = 20.dp,
    fill = Color(0x17FFFFFF),           // rgba(255,255,255,0.09)
    fillAccent = Color(0x382B7FFF),     // rgba(43,127,255,0.22)
    fillPressed = Color(0x2BFFFFFF),    // rgba(255,255,255,0.17)
    fillDisabled = Color(0x0AFFFFFF),   // rgba(255,255,255,0.04)
    fallback = Color(0xFF111E38),
    border = Color(0x3DFFFFFF),         // rgba(255,255,255,0.24)
    borderAccent = Color(0xA62B7FFF),   // rgba(43,127,255,0.65)
    borderDisabled = Color(0x0FFFFFFF), // rgba(255,255,255,0.06)
    highlight = Color(0x75FFFFFF),      // rgba(255,255,255,0.46)
    specular = Color(0x26FFFFFF),       // rgba(255,255,255,0.15)
    innerGlow = Color(0x21FFFFFF),      // rgba(255,255,255,0.13)
)

/** Back-compat alias - the export keeps `GDK` pointing at the light tier. */
val GlassDark = GlassDarkLight

/** `GLT` - light theme. Single tier: light theme always has controlled backgrounds. */
val GlassLight = GlassTokens(
    blur = 20.dp,
    fill = Color(0x4DFFFFFF),           // rgba(255,255,255,0.30)
    fillAccent = Color(0x292B7FFF),     // rgba(43,127,255,0.16)
    fillPressed = Color(0x70FFFFFF),    // rgba(255,255,255,0.44)
    fillDisabled = Color(0x14FFFFFF),   // rgba(255,255,255,0.08)
    fallback = Color(0xFFD8E6F8),
    border = Color(0x94FFFFFF),         // rgba(255,255,255,0.58)
    borderAccent = Color(0x9E2B7FFF),   // rgba(43,127,255,0.62)
    borderDisabled = Color(0x29FFFFFF), // rgba(255,255,255,0.16)
    highlight = Color(0xB8FFFFFF),      // rgba(255,255,255,0.72)
    specular = Color(0x4DFFFFFF),       // rgba(255,255,255,0.30)
    innerGlow = Color(0x3DFFFFFF),      // rgba(255,255,255,0.24)
)

/**
 * The minimum scrim for white text over a photograph.
 *
 * From §2C of the brief: Figma checked this at 4.5:1 (WCAG AA) for 13sp/500
 * white text. It is the floor, not a suggestion — going lighter fails contrast
 * on the bright patches of a photo.
 */
val ScrimMin = Color(0x70070D1B)                    // rgba(7,13,27,0.44)
val ScrimGradientTop = Color(0x33070D1B)            // rgba(7,13,27,0.20)
val ScrimGradientBottom = Color(0x8A070D1B)         // rgba(7,13,27,0.54)

/** True where the platform can actually blur. */
val canBlur: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val LocalStyleMode = staticCompositionLocalOf { StyleMode.FLAT }

/**
 * The blur source for the current screen.
 *
 * Haze works in two halves: something declares "this is the backdrop", and
 * surfaces sample it. Sharing one state through a CompositionLocal means a
 * screen opts in with a single [Modifier.hazeBackdrop] on its background and
 * every [RcxSurface] inside it frosts automatically, with no plumbing.
 *
 * Null where no screen has declared a backdrop — then surfaces stay translucent
 * rather than blurred, which is right: there is nothing behind them worth
 * blurring on a flat background.
 */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Marks this element as the thing glass surfaces blur.
 *
 * Put it on the screen's background — the photograph — and provide the same
 * state through [LocalHaze].
 */
fun Modifier.hazeBackdrop(state: HazeState): Modifier = this.hazeSource(state)

val Rcx.glass: GlassTokens
    @Composable get() = glassTokens(GlassTier.LIGHT)

/**
 * The token set for a tier. Light theme has one tier, so the parameter only
 * matters in dark — which is where photographs and colour fields live.
 */
@Composable
fun glassTokens(tier: GlassTier): GlassTokens = when {
    !Rcx.colors.isDark -> GlassLight
    tier == GlassTier.HEAVY -> GlassDarkHeavy
    else -> GlassDarkLight
}

/**
 * Applies the glass surface to a [Modifier].
 *
 * Order matters and is deliberate: blur first (it samples what is already
 * behind), then the tinted fill on top of it, then the border. Drawing the
 * fill before the blur would blur the fill instead of the background.
 *
 * @param accent use the accent-tinted fill and border — for a selected segment
 *   or a primary action.
 */
@Composable
fun Modifier.glassSurface(
    shape: RoundedCornerShape,
    state: GlassState = GlassState.Default,
    accent: Boolean = false,
    tier: GlassTier = GlassTier.LIGHT,
): Modifier {
    val g = glassTokens(tier)

    val fill = when {
        state == GlassState.Disabled -> g.fillDisabled
        state == GlassState.Pressed -> g.fillPressed
        accent -> g.fillAccent
        else -> g.fill
    }
    val borderColor = when {
        state == GlassState.Disabled -> g.borderDisabled
        accent -> g.borderAccent
        else -> g.border
    }

    val haze = LocalHaze.current
    // Read outside the effect block — that lambda is not composable.
    val base = if (Rcx.colors.isDark) g.fallback else Color.White

    return this
        .clip(shape)
        .then(
            when {
                // Real frosting: samples the screen's declared backdrop.
                haze != null && canBlur -> Modifier.hazeEffect(haze) {
                    blurRadius = g.blur
                    backgroundColor = base
                    tints = listOf(HazeTint(fill))
                }
                // API < 31 has no RenderEffect, so an opaque stand-in rather
                // than a see-through panel that cannot be read.
                haze != null -> Modifier.background(g.fallback)
                // No backdrop declared — plain translucency is correct here.
                else -> Modifier.background(fill)
            }
        )
        .border(1.dp, borderColor, shape)
}

/**
 * The two details that separate "glass" from "a translucent box": a soft
 * specular pool at the top, and a bright hairline along the very top edge.
 *
 * Drawn as a child rather than in the modifier chain because both need to sit
 * above the fill and below the content.
 */
@Composable
fun BoxScope.GlassSheen(
    state: GlassState = GlassState.Default,
    tier: GlassTier = GlassTier.LIGHT,
) {
    if (state == GlassState.Disabled) return
    val g = glassTokens(tier)

    Box(
        Modifier
            .matchParentSize()
            .background(
                // radial-gradient(ellipse 72% 38% at 50% 0%, …) from the export.
                Brush.radialGradient(
                    colors = listOf(g.specular, Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0f),
                    radius = 480f,
                )
            )
    )

    // Inset 20dp each side, as in the export — the highlight stops short of the
    // corners, which is what stops it reading as a drawn border.
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(1.dp)
            .background(g.highlight)
    )
}

/** Scrim for content sitting over a photograph. See [ScrimMin]. */
@Composable
fun Modifier.photoScrim(): Modifier = this.background(
    Brush.linearGradient(listOf(ScrimGradientTop, ScrimGradientBottom))
)
