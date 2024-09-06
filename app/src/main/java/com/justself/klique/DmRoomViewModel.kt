package com.justself.klique

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID

data class DmMessage(
    val messageId: String,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val messageType: String, // "text" or "image"
    val timeStamp: Long,
    val media: ByteArray? = null,
    val status: String = "sending" // "sent", "delivered", or "read"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DmMessage

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

class DmRoomViewModel : ViewModel() {
    private val _dmMessages = MutableStateFlow<List<DmMessage>>(emptyList())
    val dmMessages: StateFlow<List<DmMessage>> = _dmMessages
    fun generateMessageId(): String = UUID.randomUUID().toString()

    fun sendBinary(image: ByteArray, messageType: String, enemyId: Int, messageId: String, myId: Int) {
        val timeStamp = System.currentTimeMillis()
        val status = "sending"

        // Combine metadata and image bytes
        val metadata = "$messageId;$myId;$messageType;$timeStamp;$status"
        val metadataBytes = metadata.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(ByteBuffer.allocate(4).putInt(metadataBytes.size).array())
        outputStream.write(metadataBytes)
        outputStream.write(image)

        val combinedBytes = outputStream.toByteArray()
        WebSocketManager.sendBinary(combinedBytes)

        // Create and add new message
        val newMessage = DmMessage(
            messageId = messageId,
            senderId = myId,
            senderName = "Me",
            content = "",
            messageType = messageType,
            timeStamp = timeStamp,
            media = image,
            status = status
        )
        _dmMessages.value += newMessage
    }

    fun sendTextMessage(message: String, dmRoomId: Int, myId: Int) {
        val messageId = generateMessageId()
        val timeStamp = System.currentTimeMillis()
        val status = "sending"

        val newMessage = DmMessage(
            messageId = messageId,
            senderId = myId,
            senderName = "Me",
            content = message,
            messageType = "DmText",
            timeStamp = timeStamp,
            status = status
        )

        _dmMessages.value += newMessage

        val textToSend = """
        {
        "type": "dmRoomText",
        "message": "$message",
        "dmRoomId": "$dmRoomId",
        "myId": "$myId",
        "messageId": "$messageId",
        "timeStamp": "$timeStamp"
        }
        """.trimIndent()

        send(textToSend)
    }

    fun send(textToSend: String) {
        WebSocketManager.send(textToSend)
    }

    fun loadDmMessages(dmRoomId: Int) {
        val fakeMessages = listOf(
            DmMessage(
                messageId = generateMessageId(),
                senderId = 1,
                senderName = "Alice",
                content = "Hey!",
                messageType = "text",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            ),
            DmMessage(
                messageId = generateMessageId(),
                senderId = 2,
                senderName = "Bob",
                content = "Did you see the news?",
                messageType = "text",
                timeStamp = System.currentTimeMillis(),
                status = "sent"
            )
        )
        _dmMessages.value = fakeMessages
    }
}