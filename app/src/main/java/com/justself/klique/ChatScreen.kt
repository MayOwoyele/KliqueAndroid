package com.justself.klique

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChatsScreen(navController: NavHostController, chatScreenViewModel: ChatScreenViewModel, customerId: Int) {
    val chats: List<ChatList> by chatScreenViewModel.chats.observeAsState(emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(chats) { chat ->
                ChatItem(chat, modifier = Modifier.fillMaxWidth(), onClick = {
                    navController.navigate("MessageScreen/${chat.enemyId}")
                })
            }
        }
        val databaseInjection: List<ChatList> = chatScreenViewModel.getMockChats(customerId)
        val coroutineScope = rememberCoroutineScope()
        // Floating Action Button at the bottom right
        AddButton(
            onClick = {  coroutineScope.launch(Dispatchers.IO) {
                databaseInjection.forEach {chat ->
                chatScreenViewModel.addChat(chat)} } },
            icon = Icons.Default.Add,
            contentDescription = "Add Chat",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp)
        )
    }



    // Load chats when the composable is first composed
    chatScreenViewModel.loadChats(customerId)
}
@Composable
private fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun ChatItem(chat: ChatList, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 25.dp)
    val borderColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .padding(8.dp)
            .border(1.dp, borderColor, shape)
            .fillMaxWidth() // Ensure full width
            .height(100.dp)
            .clickable { onClick(/* something something */) }, // Set a fixed height
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(), // Fill the available space
            verticalAlignment = Alignment.CenterVertically // Ensure vertical alignment
        ) {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(chat.profilePhoto),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape), // Make the image circular
                    contentScale = ContentScale.Crop
                )
                if (chat.unreadMsgCounter > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Text(
                            text = chat.unreadMsgCounter.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f), // Ensure the column expands
                verticalArrangement = Arrangement.Center // Center the content vertically
            ) {
                Text(text = chat.contactName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = chat.lastMsg,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = chat.lastMsgAddtime, fontSize = 12.sp)
            }
        }
    }
}
