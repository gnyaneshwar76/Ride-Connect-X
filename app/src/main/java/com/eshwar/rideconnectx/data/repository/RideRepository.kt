package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.data.local.UserPreferencesStore
import com.eshwar.rideconnectx.data.local.db.RideBucket
import com.eshwar.rideconnectx.data.local.db.RideDao
import com.eshwar.rideconnectx.data.local.db.RideEntity
import com.eshwar.rideconnectx.data.local.db.RideTotals
import com.eshwar.rideconnectx.data.remote.FirestoreUserDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/** The ranges offered by the Statistics filter. */
enum class StatsPeriod(val label: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    YEAR("This Year");

    /** Epoch millis for the start of this range. */
    fun since(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        when (this) {
            TODAY -> Unit
            WEEK -> cal.add(Calendar.DAY_OF_YEAR, -6)
            MONTH -> cal.add(Calendar.DAY_OF_YEAR, -29)
            YEAR -> cal.add(Calendar.MONTH, -11)
        }
        return cal.timeInMillis
    }

    /** SQLite strftime pattern controlling how the chart is bucketed. */
    val bucketFormat: String
        get() = when (this) {
            TODAY -> "%H"       // hour of day
            WEEK -> "%a"        // Mon, Tue…
            MONTH -> "%d/%m"    // day/month
            YEAR -> "%m"        // month number
        }
}

/**
 * Ride history.
 *
 * Room is the source of truth so Statistics works with no network. Rides are
 * additionally mirrored to Firestore for signed-in users, and marked `synced`
 * once that succeeds so a retry never duplicates them.
 */
@Singleton
class RideRepository @Inject constructor(
    private val dao: RideDao,
    private val prefs: UserPreferencesStore,
    private val firestore: FirestoreUserDataSource,
) {
    fun observeRides(period: StatsPeriod): Flow<List<RideEntity>> =
        dao.observeSince(period.since())

    fun observeTotals(period: StatsPeriod): Flow<RideTotals> =
        dao.observeTotals(period.since())

    fun observeBuckets(period: StatsPeriod): Flow<List<RideBucket>> =
        dao.observeBuckets(period.since(), period.bucketFormat)

    /** Saves a finished ride locally, then tries to push it to the cloud. */
    suspend fun recordRide(ride: RideEntity): Long {
        val id = dao.insert(ride)
        syncPending()
        return id
    }

    /** Pushes any rides that haven't reached Firestore yet. */
    suspend fun syncPending() {
        val session = prefs.session.first()
        if (!session.syncsToCloud) return

        dao.unsynced().forEach { ride ->
            firestore.addRide(
                session.uid,
                mapOf(
                    "startedAt" to ride.startedAt,
                    "endedAt" to ride.endedAt,
                    "distanceMeters" to ride.distanceMeters,
                    "durationMillis" to ride.durationMillis,
                    "topSpeedKmh" to ride.topSpeedKmh,
                    "avgSpeedKmh" to ride.avgSpeedKmh,
                    "title" to ride.title,
                    "vehicleId" to ride.vehicleId,
                ),
            )
            dao.update(ride.copy(synced = true))
        }
    }
}
