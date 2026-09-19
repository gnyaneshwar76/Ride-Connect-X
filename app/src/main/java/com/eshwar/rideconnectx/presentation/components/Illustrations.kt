package com.eshwar.rideconnectx.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eshwar.rideconnectx.R
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxColors
import com.eshwar.rideconnectx.presentation.theme.RcxType
import kotlin.math.min

/**
 * Hero illustrations from the design. In the Figma export these are inline SVGs
 * on a 355 × 260 viewBox with `preserveAspectRatio="xMidYMid slice"`, so the
 * helpers below map that same coordinate space onto the Compose canvas and
 * scale-to-cover, matching the web output.
 *
 * **These slots now show photographs.** [WelcomeHero] and [ObIllustration] draw
 * the generated images from `res/drawable-nodpi/`; the vector versions they
 * replaced are kept below as [WelcomeHeroVector] and [ObIllustrationVector].
 * They are the design's own artwork and the photographs are an addition on top
 * of it, so switching back is one line at each call site rather than a rewrite.
 */

private const val VB_W = 355f
private const val VB_H = 260f

/**
 * Maps the design's 355×260 viewBox onto whatever space the phone gives us.
 *
 * The web export used `slice` (scale-to-cover), which is fine when the box is
 * roughly as wide as it is tall. A real phone's illustration area is far taller
 * than that, and cover would zoom ~5× and crop the artwork away. So we scale to
 * *contain* instead: the whole drawing always stays visible, centred, at correct
 * proportions — on a small phone, a large phone, or a tablet.
 */
private class ViewBox(canvas: Size) {
    val scale = min(canvas.width / VB_W, canvas.height / VB_H)
    val dx = (canvas.width - VB_W * scale) / 2f
    val dy = (canvas.height - VB_H * scale) / 2f

    fun x(v: Float) = dx + v * scale
    fun y(v: Float) = dy + v * scale
    fun len(v: Float) = v * scale
    fun pt(px: Float, py: Float) = Offset(x(px), y(py))
}

private fun DrawScope.vbLine(vb: ViewBox, x1: Float, y1: Float, x2: Float, y2: Float, color: Color, width: Float, alpha: Float) {
    drawLine(color.copy(alpha = alpha), vb.pt(x1, y1), vb.pt(x2, y2), strokeWidth = vb.len(width))
}

private fun DrawScope.vbCircle(vb: ViewBox, cx: Float, cy: Float, r: Float, color: Color, alpha: Float) {
    drawCircle(color.copy(alpha = alpha), radius = vb.len(r), center = vb.pt(cx, cy))
}

private fun DrawScope.vbCircleStroke(vb: ViewBox, cx: Float, cy: Float, r: Float, color: Color, width: Float, alpha: Float) {
    drawCircle(color.copy(alpha = alpha), radius = vb.len(r), center = vb.pt(cx, cy), style = Stroke(vb.len(width)))
}

private fun DrawScope.vbRoundRect(vb: ViewBox, x: Float, y: Float, w: Float, h: Float, r: Float, color: Color, alpha: Float) {
    drawRoundRect(
        color = color.copy(alpha = alpha),
        topLeft = vb.pt(x, y),
        size = Size(vb.len(w), vb.len(h)),
        cornerRadius = CornerRadius(vb.len(r), vb.len(r)),
    )
}

// ── 02 Welcome hero ────────────────────────────────────────────────

/**
 * The welcome photograph, with the design's fade and live-status chip on top.
 *
 * The image is drawn to fill rather than at its own 4:5 ratio: this slot takes
 * whatever height is left after the header and the bottom block, which is a
 * different shape on every phone. Cropping a scene is invisible; letterboxing
 * it inside a rounded card is not.
 */
@Composable
fun WelcomeHero(modifier: Modifier = Modifier) {
    val c = Rcx.colors
    val dark = c.isDark

    RcxPhotoFill(
        res = R.drawable.img_welcome_hero,
        modifier = modifier,
    ) {
        HeroFadeAndStatusChip(dark = dark, c = c)
    }
}

/** The pre-photograph Figma artwork: map grid, route, destination pin, skyline. */
@Composable
fun WelcomeHeroVector(modifier: Modifier = Modifier) {
    val c = Rcx.colors
    val dark = c.isDark

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    if (dark) listOf(Color(0xFF030A18), Color(0xFF070F22))
                    else listOf(Color(0xFF8AB0FF), Color(0xFFC8D8FF))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val vb = ViewBox(size)

            // Grid
            listOf(80f, 180f, 270f).forEach { vbLine(vb, it, 0f, it, 260f, c.blue, 1f, 0.1f) }
            listOf(80f, 140f, 200f).forEach { vbLine(vb, 0f, it, 355f, it, c.blue, 1f, 0.1f) }

            // Route: M0 180 Q80 165 140 155 Q210 142 270 146 Q310 146 355 135
            val route = Path().apply {
                moveTo(vb.x(0f), vb.y(180f))
                quadraticBezierTo(vb.x(80f), vb.y(165f), vb.x(140f), vb.y(155f))
                quadraticBezierTo(vb.x(210f), vb.y(142f), vb.x(270f), vb.y(146f))
                quadraticBezierTo(vb.x(310f), vb.y(146f), vb.x(355f), vb.y(135f))
            }
            drawPath(route, c.blue.copy(alpha = 0.7f), style = Stroke(vb.len(3.5f)))
            drawPath(route, c.cyan.copy(alpha = 0.4f), style = Stroke(vb.len(1.5f)))

            // Destination pin
            vbCircle(vb, 200f, 148f, 14f, c.blue, 0.12f)
            vbCircle(vb, 200f, 148f, 7f, c.blue, 0.9f)
            vbCircle(vb, 200f, 148f, 3f, Color.White, 0.95f)

            // Skyline
            if (dark) {
                val towers = listOf(
                    listOf(20f, 90f, 28f, 50f), listOf(60f, 70f, 22f, 70f),
                    listOf(290f, 80f, 30f, 60f), listOf(320f, 55f, 18f, 75f),
                )
                towers.forEach { (x, y, w, h) ->
                    vbRoundRect(vb, x, y, w, h, 3f, Color(0xFF0A1632), 0.85f)
                    listOf(4f to 8f, 4f to 14f, 10f to 8f, 10f to 14f).forEach { (ox, oy) ->
                        vbRoundRect(vb, x + ox, y + oy, 5f, 4f, 1f, c.amber, 0.55f)
                    }
                }
            } else {
                listOf(
                    listOf(20f, 100f, 28f, 50f), listOf(60f, 80f, 22f, 60f),
                    listOf(290f, 90f, 30f, 50f), listOf(320f, 65f, 18f, 65f),
                ).forEach { (x, y, w, h) ->
                    vbRoundRect(vb, x, y, w, h, 3f, Color(0xFF78A0DC), 0.4f)
                }
            }
        }

        HeroFadeAndStatusChip(dark = dark, c = c)
    }
}

/**
 * The furniture that sits on the welcome hero, shared by the photograph and the
 * vector version so the two cannot drift apart: a fade into the screen
 * background, and the design's live-status chip.
 */
@Composable
private fun BoxScope.HeroFadeAndStatusChip(dark: Boolean, c: RcxColors) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(90.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, if (dark) Color(0xFF070D1B) else Color(0xFFEEF2FF))
                )
            )
    )

    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) Color(0xFF060C18).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f))
            .border(1.dp, c.blue.copy(alpha = 0.133f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.green)
        )
        Text(
            text = "Ride Connect · Access 125 · BLE Paired",
            style = RcxType.BodySmall.copy(fontSize = 11.sp),
            color = c.text,
            modifier = Modifier.weight(1f),
        )
        Text("LIVE", style = RcxType.Mono.copy(fontSize = 9.sp), color = c.green)
    }
}

// ── 03-05 Onboarding illustrations ─────────────────────────────────

enum class ObArt { Nav, Ble, Stats }

@DrawableRes
private fun ObArt.photo(): Int = when (this) {
    ObArt.Nav -> R.drawable.img_onboarding_navigation
    ObArt.Ble -> R.drawable.img_onboarding_bluetooth
    ObArt.Stats -> R.drawable.img_onboarding_intelligence
}

/**
 * Per-step onboarding photograph, fading into the screen background exactly as
 * the vector version did — the fade is what stops the image ending on a hard
 * line above the title.
 */
@Composable
fun ObIllustration(type: ObArt, modifier: Modifier = Modifier) {
    val dark = Rcx.colors.isDark

    RcxPhotoFill(
        res = type.photo(),
        modifier = modifier,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1f to if (dark) Color(0xFF070D1B) else Color(0xFFEEF2FF),
                    )
                )
        )
    }
}

/** The pre-photograph Figma artwork: route map, BLE ripples, or a bar chart. */
@Composable
fun ObIllustrationVector(type: ObArt, modifier: Modifier = Modifier) {
    val c = Rcx.colors
    val dark = c.isDark

    val bg = when {
        dark && type == ObArt.Nav -> listOf(Color(0xFF030A18), Color(0xFF060D1B))
        dark && type == ObArt.Ble -> listOf(Color(0xFF030E14), Color(0xFF060D1B))
        dark -> listOf(Color(0xFF100A02), Color(0xFF060D1B))
        type == ObArt.Nav -> listOf(Color(0xFF8AB0FF), Color(0xFFD8E8FF))
        type == ObArt.Ble -> listOf(Color(0xFF7AD8F0), Color(0xFFD8EEFF))
        else -> listOf(Color(0xFFFFD080), Color(0xFFFFECD8))
    }

    Box(modifier = modifier.background(Brush.verticalGradient(bg))) {
        Canvas(Modifier.fillMaxSize()) {
            val vb = ViewBox(size)

            when (type) {
                ObArt.Nav -> {
                    listOf(60f, 120f, 180f, 240f, 300f).forEach { vbLine(vb, it, 0f, it, 260f, c.blue, 1f, 0.08f) }
                    listOf(60f, 120f, 180f, 240f).forEach { vbLine(vb, 0f, it, 355f, it, c.blue, 1f, 0.08f) }

                    val road = Path().apply {
                        moveTo(vb.x(0f), vb.y(120f))
                        quadraticBezierTo(vb.x(80f), vb.y(105f), vb.x(160f), vb.y(95f))
                        quadraticBezierTo(vb.x(240f), vb.y(82f), vb.x(355f), vb.y(88f))
                    }
                    drawPath(road, c.blue.copy(alpha = 0.7f), style = Stroke(vb.len(4f)))

                    // Leg from current position up to the pin
                    val leg = Path().apply {
                        moveTo(vb.x(90f), vb.y(200f))
                        lineTo(vb.x(90f), vb.y(170f))
                        lineTo(vb.x(155f), vb.y(96f))
                    }
                    drawPath(leg, c.cyan.copy(alpha = 0.5f), style = Stroke(vb.len(2.5f)))

                    vbCircle(vb, 155f, 96f, 12f, c.blue, 0.15f)
                    vbCircle(vb, 155f, 96f, 6f, c.blue, 1f)
                    vbCircle(vb, 155f, 96f, 2.5f, Color.White, 1f)
                    vbCircle(vb, 90f, 200f, 8f, c.cyan, 0.8f)
                }

                ObArt.Ble -> {
                    (1..4).forEach { i ->
                        vbCircleStroke(vb, 177f, 130f, i * 38f, c.cyan, 1.5f, 0.55f - i * 0.1f)
                    }
                    vbCircle(vb, 177f, 130f, 28f, c.cyan, 0.12f)
                    vbCircle(vb, 177f, 130f, 16f, c.cyan, 0.25f)

                    // Bluetooth glyph
                    val bt = Path().apply {
                        moveTo(vb.x(172f), vb.y(112f))
                        lineTo(vb.x(186f), vb.y(122f))
                        lineTo(vb.x(176f), vb.y(130f))
                        lineTo(vb.x(186f), vb.y(138f))
                        lineTo(vb.x(172f), vb.y(148f))
                        lineTo(vb.x(172f), vb.y(130f))
                        close()
                    }
                    drawPath(bt, c.cyan, style = Stroke(vb.len(2f)))

                    // Paired device bubbles
                    vbCircle(vb, 90f, 90f, 22f, c.card, 0.5f)
                    vbCircleStroke(vb, 90f, 90f, 22f, c.cyan, 1.5f, 0.4f)
                    vbCircle(vb, 264f, 90f, 22f, c.card, 0.5f)
                    vbCircleStroke(vb, 264f, 90f, 22f, c.cyan, 1.5f, 0.4f)

                    vbLine(vb, 112f, 90f, 155f, 125f, c.cyan, 1.5f, 0.5f)
                    vbLine(vb, 242f, 90f, 199f, 125f, c.cyan, 1.5f, 0.5f)
                }

                ObArt.Stats -> {
                    listOf(
                        60f to 80f, 100f to 120f, 140f to 60f, 180f to 150f,
                        220f to 100f, 260f to 130f, 300f to 90f,
                    ).forEach { (x, h) ->
                        vbRoundRect(vb, x, 200f - h, 28f, h, 5f, c.amber, 0.7f)
                    }
                    val trend = Path().apply {
                        moveTo(vb.x(60f), vb.y(165f))
                        quadraticBezierTo(vb.x(100f), vb.y(130f), vb.x(140f), vb.y(155f))
                        quadraticBezierTo(vb.x(180f), vb.y(115f), vb.x(220f), vb.y(130f))
                        quadraticBezierTo(vb.x(260f), vb.y(100f), vb.x(300f), vb.y(120f))
                    }
                    drawPath(trend, c.amber.copy(alpha = 0.9f), style = Stroke(vb.len(2.5f)))
                }
            }
        }

        // Fade the lower half into the screen background
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to Color.Transparent,
                        1f to if (dark) Color(0xFF070D1B) else Color(0xFFEEF2FF),
                    )
                )
        )
    }
}
