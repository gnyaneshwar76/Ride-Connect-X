package com.eshwar.rideconnectx.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eshwar.rideconnectx.core.util.rememberNotificationAccess
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxType

/**
 * Shown wherever notification access matters, and only while it is off.
 * Without it the cluster gets no turns, call lamps or message lamps, and the
 * Notifications screen used to just say "all caught up" (rider, 24 Sep).
 */
@Composable
fun NotificationAccessWarning(modifier: Modifier = Modifier) {
    val access = rememberNotificationAccess()
    if (access.granted) return
    val c = Rcx.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.amber.copy(alpha = 0.10f))
            .border(1.dp, c.amber.copy(alpha = 0.5f), shape)
            .clickable { access.openSettings() }
            .padding(16.dp),
    ) {
        Text("Notification access is off", style = RcxType.BodySmall, color = c.amber)
        Spacer(Modifier.height(4.dp))
        Text(
            "Turns, call and message alerts can't reach your scooter. Tap to turn it on.",
            style = RcxType.BodySmall,
            color = c.text,
        )
    }
}
