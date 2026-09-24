package com.eshwar.rideconnectx.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eshwar.rideconnectx.domain.model.ServiceStatus
import com.eshwar.rideconnectx.R
import com.eshwar.rideconnectx.presentation.components.RcxPhotoFill
import com.eshwar.rideconnectx.presentation.components.RcxSurface
import com.eshwar.rideconnectx.presentation.components.RiderAvatar
import com.eshwar.rideconnectx.presentation.components.VehicleArtwork
import com.eshwar.rideconnectx.presentation.components.VehicleStage
import com.eshwar.rideconnectx.presentation.theme.LocalDistanceUnit
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType
import com.eshwar.rideconnectx.presentation.viewmodel.DashboardViewModel
import java.util.Calendar

/**
 * Screen 13 — Dashboard.
 *
 * The app's hub. Reads live connection state and telemetry from
 * [DashboardViewModel], and the rider's name and chosen vehicle from the
 * setup flow. Values shown as "—" simply mean the cluster hasn't reported
 * that field yet; nothing here is faked.
 */
@Composable
fun DashboardScreen(
    onNavigate: () -> Unit,
    onPairVehicle: () -> Unit,
    onStatistics: () -> Unit,
    onNotifications: () -> Unit,
    onService: () -> Unit,
    onSafety: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    vm: DashboardViewModel = hiltViewModel(),
) {
    val c = Rcx.colors
    val state by vm.uiState.collectAsStateWithLifecycle()
    val serviceStatus by vm.serviceStatus.collectAsStateWithLifecycle()
    val distanceUnit by vm.distanceUnit.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (c.isDark) listOf(Color(0xFF060C18), Color(0xFF070D1B))
                    else listOf(Color(0xFFE4EEFF), Color(0xFFEEF2FF))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // The cap has to come *before* the width is filled. After a
                // fillMaxSize() the minimum width is already the whole screen,
                // so widthIn(max) cannot shrink it and the column ran the full
                // width of a tablet. Invisible on a phone, where the screen is
                // narrower than 600dp to begin with.
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Greeting + connection pill ──────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The greeting is the way into Profile — the rider's own name
                // and face are the most natural thing to tap for their account.
                Row(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onProfile)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RiderAvatar(name = state.nickname.ifBlank { state.riderName }, size = 40.dp)
                    Column {
                        Text(greeting(), style = RcxType.BodySmall.copy(fontSize = 12.sp), color = c.muted)
                        Text(
                            state.nickname.ifBlank { state.riderName },
                            style = RcxType.Wordmark.copy(fontSize = 18.sp),
                            color = c.text,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Only the bell up here. Two 34dp icon buttons plus the
                    // connection pill left the gear squeezed against the screen
                    // edge and too small to hit; Settings is not something the
                    // rider reaches for mid-glance, so it moved down into Quick
                    // Actions where there is room for a proper target.
                    ConnectionPill(connected = state.isConnected)
                    HeaderIconButton(Icons.Default.Notifications, stringResource(R.string.dash_notifications), onNotifications)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Vehicle hero ────────────────────────────────────────
            val vehicle = state.vehicle
            if (vehicle == null) {
                NoVehicleCard(onPairVehicle)
            } else {
                val accent = vehicle.accent
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accent.copy(alpha = 0.10f), c.card)
                            )
                        )
                        .border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(22.dp))
                        .padding(16.dp),
                ) {
                    // The vehicle is the subject of this screen, so it gets the
                    // width of the card and the name sits under it. It used to
                    // be a 130dp thumbnail in the right-hand third of a row,
                    // with "Connected Vehicle" — a label saying nothing the
                    // rider does not already know — taking the larger half.
                    VehicleStage(
                        vehicle = vehicle,
                        colorway = state.vehicleColor,
                        accent = accent,
                        modifier = Modifier.clip(RoundedCornerShape(18.dp)),
                        height = 200.dp,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                vehicle.name,
                                style = RcxType.Wordmark.copy(fontSize = 20.sp),
                                color = c.text,
                            )
                            Text(
                                state.vehicleColor?.name ?: vehicle.subtitle,
                                style = RcxType.BodySmall.copy(fontSize = 12.sp),
                                color = c.muted,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Live telemetry ──────────────────────────────────────
            // No battery here: the phone's battery is something the *cluster*
            // displays, and only one Suzuki model is electric — a battery
            // reading on this dashboard would be meaningless for the rest.
            // Speed is not in the scooter's telemetry frame, so it is not shown
            // at all rather than sitting at a permanent "—".
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Telemetry(
                    stringResource(R.string.dash_odo),
                    state.odometer?.toString() ?: "—",
                    "km",
                    c.blue,
                    Modifier.weight(1f),
                )
                FuelGauge(state.fuelSegments, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Telemetry(
                    stringResource(R.string.dash_trip_a),
                    state.tripAKm?.let { "%.1f".format(it) } ?: "—",
                    "km",
                    c.cyan,
                    Modifier.weight(1f),
                )
                Telemetry(
                    stringResource(R.string.dash_trip_b),
                    state.tripBKm?.let { "%.1f".format(it) } ?: "—",
                    "km",
                    c.green,
                    Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Quick actions ───────────────────────────────────────
            Text(stringResource(R.string.dash_quick_actions), style = RcxType.MonoTiny.copy(fontSize = 10.sp), color = c.muted)
            Spacer(Modifier.height(10.dp))

            // The two things a rider actually opens this app to do get the
            // space. Four equal rows made the list compete with the vehicle for
            // attention and told the eye nothing about what mattered.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryAction(
                    icon = Icons.Default.Navigation,
                    title = stringResource(R.string.dash_navigate),
                    subtitle = stringResource(R.string.dash_navigate_sub),
                    accent = c.blue,
                    onClick = onNavigate,
                    modifier = Modifier.weight(1f),
                )
                PrimaryAction(
                    icon = Icons.Default.Bluetooth,
                    title = if (state.isConnected) stringResource(R.string.dash_connected)
                        else stringResource(R.string.dash_pair),
                    subtitle = if (state.isConnected) stringResource(R.string.dash_connected_sub)
                        else stringResource(R.string.dash_pair_sub),
                    accent = if (state.isConnected) c.green else c.cyan,
                    onClick = onPairVehicle,
                    modifier = Modifier.weight(1f),
                    showLiveDot = state.isConnected,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Everything else, small — reachable without shouting.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SecondaryAction(Icons.Default.BarChart, stringResource(R.string.dash_stats), c.amber, onStatistics, Modifier.weight(1f))
                SecondaryAction(Icons.Default.Shield, stringResource(R.string.dash_safety), c.red, onSafety, Modifier.weight(1f))
                SecondaryAction(Icons.Default.Build, stringResource(R.string.dash_service), c.green, onService, Modifier.weight(1f))
                SecondaryAction(Icons.Default.Person, stringResource(R.string.dash_profile), c.cyan, onProfile, Modifier.weight(1f))
                SecondaryAction(Icons.Default.Settings, stringResource(R.string.dash_settings), c.muted, onSettings, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // ── Service reminder ────────────────────────────────────
            CompositionLocalProvider(LocalDistanceUnit provides distanceUnit) {
                ServiceReminderCard(status = serviceStatus, onClick = onService)
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * A large quick action — Navigate and Pair.
 *
 * Presses scale the tile down slightly and release springs it back. It is the
 * cheapest thing that makes a surface feel like it responds to a finger rather
 * than merely registering a tap.
 */
@Composable
private fun PrimaryAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showLiveDot: Boolean = false,
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(22.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "primaryActionScale",
    )

    Column(
        modifier
            .scale(scale)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.13f), accent.copy(alpha = 0.03f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.24f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(20.dp), tint = accent)
            }
            Spacer(Modifier.weight(1f))
            if (showLiveDot) {
                // A live link is worth one dot, not a paragraph.
                Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(title, style = RcxType.Section.copy(fontSize = 17.sp), color = c.text)
        Spacer(Modifier.height(3.dp))
        Text(
            subtitle,
            style = RcxType.BodySmall.copy(fontSize = 11.sp),
            color = c.muted,
        )
    }
}

/** A small quick action — a square icon tile with a one-word label. */
@Composable
private fun SecondaryAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(18.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1000f),
        label = "secondaryActionScale",
    )

    // The press state is handed to the surface as well as the scale, so glass
    // darkens under the finger the way the export specifies rather than only
    // shrinking.
    RcxSurface(
        modifier = modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        pressed = pressed,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, Modifier.size(17.dp), tint = accent)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = RcxType.BodySmall.copy(fontSize = 11.sp),
                color = c.muted,
            )
        }
    }
}

/** Square icon button in the dashboard header — bell, gear. */
@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val c = Rcx.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(16.dp), tint = c.muted)
    }
}

/**
 * Service Reminder card — next service, remaining distance and a badge when
 * it needs attention. Reads the same schedule as Screen 18; tapping opens it.
 */
@Composable
private fun ServiceReminderCard(status: ServiceStatus, onClick: () -> Unit) {
    val c = Rcx.colors
    val unit = LocalDistanceUnit.current
    val shape = RoundedCornerShape(18.dp)
    val accent = when {
        status.isOverdue -> c.red
        status.isDueSoon -> c.amber
        else -> c.green
    }
    val needsAttention = status.isOverdue || status.isDueSoon

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.card)
            .border(1.dp, if (needsAttention) accent.copy(alpha = 0.27f) else c.border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.094f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Build, null, Modifier.size(18.dp), tint = accent)
        }

        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.dash_next_service), style = RcxType.Label, color = c.text)
                // Reminder badge — only when there is something to react to.
                if (needsAttention) {
                    Text(
                        if (status.isOverdue) stringResource(R.string.dash_overdue)
                        else stringResource(R.string.dash_due_soon),
                        style = RcxType.MonoTiny.copy(fontSize = 8.sp),
                        color = accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.11f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            // Distance first, because that is what the design asks for; days
            // are the fallback when the odometer has never been seen.
            val km = status.remainingKm
            val days = status.remainingDays
            Text(
                when {
                    !status.hasBaseline -> stringResource(R.string.dash_no_baseline)
                    km != null && km > 0 -> "${unit.format(km)} remaining"
                    km != null -> "${unit.format(-km)} overdue"
                    days != null && days > 0 -> "$days days remaining"
                    else -> stringResource(R.string.dash_due_now)
                },
                style = RcxType.BodySmall.copy(fontSize = 12.sp),
                color = c.muted,
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            Modifier.size(16.dp), tint = c.muted.copy(alpha = 0.44f),
        )
    }
}

@Composable
private fun greeting(): String =
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> stringResource(R.string.dash_greeting_morning)
        in 12..16 -> stringResource(R.string.dash_greeting_afternoon)
        else -> stringResource(R.string.dash_greeting_evening)
    }

@Composable
private fun ConnectionPill(connected: Boolean) {
    val c = Rcx.colors
    val tint = if (connected) c.green else c.muted
    Row(
        Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.07f))
            .border(1.dp, tint.copy(alpha = 0.16f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tint))
        Text(
            if (connected) stringResource(R.string.dash_connected)
            else stringResource(R.string.dash_offline),
            style = RcxType.Mono.copy(fontSize = 10.sp),
            color = tint,
        )
    }
}

@Composable
private fun NoVehicleCard(onPair: () -> Unit) {
    val c = Rcx.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(22.dp))
            .clickable(onClick = onPair)
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.dash_no_vehicle), style = RcxType.Label.copy(fontSize = 15.sp), color = c.text)
        Spacer(Modifier.height(4.dp))
        Text(
            // Names only what the scooter actually sends. Speed is not in the
            // telemetry frame, and phone battery belongs on the cluster.
            stringResource(R.string.dash_no_vehicle_body),
            style = RcxType.BodySmall.copy(fontSize = 12.sp),
            color = c.muted,
        )
    }
}

@Composable
private fun Telemetry(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val c = Rcx.colors
    RcxSurface(modifier) {
    Column(Modifier.padding(14.dp)) {
        Text(label, style = RcxType.MonoTiny.copy(fontSize = 9.sp), color = c.muted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = RcxType.Wordmark.copy(fontSize = 22.sp), color = accent)
            Spacer(Modifier.size(3.dp))
            Text(unit, style = RcxType.Mono.copy(fontSize = 10.sp), color = c.muted)
        }
    }
    }
}

/**
 * Fuel drawn the way the cluster draws it — as bars, not as a number.
 *
 * The scooter reports fuel as a single ASCII digit `'0'`..`'5'` at byte 24 of
 * the telemetry frame (see the knowledge base), which is a five-segment E–F
 * bar and nothing finer. Showing that as "3 / 5" was technically right and read
 * as a score; the tank on the handlebars is a row of bars, so this is a row of
 * bars. Five is what the vehicle transmits, so five is what can honestly be
 * drawn — the count is the hardware's, not a design choice.
 *
 * The lowest bar turns red on the last segment, matching the cluster's own
 * reserve warning.
 */
@Composable
private fun FuelGauge(segments: Int?, modifier: Modifier = Modifier) {
    val c = Rcx.colors
    val total = SuzukiFuelSegments
    val filled = segments?.coerceIn(0, total)
    val low = filled != null && filled <= 1

    RcxSurface(modifier) {
    Column(Modifier.padding(14.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.dash_fuel), style = RcxType.MonoTiny.copy(fontSize = 9.sp), color = c.muted)
            Text(
                when {
                    filled == null -> "—"
                    low -> stringResource(R.string.dash_fuel_reserve)
                    else -> "$filled/$total"
                },
                style = RcxType.MonoTiny.copy(fontSize = 9.sp),
                color = if (low) c.red else c.muted,
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(total) { index ->
                val on = filled != null && index < filled
                Box(
                    Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                !on -> c.muted.copy(alpha = 0.13f)
                                low -> c.red
                                else -> c.amber
                            }
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.dash_fuel_empty), style = RcxType.MonoTiny.copy(fontSize = 9.sp), color = c.muted)
            Text(stringResource(R.string.dash_fuel_full), style = RcxType.MonoTiny.copy(fontSize = 9.sp), color = c.muted)
        }
    }
    }
}

/** Byte 24 of the telemetry frame is `'0'`..`'5'`. Hardware-confirmed. */
private const val SuzukiFuelSegments = 5

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit,
) {
    val c = Rcx.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(accent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(19.dp), tint = accent) }

        Column(Modifier.weight(1f)) {
            Text(title, style = RcxType.Label.copy(fontSize = 14.sp), color = c.text)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = RcxType.BodySmall.copy(fontSize = 12.sp), color = c.muted)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            Modifier.size(18.dp), tint = c.muted,
        )
    }
}
