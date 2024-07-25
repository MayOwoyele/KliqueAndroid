package com.justself.klique

import android.adservices.topics.Topic
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

class ChatScreenViewModel(
    private val chatDao: ChatDao, private val personalChatDao: PersonalChatDao
) : ViewModel() {
    private val _chats = MutableStateFlow<List<ChatList>>(emptyList())
    val chats: StateFlow<List<ChatList>> get() = _chats

    val myUserId = MutableStateFlow(0)

    private val _personalChats = MutableLiveData<List<PersonalChat>>(emptyList())
    val personalChats: LiveData<List<PersonalChat>> get() = _personalChats

    private val _onlineStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val onlineStatuses: StateFlow<Map<Int, Boolean>> get() = _onlineStatuses

    private val _currentChat = MutableStateFlow<Int?>(null)
    val currentChat: StateFlow<Int?> get() = _currentChat
    private val _searchResults = MutableStateFlow<List<ChatList>>(emptyList())
    val searchResults: StateFlow<List<ChatList>> get() = _searchResults
    private val _messagesToForward = MutableLiveData<List<PersonalChat>>(emptyList())
    val messagesToForward: LiveData<List<PersonalChat>> get() = _messagesToForward

    private val _isNewChat = MutableStateFlow<Boolean>(false)
    private val _selectedMessages = MutableLiveData<List<String>>(emptyList())
    val selectedMessages: LiveData<List<String>> get() = _selectedMessages

    private val _isSelectionMode = MutableLiveData<Boolean>(false)
    val isSelectionMode: LiveData<Boolean> get() = _isSelectionMode

    init {
        // simulateOnlineStatusOscillation()
    }
    fun setIsNewChat(isNew: Boolean) {
        _isNewChat.value = isNew
    }
    fun addChat(chat: ChatList) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.addChat(chat)
            loadChats(myUserId.value!!)
        }
    }

    fun updateChat(chat: ChatList) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateChat(chat)
            loadChats(myUserId.value!!)
        }
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
                loadPersonalChats(myUserId.value, currentChat.value!!)
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

    fun loadChats(myId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatList = chatDao.getAllChats(myId)/* mockData is for testing. chatList is from the real database
            val mockData = getMockChats(myId) */
            _chats.value = chatList
        }
    }
    fun setMyUserId(userId: Int) {
        myUserId.value = userId
    }
    fun enterChat(enemyId: Int) {
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
            currentSelection.add(messageId)
        }
        _selectedMessages.value = currentSelection
        _isSelectionMode.value = currentSelection.isNotEmpty()
    }

    fun clearSelection() {
        _selectedMessages.value = emptyList()
        _isSelectionMode.value = false
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
        _currentChat.value = null
    }
    private fun incrementUnreadMsgCounter(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.incrementUnreadMsgCounter(enemyId)
        }
    }
    // Apparently, the server needs to be keep delivery statuses of messages. Remember to do this
    private fun bulkUpdateUnreadMessageCounters(newMessages: List<PersonalChat>) {
        viewModelScope.launch(Dispatchers.IO) {
            val unreadCounts = newMessages.groupBy { val enemy = it.myId; enemy }.mapValues { it.value.size }
            for ((enemyId, count) in unreadCounts) {
                chatDao.incrementUnreadMsgCounterBy(enemyId, count)
            }
            loadChats(myUserId.value)
        }
    }
    // Use this as a baseline to build message receipt of all types
    // This function is supposed to be called from the Websocket Parser
    // Remember to also implement a function to let the websocket know it has been delivered
    // The Media types will come in as binary. Implement a function to save to device using File Utils before storing Uri in db
    // Use Dispatcher.IO to do this
    fun handleIncomingPersonalMessage(newMessage: PersonalChat) {
        Log.d("Incoming", "newMessage is $newMessage")
        addAndProcessPersonalChat(newMessage)
    }
    private fun updateChatListWithNewMessage(newMessage: PersonalChat) {
        val enemyId = if (newMessage.myId == myUserId.value) newMessage.enemyId else newMessage.myId
        val lastMsg = when (newMessage.messageType) {
            "PImage" -> "Photo"
            "PVideo" -> "Video"
            "PAudio" -> "Audio"
            else -> newMessage.content
        }
        val timeStamp = newMessage.timeStamp

        viewModelScope.launch(Dispatchers.IO) {
            if (_currentChat.value != enemyId) {
                incrementUnreadMsgCounter(enemyId)
            } else {
                val updatedList = _personalChats.value?.toMutableList()?.apply {
                    add(newMessage)
                }
                _personalChats.postValue(updatedList ?: emptyList()) // Update LiveData
            }
            val chat = ChatList(
                enemyId = enemyId,
                contactName = "Your NewFriend", // Set a default or fetch the actual name
                lastMsg = lastMsg,
                lastMsgAddtime = timeStamp,
                profilePhoto = "", // Placeholder or actual profile photo
                myId = myUserId.value,
                unreadMsgCounter = if (_currentChat.value != enemyId) 1 else 0
            )

            if (_isNewChat.value) {
                chatDao.addChat(chat)
                Log.d("isNewChat", "add chat called with $chat")
                setIsNewChat(false) // Reset after adding
            } else {
                chatDao.updateLastMessage(
                    enemyId = enemyId,
                    lastMsg = lastMsg,
                    timeStamp = timeStamp
                )
            }
        }
    }
    fun addAndProcessPersonalChat(personalChat: PersonalChat) {
        viewModelScope.launch(Dispatchers.IO) {
            personalChatDao.addPersonalChat(personalChat)
            updateChatListWithNewMessage(personalChat)
        }
    }
    fun loadPersonalChats(myId: Int, enemyId: Int) {
        Log.d("Database", "Loading Database")
        viewModelScope.launch(Dispatchers.IO) {
            val personalChatList = personalChatDao.getPersonalChats(myId, enemyId)
            _personalChats.postValue(personalChatList)
            Log.d("Database", "Extracted from database is $personalChatList")
        }
    }
    suspend fun checkChatExistsSync(myId: Int, enemyId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            chatDao.chatExists(myId, enemyId)
        }
    }

    fun fetchNewMessagesFromServer(){
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
    private fun updateProfile(enemyId: Int, contactName: String, profilePhoto: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateProfile(enemyId, contactName, profilePhoto)
        }
    }

    fun sendBinary(
        data: ByteArray, type: String, enemyId: Int, messageId: String, myId: Int, fullName: String, context: Context
    ) {
        val prefix = "$type:$enemyId:$messageId:$myId:$fullName".padEnd(50)
        val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
        val message = prefixBytes + data
        WebSocketManager.sendBinary(message)
        val mediaUri = when (type) {
            "PImage" -> FileUtils.saveImage(context, data)
            "PVideo" -> FileUtils.saveVideo(context, data)
            "PAudio" -> FileUtils.saveAudio(context, data)
            else -> null
        }
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = enemyId,
            myId = myId,
            content = "",
            status = "pending",
            messageType = type,
            timeStamp = System.currentTimeMillis().toString(),
            mediaUri = mediaUri?.toString()
        )
        addAndProcessPersonalChat(personalChat)
        Log.d(
            "sendBinaryInputs",
            "Data: ${data.size}, Type: $type, EnemyId: $enemyId, MessageId: $messageId, MyId: $myId, FullName: $fullName"
        )
    }

    fun startPeriodicOnlineStatusCheck() {
        viewModelScope.launch{
            while (true) {
                val relevantIds = fetchRelevantIds()
                informServerOfOnlineStatusRequest(relevantIds)
                delay(20000) // Wait for 20 seconds before fetching again
            }
        }
    }

    private suspend fun fetchRelevantIds(): List<Int> = withContext(Dispatchers.IO) {
        chatDao.getAllChats(myUserId.value).map { it.enemyId }.distinct()
    }
    private fun informServerOfOnlineStatusRequest(relevantIds: List<Int>) {
        val idsJsonArray = relevantIds.joinToString(prefix = "[", postfix = "]") { it.toString() }
        val jsonString = """
        {
            "type": "requestOnlineStatus",
            "ids": $idsJsonArray
        }
    """.trimIndent()
        send(jsonString)
    }
    // This simulation should be replaced by the onMessage logic from the websocket
    private fun simulateOnlineStatusOscillation() {
        viewModelScope.launch {
            var isOnline = false
            while (true) {
                isOnline = !isOnline
                val simulatedMessage = createSimulatedOnlineStatusMessage(123, isOnline)
                handleWebSocketMessage(simulatedMessage)
                delay(4000)
            }
        }
    }

    private fun createSimulatedOnlineStatusMessage(userId: Int, onlineStatus: Boolean): String {
        val jsonObject = JSONObject().apply {
            put("type", "onlineStatus")
            val statusesArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("userId", userId)
                    put("onlineStatus", onlineStatus)
                })
            }
            put("statuses", statusesArray)
        }
        return jsonObject.toString()
    }
    // This might be replaced by a differnt but similar websocket parsing logic
    private fun handleWebSocketMessage(websocketMessage: String) {
        val jsonObject = JSONObject(websocketMessage)
        when (jsonObject.getString("type")) {
            "onlineStatus" -> {
                val onlineStatusUpdates = parseOnlineStatusMessage(jsonObject)
                val updatedStatuses = _onlineStatuses.value.toMutableMap()
                updatedStatuses.putAll(onlineStatusUpdates)
                _onlineStatuses.value = updatedStatuses
            }
            "bulkMessageSync" -> {
                val newMessages = parseNewMessages(jsonObject)
                handleBulkMessagesRefresh(newMessages)
            }
        }
    }
    // This is for handling the websocket response type for 'fetch new messages' function
    private fun handleBulkMessagesRefresh(newBulkMessages: List<PersonalChat>){
        viewModelScope.launch(Dispatchers.IO) {
            newBulkMessages.forEach{message ->
                personalChatDao.addPersonalChat(message)
                val enemyId = message.myId
                chatDao.updateLastMessage(
                    enemyId = enemyId,
                    lastMsg = message.content,
                    timeStamp = message.timeStamp
                )}
            bulkUpdateUnreadMessageCounters(newBulkMessages)
            Log.d("Add Personal", "Add Personal handlebulk ")
        }
    }
    private fun parseNewMessages(jsonObject: JSONObject): List<PersonalChat> {
        val messages = mutableListOf<PersonalChat>()
        val messagesArray = jsonObject.getJSONArray("messages")
        for (i in 0 until messagesArray.length()) {
            val message = messagesArray.getJSONObject(i)
            messages.add(
                PersonalChat(
                    messageId = message.getString("messageId"),
                    enemyId = message.getInt("enemyId"),
                    myId = message.getInt("myId"),
                    content = message.getString("content"),
                    status = message.getString("status"),
                    messageType = message.getString("messageType"),
                    timeStamp = message.getString("timeStamp")
                )
            )
        }
        return messages
    }

    private fun parseOnlineStatusMessage(jsonObject: JSONObject): Map<Int, Boolean> {
        val statusMap = mutableMapOf<Int, Boolean>()
        val statuses = jsonObject.getJSONArray("statuses")
        for (i in 0 until statuses.length()){
            val status = statuses.getJSONObject(i)
            val userId = status.getInt("userId")
            val onlineStatus = status.getBoolean("onlineStatus")
            statusMap[userId] = onlineStatus
        }
        return statusMap
    }

    fun handleRecordedAudio(file: File, enemyId: Int, myId: Int, fullName: String, context: Context) {
        viewModelScope.launch {
            try {
                val audioByteArray = FileUtils.fileToByteArray(file)
                Log.d("ChatRoom", "Audio Byte Array: ${audioByteArray.size} bytes")

                val messageId = generateMessageId()
                sendBinary(
                    audioByteArray, "PAudio", enemyId, messageId, myId, fullName, context
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
            status = "pending",
            messageType = "PText",
            timeStamp = System.currentTimeMillis().toString()  // Use appropriate time format
        )
        viewModelScope.launch(Dispatchers.IO) {
            // personalChatDao.addPersonalChat(personalChat)
            addAndProcessPersonalChat(personalChat)

        }
        val messageJson = """
            {
            "type": "PMessage",
            "enemyId": "$enemyId",
            "content": "${
            message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b")
        }",
            "messageId": "$messageId",
            
            }
        """.trimIndent()
        send(messageJson)
    }
    fun searchChats(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = if (query.isEmpty()) {
                chatDao.getAllChats(myUserId.value)
            } else {
                chatDao.searchChats(myUserId.value, "%$query%")
            }
            _searchResults.value = result
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
                    when (message.messageType) {
                        "PText", "text" -> sendTextMessage(message.content, enemyId, myId)
                        "PImage", "PVideo", "PAudio" -> {
                            message.mediaUri?.let { uri ->
                                val data = FileUtils.loadFileAsByteArray(context, Uri.parse(uri))
                                data?.let {
                                    sendBinary(it, message.messageType, enemyId, message.messageId, myId, "MyName", context)
                                }
                            }
                        }
                        "PGistInvite" -> message.gistId?.let {
                            sendGistInvite(message.content,
                                it, enemyId, myId)
                        }
                    }
                }
            }
        }
    }
    private fun sendGistInvite(topic: String, gistId: String, enemyId: Int, myId: Int) {
        val messageId = generateMessageId()
        val personalChat = PersonalChat(
            messageId = messageId,
            enemyId = enemyId,
            myId = myId,
            content = topic,
            status = "pending",
            messageType = "PGistInvite",
            timeStamp = System.currentTimeMillis().toString(),
            mediaUri = null,
            gistId = gistId,
            topic = topic
        )
        viewModelScope.launch(Dispatchers.IO) {
            personalChatDao.addPersonalChat(personalChat)
            // Optionally, you can also send this message to the server here
        }
    }
    // Remember to implement an onMessage to attach people to gists
    fun joinGist(gistId: String){
        val joinGistJson = """
            {
            "type": "joinGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(joinGistJson)
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
            status = "pending",
            messageType = "PGistInvite",
            timeStamp = System.currentTimeMillis().toString(),
            mediaUri = null,
            gistId = gistId,
            topic = topic
        )
        val currentMessages = _messagesToForward.value?.toMutableList() ?: mutableListOf()
        currentMessages.add(personalChat)
        _messagesToForward.value = currentMessages
    }


    fun getMockChats(myId: Int): List<ChatList> {
        return listOf(
            ChatList(
                contactName = "John Doe",
                enemyId = 123,
                lastMsg = "Hey there!",
                lastMsgAddtime = "2024-07-05 12:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/men/1.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 2
            ), ChatList(
                contactName = "Jane Smith",
                enemyId = 124,
                lastMsg = "Hello!",
                lastMsgAddtime = "2024-07-05 14:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/women/2.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ), ChatList(
                contactName = "Alice Johnson",
                enemyId = 125,
                lastMsg = "Good morning! How are you, I hope you are doing well and I hope to see you tomorrow. Help me say hi to the family and thanks for yesterday",
                lastMsgAddtime = "2024-07-05 09:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/women/3.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 1
            ), ChatList(
                contactName = "Bob Brown",
                enemyId = 126,
                lastMsg = "Did you get my last message?",
                lastMsgAddtime = "2024-07-06 08:20:00",
                profilePhoto = "https://randomuser.me/api/portraits/men/4.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 3
            ), ChatList(
                contactName = "Charlie Davis",
                enemyId = 127,
                lastMsg = "Sure, let's meet at the usual place.",
                lastMsgAddtime = "2024-07-06 10:15:45",
                profilePhoto = "https://randomuser.me/api/portraits/men/5.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ), ChatList(
                contactName = "Diana Evans",
                enemyId = 128,
                lastMsg = "It was great catching up with you last night. Let's do it again soon!",
                lastMsgAddtime = "2024-07-06 11:30:25",
                profilePhoto = "https://randomuser.me/api/portraits/women/6.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 55
            ), ChatList(
                contactName = "Ethan Fox",
                enemyId = 129,
                lastMsg = "I've attached the documents you requested. Please review and let me know your thoughts.",
                lastMsgAddtime = "2024-07-06 13:45:30",
                profilePhoto = "https://randomuser.me/api/portraits/men/7.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ), ChatList(
                contactName = "Fiona Green",
                enemyId = 130,
                lastMsg = "Can't wait for the weekend! Do you have any plans?",
                lastMsgAddtime = "2024-07-06 14:50:10",
                profilePhoto = "https://randomuser.me/api/portraits/women/8.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 2
            ), ChatList(
                contactName = "George Harris",
                enemyId = 131,
                lastMsg = "Just sent you the files. Let me know if you need anything else.",
                lastMsgAddtime = "2024-07-06 16:00:00",
                profilePhoto = "https://randomuser.me/api/portraits/men/9.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 1
            ), ChatList(
                contactName = "Hannah Irvine",
                enemyId = 132,
                lastMsg = "Can we reschedule our meeting to next week? I've got a conflict.",
                lastMsgAddtime = "2024-07-06 17:10:45",
                profilePhoto = "https://randomuser.me/api/portraits/women/10.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ), ChatList(
                contactName = "Ian Jackson",
                enemyId = 133,
                lastMsg = "Thank you for your help! I really appreciate it.",
                lastMsgAddtime = "2024-07-06 18:25:30",
                profilePhoto = "https://randomuser.me/api/portraits/men/11.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ), ChatList(
                contactName = "Jackie Kim",
                enemyId = 134,
                lastMsg = "Do you have any recommendations for a good restaurant in town?",
                lastMsgAddtime = "2024-07-06 19:35:20",
                profilePhoto = "https://randomuser.me/api/portraits/women/12.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 4
            ), ChatList(
                contactName = "Karen Lee",
                enemyId = 135,
                lastMsg = "Let's catch up over coffee sometime this week.",
                lastMsgAddtime = "2024-07-06 20:45:50",
                profilePhoto = "https://randomuser.me/api/portraits/women/13.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ), ChatList(
                contactName = "Larry Moore",
                enemyId = 136,
                lastMsg = "I've been meaning to tell you about the new project I'm working on.",
                lastMsgAddtime = "2024-07-06 21:55:10",
                profilePhoto = "https://randomuser.me/api/portraits/men/14.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 1
            ), ChatList(
                contactName = "Megan Nelson",
                enemyId = 137,
                lastMsg = "Thanks for the invite! I'll definitely be there.",
                lastMsgAddtime = "2024-07-06 22:05:35",
                profilePhoto = "https://randomuser.me/api/portraits/women/15.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 3
            ), ChatList(
                contactName = "Nathan Owens",
                enemyId = 138,
                lastMsg = "Can you send me the details for tomorrow's meeting?",
                lastMsgAddtime = "2024-07-06 23:15:50",
                profilePhoto = "https://randomuser.me/api/portraits/men/16.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 2
            )
        )
    }
}

class ChatViewModelFactory(
    private val chatDao: ChatDao, private val personalChatDao: PersonalChatDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return ChatScreenViewModel(chatDao, personalChatDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}