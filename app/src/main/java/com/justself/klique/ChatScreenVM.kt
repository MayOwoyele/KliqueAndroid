package com.justself.klique

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.ui.viewModel.CliqueViewModelNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.time.Instant
import java.util.Collections
import java.util.UUID
import kotlin.collections.set


class ChatScreenViewModel(
    private val chatDao: ChatDao,
    private val personalChatDao: PersonalChatDao,
    application: Application
) : AndroidViewModel(application), WebSocketListener<PrivateChatReceivingType> {
    override val listenerId: String = ListenerIdEnum.PRIVATE_CHAT_SCREEN.theId

    private val _chats = MutableStateFlow<List<ChatList>>(emptyList())
    val chats: StateFlow<List<ChatList>> get() = _chats

    private val _personalChats = MutableStateFlow<List<PersonalChat>>(emptyList())
    val personalChats: StateFlow<List<PersonalChat>> get() = _personalChats

    private val _onlineStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val onlineStatuses: StateFlow<Map<Int, Boolean>> get() = _onlineStatuses

    private val _currentChat = MutableStateFlow<Int?>(null)
    val currentChat: StateFlow<Int?> get() = _currentChat
    private val _searchResults = MutableStateFlow<List<ChatList>>(emptyList())
    val searchResults: StateFlow<List<ChatList>> get() = _searchResults
    private val _messagesToForward = MutableLiveData<List<PersonalChat>>(emptyList())
    val messagesToForward: LiveData<List<PersonalChat>> get() = _messagesToForward

    private val _selectedMessages = MutableLiveData<List<String>>(emptyList())
    val selectedMessages: LiveData<List<String>> get() = _selectedMessages

    private val _isSelectionMode = MutableLiveData(false)
    val isSelectionMode: LiveData<Boolean> get() = _isSelectionMode
    val pageSize = 20
    private var _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading
    private val processingMediaMessages = Collections.synchronizedSet(mutableSetOf<String>())

    init {
        WebSocketManager.registerListener(this)
        WebSocketManager.setChatScreenViewModel(this)
        retryPendingMessages(getApplication<Application>().applicationContext)
        checkContactUpdate()
        loadChats(SessionManager.customerId.value)
        gatherMessages()
        calculateUnreadMsg()
    }

    override fun onMessageReceived(type: PrivateChatReceivingType, jsonObject: JSONObject) {
        when (type) {
            PrivateChatReceivingType.IS_ONLINE -> {
                val userId = jsonObject.optInt("user_id", 0)
                val status = jsonObject.optString("status", "offline")
                if (userId != 0) {
                    _onlineStatuses.update { currentStatuses ->
                        currentStatuses.toMutableMap().apply {
                            if (status == "online") {
                                this[userId] = true
                            } else {
                                this.remove(userId)
                            }
                        }
                    }
                } else {
                    loggerD("Websocket") { "Invalid user Id in json: $jsonObject" }
                }
            }

            PrivateChatReceivingType.P_TEXT -> {
                launchAndParseForNonMediaType(jsonObject, ::handleIncomingWebsocketCaller)
            }

            PrivateChatReceivingType.P_IMAGE,
            PrivateChatReceivingType.P_AUDIO,
            PrivateChatReceivingType.P_VIDEO -> {
                loggerD("AckCheck"){"onMessageReceived"}
                val messageId = jsonObject.optString("messageId", "")
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        loggerD("AckCheck"){"onMessageReceived 2"}
                        maybeParseAndReturnMessage(messageId, jsonObject)?.let { parsedMessage ->
                            handleIncomingWebsocketCaller(parsedMessage)
                        } ?: loggerD("AckCheck"){"onMessageReceived 3"}
                    } catch (e: Exception) {
                        loggerD("Websocket") { "Error processing message $messageId: ${e.message}" }
                    }
                }
            }
            PrivateChatReceivingType.P_GIST_INVITE -> {
                loggerD("Websocket") { "PGistInvite Function called" }
                launchAndParseForNonMediaType(jsonObject, ::handleIncomingWebsocketCaller)
            }
            PrivateChatReceivingType.P_GIST_CREATION -> {
                launchAndParseForNonMediaType(jsonObject, ::handleIncomingWebsocketCaller)
            }

            PrivateChatReceivingType.ACK -> {
                val statusString = jsonObject.getString("status")
                loggerD("Websocket") { "Status: $statusString" }
                val status = when (statusString) {
                    "delivered" -> PersonalMessageStatus.DELIVERED
                    "sent" -> PersonalMessageStatus.SENT
                    else -> PersonalMessageStatus.SENT
                }

                val messageId = jsonObject.optString("messageId")
                val timestamp = extractNumberLong(jsonObject)

                viewModelScope.launch(Dispatchers.IO) {
                    personalChatDao.updateStatus(messageId, status)
                    loggerD("ForStatus") { "Status: $status; timestamp: $timestamp" }
                    val currentEnemyId = _currentChat.value
                    val updatedMessage = personalChatDao.getPersonalChatById(messageId)

                    if (updatedMessage != null && currentEnemyId != null &&
                        (updatedMessage.enemyId == currentEnemyId || updatedMessage.myId == currentEnemyId)
                    ) {
                        withContext(Dispatchers.Main) {  // Switch to main thread for state update
                            _personalChats.update { currentList ->
                                currentList.toMutableList().apply {
                                    val index = indexOfFirst { it.messageId == messageId }
                                    if (index != -1) {
                                        this[index] = updatedMessage.copy(
                                            status = status,
                                            timeStamp = timestamp
                                        )
                                    }
                                }.sortedByDescending { it.timeStamp }
                            }
                        }
                    }
                }
            }

            PrivateChatReceivingType.P_PROFILE_UPDATE -> {
                loggerD("PProfileUpdate") { "PProfileUpdate Function called" }
                val profileUpdate = jsonObject.getJSONArray("profileUpdates")
                for (theIndex in 0 until profileUpdate.length()) {
                    val eachUpdate = profileUpdate.getJSONObject(theIndex)
                    val enemyId = eachUpdate.getInt("enemyId")
                    val contactName = eachUpdate.getString("fullName")
                    val profilePhoto =
                        NetworkUtils.fixLocalHostUrl(eachUpdate.getString("profileUrl"))
                    val isVerified = eachUpdate.getBoolean("isVerified")
                    updateProfile(enemyId, contactName, profilePhoto, isVerified)
                }
            }
        }
    }

    private fun downloadFileFromUrl(url: String): ByteArray? {
        val theFormattedUrl = NetworkUtils.fixLocalHostUrl(url)
        return try {
            val connection = URL(theFormattedUrl).openConnection() as HttpURLConnection
            connection.inputStream.use { input ->
                input.readBytes()
            }
        } catch (e: IOException) {
            loggerD("PersonalChats") { "Failed to download file from URL: $theFormattedUrl" }
            null
        }
    }

    private fun extractNumberLong(jsonObject: JSONObject): String {
        return jsonObject.optJSONObject("timestamp")
            ?.optJSONObject("\$date")
            ?.optString("\$numberLong") ?: System.currentTimeMillis().toString()
    }

    private fun getReadableTimestamp(instant: Instant = Instant.now()): String {
        return instant.toEpochMilli().toString()
    }

    private fun checkContactUpdate() {
        val message = """
            {
            "type": "checkContactUpdate"
            }
        """.trimIndent()
        send(BufferObject(type = WsDataType.PersonalMessageType, message = message))
    }

    fun deleteChat(enemyId: Int, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = personalChatDao.getPersonalChatsByEnemyId(SessionManager.customerId.value, enemyId)
            messages.forEach { message ->
                message.mediaUri?.let { deleteMediaFile(context, it) }
            }
            personalChatDao.deletePersonalChatsForEnemy(myId = SessionManager.customerId.value, enemyId = enemyId)
            chatDao.deleteChat(enemyId)
            loadChats(SessionManager.customerId.value)
        }
    }

    private fun calculateUnreadMsg() {
        viewModelScope.launch {
            chats.collect { chatList ->
                val unread = chatList.sumOf { it.unreadMsgCounter }
                GlobalEventBus.updateUnreadMessageCount(unread)
            }
        }
    }

    fun deleteMessage(messageId: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val message = personalChatDao.getPersonalChatById(messageId)
            message?.let {
                if (it.mediaUri != null) {
                    deleteMediaFile(context, it.mediaUri)
                }
                personalChatDao.deletePersonalChat(messageId)
                withContext(Dispatchers.Main) {
                    _personalChats.update { currentChats ->
                        currentChats.filterNot { chat -> chat.messageId == messageId }
                    }
                }
                val enemyId = reasonOutEnemyId(message)
                reinsertUpdatedChatEntry(enemyId)
            }
        }
    }

    private fun deleteMediaFile(context: Context, mediaUri: String) {
        val uri = Uri.parse(mediaUri)
        try {
            context.contentResolver.delete(uri, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChats(myId: Int, updateSearchResults: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatList = getSortedChats(myId)
            loggerD("ChatList") { "Fetched chat list: $chatList" }
            _chats.value = chatList
            if (updateSearchResults) {
                loggerD("ChatList") { "Updating searchResults with: $chatList" }
                _searchResults.value = chatList
            }
        }
    }

    fun enterChat(enemyId: Int) {
        subscribeToOnlineStatus(enemyId)
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.resetUnreadMsgCounter(enemyId)
        }
        _currentChat.value = enemyId
    }

    fun toggleMessageSelection(messageId: String) {
        val currentSelection = _selectedMessages.value?.toMutableList() ?: mutableListOf()
        if (currentSelection.contains(messageId)) {
            currentSelection.remove(messageId)
        } else {
            currentSelection.add(0, messageId)
        }
        _selectedMessages.value = currentSelection
        _isSelectionMode.value = currentSelection.isNotEmpty()
    }

    fun clearSelection() {
        _selectedMessages.value = emptyList()
        _isSelectionMode.value = false
    }

    fun clearPersonalChat() {
        _personalChats.value = emptyList()
    }

    fun subscribeToOnlineStatus(enemyId: Int) {
        val subscribeMessage = """
            {
                "type": "subscribeOnlineStatus",
                "enemyId": $enemyId
            }
        """.trimIndent()
        val theMessage = BufferObject(message = subscribeMessage)
        send(theMessage)
    }

    private fun unsubscribeFromOnlineStatus(enemyId: Int) {
        val unsubscribeMessage = """
            {
                "type": "unsubscribeOnlineStatus",
                "enemyId": $enemyId
            }
        """.trimIndent()
        send(BufferObject(message = unsubscribeMessage))
        val currentMap = _onlineStatuses.value.toMutableMap()
        currentMap.remove(enemyId)
        _onlineStatuses.value = currentMap
    }

    fun leaveChat() {
        currentChat.value?.let { unsubscribeFromOnlineStatus(it) }
        _currentChat.value = null
    }

    private fun incrementUnreadMsgCounter(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.incrementUnreadMsgCounter(enemyId)
        }
    }

    private fun handleIncomingWebsocketCaller(message: PersonalChat) {
        loggerD("AckCheck"){"handleIncomingWsCaller"}
        handleIncomingPersonalMessage(message, NetworkType.WEBSOCKET)
    }

    private fun handleIncomingHttpCaller(message: PersonalChat, acknowledgment: () -> Unit) {
        handleIncomingPersonalMessage(message, NetworkType.HTTP, acknowledgment)
    }

    private fun handleIncomingPersonalMessage(newMessage: PersonalChat, network: NetworkType, upperAcknowledgment: (() -> Unit)? = null) {
        loggerD("Incoming") { "newMessage is $newMessage" }
        viewModelScope.launch(Dispatchers.IO) {
            val chatExists = checkChatExistsSync(newMessage.myId, newMessage.enemyId)
            loggerD("Websocket") { "Is New Incoming is ${!chatExists}" }
            val messageExists = personalChatDao.getPersonalChatById(newMessage.messageId) != null
            loggerD("AckCheck"){"phase 1, $network, ${newMessage.mediaUri?.startsWith("http") == true}"}
            val acknowledgement = when (network) {
                NetworkType.WEBSOCKET -> {
                    if (newMessage.mediaUri?.startsWith("http") == true) {
                        { /* no-op: do not send ack so external URL remains valid */ }
                    } else {
                        { sendDeliveryAcknowledgment(newMessage.messageId) }
                    }
                }

                NetworkType.HTTP -> {
                    upperAcknowledgment
                }
            }
            if (!messageExists) {
                loggerD("MessageExists") {
                    "Message with id ${newMessage.messageId} doesn't exist"
                }
                addAndProcessPersonalChat(newMessage, chatExists, acknowledgement)
            } else {
                loggerD("AckCheck") {
                    "Message with id ${newMessage.messageId} already exists"
                }
                if (acknowledgement != null) {
                    acknowledgement()
                } else if (upperAcknowledgment != null) {
                    upperAcknowledgment()
                }
            }
        }
    }

    private fun reasonOutEnemyId(message: PersonalChat): Int {
        return if (message.myId == SessionManager.customerId.value) message.enemyId else message.myId
    }

    private fun updateChatListWithNewMessage(newMessage: PersonalChat, chatExists: Boolean) {
        val enemyId = reasonOutEnemyId(newMessage)
        val lastMsg = when (newMessage.messageType) {
            PersonalMessageType.P_IMAGE -> "Photo"
            PersonalMessageType.P_VIDEO -> "Video"
            PersonalMessageType.P_AUDIO -> "Audio"
            PersonalMessageType.P_GIST_INVITE -> "Gist Invite..."
            PersonalMessageType.P_TEXT -> newMessage.content
            PersonalMessageType.P_GIST_CREATION -> "Gist Creation..."
        }
        val timeStamp = newMessage.timeStamp
        loggerD("SpecialChat") { "${_currentChat.value} and $enemyId: ${newMessage.content}" }
        viewModelScope.launch(Dispatchers.IO) {
            loggerD("SpecialChat") { "${_currentChat.value} and $enemyId: ${newMessage.content} 3" }
            if (_currentChat.value == enemyId) {
                val updatedList = _personalChats.value.toMutableList()
                if (updatedList.none { it.messageId == newMessage.messageId }) {
                    val smallListThreshold = 400
                    if (updatedList.size < smallListThreshold) {
                        updatedList.add(newMessage)
                        updatedList.sortByDescending { it.timeStamp.toLongOrNull() ?: 0L }
                    } else {
                        val insertionIndex = findInsertionIndex(updatedList, newMessage)
                        updatedList.add(insertionIndex, newMessage)
                    }
                }
                withContext(Dispatchers.Main) {
                    _personalChats.value = updatedList
                }
            }
            val chat = ChatList(
                enemyId = enemyId,
                contactName = "Your NewFriend",
                lastMsg = lastMsg,
                lastMsgAddTime = timeStamp,
                profilePhoto = "",
                myId = SessionManager.customerId.value,
                unreadMsgCounter = if (_currentChat.value != enemyId && newMessage.myId != SessionManager.customerId.value) 1 else 0
            )
            if (!chatExists) {
                viewModelScope.launch(Dispatchers.Main) {
                    chatDao.addChat(chat)
                    sendJsonToUpdateProfile(listOf(enemyId))
                    loadChats(SessionManager.customerId.value)
                }
                loggerD("AckChecker") { "add chat called with $chat" }
            } else {
                loggerD("AckChecker") { "not added ${newMessage.myId}, ${newMessage.enemyId}" }
                reinsertUpdatedChatEntry(enemyId)
            }
            if (newMessage.enemyId == SessionManager.customerId.value) {
                updatePopUp(newMessage.myId, lastMsg)
            }
        }
    }

    /**
     * Helper function that performs binary search to find the correct insertion index
     * in a list sorted in descending order based on timestamp.
     */
    private fun findInsertionIndex(list: List<PersonalChat>, newMessage: PersonalChat): Int {
        val newTimestamp = newMessage.timeStamp.toLongOrNull() ?: 0L
        var low = 0
        var high = list.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val midTimestamp = list[mid].timeStamp.toLongOrNull() ?: 0L
            if (newTimestamp > midTimestamp) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return low
    }

    private fun updatePopUp(enemyId: Int, message: String) {
        if (enemyId != currentChat.value) {
            viewModelScope.launch(Dispatchers.IO) {
                val name = chatDao.getChatByEnemyId(enemyId)?.contactName ?: "Your NewFriend"
                val data = SnackBarMessageData(name, message, enemyId)
                GlobalEventBus.sendSnackBarMessage(data)
            }
        }
    }

    fun sendJsonToUpdateProfile(userIds: List<Int>) {
        val jsonObject = JSONObject().apply {
            put("type", "handleMultipleProfileUpdates")
            put("userIds", JSONArray(userIds))
        }.toString()
        send(BufferObject(type = WsDataType.PersonalMessageType, message = jsonObject))
    }

    private suspend fun getSortedChats(myId: Int): List<ChatList> {
        return withContext(Dispatchers.IO) {
            chatDao.getAllChats(myId).sortedByDescending { it.lastMsgAddTime }
        }
    }

    fun fetchChatDetails(enemyId: Int): Flow<ChatList?> = flow {
        emit(chatDao.getChatByEnemyId(enemyId))
    }.flowOn(Dispatchers.IO)

    private fun addAndProcessPersonalChat(
        personalChat: PersonalChat,
        chatExists: Boolean,
        acknowledgment: (() -> Unit)? = null
    ) {
        loggerD("MessageExists") { "addAndProcessPersonalChat called" }
        viewModelScope.launch(Dispatchers.IO) {
            val enemyId =
                if (personalChat.myId == SessionManager.customerId.value) personalChat.enemyId else personalChat.myId
            if (_currentChat.value != enemyId) {
                loggerD("currentChat") { "Current chat is ${_currentChat.value}" }
                val existingMessage = personalChatDao.getPersonalChatById(personalChat.messageId)
                val notExists = existingMessage == null
                val notSelfSent = personalChat.myId != SessionManager.customerId.value
                if (notExists && notSelfSent) {
                    loggerD("currentChat") { "Current chat is ${_currentChat.value}" }
                    incrementUnreadMsgCounter(enemyId)
                }
            }
            personalChatDao.addPersonalChat(personalChat)
            updateChatListWithNewMessage(personalChat, chatExists)
            acknowledgment?.invoke()
        }
    }

    fun loadPersonalChats(
        myId: Int,
        enemyId: Int,
        loadMore: Boolean = false,
        lastMessageId: String? = null
    ) {
        if (_isLoading.value == true) return
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val personalChatList = if (loadMore) {
                lastMessageId?.let { theLastMessageId ->
                    personalChatDao.getPersonalChatsBefore(
                        myId,
                        enemyId,
                        theLastMessageId,
                        pageSize
                    )
                        .sortedByDescending { it.timeStamp }
                }
            } else {
                personalChatDao.getInitialPersonalChats(myId, enemyId, pageSize)
            }
            if (personalChatList != null) {
                loggerD("MessageLoading") { "Loaded ${personalChatList.size} messages" }
            }
            if (loadMore) {
                val currentList = _personalChats.value.toMutableList()
                if (personalChatList != null) {
                    currentList.addAll(personalChatList)
                }
                withContext(Dispatchers.Main) {
                    _personalChats.value = currentList
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (personalChatList != null) {
                        _personalChats.value = personalChatList
                    }
                }
            }
            _isLoading.postValue(false)
        }
    }

    private suspend fun checkChatExistsSync(myId: Int, enemyId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            chatDao.chatExists(myId, enemyId)
        }
    }

    fun fetchNewMessagesFromServer() {
        viewModelScope.launch {
            if (WebSocketManager.isConnected.value) {
                val jsonData = JSONObject()
                    .put("type", "fetchUndelivered")
                    .toString()
                send(BufferObject(message = jsonData))
            } else {
                ChatVMObject.fetchUndeliveredMessages { response ->
                    val theResponse = response.toNetworkTriple()
                    if (theResponse.first) {
                        processHttpResponse(theResponse)
                    }
                }
            }
        }
    }

    fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }

    private fun updateProfile(
        enemyId: Int,
        contactName: String,
        profilePhoto: String,
        isVerified: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateProfile(enemyId, contactName, profilePhoto, isVerified)
        }
        val updatedChat = _chats.value.map {
            if (it.enemyId == enemyId) {
                it.copy(
                    profilePhoto = profilePhoto,
                    contactName = contactName,
                    isVerified = isVerified
                )
            } else {
                it
            }
        }
        _chats.value = updatedChat
    }

    fun retryPendingMessages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pendingMessages = personalChatDao.getMessagesByStatus(PersonalMessageStatus.PENDING)
            pendingMessages.forEach { message ->
                when (message.messageType) {
                    PersonalMessageType.P_TEXT -> {
                        sendTextMessage(
                            message = message.content,
                            enemyId = message.enemyId,
                            myId = message.myId,
                            theMessageId = message.messageId
                        )
                    }

                    PersonalMessageType.P_GIST_INVITE -> {
                        if (message.topic != null && message.gistId != null) {
                            sendGistInvite(
                                topic = message.topic,
                                gistId = message.gistId,
                                enemyId = message.enemyId,
                                myId = message.myId,
                                theMessageId = message.messageId
                            )
                        }
                    }

                    PersonalMessageType.P_IMAGE, PersonalMessageType.P_VIDEO, PersonalMessageType.P_AUDIO -> {
                        message.mediaUri?.let { uri ->
                            val fileData = FileUtils.loadFileAsByteArray(context, Uri.parse(uri))
                            if (fileData != null) {
                                sendBinary(
                                    data = fileData,
                                    type = message.messageType,
                                    enemyId = message.enemyId,
                                    messageId = message.messageId,
                                    myId = message.myId,
                                    fullName = "",
                                    context = context,
                                    isRetry = true
                                )
                            }
                        }
                    }

                    PersonalMessageType.P_GIST_CREATION -> {
                        message.inviteId?.let {
                            sendGistCreation(
                                inviteId = it,
                                enemyId = message.enemyId,
                                myId = message.myId,
                                theMessageId = message.messageId,
                                messageContent = message.content
                            )
                        }
                    }
                }
            }
        }
    }

    private fun launchAndParseForNonMediaType(jsonObject: JSONObject, handle: (PersonalChat) -> Unit) {
        viewModelScope.launch {
            val messageId = jsonObject.optString("messageId", "")
            if (messageId.isNotEmpty()){
                maybeParseAndReturnMessage(messageId, jsonObject)?.let { handle(it) }
            }
        }
    }

    fun sendGistCreation(
        inviteId: String,
        enemyId: Int,
        myId: Int,
        messageContent: String,
        theMessageId: String? = null
    ) {
        val jsonLambda = { messageId: String ->
            val json = JSONObject().apply {
                put("type", "PGistCreation")
                put("inviteId", inviteId)
                put("enemyId", enemyId)
                put("content", messageContent)
                put("messageId", messageId)
            }
            json.toString()
        }
        val theExecution: (String) -> Unit = { messageId ->
            val readableTimeStamp = getReadableTimestamp()
            val personalChat = PersonalChat(
                messageId = messageId,
                enemyId = enemyId,
                myId = myId,
                content = messageContent,
                status = PersonalMessageStatus.PENDING,
                messageType = PersonalMessageType.P_GIST_CREATION,
                timeStamp = readableTimeStamp,
                mediaUri = null,
                inviteId = inviteId
            )
            viewModelScope.launch(Dispatchers.IO) {
                val chatExists = chatDao.chatExists(myId, enemyId)
                addAndProcessPersonalChat(personalChat, chatExists)
            }
        }
        messageTypeJsonSender(theMessageId, theExecution, jsonLambda)
    }

    fun createGistForFriend(
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

    fun sendBinary(
        data: ByteArray,
        type: PersonalMessageType,
        enemyId: Int,
        messageId: String,
        myId: Int,
        fullName: String,
        context: Context,
        isRetry: Boolean = false
    ) {
        val prefix = "${type.typeString}:$enemyId:$messageId:$myId:$fullName"
        val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
        val outputStream = ByteArrayOutputStream()
        outputStream.write(ByteBuffer.allocate(4).putInt(prefixBytes.size).array())
        outputStream.write(prefixBytes)
        outputStream.write(data)
        val message = outputStream.toByteArray()
        WebSocketManager.sendBinary(BinaryBufferObject(data = message))
        if (!isRetry) {
            val mediaUri = when (type) {
                PersonalMessageType.P_IMAGE -> FileUtils.saveImage(context, data)
                PersonalMessageType.P_VIDEO -> FileUtils.saveVideo(context, data)
                PersonalMessageType.P_AUDIO -> FileUtils.saveAudio(context, data)
                else -> null
            }
            val readableTimeStamp = getReadableTimestamp()
            val personalChat = PersonalChat(
                messageId = messageId,
                enemyId = enemyId,
                myId = myId,
                content = "",
                status = PersonalMessageStatus.PENDING,
                messageType = type,
                timeStamp = readableTimeStamp,
                mediaUri = mediaUri?.toString()
            )
            viewModelScope.launch {
                val chatExists = chatDao.chatExists(myId, enemyId)
                addAndProcessPersonalChat(personalChat, chatExists)
            }
        }
    }

    suspend fun fetchRelevantIds(): List<Int> = withContext(Dispatchers.IO) {
        chatDao.getAllChats(SessionManager.customerId.value).map { it.enemyId }.distinct()
    }

    fun handleRecordedAudio(
        file: File,
        enemyId: Int,
        myId: Int,
        fullName: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val audioByteArray = FileUtils.fileToByteArray(file)
                val messageId = generateMessageId()
                sendBinary(
                    audioByteArray,
                    PersonalMessageType.P_AUDIO,
                    enemyId,
                    messageId,
                    myId,
                    fullName,
                    context
                )

            } catch (e: IOException) {
                Log.e("ChatRoom", "Error processing audio: ${e.message}", e)
            }
        }
    }

    fun sendTextMessage(
        message: String,
        enemyId: Int,
        myId: Int,
        theMessageId: String? = null
    ) {
        val jsonLambda = { messageId: String ->
            val json = buildJsonObject {
                put("type", PersonalMessageType.P_TEXT.typeString)
                put("enemyId", enemyId)
                put("content", message)
                put("messageId", messageId)
            }
            json.toString()
        }

        val executionLambda: (String) -> Unit = { messageId ->
            val readableTimeStamp = getReadableTimestamp()
            val personalChat = PersonalChat(
                messageId = messageId,
                enemyId = enemyId,
                myId = myId,
                content = message,
                status = PersonalMessageStatus.PENDING,
                messageType = PersonalMessageType.P_TEXT,
                timeStamp = readableTimeStamp
            )
            viewModelScope.launch(Dispatchers.IO) {
                val chatExists = chatDao.chatExists(myId, enemyId)
                addAndProcessPersonalChat(personalChat, chatExists)
            }
        }
        messageTypeJsonSender(theMessageId, executionLambda, jsonLambda)
    }

    private fun reinsertUpdatedChatEntry(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            loggerD("AckChecker"){"REINSERTED CALLED: Session.${SessionManager.customerId.value}"}
            val latestMessage = personalChatDao.getLatestMessageBetween(SessionManager.customerId.value, enemyId)
            val profileData = chatDao.getChatByEnemyId(enemyId)

            if (latestMessage == null) {
                loggerD("AckChecker") { "No latest message found for $enemyId, skipping update. ${SessionManager.customerId.value}" }
                return@launch
            } else {
                loggerD("AckChecker") { "Latest message found for $enemyId: ${latestMessage.content}" }
            }

            val chatToInsertOrUpdate = ChatList(
                enemyId = enemyId,
                contactName = profileData?.contactName ?: "Your NewFriend",
                lastMsg = latestMessage.displayableContent(),
                lastMsgAddTime = latestMessage.timeStamp,
                profilePhoto = profileData?.profilePhoto ?: "",
                myId = SessionManager.customerId.value,
                unreadMsgCounter = profileData?.unreadMsgCounter ?: 0,
                isVerified = profileData?.isVerified ?: false
            )
            chatDao.updateLastMessage(enemyId, latestMessage.displayableContent(), latestMessage.timeStamp)

            withContext(Dispatchers.Main) {
                _chats.update { current ->
                    current.toMutableList().apply {
                        removeAll { it.enemyId == enemyId }
                        add(chatToInsertOrUpdate)
                        sortByDescending { it.lastMsgAddTime }
                    }
                }
            }
        }
    }

    fun searchChats(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (query.isNotEmpty()) {
                val result = chatDao.searchChats(SessionManager.customerId.value, "%$query%")
                _searchResults.value = result
            }
        }
    }

    fun addMessageToForward(message: PersonalChat) {
        _messagesToForward.value = _messagesToForward.value?.plus(message)
    }

    fun clearMessagesToForward() {
        _messagesToForward.value = emptyList()
    }

    fun forwardMessagesToRecipients(recipients: List<Int>, myId: Int, context: Context) {
        _messagesToForward.value?.let { messages ->
            recipients.forEach { enemyId ->
                messages.forEach { message ->
                    val newMessageId = generateMessageId()
                    when (message.messageType) {
                        PersonalMessageType.P_TEXT -> sendTextMessage(
                            message.content,
                            enemyId,
                            myId
                        )

                        PersonalMessageType.P_AUDIO, PersonalMessageType.P_VIDEO, PersonalMessageType.P_IMAGE -> {
                            message.mediaUri?.let { uri ->
                                val data = FileUtils.loadFileAsByteArray(context, Uri.parse(uri))
                                data?.let {
                                    sendBinary(
                                        it,
                                        message.messageType,
                                        enemyId,
                                        newMessageId,
                                        myId,
                                        "MyName",
                                        context
                                    )
                                }
                            }
                        }

                        PersonalMessageType.P_GIST_INVITE -> message.gistId?.let {
                            sendGistInvite(
                                message.content,
                                it, enemyId, myId
                            )
                        }

                        PersonalMessageType.P_GIST_CREATION -> message.inviteId?.let {
                            sendGistCreation(
                                message.inviteId,
                                enemyId,
                                myId,
                                messageContent = message.content
                            )
                        }
                    }
                }
            }
            clearMessagesToForward()
        }
    }

    fun sendGistInvite(
        topic: String,
        gistId: String,
        enemyId: Int,
        myId: Int,
        theMessageId: String? = null
    ) {
        val jsonLambda = { messageId: String ->
            val json = buildJsonObject {
                put("type", "PGistInvite")
                put("enemyId", enemyId)
                put("content", topic)
                put("gistId", gistId)
                put("messageId", messageId)
            }
            json.toString()
        }
        val theExecution: (String) -> Unit = { messageId ->
            val readableTimeStamp = getReadableTimestamp()
            val personalChat = PersonalChat(
                messageId = messageId,
                enemyId = enemyId,
                myId = myId,
                content = topic,
                status = PersonalMessageStatus.PENDING,
                messageType = PersonalMessageType.P_GIST_INVITE,
                timeStamp = readableTimeStamp,
                mediaUri = null,
                gistId = gistId,
                topic = topic
            )
            viewModelScope.launch(Dispatchers.IO) {
                val chatExists = chatDao.chatExists(myId, enemyId)
                addAndProcessPersonalChat(personalChat, chatExists)
            }
        }
        messageTypeJsonSender(theMessageId, theExecution, jsonLambda)
    }

    private fun messageTypeJsonSender(
        theMessageId: String? = null,
        theFunction: (String) -> Unit,
        json: (String) -> String
    ) {
        val theSpecificMessageId = theMessageId ?: generateMessageId()
        if (theMessageId == null) {
            theFunction(theSpecificMessageId)
        }
        val finalJson = BufferObject(message = json(theSpecificMessageId))
        send(finalJson)
    }

    private fun send(message: BufferObject) {
        WebSocketManager.send(message)
    }

    fun addGistInviteToForward(topic: String, gistId: String) {
        val messageId = generateMessageId()
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = 0,
            myId = SessionManager.customerId.value,
            content = topic,
            status = PersonalMessageStatus.PENDING,
            messageType = PersonalMessageType.P_GIST_INVITE,
            timeStamp = System.currentTimeMillis().toString(),
            mediaUri = null,
            gistId = gistId,
            topic = topic
        )
        val currentMessages = _messagesToForward.value?.toMutableList() ?: mutableListOf()
        currentMessages.add(personalChat)
        _messagesToForward.value = currentMessages
    }

    private fun sendDeliveryAcknowledgment(messageId: String) {
        loggerD("AckCheck"){"Delivery ack sending"}
        val acknowledgmentJson = """
        {
            "type": "myIdDeliveryAck",
            "messageId": "$messageId"
        }
    """.trimIndent()
        send(BufferObject(type = WsDataType.PersonalMessageType, message = acknowledgmentJson))
    }

    override fun onCleared() {
        super.onCleared()
        loggerD("fetchUndelivered") { "fetchUndelivered cleared" }
    }

    private fun gatherMessages() {
        viewModelScope.launch {
            ChatVMObject.messagesArray.collect { newValue ->
                loggerD("fetchUndelivere") { "newValue: $newValue" }
                if (newValue.first) {
                    processHttpResponse(newValue)
                } else {
                    loggerD("fetchUndelivered") { "Error: ${newValue.third}, ${newValue.second}" }
                }
            }
        }
    }

    private suspend fun processHttpResponse(response: networkTriple) {
        loggerD("fetchUndelivered") { "Response: $response" }

        val messagesArray = JSONArray(response.second)

        for (i in 0 until messagesArray.length()) {
            val jsonMessage = messagesArray.getJSONObject(i)
            val messageId = jsonMessage.optString("messageId", "")
            val parsedMessage = if (messageId.isNotEmpty()) {
                maybeParseAndReturnMessage(messageId, jsonMessage)
            } else null

            parsedMessage?.let { message ->
                val shouldAck = message.messageType !in listOf(
                    PersonalMessageType.P_IMAGE,
                    PersonalMessageType.P_AUDIO,
                    PersonalMessageType.P_VIDEO
                ) || (message.mediaUri?.startsWith("http") == false)

                handleIncomingHttpCaller(message) {
                    if (shouldAck) {
                        AckBatcher.add(messageId)
                    }
                }
            }
        }
    }

    private suspend fun maybeParseAndReturnMessage(
        messageId: String,
        json: JSONObject
    ): PersonalChat? {
        if (processingMediaMessages.contains(messageId)) return null

        val dbMessage = personalChatDao.getPersonalChatById(messageId)
        if (dbMessage != null) {
            if (dbMessage.mediaUri?.startsWith("http") == false) {
                return dbMessage
            }
        }
        return try {
            parsePersonalChatFromJson(json)
        } finally {
            processingMediaMessages.remove(messageId)
        }
    }

    /**
     * Suspend helper that parses a JSONObject into a PersonalChat and,
     * for media messages, downloads the media file and saves it locally.
     *
     * @param jsonObject The JSONObject containing the message data.
     * @param defaultStatus An optional status override.
     * @return A PersonalChat instance or null if the type is unrecognized.
     */
    private suspend fun parsePersonalChatFromJson(
        jsonObject: JSONObject,
        defaultStatus: PersonalMessageStatus? = null
    ): PersonalChat? {
        val typeString = jsonObject.optString("type", "")
        val messageId = jsonObject.optString("messageId", "")
        val enemyId = jsonObject.optInt("enemyId", 0)
        val myId = jsonObject.optInt("myId", 0)
        val content = jsonObject.optString("content", "")
        val timestamp = extractNumberLong(jsonObject)
        val messageType = when (typeString) {
            PersonalMessageType.P_TEXT.typeString -> PersonalMessageType.P_TEXT
            PersonalMessageType.P_IMAGE.typeString -> PersonalMessageType.P_IMAGE
            PersonalMessageType.P_AUDIO.typeString -> PersonalMessageType.P_AUDIO
            PersonalMessageType.P_VIDEO.typeString -> PersonalMessageType.P_VIDEO
            PersonalMessageType.P_GIST_INVITE.typeString -> PersonalMessageType.P_GIST_INVITE
            PersonalMessageType.P_GIST_CREATION.typeString -> PersonalMessageType.P_GIST_CREATION
            else -> PersonalMessageType.P_TEXT
        }
        val status = defaultStatus ?: when (messageType) {
            PersonalMessageType.P_TEXT, PersonalMessageType.P_GIST_INVITE, PersonalMessageType.P_GIST_CREATION -> PersonalMessageStatus.SENT
            else -> PersonalMessageStatus.DELIVERED
        }
        if (messageType == PersonalMessageType.P_IMAGE ||
            messageType == PersonalMessageType.P_AUDIO ||
            messageType == PersonalMessageType.P_VIDEO
        ) {
            if (processingMediaMessages.contains(messageId)) {
                loggerD("AckCheck") { "Already processing message $messageId" }
                return null
            } else {
                processingMediaMessages.add(messageId)
                try {
                    val downloadedData = withContext(Dispatchers.IO) {
                        downloadFileFromUrl(content)
                    }
                    val mediaUri: String? = if (downloadedData != null) {
                        when (messageType) {
                            PersonalMessageType.P_IMAGE -> withContext(Dispatchers.IO) {
                                FileUtils.saveMedia(
                                    appContext,
                                    downloadedData,
                                    MediaType.IMAGE,
                                    toPublic = true
                                )
                            }

                            PersonalMessageType.P_VIDEO -> withContext(Dispatchers.IO) {
                                FileUtils.saveMedia(
                                    appContext,
                                    downloadedData,
                                    MediaType.VIDEO,
                                    toPublic = true
                                )
                            }

                            PersonalMessageType.P_AUDIO -> withContext(Dispatchers.IO) {
                                FileUtils.saveMedia(
                                    appContext,
                                    downloadedData,
                                    MediaType.AUDIO,
                                    toPublic = false
                                )
                            }

                            else -> null
                        }?.toString() ?: content
                    } else {
                        content
                    }
                    return PersonalChat(
                        messageId = messageId,
                        enemyId = enemyId,
                        myId = myId,
                        content = content,
                        status = status,
                        messageType = messageType,
                        timeStamp = timestamp,
                        mediaUri = mediaUri
                    )
                } finally {
                    processingMediaMessages.remove(messageId)
                }
            }
        } else {
            return when (messageType) {
                PersonalMessageType.P_TEXT -> PersonalChat(
                    messageId = messageId,
                    enemyId = enemyId,
                    myId = myId,
                    content = content,
                    status = status,
                    messageType = messageType,
                    timeStamp = timestamp,
                    mediaUri = null
                )

                PersonalMessageType.P_GIST_INVITE -> PersonalChat(
                    messageId = messageId,
                    enemyId = enemyId,
                    myId = myId,
                    content = content,
                    status = status,
                    messageType = messageType,
                    timeStamp = timestamp,
                    gistId = jsonObject.optString("gistId", ""),
                    topic = content,
                    mediaUri = null
                )

                PersonalMessageType.P_GIST_CREATION -> PersonalChat(
                    messageId = messageId,
                    enemyId = enemyId,
                    myId = myId,
                    content = content,
                    status = status,
                    messageType = messageType,
                    timeStamp = timestamp,
                    inviteId = jsonObject.optString("inviteId", ""),
                    mediaUri = null
                )

                else -> {
                    PersonalChat(
                        messageId = messageId,
                        enemyId = enemyId,
                        myId = myId,
                        content = content,
                        status = status,
                        messageType = messageType,
                        timeStamp = timestamp,
                        mediaUri = null
                    )
                }
            }
        }
    }
}

class ChatViewModelFactory(
    private val chatDao: ChatDao, private val personalChatDao: PersonalChatDao,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ChatScreenViewModel(
                chatDao,
                personalChatDao,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class ProfileUpdateData(
    val customerId: Int,
    val contactName: String,
    val profilePhoto: String,
    val isVerified: Boolean = false
)

enum class NetworkType {
    HTTP,
    WEBSOCKET
}