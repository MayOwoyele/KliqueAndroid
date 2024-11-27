package com.justself.klique

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class ChatRoomOptionsViewModel : ViewModel() {
    private val _options = MutableStateFlow<List<ChatRoomOption>>(emptyList())
    val options: StateFlow<List<ChatRoomOption>> = _options.asStateFlow()
    fun fetchOptions(categoryId: Int) {
        val params = mapOf("categoryId" to "$categoryId")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    NetworkUtils.makeRequest("fetchChatRoomOptions", KliqueHttpMethod.GET, params)
                if (response.first) {
                    val optionsList = mutableListOf<ChatRoomOption>()
                    val responseJson = JSONArray(response.second)
                    for (i in 0 until responseJson.length()) {
                        val jsonObject = responseJson.getJSONObject(i)
                        val optionId = jsonObject.getInt("optionId")
                        val optionName = jsonObject.getString("optionName")
                        val optionImage = jsonObject.getString("optionImage")
                        val chatroomOption = ChatRoomOption(
                            chatRoomId = optionId,
                            optionChatRoomName = optionName,
                            optionChatRoomImage = optionImage
                        )
                        optionsList.add(chatroomOption)
                    }
                    _options.value = optionsList
                }
            } catch (e: Exception) {
                Log.e("ChatRoom Options", "The options are $e")
            }
        }
    }
}

data class ChatRoomOption(
    val chatRoomId: Int,
    val optionChatRoomName: String,
    val optionChatRoomImage: String
)