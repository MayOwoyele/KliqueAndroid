// File: WebSocketListener.kt
package com.justself.klique

import org.json.JSONObject

interface WebSocketListener<T> {
    val listenerId: String
    fun onMessageReceived(type: T, jsonObject: JSONObject)
}
enum class SharedCliqueReceivingType(val type: String) {
    GIST_CREATED("gistCreated"),
    PREVIOUS_MESSAGES("previousMessages"),
    EXIT_GIST("exitGist"),
    GIST_MESSAGE_ACK("gistMessageAck"),
    GIST_CREATION_ERROR("gistCreationError"),
    K_TEXT("KText"),
    K_IMAGE("KImage"),
    K_VIDEO("KVideo"),
    K_AUDIO("KAudio"),
    SPECTATOR_UPDATE("spectatorUpdate"),
    OLDER_MESSAGES("olderMessages"),
    GIST_REFRESH_UPDATE("gistRefreshUpdate"),
    ROLE_UPDATE("roleUpdate"),
    MEMBERS_LIST("membersList"),
    MEMBER_LEFT("memberLeft"),
    SUBSCRIBER_ROLE_UPDATE("subscriberRoleUpdate"),
    MEMBER_JOINED("memberJoined"),
    ONLINE_CONTACTS("onlineContacts"),
    CONTACT_ONLINE("contactOnline"),
    CONTACT_OFFLINE("contactOffline"),
}

enum class PrivateChatReceivingType(val type: String) {
    IS_ONLINE("is_online"),
    P_TEXT("PText"),
    ACK("ack"),
    P_IMAGE("PImage"),
    P_AUDIO("PAudio"),
    P_VIDEO("PVideo"),
    P_GIST_INVITE("PGistInvite"),
    P_GIST_CREATION("PGistCreation"),
    P_PROFILE_UPDATE("PProfileUpdate")
}
enum class ChatRoomReceivingType(val type: String){
    CHAT_ROOM_MESSAGES("chatRoomMessages"),
    C_TEXT("CText"),
    C_IMAGE("CImage"),
    CHAT_ROOM_ACK("chatRoomAck"),
    CHATROOM_KC_ERROR("ChatRoomKcError")
}
enum class DmReceivingType(val type: String) {
    D_TEXT("DText"),
    D_IMAGE("DImage"),
    D_GIST_CREATION("DGistCreation"),
    DM_KC_ERROR("DmKcError"),
    PREVIOUS_DM_MESSAGES("previousDmMessages"),
    ADDITIONAL_DM_MESSAGES("additionalDmMessages"),
    DM_DELIVERY("dmDelivery")
}
enum class CliqueRequestReceivingType(val type: String) {
    CLIQUE_REQUESTS("cliqueJoinRequests"),
    CLIQUE_DECLINE_REQUEST("cliqueRequestDecline")
}
enum class ListenerIdEnum(val theId: String){
    SHARED_CLIQUE("SharedCliqueViewModel"),
    PRIVATE_CHAT_SCREEN("ChatScreenViewModel"),
    CHAT_ROOM_VIEW_MODEL("ChatRoomViewModel"),
    DM_ROOM_VIEW_MODEL("DmRoomViewModel"),
    CLIQUE_SCREEN("CliqueScreenViewModel")
}