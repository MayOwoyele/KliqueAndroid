package com.justself.klique

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun FullScreenVideo(videoUri: String, navController: NavController) {
    var isOverlayVisible by remember { mutableStateOf(true) }
    val context = LocalContext.current
    var isBuffering = remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false)}
    var isPrepared by remember { mutableStateOf(false) }
    var duration by remember { mutableIntStateOf(0) }
    var position by remember { mutableIntStateOf(0) }

    val videoView = remember {
        VideoView(context).apply {
            setVideoURI(Uri.parse(videoUri))
            setOnErrorListener { _, what, extra ->
                // Log error or show a message to the user
                Log.e("VideoView", "Error occurred: what=$what, extra=$extra")
                // Return true if the error was handled
                true
            }
            setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        isBuffering.value = true
                        Log.d("MarketVideoPlayer", "Buffering started")
                    }
                    MediaPlayer.MEDIA_INFO_BUFFERING_END,
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START-> {
                        isBuffering.value = false
                        Log.d("MarketVideoPlayer", "Buffering ended")
                    }
                }
                true
            }
            setOnPreparedListener {
                isPrepared = true
                duration = it.duration
                isBuffering.value = false
                it.start()
                isPlaying = true
            }
            setOnCompletionListener {
                isPlaying = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() },
                indication = null) { isOverlayVisible = !isOverlayVisible }
    ) {
        // VideoView
        AndroidView(
            factory = { videoView },
            modifier = Modifier.fillMaxSize(),
        )
        if (isBuffering.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                )
            }
        }

        // Top overlay with back arrow
        if (isOverlayVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Bottom overlay with play/pause button and slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)) // Semi-transparent black background
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (videoView.isPlaying) {
                                videoView.pause()
                                isPlaying = false
                            } else {
                                videoView.start()
                                isPlaying = true
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Seekbar
                    LaunchedEffect(videoView, isPlaying) {
                        while (true) {
                            delay(500) // Adjust delay as needed for smoother updates
                            if (isPrepared) {
                                val currentPosition = videoView.currentPosition
                                if (isPlaying) {
                                    if (currentPosition != position) {
                                        position = currentPosition // Update the position state
                                        isBuffering.value = false
                                    } else {
                                        isBuffering.value = true // If position hasn't changed, consider it buffering
                                    }
                                } else {
                                    isBuffering.value = false
                                }
                            }
                            Log.d("State variables", "$isPlaying, $isPrepared, $position")
                        }
                    }

                    if (duration > 0) {
                        Slider(
                            value = position.toFloat(),
                            valueRange = 0f..duration.toFloat(),
                            onValueChange = { newValue ->
                                position = newValue.toInt()
                                videoView.seekTo(position)
                            },
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}