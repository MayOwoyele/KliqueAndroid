package com.justself.klique

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationViewModel : ViewModel() {
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications

    init {
        fetchNotifications()
    }
    fun fetchNotifications() {
        _notifications.value = listOf(
            Notification(
                userId = 1,
                fullName = "John Doe",
                statusType = StatusType.AUDIO,
                contentDescription = "posted an audio",
                timestamp = System.currentTimeMillis(),
                seen = false
            ),
            Notification(
                userId = 2,
                fullName = "Jane Smith",
                statusType = StatusType.TEXT,
                contentDescription = "posted a text",
                timestamp = System.currentTimeMillis() - 3600000,
                seen = false
            )
        )
    }
    private fun generateNotificationDescription(notification: Notification): String {
        return when (notification.statusType) {
            StatusType.IMAGE -> "${notification.fullName} posted an image"
            StatusType.TEXT -> "${notification.fullName} posted a text"
            StatusType.VIDEO -> "${notification.fullName} posted a video"
            StatusType.AUDIO -> "${notification.fullName} posted an audio"
        }
    }
    fun markNotificationsAsSeen() {
        _notifications.value = _notifications.value.map { it.copy(seen = true) }
    }
}

data class Notification(
    val userId: Int,
    val fullName: String,
    val statusType: StatusType,
    val contentDescription: String, // e.g., "posted an audio"
    val timestamp: Long,
    val seen: Boolean = false
)

enum class StatusType {
    IMAGE, TEXT, VIDEO, AUDIO
}