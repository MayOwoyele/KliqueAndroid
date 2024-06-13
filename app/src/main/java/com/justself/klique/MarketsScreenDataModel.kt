package com.justself.klique

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Market(
    @SerialName("market_id") val marketId: Int,
    @SerialName("market_name") val marketName: String,
    @SerialName("location") val location: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("profile_photo") val profilePhoto: String
)
