package com.justself.klique

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


class SharedCliqueViewModel(application: Application, private val customerId: Int) : AndroidViewModel(application) {
    // WebSocketManager initialized with ViewModel reference
    private val webSocketManager = WebSocketManager(this)

    // LiveData properties for observing state changes
    private val _gistCreatedOrJoined = MutableLiveData<Pair<String, Int>?>()
    val gistCreatedOrJoined: LiveData<Pair<String, Int>?> = _gistCreatedOrJoined
    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages
    private var chatActive = false
    private var messageCounter = 0

    init {
        // Initialize messageCounter
        initializeMessageCounter()
    }

    private fun initializeMessageCounter() {
        // This is a simplified way to ensure messageCounter starts from a consistent state
        messageCounter = 0
    }

    fun generateMessageId(): Int {
        messageCounter += 1
        return customerId * 1000 + messageCounter
    }

    // Connect to WebSocket if chat is not active
    fun connect(url: String, customerId: Int, fullName: String) {
        if (!chatActive) {
            webSocketManager.connect(url, customerId, fullName)
        }
    }

    // Send message through WebSocket
    fun send(message: String) {
        webSocketManager.send(message)
    }

    // Close WebSocket connection
    fun close() {
        webSocketManager.close()
    }

    // Start a new gist and send WebSocket message
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

    // Join an existing gist and load messages from WebSocket
    fun joinGist(gistId: Int) {
        val message = """
            {
                "type": "joinGist",
                "gistId": $gistId
            }
        """.trimIndent()
        send(message)
        loadMessages(gistId) // Load messages when joining a gist
        chatActive = true
    }

    // Load messages from the server
    fun loadMessages(gistId: Int) {
        Log.i("SharedCliqueViewModel", "Loading messages for gistId: $gistId")
        val message = """
            {
                "type": "loadMessages",
                "gistId": $gistId
            }
        """.trimIndent()
        send(message)
    }

    fun handlePreviousMessages(gistId: Int, messages: List<ChatMessage>) {
        _messages.postValue(messages)
    }

    fun setGistCreatedOrJoined(topic: String, gistId: Int) {
        _gistCreatedOrJoined.postValue(Pair(topic, gistId))
        chatActive = true
    }

    fun messageAcknowledged(gistId: Int, messageId: Int, messageType: String = "text") {
        val updatedMessages = _messages.value.orEmpty().toMutableList()
        val messageIndex = updatedMessages.indexOfFirst { it.gistId == gistId && it.id == messageId && it.messageType == messageType }
        if (messageIndex != -1) {
            val message = updatedMessages[messageIndex]
            val updatedMessage = message.copy(status = "sent")
            updatedMessages[messageIndex] = updatedMessage
            _messages.postValue(updatedMessages)
        }
    }

    fun addMessage(message: ChatMessage) {
        val updatedMessages = _messages.value.orEmpty().toMutableList()
        updatedMessages.add(message)
        _messages.postValue(updatedMessages)
    }

    fun isGistActive(): Boolean {
        return chatActive
    }

    fun clearGist() {
        chatActive = false
        _messages.postValue(emptyList())
        _gistCreatedOrJoined.postValue(null)
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
