package com.justself.klique.ContactsBlock.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.justself.klique.ChatScreenViewModel

@Composable
fun BookshelfScreen(navController: NavController, chatScreenViewModel: ChatScreenViewModel, customerId: Int) {
    Box (modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)){
        Column {
            //ContactsScreen(navController, chatScreenViewModel, senderId)
        }
    }

}