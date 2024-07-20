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
import com.justself.klique.GistMessage
import com.justself.klique.GistTopRow
import com.justself.klique.Members
import com.justself.klique.UserStatus
import com.justself.klique.WebSocketListener
import com.justself.klique.WebSocketManager
import com.justself.klique.deEscapeContent
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


//TODO: Discuss with May about the List of Gists, How we are getting gists from server,
class SharedCliqueViewModel(application: Application, private val customerId: Int) :
    AndroidViewModel(application), WebSocketListener {
    override val listenerId: String = "SharedCliqueViewModel"

    // LiveData properties for observing state changes
    private val _gistCreatedOrJoined = MutableLiveData<Pair<String, String>?>()
    val gistCreatedOrJoined: LiveData<Pair<String, String>?> = _gistCreatedOrJoined
    private val _messages = MutableLiveData<List<GistMessage>>(emptyList())
    val messages: LiveData<List<GistMessage>> = _messages
    private val gistId: String
        get() = _gistCreatedOrJoined.value?.second ?: ""

    private val _userStatus = MutableLiveData(UserStatus(isSpeaker = true, isOwner = true))
    val userStatus: LiveData<UserStatus> = _userStatus

    private val _listOfContactMembers = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfNonContactMembers = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfOwners = MutableStateFlow<List<Members>>(emptyList())
    private val _listOfSpeakers = MutableStateFlow<List<Members>>(emptyList())
    val listOfNonContactMembers = _listOfNonContactMembers.asStateFlow()
    val listOfContactMembers = _listOfContactMembers.asStateFlow()
    val listOfOwners = _listOfOwners.asStateFlow()
    val listOfSpeakers = _listOfSpeakers.asStateFlow()

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


    init {
        WebSocketManager.registerListener(this)
        simulateGistCreated()
        generateMembersList()
        //startUpdatingActiveSpectators()
    }

    /**
    remove this function later here and in the init block, it is simply for simulation purpose
     * @property simulateGistCreated
     */
    fun simulateGistCreated() {
        val topic = "Kotlin"
        val gistId = "1b345kt"
        _gistCreatedOrJoined.postValue(Pair(topic, gistId))
        _gistTopRow.value = _gistTopRow.value.copy(
            gistId = gistId,
            topic = topic,
            gistDescription = "This is to change the gist description"
        )
    }
    fun doTheSearch(searchQuery: String) {
        val searchMessage = """
            "type": "GistSearch"
            "searchQuery": "$searchQuery"
        """.trimIndent()
        Log.d("WebSocketManager", "Sending search message: $searchMessage")
        send(searchMessage)
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
                _gistCreatedOrJoined.postValue(Pair(topic, gistId))
                _gistTopRow.value = _gistTopRow.value.copy(gistId = gistId, topic = topic)
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

    fun setBitmap(bitmap: Bitmap) {
        _bitmap.value = bitmap
    }

    fun clearBitmap() {
        Log.d("Bitmap", "Bitmap cleared")
        _bitmap.value = null
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

    fun generateMembersList() {
        val membersList = mutableListOf<Members>()
        for (i in 1..40) { // Generate 100 members
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
    private val application: Application, private val customerId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedCliqueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return SharedCliqueViewModel(application, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}