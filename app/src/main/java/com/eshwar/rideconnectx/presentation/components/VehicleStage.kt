package com.eshwar.rideconnectx.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eshwar.rideconnectx.domain.model.Vehicle
import com.eshwar.rideconnectx.domain.model.VehicleColor
import com.eshwar.rideconnectx.presentation.theme.Rcx

/**
 * The vehicle standing in a pool of light.
 *
 * The light is **drawn**, not photographed. It used to be the
 * `img_dashboard_hero_plate` photograph with the vehicle laid over it, and the
 * two never lined up: measured, that photograph's bright pool sits at 64% of
 * its height while a centred vehicle sits at 50%, so the beam always landed
 * behind and below the machine instead of under it. Cropping the photo to fix
 * that only moves the problem to the next card size.
 *
 * Drawing it means the ellipse is positioned from the same number that places
 * the wheels — [GROUND] — so they cannot drift apart on any screen, at any card
 * width, for any vehicle.
 *
 * The vehicle is bottom-aligned rather than centred because the artwork is
 * trimmed to its subject: the bottom edge of the image *is* the tyre contact
 * patch, which is exactly where the light should be brightest.
 */
private const val GROUND = 0.80f

@Composable
fun VehicleStage(
    vehicle: Vehicle,
    colorway: VehicleColor?,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp,
) {
    val c = Rcx.colors
    val paint = colorway?.primary ?: accent

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val vehicleHeight = maxHeight * 0.62f

        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * GROUND

            // Wide, shallow ellipse — a beam hitting a floor, not a ball of
            // light. Drawn as a radial brush scaled into an oval by giving the
            // rect a much smaller height than width.
            val rx = size.width * 0.46f
            val ry = size.height * 0.20f

            drawOval(
                brush = Brush.radialGradient(
                    0f to paint.copy(alpha = 0.55f),
                    0.45f to paint.copy(alpha = 0.22f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = rx,
                ),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2, ry * 2),
            )

            // A tighter, brighter core right under the wheels.
            val coreX = size.width * 0.26f
            val coreY = size.height * 0.075f
            drawOval(
                brush = Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.20f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy),
                    radius = coreX,
                ),
                topLeft = Offset(cx - coreX, cy - coreY),
                size = Size(coreX * 2, coreY * 2),
            )

            // Haze above the floor, so the vehicle sits in air rather than on a
            // sticker. Fades out well before the top of the card.
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to paint.copy(alpha = 0.05f),
                    1f to paint.copy(alpha = 0.13f),
                )
            )
        }

        VehicleArtwork(
            vehicle = vehicle,
            body = paint,
            outline = accent,
            colorway = colorway,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // Bottom of the artwork lands on GROUND, where the light is.
                .padding(bottom = height * (1f - GROUND))
                .fillMaxWidth(0.88f)
                .height(vehicleHeight),
        )

        // Keeps the drawn light from ending on a hard edge against the card.
        Box(
            Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to c.card.copy(alpha = 0.35f),
                        0.25f to Color.Transparent,
                        0.9f to Color.Transparent,
                        1f to c.card.copy(alpha = 0.55f),
                    )
                )
            }
        }
    }
}
