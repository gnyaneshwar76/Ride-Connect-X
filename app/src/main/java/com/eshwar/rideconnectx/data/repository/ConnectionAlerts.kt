package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.data.local.AppSettingsStore
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
 * The switch was saved but nothing read it (AUD-2).
 */
@Singleton
class ConnectionAlerts @Inject constructor(
    private val ble: BleRepository,
    private val settings: AppSettingsStore,
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
    }
}
