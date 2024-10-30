package com.justself.klique

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter

@Composable
fun DmList(navController: NavController, dmListViewModel: DmListViewModel = viewModel()) {

    val dmList by dmListViewModel.dmList.collectAsState()

    // Replace with your actual UI layout
    // For example, a LazyColumn for displaying the list of DMs
    LazyColumn {
        items(dmList) { dmItem ->
            DmListItem(dmItem, navController)
        }
    }
}

@Composable
fun DmListItem(dmItem: DmItem, navController: NavController) {
    // Layout for a single DM item
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { navController.navigate("dmChatScreen/${dmItem.enemyId}/${dmItem.fullName}") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(dmItem.imageLink),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = dmItem.fullName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = when (dmItem.lastMessage) {
                    is LastMessage.Text -> dmItem.lastMessage.content
                    is LastMessage.Photo -> "Photo"
                },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}