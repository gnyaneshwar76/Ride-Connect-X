package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.data.local.db.NotificationDao
import com.eshwar.rideconnectx.data.local.db.NotificationEntity
import com.eshwar.rideconnectx.data.local.db.NotificationKind
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-app notifications.
 *
 * Entirely local: entries are raised by the app's own events, so the list is
 * available offline and in Guest Mode. Nothing is seeded — an account with no
 * activity genuinely has no notifications, and the screen shows its empty state.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val dao: NotificationDao,
) {
    val notifications: Flow<List<NotificationEntity>> = dao.observeAll()

    /** Drives the unread badge on the Dashboard bell. */
    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    /** Raises a notification. Call this from wherever the event happens. */
    suspend fun notify(kind: NotificationKind, title: String, body: String) {
        dao.insert(
            NotificationEntity(
                createdAt = System.currentTimeMillis(),
                title = title,
                body = body,
                kind = kind,
            )
        )
    }

    suspend fun markRead(id: Long) = dao.markRead(id)

    suspend fun markAllRead() = dao.markAllRead()
}
