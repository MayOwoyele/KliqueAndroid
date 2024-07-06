package com.justself.klique

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

class ChatScreenViewModel(private val chatDatabaseHelper: ChatDatabaseHelper) : ViewModel() {
    private val _chats = MutableLiveData<List<ChatList>>()
    val chats: LiveData<List<ChatList>> get() = _chats

    fun addChat(chat: ChatList) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDatabaseHelper.addChat(chat)
            loadChats()
        }
    }
    fun updateChat(chat: ChatList) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDatabaseHelper.updateChat(chat)
            loadChats()
        }
    }
    fun deleteChat(recipientId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDatabaseHelper.deleteChat(recipientId)
            loadChats()
        }
    }
    fun loadChats() {
        viewModelScope.launch(Dispatchers.IO) {
            val chatList = chatDatabaseHelper.getAllChats()
            // remember to remove this piece of code. Pass chatList as a value to .postValue and delete getMockChats
            val mockData = getMockChats()
            _chats.postValue(mockData)
        }
    }
    fun getMockChats(): List<ChatList> {
        return listOf(
            ChatList(
                contactName = "John Doe",
                customerId = "123",
                lastMsg = "Hey there!",
                lastMsgAddtime = "2024-07-05 12:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/men/1.jpg",
                recipientId = "456",
                unreadMsgCounter = 2
            ),
            ChatList(
                contactName = "Jane Smith",
                customerId = "124",
                lastMsg = "Hello!",
                lastMsgAddtime = "2024-07-05 14:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/women/2.jpg",
                recipientId = "457",
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Alice Johnson",
                customerId = "125",
                lastMsg = "Good morning! How are you, I hope you are doing well and I hope to see you tomorrow. Help me say hi to the family and thanks for yesterday",
                lastMsgAddtime = "2024-07-05 09:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/women/3.jpg",
                recipientId = "458",
                unreadMsgCounter = 1
            ),
            ChatList(
                contactName = "Bob Brown",
                customerId = "126",
                lastMsg = "Did you get my last message?",
                lastMsgAddtime = "2024-07-06 08:20:00",
                profilePhoto = "https://randomuser.me/api/portraits/men/4.jpg",
                recipientId = "459",
                unreadMsgCounter = 3
            ),
            ChatList(
                contactName = "Charlie Davis",
                customerId = "127",
                lastMsg = "Sure, let's meet at the usual place.",
                lastMsgAddtime = "2024-07-06 10:15:45",
                profilePhoto = "https://randomuser.me/api/portraits/men/5.jpg",
                recipientId = "460",
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Diana Evans",
                customerId = "128",
                lastMsg = "It was great catching up with you last night. Let's do it again soon!",
                lastMsgAddtime = "2024-07-06 11:30:25",
                profilePhoto = "https://randomuser.me/api/portraits/women/6.jpg",
                recipientId = "461",
                unreadMsgCounter = 5
            ),
            ChatList(
                contactName = "Ethan Fox",
                customerId = "129",
                lastMsg = "I've attached the documents you requested. Please review and let me know your thoughts.",
                lastMsgAddtime = "2024-07-06 13:45:30",
                profilePhoto = "https://randomuser.me/api/portraits/men/7.jpg",
                recipientId = "462",
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Fiona Green",
                customerId = "130",
                lastMsg = "Can't wait for the weekend! Do you have any plans?",
                lastMsgAddtime = "2024-07-06 14:50:10",
                profilePhoto = "https://randomuser.me/api/portraits/women/8.jpg",
                recipientId = "463",
                unreadMsgCounter = 2
            ),
            ChatList(
                contactName = "George Harris",
                customerId = "131",
                lastMsg = "Just sent you the files. Let me know if you need anything else.",
                lastMsgAddtime = "2024-07-06 16:00:00",
                profilePhoto = "https://randomuser.me/api/portraits/men/9.jpg",
                recipientId = "464",
                unreadMsgCounter = 1
            ),
            ChatList(
                contactName = "Hannah Irvine",
                customerId = "132",
                lastMsg = "Can we reschedule our meeting to next week? I've got a conflict.",
                lastMsgAddtime = "2024-07-06 17:10:45",
                profilePhoto = "https://randomuser.me/api/portraits/women/10.jpg",
                recipientId = "465",
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Ian Jackson",
                customerId = "133",
                lastMsg = "Thank you for your help! I really appreciate it.",
                lastMsgAddtime = "2024-07-06 18:25:30",
                profilePhoto = "https://randomuser.me/api/portraits/men/11.jpg",
                recipientId = "466",
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Jackie Kim",
                customerId = "134",
                lastMsg = "Do you have any recommendations for a good restaurant in town?",
                lastMsgAddtime = "2024-07-06 19:35:20",
                profilePhoto = "https://randomuser.me/api/portraits/women/12.jpg",
                recipientId = "467",
                unreadMsgCounter = 4
            ),
            ChatList(
                contactName = "Karen Lee",
                customerId = "135",
                lastMsg = "Let's catch up over coffee sometime this week.",
                lastMsgAddtime = "2024-07-06 20:45:50",
                profilePhoto = "https://randomuser.me/api/portraits/women/13.jpg",
                recipientId = "468",
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Larry Moore",
                customerId = "136",
                lastMsg = "I've been meaning to tell you about the new project I'm working on.",
                lastMsgAddtime = "2024-07-06 21:55:10",
                profilePhoto = "https://randomuser.me/api/portraits/men/14.jpg",
                recipientId = "469",
                unreadMsgCounter = 1
            ),
            ChatList(
                contactName = "Megan Nelson",
                customerId = "137",
                lastMsg = "Thanks for the invite! I'll definitely be there.",
                lastMsgAddtime = "2024-07-06 22:05:35",
                profilePhoto = "https://randomuser.me/api/portraits/women/15.jpg",
                recipientId = "470",
                unreadMsgCounter = 3
            ),
            ChatList(
                contactName = "Nathan Owens",
                customerId = "138",
                lastMsg = "Can you send me the details for tomorrow's meeting?",
                lastMsgAddtime = "2024-07-06 23:15:50",
                profilePhoto = "https://randomuser.me/api/portraits/men/16.jpg",
                recipientId = "471",
                unreadMsgCounter = 2
            )
        )
    }
}
class ChatViewModelFactory(private val chatDatabaseHelper: ChatDatabaseHelper) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatScreenViewModel(chatDatabaseHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}