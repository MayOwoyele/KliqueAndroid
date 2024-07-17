package com.justself.klique.Bookshelf.Contacts.ui

import android.content.ContentResolver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactsViewModel(private val _contactsRepository: ContactsRepository): ViewModel() {
    // Create a stateflow for our contacts ui.
    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
            val contacts : StateFlow<List<Contact>> = _contacts

    init {
        loadContacts()
    }

    // Load contacts from the contacts repository
    fun loadContacts(){
        viewModelScope.launch {
            _contacts.value = _contactsRepository.getContacts()
        }
    }

}

