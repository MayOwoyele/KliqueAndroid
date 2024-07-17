package com.justself.klique

import android.os.Build
import android.support.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MessageScreen(navController: NavController, enemyId: Int, contactName: String) {
    Scaffold(topBar = {
        CustomTopAppBar(navController = navController, contactName = contactName, enemyId)
    }, content = { innerPadding ->
        MessageScreenContent(navController, enemyId, innerPadding)
    }, bottomBar = {
        TextBoxAndMedia(navController, enemyId)
    }, modifier = Modifier.imePadding()
    )
}

@Composable
fun CustomTopAppBar(navController: NavController, contactName: String, enemyId: Int) {
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
        }
    }
}

@Composable
fun MessageScreenContent(navController: NavController, enemyId: Int, innerPadding: PaddingValues) {
    // Implement your message screen content here
    // You can use enemyId to fetch or display the chat messages
    Column(modifier = Modifier.padding(innerPadding)) {
        // Your chat UI components

        // Add more UI components as needed
    }
}


@Composable
fun TextBoxAndMedia(navController: NavController, enemyId: Int) {
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
            singleLine = true,
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
            )
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
            IconButton(onClick = { /* Handle message sending */ }) {
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