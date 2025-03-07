package com.justself.klique.ContactsBlock.Contacts.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justself.klique.ContactsBlock.Contacts.data.Contact
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(private val _contactsRepository: ContactsRepository) : ViewModel() {
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts

    init {
        loadContactsFromDatabase()
    }
    private fun loadContactsFromDatabase(){
        viewModelScope.launch {
            _contacts.value = _contactsRepository.getSortedContactsFromDatabase()
        }
    }
    fun refreshContacts(context: Context) {
        viewModelScope.launch (Dispatchers.IO){
            if (_contacts.value.isNotEmpty()) {
                delay(1000)
            }
            val phoneContacts = _contactsRepository.getContacts(context)
            val databaseContacts = _contactsRepository.getSortedContactsFromDatabase()

            val phoneContactNumbers = phoneContacts.map { it.phoneNumber }.toSet()
            val deletedContacts = databaseContacts.filter { it.phoneNumber !in phoneContactNumbers }

            val editedContacts = phoneContacts.filter { phoneContact ->
                val dbContact = databaseContacts.find { it.phoneNumber == phoneContact.phoneNumber }
                dbContact != null && dbContact.name != phoneContact.name
            }
            if (deletedContacts.isNotEmpty()) {
                Log.d("Klique Deleted Contacts", "Deleted: ${deletedContacts.map { it.phoneNumber }}")
                _contactsRepository.deleteContactsFromDatabase(deletedContacts)
            }
            if (editedContacts.isNotEmpty()) {
                Log.d("Klique Edited Contacts", "Edited: ${editedContacts.map { it.phoneNumber }}")
                _contactsRepository.updateContactsInDatabase(editedContacts)
            }
            val localContacts = _contactsRepository.getContacts(context)
            val batchSize = 1000
            val mergedContacts = mutableListOf<Contact>()
            localContacts.chunked(batchSize).forEach { batch ->
                val serverContacts = _contactsRepository.checkContactsOnServer(batch)
                val mergedBatch = _contactsRepository.mergeContacts(batch, serverContacts)
                mergedContacts.addAll(mergedBatch)
            }
            _contactsRepository.storeContactsInDatabase(mergedContacts)
            _contacts.value = _contactsRepository.getSortedContactsFromDatabase()
        }
    }
    fun updateContactFromHomeScreen(appContext: Context) {
        loadContactsFromDatabase()
        refreshContacts(appContext)
    }
}