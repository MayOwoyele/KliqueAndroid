package com.justself.klique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.compose.ui.graphics.Color

class BioViewModel : ViewModel() {
    // This is a placeholder function to simulate fetching data from a server
    fun fetchProfile(customerId: Int) = liveData {
        val profile = Profile(
            bioImage = "https://images.unsplash.com/photo-1599566150163-29194dcaad36?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=1080",
            backgroundColor = Color.Gray,
            fullName = "Tatiana Manois",
            bioText = "This life is fucked and I don’t care",
            isContact = true,
            posts = listOf(
                Post(
                    type = "image",
                    content = "https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg",
                    topComments = listOf("Oya no lie, you be stripper", "You are gorgeous darling"),
                    totalComments = 2087,
                    kcLikesCount = 500000
                ),
                Post(
                    type = "text",
                    content = "Had a wonderful day exploring the city!",
                    topComments = listOf("Looks like fun!", "I wish I was there!"),
                    totalComments = 134,
                    kcLikesCount = 12500
                ),
                Post(
                    type = "image",
                    content = "https://images.pexels.com/photos/1130626/pexels-photo-1130626.jpeg",
                    topComments = listOf("Amazing view!", "Stunning photo!"),
                    totalComments = 562,
                    kcLikesCount = 134000
                ),
                Post(
                    type = "image",
                    content = "https://ix-marketing.imgix.net/Break%20Through%20Image.jpg?auto=format,compress&w=3038",
                    topComments = listOf("Great shot!", "Lovely picture!"),
                    totalComments = 321,
                    kcLikesCount = 89000
                ),
                Post(
                    type = "text",
                    content = "Excited about the new project coming up!",
                    topComments = listOf("Can't wait to see it!", "Good luck!"),
                    totalComments = 89,
                    kcLikesCount = 42000
                )
            )
        )
        emit(profile)
    }
}