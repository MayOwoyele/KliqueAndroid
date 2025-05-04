package com.justself.klique

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@Composable
fun UpdateProfileScreen(
    navController: NavController,
    customerId: Int,
    viewModel: ProfileViewModel
) {
    LaunchedEffect(key1 = Unit) {
            viewModel.fetchProfile()
    }
    val bioInfo by viewModel.tinyProfileDetails.collectAsState()
    var profilePictureUrl by remember { mutableStateOf(bioInfo.profileUrl) }
    var bio by remember { mutableStateOf(bioInfo.bioText) }
    var newProfilePictureUri: Uri? by remember { mutableStateOf(null) }
    var bioChanged by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isDownscaling by MediaVM.isDownScalingImageForUpdateProfile.collectAsState()
    val croppedBitmap by MediaVM.croppedBitmap.observeAsState()
    croppedBitmap?.let { bitmap ->
        val byteArray = FileUtils.bitmapToByteArray(bitmap)
        val uri = FileUtils.saveImage(context, byteArray, true)
        MediaVM.clearCroppedBitmap()
        MediaVM.setIsDownScalingImageForUpdateProfile(false)
        uri?.let {
            newProfilePictureUri = uri
        } ?: run {
            Toast.makeText(context, "Failed to save image.", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(key1 = bioInfo) {
        profilePictureUrl = bioInfo.profileUrl
    }

    Column {
        ProfilePictureSection(
            profilePictureUrl = profilePictureUrl,
            onImageChange = { uri ->
                if (uri != null) {
                    MediaVM.setBitmapFromUri(uri, context)
                }
                navController.navigate("imageEditScreen/${SourceScreen.PROFILE.name}")
            },
            newProfilePictureUri = newProfilePictureUri
        )
        BioSection(
            onBioChange = {
                bio = it
                bioChanged = true
            },
            viewModel = viewModel
        )
        SaveChangesButton(
            profilePictureChanged = newProfilePictureUri != null,
            bioChanged = bioChanged,
            onSaveChanges = {
                viewModel.updateProfile(
                    newProfilePictureUri,
                    bio,
                    context,
                    customerId
                )
            }
        )
        if (isDownscaling) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Processing image...",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilePictureSection(
    profilePictureUrl: String,
    onImageChange: (Uri?) -> Unit,
    newProfilePictureUri: Uri?
) {
    var shouldCheckPermission by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            onImageChange(uri)
        }

    Box(modifier = Modifier.padding(16.dp)) {
        val imageToDisplay = newProfilePictureUri ?: profilePictureUrl
        LaunchedEffect(key1 = Unit) {
            Logger.d("ProfileImageUri", "$newProfilePictureUri, $profilePictureUrl")
        }
        Image(
            painter = rememberAsyncImagePainter(model = imageToDisplay),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(280.dp)
                .clip(CircleShape)
                .border(2.dp, Color.Gray, CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
        )
        IconButton(
            onClick = { shouldCheckPermission = true },
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Profile Picture")
            if (shouldCheckPermission) {
                CheckAndRequestImagePermissions(context = context) {
                    imagePickerLauncher.launch("image/*")
                    shouldCheckPermission = false
                }
            }
        }
    }
}

@Composable
fun BioSection(
    onBioChange: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    var isEditing by remember { mutableStateOf(false) }
    val bioText by viewModel.bio.collectAsState()
    var text by remember { mutableStateOf(bioText) }

    LaunchedEffect(bioText) {
        if (!isEditing) {
            text = bioText
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Bio", style = MaterialTheme.typography.bodyLarge)
        if (isEditing) {
            TextField(
                value = text,
                onValueChange = {
                    val sanitizedText = it.replace("\n", "")
                    if (sanitizedText.length <= 500) {
                        text = sanitizedText
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                TextButton(onClick = { onBioChange(text); isEditing = false; viewModel.updateBio(text) }) { Text("Save") }
            }
        } else {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = { isEditing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Bio")
            }
        }
    }
}

@Composable
fun SaveChangesButton(
    profilePictureChanged: Boolean,
    bioChanged: Boolean,
    onSaveChanges: () -> Unit
) {
    val changesExist = profilePictureChanged || bioChanged
    Button(
        onClick = onSaveChanges,
        enabled = changesExist,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Save Changes")
    }
}

@Composable
fun CheckAndRequestImagePermissions(
    context: Context,
    onPermissionGranted: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPermissionGranted()
        } else {
            Toast.makeText(context, "Permission denied to access images.", Toast.LENGTH_SHORT)
                .show()
        }
    }

    LaunchedEffect(Unit) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    onPermissionGranted()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }

            else -> {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    onPermissionGranted()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
}