package com.justself.klique

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DisplayImage(mediaUri: String?, shape: Shape, navController: NavController, mediaViewModel: MediaViewModel) {
    mediaUri?.let {
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(mediaUri) {
            withContext(Dispatchers.IO) {
                val uri = Uri.parse(it)
                val decodedBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                bitmap = decodedBitmap
            }
        }

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .height(200.dp)
                    .clip(shape)
                    .clickable {
                        mediaViewModel.setBitmap(it)
                        navController.navigate("fullScreenImage")
                    }
            )
        } ?: Text(
            text = "Image not available",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun DisplayVideo(mediaUri: String?, shape: Shape, navController: NavController) {
    mediaUri?.let { videoUriString ->
        val context = LocalContext.current
        var thumbnail by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(mediaUri) {
            withContext(Dispatchers.IO) {
                val videoUri = Uri.parse(videoUriString)
                val decodedThumbnail = VideoUtils.getVideoThumbnail(context, videoUri)
                thumbnail = decodedThumbnail
            }
        }

        val aspectRatio = thumbnail?.let { it.width.toFloat() / it.height.toFloat() } ?: 1f

        Box(
            modifier = Modifier
                .height(200.dp)
                .wrapContentWidth()
                .aspectRatio(aspectRatio)
                .clip(shape)
                .clickable {
                    navController.navigate("fullScreenVideo/${Uri.encode(videoUriString)}")
                }
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Video not available",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun DisplayAudio(mediaUri: String?, context: Context) {
    mediaUri?.let { audioUriString ->
        val audioUri = Uri.parse(audioUriString)
        AudioPlayer(
            audioUri = audioUri,
            modifier = Modifier.widthIn(min = 300.dp, max = 1000.dp)
        )
    } ?: Text(
        text = "Audio not available",
        color = MaterialTheme.colorScheme.onPrimary
    )
}