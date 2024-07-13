// File: SharedCliqueViewModel.kt
package com.justself.klique.gists.ui.viewModel
import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.justself.klique.ChatMessage
import com.justself.klique.WebSocketListener
import com.justself.klique.WebSocketManager
import com.justself.klique.deEscapeContent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.random.Random


//TODO: Discuss with May about the List of Gists, How we are getting gists from server,
class SharedCliqueViewModel(application: Application, private val customerId: Int) : AndroidViewModel(application),
    WebSocketListener {
    override val listenerId: String = "SharedCliqueViewModel"

    // LiveData properties for observing state changes
    private val _gistCreatedOrJoined = MutableLiveData<Pair<String, String>?>()
    val gistCreatedOrJoined: LiveData<Pair<String, String>?> = _gistCreatedOrJoined
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages
    private val gistId: String
        get() = _gistCreatedOrJoined.value?.second ?: ""
    private var messageCounter = 0
    private val _myName = mutableStateOf("")
    private val _isSpeaker = MutableLiveData(true)
    val isSpeaker: LiveData<Boolean> get() = _isSpeaker
    private val myName: String
        get() = _myName.value

    fun setMyName(name: String) {
        _myName.value = name
        Log.d("MyName", "My name is: $myName")
    }
    init {
        WebSocketManager.registerListener(this)
        initializeMessageCounter()
        simulateGistCreated()
        startUpdatingUserCount()
    }
    // remove this function later here and in the init block
    fun simulateGistCreated() {
            val topic = "Kotlin"
            val gistId = "1b345kt"
            _gistCreatedOrJoined.postValue(Pair(topic, gistId))
    }

    override fun onCleared() {
        super.onCleared()
        WebSocketManager.unregisterListener(this)
    }

    private fun initializeMessageCounter() {
        messageCounter = 0
    }

    fun generateMessageId(): Int {
        messageCounter += 1
        return messageCounter
    }

    override fun onMessageReceived(type: String, jsonObject: JSONObject) {
        when (type) {
            "gistCreated" -> {
                val gistId = jsonObject.getString("gistId")
                val topic = jsonObject.getString("topic")
                _gistCreatedOrJoined.postValue(Pair(topic, gistId))
            }
            "previousMessages" -> {
                val gistId = jsonObject.getString("gistId")
                val messagesJsonArray = jsonObject.getJSONArray("messages")
                val messages = (0 until messagesJsonArray.length()).map { i ->
                    val msg = messagesJsonArray.getJSONObject(i)
                    ChatMessage(
                        id = msg.getInt("id"),
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
                val messageId = jsonObject.getInt("id")
                val customerId = jsonObject.getInt("customerId")
                val fullName = jsonObject.getString("fullName")
                val content = deEscapeContent(jsonObject.getString("content"))
                val message = ChatMessage(id = messageId, gistId = gistId, customerId = customerId, sender = fullName, content = content, status = "received")
                addMessage(message)
            }
            "messageAck" -> {
                val gistId = jsonObject.getString("gistId")
                val messageId = jsonObject.getInt("id")
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
        val messageId = jsonObject.getInt("id")
        println("Received message with Id: $messageId")
        val binaryData = jsonObject.getString("binaryData")

        val message = ChatMessage(
            id = messageId,
            gistId = "",  // Adjust as necessary
            customerId = 0,  // Adjust as necessary
            sender = "",  // Adjust as necessary
            content = binaryData,
            status = "received",
            messageType = type
        )
        addMessage(message)
    }
    fun sendBinary(data: ByteArray, type: String, gistId: String, messageId: Int, customerId: Int, fullName: String) {
        val prefix = "$type:$gistId:$messageId:$customerId:$fullName".padEnd(50)
        val prefixBytes = prefix.toByteArray(Charsets.UTF_8)
        val message = prefixBytes + data
        WebSocketManager.sendBinary(message)
    }
    fun addMessage(message: ChatMessage) {
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

    fun isGistActive(): Boolean {
        return _gistCreatedOrJoined.value != null
    }

    private fun messageAcknowledged(gistId: String, messageId: Int, messageType: String = "text") {
        Log.d("WebSocketManager", "Message acknowledged: gistId=$gistId, messageId=$messageId, messageType=$messageType")
        val updatedMessages = _messages.value.orEmpty().toMutableList()
        val messageIndex = updatedMessages.indexOfFirst { it.gistId == gistId && it.id == messageId && it.messageType == messageType }
        if (messageIndex != -1) {
            val message = updatedMessages[messageIndex]
            val updatedMessage = message.copy(status = "sent")
            updatedMessages[messageIndex] = updatedMessage
            _messages.postValue(updatedMessages)
            Log.d("WebSocketManager", "Message status updated to 'sent': gistId=$gistId, messageId=$messageId")
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
    fun handleTrimmedVideo(uri: Uri?) {
        uri?.let { validUri ->
            viewModelScope.launch {
                try {
                    val context = getApplication<Application>().applicationContext
                    val videoByteArray = context.contentResolver.openInputStream(validUri)?.readBytes() ?: ByteArray(0)
                    Log.d("ChatRoom", "Video Byte Array: ${videoByteArray.size} bytes")

                    val messageId = generateMessageId()
                    sendBinary(videoByteArray, "KVideo", gistId, messageId, customerId, fullName = myName)

                    val chatMessage = ChatMessage(
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
                    addMessage(chatMessage)
                } catch (e: IOException) {
                    Log.e("ChatRoom", "Error processing video: ${e.message}", e)
                }
            }
        }
    }
    private val _formattedUserCount = MutableStateFlow(formatUserCount(0))
    val formattedUserCount: StateFlow<String> = _formattedUserCount.asStateFlow()

    fun updateUserCount(newCount: Int) {
        userCount = newCount
        _formattedUserCount.value = formatUserCount(newCount)
    }

    private fun formatUserCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 100_000 -> String.format(Locale.US, "%dK", count / 100_000 * 100)
            count >= 1_000 -> String.format(Locale.US, "%dK", count / 1_000)
            else -> count.toString()
        }
    }
    private var userCount = 0
    private fun startUpdatingUserCount() {
        viewModelScope.launch {
            while (isActive) {
                delay(3000) // Delay for 3 seconds
                val randomIncrement = Random.nextInt(1, 1_000)
                updateUserCount(userCount + randomIncrement)
            }
        }
    }
}

class SharedCliqueViewModelFactory(
    private val application: Application,
    private val customerId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedCliqueViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedCliqueViewModel(application, customerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}