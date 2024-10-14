package com.justself.klique

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.gists.ui.viewModel.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class ChatRoomMessage(
    val messageId: String,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val messageType: String,
    val timeStamp: Long,
    val externalUrl: String? = null,
    val localPath: Uri? = null,
    val status: ChatRoomStatus = ChatRoomStatus.SENDING
)
enum class ChatRoomStatus {
    SENT,
    SENDING
}
enum class ChatRoomMediaType {
    IMAGE;
    fun getFileName(): String {
        when(this) {
            IMAGE -> return "CIMage${UUID.randomUUID()}"
        }
    }
}

class ChatRoomViewModel(application: Application) : AndroidViewModel(application), WebSocketListener {
    override val listenerId = "ChatRoomViewModel"
    private val _chatRoomMessages = MutableStateFlow<List<ChatRoomMessage>>(emptyList())
    val chatRoomMessages: StateFlow<List<ChatRoomMessage>> = _chatRoomMessages
    private val _thisChatRoom = MutableStateFlow<ChatRoomOption?>(null)
    val thisChatRoom: StateFlow<ChatRoomOption?> = _thisChatRoom

    init {
        loadChatRoomDetails()
        WebSocketManager.registerListener(this)
    }

    override fun onMessageReceived(type: String, jsonObject: JSONObject) {
        when (type) {
            "chatRoomMessages" -> {
                val messages = jsonObject.getJSONArray("messages")
                (0 until messages.length()).map {
                    val messageObject = messages.getJSONObject(it)
                    val messageId = messageObject.getString("messageId")
                    val senderId = messageObject.getInt("senderId")
                    val content = messageObject.getString("content")
                    val senderName = messageObject.getString("senderName")
                    val messageType = messageObject.getString("messageType")
                    val timeStamp = messageObject.getLong("timeStamp")
                    val externalUrl = messageObject.optString("externalUrl")
                    val status = ChatRoomStatus.SENT
                    val theMessage = ChatRoomMessage(
                        messageId = messageId,
                        senderId = senderId,
                        senderName = senderName,
                        content = content,
                        messageType = messageType,
                        timeStamp = timeStamp,
                        externalUrl = externalUrl.ifBlank { null },
                        status = status
                    )
                    _chatRoomMessages.value += theMessage
                    if (externalUrl.isNotEmpty()) {
                        handleMediaDownload(theMessage)
                    }
                }
            }

            "CText" -> {
                val messageId = jsonObject.getString("messageId")
                val senderId = jsonObject.getInt("senderId")
                val content = jsonObject.getString("content")
                val senderName = jsonObject.getString("senderName")
                val messageType = jsonObject.getString("messageType")
                val timeStamp = jsonObject.getLong("timeStamp")
                val status = ChatRoomStatus.SENT
                val newMessage = ChatRoomMessage(
                    messageId = messageId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    messageType = messageType,
                    timeStamp = timeStamp,
                    status = status
                )
                _chatRoomMessages.value += newMessage
            }
            "CImage" -> {
                val messageId = jsonObject.getString("messageId")
                val senderId = jsonObject.getInt("senderId")
                val content = jsonObject.getString("content")
                val senderName = jsonObject.getString("senderName")
                val messageType = jsonObject.getString("messageType")
                val timeStamp = jsonObject.getLong("timeStamp")
                val externalUrl = jsonObject.optString("externalUrl")
                val status = ChatRoomStatus.SENT
                val theMessage = ChatRoomMessage(
                    messageId = messageId,
                    senderId = senderId,
                    senderName = senderName,
                    content = content,
                    messageType = messageType,
                    timeStamp = timeStamp,
                    externalUrl = externalUrl.ifBlank { null },
                    status = status
                )
                _chatRoomMessages.value += theMessage
                if (externalUrl.isNotEmpty()) {
                    handleMediaDownload(theMessage)
                }
            }
        }
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
        val status = ChatRoomStatus.SENDING

        val metadata = "$messageId;$myId;$myName;$messageType;$timeStamp;$status"
        val metadataBytes = metadata.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(
            ByteBuffer.allocate(4).putInt(metadataBytes.size).array()
        ) // Store metadata size
        outputStream.write(metadataBytes)
        outputStream.write(image)

        val combinedBytes = outputStream.toByteArray()
        WebSocketManager.sendBinary(combinedBytes)
        viewModelScope.launch {
            val imageLocalPath = getChatRoomUriFromByteArray(image, context, ChatRoomMediaType.IMAGE)
            val newMessage = ChatRoomMessage(
                messageId = messageId,
                senderId = myId,
                senderName = myName,
                content = "",
                messageType = messageType,
                timeStamp = timeStamp,
                localPath = imageLocalPath,
                status = status
            )
            _chatRoomMessages.value += newMessage
        }
    }

    fun sendTextMessage(message: String, chatRoomId: Int, myId: Int) {
        val messageId = generateMessageId()
        val timeStamp = System.currentTimeMillis()
        val status = ChatRoomStatus.SENDING

        val newMessage = ChatRoomMessage(
            messageId = messageId,
            senderId = myId,
            senderName = "Me",
            content = message,
            messageType = "CText",
            timeStamp = timeStamp,
            status = status
        )

        _chatRoomMessages.value += newMessage

        val textToSend = """
        {
        "type": "CText",
        "message": "$message",
        "chatRoomId": "$chatRoomId",
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

    fun loadChatMessages(chatRoomId: Int) {
        val chatRoomJson = """
            {
            "type": "loadChatRoom",
            "chatRoomId": $chatRoomId
            }
        """.trimIndent()
        send(chatRoomJson)
//        val fakeMessages = listOf(
//            ChatRoomMessage(
//                messageId = generateMessageId(),
//                senderId = 1,
//                senderName = "Alice",
//                content = "Hello everyone!",
//                messageType = "CText",
//                timeStamp = System.currentTimeMillis(),
//                status = "sent"
//            ),
//            ChatRoomMessage(
//                messageId = generateMessageId(),
//                senderId = 2,
//                senderName = "Bob",
//                content = "Did you see the game last night?",
//                messageType = "CText",
//                timeStamp = System.currentTimeMillis(),
//                status = "sent"
//            ),
//            ChatRoomMessage(
//                messageId = generateMessageId(),
//                senderId = 25,
//                senderName = "Charlie",
//                content = "Good morning!",
//                messageType = "CText",
//                timeStamp = System.currentTimeMillis(),
//                status = "sent"
//            ),
//            ChatRoomMessage(
//                messageId = generateMessageId(),
//                senderId = 3,
//                senderName = "Dave",
//                content = "Here is a picture I took.",
//                messageType = "CImage",
//                timeStamp = System.currentTimeMillis(),
//                media = byteArrayOf(1, 2, 3, 4, 5), // Placeholder for image bytes
//                status = "sent"
//            ),
//            ChatRoomMessage(
//                messageId = generateMessageId(),
//                senderId = 25,
//                senderName = "Charlie",
//                content = "Let's meet at 5 PM.",
//                messageType = "CText",
//                timeStamp = System.currentTimeMillis(),
//                status = "sent"
//            ),
//            ChatRoomMessage(
//                messageId = generateMessageId(),
//                senderId = 4,
//                senderName = "Eve",
//                content = "Looking forward to the weekend!",
//                messageType = "CText",
//                timeStamp = System.currentTimeMillis(),
//                status = "sent"
//            )
//        )

        // Assign the fake messages to the StateFlow
        //_chatRoomMessages.value = fakeMessages
    }

    private fun loadChatRoomDetails() {
        // Load the chat room details (replace with real data fetching)
        _thisChatRoom.value = ChatRoomOption(
            chatRoomId = 1,
            optionChatRoomName = "General Chat",
            optionChatRoomImage = "https://example.com/general_chat.jpg"
        )
    }
    private val downloadedMediaUrls = ConcurrentHashMap<String, DownloadState>()

    private fun handleMediaDownload(message: ChatRoomMessage) {
        if (message.externalUrl != null && message.localPath == null) {
            when (val state = downloadedMediaUrls[message.externalUrl]) {
                is DownloadState.Downloaded -> {
                    updateMessageLocalPath(message.messageId, state.uri)
                    return
                }
                is DownloadState.Downloading -> {
                    return
                }
                else -> {
                    // Proceed to download
                }
            }
            val context = getApplication<Application>().applicationContext
            downloadedMediaUrls[message.externalUrl] = DownloadState.Downloading

            viewModelScope.launch {
                try {
                    val byteArray = downloadFromUrl(message.externalUrl)
                    val uri = getChatRoomUriFromByteArray(byteArray, context, ChatRoomMediaType.IMAGE)
                    downloadedMediaUrls[message.externalUrl] = DownloadState.Downloaded(uri)
                    updateMessageLocalPath(message.messageId, uri)
                } catch (e: Exception) {
                    downloadedMediaUrls.remove(message.externalUrl)
                }
            }
        }
    }

    private fun updateMessageLocalPath(messageId: String, uri: Uri) {
        _chatRoomMessages.value = _chatRoomMessages.value.map {
            if (it.messageId == messageId) {
                it.copy(localPath = uri)
            } else {
                it
            }
        }
    }
    override fun onCleared() {
        super.onCleared()
        WebSocketManager.unregisterListener(this)
        val context = getApplication<Application>().applicationContext
        clearCustomCacheDirectory(context, KliqueCacheDirString.CUSTOM_CHAT_CACHE.directoryName)
        downloadedMediaUrls.clear()
    }
    private fun clearCustomCacheDirectory(context: Context, directoryName: String) {
        val customCacheDir = File(context.cacheDir, directoryName)
        if (customCacheDir.exists() && customCacheDir.isDirectory) {
            customCacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        }
    }
}
enum class KliqueCacheDirString(val directoryName: String) {
    CUSTOM_CHAT_CACHE("custom_chat_cache"),
}
class ChatRoomViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatRoomViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}