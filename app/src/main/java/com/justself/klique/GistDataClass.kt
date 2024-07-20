package com.justself.klique

data class GistMessage(
    val id: String,
    val gistId: String,
    val customerId: Int,
    val sender: String,
    val content: String,
    val status: String,
    val messageType: String = "text",
    val binaryData: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GistMessage) return false

        return id == other.id &&
                gistId == other.gistId &&
                customerId == other.customerId &&
                sender == other.sender &&
                content == other.content &&
                status == other.status &&
                messageType == other.messageType &&
                binaryData?.contentEquals(other.binaryData) ?: (other.binaryData == null)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + gistId.hashCode()
        result = 31 * result + customerId
        result = 31 * result + sender.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + messageType.hashCode()
        result = 31 * result + (binaryData?.contentHashCode() ?: 0)
        return result
    }
}
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
    val gistImage: String
)