package com.justself.klique.ContactsBlock.Contacts.data

data class Contact(
    val name: String,
    val phoneNumber: String,
    val isAppUser: Boolean = false,
    val customerId: Int? = null,
    val thumbnailUrl: String? = null
)

data class ServerContactResponse(
    val phoneNumber: String,
    val customerId: Int,
    val thumbnailUrl: String
)
