package com.justself.klique

import android.content.Context
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import androidx.core.view.WindowInsetsCompat.Type.ime
import kotlinx.coroutines.Dispatchers

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MessageScreen(
    navController: NavController,
    enemyId: Int,
    viewModel: ChatScreenViewModel,
    onNavigateToTrimScreen: (String) -> Unit,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    contactName: String,
    mediaViewModel: MediaViewModel,
    resetSelectedEmoji: () -> Unit,
    emojiPikerHeight: (Dp) -> Unit,
    theTrimmedUri: Uri?,
    onResetTrimmed: () -> Unit
) {
    DisposableEffect(Unit) {
        viewModel.enterChat(enemyId)
        onDispose { viewModel.leaveChat() }
    }
    val myId by viewModel.myUserId.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRecording = remember { mutableStateOf(false) }
    BackHandler {
        onEmojiPickerVisibilityChange(false)
        Log.d("BackHandler", "BackHandler")
        navController.popBackStack()
    }
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                        Log.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")

                        val messageId = viewModel.generateMessageId()
                        viewModel.sendBinary(
                            imageByteArray, "PImage", enemyId, messageId, myId, contactName, context
                        )

                    } catch (e: IOException) {
                        Log.e("ChatRoom", "Error processing image: ${e.message}", e)
                    }
                }
            }
        }
    LaunchedEffect(theTrimmedUri) {
        theTrimmedUri?.let { uri ->
            coroutineScope.launch(Dispatchers.IO) {
                val videoByteArray = context.contentResolver.openInputStream(uri)?.readBytes()
                if (videoByteArray != null) {
                    val messageId = viewModel.generateMessageId()
                    viewModel.sendBinary(
                        videoByteArray,
                        "PVideo",
                        enemyId,
                        messageId,
                        myId,
                        contactName, context
                    )
                    Log.d("Add Personal", "This has been called again")
                }
            }
        }
        onResetTrimmed()
    }
    val videoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { selectedUri ->
                coroutineScope.launch(Dispatchers.IO) {
                    val filePath = FileUtils.getPath(context, selectedUri)
                    filePath?.let { path ->
                        val fileUri = Uri.fromFile(File(path))
                        val downscaledUri = VideoUtils.downscaleVideo(context, fileUri)
                        Log.d("VideoProcessing", "Downscaled URI: $downscaledUri")
                        coroutineScope.launch(Dispatchers.Main) {
                            if (downscaledUri == null) {
                                Log.e("VideoProcessing", "Downscaling failed, using original file.")
                                onNavigateToTrimScreen(fileUri.toString())  // Use the original file URI if downscaling fails
                            } else {
                                onNavigateToTrimScreen(downscaledUri.toString())  // Use the downscaled URI if successful
                            }
                        }
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
            viewModel.handleRecordedAudio(it, enemyId, myId, contactName, context)
        }
    }
    val onSendMessage: (String) -> Unit = { message ->
        if (message.isNotEmpty()) {
            viewModel.sendTextMessage(message, enemyId, myId)
        }
    }
    var maxKeyboardHeightDp by remember { mutableStateOf(0.dp) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboardHeightDp =
        with(LocalDensity.current) { WindowInsets.ime.getBottom(LocalDensity.current).toDp() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(key1 = keyboardHeightDp) {
        if (keyboardHeightDp > maxKeyboardHeightDp) maxKeyboardHeightDp = keyboardHeightDp
    }
    emojiPikerHeight(maxKeyboardHeightDp)
    Scaffold(topBar = {
        CustomTopAppBar(
            navController = navController,
            contactName = contactName,
            enemyId,
            viewModel,
            onEmojiPickerVisibilityChange
        )
    }, content = { innerPadding ->
        MessageScreenContent(
            navController,
            enemyId,
            innerPadding,
            viewModel,
            myId,
            mediaViewModel
        )
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
            showEmojiPicker = showEmojiPicker,
            isRecording = isRecording,
            imeVisible = imeVisible,
            maxKeyboardHeightDp = maxKeyboardHeightDp,
            resetSelectedEmoji = resetSelectedEmoji,
            keyboardController = keyboardController
        )
    }, modifier = Modifier.imePadding()
    )
}

@Composable
fun CustomTopAppBar(
    navController: NavController,
    contactName: String,
    enemyId: Int,
    viewModel: ChatScreenViewModel,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit
) {
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
            IconButton(onClick = {
                onEmojiPickerVisibilityChange(false)
                navController.popBackStack() }) {
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
    viewModel: ChatScreenViewModel,
    myId: Int,
    mediaViewModel: MediaViewModel
) {
    val personalChat by viewModel.personalChats.observeAsState(emptyList())
    Log.d("Personal Chats", "The value of personalChats $personalChat")
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()
    LaunchedEffect(key1 = enemyId) {
            Log.d("Personal Chats", "Loading chats for enemyId: $enemyId")
            viewModel.loadPersonalChats(myId, enemyId)
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
                            DisplayImage(message.mediaUri, shape, navController, mediaViewModel)
                        }

                        "PVideo" -> {
                            DisplayVideo(message.mediaUri, shape, navController)
                        }

                        "PAudio" -> {
                            DisplayAudio(message.mediaUri, context)
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

@OptIn(ExperimentalComposeUiApi::class)
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
    onStopRecording: (File?) -> Unit,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    isRecording: MutableState<Boolean>,
    imeVisible: Boolean,
    maxKeyboardHeightDp: Dp,
    resetSelectedEmoji: () -> Unit,
    keyboardController: SoftwareKeyboardController?
) {
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var expanded by remember { mutableStateOf(false) }
    val textNotEmpty = textState.text.isNotEmpty()
    val boxHeight by animateDpAsState(targetValue = if (textNotEmpty) Dp.Unspecified else 56.dp)
    val focusRequester = remember { FocusRequester() }
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
    val maxDuration = 2 * 60 * 1000
    var recordingDuration by remember { mutableStateOf(0) }
    LaunchedEffect(isRecording.value) {
        if (isRecording.value) {
            recordingDuration = 0
            while (isRecording.value && (recordingDuration < maxDuration)) {
                delay(1000)
                recordingDuration += 1000
            }
            if (recordingDuration >= maxDuration) {
                val recordedFile = AudioRecorder.stopRecording()
                onStopRecording(recordedFile)
                isRecording.value = false
            }
        }
    }
    val isFocused = remember { mutableStateOf(false) }
    if (textState.text.isNotEmpty() || imeVisible) {
        expanded = false
    }
    LaunchedEffect(selectedEmoji) {
        if (selectedEmoji.isNotEmpty()) {
            textState = textState.copy(
                text = textState.text + selectedEmoji,
                selection = TextRange(textState.text.length + selectedEmoji.length)
            )
        }
        resetSelectedEmoji()
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = if (showEmojiPicker) maxKeyboardHeightDp else 0.dp)
                .background(MaterialTheme.colorScheme.background)

        ) {
            val isEmpty = textState.text.isEmpty() && (!imeVisible && !showEmojiPicker)
            if (isEmpty) {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach Media",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                if (imeVisible || showEmojiPicker) {
                    IconButton(onClick = {
                        onEmojiPickerVisibilityChange(!showEmojiPicker)
                        if (showEmojiPicker) keyboardController?.show() else keyboardController?.hide()
                    }) {
                        Icon(
                            imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Outlined.EmojiEmotions,
                            contentDescription = if (showEmojiPicker) "Show Keyboard" else "Select Emoji",
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
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncherImages.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) -> {
                                    imagePickerLauncher.launch("image/*")
                                }

                                else -> {
                                    permissionLauncherImages.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Pick Image",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncherVideos.launch(Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            when (PackageManager.PERMISSION_GRANTED) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                                ) -> {
                                    videoPickerLauncher.launch("video/*")
                                }

                                else -> {
                                    permissionLauncherVideos.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                            }
                        }
                    }) {
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
                    .padding(horizontal = 8.dp)
                    .heightIn(min = 56.dp, max = 150.dp)
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
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(boxHeight)
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
                    capitalization = KeyboardCapitalization.Sentences,
                    keyboardType = KeyboardType.Text
                ),
                cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
            )

            if (textState.text.isEmpty()) {
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
                        contentDescription = if (isRecording.value) "StopRecording" else "Voice Note",
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

@Composable
fun getPersonChatStatusIcon(status: String): ImageVector {
    return when (status) {
        "sent" -> Icons.Filled.Done // Single checkmark for sent
        "delivered" -> Icons.Filled.DoneAll // Double checkmark for delivered
        // "read" -> Icons.Filled.Visibility Eye icon for read (optional, if you want to differentiate read status)
        else -> Icons.Filled.Schedule // Clock icon for pending or unknown status
    }
}