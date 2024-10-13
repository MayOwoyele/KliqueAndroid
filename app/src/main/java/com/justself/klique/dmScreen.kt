package com.justself.klique

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController


@Composable
fun DMChatScreen(
    navController: NavHostController,
    customerId: Int,
    receiverName: String,
    chatPartnerId: Int,
    viewModel: ChatViewModel = viewModel(),
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle
) {
    val messages by viewModel.messages.collectAsState()
    val sendMessageStatus by viewModel.sendMessageStatus.collectAsState()
    val (messageText, setMessageText) = remember { mutableStateOf("") }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.startPolling(customerId, chatPartnerId)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopPolling()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    LaunchedEffect(key1 = Unit) {
        Log.d("DMChatScreen", "Fetching messages for customer ID: $customerId, chatPartner ID: $chatPartnerId")
        viewModel.fetchMessages(customerId, chatPartnerId)
    }
    LaunchedEffect(sendMessageStatus) {
        when (sendMessageStatus) {
            ChatViewModel.SendMessageStatus.Success -> {
                setMessageText("")  // Clear the text field on success
                viewModel.resetSendMessageStatus()  // Reset status to avoid duplicate actions
            }
            ChatViewModel.SendMessageStatus.Failure -> {
                // Optionally handle failure (e.g., show error message)
            }
            else -> {}  // Handle any other status if necessary
        }
    }
    // Track IME (keyboard) and navigation bar insets
    val imeInsets = WindowInsets.ime
    val navigationBarsInsets = WindowInsets.navigationBars

    // Calculate bottom padding based on insets
    val bottomPadding = with(LocalDensity.current) {
        (imeInsets.getBottom(LocalDensity.current) + navigationBarsInsets.getBottom(LocalDensity.current)).toDp()
    }

    // Main UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        DMTopBar(navController, receiverName)
        MessageDisplay(
            messages = messages,
            customerId = customerId,
            modifier = Modifier.weight(1f)
        )
        MessageInputField(messageText, setMessageText) {
            viewModel.sendMessage(customerId, chatPartnerId, messageText)
        }
    }
}

@Composable
fun DMTopBar(navController: NavHostController, receiverName: String) {
    // Define the height and background color directly within the Row
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .height(20.dp),  // Standard app bar height
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Navigation Icon
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Spacer to create a gap between the icon and topic
        Spacer(modifier = Modifier.width(8.dp))

        // Title / Receiver Name
        Text(
            text = "Chat with $receiverName",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f)  // Makes the text take up the remaining space
        )
    }
}

@Composable
fun MessageDisplay(messages: List<Message>, customerId: Int, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier) {
        items(messages) { message ->
            if (message.senderId == customerId) {
                OutgoingMessageView(message.messageContent)
            } else {
                IncomingMessageView(message.messageContent)
            }
        }
    }
}


@Composable
fun OutgoingMessageView(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = message,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),  // Apply rounded corners to the left side
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun IncomingMessageView(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = message,
            modifier = Modifier
                .background(Color.Gray, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),  // Apply rounded corners to the right side
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun MessageInputField(
    messageText: String,
    setMessageText: (String) -> Unit,
    onSend: () -> Unit  // Lambda to handle sending the message
) {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = messageText,
            onValueChange = setMessageText,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Type a message...") },
            singleLine = true
        )
        Button(
            onClick = onSend,  // Use the provided lambda for sending the message
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text("Send")
        }
    }
}
