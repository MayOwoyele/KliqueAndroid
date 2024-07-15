package com.justself.klique

import ImageUtils
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.MutableState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import android.Manifest
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GistRoom(
    topic: String,
    myName: String,
    gistId: String,
    viewModel: SharedCliqueViewModel,
    customerId: Int,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    onNavigateToTrimScreen: (String) -> Unit,
    navController: NavController,
    resetSelectedEmoji: () -> Unit
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
    Log.d("MyName", "Slim Shady's name is: $myName")
    viewModel.setMyName(myName)


    // Image Picker Launcher
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    try {
                        val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                        Log.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")

                        val messageId = viewModel.generateMessageId()
                        viewModel.sendBinary(
                            imageByteArray, "KImage", gistId, messageId, customerId, myName
                        )

                        val gistMessage = GistMessage(
                            id = messageId,
                            gistId = gistId,
                            customerId = customerId,
                            sender = myName,
                            content = "",
                            status = "pending",
                            messageType = "KImage",
                            binaryData = imageByteArray
                        )
                        viewModel.addMessage(gistMessage)
                    } catch (e: IOException) {
                        Log.e("ChatRoom", "Error processing image: ${e.message}", e)
                    }
                }
            }
        }

    // Video Picker Launcher
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
                    } ?: run {
                        Log.e("ChatRoom", "Error getting video path")
                    }
                }
            }
        }

    // Permission Launcher for Images
    val permissionLauncherImages =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            permissionGrantedImages.value = granted
            if (granted) imagePickerLauncher.launch("image/*")
        }

    // Permission Launcher for Videos
    val permissionLauncherVideos =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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
            message = message.copy(
                text = message.text + selectedEmoji,
                selection = TextRange(message.text.length + selectedEmoji.length)
            )
        }
        resetSelectedEmoji()
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
    // Scroll State to handle Gist Title visibility
    val scrollState = rememberLazyListState()
    val showTitle = remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    // Scrolling down
                    showTitle.value = true
                } else if (available.y > 0) {
                    // Scrolling up
                    showTitle.value = false
                }
                return Offset.Zero
            }
        }
    }
    var isRecording = remember { mutableStateOf(false) }

    // Permission launcher for recording audio
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


    val stopRecording = { file: File? ->
        isRecording.value = false
        file?.let {
            // Handle the recorded file, e.g., send it or save it
            coroutineScope.launch {
                // Upload file to a server and handle locally
                try {
                    val audioByteArray = FileUtils.fileToByteArray(it)
                    Log.d("ChatRoom", "Audio Byte Array: ${audioByteArray.size} bytes")

                    val messageId = viewModel.generateMessageId()
                    viewModel.sendBinary(
                        audioByteArray, "KAudio", gistId, messageId, customerId, myName
                    )

                    val gistMessage = GistMessage(
                        id = messageId,
                        gistId = gistId,
                        customerId = customerId,
                        sender = myName,
                        content = "",
                        status = "pending",
                        messageType = "KAudio",
                        binaryData = audioByteArray
                    )
                    viewModel.addMessage(gistMessage)
                } catch (e: IOException) {
                    Log.e("ChatRoom", "Error processing audio: ${e.message}", e)
                }
            }
        }
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    val userStatus by viewModel.userStatus.observeAsState(initial = UserStatus(false, false))
    // Main content
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .nestedScroll(nestedScrollConnection)
        ) {
            if (showTitle.value) {
                GistTitleRow(
                    topic = topic,
                    expanded = expanded,
                    onExpandChange = { expanded = it },
                    viewModel = viewModel,
                    userStatus = userStatus,
                    navController = navController,
                    gistId = gistId
                )
            }

            Spacer(modifier = Modifier.height(0.dp))

            MessageContent(
                observedMessages = observedMessages,
                customerId = customerId,
                context = context,
                scrollState = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                navController = navController
            )
            Spacer(modifier = Modifier.height(5.dp))

            InputRow(message = message,
                onMessageChange = { message = it },
                onSendMessage = {
                    if (message.text.isNotEmpty()) {
                        val messageId = viewModel.generateMessageId()
                        val gistMessage = GistMessage(
                            id = messageId,
                            gistId = gistId,
                            customerId = customerId,
                            sender = myName,
                            content = message.text,
                            status = "pending",
                            messageType = "text"
                        )
                        viewModel.addMessage(gistMessage)
                        val messageJson = """
                            {
                            "type": "message",
                            "gistId": "$gistId",
                            "content": "${
                            message.text.replace("\\", "\\\\").replace("\"", "\\\"")
                                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
                                .replace("\b", "\\b")
                        }",
                            "id": $messageId,
                            "sender": "$myName"
                            }
                            """.trimIndent()
                        viewModel.send(messageJson)
                        message = TextFieldValue("")
                    }
                },
                imeVisible = imeVisible,
                showEmojiPicker = showEmojiPicker,
                onEmojiPickerVisibilityChange = onEmojiPickerVisibilityChange,
                keyboardController = keyboardController,
                focusRequester = focusRequester,
                isFocused = isFocused,
                maxKeyboardHeightDp = maxKeyboardHeightDp,
                imagePickerLauncher = imagePickerLauncher,
                videoPickerLauncher = videoPickerLauncher,
                permissionLauncherImages = permissionLauncherImages,
                permissionLauncherVideos = permissionLauncherVideos,
                coroutineScope = coroutineScope,
                context = context,
                isRecording = isRecording,
                onStopRecording = stopRecording,
                audioPermissionLauncher = audioPermissionLauncher,
                onShowBottomSheet = { showBottomSheet = true },
                isSpeaker = userStatus.isSpeaker
            )
        }

        // Bottom Sheet
        CustomBottomSheet(
            visible = showBottomSheet,
            onDismissRequest = { showBottomSheet = false },
        ) {
            CommentSection(viewModel, navController)
        }
    }
}

@Composable
fun GistTitleRow(
    topic: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    viewModel: SharedCliqueViewModel,
    userStatus: UserStatus,
    navController: NavController,
    gistId: String
) {
    val isOwner = userStatus.isOwner
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val activeUserCount = viewModel.formattedUserCount.collectAsState().value
        Text(
            text = "Gist: $topic", style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp, fontWeight = FontWeight.Bold
            ), color = MaterialTheme.colorScheme.onPrimary
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$activeUserCount spectators",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.then(if (isOwner) {
                        Modifier.clickable { navController.navigate("gistSettings/$gistId") }
                    } else {
                        Modifier
                    }))
                Box {
                    IconButton(onClick = { onExpandChange(true) }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(expanded = expanded,
                        onDismissRequest = { onExpandChange(false) }) {
                        if (isOwner) {
                            DropdownMenuItem(text = { Text("Gist Settings") },
                                onClick = { navController.navigate("gistSettings/$gistId") })
                        }
                        DropdownMenuItem(text = { Text("Share") },
                            onClick = { /* Handle option 1 click */ })
                        DropdownMenuItem(text = { Text("Exit") },
                            onClick = { /* Handle option 2 click */ })
                    }
                }
            }
        }
    }
}

@Composable
fun MessageContent(
    observedMessages: List<GistMessage>,
    customerId: Int,
    context: Context,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    LazyColumn(
        state = scrollState,
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(observedMessages) { message ->
            Log.d("ChatRoom", "Rendering message: ${message.content}")
            val isCurrentUser = message.customerId == customerId
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
                    if (!isCurrentUser) {
                        Text(text = message.sender,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .padding(bottom = 4.dp)
                                .clickable { navController.navigate("bioScreen/${message.customerId}") })
                    }
                    when (message.messageType) {
                        "KImage" -> {
                            message.binaryData?.let { binaryData ->
                                val bitmap = decodeBinaryToBitmap(binaryData)
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(shape)
                                )
                            } ?: Text(
                                text = "Image not available",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        "KVideo" -> {
                            message.binaryData?.let { binaryData ->
                                val videoUri = getUriFromByteArray(binaryData, context)
                                AndroidView(
                                    factory = {
                                        VideoView(context).apply {
                                            setVideoURI(videoUri)
                                            start()
                                        }
                                    }, modifier = Modifier
                                        .height(200.dp)
                                        .clip(shape)
                                )
                            } ?: Text(
                                text = "Video not available",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        "KAudio" -> {
                            message.binaryData?.let { binaryData ->
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
                                text = message.content, color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                    if (isCurrentUser) {
                        Icon(
                            imageVector = getStatusIcon(message.status),
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputRow(
    message: TextFieldValue,
    onMessageChange: (TextFieldValue) -> Unit,
    onSendMessage: () -> Unit,
    imeVisible: Boolean,
    showEmojiPicker: Boolean,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    keyboardController: SoftwareKeyboardController?,
    focusRequester: FocusRequester,
    isFocused: MutableState<Boolean>,
    maxKeyboardHeightDp: Dp,
    imagePickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    videoPickerLauncher: ManagedActivityResultLauncher<String, Uri?>,
    permissionLauncherImages: ManagedActivityResultLauncher<String, Boolean>,
    permissionLauncherVideos: ManagedActivityResultLauncher<String, Boolean>,
    coroutineScope: CoroutineScope,
    context: Context,
    isRecording: MutableState<Boolean>,
    onStopRecording: (File?) -> Job?,
    audioPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onShowBottomSheet: () -> Unit,
    isSpeaker: Boolean
) {
    val expandedState = remember { mutableStateOf(false) }
    val showClipIcon = remember(message) { mutableStateOf(message.text.isEmpty()) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    val maxRecordingDuration = 2 * 60 * 1000 // 2 minutes in milliseconds

    LaunchedEffect(message.text) {
        showClipIcon.value = message.text.isEmpty()
    }

    LaunchedEffect(isRecording.value) {
        if (isRecording.value) {
            recordingDuration = 0
            while (isRecording.value && recordingDuration < maxRecordingDuration) {
                delay(1000)
                recordingDuration += 1000
            }
            if (recordingDuration >= maxRecordingDuration) {
                val recordedFile = AudioRecorder.stopRecording()
                onStopRecording(recordedFile)
                isRecording.value = false
            }
        }
    }
    val transitionState = remember { mutableStateOf(expandedState.value) }

    LaunchedEffect(expandedState.value) {
        if (!expandedState.value) {
            // Delay to allow the collapse animation to complete
            delay(100)
            transitionState.value = expandedState.value
        } else {
            transitionState.value = expandedState.value
        }
    }

    val offset by animateDpAsState(if (isRecording.value) (-0.5).dp else 0.dp)
    val boxWidth by animateDpAsState(targetValue = if (expandedState.value) 100.dp else 5.dp)
    val textNotEmpty = message.text.isNotEmpty()
    val commentBoxWidth by animateDpAsState(targetValue = if (textNotEmpty) 100.dp else 0.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        if (isSpeaker) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (showEmojiPicker) maxKeyboardHeightDp else 0.dp)
                    .imePadding()
                    .offset(y = offset), verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp, max = 150.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (imeVisible || showEmojiPicker) {
                            IconButton(onClick = {
                                onEmojiPickerVisibilityChange(!showEmojiPicker)
                                if (showEmojiPicker) keyboardController?.show() else keyboardController?.hide()
                            }) {
                                Icon(
                                    imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                    contentDescription = if (showEmojiPicker) "Show Keyboard" else "Select Emoji"
                                )
                            }
                        }
                        val textScrollState = rememberScrollState()
                        BasicTextField(value = message,
                            onValueChange = {
                                onMessageChange(it)
                                coroutineScope.launch { textScrollState.scrollTo(textScrollState.maxValue) }
                                if (it.text.isNotEmpty()) {
                                    expandedState.value = false
                                }
                            },
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onPrimary),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(textScrollState)
                                .focusRequester(focusRequester)
                                .onFocusChanged { focusState ->
                                    isFocused.value = focusState.isFocused
                                    if (focusState.isFocused && showEmojiPicker) onEmojiPickerVisibilityChange(
                                        false
                                    )
                                }
                                .pointerInteropFilter {
                                    if (it.action == MotionEvent.ACTION_DOWN) {
                                        if (showEmojiPicker) onEmojiPickerVisibilityChange(false)
                                        if (isFocused.value) keyboardController?.show() else focusRequester.requestFocus()
                                    }
                                    false
                                },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )
                    }
                }

                if (showClipIcon.value) {
                    IconButton(onClick = {
                        expandedState.value = !expandedState.value
                    }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = if (expandedState.value) "Collapse" else "Expand"
                        )
                    }
                }

                if (transitionState.value && message.text.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(boxWidth)
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                launchPicker(
                                    permissionLauncherImages, imagePickerLauncher, context, "image"
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Select Image"
                                )
                            }
                            IconButton(onClick = {
                                launchPicker(
                                    permissionLauncherVideos, videoPickerLauncher, context, "video"
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = "Select Video"
                                )
                            }
                        }
                    }
                }

                if (!transitionState.value && message.text.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onShowBottomSheet) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = "Comment"
                                )
                            }
                            IconButton(onClick = {
                                if (isRecording.value) {
                                    val recordedFile = AudioRecorder.stopRecording()
                                    onStopRecording(recordedFile)
                                    isRecording.value = false
                                } else {
                                    if (ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) {
                                        AudioRecorder.startRecording(context)
                                        isRecording.value = true
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (isRecording.value) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording.value) "Stop Recording" else "Record Voice Note"
                                )
                            }
                        }
                    }
                }

                if (textNotEmpty) {
                    Box(
                        modifier = Modifier
                            .width(commentBoxWidth)
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onShowBottomSheet) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Comment,
                                    contentDescription = "Comment"
                                )
                            }
                            IconButton(onClick = {
                                onSendMessage()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (showEmojiPicker) maxKeyboardHeightDp else 0.dp)
                    .imePadding()
                    .offset(y = offset), verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShowBottomSheet) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = "Comment"
                    )
                }
            }
        }

        // Recording timer placed below the input row
        if (isRecording.value) {
            Text(
                text = "Recording: ${recordingDuration / 1000}s",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .align(Alignment.End)
            )
        }
    }
}

private fun launchPicker(
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    launcher: ManagedActivityResultLauncher<String, Uri?>,
    context: Context,
    mediaType: String // "image" or "video"
) {
    val permissionToCheck: String
    val mimeType: String

    if (mediaType == "video") {
        mimeType = "video/*"
        permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_VIDEO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    } else { // Defaults to "image"
        mimeType = "image/*"
        permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // Check if the necessary permission is granted
    if (ContextCompat.checkSelfPermission(
            context, permissionToCheck
        ) == PermissionChecker.PERMISSION_GRANTED
    ) {
        launcher.launch(mimeType)
    } else {
        permissionLauncher.launch(permissionToCheck)
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
