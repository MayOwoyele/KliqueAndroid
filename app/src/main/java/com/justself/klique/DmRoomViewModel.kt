package com.justself.klique

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.ui.viewModel.CliqueViewModelNavigator
import com.justself.klique.gists.ui.viewModel.DownloadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class DmMessage(
    val messageId: String,
    val senderId: Int,
    val content: String,
    val messageType: DmMessageType,
    val timeStamp: Long,
    val externalUrl: String? = null,
    val localPath: Uri? = null,
    val status: DmMessageStatus,
    val inviteId: String? = null
)

enum class DmMediaType {
    IMAGE;

    fun getFileName(): String {
        when (this) {
            IMAGE -> return "DmImage${UUID.randomUUID()}.jpg"
        }
    }
}

enum class DmMessageStatus {
    SENDING,
    SENT,
    UNSENT
}

enum class DmMessageType(val inString: String) {
    DText("DText"),
    DImage("DImage"),
    DGistCreation("DGistCreation")
}

class DmRoomViewModel : ViewModel(), WebSocketListener<DmReceivingType> {
    override val listenerId: String
        get() = ListenerIdEnum.DM_ROOM_VIEW_MODEL.theId
    private val _dmMessages = MutableStateFlow<List<DmMessage>>(emptyList())
    val dmMessages: StateFlow<List<DmMessage>> = _dmMessages
    private val _toastWarning = MutableStateFlow<String?>(null)
    val toastWarning = _toastWarning.asStateFlow()

    init {
        WebSocketManager.registerListener(this)
    }
    override fun onCleared() {
        super.onCleared()
        WebSocketManager.unregisterListener(this)
        WebSocketManager.clearWebsocketBuffer(WsDataType.ShotsRefresh)
    }

    fun generateMessageId(): String = UUID.randomUUID().toString()

    fun sendBinary(
        image: ByteArray,
        messageType: DmMessageType,
        enemyId: Int,
        messageId: String,
        myId: Int,
        context: Context
    ) {
        val timeStamp = System.currentTimeMillis()
        val metadata = "${messageType.inString}:$messageId:$enemyId"
        val metadataBytes = metadata.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(ByteBuffer.allocate(4).putInt(metadataBytes.size).array())
        outputStream.write(metadataBytes)
        outputStream.write(image)

        val combinedBytes = outputStream.toByteArray()
        WebSocketManager.sendBinary(BinaryBufferObject(WsDataType.Shots, combinedBytes))
        viewModelScope.launch {
            val imageUri = getDMRoomUriFromByteArray(image, context, DmMediaType.IMAGE)

            val newMessage = DmMessage(
                messageId = messageId,
                senderId = myId,
                content = "",
                messageType = messageType,
                timeStamp = timeStamp,
                localPath = imageUri,
                status = DmMessageStatus.SENDING
            )
            _dmMessages.value = listOf(newMessage) + _dmMessages.value
        }
    }

    override fun onMessageReceived(type: DmReceivingType, jsonObject: JSONObject) {
        Logger.d("Parsing", "type is ${type.name}")
        when (type) {
            DmReceivingType.D_TEXT -> {
                Logger.d("DText", "Plaim Error")
                try {
                    val messageId = jsonObject.getString("messageId")
                    val senderId = jsonObject.getInt("senderId")
                    val message = jsonObject.getString("content")
                    val timeStamp = jsonObject.getLong("timeStamp")
                    val newMessage = DmMessage(
                        messageId = messageId,
                        senderId = senderId,
                        content = message,
                        messageType = DmMessageType.DText,
                        status = DmMessageStatus.SENT,
                        timeStamp = timeStamp
                    )
                    _dmMessages.value = listOf(newMessage) + _dmMessages.value
                } catch (e: Exception) {
                    Logger.d("DText", "Error: $e")
                }
            }
            DmReceivingType.D_IMAGE -> {
                val messageId = jsonObject.getString("messageId")
                val senderId = jsonObject.getInt("senderId")
                val message = jsonObject.getString("content")
                val timeStamp = jsonObject.getLong("timeStamp")
                val externalUrl = jsonObject.getString("externalUrl")
                val newMessage = DmMessage(
                    messageId = messageId,
                    senderId = senderId,
                    content = message,
                    messageType = DmMessageType.DImage,
                    status = DmMessageStatus.SENT,
                    timeStamp = timeStamp,
                    externalUrl = externalUrl
                )
                _dmMessages.value = listOf(newMessage) + _dmMessages.value
                if (externalUrl.isNotEmpty()) {
                    Logger.d("External url", externalUrl)
                    handleMediaDownload(newMessage)
                }
            }
            DmReceivingType.D_GIST_CREATION -> {
                val messageId = jsonObject.getString("messageId")
                val senderId = jsonObject.getInt("senderId")
                val message = jsonObject.getString("content")
                val timeStamp = jsonObject.getLong("timeStamp")
                val inviteId = jsonObject.getString("inviteId")
                val newMessage = DmMessage(
                    messageId = messageId,
                    senderId = senderId,
                    content = message,
                    messageType = DmMessageType.DGistCreation,
                    status = DmMessageStatus.SENT,
                    inviteId = inviteId,
                    timeStamp = timeStamp
                )
                _dmMessages.value = listOf(newMessage) + _dmMessages.value
            }
            DmReceivingType.DM_KC_ERROR -> {
                Logger.d("Websocket", "DmKc triggered: $jsonObject")
                val messageId = jsonObject.getString("messageId")
                val message = jsonObject.getString("message")
                Logger.d("Websocket", "DmKc triggered: $message")
                _dmMessages.value = _dmMessages.value.map {
                    if (messageId == it.messageId) {
                        it.copy(status = DmMessageStatus.UNSENT)
                    } else {
                        it
                    }
                }
                _toastWarning.value = message
            }
            DmReceivingType.PREVIOUS_DM_MESSAGES -> {
                Logger.d("Parsing", "Logging previous")
                try {
                    val messagesArray = jsonObject.getJSONArray("messages")
                    Logger.d("Parsing", "messagesArray length: ${messagesArray.length()}")

                    val previousMessages = (0 until messagesArray.length()).map { i ->
                        val message = messagesArray.getJSONObject(i)

                        val content = message.getString("message")
                        val messageId = message.getString("messageId")
                        val mType = message.getString("messageType")
                        val messageType = when (mType) {
                            DmMessageType.DText.inString -> DmMessageType.DText
                            DmMessageType.DImage.inString -> DmMessageType.DImage
                            else -> DmMessageType.DText
                        }

                        val senderId = message.getInt("senderId")
                        val timeStamp = message.getLong("timestamp")

                        val externalUrl = if (!message.isNull("externalUrl")) {
                            message.getString("externalUrl")
                        } else {
                            null
                        }

                        val messageInstance = DmMessage(
                            content = content,
                            messageId = messageId,
                            messageType = messageType,
                            senderId = senderId,
                            timeStamp = timeStamp,
                            status = DmMessageStatus.SENT,
                            externalUrl = if (messageType != DmMessageType.DText) externalUrl else null
                        )

                        // Initiate media download if `externalUrl` is not blank or null
                        if (!externalUrl.isNullOrBlank()) {
                            viewModelScope.launch {
                                delay(10)
                                Logger.d("External url", externalUrl)
                                handleMediaDownload(messageInstance)
                            }
                        }
                        messageInstance
                    }

                    _dmMessages.value = previousMessages
                } catch (e: Exception) {
                    Log.e("ParsingError", "Error parsing previousDmMessages: ${e.message}", e)
                }
            }
            DmReceivingType.ADDITIONAL_DM_MESSAGES -> {
                val messagesArray = jsonObject.getJSONArray("messages")
                val extraPaginatedMessages = (0 until messagesArray.length()).map {
                    val message = messagesArray.getJSONObject(it)
                    val content = message.getString("message")
                    val messageId = message.getString("messageId")
                    val mType = message.getString("messageType")
                    val messageType = when (mType){
                        DmMessageType.DText.inString -> DmMessageType.DText
                        DmMessageType.DImage.inString -> DmMessageType.DImage
                        else -> DmMessageType.DText
                    }
                    val senderId = message.getInt("senderId")
                    val timeStamp = message.getLong("timeStamp")
                    val externalUrl = message.getString("externalUrl")
                    val messageInstance = DmMessage(
                        content = content,
                        messageId = messageId,
                        messageType = messageType,
                        senderId = senderId,
                        timeStamp = timeStamp,
                        status = DmMessageStatus.SENT,
                        externalUrl = if (messageType != DmMessageType.DText) externalUrl else null
                    )
                    if (externalUrl.isNotBlank()){
                        handleMediaDownload(messageInstance)
                    }
                    messageInstance
                }
                updateDmMessages(_dmMessages.value + extraPaginatedMessages)
            }
            DmReceivingType.DM_DELIVERY -> {
                val messageId = jsonObject.getString("messageId")
                _dmMessages.value = _dmMessages.value.map {
                    if (it.messageId == messageId) {
                        it.copy(status = DmMessageStatus.SENT)
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun sendTextMessage(message: String, dmRoomId: Int, myId: Int) {
        val messageId = generateMessageId()
        Logger.d("Dm", "messageId: $messageId")
        val timeStamp = System.currentTimeMillis()
        val status = DmMessageStatus.SENDING

        val newMessage = DmMessage(
            messageId = messageId,
            senderId = myId,
            content = message,
            messageType = DmMessageType.DText,
            timeStamp = timeStamp,
            status = status
        )

        _dmMessages.value = listOf(newMessage) + _dmMessages.value

        val jsonToSend = JSONObject().apply {
            put("type", DmMessageType.DText.inString)
            put("message", message)
            put("dmRoomId", dmRoomId)
            put("messageId", messageId)
            put("timeStamp", timeStamp.toString())
        }
        send(BufferObject(WsDataType.Shots, jsonToSend.toString()))
    }
    private fun updateDmMessages(newMessages: List<DmMessage>) {
        _dmMessages.value = newMessages
        Logger.d("Parsing", "Updated _dmMessages: ${_dmMessages.value}")
    }

    fun send(textToSend: BufferObject) {
        WebSocketManager.send(textToSend)
    }
    private val downloadedMediaUrls = ConcurrentHashMap<String, DownloadState>()

    private fun handleMediaDownload(message: DmMessage) {
        Logger.d("Parsing", "Called download")
        if (message.externalUrl != null && message.localPath == null) {
            when (val state = downloadedMediaUrls[message.externalUrl]) {
                is DownloadState.Downloaded -> {
                    Logger.d("Parsing", "Called download 3")
                    Logger.d("Parsing", "Local Path 3: ${state.uri}")
                    updateMessageLocalPath(message.messageId, state.uri)
                    return
                }

                is DownloadState.Downloading -> {
                    Logger.d("Parsing", "Called download 4")
                    return
                }

                else -> {
                    // Proceed to download
                }
            }
            val context = appContext
            downloadedMediaUrls[message.externalUrl] = DownloadState.Downloading

            viewModelScope.launch(Dispatchers.IO) {
                Logger.d("Parsing", "Called download 2")
                try {
                    val byteArray = downloadFromUrl(message.externalUrl)
                    val uri =
                        getDMRoomUriFromByteArray(byteArray, context, DmMediaType.IMAGE)
                    Logger.d("Parsing", "Local Path 2: $uri")
                    downloadedMediaUrls[message.externalUrl] = DownloadState.Downloaded(uri)
                    updateMessageLocalPath(message.messageId, uri)
                } catch (e: Exception) {
                    downloadedMediaUrls.remove(message.externalUrl)
                }
            }
        }
    }

    private fun updateMessageLocalPath(messageId: String, uri: Uri) {
        val updatedMessages = _dmMessages.value.map {
            if (it.messageId == messageId) {
                Logger.d("Parsing", "Local path updated?")
                it.copy(localPath = uri)
            } else {
                it
            }
        }
        updateDmMessages(updatedMessages)
    }
    fun loadAdditionalMessages(messageId: String, enemyId: Int){
        val message = """
            {
            "type": "loadMoreDmMessages",
            "enemyId": $enemyId,
            "messageId": "$messageId"
            }
        """.trimIndent()
        send(BufferObject(WsDataType.ShotsRefresh, message))
    }
    fun resetToastWarning(){
        _toastWarning.value = null
    }

    fun loadDmMessages(dmRoomId: Int) {
        _dmMessages.value = emptyList()
        val theJson = """
            {
            "type": "loadDmMessages",
            "enemyId": $dmRoomId
            }
        """.trimIndent()
        send(BufferObject(WsDataType.ShotsRefresh, theJson))
//        downloadedMediaUrls.forEach { (url, state) ->
//            Logger.d("DownloadHashMap", "URL: $url, State: $state")
//        }
//        Logger.d("Parsing", "loaded again?")
    }
    fun createGistForStranger(
        inviteId: String,
        messageContent: String,
        enemyId: Int,
        navController: NavController
    ) {
        CliqueViewModelNavigator.setNavigator(
            messageContent,
            "public",
            inviteId,
            enemyId,
            navController
        )
    }
}

suspend fun getDMRoomUriFromByteArray(
    byteArray: ByteArray,
    context: Context,
    mediaType: DmMediaType
): Uri {
    return withContext(Dispatchers.IO) {
        val customCacheDir =
            File(context.cacheDir, KliqueCacheDirString.CUSTOM_DM_CACHE.directoryName)
        if (!customCacheDir.exists()) {
            customCacheDir.mkdir()
        }

        val file = File(customCacheDir, mediaType.getFileName())
        file.writeBytes(byteArray)

        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}