package com.justself.klique

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel


@Composable
fun GistSettings(navController: NavController, gistId: String, viewModel: SharedCliqueViewModel) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("Editable Text") }
    var editedText by remember { mutableStateOf(text) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                //.height(200.dp)
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            // Left Box with Image and Pencil Icon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"),
                    contentDescription = "Image with Pencil Icon",
                    modifier = Modifier.fillMaxWidth()
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Icon",
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }

            // Right Box with Text and Pencil Icon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (isEditing) {
                    TextField(
                        value = editedText,
                        onValueChange = { newValue ->
                            if (newValue.length <= 150) {
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
                } else {
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxSize(1f)
                            .padding(4.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .align(alignment = Alignment.Center),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(
                    onClick = { isEditing = !isEditing },
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    // Handle back navigation
                    navController.popBackStack()
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (isEditing) {
                        text = editedText
                        isEditing = false
                    }
                },
                modifier = Modifier
                    .weight(1f) // Makes the button fill the remaining space
            ) {
                Text(text = "Save")
            }
        }
        val listOfContactMembers by viewModel.listOfContactMembers.collectAsState()
        val listOfNonContactMembers by viewModel.listOfNonContactMembers.collectAsState()
        val listOfOwners by viewModel.listOfOwners.collectAsState()
        val listOfSpeakers by viewModel.listOfSpeakers.collectAsState()
        // LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Headers("Owners")
            }
            items(listOfOwners) { member ->
                MyMembersList(member)
            }
            item {
                Headers("Speakers")
            }
            items(listOfSpeakers) { member ->
                MyMembersList(member)
            }
            item {
                Headers("Your Contacts")
            }
            items(listOfContactMembers) { member ->
                MyMembersList(member)
            }
            item {
                Headers("Non Contacts")
            }
            items(listOfNonContactMembers) { member ->
                MyMembersList(member)
            }
        }
    }
}


@Composable
fun MyMembersList(member: Members) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 20.dp)
            .clickable { showMenu = true }
    ) {
        Text(text = member.fullName)

        Box(modifier = Modifier.align(Alignment.BottomStart)) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                if (member.isOwner) {
                    DropdownMenuItem(onClick = {
                        showMenu = false
                    }, text = { Text("You can't change anything about owners") })
                } else if (member.isSpeaker) {
                    DropdownMenuItem(onClick = {
                        // Handle Remove speaker action
                        showMenu = false
                        // Add your logic here
                    }, text = { Text("Remove as Speaker") })
                    DropdownMenuItem(onClick = {
                        // Show confirmation dialog to make owner
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
                        // Handle Make speaker action
                        showMenu = false
                        // Add your logic here
                    }, text = { Text("Make Speaker") })
                }
            }
        }

        if (showConfirmationDialog) {
            ConfirmationDialog(
                onConfirm = {
                    // Handle Make owner action
                    showConfirmationDialog = false
                    // Add your logic here
                },
                onDismiss = { showConfirmationDialog = false }
            )
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
    AlertDialog(
        onDismissRequest = onDismiss,
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
        }
    )
}