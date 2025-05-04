// File: SharedCliqueViewModel.kt
package com.justself.klique.gists.ui.viewModel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.justself.klique.BinaryBufferObject
import com.justself.klique.BufferObject
import com.justself.klique.ChatScreenViewModel
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
import com.justself.klique.GistType
import com.justself.klique.WsDataType
import com.justself.klique.KliqueHttpMethod
import com.justself.klique.ListenerIdEnum
import com.justself.klique.Logger
import com.justself.klique.Members
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.NetworkUtils
import com.justself.klique.Reply
import com.justself.klique.Screen
import com.justself.klique.SessionManager
import com.justself.klique.SharedCliqueReceivingType
import com.justself.klique.UserStatus
import com.justself.klique.WebSocketListener
import com.justself.klique.WebSocketManager
import com.justself.klique.deEscapeContent
import com.justself.klique.downloadFromUrl
import com.justself.klique.getUriFromByteArray
import com.justself.klique.gistMediaCacheDir
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.GistUiState
import com.justself.klique.gists.ui.shared_composables.LastGistComments
import com.justself.klique.loggerD
import com.justself.klique.networkTriple
import com.justself.klique.toContact
import com.justself.klique.toNetworkTriple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SharedCliqueViewModel(
    application: Application,
    private val customerId: Int,
    private val contactDao: ContactDao,
    private val getGistStateDao: GistStateDao
) :
    AndroidViewModel(application), WebSocketListener<SharedCliqueReceivingType> {
    override val listenerId: String = ListenerIdEnum.SHARED_CLIQUE.theId
    private val _uiState =
        MutableStateFlow(GistUiState(interactions = emptyList(), trendingGists = emptyList()))
    val uiState: StateFlow<GistUiState> = _uiState

    private val _gistCreatedOrJoined = MutableLiveData<GistState?>()
    val gistCreatedOrJoined: LiveData<GistState?> = _gistCreatedOrJoined
    private val _messages = MutableStateFlow<List<GistMessage>>(emptyList())
    val messages: StateFlow<List<GistMessage>> = _messages.asStateFlow()
    private val gistId: String
        get() = _gistCreatedOrJoined.value?.gistId ?: ""

    private val _userStatus = MutableLiveData(UserStatus(isSpeaker = false, isOwner = false))
    val userStatus: LiveData<UserStatus> = _userStatus

    private val _gistCreationError = MutableLiveData<String?>()
    val gistCreationError: LiveData<String?> = _gistCreationError

    private val _gistMessage = mutableStateOf(TextFieldValue(""))
    val gistMessage: State<TextFieldValue> = _gistMessage

    data class UserInfo(val userId: Int, val profileImageUrl: String?)

    private var lastFetchedUserIds: Set<Int> = emptySet()

    private val _users = MutableStateFlow<Map<Int, UserInfo>>(emptyMap())
    val users: StateFlow<Map<Int, UserInfo>> = _users
    private val _topGists = MutableStateFlow<List<GistModel>>(emptyList())
    val topGists: StateFlow<List<GistModel>> = _topGists

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
    private val _onlineContacts = MutableStateFlow<List<Int>>(emptyList())
    val onlineContacts = _onlineContacts.asStateFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount = _commentCount.asStateFlow()
    private val _nonFriends = MutableStateFlow<List<NonFriends>>(emptyList())
    val nonFriends = _nonFriends.asStateFlow()

    var post by mutableStateOf(TextFieldValue(""))
        private set

    init {
        WebSocketManager.registerListener(this)
        WebSocketManager.setSharedCliqueViewModel(this)
        viewModelScope.launch {
            restoreGistState()
        }
        startObservingMessages()
    }

    fun onGistMessageChange(newMessage: TextFieldValue) {
        _gistMessage.value = newMessage
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
        send(BufferObject(message = message))
    }

    fun clearMessage() {
        _gistMessage.value = TextFieldValue("")
    }


    fun fetchInteractions(customerId: Int) {
        viewModelScope.launch {
            try {
                val endpoint = "interactedGists"
                val method = KliqueHttpMethod.GET
                val params = mapOf("userId" to customerId.toString(), "sendLong" to true.toString())

                val response = NetworkUtils.makeRequest(
                    endpoint = endpoint,
                    method = method,
                    params = params
                ).second
                val gists = parseGistsFromResponse(response)
                _uiState.value = _uiState.value.copy(
                    interactions = gists
                )
            } catch (e: Exception) {
                Log.e("fetchGists", "Exception is $e")
            }
        }
    }

    fun fetchTrendingGists(customerId: Int) {
        viewModelScope.launch {
            try {
                val endpoint = "gists/trending"
                val method = KliqueHttpMethod.GET
                val params = mapOf("userId" to customerId.toString(), "sendLong" to true.toString())

                val response = NetworkUtils.makeRequest(
                    endpoint = endpoint,
                    method = method,
                    params = params
                ).second
                val gists = parseGistsFromResponse(response)
                Logger.d("Gists", "Gists: $gists")
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

    fun askForHomeOnlineContacts(contacts: List<Contact>) {
        val listOfInt: MutableList<Int?> = mutableListOf()
        for (contact in contacts) {
            if (contact.isAppUser) {
                listOfInt.add(contact.customerId)
            }
        }
        val theJson = JSONObject().apply {
            put("type", "askForHomeOnlineContacts")
            put("contacts", JSONArray(listOfInt))
        }
        send(BufferObject(WsDataType.HomeOnlineContacts, theJson.toString()))
    }

    fun unsubscribeToGfUpdates() {
        val theJson = JSONObject().apply {
            put("type", "unsubscribeToGFUpdates")
        }
        send(BufferObject(WsDataType.GistFormUnsubscription, theJson.toString()))
    }

    fun enterGist(gistId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = JSONObject().put("gistId", gistId)
            val action: suspend (NetworkUtils.JwtTriple) -> Unit = { it ->
                val jsonObject = JSONObject(it.toNetworkTriple().second)
                processNewGist(jsonObject, callRefresh = { requestRefreshGistAndPreviousMessages(it) })
            }
            val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {
                Log.e("EnterGist", "Error is ${it.toNetworkTriple().second}, error code ${it.toNetworkTriple().third}")
            }
            NetworkUtils.makeJwtRequest(
                "enterGist", KliqueHttpMethod.POST,
                emptyMap(), json.toString(), action = action, errorAction = errorAction
            )
        }
    }

    fun exitGist() {
        val exitGistId = """
            {
            "type": "exitGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(BufferObject(message = exitGistId), true)
    }
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
        send(BufferObject(WsDataType.UnsubscribeForGistSetting, messageJson))
    }

    fun generateUUIDString(): String {
        val randomUUID = UUID.randomUUID().toString()
        return randomUUID
    }

    override fun onMessageReceived(type: SharedCliqueReceivingType, jsonObject: JSONObject) {
        when (type) {
            SharedCliqueReceivingType.GIST_CREATED -> {
                processNewGist(jsonObject)
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
                viewModelScope.launch(Dispatchers.IO) {
                    fetchCommentsCount(gistId)
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
                Logger.d("Called", "Add message called")
                addMessage(message)
            }

            SharedCliqueReceivingType.GIST_MESSAGE_ACK -> {
                Logger.d("Called", "this function called")
                val gistId = jsonObject.getString("gistId")
                val messageId = jsonObject.getString("messageId")
                Log.i("Called", "Message acknowledged")
                messageAcknowledged(gistId, messageId)
            }

            SharedCliqueReceivingType.K_IMAGE, SharedCliqueReceivingType.K_AUDIO, SharedCliqueReceivingType.K_VIDEO -> {
                try {
                    Logger.d("GistMedia", "Called")
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
                    Logger.d("GistMedia", "Error is ${e.message}")
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
                loggerD("OlderMessages"){"Older messages indeed: $jsonObject"}
                val messagesArray = jsonObject.getJSONArray("theMessages")
                val messages = (0 until messagesArray.length()).map { i ->
                    val msg = messagesArray.getJSONObject(i)
                    val messageType = msg.getString("messageType")
                    val theMessageType = when (messageType) {
                        GistMessageType.K_AUDIO.typeString -> GistMessageType.K_AUDIO
                        GistMessageType.K_IMAGE.typeString -> GistMessageType.K_IMAGE
                        GistMessageType.K_VIDEO.typeString -> GistMessageType.K_VIDEO
                        GistMessageType.K_TEXT.typeString -> GistMessageType.K_TEXT
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

            SharedCliqueReceivingType.ONLINE_CONTACTS -> {
                try {
                    val onlineArray = jsonObject.getJSONArray("onlineContacts")
                    val onlineList = mutableListOf<Int>()
                    for (i in 0 until onlineArray.length()) {
                        onlineList.add(onlineArray.getInt(i))
                    }
                    _onlineContacts.value = onlineList
                    val nonFriendsArray = jsonObject.getJSONArray("nonFriends")
                    val nonFriendsList = mutableListOf<NonFriends>()
                    for (i in 0 until nonFriendsArray.length()) {
                        val nonFriendsObject = nonFriendsArray.getJSONObject(i)
                        val userId = nonFriendsObject.getInt("userId")
                        val name = nonFriendsObject.getString("name")
                        nonFriendsList.add(NonFriends(userId, name))
                    }
                    _nonFriends.value = nonFriendsList.toList()
                } catch (e: Exception) {
                    Log.e("OnlineContacts", "Exception is $e")
                }
            }

            SharedCliqueReceivingType.CONTACT_ONLINE -> {
                try {
                    val userId = jsonObject.getInt("userId")
                    _onlineContacts.value = _onlineContacts.value.toMutableList().apply {
                        if (!contains(userId)) add(userId)
                    }
                    Logger.d("OnlineContacts", "User came online: $userId")
                } catch (e: Exception) {
                    Log.e("OnlineContacts", "Error processing contact online: ${e.message}")
                }
            }

            SharedCliqueReceivingType.CONTACT_OFFLINE -> {
                try {
                    val userId = jsonObject.getInt("userId")
                    _onlineContacts.value = _onlineContacts.value.toMutableList().apply {
                        remove(userId)
                    }
                    Logger.d("OnlineContacts", "User went offline: $userId")
                } catch (e: Exception) {
                    Log.e("OnlineContacts", "Error processing contact offline: ${e.message}")
                }
            }
        }
    }

    private fun processNewGist(jsonObject: JSONObject, callRefresh: ((String) -> Unit)? = null) {
        val gistId = jsonObject.getString("gistId")
        val topic = jsonObject.getString("topic")
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
        viewModelScope.launch(Dispatchers.Main) {
            _gistCreatedOrJoined.postValue(GistState(topic, gistId))
            _gistTopRow.value = gistTopRowObject
            _userStatus.value = UserStatus(isSpeaker = isSpeaker, isOwner = isOwner)
        }
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
        if (callRefresh != null) {
            callRefresh(gistId)
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
        WebSocketManager.sendBinary(BinaryBufferObject(WsDataType.GistRoomChat, combinedBytes))
        updateGistTimestamp()
    }

    private suspend fun fetchCommentsCount(gistId: String) {
        val json = JSONObject().put("gistId", gistId)
        val result = multipurposeCaller(json, "fetchCommentsCount")
        if (result.first) {
            val jsonObject = JSONObject(result.second)
            val commentsCount = jsonObject.getInt("commentsCount")
            _commentCount.value = commentsCount
        }
    }

    private fun clearCacheDirectory(context: Context) {
        val mediaCacheDir = File(context.cacheDir, gistMediaCacheDir)
        if (mediaCacheDir.exists()) {
            mediaCacheDir.deleteRecursively()
        }
    }

    fun addMessage(message: GistMessage) {
        Logger.d("Logging", "Add message logging: ${_messages.value}")
        if (_gistCreatedOrJoined.value?.gistId == message.gistId) {
            val updatedMessages = _messages.value.toMutableList()
            val messageExists = updatedMessages.any { it.id == message.id }
            if (!messageExists) {
                updatedMessages.add(0, message)
                viewModelScope.launch(Dispatchers.Main) {
                    _messages.value = updatedMessages
                }
                Logger.d("Logging", "Add message post-logging: ${_messages.value}")
                Logger.d("CliqueViewModel", "Message added: $message")
                if (message.externalUrl != null && message.localPath == null) {
                    handleMediaDownload(message)
                    Logger.d("Called", "handle media called external")
                }
            } else {
                Logger.d("CliqueViewModel", "Duplicate message prevented: $message")
            }
        }
    }

    fun startGist(
        post: String,
        type: String,
        listOfUsers: List<Int>,
        selectedType: GistType,
        chatVM: ChatScreenViewModel,
        cleanup: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var shouldReturn = false
            val action: suspend (NetworkUtils.JwtTriple) -> Unit = {}
            val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = { result ->
                if (result.toNetworkTriple().third == 422) {
                    val errorMessage = JSONObject(result.toNetworkTriple().second).getString("error")
                    _gistCreationError.postValue(errorMessage)
                }
                shouldReturn = true
            }
            val query = mapOf("userId" to SessionManager.customerId.value.toString())
            NetworkUtils.makeJwtRequest(
                "checkIfUserGistEligible", KliqueHttpMethod.GET, query, action = action, errorAction = errorAction
            )
            if (shouldReturn) {
                return@launch
            }
            when (selectedType) {
                GistType.Public -> {
                    val inviteId = generateUUIDString()
                    sendGistCreator(chatVM, inviteId, listOfUsers, post, cleanup)
                }
                GistType.Private -> createGistAndNotify(post, type, chatVM, listOfUsers, cleanup)
            }
        }
    }

    private fun sendGistCreator(
        vm: ChatScreenViewModel,
        inviteId: String,
        listOfUsers: List<Int>,
        post: String,
        cleanup: () -> Unit
    ) {
        for (enemyId in listOfUsers) {
            vm.sendGistCreation(inviteId, enemyId, SessionManager.customerId.value, post)
        }
        cleanup()
    }

    private fun createGistAndNotify(
        post: String,
        type: String,
        vm: ChatScreenViewModel,
        listOfUsers: List<Int>,
        cleanup: () -> Unit
    ) {
        val data = buildJsonObject {
            put("gistType", type)
            put("post", post)
            put("messageId", generateUUIDString())
            put("senderName", SessionManager.fullName.value)
        }
        val json = buildJsonObject {
            put("type", "startGist")
            put("userId", SessionManager.customerId.value)
            put("data", data)
        }
        viewModelScope.launch(Dispatchers.IO) {
            var generatedGistId = ""
            var topic = ""
            try {
                val action: suspend (NetworkUtils.JwtTriple) -> Unit = { result ->
                    val jsonObject = JSONObject(result.toNetworkTriple().second)
                    val refreshCall = { gistId: String ->
                        requestRefreshGistAndPreviousMessages(gistId)
                        generatedGistId = gistId
                        topic = jsonObject.getString("topic")
                    }
                    processNewGist(jsonObject, refreshCall)
                    cleanup()
                }
                val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = { result ->
                    when (result.toNetworkTriple().third) {
                        409 -> {
                            withContext(Dispatchers.Main) {
                                val receivedJson = JSONObject(result.toNetworkTriple().second).getString("message")
                                _gistCreationError.value = receivedJson
                            }
                        }

                        else -> {
                            withContext(Dispatchers.Main) {
                                Logger.d("GistCreationError", "Error: ${result.toNetworkTriple().third}, ${result.toNetworkTriple().second}"
                                )
                                _gistCreationError.value = "Unknown error, perhaps network?"
                            }
                        }
                    }
                }
                NetworkUtils.makeJwtRequest(
                    "createGist", KliqueHttpMethod.POST, emptyMap(),
                    json.toString(), action = action, errorAction = errorAction
                )
                if (generatedGistId.isNotEmpty()) {
                    for (enemyId in listOfUsers) {
                        vm.sendGistInvite(
                            topic,
                            generatedGistId,
                            enemyId,
                            SessionManager.customerId.value
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Logger.d("GistCreationError", "Error: ${e.message}")
                    _gistCreationError.value = "Unknown error, perhaps network?"
                }
            }
        }
    }

    fun setGistBackground(messageId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val jsonBody = JSONObject().apply {
                put("messageId", messageId)
                put("gistId", gistTopRow.value?.gistId)
            }

            val action: suspend (NetworkUtils.JwtTriple) -> Unit = {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Background set", Toast.LENGTH_SHORT).show()
                }
            }
            val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Error setting background", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            NetworkUtils.makeJwtRequest(
                "setGistBackground", KliqueHttpMethod.POST, emptyMap(),
                jsonBody.toString(), action = action, errorAction = errorAction
            )
        }
    }

    fun createAltGistAndNotify(post: String, type: String, enemyId: Int, inviteId: String) {
        val data = buildJsonObject {
            put("gistType", type)
            put("post", post)
            put("messageId", generateUUIDString())
            put("inviteId", inviteId)
        }
        val json = buildJsonObject {
            put("userId", enemyId)
            put("memberUserId", customerId)
            put("data", data)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val action: suspend (NetworkUtils.JwtTriple) -> Unit = { result ->
                    val jsonObject = JSONObject(result.toNetworkTriple().second)
                    val refreshCall = { gistId: String ->
                        requestRefreshGistAndPreviousMessages(gistId)
                    }
                    processNewGist(jsonObject, refreshCall)
                }
                val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = { result ->
                    when (result.toNetworkTriple().third) {
                        409 -> {
                            withContext(Dispatchers.Main) {
                                val receivedJson = JSONObject(result.toNetworkTriple().second).getString("message")
                                _gistCreationError.value = receivedJson
                            }
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                Logger.d("GistCreationError", "Error: ${result.toNetworkTriple().third}, ${result.toNetworkTriple().second}"
                                )
                            }
                        }
                    }
                }
                NetworkUtils.makeJwtRequest(
                    "createAltGist", KliqueHttpMethod.POST, emptyMap(),
                    json.toString(), action = action, errorAction = errorAction
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Logger.d("GistCreationError", "Error: ${e.message}")
                    _gistCreationError.value = "Unknown error, perhaps network?"
                }
            }
        }
    }

    suspend fun joinGist(gistId: String) {
        val action: suspend (NetworkUtils.JwtTriple) -> Unit = { result ->
            val jsonObject = JSONObject(result.toNetworkTriple().second)
            val refreshCall = { gistId: String ->
                requestRefreshGistAndPreviousMessages(gistId)
            }
            processNewGist(jsonObject, refreshCall)
        }
        val errorAction: suspend (NetworkUtils.JwtTriple) -> Unit = {
            withContext(Dispatchers.Main) {
                _gistCreationError.value = "Unknown error, perhaps network?"
            }
        }
        val jsonBody = """{"gistId": "$gistId"}"""
        NetworkUtils.makeJwtRequest(
            "joinGist", KliqueHttpMethod.POST, emptyMap(),
            jsonBody, action = action, errorAction = errorAction
        )
    }

    fun loadOlderMessages(lastMessageId: String, gistId: String) {
        Logger.d("loading", "loading older messages")
        val message = """
            {
            "type": "loadOlderMessages",
            "lastMessageId": "$lastMessageId",
            "gistId": "$gistId"
            }
            """.trimMargin()
        send(BufferObject(WsDataType.LoadOlderGistMessages, message))
    }

    fun clearGistCreationError() {
        _gistCreationError.value = null
    }

    private fun messageAcknowledged(gistId: String, messageId: String) {
        Logger.d("WebSocketManager", "Message acknowledged: gistId=$gistId, messageId=$messageId")
        val updatedMessages = _messages.value.toMutableList()
        val messageIndex =
            updatedMessages.indexOfFirst { it.gistId == gistId && it.id == messageId }
        if (messageIndex != -1) {
            val message = updatedMessages[messageIndex]
            val updatedMessage = message.copy(status = GistMessageStatus.Sent)
            updatedMessages[messageIndex] = updatedMessage
            _messages.value = updatedMessages
            Logger.d("WebSocketManager", "Message status updated to 'sent': gistId=$gistId, messageId=$messageId")
        }
    }

    fun send(message: BufferObject, showToast: Boolean = false) {
        WebSocketManager.send(message, showToast)
    }

    private val _myName = mutableStateOf("")
    private val myName: String
        get() = _myName.value

    fun setMyName(name: String) {
        _myName.value = name
        Logger.d("MyName", "My name is: $myName")
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
                    Logger.d("ChatRoom", "Video Byte Array: ${videoByteArray.size} bytes")

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
                    Logger.d("senderId", "CustomerId: $customerId")
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
            Logger.d("Contacts", "$contacts")
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
        send(BufferObject(WsDataType.FetchGistMembers, fetchRequest))
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

    private fun handleMediaDownload(message: GistMessage) {
        Logger.d("Called", "handle media called internal")
        if (message.externalUrl != null && message.localPath == null) {
            Logger.d("Called", "handle media called internal 2")
            val mediaType = GistMediaType.fromString(message.messageType.typeString)
            if (mediaType == null) {
                Log.e("SharedCliqueViewModel", "Unknown media type: ${message.messageType}")
                return
            }

            when (val state = downloadedMediaUrls[message.externalUrl]) {
                is DownloadState.Downloaded -> {
                    Logger.d("Called", "handle media called internal 3")
                    updateMessageLocalPath(message.id, state.uri)
                    return
                }

                is DownloadState.Downloading -> {
                    Logger.d("Called", "Download already in progress for ${message.externalUrl}")
                    return
                }

                else -> {}
            }

            Logger.d("Called", "handle media called internal 7")
            downloadedMediaUrls[message.externalUrl] = DownloadState.Downloading
            Logger.d("Called", "handle media called internal 8")

            viewModelScope.launch {
                Logger.d("Called", "handle media called internal 9")
                try {
                    Logger.d("Called", "handle media called internal 4")
                    val byteArray = downloadFromUrl(message.externalUrl)
                    val uri = getUriFromByteArray(byteArray, getApplication(), mediaType)
                    downloadedMediaUrls[message.externalUrl] = DownloadState.Downloaded(uri)
                    Logger.d("Called", "handle media called internal 5: ${byteArray.size} bytes")
                    updateMessageLocalPath(message.id, uri)
                } catch (e: Exception) {
                    Log.e(
                        "SharedCliqueViewModel",
                        "Failed to download media for message ${message.id}",
                        e
                    )
                    downloadedMediaUrls.remove(message.externalUrl)
                }
            }
        }
    }

    /**
     * Updates the local path of a message.
     */
    private fun updateMessageLocalPath(messageId: String, uri: Uri) {
        _messages.update { currentList ->
            val index = currentList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                currentList.toMutableList().also {
                    it[index] = it[index].copy(localPath = uri)
                }
            } else {
                currentList
            }
        }
    }

    fun removeAsSpeaker(userId: Int) {
        val message = """{
            "type": "removeAsSpeaker",
            "userId": $userId,
            "gistId": "$gistId"
            }""".trimMargin()
        send(BufferObject(WsDataType.GistRoomChat,message))
    }

    fun makeOwner(userId: Int) {
        val message = """{
            "type": "makeOwner",
            "userId": $userId,
            "gistId": "$gistId"
            }""".trimMargin()
        send(BufferObject(WsDataType.GistRoomChat,message))
    }

    fun makeSpeaker(userId: Int) {
        val message = """{
            "type": "makeSpeaker",
            "userId": $userId,
            "gistId": "$gistId"
            }""".trimMargin()
        send(BufferObject(WsDataType.GistRoomChat,message))
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
            Logger.d("GistDescription", "called")
            try {
                val jsonBody = """{
                "descriptionUpdate": "$editedText",
                "gistId": "$gistId"
            }""".trimMargin()
                NetworkUtils.makeJwtRequest(
                    "updateGistDescription",
                    KliqueHttpMethod.POST,
                    params = emptyMap(),
                    jsonBody = jsonBody,
                    action = { response ->
                        try {
                            Logger.d("Updated Description", response.toNetworkTriple().second)
                            _gistTopRow.value =
                                _gistTopRow.value?.copy(gistDescription = editedText)
                            Logger.d("GistDescription", "Successfully updated description")
                        } catch (e: Exception) {
                            Log.e("GistDescription", "Error parsing response: ${e.message}")
                        }
                    },
                    errorAction = { response ->
                        Log.e("GistDescription", "Failed to update description: ${response.toNetworkTriple().second}")
                    }
                )
            } catch (e: Exception) {
                Log.e("GistDescription", "Exception: ${e.message}", e)
            }
        }
    }

    fun fetchGistComments(
        loadingMore: Boolean = false,
        lastCommentId: String? = null,
        userId: Int
    ) {
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
            } catch (e: Exception) {
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
            try {
                NetworkUtils.makeJwtRequest(
                    endpoint,
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody,
                    action = {
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
                    },
                    errorAction = { response ->
                        Log.e("NetworkUtils", "Error during request: ${response.toNetworkTriple().second}")
                    }
                )
            } catch (e: Exception) {
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
                NetworkUtils.makeJwtRequest(
                    "addUpVotes",
                    KliqueHttpMethod.GET,
                    params,
                    action = { response ->
                        if (response.toNetworkTriple().second.contains("true")) {
                            _comments.value = _comments.value.map { comment ->
                                if (comment.id == commentId) {
                                    comment.copy(upVotes = comment.upVotes + 1, upVotedByYou = true)
                                } else {
                                    comment
                                }
                            }
                        } else if (response.toNetworkTriple().second.contains("false")) {
                            _comments.value = _comments.value.map { comment ->
                                if (comment.id == commentId) {
                                    comment.copy(
                                        upVotes = comment.upVotes - 1,
                                        upVotedByYou = false
                                    )
                                } else {
                                    comment
                                }
                            }
                        }
                    },
                    errorAction = { response ->
                        Log.e("NetworkUtils", "Error during request: ${response.toNetworkTriple().second}")
                    }
                )
            } catch (e: Exception) {
                Log.e("NetworkUtils", "Network request failed: ${e.message}")
            }
        }
    }

    fun deleteMessageById(messageId: String) {
        val messageToSend = JSONObject()
        messageToSend.apply {
            put("type", "gistMessageDelete")
            put("messageId", messageId)
        }
        send(BufferObject(WsDataType.GistRoomChat, messageToSend.toString()), true)
    }

    fun reportMessageById(messageId: String) {
        val messageToSend = JSONObject()
        messageToSend.apply {
            put("type", "gistMessageReport")
            put("messageId", messageId)
        }
        send(BufferObject(WsDataType.GistRoomChat, messageToSend.toString()), true)
    }

    fun onPostChange(newValue: TextFieldValue) {
        post = newValue
    }
    fun inviteNonFriends(thePeople: List<Int>, gistContent: String) {
        val theUUID = generateUUIDString()
        val messageToSend = JSONObject().apply {
            put("type", "DGistCreation")
            put("messageId", theUUID)
            put("receivers", JSONArray(thePeople))
            put("inviteId", theUUID)
            put("gistContent", gistContent)
        }.toString()
        val theBufferObject = BufferObject(WsDataType.Miscellaneous, messageToSend)
        send(theBufferObject)
    }
    private fun startObservingMessages() {
        viewModelScope.launch {
            _messages.collect { messageList ->
                val uniqueUserIds = messageList
                    .map { it.senderId }
                    .filter { it != SessionManager.customerId.value }
                    .toSet()
                if (uniqueUserIds != lastFetchedUserIds) {
                    lastFetchedUserIds = uniqueUserIds
                    fetchUniqueUsers(uniqueUserIds)
                }
            }
        }
    }

    private fun fetchUniqueUsers(uniqueUserIds: Set<Int>) {
        viewModelScope.launch {
            val fetchedUsers = fetchUsersFromServer(uniqueUserIds)
            Logger.d("GistPP", "fetchUniqueUsers: ${fetchedUsers.second}, ${fetchedUsers.third}")
            if (fetchedUsers.first) {
                val jsonObject = JSONObject(fetchedUsers.second)
                val userMap: Map<Int, UserInfo> = jsonObject.keys().asSequence().associate { key ->
                    val userId = key.toInt()
                    val profileUrl = if (jsonObject.isNull(key)) null else jsonObject.getString(key)
                    userId to UserInfo(userId, profileUrl)
                }
                _users.value = userMap
            }
        }
    }

    private suspend fun fetchUsersFromServer(uniqueUserIds: Set<Int>): networkTriple {
        val json = JSONObject().apply {
            put("userIds", JSONArray(uniqueUserIds.toList()))
        }
        return multipurposeCaller(json, "fetchUsersPP")
    }

    private fun hashUrl(url: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun getCachedThumbFile(userId: Int, imageUrl: String): File {
        val hash = hashUrl(imageUrl)
        return File(appContext.cacheDir, "thumbs/${userId}_$hash.jpg")
    }

    private fun clearOldThumbs(userId: Int) {
        val thumbsDir = File(appContext.cacheDir, "thumbs")
        thumbsDir.listFiles()?.filter { it.name.startsWith("${userId}_") }?.forEach { it.delete() }
    }

    private fun downscaleBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleFactor = minOf(targetWidth / width.toFloat(), targetHeight / height.toFloat())
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun cacheNewThumb(
        userId: Int,
        imageUrl: String,
        bitmap: Bitmap,
        targetWidth: Int = 100,
        targetHeight: Int = 100
    ) {
        clearOldThumbs(userId)
        val file = getCachedThumbFile(userId, imageUrl)
        file.parentFile?.mkdirs()
        val scaledBitmap = downscaleBitmap(bitmap, targetWidth, targetHeight)
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
    }

    suspend fun getOrDownloadThumbnail(
        userId: Int,
        imageUrl: String,
        targetWidth: Int = 100,
        targetHeight: Int = 100
    ): Bitmap? {
        val cachedFile = getCachedThumbFile(userId, imageUrl)
        if (cachedFile.exists()) {
            Logger.d("CachedFileExists", "exists")
            return BitmapFactory.decodeFile(cachedFile.absolutePath)
        } else {
            Logger.d("CachedFileExists", "doesn't exists")
            val downloadedBitmap = downloadImage(imageUrl)
            if (downloadedBitmap != null) {
                cacheNewThumb(userId, imageUrl, downloadedBitmap, targetWidth, targetHeight)
                return BitmapFactory.decodeFile(cachedFile.absolutePath)
            }
        }
        return null
    }

    private suspend fun downloadImage(url: String): Bitmap? {
        return try {
            val byteArray = downloadFromUrl(url)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e(
                "GistPP",
                "Error converting downloaded file to Bitmap, url: $url, error: ${e.message}",
                e
            )
            null
        }
    }

    fun getTopGists() {
        viewModelScope.launch {
            try {
                val result = NetworkUtils.makeRequest("topGists", KliqueHttpMethod.GET, emptyMap())
                if (result.first) {
                    val topGists = parseGistsFromResponse(result.second)
                    _topGists.value = topGists
                }
            } catch (e: Exception) {
                Log.e("fetchGistsTop", "Network request failed: ${e.message}")
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

fun parseGistsFromResponse(response: String): List<GistModel> {
    Logger.d("fetchGists", response)
    val jsonArray = JSONObject(response).getJSONArray("gists")
    val gists = mutableListOf<GistModel>()
    try {
        for (i in 0 until jsonArray.length()) {
            val gistJson = jsonArray.getJSONObject(i)
            val gistJsonArray = gistJson.getJSONArray("lastGistPosts")
            val lastGistPostsList = mutableListOf<LastGistComments>()
            val gistType = gistJson.getString("gistType")
            val gistTypeType = GistType.fromString(gistType)
            for (j in 0 until gistJsonArray.length()) {
                val lastGistPostJson = gistJsonArray.getJSONObject(j)
                val lastGistComments = LastGistComments(
                    senderName = lastGistPostJson.getString("name"),
                    comment = lastGistPostJson.getString("message"),
                    userId = lastGistPostJson.getInt("userId")
                )
                lastGistPostsList.add(lastGistComments)
            }
            val gist = GistModel(
                gistId = gistJson.getString("id"),
                topic = gistJson.getString("topic"),
                description = gistJson.getString("description"),
                image = if (gistJson.isNull("imageUrl")) null else gistJson.getString("imageUrl"),
                activeSpectators = gistJson.getInt("activeSpectators"),
                gistType = gistTypeType,
                lastGistComments = lastGistPostsList,
                postImage = if (gistJson.isNull("postImage")) null else gistJson.getString("postImage"),
                postVideo = if (gistJson.isNull("postVideo")) null else gistJson.getString("postVideo"),
            )
            gists.add(gist)
        }
    } catch (e: Exception) {
        Log.e("fetchGists", "Exception is $e")
    }
    return gists
}

object CliqueViewModelNavigator {
    var post: String? = null
    var type: String? = null
    var inviteId: String? = null
    var enemyId: Int? = null
    var toActivate: Boolean = false
    fun setNavigator(
        post: String,
        type: String,
        inviteId: String,
        enemyId: Int,
        navController: NavController
    ) {
        this.post = post
        this.type = type
        this.inviteId = inviteId
        this.enemyId = enemyId
        this.toActivate = true
        Screen.Home.navigate(navController)
    }

    fun clearNavigator() {
        post = null
        type = null
        inviteId = null
        enemyId = null
        toActivate = false
    }
}

suspend fun multipurposeCaller(json: JSONObject, typeField: String): networkTriple {
    json.put("type", typeField)
    return NetworkUtils.makeRequest(
        "multipurpose",
        KliqueHttpMethod.POST,
        emptyMap(),
        json.toString()
    )
}
data class NonFriends (
    val userId: Int,
    val name: String
)