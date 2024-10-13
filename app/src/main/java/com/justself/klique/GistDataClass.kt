package com.justself.klique

import android.net.Uri
import java.time.ZonedDateTime
import java.util.UUID

data class GistMessage(
    val id: String,
    val gistId: String,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val status: GistMessageStatus,
    val messageType: String = "KText",
    val localPath: Uri? = null,
    val externalUrl: String? = null,
    val timeStamp: ZonedDateTime
)
data class UserStatus(
    val isSpeaker: Boolean,
    val isOwner: Boolean
)

data class Members(
    val customerId: Int,
    val fullName: String,
    val isContact: Boolean = false,
    val isOwner: Boolean = false,
    val isSpeaker: Boolean = false
)

data class GistTopRow(
    val gistId: String,
    val topic: String,
    val gistDescription: String,
    val activeSpectators: String,
    val gistImage: String,
    val startedBy: String,
    val startedById: Int
)

data class GistState(
    val topic: String,
    val gistId: String
)

enum class GistMediaType {
    KImage,
    KAudio,
    KVideo;
    fun getFileName(): String {
        val fileName = when (this) {
            KImage -> "KImage_${UUID.randomUUID()}.jpg"
            KAudio -> "KAudio_${UUID.randomUUID()}.mp3"
            KVideo -> "KVideo_${UUID.randomUUID()}.mp4"
        }
        return fileName
    }
    fun getTypeString(): String {
        return when (this) {
            KImage -> "KImage"
            KAudio -> "KAudio"
            KVideo -> "KVideo"
        }
    }
    fun getGeneralMediaType(): String {
        return when (this) {
            KImage -> "image"
            KAudio -> "audio"
            KVideo -> "video"
        }
    }
    companion object {
        fun fromString(type: String): GistMediaType? {
            return when (type) {
                "KImage" -> KImage
                "KAudio" -> KAudio
                "KVideo" -> KVideo
                else -> null
            }
        }
    }
}
enum class GistMessageStatus {
    Sent,
    Pending
}