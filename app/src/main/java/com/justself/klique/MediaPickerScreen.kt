package com.justself.klique

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat

@Composable
fun MediaPickerScreen(navController: NavController, source: String, mediaViewModel: MediaViewModel, customerId: Int) {
    when (source) {
        "video" -> VideoPickerScreen(navController = navController, customerId = customerId, mediaViewModel)
        "image" -> ImagePickerScreen(navController = navController, mediaViewModel = mediaViewModel)
    }
}

@Composable
fun VideoPickerScreen(navController: NavController, customerId: Int, mediaViewModel: MediaViewModel) {
    mediaViewModel.setCustomerId(customerId)
    val context = LocalContext.current
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navController.navigate("VideoTrimScreen/${Uri.encode(it.toString())}/VideoStatus")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            videoPickerLauncher.launch("video/*")
        } else {
            Toast.makeText(context, "Permission denied to read your external storage", Toast.LENGTH_SHORT).show()
        }
    }
    fun pickVideo() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED) {
                    videoPickerLauncher.launch("video/*")
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_VIDEO)
                }
            }
            else -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    videoPickerLauncher.launch("video/*")
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center){
        IconButton(
            onClick = {navController.popBackStack()},
            modifier = Modifier.align(Alignment.TopStart)
        ){
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Button(onClick = {pickVideo()}, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSecondary, contentColor = MaterialTheme.colorScheme.onPrimary)){
            Text("Pick a video", style = MaterialTheme.typography.displayLarge)
        }
    }
}
@Composable
fun ImagePickerScreen(navController: NavController, mediaViewModel: MediaViewModel) {
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            mediaViewModel.setBitmapFromUri(uri, context)
            navController.navigate("imageEditScreen/${SourceScreen.STATUS.name}")
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            imagePickerLauncher.launch("image/*")
        } else {
            Toast.makeText(context, "Permission denied to read your external storage", Toast.LENGTH_SHORT).show()
        }
    }
    fun pickImage() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
            else -> {
                // Request the legacy external storage permission for Android 12 and below
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    imagePickerLauncher.launch("image/*")
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center){
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Button(onClick = { pickImage() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSecondary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
            Text("Pick an image", style = MaterialTheme.typography.displayLarge)
        }
    }
}