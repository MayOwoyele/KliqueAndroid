package com.justself.klique.Bookshelf.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.justself.klique.Bookshelf.Contacts.ui.ContactsScreen
import com.justself.klique.sharedUi.AddButton

@Composable
fun BookshelfScreen() {
    Box (modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)){
        Column {
            ContactsScreen()
        }
    }

}