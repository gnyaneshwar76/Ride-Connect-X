package com.eshwar.rideconnectx.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Aggregate figures for the summary cards. */
data class RideTotals(
    val trips: Int,
    val totalMeters: Int,
    val totalMillis: Long,
    val avgSpeed: Double,
)

/** One bar in the Statistics chart. */
data class RideBucket(
    val label: String,
    val meters: Int,
)

@Dao
interface RideDao {

    @Insert
    suspend fun insert(ride: RideEntity): Long

    @Update
    suspend fun update(ride: RideEntity)

    @Query("SELECT * FROM rides WHERE ownerId = :owner ORDER BY startedAt DESC")
    fun observeAll(owner: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE ownerId = :owner AND startedAt >= :since ORDER BY startedAt DESC")
    fun observeSince(owner: String, since: Long): Flow<List<RideEntity>>

    @Query(
        """
        SELECT COUNT(*)                                   AS trips,
               COALESCE(SUM(distanceMeters), 0)           AS totalMeters,
               COALESCE(SUM(durationMillis), 0)           AS totalMillis,
               COALESCE(AVG(NULLIF(avgSpeedKmh, 0)), 0.0) AS avgSpeed
        FROM rides
        WHERE startedAt >= :since
        """
    )
    fun observeTotals(since: Long): Flow<RideTotals>

    /**
     * Distance grouped by day, for the chart.
     * `startedAt` is epoch millis, so it is divided down to a day index and
     * rendered back as a local date by SQLite.
     */
    @Query(
        """
        SELECT strftime(:format, startedAt / 1000, 'unixepoch', 'localtime') AS label,
               SUM(distanceMeters)                                           AS meters
        FROM rides
        WHERE startedAt >= :since
        GROUP BY label
        ORDER BY MIN(startedAt) ASC
        """
    )
    fun observeBuckets(since: Long, format: String): Flow<List<RideBucket>>

    @Query("SELECT * FROM rides WHERE ownerId = :owner AND synced = 0")
    suspend fun unsynced(owner: String): List<RideEntity>

    @Query("DELETE FROM rides WHERE ownerId = :owner")
    suspend fun clear(owner: String)
}
