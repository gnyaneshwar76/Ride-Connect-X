package com.eshwar.rideconnectx.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eshwar.rideconnectx.data.local.db.RideBucket
import com.eshwar.rideconnectx.data.local.db.RideEntity
import com.eshwar.rideconnectx.data.repository.StatsPeriod
import com.eshwar.rideconnectx.R
import com.eshwar.rideconnectx.presentation.components.BackHeader
import com.eshwar.rideconnectx.presentation.components.RcxPhotoFill
import com.eshwar.rideconnectx.presentation.components.RcxSurface
import com.eshwar.rideconnectx.presentation.theme.LocalStyleMode
import com.eshwar.rideconnectx.presentation.theme.StyleMode
import com.eshwar.rideconnectx.presentation.theme.photoScrim
import com.eshwar.rideconnectx.presentation.components.RcxHeroBanner
import androidx.compose.runtime.CompositionLocalProvider
import com.eshwar.rideconnectx.presentation.theme.LocalRcxColors
import com.eshwar.rideconnectx.presentation.theme.RcxDarkColors
import androidx.compose.runtime.remember
import dev.chrisbanes.haze.HazeState
import com.eshwar.rideconnectx.presentation.theme.GlassTier
import com.eshwar.rideconnectx.presentation.theme.LocalHaze
import com.eshwar.rideconnectx.presentation.theme.hazeBackdrop
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType
import com.eshwar.rideconnectx.presentation.viewmodel.StatisticsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Screen 16 — Ride Statistics.
 *
 * Everything comes from Room, so it works with no network and no vehicle
 * connected. Until rides are recorded it shows a genuine empty state rather
 * than sample numbers.
 */
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    vm: StatisticsViewModel = hiltViewModel(),
) {
    val period by vm.period.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val rides by vm.rides.collectAsStateWithLifecycle()
    val buckets by vm.buckets.collectAsStateWithLifecycle()

    // This screen's background is a dark photograph in *both* app themes, so
    // its content is on a dark surface regardless of what the rider chose.
    //
    // Without this, Light + Glass was unreadable: light-theme text is near
    // black, the glass fill is a white translucency, and underneath it all sits
    // a dark mountain at dusk — dark text, on a pale film, on a dark photo.
    // Forcing the dark tokens here fixes the title, the labels, the glass fill
    // and the borders in one move, because every one of them keys off `isDark`.
    // The photograph is what the glass panels blur. Declared once here and
    // shared down, so every RcxSurface on this screen frosts with no plumbing.
    val hazeState = remember { HazeState() }

    CompositionLocalProvider(
        LocalRcxColors provides RcxDarkColors,
        LocalHaze provides hazeState,
    ) {
    val c = Rcx.colors

    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Full-bleed, behind everything — the photograph *is* the screen rather
        // than a 132dp band stuck on top of a list. The scrim over it is the
        // WCAG-checked floor from the Figma brief (§2C), not a guess: white
        // 13sp text has to clear 4.5:1 on the bright parts of the image.
        //
        // Photo and scrim are blurred together, so the panels sample the image
        // as the rider actually sees it rather than the raw, brighter original.
        Box(Modifier.matchParentSize().hazeBackdrop(hazeState)) {
            RcxPhotoFill(
                res = R.drawable.img_statistics_hero,
                modifier = Modifier.matchParentSize(),
            )
            Box(Modifier.matchParentSize().photoScrim())
        }

        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            BackHeader(title = "Ride Statistics", onBack = onBack)

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PeriodTabs(period, vm::setPeriod) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Summary("DISTANCE", "%.1f".format(totals.totalMeters / 1000f), "km", c.blue, Modifier.weight(1f))
                        Summary("TIME", formatDuration(totals.totalMillis), "", c.cyan, Modifier.weight(1f))
                        Summary("TRIPS", totals.trips.toString(), "", c.green, Modifier.weight(1f))
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Summary(
                            "AVG SPEED", totals.avgSpeed.roundToInt().toString(), "km/h",
                            c.amber, Modifier.weight(1f),
                        )
                        Summary(
                            "LONGEST",
                            "%.1f".format((rides.maxOfOrNull { it.distanceMeters } ?: 0) / 1000f),
                            "km", c.blue, Modifier.weight(1f),
                        )
                    }
                }

                if (buckets.isNotEmpty()) {
                    item { DistanceChart(buckets) }
                }

                item {
                    Text(
                        "RIDE HISTORY",
                        style = RcxType.MonoTiny.copy(fontSize = 10.sp),
                        // Always light, in both styles. This is bare text
                        // directly on the photograph — the photo is full-bleed
                        // whether or not the cards are glass, so keying this to
                        // the style mode left it unreadable in Flat.
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (rides.isEmpty()) {
                    item { EmptyState(period) }
                } else {
                    items(rides, key = { it.id }) { RideRow(it) }
                }
            }
        }
    }
    }
}

@Composable
private fun PeriodTabs(active: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    val c = Rcx.colors
    val onPhoto = LocalStyleMode.current == StyleMode.GLASS

    RcxSurface(corner = 14, tier = GlassTier.HEAVY) {
        Row(
            Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StatsPeriod.entries.forEach { p ->
                val on = p == active
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (on) c.blue else Color.Transparent)
                        .clickable { onSelect(p) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        p.label,
                        style = RcxType.Label.copy(fontSize = 12.sp),
                        // Unselected labels sit over the photograph, so they
                        // take a light colour rather than the muted one that
                        // was tuned against a flat card.
                        color = when {
                            on -> Color.White
                            onPhoto -> Color.White.copy(alpha = 0.75f)
                            else -> c.muted
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Summary(
    label: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val c = Rcx.colors
    // Everything on this screen now sits on a photograph, so the secondary text
    // takes a light tint instead of the muted grey that was tuned for a card.
    val onPhoto = LocalStyleMode.current == StyleMode.GLASS
    val secondary = if (onPhoto) Color.White.copy(alpha = 0.66f) else c.muted

    RcxSurface(modifier, tier = GlassTier.HEAVY) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = RcxType.MonoTiny.copy(fontSize = 9.sp), color = secondary)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = RcxType.Wordmark.copy(fontSize = 20.sp), color = accent)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(unit, style = RcxType.Mono.copy(fontSize = 10.sp), color = secondary)
                }
            }
        }
    }
}

/** Distance per bucket, scaled to the tallest bar. */
@Composable
private fun DistanceChart(buckets: List<RideBucket>) {
    val c = Rcx.colors
    val max = (buckets.maxOfOrNull { it.meters } ?: 1).coerceAtLeast(1)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text("DISTANCE", style = RcxType.MonoTiny.copy(fontSize = 9.sp), color = c.muted)
        Spacer(Modifier.height(12.dp))

        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val gap = size.width * 0.02f
            val barWidth = (size.width - gap * (buckets.size - 1)) / buckets.size
            buckets.forEachIndexed { i, bucket ->
                val h = (bucket.meters.toFloat() / max) * size.height
                drawRoundRect(
                    color = c.blue.copy(alpha = 0.75f),
                    topLeft = Offset(i * (barWidth + gap), size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            buckets.forEach {
                Text(it.label, style = RcxType.Mono.copy(fontSize = 9.sp), color = c.muted)
            }
        }
    }
}

@Composable
private fun RideRow(ride: RideEntity) {
    val c = Rcx.colors
    val date = SimpleDateFormat("d MMM · h:mm a", Locale.getDefault()).format(Date(ride.startedAt))

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                ride.title.ifBlank { "Ride" },
                style = RcxType.Label.copy(fontSize = 14.sp),
                color = c.text,
            )
            Text(date, style = RcxType.BodySmall.copy(fontSize = 12.sp), color = c.muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "%.1f km".format(ride.distanceMeters / 1000f),
                style = RcxType.Label.copy(fontSize = 14.sp),
                color = c.blue,
            )
            Text(
                "${formatDuration(ride.durationMillis)} · ${ride.avgSpeedKmh} km/h",
                style = RcxType.Mono.copy(fontSize = 11.sp),
                color = c.muted,
            )
        }
    }
}

@Composable
private fun EmptyState(period: StatsPeriod) {
    val c = Rcx.colors
    val onPhoto = LocalStyleMode.current == StyleMode.GLASS
    val secondary = if (onPhoto) Color.White.copy(alpha = 0.66f) else c.muted

    RcxSurface(Modifier.fillMaxWidth(), tier = GlassTier.HEAVY) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "No rides yet",
                style = RcxType.Label.copy(fontSize = 15.sp),
                color = if (onPhoto) Color.White else c.text,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Rides recorded while connected to your vehicle will appear here for ${period.label.lowercase()}.",
                style = RcxType.BodySmall.copy(fontSize = 12.sp),
                color = secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** "1h 24m" / "42m" / "38s". */
private fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        totalMinutes > 0 -> "${minutes}m"
        else -> "${millis / 1000}s"
    }
}
