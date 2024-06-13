package com.justself.klique

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): BigDecimal {
        return BigDecimal(decoder.decodeString())
    }
}

object JsonConfig {
    val json = Json {
        serializersModule = SerializersModule {
            contextual(BigDecimalSerializer)
        }
        ignoreUnknownKeys = true
    }
}


@Serializable
data class Product(
    @SerialName("product_id") val productId: Int,
    val name: String,
    @Contextual val price: BigDecimal,
    @SerialName("videoUrl") val videoUrl: String?,
    val description: String?,  // Product description
    @SerialName("inventory_count") val inventoryCount: Int,
    @Contextual @SerialName("distributor_cut") val distributorCut: BigDecimal,
    @Contextual @SerialName("referral_commission_rate") val referralCommissionRate: BigDecimal?,
    @Contextual val discount: BigDecimal,
    @SerialName("shop_id") val shopId: Int?,
    @SerialName("shop_name") val shopName: String,
    @SerialName("shop_description") val shopDescription: String?,
    @SerialName("profile_photo") val profilePhoto: String?,
    @SerialName("market_id") val marketId: Int?,  // Market ID can be null
    @SerialName("likes_count") val likes: Int?,
    var quantity: Int = 0  // Track quantity for cart functionality
)

@Serializable
data class ProductLike(
    @SerialName("product_id") val productId: Int,
    @SerialName("customer_id") val customerId: Int
)
@Serializable
data class LikeResponse(
    val success: Boolean,
    val likesCount: Int? = null,  // Optional, because it might not exist in case of errors
    val error: String? = null     // Optional error message
)