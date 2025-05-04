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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.navigation.NavController
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale

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
    emojiPickerHeight: (Dp) -> Unit,
    chatScreenViewModel: ChatScreenViewModel,
    onDisplayTextChange: (String, Int) -> Unit
) {
    val gistId = viewModel.gistTopRow.collectAsState().value?.gistId
    val message by viewModel.gistMessage
    val observedMessages by viewModel.messages.collectAsState()
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    var maxKeyboardHeightDp by remember { mutableStateOf(0.dp) }
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val homeScreenUri by MediaVM.homeScreenUri.observeAsState()
    viewModel.setMyName(myName)
    val imagePickerLauncher14Plus = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (gistId != null) {
            handleImageUri(context, uri, viewModel, gistId, customerId, myName, coroutineScope)
        }
    }

    val imagePickerLauncherBelow14 = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (gistId != null) {
            handleImageUri(context, uri, viewModel, gistId, customerId, myName, coroutineScope)
        }
    }
    val imagePickerLauncher: (String) -> Unit = { mimeType ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            imagePickerLauncher14Plus.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            imagePickerLauncherBelow14.launch(mimeType)
        }
    }
    val videoPickerLauncher14Plus = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        handleVideoUri(context, uri, coroutineScope, onNavigateToTrimScreen)
    }

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
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
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
    val userStatus by viewModel.userStatus.observeAsState(initial = UserStatus(false, false))
    var showWelcomeDialog by remember { mutableStateOf(!UserAppSettings.suppressAdminTip(appContext)) }
    var dontShowAgain by remember { mutableStateOf(false) }
    if (userStatus.isOwner && showWelcomeDialog) {
        AlertDialogMan(onDismiss = {showWelcomeDialog = false}, dontShowAgain = {bool -> dontShowAgain = bool }, dontShowAgainChecked = dontShowAgain, context = appContext)
    }
    emojiPickerHeight(maxKeyboardHeightDp)
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

    LaunchedEffect(homeScreenUri) {
        Logger.d("onTrim", "ontrim triggered again with value $homeScreenUri")
        homeScreenUri?.let {
            Logger.d("onTrim", "ontrim 2 triggered again with value $homeScreenUri")
            viewModel.handleTrimmedVideo(it)
            MediaVM.clearUris()
        }
    }
    DisposableEffect(Unit) {
        onDisplayTextChange(
            "gist started by ${viewModel.gistTopRow.value?.startedBy}",
            viewModel.gistTopRow.value?.startedById ?: 0
        )
        onDispose { onDisplayTextChange("", 0) }
    }
    val scrollState = rememberLazyListState()
    val showTitle = remember { mutableStateOf(true) }
    var initialMessageCount by remember { mutableIntStateOf(observedMessages.size) }
    var oldestMessageId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(observedMessages.size) {
        oldestMessageId = observedMessages.lastOrNull()?.id
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.layoutInfo.visibleItemsInfo }
            .map { visibleItems ->
                val totalItems = scrollState.layoutInfo.totalItemsCount
                val lastVisibleItemIndex = visibleItems.lastOrNull()?.index ?: 0
                Pair(lastVisibleItemIndex, totalItems)
            }
            .distinctUntilChanged()
            .collect { (lastVisibleItemIndex, totalItems) ->
                val threshold = 1
                if (lastVisibleItemIndex >= totalItems - threshold) {
                    if (oldestMessageId != null) {
                        if (gistId != null) {
                            viewModel.loadOlderMessages(oldestMessageId!!, gistId)
                        }
                    }
                }
            }
    }

    val previousFirstMessageIdReversed = remember {
        mutableStateOf(observedMessages.firstOrNull()?.id)
    }
    val previousLastMessageIdReversed = remember {
        mutableStateOf(observedMessages.lastOrNull()?.id)
    }

    LaunchedEffect(observedMessages.size) {
        if (observedMessages.isNotEmpty()) {
            val newItemsCount = observedMessages.size - initialMessageCount
            if (newItemsCount > 0) {
                val previousStartId = previousFirstMessageIdReversed.value
                val currentStartId = observedMessages.firstOrNull()?.id

                when {
                    currentStartId != previousStartId -> {
                        if (scrollState.firstVisibleItemIndex != 0) {
                            val newIndex = (scrollState.firstVisibleItemIndex + newItemsCount)
                                .coerceAtMost(observedMessages.size - 1) // Ensure index is within bounds
                            val newOffset = scrollState.firstVisibleItemScrollOffset
                            scrollState.scrollToItem(index = newIndex, scrollOffset = newOffset)
                        }
                    }
                }
            }
        }
        initialMessageCount = observedMessages.size
        if (observedMessages.isNotEmpty()) {
            previousFirstMessageIdReversed.value = observedMessages.first().id
            previousLastMessageIdReversed.value = observedMessages.last().id
        }
    }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    showTitle.value = true
                } else if (available.y > 0) {
                    showTitle.value = false
                }
                return Offset.Zero
            }
        }
    }
    val isRecording = remember { mutableStateOf(false) }

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
            coroutineScope.launch {
                try {
                    val audioByteArray = FileUtils.fileToByteArray(it)
                    Logger.d("ChatRoom", "Audio Byte Array: ${audioByteArray.size} bytes")

                    val messageId = viewModel.generateUUIDString()
                    if (gistId != null) {
                        viewModel.sendBinary(
                            audioByteArray,
                            GistMessageType.K_AUDIO.typeString,
                            gistId,
                            messageId,
                            customerId,
                            myName
                        )
                    }
                    val audioUri =
                        getUriFromByteArray(audioByteArray, context, GistMediaType.KAudio)
                    val gistMessage = gistId?.let { it1 ->
                        GistMessage(
                            id = messageId,
                            gistId = it1,
                            senderId = customerId,
                            senderName = myName,
                            content = "",
                            status = GistMessageStatus.Pending,
                            messageType = GistMessageType.K_AUDIO,
                            localPath = audioUri,
                            timeStamp = System.currentTimeMillis().toString()
                        )
                    }
                    if (gistMessage != null) {
                        viewModel.addMessage(gistMessage)
                    }
                } catch (e: IOException) {
                    Log.e("ChatRoom", "Error processing audio: ${e.message}", e)
                }
            }
        }
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
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
                reversedMessages = observedMessages,
                customerId = customerId,
                context = context,
                scrollState = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                navController = navController,
                viewModel = viewModel,
                userStatus = userStatus
            )
            Spacer(modifier = Modifier.height(5.dp))

            InputRow(
                message = message,
                onMessageChange = { viewModel.onGistMessageChange(it) },
                onSendMessage = {
                    if (message.text.trimEnd().isNotEmpty()) {
                        val messageId = viewModel.generateUUIDString()
                        val gistMessage = gistId?.let {
                            GistMessage(
                                id = messageId,
                                gistId = it,
                                senderId = customerId,
                                senderName = myName,
                                content = message.text.trimEnd(),
                                status = GistMessageStatus.Pending,
                                messageType = GistMessageType.K_TEXT,
                                timeStamp = System.currentTimeMillis().toString()
                            )
                        }
                        if (gistMessage != null) {
                            viewModel.addMessage(gistMessage)
                        }
                        val messageJson = JSONObject().apply {
                            put("type", "KText")
                            put("gistId", gistId)
                            put("content", message.text)
                            put("id", messageId)
                            put("senderName", myName)
                        }.toString()

                        viewModel.send(BufferObject(WsDataType.GistRoomChat, messageJson))
                        viewModel.updateGistTimestamp()
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
                context = context,
                isRecording = isRecording,
                onStopRecording = stopRecording,
                audioPermissionLauncher = audioPermissionLauncher,
                onShowBottomSheet = {
                    showBottomSheet = true; if (gistId != null) {
                    viewModel.fetchGistComments(userId = customerId)
                }
                },
                isSpeaker = userStatus.isSpeaker,
                viewModel = viewModel
            )
        }

        // Bottom Sheet
        CustomBottomSheet(
            visible = showBottomSheet,
            onDismissRequest = { showBottomSheet = false },
        ) {
            CommentSection(viewModel, navController, customerId)
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
    val activeUserCount = gistTopRow?.activeSpectators
    val gistTopic = gistTopRow?.topic
    val gistDescription = gistTopRow?.gistDescription
    val id = gistTopRow?.gistId
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (gistTopic != null) {
            Text(
                text = "Gist: ${gistTopic.take(20)}${if (gistTopic.length > 20) "..." else ""}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 20.sp, fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clickable { showInfoDialog.value = true }
            )
        }
        Box {
            var showOwnerInfo by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isOwner) {
                    IconButton(onClick = { showOwnerInfo = true }){
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Admin",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                }
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
                                if ((gistTopic != null) && (id != null)) {
                                    chatScreenViewModel.addGistInviteToForward(gistTopic, id)
                                }
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
            if(showOwnerInfo) {
                AlertDialogMan(onDismiss = {showOwnerInfo = false}, context = appContext, hideCheck = true)
            }
        }
        if (showInfoDialog.value) {
            AlertDialog(
                title = {
                    if (gistTopic != null) {
                        Text(gistTopic, style = MaterialTheme.typography.displayLarge)
                    }
                },
                text = {
                    if (gistDescription != null) {
                        Text(gistDescription, style = MaterialTheme.typography.bodyLarge)
                    }
                },
                onDismissRequest = { showInfoDialog.value = false },
                confirmButton = { /*TODO*/ })
        }
    }
}

@Composable
fun MessageContent(
    reversedMessages: List<GistMessage>,
    customerId: Int,
    context: Context,
    scrollState: LazyListState,
    modifier: Modifier = Modifier,
    navController: NavController,
    viewModel: SharedCliqueViewModel,
    userStatus: UserStatus
) {
    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val randomInt = remember { mutableIntStateOf((0..10000).random()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60 * 1000L)
            currentTime.longValue = System.currentTimeMillis()
        }
    }

    var showOptionsDialog by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<GistMessage?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showReportConfirmation by remember { mutableStateOf(false) }

    LazyColumn(
        state = scrollState,
        reverseLayout = true,
        modifier = modifier.padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(reversedMessages) { message ->
            Logger.d("ChatRoom", "Rendering message: ${message.localPath}")
            val isCurrentUser = message.senderId == customerId
            val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
            val shape = if (isCurrentUser)
                RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
            else
                RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

            val onLongPressLambda = {
                selectedMessage = message
                showOptionsDialog = true
            }
            val onTapLambda = {
                when (message.messageType) {
                    GistMessageType.K_IMAGE -> {
                        message.localPath?.let {
                            MediaVM.setBitmap(convertJpgToBitmap(context, it)!!)
                        }
                        Screen.FullScreenImage.navigate(navController)
                    }
                    GistMessageType.K_VIDEO -> {
                        message.localPath?.let {
                            Screen.FullScreenVideo.navigate(
                                navController,
                                Uri.encode(it.toString())
                            )
                        }
                    }
                    else -> { /* No action for text or audio */ }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(if (isCurrentUser) Alignment.End else Alignment.Start)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPressLambda() },
                            onTap = { onTapLambda() }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Gray, shape)
                        .background(getUserOverlayColor(message.senderId, randomInt.intValue).copy(alpha = 0.2f), shape)
                        .padding(8.dp),
                    horizontalAlignment = alignment
                ) {
                    if (!isCurrentUser) {
                        val userInfo = viewModel.users.collectAsState().value[message.senderId]
                        ProfileThumbnailAndName(
                            userId = message.senderId,
                            profileImageUrl = userInfo?.profileImageUrl,
                            senderName = message.senderName,
                            viewModel = viewModel,
                            navController = navController
                        )
                    }
                    when (message.messageType) {
                        GistMessageType.K_IMAGE -> {
                            var bitmap by remember { mutableStateOf<Bitmap?>(null) }
                            LaunchedEffect(message.localPath) {
                                Logger.d("Local path", "Local path called or not: $message")
                                bitmap = message.localPath?.let { convertJpgToBitmap(context, it) }
                            }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .height(200.dp)
                                        .clip(shape)
                                )
                            } else if (message.externalUrl != null) {
                                Text(
                                    text = "Loading ${GistMediaType.KImage.getGeneralMediaType()}",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Image not available",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        GistMessageType.K_VIDEO -> {
                            var videoUri by remember { mutableStateOf<Uri?>(null) }
                            LaunchedEffect(message.localPath) {
                                videoUri = message.localPath
                            }
                            if (videoUri != null) {
                                val thumbnail = VideoUtils.getVideoThumbnail(context, videoUri!!)
                                val placeholder = createPlaceholderImage(
                                    200,
                                    200,
                                    Color.Gray.toArgb(),
                                    onPrimaryColor
                                )
                                val aspectRatio = thumbnail?.let {
                                    it.width.toFloat() / it.height.toFloat()
                                } ?: 1f
                                Box(
                                    modifier = Modifier
                                        .height(200.dp)
                                        .wrapContentWidth()
                                        .aspectRatio(aspectRatio)
                                        .clip(shape)
                                ) {
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
                            } else if (message.externalUrl != null) {
                                Text(
                                    text = "Loading ${GistMediaType.KVideo.getGeneralMediaType()}",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Video not available",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        GistMessageType.K_AUDIO -> {
                            var audioUri by remember { mutableStateOf<Uri?>(null) }
                            LaunchedEffect(message.localPath) {
                                audioUri = message.localPath
                            }
                            if (audioUri != null) {
                                AudioPlayer(
                                    audioUri = audioUri!!,
                                    modifier = Modifier.widthIn(min = 300.dp, max = 1000.dp),
                                    isSelectionMode = null
                                )
                            } else if (message.externalUrl != null) {
                                Text(
                                    text = "Loading ${GistMediaType.KAudio.getGeneralMediaType()}",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Audio not available",
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        GistMessageType.K_TEXT -> {
                            Box {
                                val lines = 6
                                var visibleLines by remember { mutableIntStateOf(lines) }
                                var fullLineCount by remember { mutableIntStateOf(0) }

                                val hasMoreToShow = visibleLines < fullLineCount
                                val hasMoreToHide = visibleLines > lines
                                val showToggle = fullLineCount > lines
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    onTextLayout = {
                                        if (fullLineCount == 0) {
                                            fullLineCount = it.lineCount
                                        }
                                    },
                                    maxLines = Int.MAX_VALUE,
                                    modifier = Modifier
                                        .widthIn(max = 300.dp)
                                        .then(
                                            Modifier.layout { measurable, constraints ->
                                                measurable.measure(constraints)
                                                layout(0, 0) { }
                                            }
                                        )
                                )

                                Column(modifier = Modifier.animateContentSize()) {
                                    Text(
                                        text = message.content,
                                        color = MaterialTheme.colorScheme.background,
                                        maxLines = visibleLines,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    if (showToggle) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row {
                                            if (hasMoreToShow) {
                                                Text(
                                                    text = "Show more",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .clickable { visibleLines += 10 }
                                                        .padding(end = 8.dp),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            if (hasMoreToHide) {
                                                Text(
                                                    text = "Show less",
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .clickable { visibleLines = maxOf(6, visibleLines - 10) },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(modifier = Modifier.align(Alignment.End)) {
                        Text(
                            text = getCustomRelativeTimeSpanString(
                                message.timeStamp.toLong(),
                                currentTime.longValue
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.background
                        )
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

    // Options Dialog for message actions
    if (showOptionsDialog && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = { Text("Message Options", style = MaterialTheme.typography.displayLarge) },
            text = {
                Column {
                    if (selectedMessage?.senderId == customerId) {
                        // For current user's messages: show delete option
                        TextButton(onClick = {
                            showOptionsDialog = false
                            showDeleteConfirmation = true
                        }) {
                            Text("Delete Message", color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        // For messages not from current user: show report option
                        TextButton(onClick = {
                            showOptionsDialog = false
                            showReportConfirmation = true
                        }) {
                            Text("Report Message", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    // "Set Gist Background" option remains available to owners for media messages
                    if (userStatus.isOwner &&
                        (selectedMessage?.messageType == GistMessageType.K_IMAGE ||
                                selectedMessage?.messageType == GistMessageType.K_VIDEO)
                    ) {
                        TextButton(onClick = {
                            selectedMessage?.let { viewModel.setGistBackground(it.id) }
                            showOptionsDialog = false
                        }) {
                            Text("Set Gist Background")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showDeleteConfirmation && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Confirm Delete", style = MaterialTheme.typography.displayLarge) },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedMessage?.let { viewModel.deleteMessageById(it.id) }
                    showDeleteConfirmation = false
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showReportConfirmation && selectedMessage != null) {
        AlertDialog(
            onDismissRequest = { showReportConfirmation = false },
            title = { Text("Confirm Report", style = MaterialTheme.typography.displayLarge) },
            text = { Text("Are you sure you want to report this message?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedMessage?.let { viewModel.reportMessageById(it.id) }
                    showReportConfirmation = false
                }) {
                    Text("Yes", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportConfirmation = false }) {
                    Text("No")
                }
            }
        )
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
    context: Context,
    isRecording: MutableState<Boolean>,
    onStopRecording: (File?) -> Job?,
    audioPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    onShowBottomSheet: () -> Unit,
    isSpeaker: Boolean,
    viewModel: SharedCliqueViewModel
) {
    val expandedState = remember { mutableStateOf(false) }
    val showClipIcon = remember(message) { mutableStateOf(message.text.isEmpty()) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    val maxRecordingDuration = 2 * 60 * 1000
    val commentCount by viewModel.commentCount.collectAsState()

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
            delay(100)
            transitionState.value = expandedState.value
        } else {
            transitionState.value = expandedState.value
        }
    }

    val offset by animateDpAsState(if (isRecording.value) (-0.5).dp else 0.dp, label = "offset")
    val boxWidth by animateDpAsState(
        targetValue = if (expandedState.value) 100.dp else 5.dp,
        label = "boxWidth"
    )
    val textNotEmpty = message.text.isNotEmpty()
    val commentBoxWidth by animateDpAsState(
        targetValue = if (textNotEmpty) 100.dp else 0.dp,
        label = "commentBoxWidth"
    )

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
                                if (it.text.length <= SessionManager.GLOBAL_CHAR_LIMIT) {
                                    onMessageChange(it)
                                }
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
                            CommentIconButtonBox(commentCount, onShowBottomSheet)
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
                            CommentIconButtonBox(commentCount, onShowBottomSheet)
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
                CommentIconButtonBox(commentCount, onShowBottomSheet)
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

private fun launchPicker(
    permissionLauncher: ManagedActivityResultLauncher<String, Boolean>,
    launcher: (String) -> Unit,
    context: Context,
    mediaType: String
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
    } else {
        mimeType = "image/*"
        permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    if (ContextCompat.checkSelfPermission(
            context, permissionToCheck
        ) == PermissionChecker.PERMISSION_GRANTED
    ) {
        launcher(mimeType)
    } else {
        permissionLauncher.launch(permissionToCheck)
    }
}

fun handleImageUri(
    context: Context,
    uri: Uri?,
    viewModel: SharedCliqueViewModel,
    gistId: String,
    customerId: Int,
    myName: String,
    coroutineScope: CoroutineScope
) {
    uri?.let {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val imageByteArray = ImageUtils.processImageToByteArray(context, uri, 720)
                Logger.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")

                val messageId = viewModel.generateUUIDString()
                viewModel.sendBinary(
                    imageByteArray,
                    GistMessageType.K_IMAGE.typeString,
                    gistId,
                    messageId,
                    customerId,
                    myName
                )
                val imageUri = getUriFromByteArray(imageByteArray, context, GistMediaType.KImage)
                val gistMessage = GistMessage(
                    id = messageId,
                    gistId = gistId,
                    senderId = customerId,
                    senderName = myName,
                    content = "",
                    status = GistMessageStatus.Pending,
                    messageType = GistMessageType.K_IMAGE,
                    localPath = imageUri,
                    timeStamp = System.currentTimeMillis().toString()
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
fun getStatusIcon(status: GistMessageStatus): ImageVector {
    return if (status == GistMessageStatus.Sent) Icons.Filled.Done else Icons.Filled.Schedule
}

suspend fun getUriFromByteArray(
    byteArray: ByteArray,
    context: Context,
    mediaType: GistMediaType
): Uri {
    return withContext(Dispatchers.IO) {
        val mediaCacheDir = File(context.cacheDir, gistMediaCacheDir)
        if (!mediaCacheDir.exists()) {
            mediaCacheDir.mkdirs()
        }
        val file = File(mediaCacheDir, mediaType.getFileName())
        file.writeBytes(byteArray)
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}

const val gistMediaCacheDir = "gistMedia"


fun convertJpgToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        Logger.d("Bitmap Error", "No error converting jpg to Bitmap")
        val inputStream = context.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream).also {
            inputStream?.close()
        }
    } catch (e: Exception) {
        Logger.d("Bitmap Error", "Error converting jpg to Bitmap")
        e.printStackTrace()
        null
    }
}

@Composable
fun ProfileThumbnailAndName(
    userId: Int,
    profileImageUrl: String?,
    senderName: String,
    viewModel: SharedCliqueViewModel,
    navController: NavController
) {
    val currentUser by SessionManager.customerId.collectAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { Screen.BioScreen.navigate(navController, userId) }) {
        val thumbnailBitmap by produceState<Bitmap?>(
            initialValue = null,
            key1 = userId,
            key2 = profileImageUrl
        ) {
            value = if (userId == currentUser) {
                null
            } else {
                profileImageUrl?.let { url ->
                    val newUrl = NetworkUtils.fixLocalHostUrl(url)
                    viewModel.getOrDownloadThumbnail(
                        userId,
                        newUrl,
                        targetWidth = 100,
                        targetHeight = 100
                    )
                }
            }
        }
        val size = 20.dp
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap!!.asImageBitmap(),
                contentDescription = "Profile Thumbnail",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = senderName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun CommentIconButtonBox(
    commentCount: Int,
    onClick: (() -> Unit)
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { onClick() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Comment,
                contentDescription = "Comment",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        if (commentCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    text = formatCount(commentCount),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
@Composable
fun AlertDialogMan(onDismiss: () -> Unit, dontShowAgain: ((Boolean)-> Unit)? = null, dontShowAgainChecked: Boolean? = null, context: Context, hideCheck: Boolean = false){
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Welcome, Owner!", style = MaterialTheme.typography.displayLarge) },
        text = {
            Column {
                Text(
                    "You have admin privileges. You can set any image or video as the gist background that users will see on their timeline. It will improve engagement, just hold any image or video!",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!hideCheck){
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (dontShowAgainChecked != null) {
                            Checkbox(
                                checked = dontShowAgainChecked,
                                onCheckedChange = {
                                    if (dontShowAgain != null) {
                                        dontShowAgain(it)
                                    }
                                }
                            )
                        }
                        Text("Don't show again")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (dontShowAgainChecked == true) {
                    UserAppSettings.setSuppressAdminTip(context, true)
                }
                onDismiss() }) {
                Text("Got it")
            }
        }
    )
}

fun getUserOverlayColor(userId: Int, randomSeed: Int): Color {
    val hue = (userId * randomSeed) % 360
    return Color.hsv(hue.toFloat(), 0.6f, 0.9f, alpha = 0.2f)
}
fun formatCount(count: Int): String {
    return if (count < 1_000) {
        count.toString()
    } else if (count < 1_000_000) {
        String.format(Locale.US, "%.1fK", count / 1_000f).removeSuffix(".0K")
    } else {
        String.format(Locale.US, "%.1fM", count / 1_000_000f).removeSuffix(".0M")
    }
}