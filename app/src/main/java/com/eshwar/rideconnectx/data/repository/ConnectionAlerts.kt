package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.data.local.AppSettingsStore
import com.eshwar.rideconnectx.data.local.SafetyPreferencesStore
import com.eshwar.rideconnectx.data.local.db.NotificationKind
import com.eshwar.rideconnectx.domain.model.ConnectionAlert
import com.eshwar.rideconnectx.domain.model.ConnectionAlertRule
import com.eshwar.rideconnectx.domain.repository.BleRepository
import com.eshwar.rideconnectx.domain.repository.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Settings → "Connection alerts": tell me when the vehicle connects or drops.
 * The switch was saved but nothing read it (AUD-2). Also Safety's helmet
 * reminder, which fires on the same event.
 */
@Singleton
class ConnectionAlerts @Inject constructor(
    private val ble: BleRepository,
    private val settings: AppSettingsStore,
    private val safety: SafetyPreferencesStore,
    private val notifications: NotificationRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        appScope.launch {
            var previous: ConnectionState = ble.connectionState.value
            ble.connectionState.collect { current ->
                val alert = ConnectionAlertRule.alertFor(previous, current)
                previous = current
                // Safety → Helmet reminder: "when a ride starts". The scooter
                // connecting is the ignition coming on. The switch was saved but
                // never read (AUD-8).
                if (alert == ConnectionAlert.CONNECTED && safety.helmetReminder.first()) {
                    notifications.postToPhone(
                        HELMET_CHANNEL, "Helmet reminder", HELMET_ID,
                        "Helmet on?", "Your scooter is on. Strap your helmet before you ride.",
                    )
                }
                if (alert == null || !settings.settings.first().connectionAlerts) return@collect
                val (title, body) = when (alert) {
                    ConnectionAlert.CONNECTED ->
                        "Vehicle connected" to "${(current as ConnectionState.Connected).name.ifBlank { "Your scooter" }} is linked."
                    ConnectionAlert.LOST ->
                        "Vehicle disconnected" to "The link to your scooter dropped. RideConnectX will keep trying to reconnect."
                }
                notifications.notify(NotificationKind.VEHICLE, title, body)
                notifications.postToPhone(CHANNEL, "Connection alerts", NOTIFICATION_ID, title, body)
            }
        }
    }

    private companion object {
        const val CHANNEL = "connection_alerts"
        const val NOTIFICATION_ID = 3
        const val HELMET_CHANNEL = "helmet_reminder"
        const val HELMET_ID = 4
    }
}
