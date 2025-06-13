package com.justself.klique.ContactsBlock.Contacts.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.justself.klique.ContactsBlock.Contacts.data.Contact
import com.justself.klique.LocalContactsViewModel
import com.justself.klique.Logger
import com.justself.klique.Screen
import com.justself.klique.useful_extensions.initials
import kotlinx.coroutines.delay

@Composable
fun ContactsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel = LocalContactsViewModel.current
    val contactList by viewModel.contacts.collectAsState()
    val isLoading = remember { mutableStateOf(true) }

    var hasPermission by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val hasWaitedLongEnough = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(5000)
        hasWaitedLongEnough.value = true
        viewModel.updateAndRefreshContacts(context)
    }

    CheckContactsPermission(
        onPermissionResult = { granted ->
            hasPermission = granted
        },
        onPermissionGranted = {
            viewModel.refreshContacts(context)
        }
    )

    LaunchedEffect(contactList) {
        isLoading.value = contactList.isEmpty()
    }
    val filteredContacts = if (searchQuery.isEmpty()) {
        contactList
    } else {
        contactList.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Contacts permission is required.")
        }
    } else {
        Box {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading.value) {
                        if (hasWaitedLongEnough.value) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "You have no contacts.",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            items(filteredContacts.size) { index ->
                                ContactTile(
                                    contact = filteredContacts[index],
                                    navController = navController
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    )
                }
                Crossfade(
                    targetState = isSearching,
                    animationSpec = tween(durationMillis = 300),
                    modifier = Modifier.weight(1f)
                ) { targetIsSearching ->
                    if (targetIsSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search contacts") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth())
                    }
                }
                IconButton(onClick = {
                    if (isSearching) {
                        isSearching = false
                        searchQuery = ""
                    } else {
                        isSearching = true
                    }
                }) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearching) "Close search" else "Search contacts",
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactTile(
    contact: Contact,
    navController: NavController
) {
    val context = LocalContext.current
    val isClickable = contact.isAppUser
    var showInviteDialog by remember { mutableStateOf(false) }
    val additionalDetail = if (contact.isAppUser) {
        "is on klique"
    } else {
        "invite to klique"
    }
    val onTap: () -> Unit = {
        if (isClickable) {
            contact.customerId?.let {
                Screen.MessageScreen.navigate(navController, contact.customerId)
            }
        } else {
            showInviteDialog = true
        }
    }

    Surface(
        modifier = Modifier
            .clickable { onTap() }
            .height(100.dp)
            .border(
                1.dp,
                MaterialTheme.colorScheme.onPrimary,
                RoundedCornerShape(bottomStart = 50.dp)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .weight(3F),
                contentAlignment = Alignment.Center
            ) {
                if (contact.thumbnailUrl != null) {
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(contact.thumbnailUrl)
                            .crossfade(true)
                            .size(Size(50, 50)) // Adjust the size as needed
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .listener(
                                onStart = {
                                    Logger.d("Contact Tile", "Image loading started for ${contact.thumbnailUrl}")
                                },
                                onSuccess = { _, _ ->
                                    Logger.d("Contact Tile", "Image successfully loaded for ${contact.thumbnailUrl}")
                                },
                                onError = { _, result ->
                                    Log.e(
                                        "Contact Tile",
                                        "Image loading failed for ${contact.thumbnailUrl}, ${result.throwable}"
                                    )
                                }
                            )
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.initials(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp),
                                color = MaterialTheme.colorScheme.background
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(9F)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 17.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = additionalDetail,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }

    if (showInviteDialog) {
        InviteContactDialog(
            contact = contact,
            onDismiss = { showInviteDialog = false },
            onInvite = {
                val shareText =
                    "Hey ${contact.name}, let's gist on klique klique. Join the app at app.kliquesocial.com"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                val chooserIntent = Intent.createChooser(shareIntent, "Invite via")
                context.startActivity(chooserIntent)
                showInviteDialog = false
            }
        )
    }
}

@Composable
fun InviteContactDialog(
    contact: Contact,
    onDismiss: () -> Unit,
    onInvite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Invite ${contact.name} to Klique Klique?",
                style = MaterialTheme.typography.displayLarge
            )
        },
        text = {
            Text(
                "Would you like to invite ${contact.name} to join the klique klique app?",
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            TextButton(onClick = onInvite) {
                Text("Invite")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CheckContactsPermission(
    onPermissionResult: (Boolean) -> Unit,
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            onPermissionGranted()
        }
        onPermissionResult(isGranted)
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            onPermissionGranted()
            onPermissionResult(true)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
}