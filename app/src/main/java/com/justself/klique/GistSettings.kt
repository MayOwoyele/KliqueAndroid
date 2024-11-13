package com.justself.klique

import ImageUtils
import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.launch
import okio.IOException


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun GistSettings(navController: NavController, viewModel: SharedCliqueViewModel) {
    var isEditing by remember { mutableStateOf(false) }
    val descriptionFromServer by viewModel.gistTopRow.collectAsState()
    var text by remember { mutableStateOf(descriptionFromServer?.gistDescription) }
    var editedText by remember { mutableStateOf(text) }
    var isSearchMode by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val gistTopRow by viewModel.gistTopRow.collectAsState()
    val gistId = gistTopRow?.gistId
    val searchResults by viewModel.searchResults.observeAsState(emptyList())
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val minCharacterLimit = 10
    BackHandler {
        navController.popBackStack()
        viewModel.turnSearchPerformedOff()
    }
    LaunchedEffect(gistId) {
        if (gistId != null) {
            viewModel.fetchMembersFromServer(gistId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (isEditing) {
                    editedText?.let {
                        TextField(
                            value = it,
                            onValueChange = { newValue ->
                                if (newValue.length <= 100) {
                                    editedText = newValue.replace("\n", "")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .height(100.dp)
                                .padding(4.dp)
                                .align(alignment = Alignment.Center),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.Sentences
                            )
                        )
                    }
                } else {
                    text?.let {
                        Text(
                            text = it,
                            modifier = Modifier
                                .fillMaxSize(1f)
                                .padding(4.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                                .align(alignment = Alignment.Center),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                IconButton(
                    onClick = { isEditing = !isEditing; isSearchMode = false },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit, contentDescription = "Edit Icon"
                    )
                }
            }
        }
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSearchMode) {
                TextField(value = searchQuery,
                    onValueChange = { newValue ->
                        if (newValue.length <= 20) {
                            searchQuery = newValue
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(16.dp)),
                    singleLine = true,
                    placeholder = { Text("search using their alias, not contact name...") },
                    leadingIcon = {
                        IconButton(onClick = {
                            isSearchMode = false
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.doTheSearch(searchQuery)
                        }
                    )
                )

            } else {
                IconButton(onClick = {
                    navController.popBackStack()
                    viewModel.turnSearchPerformedOff()
                    viewModel.unsubscribeToMembersUpdate()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if ((editedText?.length ?: 0) < minCharacterLimit) {
                            errorMessage = "Description must be at least $minCharacterLimit characters long."
                        } else {
                            text = editedText
                            isEditing = false
                            editedText?.let { viewModel.sendUpdatedDescription(it, gistId!!) }
                            errorMessage = null
                        }
                    }, modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Save")
                }
                IconButton(onClick = {
                    isSearchMode = true
                }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
                }
            }
        }

        val listOfContactMembers by viewModel.listOfContactMembers.collectAsState()
        val listOfNonContactMembers by viewModel.listOfNonContactMembers.collectAsState()
        val listOfOwners by viewModel.listOfOwners.collectAsState()
        val listOfSpeakers by viewModel.listOfSpeakers.collectAsState()
        val searchPerformed by viewModel.searchPerformed.observeAsState(false)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isSearchMode) {
                if (searchPerformed && searchResults.isEmpty()) {
                    item {
                        Text(
                            text = "Contact isn't in gist",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else  {
                    items(searchResults) {member ->
                        MyMembersList(member = member, viewModel)
                    }
                }
            } else {
                item {
                    Headers("Owners")
                }
                items(listOfOwners) { member ->
                    MyMembersList(member, viewModel)
                }
                item {
                    Headers("Speakers")
                }
                items(listOfSpeakers) { member ->
                    MyMembersList(member, viewModel)
                }
                item {
                    Headers("Your Contacts")
                }
                items(listOfContactMembers) { member ->
                    MyMembersList(member, viewModel)
                }
                item {
                    Headers("Non Contacts")
                }
                items(listOfNonContactMembers) { member ->
                    MyMembersList(member, viewModel)
                }
            }
        }
    }
}


@Composable
fun MyMembersList(member: Members, viewModel: SharedCliqueViewModel) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .padding(horizontal = 20.dp)
        .clickable { showMenu = true }) {
        Text(text = member.fullName)

        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                if (member.isOwner) {
                    DropdownMenuItem(onClick = {
                        showMenu = false
                    }, text = { Text("You can't change anything about owners") })
                } else if (member.isSpeaker) {
                    DropdownMenuItem(onClick = {
                        viewModel.removeAsSpeaker(member.customerId)
                        showMenu = false
                    }, text = { Text("Remove as Speaker") })
                    DropdownMenuItem(onClick = {
                        showMenu = false
                        showConfirmationDialog = true
                    }, text = { Text("Make Owner") })
                } else {
                    DropdownMenuItem(onClick = {
                        // Show confirmation dialog to make owner
                        showMenu = false
                        showConfirmationDialog = true
                    }, text = { Text("Make Owner") })
                    DropdownMenuItem(onClick = {
                        viewModel.makeSpeaker(member.customerId)
                        showMenu = false
                    }, text = { Text("Make Speaker") })
                }
            }
        }

        if (showConfirmationDialog) {
            ConfirmationDialog(onConfirm = {
                showConfirmationDialog = false
                viewModel.makeOwner(member.customerId)
            }, onDismiss = { showConfirmationDialog = false })
        }
    }
}

@Composable
fun Headers(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 50.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
fun ConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(text = "Confirmation", style = MaterialTheme.typography.displayLarge) },
        text = {
            Text(
                "Are you sure you want to make this member an owner? This action is irreversible",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("No")
            }
        })
}
