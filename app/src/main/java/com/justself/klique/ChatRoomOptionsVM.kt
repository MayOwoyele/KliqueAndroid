package com.justself.klique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatRoomOptionsViewModel : ViewModel() {
    private val _options = MutableStateFlow<List<ChatRoomOption>>(emptyList())
    val options: StateFlow<List<ChatRoomOption>> = _options.asStateFlow()
    init {
        populateOptions()
    }
    fun fetchOptions(categoryId: Int) {
        val fetchMessage = """
            {
            "type": "fetchOptions",
            "categoryId": "$categoryId"
            }
        """.trimIndent()
        send(fetchMessage)
    }
    fun send(fetchMessage: String) {
        WebSocketManager.send(fetchMessage)
    }
    private fun populateOptions(){
        viewModelScope.launch {
            // Replace with actual data fetching logic
            val fetchedOptions = listOf(
                ChatRoomOption(1, "Option 1", "https://example.com/image1.jpg"),
                ChatRoomOption(2, "Option 2", "https://example.com/image2.jpg"),
                ChatRoomOption(3, "Option 3", "https://example.com/image3.jpg")
            )
            _options.value = fetchedOptions
        }
    }
}

data class ChatRoomOption(
    val chatRoomId: Int,
    val optionChatRoomName: String,
    val optionChatRoomImage: String
)