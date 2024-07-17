package com.justself.klique

import android.net.Uri
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

    val videoView = remember {
        VideoView(context).apply {
            setVideoURI(Uri.parse(videoUri))
        }
    }

    // Variables to hold the video duration and current position
    var duration by remember { mutableStateOf(0) }
    var position by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        // Set up the video listener to get the duration when the video is prepared
        videoView.setOnPreparedListener {
            duration = it.duration
        }
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
            modifier = Modifier.fillMaxSize()
        )

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
                            } else {
                                videoView.start()
                            }
                        },
                    ) {
                        Icon(
                            imageVector = if (videoView.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (videoView.isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Seekbar
                    LaunchedEffect(videoView) {
                        while (true) {
                            if (videoView.isPlaying) {
                                position = videoView.currentPosition
                            }
                            delay(100)
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