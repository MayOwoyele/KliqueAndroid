package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.HttpURLConnection


class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages
    private val _sendMessageStatus = MutableStateFlow(SendMessageStatus.None)
    val sendMessageStatus: StateFlow<SendMessageStatus> = _sendMessageStatus

    private val activePollingInterval = 5000L
    private var pollingJob: Job? = null
    fun fetchMessages(customerId: Int, chatPartnerId: Int) {
        viewModelScope.launch {
            val params = mapOf(
                "action" to "getMessages",
                "senderId" to customerId.toString(),
                "chatPartnerId" to chatPartnerId.toString()
            )

            try {
                val (response, statusCode) = NetworkUtils.makeRequestWithStatusCode(
                    "api.php",
                    "GET",
                    params
                )

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    val messagesList = parseMessages(response)
                    _messages.value = messagesList
                } else {

                }
            } catch (e: IOException) {
                Log.e("ChatViewModel", "Failed to fetch messages: ${e.message}", e)
            }
        }
    }


    private fun parseMessages(jsonString: String): List<Message> {
        if (jsonString.isBlank() || jsonString == "[]") {

            return emptyList() // Return an empty list if the JSON string is empty or just an empty array
        }
        return try {
            val messageList = JsonConfig.json.decodeFromString(MessageList.serializer(), jsonString)
            messageList.data // Return the list of messages from the parsed JSON
        } catch (e: SerializationException) {
            Log.e("ChatViewModel", "Failed to parse messages: ${e.message}", e)
            emptyList() // Return an empty list in case of serialization error
        }
    }


    fun sendMessage(customerId: Int, chatPartnerId: Int, messageText: String) {
        viewModelScope.launch {
            if (messageText.isNotBlank()) {
                val response = NetworkUtils.makeRequestWithStatusCode(
                    endpoint = "api.php",  // Endpoint for API
                    method = "POST",  // Ensuring POST is used
                    params = mapOf(
                        "action" to "sendMessage",
                        "senderId" to customerId.toString(),
                        "chatPartnerId" to chatPartnerId.toString(),
                        "message" to messageText
                    )
                )
                if (response.second == HttpURLConnection.HTTP_OK) {
                    fetchMessages(customerId, chatPartnerId)  // Refresh messages after sending
                    _sendMessageStatus.value = SendMessageStatus.Success
                } else {
                    Log.e("ChatViewModel", "Failed to send message: ${response.first}")
                    _sendMessageStatus.value = SendMessageStatus.Failure
                }
            }
        }
    }
    enum class SendMessageStatus {
        None, Success, Failure
    }
    fun resetSendMessageStatus() {
        _sendMessageStatus.value = SendMessageStatus.None
    }

    fun startPolling(customerId: Int, chatPartnerId: Int) {
        pollingJob = viewModelScope.launch {
            while (isActive) {  // Check that the coroutine is still active
                Log.d("ChatViewModel", "Polling for messages...")  // Log before fetching
                fetchMessages(customerId, chatPartnerId)
                delay(activePollingInterval)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d("ChatViewModel", "Polling stopped.")  // Log when polling is stopped
    }
    private fun parseSendMessageResponse(jsonString: String): Boolean {
        // Parse JSON response from the server after attempting to send a message
        return Gson().fromJson(jsonString, Boolean::class.java)
    }
}
