package com.justself.klique

import ImageUtils
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
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
import androidx.core.content.PermissionChecker
import androidx.emoji2.emojipicker.EmojiPickerView
import kotlinx.coroutines.launch
import java.io.IOException


@Composable
fun ChatRoom(
    topic: String,
    sender: String,
    gistId: Int,
    viewModel: SharedCliqueViewModel,
    customerId: Int,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean
) {
    var message by remember { mutableStateOf(TextFieldValue("")) }
    val observedMessages by viewModel.messages.observeAsState(emptyList())
    val context = LocalContext.current
    val permissionGranted = remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current

    // State to hold the maximum keyboard height
    var maxKeyboardHeightDp by remember { mutableStateOf(0.dp) }
    val focusRequester = remember { FocusRequester() }
    val isFocused = remember { mutableStateOf(false)}

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val imageByteArray = ImageUtils.processImageToByteArray(context, uri)
                val imageBase64 = Base64.encodeToString(imageByteArray, Base64.NO_WRAP)
                val messageId = viewModel.generateMessageId()
                val messageJson = """
                {
                "type": "image",
                "gistId": $gistId,
                "content": "$imageBase64",
                "id": $messageId,
                "sender": "$sender"
                }
                """.trimIndent()
                val chatMessage = ChatMessage(
                    id = messageId,
                    gistId = gistId,
                    customerId = customerId,
                    sender = sender,
                    content = imageBase64,
                    status = "pending",
                    messageType = "image"
                )
                viewModel.addMessage(chatMessage)
                viewModel.send(messageJson)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted.value = granted
        if (granted) {
            imagePickerLauncher.launch("image/*")
        }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val keyboardHeightPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val keyboardHeightDp = with(density) { keyboardHeightPx.toDp() }

    // Update maximum keyboard height
    LaunchedEffect(key1 = keyboardHeightDp) {
        if (keyboardHeightDp > maxKeyboardHeightDp) {
            maxKeyboardHeightDp = keyboardHeightDp
        }
    }

    LaunchedEffect(selectedEmoji) {
        if (selectedEmoji.isNotEmpty()) {
            message = message.copy(
                text = message.text + selectedEmoji,
                selection = TextRange(message.text.length + selectedEmoji.length)
            )
            // Reset the selectedEmoji after using it
        }
    }
    LaunchedEffect(key1 = gistId) {
        if (gistId != 0) {
            try {
                viewModel.loadMessages(gistId)
            } catch (e: Exception) {
                viewModel.close()
            }
        }
    }

    LaunchedEffect(showEmojiPicker, maxKeyboardHeightDp) {
        Log.d("ChatRoom", "showEmojiPicker: $showEmojiPicker, maxKeyboardHeightDp: $maxKeyboardHeightDp")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .imePadding() // Ensure Column respects IME
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Gist: $topic",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )

            IconButton(onClick = { expanded = true }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More options")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Add Member") },
                    onClick = { /* Handle option 1 click */ }
                )
                DropdownMenuItem(
                    text = { Text("Exit") },
                    onClick = { /* Handle option 2 click */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f) // Ensure LazyColumn takes remaining space
                .fillMaxWidth()
                .nestedScroll(remember { object : NestedScrollConnection {} }),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(observedMessages) { message ->
                Log.d("ChatRoom", "Rendering message: ${message.content}") // Debug log
                val isCurrentUser = message.sender == sender
                val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
                val backgroundColor = Color.Gray
                val shape = if (isCurrentUser) {
                    RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
                } else {
                    RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(if (isCurrentUser) Alignment.End else Alignment.Start)
                ) {
                    Column(
                        modifier = Modifier
                            .background(backgroundColor, shape)
                            .padding(8.dp),
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
                        if (message.messageType == "image") {
                            val bitmap = decodeBase64ToBitmap(message.content)
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(200.dp)
                                    .clip(shape)
                            )
                        } else {
                            Text(
                                text = message.content,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Icon(
                            imageVector = getStatusIcon(message.status),
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .align(if (isCurrentUser) Alignment.End else Alignment.Start),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (showEmojiPicker) maxKeyboardHeightDp else 0.dp)
                .imePadding(), // Ensure Row respects IME
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp, max = 150.dp) // Set min and max height
                    .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (imeVisible || showEmojiPicker) {
                        IconButton(onClick = {
                            if (showEmojiPicker) {
                                onEmojiPickerVisibilityChange(false)
                                keyboardController?.show()
                            } else {
                                onEmojiPickerVisibilityChange(true)
                                keyboardController?.hide()
                            }
                        }) {
                            Icon(
                                imageVector = if (showEmojiPicker) Icons.Default.Keyboard else Icons.Default.EmojiEmotions,
                                contentDescription = if (showEmojiPicker) "Show Keyboard" else "Select Emoji"
                            )
                        }
                    }
                    val textScrollState = rememberScrollState()
                    val coroutineScope = rememberCoroutineScope()
                    BasicTextField(
                        value = message,
                        onValueChange = {
                            message = it
                            coroutineScope.launch {
                                textScrollState.scrollTo(textScrollState.maxValue)
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
                                Log.d("ChatRoom", "TextField isFocused: ${focusState.isFocused}") // Log statement added
                                if (focusState.isFocused && showEmojiPicker) {
                                    onEmojiPickerVisibilityChange(false)
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        Log.d("ChatRoom", "TextField tapped") // Log statement for all taps
                                        if (showEmojiPicker) {
                                            Log.d("ChatRoom", "TextField tapped with emoji picker visible") // Log statement added
                                        }
                                        if (isFocused.value) {
                                            // The text field is already focused and was clicked again
                                            if (showEmojiPicker) {
                                                onEmojiPickerVisibilityChange(false)
                                            }
                                            keyboardController?.show() // Show keyboard
                                        } else {
                                            focusRequester.requestFocus()
                                        }
                                    }
                                )
                            }
                            .clickable {
                                Log.d("ChatRoom", "TextField clicked") // Log statement for clicks
                                if (showEmojiPicker) {
                                    Log.d("ChatRoom", "TextField clicked with emoji picker visible") // Log statement added
                                }
                            }
                    )

                }
            }
            IconButton(onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PermissionChecker.PERMISSION_GRANTED
                ) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }) {
                Icon(imageVector = Icons.Default.Image, contentDescription = "Select Image")
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
                        "gistId": $gistId,
                        "content": "${message.text.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\t", "\\t")
                        .replace("\b", "\\b")}",
                        "id": $messageId,
                        "sender": "$sender"
                        }
                        """.trimIndent()
                    viewModel.send(messageJson)
                    message = TextFieldValue("")
                }
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}


@Composable
fun getStatusIcon(status: String): ImageVector {
    return if (status == "sent") {
        Icons.Filled.Done
    } else {
        Icons.Filled.Schedule
    }
}

fun decodeBase64ToBitmap(base64Str: String): Bitmap {
    return try {
        val decodedString = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    } catch (e: Exception) {
        Log.e("ChatRoom", "Error decoding base64 string: ${e.message}")
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }
}
