package com.eshwar.rideconnectx.presentation.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/**
 * The app's motion vocabulary, in one place so every screen moves the same way.
 *
 * Compose scales all of these by the phone's animator duration scale, so a
 * rider with "Remove animations" on gets the end state instantly — nothing here
 * has to check for it.
 */
object RcxMotion {
    /** Screen push/pop and most enter animations. */
    const val SCREEN_MS = 380
    /** Delay between siblings in a staggered entrance. */
    const val STAGGER_MS = 55L
    /** Number count-ups. */
    const val COUNT_MS = 1100

    /** Presses: quick, with a little overshoot on release. */
    fun <T> press() = spring<T>(dampingRatio = 0.55f, stiffness = 900f)
    /** Things that pop in (badges, dots, FAB). */
    fun <T> pop() = spring<T>(dampingRatio = 0.5f, stiffness = 500f)
}

/**
 * Fades and lifts the element in the first time it appears — once per screen
 * visit, not on every recomposition or scroll back. [index] staggers siblings.
 */
fun Modifier.enterRise(index: Int = 0, distanceDp: Float = 16f): Modifier = composed {
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(index * RcxMotion.STAGGER_MS)
            progress.animateTo(1f, tween(RcxMotion.SCREEN_MS + 120, easing = FastOutSlowInEasing))
            played = true
        }
    }
    val px = with(LocalDensity.current) { distanceDp * density }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * px
    }
}

/** Shrinks slightly while [pressed] and springs back on release. */
fun Modifier.pressScale(pressed: Boolean, to: Float = 0.96f): Modifier = composed {
    val s by animateFloatAsState(if (pressed) to else 1f, RcxMotion.press(), label = "pressScale")
    graphicsLayer { scaleX = s; scaleY = s }
}

/**
 * A short horizontal shake each time [trigger] changes to a new non-zero
 * value — for input that was refused.
 */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        x.animateTo(0f, keyframes {
            durationMillis = 360
            -14f at 60; 14f at 130; -10f at 200; 8f at 270; 0f at 360
        })
    }
    graphicsLayer { translationX = x.value * density }
}

/** A slow, endless 0→1→0 value for glows and floats. */
@Composable
fun rememberBreath(periodMs: Int = 3200, label: String = "breath"): State<Float> =
    rememberInfiniteTransition(label = label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = label,
    )

/** Counts from 0 (first show) or the previous value up to [target]. */
@Composable
fun animatedCount(target: Float?, durationMs: Int = RcxMotion.COUNT_MS): Float? {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { if (target != null) a.animateTo(target, tween(durationMs, easing = FastOutSlowInEasing)) }
    return target?.let { a.value }
}
