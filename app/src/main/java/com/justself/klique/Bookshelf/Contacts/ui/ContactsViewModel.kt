package com.justself.klique.Bookshelf.Contacts.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(private val _contactsRepository: ContactsRepository) : ViewModel() {
    // Create a stateflow for our contacts ui.
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
    fun refreshContacts() {
        viewModelScope.launch (Dispatchers.IO){
            delay(1000)
            Log.d("Klique Delay", "delay is over")
            val localContacts = _contactsRepository.getContacts()
            val batchSize = 100
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
}

