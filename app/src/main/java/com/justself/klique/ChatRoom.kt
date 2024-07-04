package com.justself.klique

import ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.emoji2.emojipicker.EmojiPickerView
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatRoom(
    topic: String,
    sender: String,
    gistId: String,
    viewModel: SharedCliqueViewModel,
    customerId: Int,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean
) {
    var message by remember { mutableStateOf(TextFieldValue("")) }
    val observedMessages by viewModel.messages.observeAsState(emptyList())
    val context = LocalContext.current
    val permissionGrantedImages = remember { mutableStateOf(false) }
    val permissionGrantedVideos = remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var maxKeyboardHeightDp by remember { mutableStateOf(0.dp) }
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showTrimmingScreen by remember { mutableStateOf<Uri?>(null) }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                    Log.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")

                    val messageId = viewModel.generateMessageId()
                    viewModel.sendBinary(imageByteArray, "KImage", gistId, messageId, customerId, sender)

                    val chatMessage = ChatMessage(
                        id = messageId,
                        gistId = gistId,
                        customerId = customerId,
                        sender = sender,
                        content = "",
                        status = "pending",
                        messageType = "KImage",
                        binaryData = imageByteArray
                    )
                    viewModel.addMessage(chatMessage)
                } catch (e: IOException) {
                    Log.e("ChatRoom", "Error processing image: ${e.message}", e)
                }
            }
        }
    }

    // Video Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val filePath = FileUtils.getPath(context, it)
                filePath?.let { path ->
                    val downscaledUri = VideoUtils.downscaleVideo(context, Uri.fromFile(File(path))) ?: Uri.fromFile(File(path))
                    showTrimmingScreen = downscaledUri
                } ?: run {
                    Log.e("ChatRoom", "Error getting video path")
                }
            }
        }
    }

    // Permission Launcher for Images
    val permissionLauncherImages = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGrantedImages.value = granted
        if (granted) imagePickerLauncher.launch("image/*")
    }

    // Permission Launcher for Videos
    val permissionLauncherVideos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGrantedVideos.value = granted
        if (granted) videoPickerLauncher.launch("video/*")
    }

    // Handle Keyboard Visibility
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboardHeightDp = with(density) { WindowInsets.ime.getBottom(LocalDensity.current).toDp() }

    LaunchedEffect(key1 = keyboardHeightDp) {
        if (keyboardHeightDp > maxKeyboardHeightDp) maxKeyboardHeightDp = keyboardHeightDp
    }

    // Handle Emoji Selection
    LaunchedEffect(selectedEmoji) {
        if (selectedEmoji.isNotEmpty()) {
            message = message.copy(text = message.text + selectedEmoji, selection = TextRange(message.text.length + selectedEmoji.length))
        }
    }

    // Load Messages
    LaunchedEffect(key1 = gistId) {
        if (gistId.isNotEmpty()) {
            try {
                viewModel.loadMessages(gistId)
            } catch (e: Exception) {
                viewModel.close()
            }
        }
    }

    // Observe Emoji Picker Visibility
    LaunchedEffect(showEmojiPicker, maxKeyboardHeightDp) {
        Log.d("ChatRoom", "showEmojiPicker: $showEmojiPicker, maxKeyboardHeightDp: $maxKeyboardHeightDp")
    }

    // Main content
    if (showTrimmingScreen != null) {
        VideoTrimmingScreen(
            appContext = context,
            uri = showTrimmingScreen!!,
            onTrimComplete = { trimmedUri ->
                Log.d("ChatRoom", "OnTrimComplete called with trimmedUri: $trimmedUri")
                showTrimmingScreen = null
                trimmedUri?.let { validUri ->
                    coroutineScope.launch {
                        try {
                            val videoByteArray = context.contentResolver.openInputStream(validUri)?.readBytes() ?: ByteArray(0)
                            Log.d("ChatRoom", "Video Byte Array: ${videoByteArray.size} bytes")

                            val messageId = viewModel.generateMessageId()
                            viewModel.sendBinary(videoByteArray, "KVideo", gistId, messageId, customerId, sender)

                            val chatMessage = ChatMessage(
                                id = messageId,
                                gistId = gistId,
                                customerId = customerId,
                                sender = sender,
                                content = "",
                                status = "pending",
                                messageType = "KVideo",
                                binaryData = videoByteArray
                            )
                            viewModel.addMessage(chatMessage)
                        } catch (e: IOException) {
                            Log.e("ChatRoom", "Error processing video: ${e.message}", e)
                        }
                    }
                }
            },
            onCancel = {
                showTrimmingScreen = null
            }
        )
    } else {
        // Your existing ChatRoom UI goes here
        Column(modifier = Modifier.fillMaxSize().padding(8.dp).imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Gist: $topic",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Add Member") },
                            onClick = { /* Handle option 1 click */ })
                        DropdownMenuItem(
                            text = { Text("Exit") },
                            onClick = { /* Handle option 2 click */ })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().nestedScroll(remember { object : NestedScrollConnection {} }),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(observedMessages) { message ->
                    Log.d("ChatRoom", "Rendering message: ${message.content}")
                    val isCurrentUser = message.sender == sender
                    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
                    val shape = if (isCurrentUser) RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

                    Box(modifier = Modifier.fillMaxWidth().wrapContentWidth(if (isCurrentUser) Alignment.End else Alignment.Start)) {
                        Column(
                            modifier = Modifier.background(Color.Gray, shape).padding(8.dp),
                            horizontalAlignment = alignment
                        ) {
                            if (!isCurrentUser) {
                                Text(
                                    text = message.sender,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            when (message.messageType) {
                                "KImage" -> {
                                    message.binaryData?.let { binaryData ->
                                        val bitmap = decodeBinaryToBitmap(binaryData)
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.height(200.dp).clip(shape)
                                        )
                                    } ?: Text(text = "Image not available", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                "KVideo" -> {
                                    message.binaryData?.let { binaryData ->
                                        val videoUri = getUriFromByteArray(binaryData, context)
                                        AndroidView(
                                            factory = { VideoView(context).apply {
                                                setVideoURI(videoUri)
                                                start()
                                            } },
                                            modifier = Modifier.height(200.dp).clip(shape)
                                        )
                                    } ?: Text(text = "Video not available", color = MaterialTheme.colorScheme.onPrimary)
                                }
                                else -> {
                                    Text(text = message.content, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Icon(
                                imageVector = getStatusIcon(message.status),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).align(if (isCurrentUser) Alignment.End else Alignment.Start),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = if (showEmojiPicker) maxKeyboardHeightDp else 0.dp).imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp, max = 150.dp).border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small).padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (imeVisible || showEmojiPicker) {
                            IconButton(onClick = {
                                onEmojiPickerVisibilityChange(!showEmojiPicker)
                                if (showEmojiPicker) keyboardController?.show() else keyboardController?.hide()
                            }) {
                                Icon(imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.EmojiEmotions, contentDescription = if (showEmojiPicker) "Show Keyboard" else "Select Emoji")
                            }
                        }
                        val textScrollState = rememberScrollState()
                        BasicTextField(
                            value = message,
                            onValueChange = {
                                message = it
                                coroutineScope.launch { textScrollState.scrollTo(textScrollState.maxValue) }
                            },
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onPrimary),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier.fillMaxWidth().verticalScroll(textScrollState).focusRequester(focusRequester).onFocusChanged { focusState ->
                                isFocused.value = focusState.isFocused
                                if (focusState.isFocused && showEmojiPicker) onEmojiPickerVisibilityChange(false)
                            }.pointerInteropFilter {
                                if (it.action == MotionEvent.ACTION_DOWN) {
                                    if (showEmojiPicker) onEmojiPickerVisibilityChange(false)
                                    if (isFocused.value) keyboardController?.show() else focusRequester.requestFocus()
                                }
                                false
                            }
                        )
                    }
                }
                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            permissionLauncherImages.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            permissionLauncherImages.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = "Select Image")
                }
                IconButton(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PermissionChecker.PERMISSION_GRANTED) {
                            videoPickerLauncher.launch("video/*")
                        } else {
                            permissionLauncherVideos.launch(android.Manifest.permission.READ_MEDIA_VIDEO)
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
                            videoPickerLauncher.launch("video/*")
                        } else {
                            permissionLauncherVideos.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }) {
                    Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = "Select Video")
                }
                IconButton(onClick = {
                    if (message.text.isNotEmpty()) {
                        val messageId = viewModel.generateMessageId()
                        val chatMessage = ChatMessage(
                            id = messageId,
                            gistId = gistId,
                            customerId = customerId,
                            sender = sender,
                            content = message.text,
                            status = "pending",
                            messageType = "text"
                        )
                        viewModel.addMessage(chatMessage)
                        val messageJson = """
                            {
                            "type": "message",
                            "gistId": "$gistId",
                            "content": "${message.text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b")}",
                            "id": $messageId,
                            "sender": "$sender"
                            }
                            """.trimIndent()
                        viewModel.send(messageJson)
                        message = TextFieldValue("")
                    }
                }) {
                    if (message.text.isEmpty()) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Record Voice Note")
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun getStatusIcon(status: String): ImageVector {
    return if (status == "sent") Icons.Filled.Done else Icons.Filled.Schedule
}

fun decodeBinaryToBitmap(binaryData: ByteArray): Bitmap {
    return try {
        BitmapFactory.decodeByteArray(binaryData, 0, binaryData.size).also {
            Log.d("ChatRoom", "Bitmap Decoded: ${it.width}x${it.height}")
        }
    } catch (e: Exception) {
        Log.e("ChatRoom", "Error decoding binary data: ${e.message}", e)
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}

fun getUriFromByteArray(byteArray: ByteArray, context: Context): Uri {
    val file = File(context.cacheDir, "video.mp4")
    file.writeBytes(byteArray)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}
