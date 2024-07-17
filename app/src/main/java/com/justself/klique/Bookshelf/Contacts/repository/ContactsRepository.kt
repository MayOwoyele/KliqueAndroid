package com.justself.klique.Bookshelf.Contacts.repository

import android.content.ContentResolver
import android.provider.ContactsContract
import com.justself.klique.Bookshelf.Contacts.data.Contact


//Our repository class for managing and fetching of data from the content provider
class ContactsRepository(private val contentResolver: ContentResolver){
    fun getContacts(): List<Contact>{
        val contactList: MutableList<Contact> = mutableListOf<Contact>()

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
            while(it.moveToNext()){
                val name = it.getString(nameColumn)
                val phoneNumber = it.getString(phoneNumberColumn)
                contactList.add(Contact(name, phoneNumber))
            }
        }

        return contactList
    }
}
