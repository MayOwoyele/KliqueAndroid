package com.justself.klique

import ImageUtils
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.Forward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

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
    emojiPickerHeight: (Dp) -> Unit,
    isVerified: Boolean
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.enterChat(enemyId)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.leaveChat(); viewModel.clearSelection(); onEmojiPickerVisibilityChange(
            false
        )
            viewModel.clearPersonalChat()
        }
    }
    val myId by viewModel.myUserId.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isRecording = remember { mutableStateOf(false) }
    var theRealTrimmedUri by remember { mutableStateOf<Uri?>(null) }
    val messageScreenUri by mediaViewModel.messageScreenUri.observeAsState()
    LaunchedEffect(messageScreenUri) {
        Log.d("onTrim", "ontrim triggered again with value $messageScreenUri")
        messageScreenUri?.let {
            Log.d("onTrim", "ontrim triggered again 2 with value $messageScreenUri")
            theRealTrimmedUri = messageScreenUri
        }
    }
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
                        val imageByteArray = ImageUtils.processImageToByteArray(context, uri, 1080)
                        Log.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")
                        val messageId = viewModel.generateMessageId()
                        viewModel.sendBinary(
                            imageByteArray,
                            PersonalMessageType.P_IMAGE,
                            enemyId,
                            messageId,
                            myId,
                            contactName,
                            context
                        )
                    } catch (e: IOException) {
                        Log.e("ChatRoom", "Error processing image: ${e.message}", e)
                    }
                }
            }
        }
    LaunchedEffect(theRealTrimmedUri) {
        Log.d("onTrim", "ontrim triggered again 3 with value $messageScreenUri")
        theRealTrimmedUri?.let { uri ->
            Log.d("onTrim", "ontrim triggered again 4 with value $theRealTrimmedUri")
            coroutineScope.launch(Dispatchers.IO) {
                val videoByteArray = context.contentResolver.openInputStream(uri)?.readBytes()
                if (videoByteArray != null) {
                    val messageId = viewModel.generateMessageId()
                    viewModel.sendBinary(
                        videoByteArray,
                        PersonalMessageType.P_VIDEO,
                        enemyId,
                        messageId,
                        myId,
                        contactName,
                        context
                    )
                    Log.d("Add Personal", "This has been called again")
                }
            }
        }
        mediaViewModel.clearUris()
    }
    val videoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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
    emojiPickerHeight(maxKeyboardHeightDp)
    val selectedMessages by viewModel.selectedMessages.observeAsState(emptyList())
    val personalChat by viewModel.personalChats.collectAsState(emptyList())
    val containsMediaMessages = selectedMessages.any { messageId ->
        val messageType = personalChat.find { it.messageId == messageId }?.messageType
        messageType == PersonalMessageType.P_VIDEO || messageType == PersonalMessageType.P_IMAGE || messageType == PersonalMessageType.P_AUDIO
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val onShowDeleteDialog = { showDeleteDialog = false }
    Scaffold(topBar = {
        val targetHeight = if (selectedMessages.isNotEmpty()) 56.dp else 0.dp
        val animatedHeight by animateDpAsState(
            targetValue = targetHeight, animationSpec = tween(durationMillis = 300)
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            if (animatedHeight < 56.dp) {
                CustomTopAppBar(
                    navController = navController,
                    contactName = contactName,
                    enemyId = enemyId,
                    viewModel = viewModel,
                    onEmojiPickerVisibilityChange = onEmojiPickerVisibilityChange
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedHeight)
            ) {
                if (selectedMessages.isNotEmpty() || animatedHeight > 0.dp) {
                    MessageContextMenu(
                        selectedMessages = selectedMessages,
                        onDelete = { showDeleteDialog = true },
                        onCopy = { sortedMessages ->
                            val textMessages = sortedMessages.mapNotNull { messageId ->
                                personalChat.find { it.messageId == messageId }?.takeIf {
                                    it.messageType == PersonalMessageType.P_TEXT
                                }?.content
                            }
                            val copiedText = if (textMessages.size > 1) {
                                textMessages.joinToString("\n")
                            } else {
                                textMessages.firstOrNull().orEmpty()
                            }

                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Copied Messages", copiedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Messages copied", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { viewModel.clearSelection() },
                        showCopyOption = !containsMediaMessages,
                        onForward = { sortedMessages ->
                            sortedMessages.forEach { messageId ->
                                val message = personalChat.find { it.messageId == messageId }
                                message?.let {
                                    viewModel.addMessageToForward(it)
                                }
                            }
                            navController.navigate("forwardChatsScreen")
                        },
                        personalChat = personalChat
                    )
                }
            }
        }
    }, content = { innerPadding ->
        MessageScreenContent(
            navController,
            enemyId,
            innerPadding,
            viewModel,
            myId,
            mediaViewModel,
            showDeleteDialog,
            onShowDeleteDialog
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
                navController.popBackStack()
            }) {
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
                    .clickable(enabled = true, onClick = {
                        navController.navigate("bioScreen/$enemyId")
                        onEmojiPickerVisibilityChange(false)
                    })
            )
            Box(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .size(10.dp)
                    .background(
                        color = if (isOnline) Color(0xFFFF69B4) else Color.Gray, shape = CircleShape
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
    mediaViewModel: MediaViewModel,
    showDeleteDialog: Boolean,
    onShowDeleteDialog: () -> Unit
) {
    val personalChat by viewModel.personalChats.collectAsState(emptyList())
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    val isSelectionMode by viewModel.isSelectionMode.observeAsState(false)
    val selectedMessages by viewModel.selectedMessages.observeAsState(emptyList())
    var initialLoad by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    var lastSeenMessageCount by remember { mutableIntStateOf(personalChat.size) }
    val isAtBottom by remember {
        derivedStateOf {
            scrollState.layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0
        }
    }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(personalChat.size) {
        if (isAtBottom) {
            lastSeenMessageCount = personalChat.size
        }
    }
    fun toggleMessageSelection(messageId: String) {
        viewModel.toggleMessageSelection(messageId)
    }
    LaunchedEffect(key1 = enemyId) {
        viewModel.loadPersonalChats(myId, enemyId)
        initialLoad = true
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo }.map { visibleItems ->
                val totalItems = scrollState.layoutInfo.totalItemsCount
                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: 0
                Pair(lastVisibleItemIndex, totalItems)
            }.distinctUntilChanged().collect { (lastVisibleItemIndex, totalItems) ->
                val threshold = 1
                if (lastVisibleItemIndex >= totalItems - threshold && !viewModel.isLoading.value!! && personalChat.size >= viewModel.pageSize) {
                    Log.d("PersonalChats", "Load More loading")
                    viewModel.loadPersonalChats(
                        myId, enemyId, loadMore = true, personalChat.last().messageId
                    )
                }
            }
    }
    val contentHeight = remember { mutableIntStateOf(0) }
    val viewportHeight = remember { mutableIntStateOf(0) }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo }.collect { layoutInfo ->
            contentHeight.intValue =
                layoutInfo.totalItemsCount * layoutInfo.viewportSize.height / (layoutInfo.visibleItemsInfo.size.takeIf { it > 0 }
                    ?: 1)
            viewportHeight.intValue = layoutInfo.viewportSize.height
        }
    }

    val scrollBarHeight = remember {
        derivedStateOf {
            if (contentHeight.intValue > 0 && viewportHeight.intValue > 0) {
                (viewportHeight.intValue.toFloat() / contentHeight.intValue * viewportHeight.intValue).coerceAtLeast(
                    with(density) { 20.dp.toPx() })
            } else {
                with(density) { 20.dp.toPx() }
            }
        }
    }

    val scrollBarOffset = remember {
        derivedStateOf {
            if (contentHeight.intValue > 0) {
                (scrollState.firstVisibleItemIndex.toFloat() / (personalChat.size - 1).toFloat() * (viewportHeight.value - scrollBarHeight.value)).coerceIn(
                    0f, viewportHeight.intValue - scrollBarHeight.value
                )
            } else {
                0f
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            reverseLayout = true,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(personalChat, key = { it.messageId }) { message ->
                Log.d(
                    "MessageLoading",
                    "Displaying messageId: ${message.messageId}, type: ${message.messageType}, timeStamp: ${message.timeStamp}, content ${message.content}"
                )
                val isCurrentUser = message.myId == myId
                val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
                val shape = if (isCurrentUser) RoundedCornerShape(
                    16.dp, 0.dp, 16.dp, 16.dp
                ) else RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
                val onLongPressLambda = { toggleMessageSelection(message.messageId) }
                val onTapLambda = {
                    if (isSelectionMode) {
                        toggleMessageSelection(message.messageId)
                    } else {
                        when (message.messageType) {
                            PersonalMessageType.P_IMAGE -> {
                                message.mediaUri?.let {
                                    mediaViewModel.setBitmapFromUri(
                                        it, context
                                    )
                                }
                                navController.navigate("fullScreenImage")
                            }

                            PersonalMessageType.P_VIDEO -> {
                                navController.navigate("fullScreenVideo/${Uri.encode(message.mediaUri)}")
                            }

                            else -> {

                            }
                        }
                    }
                }
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(if (isCurrentUser) Alignment.End else Alignment.Start)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onLongPressLambda() },
                            onTap = { onTapLambda() })
                    }) {
                    Column(
                        modifier = Modifier
                            .background(Color.Gray, shape)
                            .padding(8.dp),
                        horizontalAlignment = alignment
                    ) {
                        when (message.messageType) {
                            PersonalMessageType.P_IMAGE -> {
                                DisplayImage(
                                    message.mediaUri,
                                    shape,
                                    navController,
                                    mediaViewModel,
                                    onLongPressLambda,
                                    isSelectionMode,
                                    onTapLambda
                                )
                            }

                            PersonalMessageType.P_VIDEO -> {
                                Log.d("isSelectionMode", "is now $isSelectionMode")
                                DisplayVideo(
                                    message.mediaUri,
                                    shape,
                                    navController,
                                    onLongPressLambda,
                                    isSelectionMode,
                                    onTapLambda
                                )
                            }

                            PersonalMessageType.P_AUDIO -> {
                                Log.d("isSelectionMode", "is now $isSelectionMode")
                                DisplayAudio(
                                    message.mediaUri,
                                    context,
                                    onLongPressLambda,
                                    isSelectionMode,
                                    onTapLambda
                                )
                            }

                            PersonalMessageType.P_GIST_INVITE -> {
                                DisplayGistInvite(
                                    topic = message.topic,
                                    shape = shape,
                                    onLongPressLambda = onLongPressLambda,
                                    isSelectionMode = isSelectionMode,
                                    onTapLambda = onTapLambda
                                ) {
                                    message.gistId?.let {
                                        viewModel.joinGist(it)
                                    }
                                    navController.navigate("home")
                                }
                            }

                            else -> {
                                Box(modifier = Modifier
                                    .wrapContentWidth()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onLongPress = { onLongPressLambda() },
                                            onTap = { onTapLambda() })
                                    }) {
                                    ClickableMessageText(
                                        message.content,
                                        isSelectionMode,
                                        onLongPressLambda,
                                        onTapLambda
                                    )
                                }
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
                    if (isSelectionMode && selectedMessages.contains(message.messageId)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f))
                        )
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(onDismissRequest = onShowDeleteDialog, confirmButton = {
                        TextButton(onClick = {
                            // Delete selected messages logic
                            selectedMessages.forEach { messageId ->
                                viewModel.deleteMessage(
                                    messageId, context
                                )
                            }
                            viewModel.clearSelection()
                            onShowDeleteDialog()
                        }) {
                            Text("Delete")
                        }
                    }, dismissButton = {
                        TextButton(onClick = {
                            viewModel.clearSelection()
                            onShowDeleteDialog()
                        }) {
                            Text("Cancel")
                        }
                    }, title = {
                        Text(
                            "Confirm Deletion", style = MaterialTheme.typography.displayLarge
                        )
                    }, text = { Text("Are you sure you want to delete the selected messages?") })
                }
            }
        }
        if (contentHeight.intValue > viewportHeight.intValue) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(vertical = 8.dp)
                    .width(8.dp)
                    .fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = with(LocalDensity.current) { scrollBarOffset.value.toDp() })
                    .width(8.dp)
                    .height(with(LocalDensity.current) { scrollBarHeight.value.toDp() })
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clip(RoundedCornerShape(50)),
            )
        }
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.firstVisibleItemIndex }
                .collect { index ->
                    if (index == 0) {
                        lastSeenMessageCount = personalChat.size
                    }
                }

        }
        val shouldShowButton by remember {
            derivedStateOf {
                personalChat.size > lastSeenMessageCount || scrollState.firstVisibleItemIndex > viewModel.pageSize
            }
        }
        if (shouldShowButton) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollToItem(0)
                        lastSeenMessageCount = personalChat.size
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 60.dp, end = 16.dp)
                    .imePadding(),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = "Scroll to bottom",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
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
                val recordedFile = AudioRecorder.stopRecording(context)
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
                                    context, Manifest.permission.READ_EXTERNAL_STORAGE
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
                                    context, Manifest.permission.READ_EXTERNAL_STORAGE
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

            BasicTextField(value = textState,
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
                        contentDescription = if (isRecording.value) "StopRecording" else "Voice Note",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .rotate(micRotation)
                            .alpha(transitionAlpha)
                    )
                }
            } else {
                IconButton(onClick = {
                    onSendMessage(textState.text.trimEnd())
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
fun getPersonChatStatusIcon(status: PersonalMessageStatus): ImageVector {
    return when (status) {
        PersonalMessageStatus.SENT -> Icons.Filled.Done
        PersonalMessageStatus.DELIVERED -> Icons.Filled.DoneAll
        PersonalMessageStatus.PENDING -> Icons.Filled.Schedule
    }
}

@Composable
fun MessageContextMenu(
    selectedMessages: List<String>,
    onDelete: () -> Unit,
    onCopy: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    showCopyOption: Boolean,
    onForward: (List<String>) -> Unit,
    personalChat: List<PersonalChat>
) {
    val sortedSelectedMessages = selectedMessages.sortedByDescending { messageId ->
        personalChat.indexOfLast { it.messageId == messageId }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primary),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedMessages.size} selected",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
            Row {
                if (showCopyOption) {
                    IconButton(onClick = { onCopy(sortedSelectedMessages) }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { onForward(sortedSelectedMessages) }) {  // Add this IconButton
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Forward,  // Use a forward icon
                        contentDescription = "Forward", tint = MaterialTheme.colorScheme.onPrimary
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