package com.eshwar.rideconnectx.presentation.theme

import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.staticCompositionLocalOf
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

    /**
     * Apple's SwiftUI springs, converted: SwiftUI describes a spring by its
     * `response` (seconds per oscillation) and damping fraction; Compose by
     * stiffness = (2 pi / response)^2 and the same damping ratio.
     */
    private fun <T> apple(response: Float, damping: Float) =
        spring<T>(dampingRatio = damping, stiffness = (2f * Math.PI.toFloat() / response).let { it * it })

    /** `.smooth` (0.5 s, no bounce): screen pushes, large moves. */
    fun <T> smooth() = apple<T>(0.5f, 1f)
    /** `.snappy` (0.35 s, 0.85): entrances, toggles. */
    fun <T> snappy() = apple<T>(0.35f, 0.85f)
    /** `.bouncy` (0.5 s, 0.7): playful pops. */
    fun <T> bouncy() = apple<T>(0.5f, 0.7f)
    /** `.interactiveSpring` (0.15 s, 0.86): follows a finger. */
    fun <T> interactive() = apple<T>(0.15f, 0.86f)

    /** Presses: the interactive spring, so release feels immediate. */
    fun <T> press() = interactive<T>()
    /** Things that pop in (badges, dots, FAB). */
    fun <T> pop() = bouncy<T>()
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
            progress.animateTo(1f, RcxMotion.snappy())
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
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        // Felt as well as seen: a refusal the rider notices without looking.
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.Reject)
        x.animateTo(0f, keyframes {
            durationMillis = 360
            -14f at 60; 14f at 130; -10f at 200; 8f at 270; 0f at 360
        })
    }
    graphicsLayer { translationX = x.value * density }
}

/**
 * A counter that goes up each time [error] turns on or changes, to feed
 * [shake] — so a refused character wiggles the field once per new message.
 */
@Composable
fun rememberShakeKey(error: String?): Int {
    var key by remember { mutableStateOf(0) }
    LaunchedEffect(error) { if (error != null) key++ }
    return key
}

/**
 * False in Battery Saver (and when the phone's animations are turned off):
 * ambient motion - drifting backgrounds, breathing glows - holds still.
 * Provided by MainActivity.
 */
val LocalAmbientMotion = staticCompositionLocalOf { true }

/** Ambient motion redraws at ~30 fps; see [rememberBreath]. */
private const val AMBIENT_FRAME_MS = 33L

/**
 * A slow, endless 0->1->0 value for glows, floats and drifting backgrounds.
 *
 * Battery: these move too slowly for the eye to tell 30 from 120 frames a
 * second, so they tick ~30 times a second instead of every display refresh.
 * Taps, scrolls and screen changes still run at the display's full rate.
 * Holds at the midpoint when [LocalAmbientMotion] is off.
 */
@Composable
fun rememberBreath(periodMs: Int = 3200, @Suppress("UNUSED_PARAMETER") label: String = "breath"): State<Float> {
    val value = remember { mutableFloatStateOf(0.5f) }
    val on = LocalAmbientMotion.current
    LaunchedEffect(on, periodMs) {
        if (!on) { value.floatValue = 0.5f; return@LaunchedEffect }
        val start = System.nanoTime()
        while (true) {
            val phase = ((System.nanoTime() - start) / 1_000_000L % (2L * periodMs)).toFloat() / periodMs
            val tri = if (phase < 1f) phase else 2f - phase
            value.floatValue = FastOutSlowInEasing.transform(tri)
            delay(AMBIENT_FRAME_MS)
        }
    }
    return value
}

/** Counts from 0 (first show) or the previous value up to [target]. */
@Composable
fun animatedCount(target: Float?, durationMs: Int = RcxMotion.COUNT_MS): Float? {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { if (target != null) a.animateTo(target, tween(durationMs, easing = FastOutSlowInEasing)) }
    return target?.let { a.value }
}

/** Scales in from nothing with a small overshoot: ticks, badges, dots. */
fun Modifier.popIn(): Modifier = composed {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { s.animateTo(1f, RcxMotion.pop()) }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
}

/** A slow few-dp bob for empty-state artwork; still in Battery Saver. */
fun Modifier.floaty(amplitudeDp: Float = 4f): Modifier = composed {
    val t by rememberBreath(3600, "floaty")
    graphicsLayer { translationY = (t - 0.5f) * 2f * amplitudeDp * density }
}

/** A tick that draws itself, short stroke then long, once. */
@Composable
fun DrawnTick(color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p.animateTo(1f, tween(380, delayMillis = 60, easing = FastOutSlowInEasing)) }
    androidx.compose.foundation.Canvas(modifier) {
        val a = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.54f)
        val b = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.76f)
        val c = androidx.compose.ui.geometry.Offset(size.width * 0.84f, size.height * 0.28f)
        val stroke = size.minDimension * 0.12f
        val cap = androidx.compose.ui.graphics.StrokeCap.Round
        val first = (p.value / 0.4f).coerceIn(0f, 1f)
        val second = ((p.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
        if (first > 0f) drawLine(color, a, a + (b - a) * first, stroke, cap)
        if (second > 0f) drawLine(color, b, b + (c - b) * second, stroke, cap)
    }
}

// ── 3D ─────────────────────────────────────────────────────────────

/**
 * The phone's tilt, shared by everything that shows depth.
 *
 * One sensor listener however many surfaces use it, ~30 readings a second (the
 * ambient rate), and only while a screen that uses it is in front. The resting
 * angle follows wherever the phone is held, so the effect answers movement, not
 * posture: hold the phone still at any angle and everything settles level.
 */
private object TiltSensor : android.hardware.SensorEventListener {
    val tilt = mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)
    private var users = 0
    private var rest: FloatArray? = null

    fun acquire(context: android.content.Context) {
        if (users++ > 0) return
        val manager = context.getSystemService(android.hardware.SensorManager::class.java) ?: return
        val sensor = manager.getDefaultSensor(android.hardware.Sensor.TYPE_GRAVITY)
            ?: manager.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) ?: return
        rest = null
        manager.registerListener(this, sensor, 33_000)
    }

    fun release(context: android.content.Context) {
        if (--users > 0) return
        context.getSystemService(android.hardware.SensorManager::class.java)?.unregisterListener(this)
        tilt.value = androidx.compose.ui.geometry.Offset.Zero
    }

    override fun onSensorChanged(e: android.hardware.SensorEvent) {
        val r = rest ?: floatArrayOf(e.values[0], e.values[1]).also { rest = it }
        r[0] += (e.values[0] - r[0]) * REST_FOLLOW
        r[1] += (e.values[1] - r[1]) * REST_FOLLOW
        val x = ((e.values[0] - r[0]) / FULL_TILT).coerceIn(-1f, 1f)
        val y = ((e.values[1] - r[1]) / FULL_TILT).coerceIn(-1f, 1f)
        val old = tilt.value
        tilt.value = androidx.compose.ui.geometry.Offset(old.x + (x - old.x) * SMOOTH, old.y + (y - old.y) * SMOOTH)
    }

    override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) = Unit

    /** m/s² of sideways gravity that counts as fully tilted (~18°). */
    private const val FULL_TILT = 3f
    private const val REST_FOLLOW = 0.02f
    private const val SMOOTH = 0.25f
}

/**
 * Tilt as -1..1 on each axis. Read it inside `graphicsLayer {}` so a reading
 * only redraws, never recomposes. Level and silent in Battery Saver.
 */
@Composable
fun rememberTilt(): State<androidx.compose.ui.geometry.Offset> {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val on = LocalAmbientMotion.current
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(on, owner) {
        var held = false
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && on && !held) { TiltSensor.acquire(context); held = true }
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && held) { TiltSensor.release(context); held = false }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (held) TiltSensor.release(context)
        }
    }
    return TiltSensor.tilt
}

/** Leans toward the finger while pressed, like pushing on a real card. */
fun Modifier.pressTilt(
    interaction: androidx.compose.foundation.interaction.InteractionSource,
    maxDegrees: Float = 7f,
): Modifier = composed {
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    LaunchedEffect(interaction) {
        interaction.interactions.collect { i ->
            if (i is androidx.compose.foundation.interaction.PressInteraction.Press) {
                if (size.width > 0 && size.height > 0) {
                    val nx = (i.pressPosition.x / size.width - 0.5f) * 2f
                    val ny = (i.pressPosition.y / size.height - 0.5f) * 2f
                    launch { ry.animateTo(nx * maxDegrees, RcxMotion.press()) }
                    launch { rx.animateTo(-ny * maxDegrees, RcxMotion.press()) }
                }
            } else if (i is androidx.compose.foundation.interaction.PressInteraction.Release ||
                i is androidx.compose.foundation.interaction.PressInteraction.Cancel
            ) {
                launch { ry.animateTo(0f, RcxMotion.bouncy()) }
                launch { rx.animateTo(0f, RcxMotion.bouncy()) }
            }
        }
    }
    onSizeChanged { size = it }.graphicsLayer {
        rotationX = rx.value
        rotationY = ry.value
        cameraDistance = 12f * density
    }
}

/**
 * A card with two faces that turns over in 3D. The front stays composed so the
 * card keeps its size; the back fills the same space. [tilt] adds the phone's
 * lean on top, so the card also shifts with the hand.
 */
@Composable
fun FlipCard(
    flipped: Boolean,
    modifier: Modifier = Modifier,
    tilt: State<androidx.compose.ui.geometry.Offset>? = null,
    back: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
    front: @Composable () -> Unit,
) {
    val angle by animateFloatAsState(if (flipped) 180f else 0f, RcxMotion.smooth(), label = "flip")
    androidx.compose.foundation.layout.Box(
        modifier.graphicsLayer {
            rotationY = angle + (tilt?.value?.x ?: 0f) * TILT_DEGREES
            rotationX = -(tilt?.value?.y ?: 0f) * TILT_DEGREES
            cameraDistance = 14f * density
        },
    ) {
        androidx.compose.foundation.layout.Box(Modifier.graphicsLayer { alpha = if (angle <= 90f) 1f else 0f }) { front() }
        if (angle > 90f) {
            androidx.compose.foundation.layout.Box(
                Modifier.matchParentSize().graphicsLayer { rotationY = 180f },
                content = back,
            )
        }
    }
}

/** How far a card leans at full tilt. */
private const val TILT_DEGREES = 5f
