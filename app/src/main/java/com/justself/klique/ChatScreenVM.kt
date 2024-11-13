package com.justself.klique

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
import java.util.UUID

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

    private val _isNewChat = MutableStateFlow<Boolean>(false)
    private val _isNewIncomingMessage = MutableStateFlow(false)
    private val _selectedMessages = MutableLiveData<List<String>>(emptyList())
    val selectedMessages: LiveData<List<String>> get() = _selectedMessages

    private val _isSelectionMode = MutableLiveData<Boolean>(false)
    val isSelectionMode: LiveData<Boolean> get() = _isSelectionMode
    private val _pageSize = 20
    private var _currentPage = 0
    private var _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        WebSocketManager.registerListener(this)
        retryPendingMessages(getApplication<Application>().applicationContext)
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
                // Extract fields from the JSON object
                val messageId = jsonObject.optString("messageId", "")
                val enemyId = jsonObject.optInt("enemyId", 0)
                val myId = jsonObject.optInt("myId", 0)
                val content = jsonObject.optString("content", "")
                val status = PersonalMessageStatus.SENT
                val messageType = PersonalMessageType.P_TEXT
                val timeStamp = System.currentTimeMillis()


                // Create a PersonalChat object
                val newMessage = PersonalChat(
                    messageId = messageId,
                    enemyId = enemyId,
                    myId = myId,
                    content = content,
                    status = status,
                    messageType = messageType,
                    timeStamp = timeStamp.toString(),
                )

                // Call the function to handle the new message
                Log.d("Websocket", "handleIncoming Function called with $newMessage")
                handleIncomingPersonalMessage(newMessage)
            }

            PrivateChatReceivingType.P_IMAGE, PrivateChatReceivingType.P_AUDIO, PrivateChatReceivingType.P_VIDEO -> {
                val messageId = jsonObject.optString("messageId", "")
                val enemyId = jsonObject.optInt("enemyId", 0)
                val myId = jsonObject.optInt("myId", 0)
                val contentUrl =
                    jsonObject.optString("content", "") // This is the URL for the media
                val extractedMessageType = jsonObject.optString("type", "")
                val messageType = when (extractedMessageType) {
                    PersonalMessageType.P_IMAGE.typeString -> PersonalMessageType.P_IMAGE
                    PersonalMessageType.P_AUDIO.typeString -> PersonalMessageType.P_AUDIO
                    PersonalMessageType.P_VIDEO.typeString -> PersonalMessageType.P_VIDEO
                    else -> PersonalMessageType.P_IMAGE
                }
                val timeStamp = System.currentTimeMillis().toString()

                CoroutineScope(Dispatchers.IO).launch {
                    val messageExists = personalChatDao.getPersonalChatById(messageId) != null

                    if (messageExists) {
                        sendDeliveryAcknowledgment(messageId)
                        Log.d("Websocket", "Message already exists, acknowledgment sent.")
                    } else {
                        try {
                            val downloadedData = downloadFileFromUrl(contentUrl)
                            if (downloadedData != null) {
                                val context = getApplication<Application>().applicationContext
                                val mediaUri = when (messageType) {
                                    PersonalMessageType.P_IMAGE -> FileUtils.saveMedia(
                                        context,
                                        downloadedData,
                                        MediaType.IMAGE,
                                        toPublic = true
                                    )

                                    PersonalMessageType.P_VIDEO -> FileUtils.saveMedia(
                                        context,
                                        downloadedData,
                                        MediaType.VIDEO,
                                        toPublic = true
                                    )

                                    PersonalMessageType.P_AUDIO -> FileUtils.saveMedia(
                                        context,
                                        downloadedData,
                                        MediaType.AUDIO,
                                        toPublic = true
                                    )

                                    else -> null
                                }
                                val newMessage = PersonalChat(
                                    messageId = messageId,
                                    enemyId = enemyId,
                                    myId = myId,
                                    content = contentUrl,
                                    status = PersonalMessageStatus.DELIVERED,
                                    messageType = messageType,
                                    timeStamp = timeStamp,
                                    mediaUri = mediaUri?.toString()
                                )
                                handleIncomingPersonalMessage(newMessage)

                                sendDeliveryAcknowledgment(messageId)
                                Log.d(
                                    "Websocket",
                                    "Media saved and acknowledgment sent for $messageId"
                                )
                            } else {
                                Log.e("Websocket", "Failed to download media from URL: $contentUrl")
                            }
                        } catch (e: Exception) {
                            Log.e("Websocket", "Error processing media message: ${e.message}")
                        }
                    }
                }
            }

            PrivateChatReceivingType.P_GIST_INVITE -> {
                Log.d("Websocket", "PGistInvite Function called")

                val messageId = jsonObject.optString("messageId", "")
                val enemyId = jsonObject.optInt("enemyId", 0)
                val myId = jsonObject.optInt("myId", 0)
                val content = jsonObject.optString("content", "")
                val gistId = jsonObject.optString("gistId", "")
                val timeStamp = System.currentTimeMillis().toString()

                val newMessage = PersonalChat(
                    messageId = messageId,
                    enemyId = enemyId,
                    myId = myId,
                    content = content,
                    status = PersonalMessageStatus.DELIVERED,
                    messageType = PersonalMessageType.P_GIST_INVITE,
                    timeStamp = timeStamp,
                    gistId = gistId,
                    topic = content
                )

                Log.d("Websocket", "handleIncoming Function called with $newMessage")
                handleIncomingPersonalMessage(newMessage)

                sendDeliveryAcknowledgment(messageId)
                Log.d("Websocket", "Acknowledgment sent for PGistInvite message $messageId")
            }

            PrivateChatReceivingType.ACK -> {
                val status = PersonalMessageStatus.DELIVERED
                val messageId = jsonObject.optString("messageId")
                viewModelScope.launch(Dispatchers.IO) {
                    personalChatDao.updateStatus(messageId, status)
                    Log.d("ForStatus", "called with : $status")
                    val currentEnemyId = _currentChat.value
                    val updatedMessage = personalChatDao.getPersonalChatById(messageId)

                    if (updatedMessage != null && currentEnemyId != null) {
                        if (updatedMessage.enemyId == currentEnemyId || updatedMessage.myId == currentEnemyId) {
                            val updatedList = _personalChats.value.toMutableList().apply {
                                val index = indexOfFirst { it.messageId == messageId }
                                if (index != -1) {
                                    this[index] = updatedMessage.copy(status = status)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                _personalChats.value = updatedList
                            }
                        }
                    }
                }
            }
            PrivateChatReceivingType.P_PROFILE_UPDATE -> {
                val profileUpdate = jsonObject.getJSONArray("profileUpdates")
                for (theIndex in 0 until profileUpdate.length()){
                    val eachUpdate = profileUpdate.getJSONObject(theIndex)
                    val enemyId = eachUpdate.getInt("enemyId")
                    val contactName = eachUpdate.getString("fullName")
                    val profilePhoto = eachUpdate.getString("profileUrl")
                    val isVerified = eachUpdate.getBoolean("isVerified")
                    updateProfile(enemyId, contactName, profilePhoto, isVerified)
                }
            }
        }
    }

    private fun downloadFileFromUrl(url: String): ByteArray? {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.inputStream.use { input ->
                input.readBytes()
            }
        } catch (e: IOException) {
            Log.e("Websocket", "Failed to download file from URL: $url", e)
            null
        }
    }

    fun setIsNewChat(isNew: Boolean) {
        _isNewChat.value = isNew
    }

    fun checkContactUpdate(){
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
        _currentPage = 0
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
        // Send a WebSocket message to the server to unsubscribe from the online status of enemyId
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

    /*
    fun simulateDelayedWebSocketMessages(enemyId: Int, delayMillis: Long = 15000) {
        viewModelScope.launch {
            // Wait for the specified delay
            delay(delayMillis)

            // Ensure the user is still in the chat
            if (_currentChat.value == enemyId) {
                // Simulate receiving new messages
                val simulatedMessages = listOf(
                    PersonalChat(
                        messageId = generateMessageId(),
                        enemyId = myUserId.value,
                        myId = enemyId,
                        content = "Simulated message 1",
                        status = "delivered",
                        messageType = "text",
                        timeStamp = System.currentTimeMillis().toString()
                    ),
                    PersonalChat(
                        messageId = generateMessageId(),
                        enemyId = myUserId.value,
                        myId = enemyId,
                        content = "Simulated message 2",
                        status = "delivered",
                        messageType = "text",
                        timeStamp = System.currentTimeMillis().toString()
                    )
                )

                // Handle the incoming messages
                simulatedMessages.forEach { message ->
                    handleIncomingPersonalMessage(message)
                    Log.d("Incoming", "This message is $message")
                }
            }
        }
    }
     */
    fun leaveChat() {
        currentChat.value?.let { unsubscribeFromOnlineStatus(it) }
        _currentChat.value = null
    }

    private fun incrementUnreadMsgCounter(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.incrementUnreadMsgCounter(enemyId)
        }
    }

    // Use this as a baseline to build message receipt of all types
    // This function is supposed to be called from the Websocket Parser
    // Remember to also implement a function to let the websocket know it has been delivered
    // The Media types will come in as binary. Implement a function to save to device using File Utils before storing Uri in db
    // Use Dispatcher.IO to do this
    private fun handleIncomingPersonalMessage(newMessage: PersonalChat) {
        Log.d("Incoming", "newMessage is $newMessage")
        viewModelScope.launch(Dispatchers.IO) {
            val chatExists = checkChatExistsSync(newMessage.myId, newMessage.enemyId)
            _isNewIncomingMessage.value = !chatExists
            Log.d("Websocket", "Is New Incoming is ${!chatExists}")
            addAndProcessPersonalChat(newMessage)
            sendDeliveryAcknowledgment(newMessage.messageId)
        }
    }

    private fun updateChatListWithNewMessage(newMessage: PersonalChat) {
        //this is to fish out the person who's not you,
        //as the other participant can be "myId" sometimes
        val enemyId = if (newMessage.myId == myUserId.value) newMessage.enemyId else newMessage.myId

        val lastMsg = when (newMessage.messageType) {
            PersonalMessageType.P_IMAGE -> "Photo"
            PersonalMessageType.P_VIDEO -> "Video"
            PersonalMessageType.P_AUDIO -> "Audio"
            PersonalMessageType.P_GIST_INVITE -> "Gist Invite..."
            PersonalMessageType.P_TEXT -> newMessage.content
        }
        val timeStamp = newMessage.timeStamp

        viewModelScope.launch(Dispatchers.IO) {
            if (_currentChat.value == enemyId) {
                val updatedList = _personalChats.value.toMutableList().apply {
                    if (none { it.messageId == newMessage.messageId }) {
                        add(0, newMessage)
                    } else {
                        Log.d(
                            "Websocket",
                            "Message with messageId ${newMessage.messageId} already exists. Skipping addition."
                        )
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
                unreadMsgCounter = if (_currentChat.value != enemyId) 1 else 0
            )

            if (_isNewChat.value || _isNewIncomingMessage.value) {
                chatDao.addChat(chat)
                Log.d("isNewChat", "add chat called with $chat")
                setIsNewChat(false) // Reset after adding
                _isNewIncomingMessage.value = false
            } else {
                chatDao.updateLastMessage(
                    enemyId = enemyId,
                    lastMsg = lastMsg,
                    timeStamp = timeStamp
                )
            }
            val updatedChats = getSortedChats(myUserId.value)
            _chats.value = updatedChats
        }
    }

    private suspend fun getSortedChats(myId: Int): List<ChatList> {
        return withContext(Dispatchers.IO) {
            chatDao.getAllChats(myId).sortedByDescending { it.lastMsgAddTime }
        }
    }

    private fun addAndProcessPersonalChat(personalChat: PersonalChat) {
        viewModelScope.launch(Dispatchers.IO) {
            val enemyId =
                if (personalChat.myId == myUserId.value) personalChat.enemyId else personalChat.myId
            if (_currentChat.value != enemyId) {
                Log.d("currentChat", "Current chat is ${_currentChat.value}")
                val existingMessage = personalChatDao.getPersonalChatById(personalChat.messageId)
                Log.d("currentChat", "Existing message is $existingMessage")
                if (existingMessage == null) {
                    incrementUnreadMsgCounter(enemyId)
                }
            }
            personalChatDao.addPersonalChat(personalChat)
            updateChatListWithNewMessage(personalChat)
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
                        _pageSize
                    )
                        .sortedByDescending { it.timeStamp }
                }
            } else {
                Log.d("Database", "this one called")
                personalChatDao.getInitialPersonalChats(myId, enemyId, _pageSize)
            }
            if (personalChatList != null) {
                Log.d(
                    "MessageLoading",
                    "Loaded ${personalChatList.size} messages, first message timestamp: ${personalChatList.firstOrNull()?.timeStamp}, last message timestamp: ${personalChatList.lastOrNull()?.timeStamp}"
                )
            }
            if (loadMore) {
                Log.d("Database", "Still even executing")
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

            if (personalChatList != null) {
                if (personalChatList.isNotEmpty()) {
                    _currentPage++
                }
            }
            _isLoading.postValue(false)
        }
    }

    suspend fun checkChatExistsSync(myId: Int, enemyId: Int): Boolean {
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
    fun updateProfile(
        enemyId: Int,
        contactName: String,
        profilePhoto: String,
        isVerified: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateProfile(enemyId, contactName, profilePhoto, isVerified)
        }
    }

    fun retryPendingMessages(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pendingMessages = personalChatDao.getMessagesByStatus(PersonalMessageStatus.PENDING)
            pendingMessages.forEach { message ->
                when (message.messageType) {
                    PersonalMessageType.P_TEXT -> {
                        retryTextMessage(message)
                    }

                    PersonalMessageType.P_GIST_INVITE -> {
                        retryGistInviteMessage(message)
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
                                    fullName = "",  // Pass full name
                                    context = context,
                                    isRetry = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun retryTextMessage(message: PersonalChat) {
        withContext(Dispatchers.IO) {
            val messageJson = """
    {
        "type": "${message.messageType}",
        "enemyId": ${message.enemyId},
        "content": "${
                message.content.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r")
                    .replace("\t", "\\t").replace("\b", "\\b")
            }",
        "messageId": "${message.messageId}",
        "myId": "${message.myId}"
    }
""".trimIndent()
            send(messageJson)
            // Optionally update status to "sent" if acknowledged by the server
        }
    }

    private suspend fun retryGistInviteMessage(message: PersonalChat) {
        withContext(Dispatchers.IO) {
            val gistInviteJson = """
        {
        "type": "${message.messageType}",
        "enemyId": ${message.enemyId},
        "content": "${
                message.content.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r")
                    .replace("\t", "\\t").replace("\b", "\\b")
            }",
        "gistId": "${message.gistId}",
        "messageId": "${message.messageId}"
        }
        """.trimIndent()

            send(gistInviteJson)
            // Optionally update status to "sent" if acknowledged by the server
        }
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
            val personalChat = PersonalChat(
                messageId = messageId,
                enemyId = enemyId,
                myId = myId,
                content = "",
                status = PersonalMessageStatus.PENDING,
                messageType = type,
                timeStamp = System.currentTimeMillis().toString(),
                mediaUri = mediaUri?.toString()
            )
            addAndProcessPersonalChat(personalChat)
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
                    audioByteArray, PersonalMessageType.P_AUDIO, enemyId, messageId, myId, fullName, context
                )

            } catch (e: IOException) {
                Log.e("ChatRoom", "Error processing audio: ${e.message}", e)
            }
        }
    }

    fun sendTextMessage(message: String, enemyId: Int, myId: Int) {
        val messageId = generateMessageId()
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = enemyId,
            myId = myId,
            content = message,
            status = PersonalMessageStatus.PENDING,
            messageType = PersonalMessageType.P_TEXT,
            timeStamp = System.currentTimeMillis().toString()  // Use appropriate time format
        )
        viewModelScope.launch(Dispatchers.IO) {
            // personalChatDao.addPersonalChat(personalChat)
            addAndProcessPersonalChat(personalChat)

        }
        val messageJson = """
            {
            "type": "PText",
            "enemyId": $enemyId,
            "content": "${
            message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b")
        }",
            "messageId": "$messageId"
            }
        """.trimIndent()
        Log.d("RawWebsocket", "send Text json $messageJson")
        send(messageJson)
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
                        PersonalMessageType.P_TEXT -> sendTextMessage(message.content, enemyId, myId)
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
                    }
                }
            }
            clearMessagesToForward()
        }
    }

    private fun sendGistInvite(topic: String, gistId: String, enemyId: Int, myId: Int) {
        val messageId = generateMessageId()
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = enemyId,
            myId = myId,
            content = topic,
            status = PersonalMessageStatus.PENDING,
            messageType = PersonalMessageType.P_GIST_INVITE,
            timeStamp = System.currentTimeMillis().toString(),
            mediaUri = null,
            gistId = gistId,
            topic = topic
        )
        viewModelScope.launch(Dispatchers.IO) {
            addAndProcessPersonalChat(personalChat)
            // Optionally, you can also send this message to the server here
        }
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

        Log.d("RawWebsocket", "send GistInvite json $gistInviteJson")
        send(gistInviteJson)
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

    // Send JSON message
    private fun send(message: String) {
        WebSocketManager.send(message)
        Log.d("send", message)
    }

    fun addGistInviteToForward(topic: String, gistId: String) {
        val messageId = generateMessageId()
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = 0, // This will be set when forwarding
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