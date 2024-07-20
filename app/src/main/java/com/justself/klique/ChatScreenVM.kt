package com.justself.klique

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

    private val _personalChats = MutableStateFlow<List<PersonalChat>>(emptyList())
    val personalChats: StateFlow<List<PersonalChat>> get() = _personalChats

    private val _onlineStatuses = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val onlineStatuses: StateFlow<Map<Int, Boolean>> get() = _onlineStatuses

    private val _currentChat = MutableStateFlow<Int?>(null)
    val currentChat: StateFlow<Int?> get() = _currentChat
    init {
        simulateOnlineStatusOscillation()
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

    fun deleteChat(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteChat(enemyId)
            loadChats(myUserId.value!!)
        }
    }

    fun loadChats(myId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatList = chatDao.getAllChats(myId)/* mockData is for testing. chatList is from the real database
            val mockData = getMockChats(myId) */
            _chats.value = chatList
            myUserId.value = myId
        }
    }
    fun enterChat(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.resetUnreadMsgCounter(enemyId)
        }
        _currentChat.value = enemyId
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
            val unreadCounts = newMessages.groupBy { it.enemyId }.mapValues { it.value.size }
            for ((enemyId, count) in unreadCounts) {
                chatDao.incrementUnreadMsgCounterBy(enemyId, count)
            }
            loadChats(myUserId.value)
        }
    }
    // Use this as a baseline to build message receipt of all types
    // This function is supposed to be called from the Websocket Parser
    // Remember to also implement a function to let the websocket know it has been delivered
    fun handleIncomingPersonalMessage(newMessage: PersonalChat) {
        Log.d("Incoming", "newMessage is $newMessage")
        viewModelScope.launch(Dispatchers.IO) {
            personalChatDao.addPersonalChat(newMessage)
            singleMessageUpdate(newMessage.myId, newMessage)
            // in the incoming message, myId is the enemyId
        }
    }
    private fun singleMessageUpdate(enemyId: Int, newMessage: PersonalChat) {
        if (_currentChat.value != enemyId) {
            incrementUnreadMsgCounter(enemyId)
        } else {
            val updatedList = _personalChats.value.toMutableList().apply{
                add(newMessage)
            }
            _personalChats.value = updatedList
        }
    }
    fun addPersonalChat(personalChat: PersonalChat) {
        viewModelScope.launch(Dispatchers.IO) {
            personalChatDao.addPersonalChat(personalChat)
            val updatedList = _personalChats.value.toMutableList().apply {
                add(personalChat)
            }
            _personalChats.value = updatedList
        }
    }
    fun loadPersonalChats(myId: Int, enemyId: Int) {
        Log.d("Database", "Loading Database")
        viewModelScope.launch(Dispatchers.IO) {
            val personalChatList = personalChatDao.getPersonalChats(myId, enemyId)
            _personalChats.value = personalChatList
            Log.d("Database", "Extracted from database is $personalChatList")
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
    private fun updateProfile(enemyId: Int, contactName: String, profilePhoto: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateProfile(enemyId, contactName, profilePhoto)
        }
    }

    fun sendBinary(
        data: ByteArray, type: String, enemyId: Int, messageId: String, myId: Int, fullName: String
    ) {
        val prefix = "$type:$enemyId:$messageId:$myId:$fullName".padEnd(50)
        val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
        val message = prefixBytes + data
        WebSocketManager.sendBinary(message)
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
            newBulkMessages.forEach{personalChatDao.addPersonalChat(it)}
            bulkUpdateUnreadMessageCounters(newBulkMessages)
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

    fun handleRecordedAudio(file: File, enemyId: Int, myId: Int, fullName: String) {
        viewModelScope.launch {
            try {
                val audioByteArray = FileUtils.fileToByteArray(file)
                Log.d("ChatRoom", "Audio Byte Array: ${audioByteArray.size} bytes")

                val messageId = generateMessageId()
                sendBinary(
                    audioByteArray, "PAudio", enemyId, messageId, myId, fullName
                )

                val personalChat = PersonalChat(
                    messageId = messageId,
                    enemyId = enemyId,
                    myId = myId,
                    content = "",
                    status = "pending",
                    messageType = "PAudio",
                    timeStamp = System.currentTimeMillis()
                        .toString(),  // Use appropriate time format
                    mediaContent = audioByteArray
                )
                addPersonalChat(personalChat)
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
            messageType = "text",
            timeStamp = System.currentTimeMillis().toString()  // Use appropriate time format
        )
        viewModelScope.launch(Dispatchers.IO) {
            // personalChatDao.addPersonalChat(personalChat)
            addPersonalChat(personalChat)

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

    // Send JSON message
    private fun send(message: String) {
        WebSocketManager.send(message)
        Log.d("send", message)
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