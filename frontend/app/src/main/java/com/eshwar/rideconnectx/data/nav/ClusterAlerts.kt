package com.eshwar.rideconnectx.data.nav

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether the cluster should be showing a message indicator or a missed-call
 * indicator right now.
 *
 * The notification listener sets these; the 0x33 heartbeat reads them. They are
 * deliberately just two booleans — the cluster has two lamps, not a message
 * queue, and the official app treats them the same way (`i0` and `j0` on
 * `HomeScreenActivity`).
 */
object ClusterAlerts {

    /**
     * How long a lamp stays lit after the notification arrives.
     *
     * The lamp used to be cleared the moment a single heartbeat had carried it,
     * which at a 5s heartbeat meant it could be visible for well under a second
     * - effectively invisible to someone riding. The official app holds its flag
     * for three heartbeats (~15s); the rider asked for longer, so 30s.
     *
     * Expiry is by wall clock rather than by counting heartbeats, so a dropped
     * or delayed packet cannot leave a lamp latched on forever.
     */
    private const val HOLD_MS = 30_000L

    /**
     * One alert, with who it is from.
     *
     * The lamp alone is not enough. The rider's point, 18 August: a lamp with no
     * name means they pull the phone out of their pocket mid-road to see who it
     * is, which is the exact hazard the cluster display exists to prevent.
     */
    data class Alert(
        val appIdentifier: Char,
        val title: String,
        val text: String,
        val isCall: Boolean,
    )

    /**
     * Emitted the instant a notification arrives, so the link can push it
     * immediately instead of waiting for the next 5s heartbeat tick.
     */
    private val _alerts = MutableSharedFlow<Alert>(
        replay = 1,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val alerts: SharedFlow<Alert> = _alerts.asSharedFlow()

    private val _notificationPending = MutableStateFlow(false)
    val notificationPending: StateFlow<Boolean> = _notificationPending.asStateFlow()

    private val _missedCallPending = MutableStateFlow(false)
    val missedCallPending: StateFlow<Boolean> = _missedCallPending.asStateFlow()

    @Volatile private var notificationUntil = 0L
    @Volatile private var missedCallUntil = 0L

    fun onNotification(appIdentifier: Char = 'N', title: String = "", text: String = "") {
        notificationUntil = System.currentTimeMillis() + HOLD_MS
        _notificationPending.value = true
        _alerts.tryEmit(Alert(appIdentifier, title, text, isCall = false))
    }

    fun onMissedCall(title: String = "", text: String = "") {
        missedCallUntil = System.currentTimeMillis() + HOLD_MS
        _missedCallPending.value = true
        _alerts.tryEmit(Alert('C', title, text, isCall = true))
    }

    /**
     * Drops any lamp whose hold has elapsed.
     *
     * Called by the heartbeat before it reads the flags, so the cluster keeps
     * being told 'alert' for the whole [HOLD_MS] and then stops.
     */
    fun expireStale() {
        val now = System.currentTimeMillis()
        if (_notificationPending.value && now >= notificationUntil) {
            _notificationPending.value = false
        }
        if (_missedCallPending.value && now >= missedCallUntil) {
            _missedCallPending.value = false
        }
    }

    /** Force a lamp out early - the rider opened the message, say. */
    fun clearNotification() {
        notificationUntil = 0L
        _notificationPending.value = false
    }

    fun clearMissedCall() {
        missedCallUntil = 0L
        _missedCallPending.value = false
    }

    /** A fresh connection starts with a clean slate. */
    fun reset() {
        notificationUntil = 0L
        missedCallUntil = 0L
        _notificationPending.value = false
        _missedCallPending.value = false
    }
}
