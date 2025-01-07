package com.justself.klique

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

@Composable
fun DmRoom(
    navController: NavController,
    myId: Int,
    enemyId: Int,
    enemyName: String,
    mediaViewModel: MediaViewModel,
    viewModel: DmRoomViewModel = viewModel(
        factory = DmRoomViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val context = LocalContext.current
    val toastWarning by viewModel.toastWarning.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                        Log.d("ChatRoom", "Image Byte Array: ${imageByteArray.size} bytes")

                        val messageId = viewModel.generateMessageId()
                        Log.d("Dm", messageId)
                        viewModel.sendBinary(
                            image = imageByteArray,
                            messageType = DmMessageType.DImage,
                            enemyId = enemyId,
                            messageId = messageId,
                            myId = myId,
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
            viewModel.sendTextMessage(message, enemyId, myId)
        }
    }
    LaunchedEffect(key1 = toastWarning) {
        if (toastWarning != null){
            Toast.makeText(context, toastWarning, Toast.LENGTH_LONG).show()
        }
        viewModel.resetToastWarning()
    }
    LaunchedEffect(Unit) {
        Log.d("Dm", "$enemyId")
        viewModel.loadDmMessages(enemyId)
    }

    Scaffold(
        topBar = {
            CustomDmTopAppBar(navController, enemyName, viewModel, enemyId)
        },
        content = { innerPadding ->
            DmRoomContent(
                navController,
                innerPadding,
                viewModel,
                myId,
                enemyId,
                mediaViewModel,
                enemyName
            )
        },
        bottomBar = {
            DmTextBoxAndMedia(
                navController = navController,
                context = context,
                imagePickerLauncher = imagePickerLauncher,
                permissionLauncher = readStoragePermissionLauncher,
                onSendMessage = onSendMessage
            )
        }
    )
}

@Composable
fun CustomDmTopAppBar(
    navController: NavController,
    enemyName: String,
    viewModel: DmRoomViewModel,
    enemyId: Int
) {
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
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Text(
                text = enemyName,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable { navController.navigate("bioScreen/$enemyId") }
            )
        }
    }
}

@Composable
fun DmRoomContent(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: DmRoomViewModel,
    myId: Int,
    enemyId: Int,
    mediaViewModel: MediaViewModel,
    enemyName: String
) {
    val dmMessages by viewModel.dmMessages.collectAsState()
    LaunchedEffect(dmMessages){
        Log.d("Parsing", "Ui log $dmMessages")
    }
    val lazyListState = rememberLazyListState()
    var lastSeenMessageCount by remember { mutableIntStateOf(dmMessages.size) }
    val coroutineScope = rememberCoroutineScope()
    val isAtTop by remember { derivedStateOf { lazyListState.firstVisibleItemIndex == dmMessages.lastIndex } }
    val isAtBottom by remember { derivedStateOf { lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0 } }
    val isScrollable by remember {
        derivedStateOf {
            lazyListState.layoutInfo.totalItemsCount > 0 &&
                    lazyListState.layoutInfo.visibleItemsInfo.size < lazyListState.layoutInfo.totalItemsCount
        }
    }
    LaunchedEffect(dmMessages.size) {
        if (isAtBottom) {
            lastSeenMessageCount = dmMessages.size
        }
    }
    LaunchedEffect(isAtTop) {
        if (isAtTop && isScrollable){
            dmMessages.lastOrNull()?.let { viewModel.loadAdditionalMessages(it.messageId, enemyId) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            reverseLayout = true,
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(dmMessages, key = { it.messageId }) { message ->
                DmMessageItem(
                    message = message,
                    isCurrentUser = message.senderId == myId,
                    navController = navController,
                    mediaViewModel = mediaViewModel,
                    enemyName = enemyName
                )
            }
        }
        if (dmMessages.size > lastSeenMessageCount) {
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                        lastSeenMessageCount = dmMessages.size
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

@Composable
fun DmMessageItem(
    message: DmMessage,
    isCurrentUser: Boolean,
    navController: NavController,
    mediaViewModel: MediaViewModel,
    enemyName: String
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
                text = enemyName,
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
                .wrapContentWidth(alignment),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, shape)
            ) {
                when (message.messageType) {
                    DmMessageType.DText -> Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DmMessageType.DImage -> {
                        ChatRoomImageItem(
                            image = message.localPath,
                            shape = shape,
                            mediaViewModel = mediaViewModel,
                            navController = navController
                        )
                    }
                }
            }

            if (isCurrentUser) {
                val icon = when (message.status) {
                    DmMessageStatus.SENDING -> Icons.Default.Schedule
                    DmMessageStatus.SENT -> Icons.Default.Done
                    DmMessageStatus.UNSENT -> Icons.Default.ArrowDownward
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
fun DmTextBoxAndMedia(
    navController: NavController,
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
        // Image picker button
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

        // Text input field
        TextField(
            value = messageText,
            onValueChange = {
                if (it.length <= 2000) {
                    messageText = it
                }
            },
            placeholder = { Text("1KC per message") },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        IconButton(
            onClick = {
                if (messageText.trimEnd().isNotEmpty()) {
                    onSendMessage(messageText.trimEnd())
                    messageText = ""
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
        }
    }
}