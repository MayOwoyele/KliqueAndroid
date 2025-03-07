package com.justself.klique

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import com.justself.klique.ContactsBlock.Contacts.ui.CheckContactsPermission
import com.justself.klique.ContactsBlock.Contacts.ui.ContactsViewModel
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.ui.GistScreen
import com.justself.klique.sharedUi.AddButton
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    customerId: Int,
    fullName: String,
    viewModel: SharedCliqueViewModel,
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
    val coroutineScope = rememberCoroutineScope()
    var showOptions by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    val gistCreationError by viewModel.gistCreationError.observeAsState()
    val gistState by viewModel.gistCreatedOrJoined.observeAsState()
    val gistActive = gistState != null
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

    gistCreationError?.let { error ->
        ErrorDialog(
            errorMessage = error,
            onDismiss = { viewModel.clearGistCreationError() }
        )
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        if (gistActive) {
            GistRoom(
                myName = fullName,
                viewModel = viewModel,
                customerId = customerId,
                onEmojiPickerVisibilityChange = onEmojiPickerVisibilityChange,
                selectedEmoji = selectedEmoji,
                showEmojiPicker = showEmojiPicker,
                onNavigateToTrimScreen = onNavigateToTrimScreen,
                navController = navController,
                resetSelectedEmoji = resetSelectedEmoji,
                mediaViewModel = mediaViewModel,
                emojiPickerHeight = emojiPickerHeight,
                chatScreenViewModel = chatScreenViewModel,
                onDisplayTextChange = onDisplayTextChange
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
                GistScreen(
                    modifier = Modifier
                        .padding(top = 15.dp)
                        .padding(horizontal = 16.dp)
                        .fillMaxSize(),
                    customerId = customerId,
                    viewModel = viewModel,
                    navController
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(32.dp)
                ) {
                    AddButton(
                        onClick = {
                            showForm = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(56.dp),
                        icon = Icons.Default.Edit
                    )
                }
            }
            if (showForm) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                        .clickable(
                            onClick = { showForm = false },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GistForm(viewModel = viewModel, onSubmit = { post, type, namesList, selectedType ->
                        coroutineScope.launch {
                            viewModel.startGist(post, type, namesList, selectedType, chatScreenViewModel)
                            showForm = false
                        }
                    }, onBack = { showForm = false }
                    )
                }
            }
        }
    }
}

@Composable
fun GistForm(
    viewModel: SharedCliqueViewModel,
    onSubmit: (String, String, List<Int>, GistType) -> Unit,
    onBack: () -> Unit
) {
    var hasContactsPermission by remember { mutableStateOf(false) }
    val repository = remember { ContactsRepository(appContext.contentResolver, appContext) }
    val contactViewModel = remember { ContactsViewModel(repository) }
    val contacts by contactViewModel.contacts.collectAsState()
    CheckContactsPermission(
        onPermissionResult = { granted ->
            hasContactsPermission = granted
        },
        onPermissionGranted = {
            contactViewModel.updateContactFromHomeScreen(appContext)
        }
    )

    if (!hasContactsPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Contacts permission is required.")
        }
        return
    }
    var post by remember { mutableStateOf(TextFieldValue("")) }
    var selectedType by remember { mutableStateOf(GistType.Public) }
    val minPostLength = 5
    val maxPostLength = 120
    var showPostError by remember { mutableStateOf(false) }
    val selectedUserIds = remember { mutableStateListOf<Int>() }
    val info = when (selectedType) {
        GistType.Public -> "This means any new member into the gist is a Speaker by default"
        GistType.Private -> "You have to manually choose who gets to speak but everyone can drop comments"
    }
    val onlineContacts by viewModel.onlineContacts.collectAsState()
    val mergedContacts = remember(contacts, onlineContacts) {
        val onlineSet = onlineContacts.toSet()
        val onlineList = contacts.filter { it.customerId in onlineSet }
        val offlineList = contacts.filter { it.customerId !in onlineSet }
        onlineList.sortedBy { it.name } + offlineList.sortedBy { it.name }
    }
    var emptyText by remember { mutableStateOf("Loading your online contacts...") }
    LaunchedEffect(contacts) {
        if (contacts.isNotEmpty()) {
            viewModel.askForHomeOnlineContacts(contacts)
        }
    }
    LaunchedEffect(Unit) {
        delay(5000)
        emptyText = "You have no contacts, invite your friends to join klique"
    }
    DisposableEffect(Unit) {
        WebSocketManager.isGistFormVisible = true
        onDispose {
            WebSocketManager.isGistFormVisible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "What is the gist?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = post,
            onValueChange = { newText ->
                if (newText.text.length <= maxPostLength) {
                    post = newText
                    showPostError = newText.text.length < minPostLength
                } else {
                    showPostError = true
                }
            },
            label = { Text("What's on your mind?", color = MaterialTheme.colorScheme.onPrimary) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            RadioButton(
                selected = selectedType == GistType.Public,
                onClick = { selectedType = GistType.Public },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Text(
                text = "Open",
                modifier = Modifier.clickable { selectedType = GistType.Public },
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = selectedType == GistType.Private,
                onClick = { selectedType = GistType.Private },
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Text(
                text = "Controlled",
                modifier = Modifier.clickable { selectedType = GistType.Private },
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text(text = info, color = MaterialTheme.colorScheme.onPrimary)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select at least one user:", color = MaterialTheme.colorScheme.onPrimary)
        val cornerRadius = 12.dp
        Box(
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            if (mergedContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emptyText,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            } else {
                LazyColumn {
                    itemsIndexed(mergedContacts) { index, user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedUserIds.contains(user.customerId)) {
                                        selectedUserIds.remove(user.customerId)
                                    } else {
                                        user.customerId?.let { selectedUserIds.add(it) }
                                    }
                                }
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = selectedUserIds.contains(user.customerId),
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        user.customerId?.let { selectedUserIds.add(it) }
                                    } else {
                                        selectedUserIds.remove(user.customerId)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.background
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = user.name,
                                color = MaterialTheme.colorScheme.background
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (user.customerId in onlineContacts) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape )
                                )
                            }
                        }
                        if (index < mergedContacts.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.background)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val isPostValid = post.text.length in minPostLength..maxPostLength
                val isUserSelectionValid = selectedUserIds.isNotEmpty()
                if (isPostValid && isUserSelectionValid) {
                    showPostError = false
                    onSubmit(post.text, selectedType.value, selectedUserIds.toList(), selectedType)
                } else {
                    showPostError = true
                }
            },
            enabled = selectedUserIds.isNotEmpty()
        ) {
            Text("Submit", color = MaterialTheme.colorScheme.background)
        }
        if (showPostError) {
            val errorMessage = if (selectedUserIds.isEmpty()) {
                "Please select at least one user"
            } else {
                "Description must be between $minPostLength and $maxPostLength characters"
            }
            Text(
                errorMessage,
                color = MaterialTheme.colorScheme.background,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun ErrorDialog(
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Gist Creation Error", style = MaterialTheme.typography.displayLarge)

                Spacer(modifier = Modifier.height(16.dp))

                Text(errorMessage, style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { onDismiss() }) {
                    Text("OK", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

enum class GistType(val value: String) {
    Public("public"),
    Private("private")
}
data class User(val userId: Int, val name: String)
