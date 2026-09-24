package com.eshwar.rideconnectx.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eshwar.rideconnectx.presentation.components.PrimaryButton
import com.eshwar.rideconnectx.presentation.components.RiderAvatar
import com.eshwar.rideconnectx.presentation.components.VehicleArtwork
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType
import com.eshwar.rideconnectx.presentation.viewmodel.ProfileFoundViewModel

/**
 * Shown when signing in turns up a profile already attached to the account.
 *
 * Reinstalling used to drop the rider back into Create Profile as though they
 * were new, which defeats the point of signing in. Restoring silently would be
 * the opposite mistake — so the details are shown and the rider chooses.
 */
@Composable
fun ProfileFoundScreen(
    onContinue: () -> Unit,
    onStartFresh: () -> Unit,
    vm: ProfileFoundViewModel = hiltViewModel(),
) {
    val c = Rcx.colors
    val profile by vm.profile.collectAsStateWithLifecycle()
    val accent = profile.color?.primary ?: c.blue

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (c.isDark) listOf(Color(0xFF070D1B), Color(0xFF0C1428))
                    else listOf(Color(0xFFE4EEFF), Color(0xFFEEF2FF))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = c.green,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Account found",
                    style = RcxType.Wordmark.copy(fontSize = 22.sp),
                    color = c.text,
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "We restored the profile saved to this Google account. " +
                    "Continue with it, or start over.",
                style = RcxType.Body.copy(fontSize = 14.sp),
                color = c.muted,
            )

            Spacer(Modifier.height(24.dp))

            // ── The saved profile ────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(c.card)
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // The account's own picture, restored alongside the rest of
                    // the profile. A generic person glyph on the screen that
                    // asks "is this you?" was answering its own question badly.
                    RiderAvatar(name = profile.riderName, size = 52.dp, accent = accent)

                    Spacer(Modifier.size(14.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            profile.riderName.ifBlank { "Rider" },
                            style = RcxType.Wordmark.copy(fontSize = 18.sp),
                            color = c.text,
                        )
                        if (profile.location.isNotBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = c.muted,
                                    modifier = Modifier.size(13.dp),
                                )
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    profile.location,
                                    style = RcxType.Body.copy(fontSize = 13.sp),
                                    color = c.muted,
                                )
                            }
                        }
                    }
                }

                // Only drawn when a vehicle was actually chosen — an empty
                // placeholder here would look like something failed to load.
                profile.vehicle?.let { vehicle ->
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "YOUR VEHICLE",
                                style = RcxType.Mono.copy(fontSize = 10.sp),
                                color = c.muted,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                vehicle.name,
                                style = RcxType.Wordmark.copy(fontSize = 17.sp),
                                color = c.text,
                            )
                            profile.color?.let {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    it.name,
                                    style = RcxType.Body.copy(fontSize = 12.sp),
                                    color = c.muted,
                                )
                            }
                        }
                        VehicleArtwork(
                            vehicle = vehicle,
                            body = accent,
                            outline = accent,
                            modifier = Modifier.size(width = 112.dp, height = 58.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                label = "Continue as ${profile.riderName.ifBlank { "this rider" }}",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))

            TextButton(
                onClick = { vm.startFresh(onStartFresh) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Set up a new profile instead",
                    style = RcxType.Body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    color = c.muted,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "Starting over replaces the saved profile on this account " +
                    "once you finish setting it up.",
                style = RcxType.Body.copy(fontSize = 12.sp),
                color = c.muted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
