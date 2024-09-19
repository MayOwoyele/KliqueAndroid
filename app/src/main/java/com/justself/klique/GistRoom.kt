package com.justself.klique

import ImageUtils
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun GistRoom(
    myName: String,
    viewModel: SharedCliqueViewModel,
    customerId: Int,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    onNavigateToTrimScreen: (String) -> Unit,
    navController: NavController,
    resetSelectedEmoji: () -> Unit,
    mediaViewModel: MediaViewModel,
    emojiPickerHeight: (Dp) -> Unit,
    chatScreenViewModel: ChatScreenViewModel,
    onDisplayTextChange: (String, Int) -> Unit
) {
    val gistId = viewModel.gistTopRow.collectAsState().value.gistId
    val message by viewModel.gistMessage
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
    val homeScreenUri by mediaViewModel.homeScreenUri.observeAsState()
    viewModel.setMyName(myName)
    // Image Picker Launcher for Android 14+ (TIRAMISU)
    val imagePickerLauncher14Plus = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handleImageUri(context, uri, viewModel, gistId, customerId, myName, coroutineScope)
    }

// Image Picker Launcher for Android 13 and below
    val imagePickerLauncherBelow14 = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handleImageUri(context, uri, viewModel, gistId, customerId, myName, coroutineScope)
    }
    // Image Picker Launcher
    val imagePickerLauncher: (String) -> Unit = { mimeType ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 14+ (TIRAMISU and above), launch the PickVisualMedia launcher
            imagePickerLauncher14Plus.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            // For Android 13 and below, launch the GetContent launcher with the MIME type
            imagePickerLauncherBelow14.launch(mimeType)
        }
    }
    // Video Picker Launcher for Android 14+ (TIRAMISU)
    val videoPickerLauncher14Plus = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handleVideoUri(context, uri, coroutineScope, onNavigateToTrimScreen)
    }

// Video Picker Launcher for Android 13 and below
    val videoPickerLauncherBelow14 = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        handleVideoUri(context, uri, coroutineScope, onNavigateToTrimScreen)
    }

    // Video Picker Launcher
    val videoPickerLauncher: (String) -> Unit = { mimeType ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            videoPickerLauncher14Plus.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        } else {
            videoPickerLauncherBelow14.launch(mimeType)
        }
    }

    // Permission Launcher for Images
    val permissionLauncherImages =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    imagePickerLauncher14Plus.launch(PickVisualMediaRequest())
                } else {
                    imagePickerLauncherBelow14.launch("image/*")
                }
            } else {
                // Handle permission denied
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Permission Launcher for Videos
    // Permission Launcher for Videos
    val permissionLauncherVideos =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                // Use the appropriate video picker based on the Android version
                videoPickerLauncher("video/*")
            } else {
                // Handle permission denied
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Handle Keyboard Visibility
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboardHeightDp = with(density) { WindowInsets.ime.getBottom(LocalDensity.current).toDp() }

    LaunchedEffect(key1 = keyboardHeightDp) {
        if (keyboardHeightDp > maxKeyboardHeightDp) maxKeyboardHeightDp = keyboardHeightDp
    }
    emojiPickerHeight(maxKeyboardHeightDp)
    // Handle Emoji Selection
    LaunchedEffect(selectedEmoji) {
        if (selectedEmoji.isNotEmpty()) {
            viewModel.onGistMessageChange(
                message.copy(
                    text = message.text + selectedEmoji,
                    selection = TextRange(message.text.length + selectedEmoji.length)
                )
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
    LaunchedEffect(homeScreenUri) {
        Log.d("onTrim", "ontrim triggered again with value $homeScreenUri")
        homeScreenUri?.let {
            Log.d("onTrim", "ontrim 2 triggered again with value $homeScreenUri")
            viewModel.handleTrimmedVideo(it)
            mediaViewModel.clearUris()
        }
    }
    DisposableEffect(Unit) {
        onDisplayTextChange("gist started by ${viewModel.gistTopRow.value.startedBy}", 25)
        onDispose { onDisplayTextChange("", 0) }
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
    val isRecording = remember { mutableStateOf(false) }

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
                    expanded = expanded,
                    onExpandChange = { expanded = it },
                    viewModel = viewModel,
                    userStatus = userStatus,
                    navController = navController,
                    chatScreenViewModel = chatScreenViewModel
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
                navController = navController,
                viewModel = viewModel,
                mediaViewModel = mediaViewModel
            )
            Spacer(modifier = Modifier.height(5.dp))

            InputRow(
                message = message,
                onMessageChange = { viewModel.onGistMessageChange(it) },
                onSendMessage = {
                    if (message.text.trimEnd().isNotEmpty()) {
                        val messageId = viewModel.generateMessageId()
                        val gistMessage = GistMessage(
                            id = messageId,
                            gistId = gistId,
                            customerId = customerId,
                            sender = myName,
                            content = message.text.trimEnd(),
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
                        viewModel.clearMessage()
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
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    viewModel: SharedCliqueViewModel,
    userStatus: UserStatus,
    navController: NavController,
    chatScreenViewModel: ChatScreenViewModel
) {
    val isOwner = userStatus.isOwner
    val showDialog = remember { mutableStateOf(false) }
    val showInfoDialog = remember { mutableStateOf(false) }
    val gistTopRow by viewModel.gistTopRow.collectAsState()
    val activeUserCount = gistTopRow.activeSpectators
    val gistTopic = gistTopRow.topic
    val gistDescription = gistTopRow.gistDescription
    val id = gistTopRow.gistId
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = "Gist: $gistTopic",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp, fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.clickable { showInfoDialog.value = true }
        )
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "$activeUserCount spectators",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.then(if (isOwner) {
                        Modifier.clickable { navController.navigate("gistSettings/$id") }
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
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { onExpandChange(false) }) {
                        if (isOwner) {
                            DropdownMenuItem(text = { Text("Make Everyone a Speaker") },
                                onClick = { showDialog.value = true })
                            DropdownMenuItem(text = { Text("Gist Settings") },
                                onClick = { navController.navigate("gistSettings/$id") })
                        }
                        DropdownMenuItem(text = { Text("Share") },
                            onClick = {
                                chatScreenViewModel.addGistInviteToForward(gistTopic, id)
                                onExpandChange(false)
                                navController.navigate("forwardChatsScreen")
                            })
                        DropdownMenuItem(text = { Text("Exit") },
                            onClick = { viewModel.exitGist() })
                    }
                    if (showDialog.value) {
                        AlertDialog(
                            onDismissRequest = { showDialog.value = false },
                            title = {
                                Text(
                                    "Are you sure?",
                                    style = MaterialTheme.typography.displayLarge
                                )
                            },
                            text = {
                                Text(
                                    "Are you sure you want to make everyone a speaker? This might cause a rowdy gist",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showDialog.value = false
                                        // Handle the "Yes" action here
                                    }
                                ) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showDialog.value = false }
                                ) {
                                    Text("No")
                                }
                            }
                        )
                    }
                }
            }
        }
        if (showInfoDialog.value) {
            AlertDialog(
                title = { Text(gistTopic, style = MaterialTheme.typography.displayLarge) },
                text = { Text(gistDescription, style = MaterialTheme.typography.bodyLarge) },
                onDismissRequest = { showInfoDialog.value = false },
                confirmButton = { /*TODO*/ })
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
    navController: NavController,
    viewModel: SharedCliqueViewModel,
    mediaViewModel: MediaViewModel
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
            var isExpanded by remember { mutableStateOf(false) }
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
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
                            var bitmap by remember { mutableStateOf<Bitmap?>(null)}
                            LaunchedEffect(message.binaryData) {
                                bitmap = message.binaryData?.let { decodeBinaryToBitmap(it)}
                            }
                            bitmap?.let {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(shape)
                                        .clickable {
                                            mediaViewModel.setBitmap(it)
                                            navController.navigate("fullScreenImage")
                                        }
                                )
                            } ?: Text(
                                text = "Image not available",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        "KVideo" -> {
                            var videoUri by remember { mutableStateOf<Uri?>(null)}
                            LaunchedEffect(message.binaryData) {
                                videoUri = message.binaryData?.let { getUriFromByteArray(it, context)}
                            }
                            videoUri?.let {
                                //val videoUri = getUriFromByteArray(binaryData, context)
                                val thumbnail = VideoUtils.getVideoThumbnail(context, it)
                                val placeholder = createPlaceholderImage(200, 200, Color.Gray.toArgb(), onPrimaryColor)
                                val aspectRatio = thumbnail?.let {
                                    it.width.toFloat() / it.height.toFloat()
                                } ?: 1f
                                Box(modifier = Modifier
                                    .height(200.dp)
                                    .wrapContentWidth()
                                    .aspectRatio(aspectRatio)
                                    .clip(shape)
                                    .clickable {
                                        navController.navigate(
                                            "fullScreenVideo/${Uri.encode(videoUri.toString())}"
                                        )
                                    }) {
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail.asImageBitmap(),
                                            contentDescription = "Video Thumbnail",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            bitmap = placeholder.asImageBitmap(),
                                            contentDescription = "Video Thumbnail",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(48.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } ?: Text(
                                text = "Video not available",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        "KAudio" -> {
                            var audioUri by remember {
                                mutableStateOf<Uri?>(null)}
                            LaunchedEffect(message.binaryData) {
                                audioUri = message.binaryData?.let{ getUriFromByteArray(it, context)}
                            }
                            audioUri?.let {
                                AudioPlayer(
                                    audioUri = it,
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
    imagePickerLauncher: (String) -> Unit,
    videoPickerLauncher: (String) -> Unit,
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
                val recordedFile = AudioRecorder.stopRecording(context)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp)
                    ) {
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
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(alignment = Alignment.CenterVertically)
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
                                    val recordedFile = AudioRecorder.stopRecording(context)
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
    launcher: (String) -> Unit,
    context: Context,
    mediaType: String // "image" or "video"
) {
    val permissionToCheck: String
    val mimeType: String

    if (mediaType == "video") {
        mimeType = "video/*"
        permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    } else { // Defaults to "image"
        mimeType = "image/*"
        permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // Check if the necessary permission is granted
    if (ContextCompat.checkSelfPermission(
            context, permissionToCheck
        ) == PermissionChecker.PERMISSION_GRANTED
    ) {
        launcher(mimeType)
    } else {
        permissionLauncher.launch(permissionToCheck)
    }
}
fun handleImageUri(context: Context, uri: Uri?, viewModel: SharedCliqueViewModel, gistId: String, customerId: Int, myName: String, coroutineScope: CoroutineScope) {
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
fun handleVideoUri(
    context: Context,
    uri: Uri?,
    coroutineScope: CoroutineScope,
    onNavigateToTrimScreen: (String) -> Unit
) {
    uri?.let { selectedUri ->
        coroutineScope.launch {
            val filePath = FileUtils.getPath(context, selectedUri)
            filePath?.let { path ->
                val fileUri = Uri.fromFile(File(path))
                onNavigateToTrimScreen(fileUri.toString())
            } ?: run {
                Log.e("ChatRoom", "Error getting video path")
            }
        }
    }
}

@Composable
fun getStatusIcon(status: String): ImageVector {
    return if (status == "sent") Icons.Filled.Done else Icons.Filled.Schedule
}

suspend fun decodeBinaryToBitmap(binaryData: ByteArray): Bitmap {
    return withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeByteArray(binaryData, 0, binaryData.size).also {
                Log.d("ChatRoom", "Bitmap Decoded: ${it.width}x${it.height}")
            }
        } catch (e: Exception) {
            Log.e("ChatRoom", "Error decoding binary data: ${e.message}", e)
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }
}

suspend fun getUriFromByteArray(byteArray: ByteArray, context: Context): Uri {
    return withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "video.mp4")
        file.writeBytes(byteArray)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}