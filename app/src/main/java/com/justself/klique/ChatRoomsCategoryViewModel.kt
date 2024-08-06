package com.justself.klique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatRoomCategory(
    val categoryId: Int,
    val categoryName: String,
    val categoryImage: String
)
class ChatRoomsCategoryViewModel: ViewModel() {
    private val _campusesCategories = MutableStateFlow<List<ChatRoomCategory>>(emptyList())
    val campusesCategories: StateFlow<List<ChatRoomCategory>> = _campusesCategories.asStateFlow()

    private val _interestsCategories = MutableStateFlow<List<ChatRoomCategory>>(emptyList())
    val interestsCategories: StateFlow<List<ChatRoomCategory>> = _interestsCategories.asStateFlow()

    fun fetchCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val newCampusesCategories = listOf(
                ChatRoomCategory(
                    categoryId = 1,
                    categoryName = "Moremi Hall",
                    categoryImage = "https://picsum.photos/200/300"
                ),
                ChatRoomCategory(
                    categoryId = 2,
                    categoryName = "Honors",
                    categoryImage = "https://picsum.photos/200/301"
                ),
                ChatRoomCategory(
                    categoryId = 3,
                    categoryName = "Kofo",
                    categoryImage = "https://picsum.photos/200/302"
                )
            )
            val newInterestsCategories = listOf(
                ChatRoomCategory(
                    categoryId = 4,
                    categoryName = "Technology",
                    categoryImage = "https://samplelib.com/sample-jpeg.html"
                ),
                ChatRoomCategory(
                    categoryId = 5,
                    categoryName = "Music",
                    categoryImage = "https://graydart.com/sample/images/jpg"
                ),
                ChatRoomCategory(
                    categoryId = 6,
                    categoryName = "Sports",
                    categoryImage = "https://picsum.photos/200/304"
                )
            )
            _campusesCategories.value = newCampusesCategories
            _interestsCategories.value = newInterestsCategories
        }
    }
    fun send(message: String){
        WebSocketManager.send(message)
    }
}