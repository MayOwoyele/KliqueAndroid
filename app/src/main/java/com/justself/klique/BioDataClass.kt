package com.justself.klique

data class NewGist(
    val id: String,
    val type: String,
    val content: String,
    val thumbnail: String? = null,
    val topComments: List<StatusComments>,
    val totalComments: Int,
    val kcLikesCount: Int
)

data class Profile(
    val customerId: Int,
    val bioImage: String,
    val fullName: String,
    val bioText: String,
    val posts: List<NewGist>,
    val classSection: String,
    val isSpectator: Boolean,
    val seatedCount: Int,
    val isVerified: Boolean = false
)
data class StatusComments(
    val name: String,
    val customerId: Int,
    val text: String,
    val replyingToId: Int? = null,
    val replyingTo: String? = null
)