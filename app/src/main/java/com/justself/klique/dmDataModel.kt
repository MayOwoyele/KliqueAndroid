package com.justself.klique

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    @SerialName("message_id") val messageId: Int,
    @SerialName("sender_id") val senderId: Int,
    @SerialName("receiver_id") val receiverId: Int,
    @SerialName("message_content") val messageContent: String,
    val timestamp: String
)

@Serializable
data class MessageList(
    val status: String,
    val data: List<Message>,
    val count: Int
)

