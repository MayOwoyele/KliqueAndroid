package com.justself.klique

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter

@Composable
fun ForwardChatsScreen(
    navController: NavController,
    viewModel: ChatScreenViewModel,
    customerId: Int
) {
    val chats by viewModel.searchResults.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val selectedChats = remember { mutableStateListOf<Int>() }
    val context = LocalContext.current
    val messageToForward by viewModel.messagesToForward.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChats(customerId)
        viewModel.setMyUserId(customerId)
    }
    LaunchedEffect(searchQuery) {
        viewModel.searchChats(searchQuery)
    }
    LaunchedEffect(messageToForward) {
        Log.d("Message", "Message to forward is $messageToForward")
    }

    fun toggleSelection(enemyId: Int) {
        if (selectedChats.contains(enemyId)) {
            selectedChats.remove(enemyId)
        } else {
            selectedChats.add(enemyId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f),
                    placeholder = {
                        Text(
                            "Filter your chats...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = {
                        IconButton(onClick = {navController.popBackStack()}) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Search Icon"
                            )
                        }
                    },
                    shape = RoundedCornerShape(bottomStart = 40.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chats, key = {chat -> chat.enemyId}) { chat ->
                    TheChatItem(chat, modifier = Modifier.fillMaxWidth(), onClick = {
                        toggleSelection(chat.enemyId)
                    }, onLongPress = {
                        toggleSelection(chat.enemyId)
                    }, isSelected = selectedChats.contains(chat.enemyId),
                        isSelectionMode = true
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = {
                viewModel.forwardMessagesToRecipients(selectedChats, myId = customerId, context = context)
                navController.popBackStack()
                      viewModel.clearMessagesToForward()// Navigate back after forwarding
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(25.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Forward",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun TheChatItem(
    chat: ChatList,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    isSelected: Boolean,
    isSelectionMode: Boolean
) {
    val shape =
        RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 0.dp, bottomStart = 25.dp)
    val borderColor = MaterialTheme.colorScheme.primary
    val backgroundColor =
        if (isSelected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.background

    Card(
        modifier = modifier
            .padding(8.dp)
            .border(1.dp, borderColor, shape)
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() },
                    onTap = {
                        onClick()
                    }
                )
            },
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp), // Fill the available space
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
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }
        }
    }
}