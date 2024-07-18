package com.justself.klique.Bookshelf.Contacts.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.Bookshelf.Contacts.data.ServerContactResponse
import com.justself.klique.ContactsDatabase
import com.justself.klique.DatabaseProvider
import com.justself.klique.NetworkUtils.makeRequest
import com.justself.klique.toContact
import com.justself.klique.toContactEntity
import okio.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.random.Random


//Our repository class for managing and fetching of data from the content provider
class ContactsRepository(private val contentResolver: ContentResolver, context: Context) {
    private val database: ContactsDatabase = DatabaseProvider.getContactsDatabase(context)
    private val phoneUtil = PhoneNumberUtil.getInstance()
    private fun normalizePhoneNumber(phoneNumber: String, region: String = "NG"): String? {
        return try {
            val number: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, region)
            phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            null
        }
    }

    fun getContacts(): List<Contact> {
        val contactList: MutableList<Contact> = mutableListOf()
        Log.d("Check 2", "Check")


        // Perform the query on the Contacts content provider
        // Cursor provides read-only access to the results
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, // URI of the content provider
            null, // Projection (null means all columns)
            null, // Selection (null means no selection criteria)
            null, // Selection arguments (null means no arguments)
            null  // Sort order (null means default sort order)
        )

        // Using cursor to iterate over the contacts results
        cursor?.use {
            // gets the index of the columns that we are interested in (name and Phone number for now)
            val nameColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneNumberColumn = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            // Iterate over the rows for each contact in the cursor
            while (it.moveToNext()) {
                val name = it.getString(nameColumn)
                val phoneNumber = it.getString(phoneNumberColumn)
                val normalizedPhoneNumber = normalizePhoneNumber(phoneNumber)
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
                            put("customerId", Random.nextInt(1000, 9999)) // Random customer ID
                            put("thumbnailUrl", "https://picsum.photos/200/200?random=$index") // Mock thumbnail URL
                        })
                    }
                }
            }.toString()

            // Simulate a network request using mock response instead of actual network call
            // val jsonResponse = makeRequest(
            //     endpoint = "/checkContacts",
            //     method = "POST",
            //     jsonBody = jsonArray.toString(),
            //     params = emptyMap()
            // )
            val jsonResponse = mockJsonResponse // Use mock response directly

            val serverContacts = mutableListOf<ServerContactResponse>()
            try {
                Log.d("checkContactsOnServer", "Received mock server response")
                val responseArray = JSONArray(jsonResponse)
                for (i in 0 until responseArray.length()) {
                    val jsonObject = responseArray.getJSONObject(i)
                    val phoneNumber = jsonObject.getString("phoneNumber")
                    val customerId = jsonObject.getInt("customerId")
                    val thumbnailUrl = jsonObject.getString("thumbnailUrl")
                    Log.d("checkContactsOnServer", "Processing contact: $phoneNumber, customerId: $customerId")
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
