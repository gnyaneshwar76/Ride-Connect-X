package com.eshwar.rideconnectx.presentation.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eshwar.rideconnectx.R
import com.eshwar.rideconnectx.data.local.db.NotificationEntity
import com.eshwar.rideconnectx.data.local.db.NotificationKind
import com.eshwar.rideconnectx.presentation.components.BackHeader
import com.eshwar.rideconnectx.presentation.components.NotificationAccessWarning
import com.eshwar.rideconnectx.presentation.components.RcxPhoto
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxColors
import com.eshwar.rideconnectx.presentation.theme.RcxType
import com.eshwar.rideconnectx.presentation.viewmodel.NotificationsViewModel
import java.util.concurrent.TimeUnit

/**
 * Screen 17 — Notifications.
 *
 * Backed by Room, so it works offline and in Guest Mode. Tapping a row marks it
 * read and jumps to the feature it refers to; [onOpen] receives the kind so the
 * nav graph — not this screen — decides the destination.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpen: (NotificationKind) -> Unit,
    vm: NotificationsViewModel = hiltViewModel(),
) {
    val c = Rcx.colors
    val items by vm.notifications.collectAsStateWithLifecycle()
    val unread by vm.unreadCount.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(
            Modifier
                .fillMaxHeight()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            BackHeader(
                title = stringResource(R.string.notifs_title),
                onBack = onBack,
                right = {
                    // Only offered when it would actually do something.
                    if (unread > 0) {
                        Text(
                            stringResource(R.string.notifs_mark_all),
                            style = RcxType.BodySmall.copy(fontSize = 11.sp),
                            color = c.blue,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { vm.markAllRead() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                },
            )

            NotificationAccessWarning(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp))

            if (items.isEmpty()) {
                EmptyNotifications(Modifier.padding(horizontal = 20.dp))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.id }) { n ->
                        NotificationRow(
                            notification = n,
                            onClick = {
                                vm.markRead(n.id)
                                onOpen(n.kind)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationEntity,
    onClick: () -> Unit,
) {
    val c = Rcx.colors
    val accent = notification.kind.accent(c)
    val shape = RoundedCornerShape(18.dp)
    val unread = notification.unread

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            // Unread rows carry a tint of their own accent; read rows go flat.
            .background(if (unread) accent.copy(alpha = if (c.isDark) 0.047f else 0.031f) else c.card)
            .border(1.dp, if (unread) accent.copy(alpha = 0.157f) else c.border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.094f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                notification.kind.icon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    notification.title,
                    style = RcxType.Label.copy(fontSize = 14.sp),
                    color = c.text,
                )
                if (unread) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c.blue))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                notification.body,
                style = RcxType.BodySmall.copy(fontSize = 12.sp),
                color = c.muted,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                relativeTime(notification.createdAt),
                style = RcxType.MonoTiny.copy(fontSize = 10.sp),
                color = c.muted.copy(alpha = 0.44f),
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = c.muted.copy(alpha = 0.31f),
            modifier = Modifier.size(14.dp).padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptyNotifications(modifier: Modifier = Modifier) {
    val c = Rcx.colors
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(20.dp))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The photograph replaces the icon tile here rather than sitting above
        // it: an empty state is the one place with room to spare, and a still
        // life reads as "nothing to do" far better than a bell glyph does.
        RcxPhoto(
            res = R.drawable.img_empty_notifications,
            ratio = 1f,
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(24.dp)),
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.notifs_empty), style = RcxType.Label.copy(fontSize = 15.sp), color = c.text)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.notifs_empty_body),
            style = RcxType.BodySmall.copy(fontSize = 12.sp),
            color = c.muted,
            textAlign = TextAlign.Center,
        )
    }
}

private fun NotificationKind.icon(): ImageVector = when (this) {
    NotificationKind.SERVICE -> Icons.Filled.Build
    NotificationKind.VEHICLE -> Icons.Filled.TwoWheeler
    NotificationKind.RIDE -> Icons.AutoMirrored.Filled.DirectionsBike
    NotificationKind.SAFETY -> Icons.Filled.Shield
    NotificationKind.SYSTEM -> Icons.Filled.Info
}

private fun NotificationKind.accent(c: RcxColors): Color = when (this) {
    NotificationKind.SERVICE -> c.amber
    NotificationKind.VEHICLE -> c.blue
    NotificationKind.RIDE -> c.cyan
    NotificationKind.SAFETY -> c.red
    NotificationKind.SYSTEM -> c.green
}

/** "Just now" / "12m ago" / "3h ago" / "5d ago". */
private fun relativeTime(millis: Long): String {
    val delta = (System.currentTimeMillis() - millis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
    val hours = TimeUnit.MILLISECONDS.toHours(delta)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
