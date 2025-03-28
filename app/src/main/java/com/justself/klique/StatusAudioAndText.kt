package com.justself.klique

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StatusAudio(navController: NavController) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var permissionsGranted by remember { mutableStateOf(false) }
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var recordingDuration by remember { mutableIntStateOf(0) }
    val audioStatus by MediaVM.audioStatusSubmissionResult.collectAsState()
    LaunchedEffect(audioStatus) {
        audioStatus?.let {statusSent ->
            if (statusSent) {
                Toast.makeText(context, "Audio status sent successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Audio status couldn't send", Toast.LENGTH_SHORT).show()
            }
        }
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.all { it.value }
        if (!permissionsGranted) {
            Toast.makeText(context, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.RECORD_AUDIO)
            )
        }
    }
    LaunchedEffect(Unit) {
        checkAndRequestPermissions()
    }

    fun startRecording(context: Context) {
        if (permissionsGranted) {
            AudioRecorder.startRecording(context)
            isRecording = true
            recordingDuration = 0
            coroutineScope.launch {
                while (isRecording) {
                    delay(1000)
                    recordingDuration++
                }
            }
        } else {
            checkAndRequestPermissions()
        }
    }

    fun stopRecording() {
        coroutineScope.launch(Dispatchers.IO) {
            val file = AudioRecorder.stopRecording(context)
            file?.let {
                val uri = Uri.fromFile(it)
                withContext(Dispatchers.Main) {
                    audioUri = uri
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Recording failed", Toast.LENGTH_SHORT).show()
                }
            }
            isRecording = false
        }
    }

    fun cancelRecording() {
        audioUri?.let {
            val file = it.path?.let { it1 -> File(it1) }
            if (file != null) {
                if (file.exists()) {
                    file.delete()
                }
            }
        }
        audioUri = null
    }
    Box(
        contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick = { navController.popBackStack() }, modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
        Box(
            contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()
        ) {
            if (audioUri == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                            } else {
                                startRecording(context)
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(100.dp),
                        containerColor = if (isRecording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                            modifier = Modifier.size(50.dp),
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    if (isRecording) {
                        Text(
                            text = "Recording: ${recordingDuration}s",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            } else {
                CustomAudioPlayer(audioUri = audioUri!!)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            cancelRecording()
                        }
                        .padding(16.dp)) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Box(modifier = Modifier
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            audioUri?.let { uri ->
                                MediaVM.uploadAudioFile(context, uri)
                            }
                        }
                        .padding(16.dp)) {
                        Text(
                            "Post",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomAudioPlayer(audioUri: Uri) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var progress by remember { mutableStateOf(0f) }
    var duration by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    LaunchedEffect(audioUri) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(context, audioUri)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration
        } catch (e: Exception) {
            Log.e("CustomAudioPlayer", "Error preparing media player", e)
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                progress = 0f
                mediaPlayer.seekTo(0)
            }
            while (mediaPlayer.isPlaying) {
                progress = mediaPlayer.currentPosition / duration.toFloat()
                delay(100)
            }
        } else {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        IconButton(onClick = {
            isPlaying = !isPlaying
        }) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        Slider(
            value = progress, onValueChange = { newProgress ->
                progress = newProgress
                val newPosition = (newProgress * duration).toInt()
                mediaPlayer.seekTo(newPosition)
            }, modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = "${mediaPlayer.currentPosition / 1000}s / ${duration / 1000}s",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun StatusText(navController: NavController, customerId: Int) {
    val context = LocalContext.current
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    val charLimit = 100
    var showConfirmationDialog by remember { mutableStateOf(false) }
    val submissionResult by MediaVM.textStatusSubmissionResult.collectAsState()

    LaunchedEffect(submissionResult) {
        submissionResult?.let { success ->
            if (success) {
                Toast.makeText(context, "Text status sent successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to send status. Try again.", Toast.LENGTH_SHORT).show()
            }
            MediaVM.resetSubmissionResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .imePadding()
    ) {
        Text(
            text = "New Status",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = textState,
            onValueChange = {
                if (it.text.length <= charLimit) {
                    textState = it
                }
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            placeholder = {
                Text(
                    "What's on your mind in $charLimit characters",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            },
            maxLines = 10,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                cursorColor = MaterialTheme.colorScheme.onPrimary,
                focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${textState.text.length}/$charLimit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Row {
                Box(modifier = Modifier
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        navController.popBackStack()
                    }
                    .padding(16.dp)) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        showConfirmationDialog = true
                    }
                    .padding(16.dp)) {
                    Text(
                        "Post",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
    if (showConfirmationDialog) {
        AlertDialog(onDismissRequest = { showConfirmationDialog = false }, confirmButton = {
            TextButton(onClick = {
                MediaVM.sendTextStatus(textState.text, customerId)
                showConfirmationDialog = false
                navController.popBackStack()
            }) {
                Text(
                    "Yes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }, title = {
            Text(
                "Confirm Post",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }, dismissButton = {
            TextButton(onClick = { showConfirmationDialog = false }) {
                Text(
                    "No",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }, text = {
            Text("All good?")
        })
    }
}