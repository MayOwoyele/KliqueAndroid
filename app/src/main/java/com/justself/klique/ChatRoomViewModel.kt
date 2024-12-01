package com.justself.klique

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.gists.ui.viewModel.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val messageType: ChatRoomMessageType,
    val timeStamp: Long,
    val externalUrl: String? = null,
    val localPath: Uri? = null,
    val status: ChatRoomStatus = ChatRoomStatus.SENDING
)

enum class ChatRoomStatus(val statusString: String) {
    SENT("sent"),
    SENDING("sending"),
    UNSENT("unsent")
}

enum class ChatRoomMediaType {
    IMAGE;

    fun getFileName(): String {
        when (this) {
            IMAGE -> return "CIMage${UUID.randomUUID()}.jpg"
        }
    }
}

enum class ChatRoomMessageType(val typeString: String) {
    CIMAGE("CImage"),
    CTEXT("CText")
}
class ChatRoomViewModel(application: Application) : AndroidViewModel(application),
    WebSocketListener<ChatRoomReceivingType> {
    override val listenerId = ListenerIdEnum.CHAT_ROOM_VIEW_MODEL.theId
    private val _chatRoomMessages = MutableStateFlow<List<ChatRoomMessage>>(emptyList())
    val chatRoomMessages: StateFlow<List<ChatRoomMessage>> = _chatRoomMessages
    private val _thisChatRoom = MutableStateFlow<ChatRoomOption?>(null)
    val thisChatRoom: StateFlow<ChatRoomOption?> = _thisChatRoom
    private val _chatRoomId = mutableStateOf<Int?>(null)
    private val chatRoomId: Int?
        get() = _chatRoomId.value
    private val _toastWarning = MutableStateFlow<String?>(null)
    val toastWarning = _toastWarning.asStateFlow()
    fun setChatRoomId(id: Int) {
        _chatRoomId.value = id
    }

    init {
        WebSocketManager.registerListener(this)
        WebSocketManager.setChatRoomViewModel(this)
    }

    override fun onMessageReceived(type: ChatRoomReceivingType, jsonObject: JSONObject) {
        when (type) {
            ChatRoomReceivingType.CHAT_ROOM_MESSAGES -> {
                _chatRoomMessages.value = emptyList()
                    val messages = jsonObject.getJSONArray("messages")
                    (0 until messages.length()).map {
                        val messageObject = messages.getJSONObject(it)
                        val messageId = messageObject.getString("messageId")
                        val senderId = messageObject.getInt("senderId")
                        val content = messageObject.getString("content")
                        val senderName = messageObject.getString("senderName")
                        val messageTypeExtract = messageObject.getString("messageType")
                        val messageType = when (messageTypeExtract) {
                            ChatRoomMessageType.CIMAGE.typeString -> ChatRoomMessageType.CIMAGE
                            ChatRoomMessageType.CIMAGE.typeString -> ChatRoomMessageType.CTEXT
                            else -> ChatRoomMessageType.CTEXT
                        }
                        val timeStamp = messageObject.getLong("timeStamp")
                        val externalUrl = messageObject.optString("externalUrl")
                        Log.d("ChatRoom", "external url is $externalUrl")
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

            ChatRoomReceivingType.C_TEXT -> {
                val messageId = jsonObject.getString("messageId")
                val senderId = jsonObject.getInt("senderId")
                val content = jsonObject.getString("content")
                val senderName = jsonObject.getString("senderName")
                val messageType = ChatRoomMessageType.CTEXT
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
                val messageExists = _chatRoomMessages.value.any{it.messageId == messageId}
                if (!messageExists) {
                    _chatRoomMessages.value = listOf(newMessage) + _chatRoomMessages.value
                    Log.d("RawWebsocket", "${_chatRoomMessages.value}")
                }
            }

            ChatRoomReceivingType.C_IMAGE -> {
                val messageId = jsonObject.getString("messageId")
                val senderId = jsonObject.getInt("senderId")
                val content = jsonObject.getString("content")
                val senderName = jsonObject.getString("senderName")
                val messageType = ChatRoomMessageType.CIMAGE
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
                val messageExists = _chatRoomMessages.value.any{it.messageId == messageId}
                if (!messageExists) {
                    Log.d("RawWebsocket", "C image enter, $messageId")
                    _chatRoomMessages.value = listOf(theMessage) + _chatRoomMessages.value
                    Log.d("RawWebsocket", "${_chatRoomMessages.value}")
                } else {
                    Log.d("RawWebsocket", "C image not")
                }
                if (externalUrl.isNotEmpty()) {
                    handleMediaDownload(theMessage)
                }
            }
            ChatRoomReceivingType.CHAT_ROOM_ACK -> {
                val messageId = jsonObject.getString("messageId")
                _chatRoomMessages.value = _chatRoomMessages.value.map { message ->
                    if (messageId == message.messageId) {
                        message.copy(status = ChatRoomStatus.SENT)
                    } else {
                        message
                    }
                }
            }
            ChatRoomReceivingType.CHATROOM_KC_ERROR -> {
                val messageId = jsonObject.getString("messageId")
                val message = jsonObject.getString("message")
                _chatRoomMessages.value = _chatRoomMessages.value.map {
                    if (messageId == it.messageId) {
                        it.copy(status = ChatRoomStatus.UNSENT)
                    } else {
                        it
                    }
                }
                _toastWarning.value = message
            }
        }
    }

    fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }

    fun sendBinary(
        image: ByteArray,
        messageType: ChatRoomMessageType,
        chatRoomId: Int,
        messageId: String,
        myId: Int,
        myName: String,
        context: Context
    ) {
        val timeStamp = System.currentTimeMillis()

        val metadata = "${messageType.typeString}:$messageId:$chatRoomId"
        val metadataBytes = metadata.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(
            ByteBuffer.allocate(4).putInt(metadataBytes.size).array()
        )
        outputStream.write(metadataBytes)
        outputStream.write(image)

        val combinedBytes = outputStream.toByteArray()
        WebSocketManager.sendBinary(combinedBytes)
        viewModelScope.launch{
            val imageLocalPath =
                getChatRoomUriFromByteArray(image, context, ChatRoomMediaType.IMAGE)
            val newMessage = ChatRoomMessage(
                messageId = messageId,
                senderId = myId,
                senderName = myName,
                content = "",
                messageType = messageType,
                timeStamp = timeStamp,
                localPath = imageLocalPath,
                status = ChatRoomStatus.SENDING
            )
            val updatedList = _chatRoomMessages.value.toMutableList()
            updatedList.add(0, newMessage)
            _chatRoomMessages.value = updatedList
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
            messageType = ChatRoomMessageType.CTEXT,
            timeStamp = timeStamp,
            status = status
        )
        val updatedList = _chatRoomMessages.value.toMutableList()
        updatedList.add(0, newMessage)
        _chatRoomMessages.value = updatedList

        val textToSend = """
        {
        "type": "${ChatRoomMessageType.CTEXT.typeString}",
        "message": "$message",
        "chatRoomId": $chatRoomId,
        "myId": $myId,
        "messageId": "$messageId",
        "timeStamp": "$timeStamp"
        }
    """.trimIndent()

        send(textToSend)
    }

    fun send(textToSend: String) {
        WebSocketManager.send(textToSend)
    }
    fun retrial(){
        viewModelScope.launch {
            retryPendingMessages()
            chatRoomId?.let { loadChatMessages(it) }
        }
    }
    private fun retryPendingMessages() {
        val pendingMessages = _chatRoomMessages.value.filter { it.status == ChatRoomStatus.SENDING }
        pendingMessages.forEach {
            when (it.messageType) {
                ChatRoomMessageType.CTEXT -> resendText(it)
                ChatRoomMessageType.CIMAGE -> resendImage(it)
            }
        }
    }

    private fun resendText(text: ChatRoomMessage) {
        val message = text.content
        val myId = text.senderId
        val chatRoomId = chatRoomId
        if (chatRoomId != null) {
            sendTextMessage(message, chatRoomId, myId)
        }
    }

    private fun resendImage(image: ChatRoomMessage) {
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val imageByteArray = image.localPath?.let { FileUtils.loadFileAsByteArray(context, it) }
            if (imageByteArray != null) {
                sendBinary(
                    image = imageByteArray,
                    messageType = ChatRoomMessageType.CIMAGE,
                    chatRoomId = chatRoomId!!,
                    messageId = image.messageId,
                    myId = image.senderId,
                    myName = image.senderName,
                    context = context
                )
            }
        }
    }

    fun loadChatMessages(chatRoomId: Int) {
        val chatRoomJson = """
            {
            "type": "loadChatRoom",
            "chatRoomId": $chatRoomId
            }
        """.trimIndent()
        send(chatRoomJson)
    }
    fun exitChatRoom(chatRoomId: Int, myId: Int){
        val jsonBody = """
            {
            "type": "exitChatRoom",
            "chatRoomId": $chatRoomId,
            "userId": $myId
            }
        """.trimIndent()
        send(jsonBody)
    }

    fun loadChatRoomDetails(chatRoomId: Int) {
        val params = mapOf("chatRoomId" to "$chatRoomId")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    NetworkUtils.makeRequest("chatRoomDetails", KliqueHttpMethod.GET, params)
                Log.d("ChatRoom", "Chat room details: $response")
                if (response.first) {
                    val jsonResponse = JSONObject(response.second)
                    val chatRoomName = jsonResponse.getString("optionName")
                    val chatRoomImage = jsonResponse.getString("optionImage")
                    _thisChatRoom.value = ChatRoomOption(
                        chatRoomId = chatRoomId,
                        optionChatRoomName = chatRoomName,
                        optionChatRoomImage = chatRoomImage
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val downloadedMediaUrls = ConcurrentHashMap<String, DownloadState>()

    private fun handleMediaDownload(message: ChatRoomMessage) {
        Log.d("ChatRoom", "Called 1")
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
            Log.d("ChatRoom", "Called 2")
            val context = getApplication<Application>().applicationContext
            downloadedMediaUrls[message.externalUrl] = DownloadState.Downloading

            viewModelScope.launch(Dispatchers.IO) {
                Log.d("ChatRoom", "Called 3")
                try {
                    Log.d("ChatRoom", "Called 4")
                    val byteArray = downloadFromUrl(message.externalUrl)
                    Log.d("ChatRoom", "Called 5")
                    val uri =
                        getChatRoomUriFromByteArray(byteArray, context, ChatRoomMediaType.IMAGE)
                    Log.d("ChatRoom", "Uri from byte array: $uri")
                    downloadedMediaUrls[message.externalUrl] = DownloadState.Downloaded(uri)
                    updateMessageLocalPath(message.messageId, uri)
                } catch (e: Exception) {
                    Log.d("ChatRoom", "Exception: $e")
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
    fun resetToastWarning(){
        _toastWarning.value = null
    }

    override fun onCleared() {
        super.onCleared()
        WebSocketManager.unregisterListener(this)
        val context = getApplication<Application>().applicationContext
        clearCustomCacheDirectory(
            context,
            KliqueCacheDirString.CUSTOM_CHAT_ROOM_CACHE.directoryName
        )
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
    CUSTOM_CHAT_ROOM_CACHE("custom_chat_room_cache"),
    CUSTOM_DM_CACHE("dm_cache")
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