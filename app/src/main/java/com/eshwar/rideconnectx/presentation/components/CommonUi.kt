package com.eshwar.rideconnectx.presentation.components

import com.eshwar.rideconnectx.presentation.theme.isGlass
import com.eshwar.rideconnectx.presentation.theme.glassButton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.eshwar.rideconnectx.presentation.theme.pressScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eshwar.rideconnectx.R
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType

/**
 * Shared building blocks, translated 1:1 from the Figma design
 * (`App.tsx` → StatusBar, GestureBar, PrimaryBtn, LogoIcon, LogoMark).
 *
 * Alpha suffixes in the design (e.g. `${C.blue}28`) are hex bytes;
 * they are converted here to Compose alpha fractions.
 */

/*
 * Note: the design also draws a mock status bar ("9:41" + signal/wifi/battery)
 * and an iOS home-indicator pill. Those exist only so the Figma frame looks like
 * a phone — Android draws both for real, so they are deliberately not ported.
 * Screens inset their content with WindowInsets.safeDrawing instead.
 */

/**
 * Full-width CTA.
 * Primary  → blue gradient fill with glow shadow.
 * Secondary → translucent blue with blue border.
 */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    secondary: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val base = modifier
        .pressScale(pressed && enabled, 0.97f)
        .height(56.dp)
        // The design dims disabled buttons to 40%; without this they look tappable.
        .alpha(if (enabled) 1f else 0.4f)
        .clip(shape)

    val styled = if (isGlass) {
        base.glassButton(shape, c.blue, strong = !secondary, pressed = pressed)
    } else if (secondary) {
        base
            .background(c.blue.copy(alpha = 0.051f))
            .border(1.5.dp, c.blue.copy(alpha = 0.267f), shape)
    } else {
        base.background(
            Brush.linearGradient(listOf(c.blue, Color(0xFF1A56CC)))
        )
    }

    Box(
        modifier = styled
            .then(if (enabled) Modifier.clickable(interaction, LocalIndication.current, onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = RcxType.Button.copy(fontWeight = FontWeight.SemiBold),
                color = if (secondary) c.blue else Color.White,
            )
            icon?.invoke()
        }
    }
}

/** Progress dots — the active dot is a wide pill, the rest are small. */
@Composable
fun DotIndicator(
    count: Int,
    active: Int,
    modifier: Modifier = Modifier,
) {
    val c = Rcx.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val on = i == active
            // The active pill stretches and the colour follows, so moving a
            // page reads as the pill travelling rather than two dots swapping.
            val w by animateDpAsState(if (on) 22.dp else 7.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "dotW")
            val col by animateColorAsState(if (on) c.blue else c.blue.copy(alpha = 0.157f), label = "dotC")
            Box(
                Modifier
                    .width(w)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(col)
            )
        }
    }
}

/** Aspect ratio of the RCX wordmark artwork (783 × 285 in the source). */
private const val WORDMARK_RATIO = 783f / 285f

/**
 * The brand mark on its own.
 *
 * Renders [R.drawable.ic_rcx_wordmark] — the traced vector of the approved
 * logo — so it stays crisp at any size and keeps its mint-to-blue gradient.
 *
 * @param height how tall the mark should be; width follows the artwork's ratio.
 */
@Composable
fun RcxLogo(
    height: androidx.compose.ui.unit.Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_rcx_wordmark),
        contentDescription = "RideConnectX",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .width(height * WORDMARK_RATIO),
    )
}

/** Hero placement of the brand mark, used on the splash screen. */
@Composable
fun LogoIcon(
    size: androidx.compose.ui.unit.Dp = 96.dp,
    modifier: Modifier = Modifier,
) {
    // `size` is the design's square tile edge; the wordmark is wide, so it is
    // rendered at a matching optical weight rather than a literal square.
    RcxLogo(height = size * 0.62f, modifier = modifier)
}

/** Brand mark plus the "RideConnectX" name, used in screen headers. */
@Composable
fun LogoMark(
    size: androidx.compose.ui.unit.Dp = 34.dp,
    modifier: Modifier = Modifier,
) {
    val c = Rcx.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RcxLogo(height = size * 0.60f)
        Text(
            text = "RideConnectX",
            style = RcxType.Wordmark.copy(
                fontSize = (size.value * 0.37f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = c.text,
        )
    }
}

/**
 * Back arrow plus screen title, used on every pushed screen.
 *
 * @param right optional trailing slot, e.g. Notifications' "Mark all read".
 */
@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    right: (@Composable () -> Unit)? = null,
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .then(
                    if (isGlass) Modifier.glassButton(shape, c.blue, strong = false)
                    else Modifier.clip(shape)
                        .background(c.blue.copy(alpha = 0.07f))
                        .border(1.dp, c.blue.copy(alpha = 0.133f), shape)
                )
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Back",
                modifier = Modifier.size(18.dp),
                tint = c.blue,
            )
        }
        Text(
            text = title,
            style = RcxType.Wordmark.copy(fontSize = 16.sp),
            color = c.text,
            modifier = Modifier.weight(1f),
        )
        right?.invoke()
    }
}

/**
 * The rider's name as it goes to the cluster, updating while they type. The
 * profile packet carries 20 characters, so anything longer is cut here too.
 */
@Composable
fun ClusterGreetingPreview(name: String, modifier: Modifier = Modifier) {
    val shown = name.trim().take(20)
    val shape = RoundedCornerShape(12.dp)
    androidx.compose.foundation.layout.Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A0F14))
            .border(1.dp, Color(0xFF1E2A36), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text("ON YOUR SCOOTER'S DISPLAY", style = RcxType.MonoTiny.copy(fontSize = 8.sp), color = Color(0xFF5B6B7A))
        Text(
            if (shown.isEmpty()) "WELCOME" else "WELCOME $shown",
            style = RcxType.Mono.copy(fontSize = 15.sp),
            color = Color(0xFF7CFFCB),
            maxLines = 1,
        )
    }
}
