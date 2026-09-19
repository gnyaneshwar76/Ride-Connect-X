package com.eshwar.rideconnectx.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One completed ride.
 *
 * Distances are metres and durations milliseconds, so the numbers stay exact;
 * formatting into km / minutes happens in the UI layer.
 */
@Entity(tableName = "rides", indices = [Index(value = ["ownerId"])])
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /**
     * The Google account this row belongs to, or [OwnerScope.GUEST].
     *
     * Every query filters on it. Two accounts share one database file, and
     * before this column existed the second person to sign in on a phone saw
     * the first person's rows — their emergency contact, their rides, their
     * service history. No default: the compiler has to point at every write
     * site, because a row saved under the wrong owner is the bug this prevents.
     */
    val ownerId: String,

    /** Epoch millis when the ride started — also the sort key. */
    val startedAt: Long,
    val endedAt: Long,

    val distanceMeters: Int,
    val durationMillis: Long,

    /** Peak and mean speed in km/h, straight from cluster telemetry. */
    val topSpeedKmh: Int = 0,
    val avgSpeedKmh: Int = 0,

    /** Optional label, e.g. "Morning Commute". */
    val title: String = "",

    val vehicleId: String = "",

    /** False until the ride has been mirrored to Firestore. */
    val synced: Boolean = false,
)
