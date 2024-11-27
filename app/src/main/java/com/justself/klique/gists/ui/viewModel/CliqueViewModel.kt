// File: SharedCliqueViewModel.kt
package com.justself.klique.gists.ui.viewModel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.ContactDao
import com.justself.klique.ContactsBlock.Contacts.data.Contact
import com.justself.klique.GistComment
import com.justself.klique.GistMediaType
import com.justself.klique.GistMessage
import com.justself.klique.GistMessageStatus
import com.justself.klique.GistMessageType
import com.justself.klique.GistState
import com.justself.klique.GistStateDao
import com.justself.klique.GistStateEntity
import com.justself.klique.GistTopRow
import com.justself.klique.KliqueHttpMethod
import com.justself.klique.ListenerIdEnum
import com.justself.klique.Members
import com.justself.klique.NetworkUtils
import com.justself.klique.Reply
import com.justself.klique.SharedCliqueReceivingType
import com.justself.klique.UserStatus
import com.justself.klique.WebSocketListener
import com.justself.klique.WebSocketManager
import com.justself.klique.deEscapeContent
import com.justself.klique.downloadFromUrl
import com.justself.klique.getUriFromByteArray
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.GistUiState
import com.justself.klique.toContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class SharedCliqueViewModel(
    application: Application,
    private val customerId: Int,
    private val contactDao: ContactDao,
    private val getGistStateDao: GistStateDao
) :
    AndroidViewModel(application), WebSocketListener<SharedCliqueReceivingType> {
    override val listenerId: String = ListenerIdEnum.SHARED_CLIQUE.theId
    private val _uiState = MutableStateFlow(GistUiState(myGists = emptyList(), trendingGists = emptyList()))
    val uiState: StateFlow<GistUiState> = _uiState

    private val _gistCreatedOrJoined = MutableLiveData<GistState?>()
    val gistCreatedOrJoined: LiveData<GistState?> = _gistCreatedOrJoined
    private val _messages = MutableStateFlow<List<GistMessage>>(emptyList())
    val messages: StateFlow<List<GistMessage>> = _messages.asStateFlow()
    private val gistId: String
        get() = _gistCreatedOrJoined.value?.gistId ?: ""

    private val _userStatus = MutableLiveData(UserStatus(isSpeaker = true, isOwner = true))
    val userStatus: LiveData<UserStatus> = _userStatus

    private val _gistCreationError = MutableLiveData<String?>()
    val gistCreationError: LiveData<String?> = _gistCreationError

    private val _gistMessage = mutableStateOf(TextFieldValue(""))
    val gistMessage: State<TextFieldValue> = _gistMessage

    private val _listOfContactMembers = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfNonContactMembers = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfOwners = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfSpeakers = MutableStateFlow<List<Members>>(emptyList())
    val listOfNonContactMembers = _listOfNonContactMembers.asStateFlow()
    val listOfContactMembers = _listOfContactMembers.asStateFlow()
    val listOfOwners = _listOfOwners.asStateFlow()
    val listOfSpeakers = _listOfSpeakers.asStateFlow()

    private val _comments = MutableStateFlow<List<GistComment>>(emptyList())
    val comments: StateFlow<List<GistComment>> = _comments

    private val _gistTopRow: MutableStateFlow<GistTopRow?> = MutableStateFlow(null)
    val gistTopRow = _gistTopRow.asStateFlow()

    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
    private val _searchResults = MutableLiveData<List<Members>>()
    val searchResults: LiveData<List<Members>> = _searchResults
    private val _searchPerformed = MutableLiveData(false)
    val searchPerformed: LiveData<Boolean> = _searchPerformed

    private val downloadedMediaUrls = ConcurrentHashMap<String, DownloadState>()

    init {
        WebSocketManager.registerListener(this)
        viewModelScope.launch {
            restoreGistState()
        }
        generateMembersList()
    }

    fun onGistMessageChange(newMessage: TextFieldValue) {
        _gistMessage.value = newMessage
    }

    private fun updateSimulatedGistComments() {
        _comments.value = List(20) { index ->
            GistComment(
                id = generateUUIDString(),
                fullName = "User $index",
                comment = "This is comment number $index",
                customerId = index,
                replies = List(index % 3) { replyIndex ->
                    Reply(
                        id = replyIndex.toString(),
                        fullName = "User ${replyIndex + 1}",
                        customerId = index,
                        reply = "This is a reply to comment $index"
                    )
                },
                upVotes = 1
            )
        }
    }

    suspend fun restoreGistState() {
        val persistedGist = getGistStateDao.getGistState()
        val context = getApplication<Application>().applicationContext
        if (persistedGist != null) {
            _gistCreatedOrJoined.postValue(GistState(persistedGist.topic, persistedGist.gistId))
            _gistTopRow.value = GistTopRow(
                gistId = persistedGist.gistId,
                topic = persistedGist.topic,
                gistDescription = persistedGist.description,
                activeSpectators = formatUserCount(persistedGist.activeSpectators),
                gistImage = persistedGist.gistImage,
                startedBy = persistedGist.startedBy,
                startedById = persistedGist.startedById
            )
            _userStatus.postValue(
                UserStatus(
                    isSpeaker = persistedGist.isSpeaker,
                    isOwner = persistedGist.isOwner
                )
            )
        }
        clearCacheDirectory(context)
        downloadedMediaUrls.clear()
        if (persistedGist != null) {
            requestRefreshGistAndPreviousMessages(persistedGist.gistId)
        }
    }

    private fun requestRefreshGistAndPreviousMessages(gistId: String) {
        val message = """{
            "type": "requestRefreshGistAndPreviousMessages",
            "gistId": "$gistId"
            }""".trimMargin()
        send(message)
    }

    fun clearMessage() {
        _gistMessage.value = TextFieldValue("")
    }

    fun fetchMyGists(customerId: Int) {
        viewModelScope.launch {
            try {
                val endpoint = "gists/my"
                val method = KliqueHttpMethod.GET
                val params = mapOf("userId" to customerId.toString())

                val response = NetworkUtils.makeRequest(
                    endpoint = endpoint,
                    method = method,
                    params = params
                ).second
                val gists = parseGistsFromResponse(response)
                _uiState.value = _uiState.value.copy(
                    myGists = gists
                )
            } catch (e: Exception) {
                Log.e("fetchGists", "Exception is $e")
            }
        }
    }
    fun fetchTrendingGists(customerId: Int){
        viewModelScope.launch {
            try {
                val endpoint = "gists/trending"
                val method = KliqueHttpMethod.GET
                val params = mapOf("userId" to customerId.toString())

                val response = NetworkUtils.makeRequest(
                    endpoint = endpoint,
                    method = method,
                    params = params
                ).second
                val gists = parseGistsFromResponse(response)
                Log.d("Gists", "Gists: $gists")
                _uiState.value = _uiState.value.copy(
                    trendingGists = gists
                )
            } catch (e: Exception) {
                Log.e("fetchGists", "Exception is $e")
            }
        }
    }

    fun updateGistTimestamp() {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            getGistStateDao.updateTimestamp(currentTime)
        }
    }

    private fun parseGistsFromResponse(response: String): List<GistModel> {
        Log.d("fetchGists", response)
        val jsonArray = JSONObject(response).getJSONArray("gists")
        val gists = mutableListOf<GistModel>()
        for (i in 0 until jsonArray.length()) {
            val gistJson = jsonArray.getJSONObject(i)
            val gist = GistModel(
                gistId = gistJson.getString("id"),
                topic = gistJson.getString("topic"),
                description = gistJson.getString("description"),
                image = gistJson.getString("imageUrl"),
                activeSpectators = gistJson.getInt("activeSpectators")
            )
            gists.add(gist)
        }
        return gists
    }

    fun enterGist(gistId: String) {
        val enterGistId = """
            {
            "type": "enterGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(enterGistId)
    }

    fun exitGist() {
        val exitGistId = """
            {
            "type": "exitGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(exitGistId)
    }

    fun floatGist(gistId: String) {
        val floatGistId = """
            {
            "type": "floatGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(floatGistId)
    }

    // remember to handle the websocket receipt message
    // use this to update _searchResults state variable
    fun doTheSearch(searchQuery: String) {
        val allMembers = _listOfContactMembers.value + _listOfNonContactMembers.value
        val filteredMembers = allMembers.filter {
            it.fullName.contains(searchQuery, ignoreCase = true)
        }
        _searchResults.value = filteredMembers
        _searchPerformed.value = true
    }

    fun turnSearchPerformedOff() {
        _searchPerformed.value = false
    }

    fun unsubscribeToMembersUpdate() {
        val messageJson = """{
            "type": "unsubscribeToMembersUpdate"
            }""".trimMargin()
        send(messageJson)
    }

    // This simulation can be used as a data structure template for the server
    fun simulateSearchResults() {
        val simulatedMembers = listOf(
            Members(1, "May Owoyele", isContact = true, isOwner = false, isSpeaker = false),
            Members(2, "Wale Adams", isContact = true, isOwner = false, isSpeaker = true),
            Members(3, "John Doe", isContact = false, isOwner = true, isSpeaker = true),
            // Add more simulated members as needed
        )
        _searchResults.postValue(simulatedMembers)
    }

    override fun onCleared() {
        super.onCleared()
        WebSocketManager.unregisterListener(this)
    }

    fun generateUUIDString(): String {
        val randomUUID = UUID.randomUUID().toString()
        return randomUUID
    }

    override fun onMessageReceived(type: SharedCliqueReceivingType, jsonObject: JSONObject) {
        when (type) {
            SharedCliqueReceivingType.GIST_CREATED -> {
                val gistId = jsonObject.getString("gistId")
                val topic = jsonObject.getString("topic")
                _gistCreatedOrJoined.postValue(GistState(topic, gistId))
                val startedBy = jsonObject.getString("startedBy")
                val startedById = jsonObject.getInt("startedById")
                val description = jsonObject.getString("description")
                val isSpeaker = jsonObject.getBoolean("isSpeaker")
                val isOwner = jsonObject.getBoolean("isOwner")
                val activeSpectators = jsonObject.getInt("activeSpectators")
                val gistImageUrl = jsonObject.getString("gistImageUrl")
                val gistTopRowObject = GistTopRow(
                    gistId = gistId,
                    topic = topic,
                    gistDescription = description,
                    activeSpectators = formatUserCount(activeSpectators),
                    gistImage = gistImageUrl,
                    startedBy = startedBy,
                    startedById = startedById
                )
                _gistTopRow.value = gistTopRowObject
                _userStatus.value = UserStatus(isSpeaker = isSpeaker, isOwner = isOwner)
                viewModelScope.launch {
                    getGistStateDao.insertGistState(
                        GistStateEntity(
                            gistId = gistId,
                            topic = topic,
                            description = description,
                            startedBy = startedBy,
                            startedById = startedById,
                            activeSpectators = activeSpectators,
                            gistImage = gistImageUrl,
                            isSpeaker = isSpeaker,
                            isOwner = isOwner
                        )
                    )
                }
            }

            SharedCliqueReceivingType.GIST_CREATION_ERROR -> {
                val errorMessage = jsonObject.getString("message")
                _gistCreationError.postValue(errorMessage)
            }

            SharedCliqueReceivingType.PREVIOUS_MESSAGES -> {
                val messagesJsonArray = jsonObject.getJSONArray("messages")
                val messages = (0 until messagesJsonArray.length()).map { i ->
                    val msg = messagesJsonArray.getJSONObject(i)
                    val messageType = msg.getString("messageType")
                    val theMessageType = when (messageType) {
                        GistMessageType.K_AUDIO.typeString -> GistMessageType.K_AUDIO
                        GistMessageType.K_IMAGE.typeString -> GistMessageType.K_IMAGE
                        GistMessageType.K_VIDEO.typeString -> GistMessageType.K_VIDEO
                        GistMessageType.K_TEXT.typeString -> GistMessageType.K_TEXT
                        else -> GistMessageType.K_TEXT
                    }
                    val timeStampString = msg.getString("timeStamp")
                    val content = deEscapeContent(msg.getString("content"))
                    val externalUrl = when (messageType) {
                        "KImage", "KAudio", "KVideo" -> content
                        else -> null
                    }
                    GistMessage(
                        id = msg.getString("id"),
                        gistId = msg.getString("gistId"),
                        senderId = msg.getInt("senderId"),
                        senderName = msg.getString("fullName"),
                        content = content,
                        status = GistMessageStatus.Sent,
                        messageType = theMessageType,
                        timeStamp = timeStampString,
                        externalUrl = externalUrl
                    )
                }
                _messages.value = messages
                messages.forEach { message ->
                    handleMediaDownload(message)
                }
            }

            SharedCliqueReceivingType.K_TEXT -> {
                val gistId = jsonObject.getString("gistId")
                val messageId = jsonObject.getString("messageId")
                val customerId = jsonObject.getInt("senderId")
                val senderName = jsonObject.getString("senderName")
                val content = deEscapeContent(jsonObject.getString("content"))
                val timeStampString = jsonObject.getString("timeStamp")
                val message = GistMessage(
                    id = messageId,
                    gistId = gistId,
                    senderId = customerId,
                    senderName = senderName,
                    content = content,
                    status = GistMessageStatus.Sent,
                    messageType = GistMessageType.K_TEXT,
                    timeStamp = timeStampString
                )
                Log.d("Called", "Add message called")
                addMessage(message)
            }

            SharedCliqueReceivingType.GIST_MESSAGE_ACK -> {
                Log.d("Called", "this function called")
                val gistId = jsonObject.getString("gistId")
                val messageId = jsonObject.getString("messageId")
                Log.i("Called", "Message acknowledged")
                messageAcknowledged(gistId, messageId)
            }

            SharedCliqueReceivingType.K_IMAGE, SharedCliqueReceivingType.K_AUDIO, SharedCliqueReceivingType.K_VIDEO -> {
                try {
                    Log.d("GistMedia", "Called")
                    val messageId = jsonObject.getString("messageId")
                    val gistId = jsonObject.getString("gistId")
                    val senderId = jsonObject.getInt("senderId")
                    val senderName = jsonObject.getString("senderName")
                    val content = jsonObject.getString("content")
                    val messageType = jsonObject.getString("messageType")
                    val theMessageType = when (messageType) {
                        GistMessageType.K_AUDIO.typeString -> GistMessageType.K_AUDIO
                        GistMessageType.K_IMAGE.typeString -> GistMessageType.K_IMAGE
                        GistMessageType.K_VIDEO.typeString -> GistMessageType.K_VIDEO
                        else -> GistMessageType.K_IMAGE
                    }
                    val timeStampString = jsonObject.getString("timeStamp")
                    val messageObject = GistMessage(
                        id = messageId,
                        gistId = gistId,
                        senderId = senderId,
                        senderName = senderName,
                        content = content,
                        status = GistMessageStatus.Sent,
                        messageType = theMessageType,
                        externalUrl = content,
                        timeStamp = timeStampString
                    )
                    addMessage(messageObject)
                } catch (e: Exception) {
                    Log.d("GistMedia", "Error is ${e.message}")
                }
            }

            SharedCliqueReceivingType.SPECTATOR_UPDATE -> {
                val activeSpectatorsUpdate = jsonObject.getInt("activeSpectators")
                val gistId = jsonObject.getString("gistId")
                if (gistId == _gistCreatedOrJoined.value?.gistId) {
                    _gistTopRow.value = _gistTopRow.value?.copy(
                        activeSpectators = formatUserCount(activeSpectatorsUpdate)
                    )
                }
                viewModelScope.launch(Dispatchers.IO) {
                    getGistStateDao.updateActiveSpectators(activeSpectatorsUpdate)
                }
            }

            SharedCliqueReceivingType.OLDER_MESSAGES -> {
                val messagesArray = jsonObject.getJSONArray("theMessages")
                val messages = (0 until messagesArray.length()).map { i ->
                    val msg = messagesArray.getJSONObject(i)
                    val messageType = msg.getString("messageType")
                    val theMessageType = when (messageType) {
                        GistMessageType.K_AUDIO.typeString -> GistMessageType.K_AUDIO
                        GistMessageType.K_IMAGE.typeString -> GistMessageType.K_IMAGE
                        GistMessageType.K_VIDEO.typeString -> GistMessageType.K_VIDEO
                        else -> GistMessageType.K_IMAGE
                    }
                    val timeStampString = msg.getString("timeStamp")
                    val content = deEscapeContent(msg.getString("content"))
                    val externalUrl = when (messageType) {
                        "KImage", "KAudio", "KVideo" -> content
                        else -> null
                    }
                    GistMessage(
                        id = msg.getString("id"),
                        gistId = msg.getString("gistId"),
                        senderId = msg.getInt("senderId"),
                        senderName = msg.getString("fullName"),
                        content = content,
                        status = GistMessageStatus.Sent,
                        messageType = theMessageType,
                        timeStamp = timeStampString,
                        externalUrl = externalUrl
                    )
                }
                val currentMessages = _messages.value
                val updatedMessages = currentMessages + messages
                _messages.value = updatedMessages
                updatedMessages.forEach { message ->
                    handleMediaDownload(message)
                }
            }

            SharedCliqueReceivingType.GIST_REFRESH_UPDATE -> {
                val isSpeaker = jsonObject.getBoolean("isSpeaker")
                val isOwner = jsonObject.getBoolean("isOwner")
                val activeSpectators = jsonObject.getInt("activeSpectators")
                _userStatus.value = UserStatus(isSpeaker = isSpeaker, isOwner = isOwner)
                _gistTopRow.value = _gistTopRow.value?.copy(
                    activeSpectators = activeSpectators.toString()
                )
                viewModelScope.launch(Dispatchers.IO) {
                    getGistStateDao.updateActiveSpectators(activeSpectators)
                    getGistStateDao.insertGistState(
                        GistStateEntity(
                            gistId = _gistTopRow.value?.gistId ?: "",
                            topic = _gistTopRow.value?.topic ?: "",
                            description = _gistTopRow.value?.gistDescription ?: "",
                            startedBy = _gistTopRow.value?.startedBy ?: "",
                            startedById = _gistTopRow.value?.startedById ?: 0,
                            activeSpectators = activeSpectators,
                            gistImage = _gistTopRow.value?.gistImage ?: "",
                            isSpeaker = isSpeaker,
                            isOwner = isOwner
                        )
                    )
                }
            }

            SharedCliqueReceivingType.ROLE_UPDATE -> {
                val isSpeaker = jsonObject.getBoolean("isSpeaker")
                val isOwner = jsonObject.getBoolean("isOwner")
                _userStatus.value = UserStatus(isSpeaker = isSpeaker, isOwner = isOwner)
                viewModelScope.launch(Dispatchers.IO) {
                    val currentGistState = getGistStateDao.getGistState()
                    val existingActiveSpectators = currentGistState?.activeSpectators ?: 1
                    getGistStateDao.insertGistState(
                        GistStateEntity(
                            gistId = _gistTopRow.value?.gistId ?: "",
                            topic = _gistTopRow.value?.topic ?: "",
                            description = _gistTopRow.value?.gistDescription ?: "",
                            startedBy = _gistTopRow.value?.startedBy ?: "",
                            startedById = _gistTopRow.value?.startedById ?: 0,
                            activeSpectators = existingActiveSpectators,
                            gistImage = _gistTopRow.value?.gistImage ?: "",
                            isSpeaker = isSpeaker,
                            isOwner = isOwner
                        )
                    )
                }
            }

            SharedCliqueReceivingType.MEMBERS_LIST -> {
                val membersArray = jsonObject.getJSONArray("members")
                val membersList = (0 until membersArray.length()).map { member ->
                    val theMember = membersArray.getJSONObject(member)
                    val customerId = theMember.getInt("userId")
                    val fullName = theMember.getString("fullName")
                    val isSpeaker = theMember.getBoolean("isSpeaker")
                    val isOwner = theMember.getBoolean("isOwner")
                    Members(
                        customerId = customerId,
                        fullName = fullName,
                        isOwner = isOwner,
                        isSpeaker = isSpeaker
                    )
                }
                fetchMembersAndCompare(membersList)
            }

            SharedCliqueReceivingType.MEMBER_LEFT -> {
                val userId = jsonObject.getInt("userId")
                removeMemberById(userId)
            }

            SharedCliqueReceivingType.MEMBER_JOINED -> {
                val userId = jsonObject.getInt("userId")
                val fullName = jsonObject.getString("fullName")
                val isSpeaker = jsonObject.getBoolean("isSpeaker")
                val isOwner = jsonObject.getBoolean("isOwner")
                val member = Members(
                    customerId = userId,
                    fullName = fullName,
                    isSpeaker = isSpeaker,
                    isOwner = isOwner
                )
                addNewMember(member)
            }

            SharedCliqueReceivingType.SUBSCRIBER_ROLE_UPDATE -> {
                val userId = jsonObject.getInt("userId")
                val fullName = jsonObject.getString("fullName")
                val newRole = jsonObject.getString("newRole")

                val isOwner = newRole == "owner"
                val isSpeaker = newRole == "speaker" || isOwner
                val updatedMember = Members(
                    customerId = userId,
                    fullName = fullName,
                    isOwner = isOwner,
                    isSpeaker = isSpeaker
                )
                updateMemberRole(updatedMember)
            }

            SharedCliqueReceivingType.EXIT_GIST -> {
                val context = getApplication<Application>().applicationContext
                _gistCreatedOrJoined.value = null
                _messages.value = emptyList()
                _userStatus.value = UserStatus(isSpeaker = false, isOwner = false)
                _gistTopRow.value = null
                clearCacheDirectory(context)
                clearGistState()
                downloadedMediaUrls.clear()
            }
        }
    }

    private fun clearGistState() {
        viewModelScope.launch {
            getGistStateDao.deleteGistState()
        }
    }

    fun sendBinary(
        data: ByteArray,
        type: String,
        gistId: String = "",
        messageId: String = "",
        customerId: Int = 0,
        fullName: String = ""
    ) {
        val metadata = "$type:$gistId:$messageId:$customerId:$fullName"
        val metadataBytes = metadata.toByteArray(Charsets.UTF_8)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(ByteBuffer.allocate(4).putInt(metadataBytes.size).array())
        outputStream.write(metadataBytes)
        outputStream.write(data)

        val combinedBytes = outputStream.toByteArray()
        WebSocketManager.sendBinary(combinedBytes)
        updateGistTimestamp()

        Log.d(
            "sendBinaryInputs",
            "Data: ${data.size}, Type: $type, GistId: $gistId, MessageId: $messageId, CustomerId: $customerId, FullName: $fullName"
        )
    }

    private fun clearCacheDirectory(context: Context) {
        val cacheDir = context.cacheDir
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        }
    }

    fun addMessage(message: GistMessage) {
        Log.d("Logging", "Add message logging: ${_messages.value}")
        if (_gistCreatedOrJoined.value?.gistId == message.gistId){
            val updatedMessages = _messages.value.toMutableList()
            val messageExists = updatedMessages.any { it.id == message.id }
            if (!messageExists) {
                updatedMessages.add(0, message)
                _messages.value = updatedMessages
                Log.d("Logging", "Add message post-logging: ${_messages.value}")
                Log.d("CliqueViewModel", "Message added: $message")
                if (message.externalUrl != null && message.localPath == null) {
                    handleMediaDownload(message)
                    Log.d("Called", "handle media called external")
                }
            } else {
                Log.d("CliqueViewModel", "Duplicate message prevented: $message")
            }
        }
    }

    fun loadMessages(gistId: String) {
        Log.i("SharedCliqueViewModel", "Loading messages for gistId: $gistId")
        val message = """
            {
                "type": "loadMessages",
                "gistId": "$gistId"
            }
        """.trimIndent()
        send(message)
    }

    fun startGist(topic: String, type: String, description: String) {
        val message = """
            {
                "type": "createGist",
                "topic": "$topic",
                "gistType": "$type",
                "description": "$description"
            }
        """.trimIndent()
        send(message)
    }

    fun loadOlderMessages(lastMessageId: String, gistId: String) {
        Log.d("loading", "loading older messages")
        val message = """
            {
            "type": "loadOlderMessages",
            "lastMessageId": "$lastMessageId",
            "gistId": "$gistId"
            }
            """.trimMargin()
        send(message)
    }

    fun clearGistCreationError() {
        _gistCreationError.value = null
    }

    private fun messageAcknowledged(gistId: String, messageId: String) {
        Log.d(
            "WebSocketManager",
            "Message acknowledged: gistId=$gistId, messageId=$messageId"
        )
        val updatedMessages = _messages.value.toMutableList()
        val messageIndex =
            updatedMessages.indexOfFirst { it.gistId == gistId && it.id == messageId }
        if (messageIndex != -1) {
            val message = updatedMessages[messageIndex]
            val updatedMessage = message.copy(status = GistMessageStatus.Sent)
            updatedMessages[messageIndex] = updatedMessage
            _messages.value = updatedMessages
            Log.d(
                "WebSocketManager",
                "Message status updated to 'sent': gistId=$gistId, messageId=$messageId"
            )
        }
    }

    // Additional ViewModel-specific logic

    fun send(message: String) {
        WebSocketManager.send(message)
    }

    private val _myName = mutableStateOf("")
    private val myName: String
        get() = _myName.value

    fun setMyName(name: String) {
        _myName.value = name
        Log.d("MyName", "My name is: $myName")
    }

    fun handleTrimmedVideo(uri: Uri?) {
        uri?.let { validUri ->
            viewModelScope.launch {
                try {
                    val context = getApplication<Application>().applicationContext
                    val videoByteArray =
                        context.contentResolver.openInputStream(validUri)?.readBytes() ?: ByteArray(
                            0
                        )
                    Log.d("ChatRoom", "Video Byte Array: ${videoByteArray.size} bytes")

                    val messageId = generateUUIDString()
                    sendBinary(
                        videoByteArray,
                        GistMessageType.K_VIDEO.typeString,
                        gistId,
                        messageId,
                        customerId,
                        fullName = myName
                    )
                    val videoUri =
                        getUriFromByteArray(videoByteArray, context, GistMediaType.KVideo)
                    val gistMessage = GistMessage(
                        id = messageId,
                        gistId = gistId,
                        senderId = customerId,
                        senderName = myName,
                        content = "",
                        status = GistMessageStatus.Pending,
                        messageType = GistMessageType.K_VIDEO,
                        localPath = videoUri,
                        timeStamp = System.currentTimeMillis().toString()
                    )
                    Log.d("senderId", "CustomerId: $customerId")
                    addMessage(gistMessage)
                } catch (e: IOException) {
                    Log.e("ChatRoom", "Error processing video: ${e.message}", e)
                }
            }
        }
    }

    private fun formatUserCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(
                Locale.US,
                "%dK",
                count / 1_000
            ) // Keep in thousands up to a million
            else -> count.toString()
        }
    }

    private suspend fun loadContacts(): List<Contact> {
        return contactDao.getSortedContacts().map { it.toContact() }
    }

    private fun compareAndUpdateMembers(members: List<Members>, contacts: List<Contact>) {
        val contactSet = contacts.mapNotNull { it.customerId }.toSet()
        val updatedMembers = members.map { member ->
            member.copy(isContact = contactSet.contains(member.customerId))
        }
        sortMembersByContact(updatedMembers.toMutableList())
    }

    private fun fetchMembersAndCompare(members: List<Members>) {
        viewModelScope.launch {
            val contacts = loadContacts()
            Log.d("Contacts", "$contacts")
            compareAndUpdateMembers(members, contacts)
        }
    }
    fun fetchMembersFromServer(gistId: String) {
        val fetchRequest = """
            {
            "type": "fetchMembersFromServer",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(fetchRequest)
    }

    private fun sortMembersByContact(members: MutableList<Members>) {
        val contacts = members.filter { it.isContact }
        val nonContacts = members.filter { !it.isContact }
        val owners = members.filter { it.isOwner }
        val speakers = members.filter { it.isSpeaker && !it.isOwner }
        _listOfContactMembers.value = contacts
        _listOfNonContactMembers.value = nonContacts
        _listOfOwners.value = owners
        _listOfSpeakers.value = speakers
    }

    private fun generateMembersList() {
        val membersList = mutableListOf<Members>()
        for (i in 1..40) { // Generate 40 members
            val owner = Random.nextFloat() < 0.3
            val speaker = if (owner) true else Random.nextFloat() < 0.45
            membersList.add(
                Members(
                    customerId = i, fullName = "User ${Random.nextInt(1000)}", // Random full names
                    isContact = Random.nextBoolean(), isOwner = owner, isSpeaker = speaker,
                )
            )
        }
        sortMembersByContact(membersList)
    }

    private fun handleMediaDownload(message: GistMessage) {
        Log.d("Called", "handle media called internal")
        if (message.externalUrl != null && message.localPath == null) {
            Log.d("Called", "handle media called internal 2")
            val mediaType = GistMediaType.fromString(message.messageType.typeString)
            if (mediaType == null) {
                Log.e("SharedCliqueViewModel", "Unknown media type: ${message.messageType}")
                return
            }

            when (val state = downloadedMediaUrls[message.externalUrl]) {
                is DownloadState.Downloaded -> {
                    Log.d("Called", "handle media called internal 3")
                    updateMessageLocalPath(message.id, state.uri)
                    return
                }

                is DownloadState.Downloading -> {
                    Log.d("Called", "Download already in progress for ${message.externalUrl}")
                    return
                }

                else -> {
                    // Proceed to download
                }
            }

            Log.d("Called", "handle media called internal 7")
            downloadedMediaUrls[message.externalUrl] = DownloadState.Downloading
            Log.d("Called", "handle media called internal 8")

            viewModelScope.launch {
                Log.d("Called", "handle media called internal 9")
                try {
                    Log.d("Called", "handle media called internal 4")
                    val byteArray = downloadFromUrl(message.externalUrl)
                    val uri = getUriFromByteArray(byteArray, getApplication(), mediaType)
                    downloadedMediaUrls[message.externalUrl] = DownloadState.Downloaded(uri)
                    Log.d("Called", "handle media called internal 5: ${byteArray.size} bytes")
                    updateMessageLocalPath(message.id, uri)
                } catch (e: Exception) {
                    Log.e(
                        "SharedCliqueViewModel",
                        "Failed to download media for message ${message.id}",
                        e
                    )
                    // Remove from cache on failure
                    downloadedMediaUrls.remove(message.externalUrl)
                    // Optionally, update the message with an error state or notify the user
                    // For example:
                    // updateMessageWithError(message.id, "Failed to load media")
                }
            }
        }
    }

    /**
     * Updates the local path of a message.
     */
    private fun updateMessageLocalPath(messageId: String, uri: Uri) {
        Log.d("Called", "handle media called internal 6")
        val updatedMessages = _messages.value.map { message ->
            Log.d("Local path Message id", "${message.id} and $messageId")
            if (message.id == messageId) {
                message.copy(localPath = uri)
            } else {
                message
            }
        }.toMutableList()
        _messages.value = updatedMessages
    }

    fun removeAsSpeaker(userId: Int) {
        val message = """{
            "type": "removeAsSpeaker",
            "userId": $userId,
            "gistId": "$gistId"
            }""".trimMargin()
        send(message)
    }

    fun makeOwner(userId: Int) {
        val message = """{
            "type": "makeOwner",
            "userId": $userId,
            "gistId": "$gistId"
            }""".trimMargin()
        send(message)
    }

    fun makeSpeaker(userId: Int) {
        val message = """{
            "type": "makeSpeaker",
            "userId": $userId,
            "gistId": "$gistId"
            }""".trimMargin()
        send(message)
    }

    private fun removeMemberById(memberId: Int) {
        _listOfContactMembers.value =
            _listOfContactMembers.value.filter { it.customerId != memberId }
        _listOfNonContactMembers.value =
            _listOfNonContactMembers.value.filter { it.customerId != memberId }
        _listOfOwners.value = _listOfOwners.value.filter { it.customerId != memberId }
        _listOfSpeakers.value = _listOfSpeakers.value.filter { it.customerId != memberId }
    }

    private fun addNewMember(newMember: Members) {
        viewModelScope.launch {
            val contacts = loadContacts()
            val contactSet = contacts.mapNotNull { it.customerId }.toSet()
            val isContact = contactSet.contains(newMember.customerId)
            val updatedMember = newMember.copy(isContact = isContact)

            if (updatedMember.isContact) {
                _listOfContactMembers.value += updatedMember
            } else {
                _listOfNonContactMembers.value += updatedMember
            }
            if (updatedMember.isOwner) {
                _listOfOwners.value += updatedMember
            }
            if (updatedMember.isSpeaker && !updatedMember.isOwner) {
                _listOfSpeakers.value += updatedMember
            }
        }
    }

    private fun updateMemberRole(updatedMember: Members) {
        removeMemberById(updatedMember.customerId)
        addNewMember(updatedMember)
    }

    fun sendUpdatedDescription(editedText: String, gistId: String) {
        viewModelScope.launch {
            Log.d("GistDescription", "called")
            try {
                val jsonBody = """{
                "descriptionUpdate": "$editedText",
                "gistId": "$gistId"
                }
            """.trimMargin()
                val response = NetworkUtils.makeRequest(
                    "updateGistDescription",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody
                )
                if (response.first) {
                    val jsonObject = JSONObject(response.second)
                    val description = jsonObject.getString("description")
                    _gistTopRow.value = _gistTopRow.value?.copy(gistDescription = description)
                }
            } catch (e: Exception){
                Log.e("NetworkUtils", "Network request failed: ${e.message}")
            }
        }
    }

    fun fetchGistComments(loadingMore: Boolean = false, lastCommentId: String? = null, userId: Int) {
        val params = mutableMapOf("gistId" to gistId, "userId" to "$userId")
        if (loadingMore) {
            params["loadingMore"] = "true"
            lastCommentId?.let {
                params["lastCommentId"] = lastCommentId
            }
        }
        viewModelScope.launch {
            try {
                val responseString =
                    NetworkUtils.makeRequest(
                        "fetchGistComments",
                        KliqueHttpMethod.GET,
                        params
                    ).second
                val parsedGistComments = parseGistCommentsResponseString(responseString)
                if (loadingMore) {
                    _comments.value += parsedGistComments
                } else {
                    _comments.value = parsedGistComments
                }
            } catch (e: Exception){
                Log.e("NetworkUtils", "Network request failed: ${e.message}")
            }
        }
    }

    private fun parseGistCommentsResponseString(theJson: String): List<GistComment> {
        val commentsJsonArray = JSONArray(theJson)
        val commentsList = mutableListOf<GistComment>()

        for (i in 0 until commentsJsonArray.length()) {
            val commentJson = commentsJsonArray.getJSONObject(i)
            val id = commentJson.getString("id")
            val fullName = commentJson.getString("fullName")
            val comment = commentJson.getString("comment")
            val customerId = commentJson.getInt("userId")
            val upVotes = commentJson.getInt("upVotes")
            val upVotedByYou = commentJson.getBoolean("upVotedByYou")
            val repliesJsonArray = commentJson.getJSONArray("replies")
            val repliesList = mutableListOf<Reply>()

            for (j in 0 until repliesJsonArray.length()) {
                val replyJson = repliesJsonArray.getJSONObject(j)
                val replyId = replyJson.getString("id")
                val replyFullName = replyJson.getString("fullName")
                val replyCustomerId = replyJson.getInt("userId")
                val replyText = replyJson.getString("reply")

                val reply = Reply(
                    id = replyId,
                    fullName = replyFullName,
                    customerId = replyCustomerId,
                    reply = replyText
                )
                repliesList.add(reply)
            }

            val gistComment = GistComment(
                id = id,
                fullName = fullName,
                comment = comment,
                customerId = customerId,
                replies = repliesList,
                upVotes = upVotes,
                upVotedByYou = upVotedByYou
            )
            commentsList.add(gistComment)
        }
        return commentsList
    }

    fun sendGistComment(comment: String, isReply: Boolean, commentId: String? = null, userId: Int) {
        val endpoint = if (isReply) "commentReply" else "mainComment"
        val theCommentId = commentId ?: generateUUIDString()
        val commentReplyId = if (isReply) generateUUIDString() else null
        val jsonBody = if (isReply) {
            """
    {
        "comment": "$comment",
        "commentId": "$theCommentId",
        "userId": $userId,
        "gistId": "$gistId",
        "commentReplyId": "$commentReplyId"
    }
    """
        } else {
            """
    {
        "comment": "$comment",
        "commentId": "$theCommentId",
        "userId": $userId,
        "gistId": "$gistId"
    }
    """
        }
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val response =
                    NetworkUtils.makeRequest(endpoint, KliqueHttpMethod.POST, emptyMap(), jsonBody)
                if (response.first) {
                    val myNewComment = GistComment(
                        comment = comment,
                        fullName = "My Comment",
                        customerId = customerId,
                        replies = emptyList(),
                        id = theCommentId,
                        upVotes = 0
                    )
                    val myNewReply = Reply(
                        id = theCommentId,
                        fullName = "My Comment Reply",
                        customerId = customerId,
                        reply = comment,
                    )
                    if (!isReply) {
                        _comments.value += myNewComment
                    } else {
                        _comments.value = _comments.value.map {
                            if (it.id == commentId) {
                                it.copy(replies = it.replies + myNewReply)
                            } else {
                                it
                            }
                        }
                    }
                }
            } catch (e: Exception){
                Log.e("NetworkUtils", "Network request failed: ${e.message}")
            }
        }
    }

    fun sendUpVotes(commentId: String) {
        val params = mapOf(
            "commentId" to commentId,
            "gistId" to gistId, "userId" to "$customerId"
        )
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkUtils.makeRequest("addUpVotes", KliqueHttpMethod.GET, params)
                if (response.first) {
                    if (response.second.contains("true")) {
                        _comments.value = _comments.value.map { comment ->
                            if (comment.id == commentId) comment.copy(upVotes = comment.upVotes + 1, upVotedByYou = true) else {
                                comment
                            }
                        }
                    }
                    if (response.second.contains("false")) {
                        _comments.value = _comments.value.map { comment ->
                            if (comment.id == commentId) comment.copy(upVotes = comment.upVotes - 1, upVotedByYou = false) else {
                                comment
                            }
                        }
                    }
                }
            } catch (e: Exception){
                Log.e("NetworkUtils", "Network request failed: ${e.message}")
            }
        }
    }
}

class SharedCliqueViewModelFactory(
    private val application: Application,
    private val customerId: Int,
    private val contactDao: ContactDao,
    private val getGistStateDao: GistStateDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedCliqueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return SharedCliqueViewModel(
                application,
                customerId,
                contactDao,
                getGistStateDao
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

sealed class DownloadState {
    object NotDownloaded : DownloadState()
    object Downloading : DownloadState()
    data class Downloaded(val uri: Uri) : DownloadState()
}