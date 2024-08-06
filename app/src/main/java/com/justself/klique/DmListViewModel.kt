package com.justself.klique

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
data class DmItem(
    val imageLink: String,
    val fullName: String,
    val enemyId: Int,
    val lastMessage: LastMessage
)

sealed class LastMessage {
    data class Text(val content: String) : LastMessage()
    object Photo : LastMessage()
}

class DmListViewModel : ViewModel() {
    private val _dmList = MutableStateFlow<List<DmItem>>(emptyList())
    val dmList: StateFlow<List<DmItem>> = _dmList

    init {
        fetchDmList()
    }

    private fun fetchDmList() {
        // Replace with real data fetching logic
        _dmList.value = listOf(
            DmItem(
                imageLink = "https://example.com/user1.jpg",
                fullName = "John Doe",
                enemyId = 1,
                lastMessage = LastMessage.Text("Hey, how are you?")
            ),
            DmItem(
                imageLink = "https://example.com/user2.jpg",
                fullName = "Jane Smith",
                enemyId = 2,
                lastMessage = LastMessage.Photo
            ),
            // Add more dummy data as needed
        )
    }
}