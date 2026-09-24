package com.eshwar.rideconnectx.data.repository

import com.eshwar.rideconnectx.data.local.OwnerScope
import com.eshwar.rideconnectx.data.local.db.NotificationDao
import com.eshwar.rideconnectx.data.local.db.NotificationEntity
import com.eshwar.rideconnectx.data.local.db.NotificationKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
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
    private val owner: OwnerScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications: Flow<List<NotificationEntity>> =
        owner.current.flatMapLatest { dao.observeAll(it) }

    /** Drives the unread badge on the Dashboard bell. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCount: Flow<Int> =
        owner.current.flatMapLatest { dao.observeUnreadCount(it) }

    /** Raises a notification. Call this from wherever the event happens. */
    suspend fun notify(kind: NotificationKind, title: String, body: String) {
        dao.insert(
            NotificationEntity(
                ownerId = owner.currentId(),
                createdAt = System.currentTimeMillis(),
                title = title,
                body = body,
                kind = kind,
            )
        )
    }

    suspend fun markRead(id: Long) = dao.markRead(owner.currentId(), id)

    suspend fun markAllRead() = dao.markAllRead(owner.currentId())
}
