package com.justself.klique.ContactsBlock.Contacts.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.justself.klique.ContactsBlock.Contacts.data.Contact
import com.justself.klique.ContactsBlock.Contacts.data.ServerContactResponse
import com.justself.klique.ContactEntity
import com.justself.klique.ContactsDatabase
import com.justself.klique.DatabaseProvider
import com.justself.klique.toContact
import com.justself.klique.toContactEntity
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random


//Our repository class for managing and fetching of data from the content provider
class ContactsRepository(private val contentResolver: ContentResolver, context: Context) {
    private val database: ContactsDatabase = DatabaseProvider.getContactsDatabase(context)
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private fun normalizePhoneNumber(phoneNumber: String, context: Context): String? {
        val region = getUserCountryCode(context)
        return try {
            val number: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, region)
            phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getContactByCustomerId(customerId: Int): ContactEntity? {
        return database.contactDao().getContactByCustomerId(customerId)
    }

    fun getContacts(context: Context): List<Contact> {
        val contactList: MutableList<Contact> = mutableListOf()
        Log.d("Check 2", "Check")

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        cursor?.use {
            val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneNumberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val phoneNumber = it.getString(phoneNumberColumn)
                val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber, context)
                if (normalizedPhoneNumber != null) {
                    contactList.add(Contact(name, normalizedPhoneNumber))
                }
            }
        }

        return contactList
    }
    fun getUserCountryCode(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val networkCountry = telephonyManager.networkCountryIso?.uppercase()
        val simCountry = telephonyManager.simCountryIso?.uppercase()

        val localeCountry = Locale.getDefault().country
        return networkCountry ?: simCountry ?: localeCountry
    }

    fun checkContactsOnServer(localContacts: List<Contact>): List<ServerContactResponse> {
        val jsonArray = JSONArray().apply {
            localContacts.forEach {
                put(JSONObject().apply {
                    put("name", it.name)
                    put("phoneNumber", it.phoneNumber)
                })
            }
        }

        try {
            Log.d("checkContactsOnServer", "Preparing mock response")
            val mockJsonResponse = JSONArray().apply {
                localContacts.forEachIndexed { index, contact ->
                    val isAppUser = Random.nextBoolean()
                    Log.d("checkContactsOnServer", "Contact: ${contact.phoneNumber}, isAppUser: $isAppUser")
                    if (isAppUser) {
                        put(JSONObject().apply {
                            put("phoneNumber", contact.phoneNumber)
                            put("senderId", Random.nextInt(1000, 9999)) // Random customer ID
                            put("thumbnailUrl", "https://picsum.photos/200/200?random=$index") // Mock thumbnail URL
                        })
                    }
                }
            }.toString()
            val jsonResponse = mockJsonResponse

            val serverContacts = mutableListOf<ServerContactResponse>()
            try {
                Log.d("checkContactsOnServer", "Received mock server response")
                val responseArray = JSONArray(jsonResponse)
                for (i in 0 until responseArray.length()) {
                    val jsonObject = responseArray.getJSONObject(i)
                    val phoneNumber = jsonObject.getString("phoneNumber")
                    val customerId = jsonObject.getInt("senderId")
                    val thumbnailUrl = jsonObject.getString("thumbnailUrl")
                    Log.d("checkContactsOnServer", "Processing contact: $phoneNumber, senderId: $customerId")
                    serverContacts.add(ServerContactResponse(phoneNumber, customerId, thumbnailUrl))
                }
            } catch (e: JSONException) {
                Log.e("checkContactsOnServer", "Failed to parse server response", e)
                return emptyList()
            }

            return serverContacts
        } catch (e: IOException) {
            Log.e("checkContactsOnServer", "Network Error", e)
            return emptyList()
        }
    }

    suspend fun mergeContacts(
        localContacts: List<Contact>,
        serverContacts: List<ServerContactResponse>
    ): List<Contact> {
        val serverContactMap = serverContacts.associateBy { it.phoneNumber }
        return localContacts.map { localContact ->
            serverContactMap[localContact.phoneNumber]?.let { serverContact ->
                localContact.copy(
                    isAppUser = true,
                    customerId = serverContact.customerId,
                    thumbnailUrl = serverContact.thumbnailUrl
                )
            } ?: localContact
        }
    }

    suspend fun storeContactsInDatabase(contacts: List<Contact>) {
        database.contactDao().insertAll(contacts.map { it.toContactEntity() })
    }

    suspend fun getSortedContactsFromDatabase(): List<Contact> {
        return database.contactDao().getSortedContacts().map { it.toContact() }
    }
}
