package com.justself.klique

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Observer
import com.justself.klique.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun HomeScreen(
    customerId: Int,
    fullName: String,
    viewModel: SharedCliqueViewModel,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean
) {
    val coroutineScope = rememberCoroutineScope()
    var showOptions by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    // Observe gist state from ViewModel
    val gistState by viewModel.gistCreatedOrJoined.observeAsState()
    val gistActive = gistState != null
    val gistTopic = gistState?.first.orEmpty()
    val gistId = gistState?.second.orEmpty()

    val density = LocalDensity.current
    val buttonPosition = remember { mutableStateOf(Offset.Zero) }
    val enterTransition: EnterTransition = slideIn(
        initialOffset = {
            IntOffset(buttonPosition.value.x.roundToInt(), buttonPosition.value.y.roundToInt())
        },
        animationSpec = tween(durationMillis = 300)
    ) + scaleIn(
        initialScale = 0.3f,
        animationSpec = tween(durationMillis = 300)
    )
    val exitTransition: ExitTransition = slideOut(
        targetOffset = {
            IntOffset(buttonPosition.value.x.roundToInt(), buttonPosition.value.y.roundToInt())
        },
        animationSpec = tween(durationMillis = 300)
    ) + scaleOut(
        targetScale = 0.3f,
        animationSpec = tween(durationMillis = 300)
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val messageObserver = Observer<List<ChatMessage>> { newMessages ->
            newMessages.let {
                // Handle new messages if needed
            }
        }

        // Attach observers
        viewModel.messages.observe(lifecycleOwner, messageObserver)

        onDispose {
            // Detach observers
            viewModel.messages.removeObserver(messageObserver)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (gistActive) {
            ChatRoom(
                topic = gistTopic,
                sender = fullName,
                gistId = gistId,
                viewModel = viewModel,
                customerId = customerId,
                onEmojiPickerVisibilityChange = onEmojiPickerVisibilityChange,
                selectedEmoji = selectedEmoji,
                showEmojiPicker = showEmojiPicker
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = { showOptions = false },
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp)
                ) {
                    AnimatedVisibility(
                        visible = showOptions,
                        enter = enterTransition,
                        exit = exitTransition
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .padding(end = 72.dp) // Adjust padding to position the options correctly
                                .zIndex(1f)
                        ) {
                            OptionButton(
                                text = "Start Gist",
                                onClick = {
                                    coroutineScope.launch {
                                        showForm = true
                                        showOptions = false
                                    }
                                }
                            )
                            OptionButton(
                                text = "Start Ajo",
                                onClick = {
                                    coroutineScope.launch {
                                        showOptions = false
                                    }
                                }
                            )
                        }
                    }
                    AddButton(
                        onClick = {
                            showOptions = !showOptions
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(56.dp),
                        icon = Icons.Default.Add
                    )
                }
            }

            // GistForm overlay Box
            if (showForm) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            onClick = { showForm = false },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GistForm(onSubmit = { topic, type ->
                        coroutineScope.launch {
                            viewModel.startGist(topic, type)
                            showForm = false
                        }
                    }, onBack = { showForm = false })
                }
            }
        }
    }
}

@Composable
private fun AddButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.primary)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun OptionButton(
    text: String,
    onClick: () -> Unit
) {
    Button(onClick = onClick) {
        Text(text)
    }
}

@Composable
fun GistForm(onSubmit: (String, String) -> Unit, onBack: () -> Unit) {
    var topic by remember { mutableStateOf(TextFieldValue("")) }
    var selectedType by remember { mutableStateOf("public") } // State for selected gist type

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row with back arrow and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) { // Back arrow button
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Start a New Gist", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input field for gist topic
        OutlinedTextField(
            value = topic,
            onValueChange = { topic = it },
            label = { Text("Gist Topic") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Binary selection for gist type
        Text(text = "Gist Type")
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            RadioButton(
                selected = selectedType == "public",
                onClick = { selectedType = "public" }
            )
            Text(text = "Public", modifier = Modifier.clickable { selectedType = "public" })
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = selectedType == "private",
                onClick = { selectedType = "private" }
            )
            Text(text = "Private", modifier = Modifier.clickable { selectedType = "private" })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Submit button
        Button(onClick = {
            onSubmit(topic.text, selectedType)
        }) {
            Text("Submit")
        }
    }
}