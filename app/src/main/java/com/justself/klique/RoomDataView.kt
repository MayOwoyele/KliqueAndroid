package com.justself.klique

data class ChatMessage(
    val id: Int,
    val gistId: Int,
    val customerId: Int,
    val sender: String,
    val content: String,
    val status: String,
    val messageType: String = "text" // Default to "text" for regular messages
)
