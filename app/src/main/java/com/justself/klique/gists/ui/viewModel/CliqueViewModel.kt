// File: SharedCliqueViewModel.kt
package com.justself.klique.gists.ui.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.justself.klique.ChatMessage
import com.justself.klique.WebSocketListener
import com.justself.klique.WebSocketManager
import com.justself.klique.deEscapeContent
import org.json.JSONObject

class SharedCliqueViewModel(application: Application, private val customerId: Int) : AndroidViewModel(application),
    WebSocketListener {
    override val listenerId: String = "SharedCliqueViewModel"

    // LiveData properties for observing state changes
    private val _gistCreatedOrJoined = MutableLiveData<Pair<String, String>?>()
    val gistCreatedOrJoined: LiveData<Pair<String, String>?> = _gistCreatedOrJoined
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages
    private var messageCounter = 0

    init {
        WebSocketManager.registerListener(this)
        initializeMessageCounter()
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