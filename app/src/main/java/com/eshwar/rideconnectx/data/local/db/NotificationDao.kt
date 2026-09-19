package com.eshwar.rideconnectx.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert
    suspend fun insert(notification: NotificationEntity): Long

    @Query("SELECT * FROM notifications WHERE ownerId = :owner ORDER BY createdAt DESC")
    fun observeAll(owner: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE ownerId = :owner AND unread = 1")
    fun observeUnreadCount(owner: String): Flow<Int>

    @Query("UPDATE notifications SET unread = 0 WHERE ownerId = :owner AND id = :id")
    suspend fun markRead(owner: String, id: Long)

    @Query("UPDATE notifications SET unread = 0 WHERE ownerId = :owner")
    suspend fun markAllRead(owner: String)

    @Query("DELETE FROM notifications WHERE ownerId = :owner")
    suspend fun clear(owner: String)
}
