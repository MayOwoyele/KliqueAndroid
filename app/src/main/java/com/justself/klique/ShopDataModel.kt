package com.justself.klique

import com.justself.klique.Product
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Shop(
    @SerialName("shop_id") val shopId: Int,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_description") val shopDescription: String,
    @SerialName("profile_photo") val profilePhoto: String,
    @SerialName("market_id") val marketId: Int,
    @SerialName("address") val address: String?,  // Address field is nullable
    @SerialName("owner_id") val ownerId: Int,  // Add owner ID
    val products: List<Product>
)