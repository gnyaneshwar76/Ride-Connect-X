package com.eshwar.rideconnectx.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eshwar.rideconnectx.data.local.db.NotificationEntity
import com.eshwar.rideconnectx.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> =
        repository.notifications.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val unreadCount: StateFlow<Int> =
        repository.unreadCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0,
        )

    fun markRead(id: Long) = viewModelScope.launch { repository.markRead(id) }

    fun markAllRead() = viewModelScope.launch { repository.markAllRead() }
}
