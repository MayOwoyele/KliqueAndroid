package com.justself.klique

import androidx.compose.ui.graphics.Color

data class Post(
    val id: String,
    val type: String, // "text", "image", "audio", or "video"
    val content: String, // URL or text content
    val thumbnail: String? = null,
    val topComments: List<StatusComments>,
    val totalComments: Int,
    val kcLikesCount: Int
)

data class Profile(
    val customerId: Int,
    val bioImage: String,
    val backgroundColor: Color,
    val fullName: String,
    val bioText: String,
    val posts: List<Post>,
    val classSection: String,
    val isSpectator: Boolean,
    val seatedCount: Int,
    val isVerified: Boolean = false
)
data class StatusComments(
    val name: String,
    val customerId: Int,
    val text: String
)