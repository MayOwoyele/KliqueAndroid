package com.justself.klique.Bookshelf.Contacts.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import com.justself.klique.R
import com.justself.klique.useful_extensions.initials

@Composable
fun ContactsScreen(navController: NavController) {
    Log.d("Check", "Check")
    Log.d("Check Permissions", "Check")
    val context = LocalContext.current
    val repository = remember { ContactsRepository(context.contentResolver, context) }
    val viewModel = remember { ContactsViewModel(repository) };
    val contactList by viewModel.contacts.collectAsState()


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.refreshContacts()
        }
    }

    LaunchedEffect(Unit) {
        Log.d("Check Permissions", "Check")
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                viewModel.refreshContacts()
                Log.d("Check Permissions", "Check")
            }

            else -> {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                Log.d("Check Permissions", "Check")
            }
        }
    }


    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(contactList.size) { index ->
            ContactTile(contact = contactList[index], navController = navController)
        }
    }
}

@Composable
fun ContactTile(contact: Contact, navController: NavController) {
    val isClickable = contact.isAppUser
    val onTap: () -> Unit = {
        if (isClickable) {
            navController.navigate("messageScreen/${contact.customerId}/${contact.name}")
        }
    }
    Surface(
        modifier = Modifier
            .let {
                if (isClickable) {
                    it.clickable { onTap() }
                } else {
                    it
                }
            }
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
                    .size(50.dp) // Ensure the size is consistent
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
                            .listener(onStart = {
                                Log.d(
                                    "Contact Tile",
                                    "Image loading started for ${contact.thumbnailUrl}"
                                )
                            },
                                onSuccess = { _, _ ->
                                    Log.d(
                                        "Contact Tile",
                                        "Image successfully loaded for ${contact.thumbnailUrl}"
                                    )
                                }, onError = { _, result ->
                                    Log.e(
                                        "Contact Tile",
                                        "Image loading failed for ${contact.thumbnailUrl}, ${result.throwable}"
                                    )
                                })
                            .build()
                    )
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp) // Ensure the size matches the parent Box
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
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onPrimary),
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
                if (contact.isAppUser) {
                    Text(
                        text = "is on klique",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}