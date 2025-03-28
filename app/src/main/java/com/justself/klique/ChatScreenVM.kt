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

    val myUserId = MutableStateFlow(0)

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
        retryPendingMessages(getApplication<Application>().applicationContext)
        loadChats(myUserId.value)
        gatherMessages()
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
                    println("Invalid user Id in json: $jsonObject")
                }
            }

            PrivateChatReceivingType.P_TEXT -> {
                Log.d("Websocket", "PText Function called")
                viewModelScope.launch {
                    val newMessage = parsePersonalChatFromJson(jsonObject)
                    Log.d("Websocket", "Parsed text message: $newMessage")
                    if (newMessage != null) {
                        handleIncomingWebsocketCaller(newMessage)
                    }
                }
            }

            // Handle media messages (P_IMAGE, P_AUDIO, P_VIDEO)
            PrivateChatReceivingType.P_IMAGE,
            PrivateChatReceivingType.P_AUDIO,
            PrivateChatReceivingType.P_VIDEO -> {
                val messageId = jsonObject.optString("messageId", "")
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        // Retrieve an existing message from the DB
                        val existingMessage = personalChatDao.getPersonalChatById(messageId)
                        if (existingMessage != null && existingMessage.mediaUri?.startsWith("http") == false) {
                            // The message is already downloaded (mediaUri is local), so use it directly.
                            handleIncomingWebsocketCaller(existingMessage)
                        } else {
                            // Either no message exists or the media download previously failed (mediaUri starts with "http")
                            // In that case, attempt to process the message (which includes downloading the media)
                            val newMessage = parsePersonalChatFromJson(jsonObject)
                            Log.d("Websocket", "Parsed media message: $newMessage")
                            if (newMessage != null) {
                                handleIncomingWebsocketCaller(newMessage)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Websocket", "Error processing media message: ${e.message}")
                    } finally {
                        processingMediaMessages.remove(messageId)
                    }
                }
            }

            // Handle gist invite messages
            PrivateChatReceivingType.P_GIST_INVITE -> {
                Log.d("Websocket", "PGistInvite Function called")
                viewModelScope.launch {
                    val newMessage = parsePersonalChatFromJson(jsonObject)
                    Log.d("Websocket", "Parsed gist invite: $newMessage")
                    if (newMessage != null) {
                        handleIncomingWebsocketCaller(newMessage)
                    }
                }
            }

            // Handle gist creation messages
            PrivateChatReceivingType.P_GIST_CREATION -> {
                Log.d("Websocket", "PGistCreation Function called")
                viewModelScope.launch {
                    val newMessage = parsePersonalChatFromJson(jsonObject)
                    Log.d("Websocket", "Parsed gist creation: $newMessage")
                    if (newMessage != null) {
                        handleIncomingWebsocketCaller(newMessage)
                    }
                }
            }

            PrivateChatReceivingType.ACK -> {
                val statusString = jsonObject.getString("status")
                Log.d("RawWebsocket", statusString)

                val status = when (statusString) {
                    "delivered" -> PersonalMessageStatus.DELIVERED
                    "sent" -> PersonalMessageStatus.SENT
                    else -> PersonalMessageStatus.SENT
                }

                val messageId = jsonObject.optString("messageId")
                val timestamp = extractNumberLong(jsonObject)

                viewModelScope.launch(Dispatchers.IO) {
                    personalChatDao.updateStatus(messageId, status)
                    Log.d("ForStatus", "called with: $status; timestamp: $timestamp")
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
                Log.d("PProfileUpdate", "called")
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
            Log.e("PersonalChats", "Failed to download file from URL: $theFormattedUrl", e)
            null
        }
    }

    private fun extractNumberLong(jsonObject: JSONObject): String {
        return jsonObject.optJSONObject("timestamp")
            ?.optJSONObject("\$date")
            ?.optString("\$numberLong", null) ?: Instant.now().toEpochMilli().toString()
    }

    private fun getReadableTimestamp(instant: Instant = Instant.now()): String {
        return instant.toEpochMilli().toString()
    }

    fun checkContactUpdate() {
        val message = """
            {
            "type": "checkContactUpdate"
            }
        """.trimIndent()
        send(message)
    }

    fun deleteChat(enemyId: Int, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val messages = personalChatDao.getPersonalChatsByEnemyId(myUserId.value, enemyId)
            messages.forEach { message ->
                message.mediaUri?.let { deleteMediaFile(context, it) }
            }
            personalChatDao.deletePersonalChatsForEnemy(myId = myUserId.value, enemyId = enemyId)
            chatDao.deleteChat(enemyId)
            loadChats(myUserId.value)
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
            Log.d("ChatList", "Fetched chat list: $chatList")

            _chats.value = chatList
            if (updateSearchResults) {
                Log.d("ChatList", "Updating searchResults with: $chatList")
                _searchResults.value = chatList
            }
            var unread = 0
            for (chat in _chats.value) {
                if (chat.unreadMsgCounter > 0) {
                    unread += chat.unreadMsgCounter
                }
            }
            GlobalEventBus.updateUnreadMessageCount(unread)
            Log.d("ChatList", "_chats value after update: ${_chats.value}")
            Log.d("ChatList", "_searchResults value after update: ${_searchResults.value}")
        }
    }

    fun setMyUserId(userId: Int) {
        myUserId.value = userId
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
        send(subscribeMessage)
    }

    private fun unsubscribeFromOnlineStatus(enemyId: Int) {
        val unsubscribeMessage = """
            {
                "type": "unsubscribeOnlineStatus",
                "enemyId": $enemyId
            }
        """.trimIndent()
        send(unsubscribeMessage)
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
    private fun handleIncomingWebsocketCaller(message: PersonalChat){
        handleIncomingPersonalMessage(message, NetworkType.WEBSOCKET)
    }
    private fun handleIncomingHttpCaller(message: PersonalChat){
        handleIncomingPersonalMessage(message, NetworkType.HTTP)
    }

    private fun handleIncomingPersonalMessage(newMessage: PersonalChat, network: NetworkType) {
        Log.d("Incoming", "newMessage is $newMessage")
        viewModelScope.launch(Dispatchers.IO) {
            val chatExists = checkChatExistsSync(newMessage.myId, newMessage.enemyId)
            Log.d("Websocket", "Is New Incoming is ${!chatExists}")
            val messageExists = personalChatDao.getPersonalChatById(newMessage.messageId) != null
            val acknowledgement = when (network) {
                NetworkType.WEBSOCKET -> {
                    if (newMessage.mediaUri?.startsWith("http") == true) {
                        { /* no-op: do not send ack so external URL remains valid */ }
                    } else {
                        { sendDeliveryAcknowledgment(newMessage.messageId) }
                    }
                }
                NetworkType.HTTP -> {{}
                }
            }
            if (!messageExists) {
                addAndProcessPersonalChat(newMessage, chatExists, acknowledgement)
            }
        }
    }

    private fun updateChatListWithNewMessage(newMessage: PersonalChat, chatExists: Boolean) {
        val enemyId = if (newMessage.myId == myUserId.value) newMessage.enemyId else newMessage.myId
        val lastMsg = when (newMessage.messageType) {
            PersonalMessageType.P_IMAGE -> "Photo"
            PersonalMessageType.P_VIDEO -> "Video"
            PersonalMessageType.P_AUDIO -> "Audio"
            PersonalMessageType.P_GIST_INVITE -> "Gist Invite..."
            PersonalMessageType.P_TEXT -> newMessage.content
            PersonalMessageType.P_GIST_CREATION -> "Gist Creation..."
        }
        val timeStamp = newMessage.timeStamp
        Log.d("SpecialChat", "${_currentChat.value} and $enemyId: ${newMessage.content}")
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("SpecialChat", "${_currentChat.value} and $enemyId: ${newMessage.content} 2")
            if (_currentChat.value == enemyId) {
                val updatedList = _personalChats.value.toMutableList()
                // Only insert if the message isn't already present
                if (updatedList.none { it.messageId == newMessage.messageId }) {
                    val smallListThreshold = 400
                    if (updatedList.size < smallListThreshold) {
                        // For small lists, simply add and sort
                        updatedList.add(newMessage)
                        updatedList.sortByDescending { it.timeStamp.toLongOrNull() ?: 0L }
                    } else {
                        // For large lists, use binary search to insert at the correct position
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
                myId = myUserId.value,
                unreadMsgCounter = if (_currentChat.value != enemyId && newMessage.myId != myUserId.value) 1 else 0
            )
            if (!chatExists) {
                viewModelScope.launch(Dispatchers.Main) {
                    chatDao.addChat(chat)
                    sendJsonToUpdateProfile(listOf(enemyId))
                    loadChats(myUserId.value)
                }
                Log.d("isNewChat", "add chat called with $chat")
            } else {
                Log.d("isNewChat", "add chat not called with $chat")
                chatDao.updateLastMessage(
                    enemyId = enemyId,
                    lastMsg = lastMsg,
                    timeStamp = timeStamp
                )
            }
            if (newMessage.enemyId == myUserId.value) {
                updatePopUp(newMessage.myId, lastMsg)
            }
            val updatedChats = getSortedChats(myUserId.value)
            _chats.value = updatedChats
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
        viewModelScope.launch(Dispatchers.IO) {
            val name = chatDao.getChatByEnemyId(enemyId)?.contactName ?: "Your NewFriend"
            val data = SnackBarMessageData(name, message, enemyId)
            GlobalEventBus.sendSnackBarMessage(data)
        }
    }

    fun sendJsonToUpdateProfile(userIds: List<Int>) {
        val jsonObject = JSONObject().apply {
            put("type", "handleMultipleProfileUpdates")
            put("userIds", JSONArray(userIds))
        }.toString()
        send(jsonObject)
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
        Log.d(
            "PersonalChat",
            "addAndProcessPersonalChat called with messageId: ${personalChat.messageId}"
        )
        viewModelScope.launch(Dispatchers.IO) {
            val enemyId =
                if (personalChat.myId == myUserId.value) personalChat.enemyId else personalChat.myId
            if (_currentChat.value != enemyId) {
                Log.d("currentChat", "Current chat is ${_currentChat.value}")
                val existingMessage = personalChatDao.getPersonalChatById(personalChat.messageId)
                Log.d("currentChat", "Existing message is $existingMessage")
                val notExists = existingMessage == null
                val notSelfSent = personalChat.myId != myUserId.value
                if (notExists && notSelfSent) {
                    Log.d(
                        "PersonalChat",
                        "incrementing unread msg counter: ${personalChat.myId}, ${personalChat.enemyId}, ${myUserId.value}"
                    )
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
        Log.d("Database", "load more: $loadMore")
        if (_isLoading.value == true) return

        _isLoading.value = true
        Log.d("Database", "Still executing")
        viewModelScope.launch(Dispatchers.IO) {
            val personalChatList = if (loadMore) {
                lastMessageId?.let { theLastMessageId ->
                    Log.d("LoadMore", "called, last MessageId: $theLastMessageId")
                    personalChatDao.getPersonalChatsBefore(
                        myId,
                        enemyId,
                        theLastMessageId,
                        pageSize
                    )
                        .sortedByDescending { it.timeStamp }
                }
            } else {
                Log.d("Database", "this one called")
                personalChatDao.getInitialPersonalChats(myId, enemyId, pageSize)
            }
            if (personalChatList != null) {
                Log.d(
                    "MessageLoading",
                    "Loaded ${personalChatList.size} messages, first message timestamp: ${personalChatList.firstOrNull()?.timeStamp}, last message timestamp: ${personalChatList.lastOrNull()?.timeStamp}"
                )
            }
            if (loadMore) {
                Log.d("PersonalChat", "Still even executing")
                val currentList = _personalChats.value.toMutableList()
                if (personalChatList != null) {
                    currentList.addAll(personalChatList)
                }
                withContext(Dispatchers.Main) {
                    _personalChats.value = currentList
                }
                Log.d("Database", "Still even even executing")
                Log.d("Database", "Current list size: ${currentList.size}")
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
        val jsonData = """
            {
            "type": "fetchUndelivered"
            }
        """.trimIndent()
        send(jsonData)
    }

    fun generateMessageId(): String {
        return UUID.randomUUID().toString()
    }

    // Provision should be made and thought of, on how such a message should be received
    private fun updateProfile(
        enemyId: Int,
        contactName: String,
        profilePhoto: String,
        isVerified: Boolean
    ) {
        Log.d("PProfileUpdate", "called")
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
        WebSocketManager.sendBinary(message)
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
        Log.d(
            "sendBinaryInputs",
            "Data: ${data.size}, Type: $type, EnemyId: $enemyId, MessageId: $messageId, MyId: $myId, FullName: $fullName"
        )
    }

    suspend fun fetchRelevantIds(): List<Int> = withContext(Dispatchers.IO) {
        chatDao.getAllChats(myUserId.value).map { it.enemyId }.distinct()
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
                Log.d("ChatRoom", "Audio Byte Array: ${audioByteArray.size} bytes")

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
            val messageJson = """
            {
                "type": "${PersonalMessageType.P_TEXT.typeString}",
                "enemyId": $enemyId,
                "content": "${
                message.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    .replace("\b", "\\b")
            }",
                "messageId": "$messageId"
            }
        """.trimIndent()
            messageJson
        }

        // Lambda for additional processing if no message ID was provided (i.e. creating a PersonalChat)
        val executionLambda: (String) -> Unit = { messageId ->
            Log.d("PersonalChat", "Message Id causing crash?: $messageId")
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

        // Use the common wrapper to handle message ID generation, extra processing, JSON creation, and sending.
        messageTypeJsonSender(theMessageId, executionLambda, jsonLambda)
    }

    fun searchChats(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (query.isNotEmpty()) {
                val result = chatDao.searchChats(myUserId.value, "%$query%")
                Log.d("ChatList", "Search result for query '$query': $result")
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
                                    Log.d(
                                        "ForwardedMessage",
                                        "Forwarding messageId: $newMessageId, type: ${message.messageType}, enemyId: $enemyId, timeStamp: ${System.currentTimeMillis()}"
                                    )
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
            val gistInviteJson = """
        {
        "type": "PGistInvite",
        "enemyId": $enemyId,
        "content": "${
                topic.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                    .replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b")
            }",
        "gistId": "$gistId",
        "messageId": "$messageId"
        }
    """.trimIndent()
            gistInviteJson
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
        val finalJson = json(theSpecificMessageId)
        send(finalJson)
    }

    // Remember to implement an onMessage to attach people to gists
    fun joinGist(gistId: String) {
        val joinGistJson = """
            {
            "type": "joinGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(joinGistJson)
        Log.d("Join Gist", "Join gist id is $gistId")
    }

    private fun send(message: String) {
        WebSocketManager.send(message)
        Log.d("send", message)
    }

    fun addGistInviteToForward(topic: String, gistId: String) {
        val messageId = generateMessageId()
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = 0,
            myId = myUserId.value,
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
        val acknowledgmentJson = """
        {
            "type": "myIdDeliveryAck",
            "messageId": "$messageId"
        }
    """.trimIndent()
        Log.d("Websocket", "Sending $acknowledgmentJson")
        send(acknowledgmentJson)
    }
    private fun gatherMessages(){
        viewModelScope.launch {
            ChatVMObject.messagesArray.collect { newValue ->
                if (newValue.third > 0){
                    processHttpResponse(newValue)
                }
            }
        }
    }
    suspend fun fetchUndeliveredViaHttp(){
        ChatVMObject.fetchUndeliveredMessages{ responseTriple ->
            processHttpResponse(responseTriple)
        }
    }
    private suspend fun processHttpResponse(response: networkTriple) {
        val messagesArray = JSONArray(response.second)
        val processedIds = mutableListOf<String>()
        for (i in 0 until messagesArray.length()) {
            val jsonMessage = messagesArray.getJSONObject(i)
            val messageId = jsonMessage.optString("messageId")
            val parsedMessage = parsePersonalChatFromJson(jsonMessage)
            if (parsedMessage != null) {
                handleIncomingHttpCaller(parsedMessage)
                if (parsedMessage.messageType !in listOf(
                        PersonalMessageType.P_IMAGE,
                        PersonalMessageType.P_AUDIO,
                        PersonalMessageType.P_VIDEO
                    ) || (parsedMessage.mediaUri?.startsWith("http") == false)
                ) {
                    processedIds.add(messageId)
                } else {
                    Log.e("FetchUndelivered", "Media download failed for message $messageId; not acknowledging.")
                }
            }
        }
        acknowledgeMessages(processedIds)
    }

    private suspend fun acknowledgeMessages(messageIds: List<String>) {
        val jsonBody = JSONObject().apply {
            put("messageIds", JSONArray(messageIds))
        }.toString()
        val (isSuccessful, _, code) = NetworkUtils.makeJwtRequest(
            endpoint = "acknowledgeMessages",
            method = KliqueHttpMethod.POST,
            params = emptyMap(),
            jsonBody = jsonBody
        )
        if (isSuccessful) {
            Log.d("AckMessages", "Successfully acknowledged messages.")
        } else {
            Log.e("AckMessages", "Failed to acknowledge messages: $code")
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

        // Map the type string to PersonalMessageType.
        val messageType = when (typeString) {
            PersonalMessageType.P_TEXT.typeString -> PersonalMessageType.P_TEXT
            PersonalMessageType.P_IMAGE.typeString -> PersonalMessageType.P_IMAGE
            PersonalMessageType.P_AUDIO.typeString -> PersonalMessageType.P_AUDIO
            PersonalMessageType.P_VIDEO.typeString -> PersonalMessageType.P_VIDEO
            PersonalMessageType.P_GIST_INVITE.typeString -> PersonalMessageType.P_GIST_INVITE
            PersonalMessageType.P_GIST_CREATION.typeString -> PersonalMessageType.P_GIST_CREATION
            else -> PersonalMessageType.P_TEXT
        }

        // Use the provided status or default based on message type.
        val status = defaultStatus ?: when (messageType) {
            PersonalMessageType.P_TEXT -> PersonalMessageStatus.SENT
            else -> PersonalMessageStatus.DELIVERED
        }

        // If this is a media message, incorporate the prevention logic.
        if (messageType == PersonalMessageType.P_IMAGE ||
            messageType == PersonalMessageType.P_AUDIO ||
            messageType == PersonalMessageType.P_VIDEO) {

            // Check if this message is already being processed.
            if (processingMediaMessages.contains(messageId)) {
                return null
            } else {
                // Mark this message as being processed.
                processingMediaMessages.add(messageId)
                try {
                    // Download the media data.
                    val downloadedData = withContext(Dispatchers.IO) {
                        downloadFileFromUrl(content)
                    }
                    // If download succeeds, save the file locally; otherwise, fall back.
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
                        // Download failed, so we fall back to the original URL.
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
                    // Always remove the message ID from processing.
                    processingMediaMessages.remove(messageId)
                }
            }
        } else {
            // Non-media messages: return the appropriate PersonalChat.
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
                else -> { // Should not reach here for media messages.
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
    val isVerified: Boolean = false // New Boolean property
)
enum class NetworkType {
    HTTP,
    WEBSOCKET
}