package com.justself.klique

import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatScreenViewModel(private val chatDao: ChatDao) : ViewModel() {
    private val _chats = MutableLiveData<List<ChatList>>()
    val chats: LiveData<List<ChatList>> get() = _chats
    private val myUserId = MutableLiveData(0)

    /*init {
        loadChats()
    }*/

    fun addChat(chat: ChatList) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.addChat(chat)
            loadChats(myUserId.value!!)
        }
    }

    fun updateChat(chat: ChatList) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateChat(chat)
            loadChats(myUserId.value!!)
        }
    }

    fun deleteChat(enemyId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteChat(enemyId)
            loadChats(myUserId.value!!)
        }
    }

    fun loadChats(myId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val chatList = chatDao.getAllChats(myId)
            /* mockData is for testing. chatList is from the real database
            val mockData = getMockChats(myId) */
            _chats.postValue(chatList)
            myUserId.postValue(myId)
        }
    }

    fun getMockChats(myId: Int): List<ChatList> {
        return listOf(
            ChatList(
                contactName = "John Doe",
                enemyId = 123,
                lastMsg = "Hey there!",
                lastMsgAddtime = "2024-07-05 12:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/men/1.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 2
            ),
            ChatList(
                contactName = "Jane Smith",
                enemyId = 124,
                lastMsg = "Hello!",
                lastMsgAddtime = "2024-07-05 14:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/women/2.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Alice Johnson",
                enemyId = 125,
                lastMsg = "Good morning! How are you, I hope you are doing well and I hope to see you tomorrow. Help me say hi to the family and thanks for yesterday",
                lastMsgAddtime = "2024-07-05 09:34:56",
                profilePhoto = "https://randomuser.me/api/portraits/women/3.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 1
            ),
            ChatList(
                contactName = "Bob Brown",
                enemyId = 126,
                lastMsg = "Did you get my last message?",
                lastMsgAddtime = "2024-07-06 08:20:00",
                profilePhoto = "https://randomuser.me/api/portraits/men/4.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 3
            ),
            ChatList(
                contactName = "Charlie Davis",
                enemyId = 127,
                lastMsg = "Sure, let's meet at the usual place.",
                lastMsgAddtime = "2024-07-06 10:15:45",
                profilePhoto = "https://randomuser.me/api/portraits/men/5.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Diana Evans",
                enemyId = 128,
                lastMsg = "It was great catching up with you last night. Let's do it again soon!",
                lastMsgAddtime = "2024-07-06 11:30:25",
                profilePhoto = "https://randomuser.me/api/portraits/women/6.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 55
            ),
            ChatList(
                contactName = "Ethan Fox",
                enemyId = 129,
                lastMsg = "I've attached the documents you requested. Please review and let me know your thoughts.",
                lastMsgAddtime = "2024-07-06 13:45:30",
                profilePhoto = "https://randomuser.me/api/portraits/men/7.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Fiona Green",
                enemyId = 130,
                lastMsg = "Can't wait for the weekend! Do you have any plans?",
                lastMsgAddtime = "2024-07-06 14:50:10",
                profilePhoto = "https://randomuser.me/api/portraits/women/8.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 2
            ),
            ChatList(
                contactName = "George Harris",
                enemyId = 131,
                lastMsg = "Just sent you the files. Let me know if you need anything else.",
                lastMsgAddtime = "2024-07-06 16:00:00",
                profilePhoto = "https://randomuser.me/api/portraits/men/9.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 1
            ),
            ChatList(
                contactName = "Hannah Irvine",
                enemyId = 132,
                lastMsg = "Can we reschedule our meeting to next week? I've got a conflict.",
                lastMsgAddtime = "2024-07-06 17:10:45",
                profilePhoto = "https://randomuser.me/api/portraits/women/10.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Ian Jackson",
                enemyId = 133,
                lastMsg = "Thank you for your help! I really appreciate it.",
                lastMsgAddtime = "2024-07-06 18:25:30",
                profilePhoto = "https://randomuser.me/api/portraits/men/11.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Jackie Kim",
                enemyId = 134,
                lastMsg = "Do you have any recommendations for a good restaurant in town?",
                lastMsgAddtime = "2024-07-06 19:35:20",
                profilePhoto = "https://randomuser.me/api/portraits/women/12.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 4
            ),
            ChatList(
                contactName = "Karen Lee",
                enemyId = 135,
                lastMsg = "Let's catch up over coffee sometime this week.",
                lastMsgAddtime = "2024-07-06 20:45:50",
                profilePhoto = "https://randomuser.me/api/portraits/women/13.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 0
            ),
            ChatList(
                contactName = "Larry Moore",
                enemyId = 136,
                lastMsg = "I've been meaning to tell you about the new project I'm working on.",
                lastMsgAddtime = "2024-07-06 21:55:10",
                profilePhoto = "https://randomuser.me/api/portraits/men/14.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 1
            ),
            ChatList(
                contactName = "Megan Nelson",
                enemyId = 137,
                lastMsg = "Thanks for the invite! I'll definitely be there.",
                lastMsgAddtime = "2024-07-06 22:05:35",
                profilePhoto = "https://randomuser.me/api/portraits/women/15.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 3
            ),
            ChatList(
                contactName = "Nathan Owens",
                enemyId = 138,
                lastMsg = "Can you send me the details for tomorrow's meeting?",
                lastMsgAddtime = "2024-07-06 23:15:50",
                profilePhoto = "https://randomuser.me/api/portraits/men/16.jpg",
                myId = myId,  // Use the provided myId
                unreadMsgCounter = 2
            )
        )
    }
}

class ChatViewModelFactory(private val chatDao: ChatDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatScreenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatScreenViewModel(chatDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}