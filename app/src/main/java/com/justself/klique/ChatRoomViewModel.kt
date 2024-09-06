package com.justself.klique

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID
data class ChatRoomMessage(
    val messageId: String,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val messageType: String, // Either "text" or "image"
    val timeStamp: Long,
    val media: ByteArray? = null,
    val status: String = "sending" // Either "sent", "delivered", or "read"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatRoomMessage

        if (messageId != other.messageId) return false
        if (senderId != other.senderId) return false
        if (senderName != other.senderName) return false
        if (content != other.content) return false
        if (messageType != other.messageType) return false
        if (timeStamp != other.timeStamp) return false
        if (media != null) {
            if (other.media == null) return false
            if (!media.contentEquals(other.media)) return false
        } else if (other.media != null) return false
        if (status != other.status) return false

        return true
    }

    override fun hashCode(): Int {
        var result = messageId.hashCode()
        result = 31 * result + senderId
        result = 31 * result + senderName.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + messageType.hashCode()
        result = 31 * result + timeStamp.hashCode()
        result = 31 * result + (media?.contentHashCode() ?: 0)
        result = 31 * result + status.hashCode()
        return result
    }
}

class ChatRoomViewModel : ViewModel() {
    private val _chatRoomMessages = MutableStateFlow<List<ChatRoomMessage>>(emptyList())
    // Publicly exposed immutable StateFlow
    val chatRoomMessages: StateFlow<List<ChatRoomMessage>> = _chatRoomMessages
    private val _thisChatRoom = MutableStateFlow<ChatRoomOption?>(null)
    val thisChatRoom: StateFlow<ChatRoomOption?> = _thisChatRoom
    init {
        loadChatRoomDetails()
    }
    fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }

    fun sendBinary(
        image: ByteArray,
        messageType: String,
        chatRoomId: Int,
        messageId: String,
        myId: Int,
        myName: String,
        context: Context
    ) {
        val timeStamp = System.currentTimeMillis()
        val status = "sending"

        // Convert metadata to bytes
        val metadata = "$messageId;$myId;$myName;$messageType;$timeStamp;$status"
        val metadataBytes = metadata.toByteArray(Charsets.UTF_8)

        // Combine metadata and image bytes
        val outputStream = ByteArrayOutputStream()
        outputStream.write(ByteBuffer.allocate(4).putInt(metadataBytes.size).array()) // Store metadata size
        outputStream.write(metadataBytes)
        outputStream.write(image)

        val combinedBytes = outputStream.toByteArray()

        // Send the combined byte array
        WebSocketManager.sendBinary(combinedBytes)

        // Create a new message object and add it to the message list
        val newMessage = ChatRoomMessage(
            messageId = messageId,
            senderId = myId,
            senderName = myName,
            content = "", // Image messages might have empty content
            messageType = messageType,
            timeStamp = timeStamp,
            media = image,
            status = status
        )
        _chatRoomMessages.value = _chatRoomMessages.value + newMessage
    }
    fun sendTextMessage(message: String, chatRoomId: Int, myId: Int) {
        val messageId = generateMessageId()
        val timeStamp = System.currentTimeMillis()
        val status = "sending"

        // Create a new message object and add it to the message list
        val newMessage = ChatRoomMessage(
            messageId = messageId,
            senderId = myId,
            senderName = "Me",
            content = message,
            messageType = "CText",
            timeStamp = timeStamp,
            status = status
        )

        // Update the chatRoomMessages StateFlow with the new message
        _chatRoomMessages.value += newMessage

        // Construct the JSON message
        val textToSend = """
        {
        "type": "chatRoomText",
        "message": "$message",
        "chatRoomId": "$chatRoomId",
        "myId": "$myId",
        "messageId": "$messageId",
        "timeStamp": "$timeStamp"
        }
    """.trimIndent()

        // Send the message using WebSocketManager
        send(textToSend)
    }

    fun send(textToSend: String){
        WebSocketManager.send(textToSend)
    }
    fun loadChatMessages(chatRoomId: Int) {
        // Fake data representing chat messages
        val fakeMessages = listOf(
            ChatRoomMessage(
                messageId = generateMessageId(),
                senderId = 1,
                senderName = "Alice",
                content = "Hello everyone!",
                messageType = "CText",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            ),
            ChatRoomMessage(
                messageId = generateMessageId(),
                senderId = 2,
                senderName = "Bob",
                content = "Did you see the game last night?",
                messageType = "CText",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            ),
            ChatRoomMessage(
                messageId = generateMessageId(),
                senderId = 25,
                senderName = "Charlie",
                content = "Good morning!",
                messageType = "CText",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            ),
            ChatRoomMessage(
                messageId = generateMessageId(),
                senderId = 3,
                senderName = "Dave",
                content = "Here is a picture I took.",
                messageType = "CImage",
                timeStamp = System.currentTimeMillis(),
                media = byteArrayOf(1, 2, 3, 4, 5), // Placeholder for image bytes
                status = "sent"
            ),
            ChatRoomMessage(
                messageId = generateMessageId(),
                senderId = 25,
                senderName = "Charlie",
                content = "Let's meet at 5 PM.",
                messageType = "CText",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            ),
            ChatRoomMessage(
                messageId = generateMessageId(),
                senderId = 4,
                senderName = "Eve",
                content = "Looking forward to the weekend!",
                messageType = "CText",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            )
        )

        // Assign the fake messages to the StateFlow
        _chatRoomMessages.value = fakeMessages
    }
    private fun loadChatRoomDetails() {
        // Load the chat room details (replace with real data fetching)
        _thisChatRoom.value = ChatRoomOption(
            chatRoomId = 1,
            optionChatRoomName = "General Chat",
            optionChatRoomImage = "https://example.com/general_chat.jpg"
        )
    }
}