package com.justself.klique

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    @SerialName("comment_id") val commentId: Int,
    @SerialName("product_id") val productId: Int,
    @SerialName("customer_id") val customerId: Int,
    val comment: String,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String
)
@Serializable
data class NewComment(
    val productId: Int,
    val customerId: Int,
    val comment: String
)
