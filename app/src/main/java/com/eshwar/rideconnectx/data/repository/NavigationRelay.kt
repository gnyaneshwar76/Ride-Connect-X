package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.domain.ProtocolEngine
import com.eshwar.rideconnectx.domain.model.NavManeuver
import com.eshwar.rideconnectx.domain.model.NavState
import com.eshwar.rideconnectx.domain.repository.BleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the live navigation session and mirrors it to the vehicle cluster.
 *
 * This is the seam between Google Maps and the bike. A NotificationListener-
 * Service will call [onManeuver] for each instruction Maps posts; everything
 * downstream — the Navigation screen and the BLE packets — reads from here, so
 * that service can be added later without touching the UI.
 */
@Singleton
class NavigationRelay @Inject constructor(
    private val bleRepository: BleRepository,
    private val rideLog: com.eshwar.rideconnectx.data.nav.RideLog,
    private val appSettings: com.eshwar.rideconnectx.data.local.AppSettingsStore,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val _state = MutableStateFlow<NavState>(NavState.Inactive)
    val state: StateFlow<NavState> = _state.asStateFlow()

    private companion object {
        const val TAG = "RCX-Nav"
        /** Rider's ask: clear 20-30 s after reaching the destination. */
        const val ARRIVAL_CLEAR_MS = 25_000L
        /** Maps silent this long mid-route = frozen. Tunable; see [armWatchdog]. */
        const val STALE_CLEAR_MS = 90_000L
        const val ARRIVAL_METRES = 50
        val DISTANCE = Regex("""(\d+(?:[.,]\d+)?)\s*(km|m|mi|ft|yd)""", RegexOption.IGNORE_CASE)
    }

    private val _clusterLinked = MutableStateFlow(false)

    /** True while maneuvers are actually reaching the cluster over BLE. */
    val clusterLinked: StateFlow<Boolean> = _clusterLinked.asStateFlow()

    /** Called when the rider hands off to Google Maps. */
    fun awaitMaps() {
        if (_state.value is NavState.Inactive) {
            _state.value = NavState.AwaitingMaps
            rideLog.sessionStart()
        }
    }

    /** Called for every maneuver update read from the Maps notification. */
    suspend fun onManeuver(maneuver: NavManeuver) {
        // Settings → "Auto start navigation" off: a route started in Maps alone
        // stays off the cluster until the rider starts it from the app
        // ([awaitMaps]). The switch was saved but never read (AUD-3).
        if (_state.value is NavState.Inactive && !appSettings.settings.first().autoStartNavigation) return
        _state.value = NavState.Active(maneuver)
        relay(maneuver)
        armWatchdog(arrived = maneuver.isArrival())
    }

    /**
     * Clears the cluster when Maps stops talking. Two cases seen on the bike,
     * 19 Sep 2026: the route ended but the ETA/km stayed on screen, and Maps
     * froze with its notification still posted, so no "ended" event ever came.
     *
     * Every update re-arms it, so it only fires on silence:
     *  - arrived: [ARRIVAL_CLEAR_MS] after the last update, the rider's ask.
     *  - otherwise: [STALE_CLEAR_MS]. Longer on purpose - Maps posts nothing
     *    while the rider waits at a red light, and a long Indian signal must
     *    not blank a live route. If Maps speaks again, the next update simply
     *    draws the arrow back.
     */
    private var watchdog: Job? = null

    private fun armWatchdog(arrived: Boolean) {
        watchdog?.cancel()
        watchdog = appScope.launch {
            delay(if (arrived) ARRIVAL_CLEAR_MS else STALE_CLEAR_MS)
            Log.d(TAG, "No Maps update for a while (arrived=$arrived) - clearing cluster")
            if (arrived) stop() else clearCluster()
        }
    }

    /** Maps says "Arrive at…" / "…destination…", or under ~50 m remain. */
    private fun NavManeuver.isArrival(): Boolean {
        val text = instruction.lowercase()
        if ("arriv" in text || "destination" in text) return true
        return remainingMetres() in 1..ARRIVAL_METRES
    }

    fun stop() {
        watchdog?.cancel()
        if (_state.value !is NavState.Inactive) {
            rideLog.sessionEnd()
            clearCluster()
        }
        _state.value = NavState.Inactive
        _clusterLinked.value = false
    }

    /**
     * Blanks the arrow when navigation ends.
     *
     * The cluster holds the last navigation frame until told otherwise, so the
     * final arrow stayed on the dashboard after Maps was closed — the rider
     * caught it on 11 Sep 2026. Code 46 is the official app's own deliberate
     * blank (it sends it while GPS is not locked) and drew nothing in our
     * 11 Aug sweep, so source and hardware agree.
     */
    private fun clearCluster() {
        val packet = ProtocolEngine.buildNavigationPacket(
            clusterCode = ProtocolEngine.Maneuver.FIRST_BLANK,
            distanceMetres = 0,
            clock = clockNow(),
            remainingMetres = 0,
            // The field that actually ends navigation. Blanking the arrow and
            // zeroing the distances was not enough: navActive defaults to '1',
            // so the cluster was told "still navigating, with empty values" and
            // kept the rider's last ETA and remaining-km frozen on screen after
            // the route ended. Observed on the bike, 19 Sep 2026.
            navActive = '0',
        )
        appScope.launch {
            val sent = runCatching { bleRepository.sendPacket(packet).first() }.getOrDefault(false)
            Log.d(TAG, "Navigation ended — cluster blanked (code 46) sent=$sent")
        }
        _clusterLinked.value = false
    }

    /**
     * Pushes the maneuver to the cluster. Failures are swallowed on purpose:
     * navigation must keep working on the phone even when the bike is out of
     * range, which is what the spec calls degrading to phone-only.
     */
    private suspend fun relay(maneuver: NavManeuver) {
        // `maneuverId` already holds the cluster code. The official app maps the
        // Mappls maneuver id onto these values in ViewOnClickListenerC4857A0,
        // and the app's own Maneuver constants are that table's output side.
        val packet = ProtocolEngine.buildNavigationPacket(
            clusterCode = maneuver.maneuverId,
            // Beside the arrow: how far to the next turn.
            distanceMetres = maneuver.distanceMetres(),
            // The cluster prints this field under the label ETA, so it wants the
            // ARRIVAL time, not the current one. It had been fed the phone's
            // clock since the field was first written, which the rider spotted
            // on 21 August: the dashboard read 0858Pm while the clock beside it
            // read 8:59. Maps states the journey time in its subText
            // ("47 min - 22 km - 9:35 pm ETA") and the parser already extracts
            // it, so the arrival time is simply now plus that.
            //
            // The 0x33 heartbeat clock is a different field and is confirmed
            // correct on the dashboard - it keeps the real time.
            clock = etaClock(maneuver.etaMinutes),
            // Beside the clock: how far is left of the whole journey. This was
            // never sent, so that slot showed the turn distance instead - which
            // is what the rider spotted on 19 August.
            remainingMetres = maneuver.remainingMetres(),
        )

        val delivered = runCatching { bleRepository.sendPacket(packet).first() }
            .getOrDefault(false)

        Log.d(
            TAG,
            "Relay ${maneuver.instruction} code=${maneuver.maneuverId} " +
                "dist=${maneuver.distanceMetres()}m delivered=$delivered",
        )
        // Also to a file: a real ride happens with no laptop attached, and
        // logcat will not survive it.
        rideLog.maneuver(
            instruction = maneuver.instruction,
            code = maneuver.maneuverId,
            distanceMetres = maneuver.distanceMetres(),
            delivered = delivered,
            phraseRecognised = maneuver.phraseRecognised,
            iconName = maneuver.iconName,
            iconCode = maneuver.iconManeuverId,
            codeSource = maneuver.codeSource.name,
            screenOn = maneuver.screenOn,
        )
        _clusterLinked.value = delivered
    }

    /** "400 m" / "1.2 km" / "0.5 mi" → metres. */
    /**
     * Metres left of the whole journey, parsed from Maps' subText.
     *
     * Maps writes it as "38 km" or "850 m" inside a line like
     * "1 hr 12 min * 38 km * 1:38 am ETA". The cluster field is four digits, so
     * anything past 9,999 m is clamped by the builder - on a long ride it simply
     * sits at 9999 until the last 10 km, which is honest enough.
     */
    private fun NavManeuver.remainingMetres(): Int {
        val match = DISTANCE.find(remainingDistance) ?: return 0
        val value = match.groupValues[1].replace(',', '.').toFloatOrNull() ?: return 0
        return when (match.groupValues[2].lowercase()) {
            "km" -> value * 1000
            "mi" -> value * 1609.34f
            "ft" -> value * 0.3048f
            "yd" -> value * 0.9144f
            else -> value
        }.toInt()
    }

    private fun NavManeuver.distanceMetres(): Int {
        val match = DISTANCE.find(distanceToTurn) ?: return 0
        val value = match.groupValues[1].replace(',', '.').toFloatOrNull() ?: return 0
        return when (match.groupValues[2].lowercase()) {
            "km" -> value * 1000
            "mi" -> value * 1609.34f
            "ft" -> value * 0.3048f
            "yd" -> value * 0.9144f
            else -> value
        }.toInt()
    }

    private fun clockNow(): String =
        SimpleDateFormat("hhmma", Locale.US).format(Date()).uppercase()

    /**
     * The time the rider is expected to arrive, for the cluster's ETA field.
     *
     * Falls back to the current time when Maps has not stated a journey
     * duration - showing the clock is wrong, but showing nothing at all is
     * worse, and a blank field looks like a fault rather than a gap.
     */
    private fun etaClock(etaMinutes: Int?): String {
        if (etaMinutes == null || etaMinutes <= 0) return clockNow()
        val arrival = Date(System.currentTimeMillis() + etaMinutes * 60_000L)
        return SimpleDateFormat("hhmma", Locale.US).format(arrival).uppercase()
    }
}
