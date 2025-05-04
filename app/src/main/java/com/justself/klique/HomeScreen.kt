package com.justself.klique

import android.se.omapi.Session
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import com.justself.klique.ContactsBlock.Contacts.ui.CheckContactsPermission
import com.justself.klique.ContactsBlock.Contacts.ui.ContactsViewModel
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.ui.GistScreen
import com.justself.klique.gists.ui.viewModel.CliqueViewModelNavigator
import com.justself.klique.sharedUi.AddButton
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    emojiPickerHeight: (Dp) -> Unit,
    chatScreenViewModel: ChatScreenViewModel,
    onDisplayTextChange: (String, Int) -> Unit,
    gistId: String?,
    commentId: String?
) {
    var showOptions by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    val gistCreationError by viewModel.gistCreationError.observeAsState()
    val gistState by viewModel.gistCreatedOrJoined.observeAsState()
    val gistActive = gistState != null

    gistCreationError?.let { error ->
        ErrorDialog(
            errorMessage = error,
            onDismiss = { viewModel.clearGistCreationError() }
        )
    }
    LaunchedEffect(Unit) {
        if (CliqueViewModelNavigator.toActivate) {
            viewModel.createAltGistAndNotify(
                CliqueViewModelNavigator.post!!,
                CliqueViewModelNavigator.type!!,
                CliqueViewModelNavigator.enemyId!!,
                CliqueViewModelNavigator.inviteId!!
            )
            CliqueViewModelNavigator.clearNavigator()
        }
        if (gistId != null) {
            Logger.d("GistId", "The gist id is $gistId")
            viewModel.enterGist(gistId)
        }
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
                    customerId = customerId,
                    viewModel = viewModel,
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
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                        .clickable(
                            onClick = { showForm = false },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ),
                    contentAlignment = Alignment.Center
                ) {
//                    GistForm(viewModel = viewModel, onSubmit = { post, type, namesList, selectedType ->
//                        coroutineScope.launch {
//                            viewModel.startGist(post, type, namesList, selectedType, chatScreenViewModel)
//                            showForm = false
//                        }
//                    }, onBack = { showForm = false }
//                    )
                    StartGistDialog(
                        viewModel,
                        ContactsViewModel(
                            ContactsRepository(
                                appContext.contentResolver,
                                appContext
                            )
                        ),
                        chatScreenViewModel,
                        onDismiss = { showForm = false })
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
        val appUsersOnly = contacts.filter { it.customerId != null }
        val onlineSet = onlineContacts.toSet()
        val onlineList = appUsersOnly.filter { it.customerId in onlineSet }
        val offlineList = appUsersOnly.filter { it.customerId !in onlineSet }
        onlineList.sortedBy { it.name } + offlineList.sortedBy { it.name }
    }
    var emptyText by remember { mutableStateOf("Loading your online contacts...") }
    LaunchedEffect(mergedContacts) {
        if (mergedContacts.isNotEmpty()) {
            viewModel.askForHomeOnlineContacts(contacts)
        }
    }
    LaunchedEffect(Unit) {
        delay(5000)
        emptyText = "You have no contacts, invite your friends to join klique"
    }
    DisposableEffect(Unit) {
        WebSocketManager.isGistFormVisible = true
        WebSocketManager.clearWebsocketBuffer(WsDataType.GistFormUnsubscription)
        onDispose {
            WebSocketManager.isGistFormVisible = false
            viewModel.unsubscribeToGfUpdates()
            WebSocketManager.clearWebsocketBuffer(WsDataType.HomeOnlineContacts)
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
            value = viewModel.post,
            onValueChange = { newText ->
                viewModel.onPostChange(newText)
                showPostError = newText.text.length < minPostLength
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
                text = "Gist",
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
                text = "News",
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
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
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
                val isPostValid = viewModel.post.text.length in minPostLength..maxPostLength
                val isUserSelectionValid = selectedUserIds.isNotEmpty()
                if (isPostValid && isUserSelectionValid) {
                    showPostError = false
                    onSubmit(
                        viewModel.post.text,
                        selectedType.value,
                        selectedUserIds.toList(),
                        selectedType
                    )
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
fun StartGistDialog(
    viewModel: SharedCliqueViewModel,
    contactsVm: ContactsViewModel,
    chatVm: ChatScreenViewModel,
    onDismiss: () -> Unit
) {
    val contacts by contactsVm.contacts.collectAsState()
    val onlineIds by viewModel.onlineContacts.collectAsState()
    val nonFriends by viewModel.nonFriends.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf(GistType.Public) }
    val selectedFriends = remember { mutableStateListOf<Int>() }
    val selectedStrangers = remember { mutableStateListOf<Int>() }
    var tooShort by remember { mutableStateOf(false) }
    val maxChars = 150
    val tooLong = viewModel.post.text.length > maxChars
    var emptyFriendsText by remember { mutableStateOf("Loading your friends…") }

    val appUsers = contacts.filter { it.customerId != null }
    val mergedFriends = remember(appUsers, onlineIds) {
        val myId = SessionManager.customerId.value
        val (on, off) = appUsers
            .filter { it.customerId != myId }
            .partition { it.customerId in onlineIds }
        (on.sortedBy { it.name } + off.sortedBy { it.name })
    }
    val strangers = remember(nonFriends, mergedFriends) {
        val friendIds = mergedFriends.mapNotNull { it.customerId }.toSet()
        nonFriends.filter { it.userId !in friendIds && it.userId != SessionManager.customerId.value }
    }
    LaunchedEffect(mergedFriends) {
        viewModel.askForHomeOnlineContacts(contacts)
    }
    LaunchedEffect(Unit) {
        delay(5000)
        if (mergedFriends.isEmpty()) {
            emptyFriendsText = "You have no contacts, invite your friends to join Klique"
        }
    }
    val pink = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val tabIndex = pagerState.currentPage
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnClickOutside = true, usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .heightIn(max = 550.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    val type = MaterialTheme.typography.displayLarge
                    Tab(selected = tabIndex == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } }
                    ) { Text("Friends", color = if (tabIndex == 0) pink else onPrimary, style = type) }
                    Tab(selected = tabIndex == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } }
                    ) { Text("Strangers", color = if (tabIndex == 1) pink else onPrimary, style = type) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.fillMaxWidth()){
                        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Pink700)
                        }
                        Text("What’s the gist?", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) { page ->
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp)
                        ) {
                            OutlinedTextField(
                                value = viewModel.post,
                                onValueChange = { rawValue ->
                                    val sanitizedText = rawValue.text
                                        .replace("\r", "")
                                        .replace("\n", " ")
                                    val filteredValue = rawValue.copy(text = sanitizedText)
                                    if (filteredValue.text.length <= maxChars + 2) {
                                        viewModel.onPostChange(filteredValue)
                                    }
                                    tooShort = filteredValue.text.isEmpty()
                                },
                                placeholder = { Text("What’s on your mind?") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 1
                            )
                            Spacer(Modifier.height(12.dp))
                            if (page == 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = selectedType == GistType.Public,
                                        onClick = { selectedType = GistType.Public },
                                        colors = RadioButtonDefaults.colors(selectedColor = Pink700)
                                    )
                                    Text("Gist")
                                    Spacer(Modifier.width(16.dp))
                                    RadioButton(
                                        selected = selectedType == GistType.Private,
                                        onClick = { selectedType = GistType.Private },
                                        colors = RadioButtonDefaults.colors(selectedColor = Pink700)
                                    )
                                    Text("News")
                                }
                                Text(
                                    if (selectedType == GistType.Public)
                                        "New members are speakers by default"
                                    else
                                        "You choose who gets to speak, others can comment",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.height(8.dp))
                            }

                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                if (page == 0) {
                                    if (mergedFriends.isEmpty()) {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                emptyFriendsText,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    } else LazyColumn {
                                        itemsIndexed(mergedFriends) { idx, c ->
                                            val id = c.customerId!!
                                            SelectableRow(
                                                label = c.name,
                                                checked = id in selectedFriends,
                                                onToggle = {
                                                    if (id in selectedFriends)
                                                        selectedFriends.remove(id)
                                                    else selectedFriends.add(id)
                                                },
                                                showDot = id in onlineIds
                                            )
                                            if (idx < mergedFriends.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier
                                                        .padding(
                                                            start = 56.dp,
                                                            end = 0.dp
                                                        ) // indent if you like
                                                        .fillMaxWidth(),
                                                    thickness = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    if (strangers.isEmpty()) EmptyBox()
                                    else LazyColumn {
                                        itemsIndexed(strangers) { idx, s ->
                                            SelectableRow(
                                                label = s.name,
                                                checked = s.userId in selectedStrangers,
                                                onToggle = {
                                                    if (s.userId in selectedStrangers)
                                                        selectedStrangers.remove(s.userId)
                                                    else selectedStrangers.add(s.userId)
                                                },
                                                showDot = true
                                            )
                                            if (idx < strangers.lastIndex) {
                                                HorizontalDivider(
                                                    modifier = Modifier
                                                        .padding(
                                                            start = 56.dp,
                                                            end = 0.dp
                                                        ) // indent if you like
                                                        .fillMaxWidth(),
                                                    thickness = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val translucentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    if (pagerState.currentPage < pagerState.pageCount - 1) {
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .width(24.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Transparent, translucentColor)
                                    )
                                )
                                .pointerInput(Unit) { /* pass touches through */ }
                        )
                    }

                    // 3) Fade on the left when you’re not on the first page
                    if (pagerState.currentPage > 0) {
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .width(24.dp)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(translucentColor, Color.Transparent)
                                    )
                                )
                                .pointerInput(Unit) { /* pass touches through */ }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                val cleanup = {
                    selectedFriends.clear()
                    selectedStrangers.clear()
                    tooShort = false
                    selectedType = GistType.Public
                    viewModel.onPostChange(TextFieldValue(""))
                }
                if (tooLong) {
                    Text(
                        "Must not exceed $maxChars characters",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(
                    enabled = (selectedFriends.isNotEmpty() || selectedStrangers.isNotEmpty())
                            && viewModel.post.text.isNotBlank()
                            && !tooShort && !tooLong,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    onClick = {
                        scope.launch {
                            if (pagerState.currentPage == 0) {
                                viewModel.startGist(
                                    viewModel.post.text,
                                    selectedType.value,
                                    selectedFriends.toList(),
                                    selectedType,
                                    chatVm,
                                    cleanup
                                )
                            } else {
                                viewModel.inviteNonFriends(
                                    selectedStrangers.toList(),
                                    viewModel.post.text
                                )
                                cleanup()
                            }
                            onDismiss()
                        }
                    }
                ) {
                    Text("Start Gist")
                }
            }
        }
    }
}
@Composable
private fun EmptyBox() =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("No strangers to invite 😅", style = MaterialTheme.typography.bodyMedium)
    }

@Composable
private fun SelectableRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    showDot: Boolean
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
        .fillMaxWidth()
        .clickable { onToggle() }
        .padding(horizontal = 12.dp, vertical = 8.dp)
) {
    Checkbox(checked = checked, onCheckedChange = { onToggle() })
    Spacer(Modifier.width(8.dp))
    Text(label, Modifier.weight(1f))
    if (showDot) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Pink700)
        )
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
    Private("private");

    companion object {
        fun fromString(value: String): GistType {
            return when (value.lowercase(Locale.getDefault())) {
                "public" -> Public
                "private" -> Private
                else -> Public
            }
        }
    }
}
