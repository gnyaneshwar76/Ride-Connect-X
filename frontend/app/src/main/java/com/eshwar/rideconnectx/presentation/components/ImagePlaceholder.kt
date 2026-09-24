package com.eshwar.rideconnectx.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType

/**
 * A labelled hole where a photograph will go.
 *
 * The app's images are being generated from `docs/design/Image-Prompts-Gemini.md`.
 * Until a file lands in `res/drawable-nodpi/`, its slot shows this box with the
 * exact file name it is waiting for — so the layout is already correct, and
 * both of us can see at a glance which images are still missing and where.
 *
 * Swapping one in is a one-line change at the call site:
 *
 *     RcxImagePlaceholder("img_welcome_hero.jpg", 4f / 5f)
 *     →  Image(painterResource(R.drawable.img_welcome_hero), …)
 *
 * @param fileName the name from the prompts document, shown in the box.
 * @param ratio width ÷ height, matching the ratio the prompt asks for.
 */
@Composable
fun RcxImagePlaceholder(
    fileName: String,
    ratio: Float,
    modifier: Modifier = Modifier,
    caption: String = "",
) {
    val c = Rcx.colors
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(shape)
            .background(c.card2),
        contentAlignment = Alignment.Center,
    ) {
        // Dashed outline, so it never reads as a finished surface.
        Canvas(Modifier.fillMaxWidth().aspectRatio(ratio)) {
            drawRoundRect(
                color = c.blue.copy(alpha = 0.35f),
                cornerRadius = CornerRadius(20.dp.toPx()),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(12.dp.toPx(), 9.dp.toPx())
                    ),
                ),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Icon(
                Icons.Filled.Image,
                contentDescription = null,
                tint = c.blue.copy(alpha = 0.5f),
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                fileName,
                style = RcxType.Mono.copy(fontSize = 11.sp),
                color = c.blue,
                textAlign = TextAlign.Center,
            )
            if (caption.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    caption,
                    style = RcxType.BodySmall.copy(fontSize = 11.sp),
                    color = c.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
