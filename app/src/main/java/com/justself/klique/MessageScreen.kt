package com.justself.klique

import android.content.Context
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MessageScreen(
    navController: NavController,
    enemyId: Int,
    chatScreenViewModel: ChatScreenViewModel,
    onNavigateToTrimScreen: (String) -> Unit,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    contactName: String
) {
    DisposableEffect(Unit) {
        chatScreenViewModel.enterChat(enemyId)
        onDispose { chatScreenViewModel.leaveChat()}
    }
    val myId by chatScreenViewModel.myUserId.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRecording = remember { mutableStateOf(false)}
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    try {
                        val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                        Log.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")

                        val messageId = chatScreenViewModel.generateMessageId()
                        chatScreenViewModel.sendBinary(
                            imageByteArray, "PImage", enemyId, messageId, myId, contactName
                        )

                        val personalChat = PersonalChat(
                            messageId = messageId,
                            enemyId = enemyId,
                            myId = myId,
                            content = "",
                            status = "pending",
                            messageType = "PImage",
                            timeStamp = System.currentTimeMillis()
                                .toString(),  // Use appropriate time format
                            mediaContent = imageByteArray
                        )
                        chatScreenViewModel.addPersonalChat(personalChat)
                    } catch (e: IOException) {
                        Log.e("ChatRoom", "Error processing image: ${e.message}", e)
                    }
                }
            }
        }
    val videoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                coroutineScope.launch {
                    val filePath = FileUtils.getPath(context, selectedUri)
                    filePath?.let { path ->
                        val fileUri = Uri.fromFile(File(path))
                        val downscaledUri = VideoUtils.downscaleVideo(context, fileUri)
                        Log.d("VideoProcessing", "Downscaled URI: $downscaledUri")
                        if (downscaledUri == null) {
                            Log.e("VideoProcessing", "Downscaling failed, using original file.")
                            onNavigateToTrimScreen(fileUri.toString())  // Use the original file URI if downscaling fails
                        } else {
                            onNavigateToTrimScreen(downscaledUri.toString())  // Use the downscaled URI if successful
                        }

                        val videoByteArray = File(downscaledUri?.path ?: filePath).readBytes()
                        val messageId = chatScreenViewModel.generateMessageId()
                        chatScreenViewModel.sendBinary(
                            videoByteArray, "PVideo", enemyId, messageId, myId, contactName
                        )

                        val personalChat = PersonalChat(
                            messageId = messageId,
                            enemyId = enemyId,
                            myId = myId,
                            content = "",
                            status = "pending",
                            messageType = "PVideo",
                            timeStamp = System.currentTimeMillis()
                                .toString(),  // Use appropriate time format
                            mediaContent = videoByteArray
                        )
                        chatScreenViewModel.addPersonalChat(personalChat)
                    } ?: run {
                        Log.e("ChatRoom", "Error getting video path")
                    }
                }
            }
        }
    val permissionLauncherImages =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) imagePickerLauncher.launch("image/*")
        }

    val permissionLauncherVideos =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) videoPickerLauncher.launch("video/*")
        }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            AudioRecorder.startRecording(context)
            isRecording.value = true
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    val stopRecording: (File?) -> Unit = { file ->
        isRecording.value = false
        file?.let {
            chatScreenViewModel.handleRecordedAudio(it, enemyId, myId, contactName)
        }
    }
    val onSendMessage: (String) -> Unit = { message ->
        if (message.isNotEmpty()) {
            chatScreenViewModel.sendTextMessage(message, enemyId, myId)
        }
    }
    Scaffold(topBar = {
        CustomTopAppBar(navController = navController, contactName = contactName, enemyId, chatScreenViewModel)
    }, content = { innerPadding ->
        MessageScreenContent(navController, enemyId, innerPadding, chatScreenViewModel, myId)
    }, bottomBar = {
        TextBoxAndMedia(
            navController = navController,
            enemyId = enemyId,
            context = context,
            imagePickerLauncher = imagePickerLauncher,
            videoPickerLauncher = videoPickerLauncher,
            permissionLauncherImages = permissionLauncherImages,
            permissionLauncherVideos = permissionLauncherVideos,
            audioPermissionLauncher = audioPermissionLauncher,
            onSendMessage = onSendMessage,
            onStopRecording = stopRecording,
            onEmojiPickerVisibilityChange = onEmojiPickerVisibilityChange,
            selectedEmoji = selectedEmoji,
            showEmojiPicker = showEmojiPicker
            )
    }, modifier = Modifier.imePadding()
    )
}

@Composable
fun CustomTopAppBar(navController: NavController, contactName: String, enemyId: Int, viewModel: ChatScreenViewModel) {
    LaunchedEffect(enemyId) {
        viewModel.startPeriodicOnlineStatusCheck()
    }
    val onlineStatuses by viewModel.onlineStatuses.collectAsState()
    val isOnline = onlineStatuses[enemyId] ?: false
    Surface(
        color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = contactName,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
                    .padding(start = 20.dp)
                    .clickable(enabled = true,
                        onClick = { navController.navigate("bioScreen/$enemyId") })
            )
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(10.dp)
                    .background(
                        color = if (isOnline) Color(0xFFFF69B4) else Color.Gray,
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun MessageScreenContent(
    navController: NavController,
    enemyId: Int,
    innerPadding: PaddingValues,
    chatScreenViewModel: ChatScreenViewModel,
    myId: Int
) {
    val personalChat by chatScreenViewModel.personalChats.collectAsState()
    Log.d("Personal Chats", "The value of personalChats $personalChat")
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    LaunchedEffect(enemyId) {
        chatScreenViewModel.loadPersonalChats(myId, enemyId)
    }
    LazyColumn(
        state = scrollState,
        modifier = Modifier
            .padding(innerPadding)
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(personalChat) { message ->
            val isCurrentUser = message.myId == myId
            val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
            val shape = if (isCurrentUser) RoundedCornerShape(
                16.dp, 0.dp, 16.dp, 16.dp
            ) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(if (isCurrentUser) Alignment.End else Alignment.Start)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Gray, shape)
                        .padding(8.dp),
                    horizontalAlignment = alignment
                ) {
                    /*if (!isCurrentUser) {
                        Text(
                            text = "Sender",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } */
                    when (message.messageType) {
                        "PImage" -> {
                            message.mediaContent?.let { binaryData ->
                                val bitmap = decodeBinaryToBitmap(binaryData)
                                Image(bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(shape)
                                        .clickable {})
                            } ?: Text(
                                text = "Image not available",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        "PVideo" -> {
                            message.mediaContent?.let { binaryData ->
                                val videoUri = getUriFromByteArray(binaryData, context)
                                Box(modifier = Modifier
                                    .height(200.dp)
                                    .wrapContentWidth()
                                    .clip(shape)
                                    .clickable { }) {
                                    AndroidView(
                                        factory = {
                                            VideoView(context).apply {
                                                setVideoURI(videoUri)
                                                seekTo(1)
                                            }
                                        }, modifier = Modifier
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                        contentDescription = "Play",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(48.dp)
                                            .clickable {
                                                navController.navigate(
                                                    "fullScreenVideo/${Uri.encode(videoUri.toString())}"
                                                )
                                            },
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } ?: Text(
                                text = "Video not available",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        "PAudio" -> {
                            message.mediaContent?.let { binaryData ->
                                val audioUri = getUriFromByteArray(binaryData, context)
                                AudioPlayer(
                                    audioUri = audioUri,
                                    modifier = Modifier.widthIn(min = 300.dp, max = 1000.dp)
                                )
                            } ?: Text(
                                text = "Audio not available",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        else -> {
                            Text(
                                text = message.content, color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    if (isCurrentUser) {
                        Icon(
                            imageVector = getPersonChatStatusIcon(status = message.status),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun TextBoxAndMedia(
    navController: NavController,
    enemyId: Int,
    context: Context,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    videoPickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    permissionLauncherImages: ManagedActivityResultLauncher<String, Boolean>,
    permissionLauncherVideos: ManagedActivityResultLauncher<String, Boolean>,
    audioPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onSendMessage: (String) -> Unit,
    onStopRecording: (File) -> Unit,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean
) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var expanded by remember { mutableStateOf(false) }
    val transitionAlpha by animateFloatAsState(
        targetValue = if (textState.text.isEmpty()) 1f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    val transitionRotation by animateFloatAsState(
        targetValue = if (textState.text.isEmpty()) -45f else 0f,
        animationSpec = tween(durationMillis = 300)
    )
    val micRotation by animateFloatAsState(
        targetValue = if (textState.text.isEmpty()) 0f else 45f,
        animationSpec = tween(durationMillis = 300)
    )


    if (textState.text.isNotEmpty()) {
        expanded = false
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(MaterialTheme.colorScheme.background)
            .animateContentSize()
    ) {
        Crossfade(targetState = textState.text.isEmpty()) { isEmpty ->
            if (isEmpty) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach Media",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(onClick = { /* Handle emoji picker */ }) {
                    Icon(
                        imageVector = Icons.Outlined.EmojiEmotions,
                        contentDescription = "Emoji Picker",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expanded && textState.text.isEmpty(),
            enter = expandHorizontally(animationSpec = tween(durationMillis = 300)),
            exit = shrinkHorizontally(animationSpec = tween(durationMillis = 300))
        ) {
            Row {
                IconButton(onClick = { /* Handle image picking */ }) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Pick Image",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { /* Handle video picking */ }) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Pick Video",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        BasicTextField(
            value = textState,
            onValueChange = { textState = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textState.text.isEmpty()) {
                        Text(
                            text = "Type a message",
                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.Gray),
                            textAlign = TextAlign.Start
                        )
                    }
                    innerTextField()
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text
            ),
            cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
        )

        if (textState.text.isEmpty()) {
            IconButton(onClick = { /* Handle voice note recording */ }) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Note",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .rotate(micRotation)
                        .alpha(transitionAlpha)
                )
            }
        } else {
            IconButton(onClick = {
                onSendMessage(textState.text)
                textState = TextFieldValue("")
            }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Message",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .rotate(transitionRotation)
                        .alpha(1 - transitionAlpha)
                )
            }
        }
    }
}

@Composable
fun getPersonChatStatusIcon(status: String): ImageVector {
    return when (status) {
        "sent" -> Icons.Filled.Done // Single checkmark for sent
        "delivered" -> Icons.Filled.DoneAll // Double checkmark for delivered
        // "read" -> Icons.Filled.Visibility Eye icon for read (optional, if you want to differentiate read status)
        else -> Icons.Filled.Schedule // Clock icon for pending or unknown status
    }
}