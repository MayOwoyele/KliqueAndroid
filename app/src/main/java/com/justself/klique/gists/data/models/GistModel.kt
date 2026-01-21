package com.justself.klique.gists.data.models

import com.justself.klique.GistType
import com.justself.klique.gists.ui.shared_composables.LastGistComments


data class GistModel(
    val gistId: String = "",
    val topic: String,
    val description: String,
    val image: String?,
    val activeSpectators: Int,
    val gistType: GistType = GistType.Public,
    val lastGistComments: List<LastGistComments>,
    val postImage: String? = null,
    val postVideo: String? = null,
)
data class Post(
    val postId: String,
    val name: String,
    val profileImage: String,
    val isVerified: Boolean,
    val userId: Int,
    val text: String,
    val views: Int,
    val superViews: Int,
    val commentCount: Int,
    val postType: PostType,
    val superViewed: Boolean,
    val mediaLink: String? = null,
    val gistReply: Post? = null,
    val isFollowing: Boolean? = null
)

//data class Challenge(
//    val challengeId: String,
//    val cliqueName: String,
//    val cliqueId: Int,
//    val content: String,
//    val viewsCount: Int,
//    val timestamp: Long,
//    val starter: String,
//    val starterId: Int,
//    val type: ChallengeType
//)
//enum class ChallengeType(val gameString: String) {
//    GuessingGame("Guessing Game"),
//    Unidentified("Unidentified")
//}
enum class PostType {
    Text,
    Image,
    Video,
    Audio,
    Unidentified
}