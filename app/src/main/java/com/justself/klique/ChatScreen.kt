package com.justself.klique

import android.graphics.Paint.Align
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    navController: NavHostController,
    viewModel: ChatScreenViewModel,
    customerId: Int
) {
    val chats by viewModel.chats.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedChats = remember { mutableStateListOf<Int>() }
    val searchBarHeight by animateDpAsState(
        targetValue = if (isSearchVisible) 65.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 500, // Duration in milliseconds
            easing = FastOutSlowInEasing // Easing function for the animation
        )
    )
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.startPeriodicOnlineStatusCheck()
    }
    LaunchedEffect(Unit) {
        viewModel.loadChats(customerId)
        viewModel.setMyUserId(customerId)
        viewModel.fetchNewMessagesFromServer()
        Log.d("loadChats", "Chat has been loaded")
    }
    LaunchedEffect(searchQuery) {
        if (searchQuery.isEmpty()) {
            viewModel.loadChats(customerId, updateSearchResults = true)
        } else {
            viewModel.searchChats(searchQuery)
        }
    }
    fun toggleSelection(enemyId: Int) {
        if (selectedChats.contains(enemyId)) {
            selectedChats.remove(enemyId)
        } else {
            selectedChats.add(enemyId)
        }
        if (selectedChats.isEmpty()) {
            isSelectionMode = false
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            CustomContextMenu(
                selectedChats = selectedChats,
                onDelete = {
                    showDialog = true
                },
                onDismiss = {
                    isSelectionMode = false
                    selectedChats.clear()
                }
            )
            if (searchBarHeight > 0.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(searchBarHeight)
                        .background(MaterialTheme.colorScheme.background)
                ) {
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
                                    "Search for your chats..",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            leadingIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
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
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                items(
                    if (isSearchVisible) searchResults else chats,
                    key = { chat -> chat.enemyId }) { chat ->
                    ChatItem(chat, modifier = Modifier.fillMaxWidth(), onClick = {
                        if (isSelectionMode) {
                            toggleSelection(chat.enemyId)
                        } else {
                            navController.navigate(
                                "messageScreen/${chat.enemyId}/${Uri.encode(chat.contactName)}?isVerified=${if (chat.isVerified) 1 else 0}"
                            )
                        }
                    }, onLongPress = {
                        isSelectionMode = true
                        toggleSelection(chat.enemyId)
                    }, isSelected = selectedChats.contains(chat.enemyId),
                        isSelectionMode = isSelectionMode
                    )
                }
            }
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                selectedChats.forEach { enemyId ->
                                    viewModel.deleteChat(
                                        enemyId,
                                        context
                                    )
                                }
                                isSelectionMode = false
                                selectedChats.clear()
                                showDialog = false
                            }
                        ) {
                            Text("Yes", style = MaterialTheme.typography.bodyLarge)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDialog = false }) {
                            Text("No", style = MaterialTheme.typography.bodyLarge)
                        }
                    },
                    title = {
                        Text(
                            "Confirm Deletion",
                            style = MaterialTheme.typography.displayLarge
                        )
                    },
                    text = {
                        Text(
                            "Are you sure you want to delete the selected chats?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                )
            }
        }
        val databaseInjection: List<ChatList> = viewModel.getMockChats(customerId)
        val coroutineScope = rememberCoroutineScope()
        var menuExpanded by remember { mutableStateOf(false) }
        // Floating Action Button at the bottom right
        Box(
            contentAlignment = Alignment.BottomEnd,
            modifier = Modifier
                .fillMaxSize()
                .padding(25.dp)
        ) {
            Log.d("isSelectionMode", "$isSelectionMode")
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = menuExpanded && !isSelectionMode && !isSearchVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it }, // Start from the bottom
                        animationSpec = tween(durationMillis = 300)
                    ) + scaleIn(
                        initialScale = 0.8f,
                        animationSpec = tween(durationMillis = 300)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it }, // Slide out to the bottom
                        animationSpec = tween(durationMillis = 300)
                    ) + scaleOut(
                        targetScale = 0.8f,
                        animationSpec = tween(durationMillis = 300)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .animateContentSize()
                    ) {
                        TextOption("Contacts", onClick = {navController.navigate("contactsScreen")})
                        TextOption("Update Status", onClick = {navController.navigate("statusSelectionScreen")})
                        TextOption("Personal Shopper", onClick = {navController.navigate("messageScreen/1/Personal Shopper")})
                    }
                }
                AddButton(
                    onClick = {
                        menuExpanded = !menuExpanded
                    },
                    icon = if (menuExpanded && !isSearchVisible && !isSelectionMode) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (menuExpanded) "Close Menu" else "Open Menu",
                    modifier = Modifier
                        .padding(25.dp)
                )
            }
            if (!isSelectionMode) {
                AddButton(
                    onClick = { isSearchVisible = !isSearchVisible; menuExpanded = false },
                    icon = Icons.Default.Search,
                    contentDescription = "Search",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(25.dp)
                )
            }
        }
    }
}

@Composable
fun TextOption(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.displayLarge
    )
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
fun ChatItem(
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = chat.contactName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (chat.isVerified){
                        Icon(
                            imageVector = Icons.Default.CheckCircle, // You can use a different icon if needed
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }
                }
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
                    onCheckedChange = { onLongPress() }
                )
            }
        }
    }
}

@Composable
fun CustomContextMenu(
    selectedChats: List<Int>,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val contextMenuHeight by animateDpAsState(
        targetValue = if (selectedChats.isNotEmpty()) 56.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    )
    if (selectedChats.isNotEmpty() || contextMenuHeight > 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(contextMenuHeight)
                .background(MaterialTheme.colorScheme.onSecondary),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedChats.size} selected",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
            Row {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}