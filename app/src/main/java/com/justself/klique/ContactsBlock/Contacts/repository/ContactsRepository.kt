package com.justself.klique.ContactsBlock.Contacts.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.justself.klique.AppUpdateManager
import com.justself.klique.ContactsBlock.Contacts.data.Contact
import com.justself.klique.ContactsBlock.Contacts.data.ServerContactResponse
import com.justself.klique.ContactEntity
import com.justself.klique.ContactsDatabase
import com.justself.klique.DatabaseProvider
import com.justself.klique.KliqueHttpMethod
import com.justself.klique.NetworkUtils
import com.justself.klique.SessionManager
import com.justself.klique.toContact
import com.justself.klique.toContactEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        val region = SessionManager.getUserCountryCode()
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

    suspend fun checkContactsOnServer(localContacts: List<Contact>): List<ServerContactResponse> {
        val jsonArray = JSONArray().apply {
            localContacts.forEach {
                put(it.phoneNumber)
            }
        }
        val jsonObject = JSONObject().apply {
            put("userId", SessionManager.customerId.value)
            put("numbers", jsonArray)
        }.toString()
        Log.d("number", jsonObject)
        val users = mutableListOf<ServerContactResponse>()
        try {
            val response = NetworkUtils.makeRequest(
                "fetchContacts",
                KliqueHttpMethod.POST,
                emptyMap(),
                jsonBody = jsonObject
            )
            if (response.first) {
                val responseJsonArray = JSONArray(response.second)
                for (i in 0 until responseJsonArray.length()) {
                    val eachJsonObject = responseJsonArray.getJSONObject(i)
                    val phoneNumber = eachJsonObject.getString("phoneNumber")
                    val customerId = eachJsonObject.getInt("userId")
                    val thumbnailUrl =
                        NetworkUtils.fixLocalHostUrl(eachJsonObject.getString("profilePictureUrl"))
                    val theData = ServerContactResponse(phoneNumber, customerId, thumbnailUrl)
                    users.add(theData)
                }
            }
            Log.d("number", users.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return users;
    }

    fun mergeContacts(
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
    suspend fun deleteContactsFromDatabase(contacts: List<Contact>) {
        val toDelete = contacts.map { it.toContactEntity() }
        database.contactDao().deleteContacts(toDelete)
    }
    suspend fun updateContactsInDatabase(contacts: List<Contact>) {
        val toUpdate = contacts.map { it.toContactEntity() }
        database.contactDao().updateContacts(toUpdate)
    }
}
