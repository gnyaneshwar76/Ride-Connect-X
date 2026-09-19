package com.eshwar.rideconnectx.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.eshwar.rideconnectx.data.nav.MapsNotificationListener

/**
 * Notification access — the permission that lets RideConnectX read Google Maps'
 * turn-by-turn notification.
 *
 * There is no runtime dialog for this one: Android only grants it from a
 * dedicated Settings page, which is why this is a screen-opener rather than a
 * request.
 */
class NotificationAccess(
    val granted: Boolean,
    val openSettings: () -> Unit,
    val refresh: () -> Unit,
)

@Composable
fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasNotificationAccess()) }

    val refresh = { granted = context.hasNotificationAccess() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refresh() }

    return NotificationAccess(
        granted = granted,
        openSettings = {
            runCatching {
                launcher.launch(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        },
        refresh = refresh,
    )
}

/**
 * Reads the system's list of enabled listeners rather than asking the service
 * whether it is running — the service is only constructed once access has been
 * granted, so asking it directly would always say no.
 */
fun Context.hasNotificationAccess(): Boolean {
    val component = ComponentName(this, MapsNotificationListener::class.java)
    val enabled = Settings.Secure.getString(
        contentResolver,
        "enabled_notification_listeners",
    ).orEmpty()

    return enabled.split(':').any {
        runCatching { ComponentName.unflattenFromString(it) }.getOrNull() == component
    }
}
