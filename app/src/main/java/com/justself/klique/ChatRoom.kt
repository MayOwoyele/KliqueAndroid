package com.justself.klique

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ChatRoom(
    navController: NavController,
    chatRoomId: Int,
    myId: Int,
    contactName: String,
    viewModel: ChatRoomViewModel = viewModel(
        factory = ChatRoomViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    LaunchedEffect(Unit) {
        viewModel.loadChatRoomDetails(chatRoomId)
        viewModel.setChatRoomId(chatRoomId)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.exitChatRoom(chatRoomId, myId) }
    }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                        val messageId = viewModel.generateMessageId()
                        viewModel.sendBinary(
                            image = imageByteArray,
                            messageType = ChatRoomMessageType.CIMAGE,
                            chatRoomId = chatRoomId,
                            messageId = messageId,
                            myId = myId,
                            myName = contactName,
                            context = context
                        )
                    } catch (e: IOException) {
                        Log.e("ChatRoom", "Error processing image: ${e.message}", e)
                    }
                }
            }
        }
    val readStoragePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true ||
                permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
            ) {
                imagePickerLauncher.launch("image/*")
            } else {
                Toast.makeText(context, "Permission denied to access photos", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    val onSendMessage: (String) -> Unit = { message ->
        if (message.isNotEmpty()) {
            viewModel.sendTextMessage(message, chatRoomId, myId)
        }
    }

    Scaffold(
        topBar = {
            CustomCRTopAppBar(
                navController = navController,
                contactName = contactName,
                chatRoomId = chatRoomId,
                viewModel = viewModel
            )
        },
        content = { innerPadding ->
            ChatRoomContent(
                navController,
                chatRoomId,
                innerPadding,
                viewModel,
                myId
            )
        },
        bottomBar = {
            CrTextBoxAndMedia(
                navController = navController,
                chatRoomId = chatRoomId,
                context = context,
                imagePickerLauncher = imagePickerLauncher,
                permissionLauncher = readStoragePermissionLauncher,
                onSendMessage = onSendMessage
            )
        }
    )
}

@Composable
fun CustomCRTopAppBar(
    navController: NavController,
    contactName: String,
    chatRoomId: Int,
    viewModel: ChatRoomViewModel
) {
    val thisChatRoom by viewModel.thisChatRoom.collectAsState()
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {
            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = thisChatRoom?.optionChatRoomName ?: "Chat Room",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
fun ChatRoomContent(
    navController: NavController,
    chatRoomId: Int,
    innerPadding: PaddingValues,
    viewModel: ChatRoomViewModel,
    myId: Int
) {
    val chatRoomMessages by viewModel.chatRoomMessages.collectAsState()
    val toastWarning by viewModel.toastWarning.collectAsState()
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    var lastSeenMessageCount by remember { mutableIntStateOf(chatRoomMessages.size) }
    val coroutineScope = rememberCoroutineScope()
    val isAtBottom by remember {
        derivedStateOf {
            lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0
        }
    }
    LaunchedEffect(chatRoomId) {
        viewModel.loadChatMessages(chatRoomId)
        lastSeenMessageCount = chatRoomMessages.size
    }
    LaunchedEffect(chatRoomMessages.size) {
        if (isAtBottom) {
            lastSeenMessageCount = chatRoomMessages.size
        }
    }
    LaunchedEffect(toastWarning) {
        if (toastWarning != null) {
            Toast.makeText(context, toastWarning, Toast.LENGTH_LONG).show()
        }
        viewModel.resetToastWarning()
    }
    val newMessagesExist = chatRoomMessages.size > lastSeenMessageCount

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(chatRoomMessages, key = { it.messageId }) { message ->
                ChatRoomMessageItem(
                    message = message,
                    isCurrentUser = message.senderId == myId,
                    navController = navController
                )
            }
        }
        if (newMessagesExist) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                        lastSeenMessageCount = chatRoomMessages.size
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 60.dp, end = 16.dp)
                    .imePadding(),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.ArrowDownward, contentDescription = "Scroll to bottom",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ChatRoomMessageItem(
    message: ChatRoomMessage,
    isCurrentUser: Boolean,
    navController: NavController,
) {
    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val shape = if (isCurrentUser) RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(alignment)
            .padding(8.dp)
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .clickable { navController.navigate("bioScreen/${message.senderId}") }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(alignment)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, shape)
                    .padding(8.dp)
            ) {
                when (message.messageType) {
                    ChatRoomMessageType.CTEXT -> Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ChatRoomMessageType.CIMAGE -> {
                        ShotRoomImageItem(
                            image = message.localPath,
                            shape = shape,
                            navController = navController
                        )
                    }
                }
            }

            if (isCurrentUser) {
                val icon = when (message.status) {
                    ChatRoomStatus.SENDING -> Icons.Default.Schedule
                    ChatRoomStatus.SENT -> Icons.Default.Done
                    ChatRoomStatus.UNSENT -> Icons.Default.ArrowDownward
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Message status",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
fun CrTextBoxAndMedia(
    navController: NavController,
    chatRoomId: Int,
    context: Context,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    permissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    onSendMessage: (String) -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                    }
                } else {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        imagePickerLauncher.launch("image/*")
                    } else {
                        permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                    }
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Add Image")
        }
        TextField(
            value = messageText,
            onValueChange = { messageText = it },
            placeholder = { Text("1KC per message") },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        // Send message button
        IconButton(
            onClick = {
                if (messageText.isNotEmpty()) {
                    onSendMessage(messageText)
                    messageText = "" // Clear the text field
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
        }
    }
}


suspend fun getChatRoomUriFromByteArray(
    byteArray: ByteArray,
    context: Context,
    mediaType: ChatRoomMediaType
): Uri {
    return withContext(Dispatchers.IO) {
        val customCacheDir =
            File(context.cacheDir, KliqueCacheDirString.CUSTOM_CHAT_ROOM_CACHE.directoryName)
        if (!customCacheDir.exists()) {
            customCacheDir.mkdir()
        }

        val file = File(customCacheDir, mediaType.getFileName())
        file.writeBytes(byteArray)

        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}