package com.justself.klique.Bookshelf.Contacts.ui

import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // Load contacts from the contacts repository
    fun refreshContacts() {
        viewModelScope.launch {
            delay(2000)
            Log.d("Klique Delay", "delay is over")
            val localContacts = _contactsRepository.getContacts()
            val batchSize = 100
            val mergedContacts = mutableListOf<Contact>()
            // server is supposed to return only contacts that are on Klique
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

