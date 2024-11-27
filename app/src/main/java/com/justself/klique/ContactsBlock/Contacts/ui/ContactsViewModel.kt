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
    fun refreshContacts(context: Context) {
        viewModelScope.launch (Dispatchers.IO){
            delay(1000)
            Log.d("Klique Delay", "delay is over")
            val localContacts = _contactsRepository.getContacts(context)
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

