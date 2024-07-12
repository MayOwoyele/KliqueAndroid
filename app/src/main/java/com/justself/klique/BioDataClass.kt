package com.justself.klique

import androidx.compose.ui.graphics.Color

data class Post(
    val type: String, // "text", "image", "audio", or "video"
    val content: String, // URL or text content
    val topComments: List<String>,
    val totalComments: Int,
    val kcLikesCount: Int
)

data class Profile(
    val bioImage: String,
    val backgroundColor: Color,
    val fullName: String,
    val bioText: String,
    val isContact: Boolean,
    val posts: List<Post>
)