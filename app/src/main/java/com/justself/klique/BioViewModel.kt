package com.justself.klique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import java.util.Locale

class BioViewModel(private val contactsRepository: ContactsRepository) : ViewModel() {
    // This is a placeholder function to simulate fetching data from a server
    fun fetchProfile(enemyId: Int) = liveData {
        val profile = Profile(
            customerId = 20,
            bioImage = "https://images.unsplash.com/photo-1599566150163-29194dcaad36?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
            backgroundColor = Color.White,
            fullName = "Tatiana Manois",
            bioText = "This life is fucked and I don’t care",
            posts = listOf(
                Post(
                    id = "25",
                    type = "image",
                    content = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
                    topComments = listOf(StatusComments(name = "Alice", customerId = 1, text = "Oya no lie, you be stripper"),
                        StatusComments(name = "Bob", customerId = 2, text = "You are gorgeous darling")),
                    totalComments = 2087,
                    kcLikesCount = 500000
                ),
                Post(
                    id = "26",
                    type = "text",
                    content = "Had a wonderful day exploring the city!",
                    topComments = listOf(StatusComments(name = "Charlie", customerId = 3, text = "Looks like fun!"),
                        StatusComments(name = "Dave", customerId = 4, text = "I wish I was there!")),
                    totalComments = 134,
                    kcLikesCount = 12500
                ),
                Post(
                    "27",
                    type = "image",
                    content = "https://images.pexels.com/photos/1130626/pexels-photo-1130626.jpeg",
                    topComments = listOf(StatusComments(name = "Eve", customerId = 5, text = "Amazing view!"),
                        StatusComments(name = "Frank", customerId = 6, text = "Stunning photo!")),
                    totalComments = 562,
                    kcLikesCount = 134000
                ),
                Post(
                    "28",
                    type = "image",
                    content = "https://ix-marketing.imgix.net/Break%20Through%20Image.jpg?auto=format,compress&w=3038",
                    topComments = listOf(StatusComments(name = "Grace", customerId = 7, text = "Great shot!"),
                        StatusComments(name = "Heidi", customerId = 8, text = "Lovely picture! We should also meet up on sunday so that we can have sex, shouldn't we? Or what else do you think?")),
                    totalComments = 321,
                    kcLikesCount = 89000
                ),
                Post("29",
                    type = "text",
                    content = "Excited about the new project coming up!",
                    topComments = listOf(StatusComments(name = "Ivan", customerId = 9, text = "Can't wait to see it!"),
                        StatusComments(name = "Judy", customerId = 10, text = "Good luck!")),
                    totalComments = 89,
                    kcLikesCount = 42000
                ),
                Post("30",
                    type = "video",
                    content = "https://everythinglucii.com/SVideos/24-heures-8b214f67.mp4",
                    thumbnail = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
                    topComments = listOf(
                        StatusComments(name = "Alice", customerId = 1, text = "Great video!"),
                        StatusComments(name = "Bob", customerId = 2, text = "Amazing visuals!")
                    ),
                    totalComments = 2087,
                    kcLikesCount = 500000
                ),
                Post(
                    id = "31", // Unique identifier for the post
                    type = "audio", // Indicates the post type is audio
                    content = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", // URL of the audio file
                    topComments = listOf(
                        StatusComments(name = "Sam", customerId = 11, text = "This audio is fantastic!"),
                        StatusComments(name = "Alex", customerId = 12, text = "I love the melody!")
                    ),
                    totalComments = 47,
                    kcLikesCount = 7500
                )
            ),
            classSection = "Class 1",
            isSpectator = true,
            seatedCount = 950000
        )
        emit(profile)
    }
    fun leaveSeat(){

    }
    fun takeSeat(){
    }
    fun checkIfContact(enemyId: Int): LiveData<String?> = liveData {
        val contact = contactsRepository.getContactByCustomerId(enemyId)
        emit(contact?.name)
    }
    fun formatSpectatorCount(count: Int): String {
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format(Locale.US, "%dK", count / 1_000)
            else -> count.toString()
        }
    }
    fun sendComment(comment: String){

    }
    fun fetchPostComments(postId: String) = liveData {
        // List of predefined comments for demonstration
        val postComments = listOf(
            StatusComments(name = "Alice", customerId = 1, text = "Oya no lie, you be stripper"),
            StatusComments(name = "Bob", customerId = 2, text = "You are gorgeous darling"),
            StatusComments(name = "Carol", customerId = 3, text = "This picture is stunning! The colors are so vibrant."),
            StatusComments(name = "Dave", customerId = 4, text = "Where was this taken? It looks amazing!"),
            StatusComments(name = "Eve", customerId = 5, text = "You have such a great eye for photography."),
            StatusComments(name = "Frank", customerId = 6, text = "The composition of this shot is perfect. Great job!"),
            StatusComments(name = "Grace", customerId = 7, text = "I'm in love with this photo! So beautiful."),
            StatusComments(name = "Heidi", customerId = 8, text = "The lighting in this picture is just perfect."),
            StatusComments(name = "Ivan", customerId = 9, text = "This is art! Truly mesmerizing."),
            StatusComments(name = "Judy", customerId = 10, text = "You've captured such a beautiful moment. Love it!")
        )

        // Emit the list of comments
        emit(postComments)
    }

}
class BioViewModelFactory(
    private val contactsRepository: ContactsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BioViewModel(contactsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}