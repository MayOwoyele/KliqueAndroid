package com.justself.klique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class ChatRoomCategory(
    val categoryId: Int,
    val categoryName: String,
    val categoryImage: String
)

class ChatRoomsCategoryViewModel : ViewModel() {
    private val _campusesCategories = MutableStateFlow<List<ChatRoomCategory>>(emptyList())
    val campusesCategories: StateFlow<List<ChatRoomCategory>> = _campusesCategories.asStateFlow()

    private val _interestsCategories = MutableStateFlow<List<ChatRoomCategory>>(emptyList())
    val interestsCategories: StateFlow<List<ChatRoomCategory>> = _interestsCategories.asStateFlow()

    fun fetchCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = NetworkUtils.makeRequest(
                "fetchChatRoomCategories",
                KliqueHttpMethod.GET,
                emptyMap()
            )
            if (response.first) {
                val campusList = mutableListOf<ChatRoomCategory>()
                val interestList = mutableListOf<ChatRoomCategory>()
                val responseJson = JSONArray(response.second)
                for (i in 0 until responseJson.length()) {
                    val jsonObject = responseJson.getJSONObject(i)
                    val type = jsonObject.getString("categoryType")
                    val categoryId = jsonObject.getInt("categoryId")
                    val categoryName = jsonObject.getString("categoryName")
                    val categoryImage = jsonObject.getString("categoryImage")
                    val chatroomCategory = ChatRoomCategory(
                        categoryId = categoryId,
                        categoryName = categoryName,
                        categoryImage = categoryImage
                    )
                    when (type) {
                        "Campus" -> {
                            campusList.add(
                                chatroomCategory
                            )
                        }
                        "Interest" -> {
                            interestList.add(
                                chatroomCategory
                            )
                        }
                    }
                }
                _campusesCategories.value = campusList
                _interestsCategories.value = interestList
            }
        }
    }

    fun send(message: String) {
        WebSocketManager.send(message)
    }
}