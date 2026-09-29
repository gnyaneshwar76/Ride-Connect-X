package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.core.di.ApplicationScope
import com.eshwar.rideconnectx.data.local.OwnerScope
import com.eshwar.rideconnectx.data.local.db.RideEntity
import com.eshwar.rideconnectx.domain.repository.BleRepository
import com.eshwar.rideconnectx.domain.repository.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Adds up how far the scooter moved from its own trip meter.
 *
 * The scooter does not send speed or GPS, only its trip and odometer readings,
 * so distance is the sum of every increase in Trip A (0.1 km steps, the number
 * the cluster itself shows). A drop means the rider reset the trip meter; a
 * jump over [maxJumpKm] is a bad frame (E3). Either way that step falls back
 * to the odometer instead.
 */
class RideAccumulator(private val minMetres: Int = 200, private val maxJumpKm: Float = 2f) {
    private var lastTrip: Float? = null
    private var lastOdo: Int? = null
    private var lastReadingAt = 0L

    var metres = 0
        private set
    var startedAt = 0L
        private set
    var lastMoveAt = 0L
        private set

    fun onReading(tripKm: Float, odoKm: Int, now: Long) {
        val prevTrip = lastTrip
        val prevOdo = lastOdo
        val prevAt = lastReadingAt
        lastTrip = tripKm; lastOdo = odoKm; lastReadingAt = now
        if (prevTrip == null) return

        var km = tripKm - prevTrip
        if (km < 0f || km > maxJumpKm) {
            val odo = if (prevOdo != null) odoKm - prevOdo else 0
            km = if (odo in 1..maxJumpKm.toInt()) odo.toFloat() else 0f
        }
        if (km <= 0f) return

        // Movement began somewhere after the previous (still) reading.
        if (metres == 0) startedAt = prevAt
        metres += (km * 1000).roundToInt()
        lastMoveAt = now
    }

    /** The ride so far if it is long enough to keep, then starts afresh. */
    fun finish(ownerId: String): RideEntity? {
        val ride = if (metres >= minMetres && lastMoveAt > startedAt) {
            val duration = lastMoveAt - startedAt
            RideEntity(
                ownerId = ownerId,
                startedAt = startedAt,
                endedAt = lastMoveAt,
                distanceMeters = metres,
                durationMillis = duration,
                avgSpeedKmh = (metres / 1000.0 / (duration / 3_600_000.0)).roundToInt(),
                title = titleFor(startedAt),
            )
        } else null
        metres = 0; startedAt = 0L; lastMoveAt = 0L
        lastTrip = null; lastOdo = null
        return ride
    }

    private fun titleFor(at: Long): String =
        when (Calendar.getInstance().apply { timeInMillis = at }.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Morning ride"
            in 12..16 -> "Afternoon ride"
            in 17..20 -> "Evening ride"
            else -> "Night ride"
        }
}

/**
 * Records rides into Statistics (F1/F2): a ride runs while the scooter is
 * connected and moving, and is saved when the link ends (ignition off).
 *
 * ponytail: kept in memory; a ride in progress is lost if Android kills the
 * app. The BLE foreground service normally keeps it alive for the whole ride.
 */
@Singleton
class RideTracker @Inject constructor(
    private val ble: BleRepository,
    private val rides: RideRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val acc = RideAccumulator()
    private var started = false

    fun start() {
        if (started) return
        started = true
        appScope.launch {
            ble.telemetry.collect { t ->
                if (t.isValid) synchronized(acc) { acc.onReading(t.tripAKm, t.odometerKm, System.currentTimeMillis()) }
            }
        }
        appScope.launch {
            var wasConnected = false
            ble.connectionState.collect { s ->
                val connected = s is ConnectionState.Connected
                if (wasConnected && !connected) save()
                wasConnected = connected
            }
        }
    }

    private suspend fun save() {
        val ride = synchronized(acc) { acc.finish(OwnerScope.DRAFT) } ?: return
        // recordRide stamps the real owner (account or guest).
        runCatching { rides.recordRide(ride) }
    }
}
