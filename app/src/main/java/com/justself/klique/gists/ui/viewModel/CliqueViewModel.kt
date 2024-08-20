// File: SharedCliqueViewModel.kt
package com.justself.klique.gists.ui.viewModel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.ContactDao
import com.justself.klique.GistMessage
import com.justself.klique.GistState
import com.justself.klique.GistTopRow
import com.justself.klique.Members
import com.justself.klique.UserStatus
import com.justself.klique.WebSocketListener
import com.justself.klique.WebSocketManager
import com.justself.klique.deEscapeContent
import com.justself.klique.toContact
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class SharedCliqueViewModel(application: Application, private val customerId: Int, private val contactDao: ContactDao) :
    AndroidViewModel(application), WebSocketListener {
    override val listenerId: String = "SharedCliqueViewModel"

    // LiveData properties for observing state changes
    private val _gistCreatedOrJoined = MutableLiveData<GistState?>()
    val gistCreatedOrJoined: LiveData<GistState?> = _gistCreatedOrJoined
    private val _messages = MutableLiveData<List<GistMessage>>(emptyList())
    val messages: LiveData<List<GistMessage>> = _messages
    private val gistId: String
        get() = _gistCreatedOrJoined.value?.gistId ?: ""

    private val _userStatus = MutableLiveData(UserStatus(isSpeaker = true, isOwner = true))
    val userStatus: LiveData<UserStatus> = _userStatus

    // Add this to SharedCliqueViewModel
    private val _gistCreationError = MutableLiveData<String?>()
    val gistCreationError: LiveData<String?> = _gistCreationError

    private val _listOfContactMembers = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfNonContactMembers = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfOwners = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfSpeakers = MutableStateFlow<List<Members>>(emptyList())
    val listOfNonContactMembers = _listOfNonContactMembers.asStateFlow()
    val listOfContactMembers = _listOfContactMembers.asStateFlow()
    val listOfOwners = _listOfOwners.asStateFlow()
    val listOfSpeakers = _listOfSpeakers.asStateFlow()

    // this val should be replaced by server info someway, somehow
    private val _gistTopRow =
        MutableStateFlow(
            GistTopRow(
                gistId = "",
                topic = "",
                gistDescription = "This is a gist description",
                activeSpectators = "",
                gistImage = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"
            )
        )
    val gistTopRow = _gistTopRow.asStateFlow()

    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
    private val _searchResults = MutableLiveData<List<Members>>()
    val searchResults: LiveData<List<Members>> = _searchResults
    private val _searchPerformed = MutableLiveData(false)
    val searchPerformed: LiveData<Boolean> = _searchPerformed


    init {
        WebSocketManager.registerListener(this)
        //simulateGistCreated()
        generateMembersList()
        //startUpdatingActiveSpectators()
    }

    // remove this function later here and in the init block, it is simply for simulation purpose
    fun simulateGistCreated() {
        val topic = "Kotlin"
        val gistId = "1b345kt"
        _gistCreatedOrJoined.postValue(GistState(topic, gistId))
        _gistTopRow.value = _gistTopRow.value.copy(
            gistId = gistId,
            topic = topic,
            gistDescription = "This is to change the gist description"
        )
    }
    // handle websocket's acknowledgment for entering the gist by assigning a value to gistJoinedOrCreated
    fun enterGist(gistId: String){
        val enterGistId = """
            {
            "type": "enterGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(enterGistId)
    }
    // handle the exit message by setting gistJoinedOrCreated to null when it arrives
    fun exitGist(){
        val exitGistId = """
            {
            "type": "exitGist",
            "gistId": "$gistId"
            }
        """.trimIndent()
        send(exitGistId)
        _gistCreatedOrJoined.postValue(null)
    }
    fun floatGist(gistId: String){
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
        _searchPerformed.value = true
        val searchMessage = """
            {
            "type": "GistSearch",
            "searchQuery": "$searchQuery"
            }
        """.trimIndent()
        Log.d("WebSocketManager", "Sending search message: $searchMessage")
        send(searchMessage)
    }
    fun turnSearchPerformedOff(){
        _searchPerformed.value = false
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
    fun generateMessageId(): String {
        val randomUUID = UUID.randomUUID().toString()
        return randomUUID
    }

    override fun onMessageReceived(type: String, jsonObject: JSONObject) {
        when (type) {
            "gistCreated" -> {
                val gistId = jsonObject.getString("gistId")
                val topic = jsonObject.getString("topic")
                _gistCreatedOrJoined.postValue(GistState(topic, gistId))
                _gistTopRow.value = _gistTopRow.value.copy(gistId = gistId, topic = topic)
            }
            "gistCreationError" -> {
                val errorMessage = jsonObject.getString("message")
                _gistCreationError.postValue(errorMessage)
            }
            "previousMessages" -> {
                val gistId = jsonObject.getString("gistId")
                val messagesJsonArray = jsonObject.getJSONArray("messages")
                val messages = (0 until messagesJsonArray.length()).map { i ->
                    val msg = messagesJsonArray.getJSONObject(i)
                    GistMessage(
                        id = msg.getString("id"),
                        gistId = msg.getString("gistId"),
                        customerId = msg.getInt("customerId"),
                        sender = msg.getString("fullName"),
                        content = deEscapeContent(msg.getString("content")),
                        status = msg.getString("status"),
                        messageType = msg.getString("messageType")
                    )
                }
                _messages.postValue(messages)
            }

            "message" -> {
                val gistId = jsonObject.getString("gistId")
                val messageId = jsonObject.getString("id")
                val customerId = jsonObject.getInt("customerId")
                val fullName = jsonObject.getString("fullName")
                val content = deEscapeContent(jsonObject.getString("content"))
                val message = GistMessage(
                    id = messageId,
                    gistId = gistId,
                    customerId = customerId,
                    sender = fullName,
                    content = content,
                    status = "received"
                )
                addMessage(message)
            }

            "messageAck" -> {
                val gistId = jsonObject.getString("gistId")
                val messageId = jsonObject.getString("id")
                val ackMessage = jsonObject.getString("message")
                val messageType = jsonObject.optString("messageType", "text")
                Log.i("WebSocketManager", "Message acknowledged: $ackMessage")
                messageAcknowledged(gistId, messageId, messageType)
            }

            "KImage", "KAudio", "KVideo" -> {
                handleBinaryMessage(type, jsonObject)
            }
        }
    }

    private fun handleBinaryMessage(type: String, jsonObject: JSONObject) {
        val messageId = jsonObject.getString("id")
        println("Received message with Id: $messageId")
        val binaryData = jsonObject.getString("binaryData")

        val message = GistMessage(
            id = messageId, gistId = "",  // Adjust as necessary
            customerId = 0,  // Adjust as necessary
            sender = "",  // Adjust as necessary
            content = binaryData, status = "received", messageType = type
        )
        addMessage(message)
    }

    fun sendBinary(
        data: ByteArray,
        type: String,
        gistId: String = "",
        messageId: String = "",
        customerId: Int = 0,
        fullName: String = ""
    ) {
        val prefix = "$type:$gistId:$messageId:$customerId:$fullName".padEnd(50)
        val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
        val message = prefixBytes + data
        WebSocketManager.sendBinary(message)
        Log.d(
            "sendBinaryInputs",
            "Data: ${data.size}, Type: $type, GistId: $gistId, MessageId: $messageId, CustomerId: $customerId, FullName: $fullName"
        )
    }

    fun addMessage(message: GistMessage) {
        val updatedMessages = _messages.value.orEmpty().toMutableList()
        updatedMessages.add(message)
        _messages.postValue(updatedMessages)
        Log.d("CliqueViewModel", "Message added: $message")
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

    fun startGist(topic: String, type: String) {
        val message = """
            {
                "type": "createGist",
                "topic": "$topic",
                "gistType": "$type"
            }
        """.trimIndent()
        send(message)
    }
    fun clearGistCreationError() {
        _gistCreationError.value = null
    }
    fun simulateGistCreationError() {
        // This is a simulated error message for testing purposes
        _gistCreationError.postValue("You can't create more than 5 gists. Please float an existing gist from 'My Gists'.")
    }
    fun isGistActive(): Boolean {
        return _gistCreatedOrJoined.value != null
    }

    private fun messageAcknowledged(gistId: String, messageId: String, messageType: String = "text") {
        Log.d(
            "WebSocketManager",
            "Message acknowledged: gistId=$gistId, messageId=$messageId, messageType=$messageType"
        )
        val updatedMessages = _messages.value.orEmpty().toMutableList()
        val messageIndex =
            updatedMessages.indexOfFirst { it.gistId == gistId && it.id == messageId && it.messageType == messageType }
        if (messageIndex != -1) {
            val message = updatedMessages[messageIndex]
            val updatedMessage = message.copy(status = "sent")
            updatedMessages[messageIndex] = updatedMessage
            _messages.postValue(updatedMessages)
            Log.d(
                "WebSocketManager",
                "Message status updated to 'sent': gistId=$gistId, messageId=$messageId"
            )
        }
    }

    // Additional ViewModel-specific logic
    fun connect(url: String, customerId: Int, fullName: String) {
        WebSocketManager.connect(url, customerId, fullName)
    }

    fun send(message: String) {
        WebSocketManager.send(message)
    }

    fun close() {
        WebSocketManager.close()
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

                    val messageId = generateMessageId()
                    sendBinary(
                        videoByteArray, "KVideo", gistId, messageId, customerId, fullName = myName
                    )

                    val gistMessage = GistMessage(
                        id = messageId,
                        gistId = gistId,
                        customerId = customerId,
                        sender = myName,
                        content = "",
                        status = "pending",
                        messageType = "KVideo",
                        binaryData = videoByteArray
                    )
                    Log.d("customerId", "CustomerId: $customerId")
                    addMessage(gistMessage)
                } catch (e: IOException) {
                    Log.e("ChatRoom", "Error processing video: ${e.message}", e)
                }
            }
        }
    }

    fun updateSpectatorCount(newCount: Int) {
        _gistTopRow.value = _gistTopRow.value.copy(activeSpectators = (formatUserCount(newCount)))
    }

    var activeSpectatorsCount = 0
    fun startUpdatingActiveSpectators() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Delay for 3 seconds
                val randomIncrement = Random.nextInt(1, 10000)
                activeSpectatorsCount += randomIncrement
                updateSpectatorCount(activeSpectatorsCount)
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
    fun compareAndUpdateMembers(members: List<Members>, contacts: List<Contact>) {
        val contactSet = contacts.mapNotNull {it.customerId}.toSet()
        val updatedMembers = members.map {member ->
            member.copy(isContact = contactSet.contains(member.customerId))
        }
        sortMembersByContact(updatedMembers.toMutableList())
    }
    // this function will be changed to react to the websocket on Message instead..
    // fetchMembersFromServer will eventually have no return type
    fun fetchMembersAndCompare(members: List<Members>) {
        viewModelScope.launch {
            val contacts = loadContacts()
            Log.d("Contacts", "$contacts")
            compareAndUpdateMembers(members, contacts)
        }
    }
    // trigger the fetchMembersFromServer function first using the existing LaunchedEffect
    // Use the onMessage function to trigger fetchMembersAndCompare
    // Pass the membersFromServer List to fetchMembersAndCompare
    fun fetchMembersFromServer(gistId: String) {
        try {
            val fetchRequest = """
            {
            "type": "fetchMembersFromServer",
            "gistId": "$gistId"
            }
        """.trimIndent()
            send(fetchRequest)
        } catch (e: Exception) {
            Log.e("SharedCliqueViewModel", "Failed to send fetch request: ${e.message}")
        }
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

    //This is a test function for testing the sorting power

    fun generateMembersList() {
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

}

class SharedCliqueViewModelFactory(
    private val application: Application, private val customerId: Int, private val contactDao: ContactDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedCliqueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return SharedCliqueViewModel(application, customerId, contactDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}