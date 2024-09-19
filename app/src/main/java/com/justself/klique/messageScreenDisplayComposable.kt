package com.justself.klique

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun DisplayImage(
    mediaUri: String?,
    shape: Shape,
    navController: NavController,
    mediaViewModel: MediaViewModel,
    onLongPressLambda: () -> Unit,
    isSelectionMode: Boolean,
    onTapLambda: () -> Unit
) {
    mediaUri?.let {
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(mediaUri) {
            withContext(Dispatchers.IO) {
                val uri = Uri.parse(it)
                val decodedBitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        context.contentResolver,
                        uri
                    )
                )
                bitmap = decodedBitmap
            }
        }

        bitmap?.let { bmp ->
            Log.d("isSelectionMode", "isSelectionModeImageLogger if Bitmap: $isSelectionMode")
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .height(200.dp)
                    .clip(shape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { onLongPressLambda() },
                            onTap = {Log.d("isSelectionMode", "Image Selected $isSelectionMode")
                                onTapLambda()
                            }
                        )
                    }
            )
        } ?: Text(
            text = "Image not available",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun DisplayVideo(
    mediaUri: String?,
    shape: Shape,
    navController: NavController,
    onLongPressLambda: () -> Unit,
    isSelectionMode: Boolean,
    onTapLambda: () -> Unit
) {
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
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPressLambda() },
                        onTap = {Log.d("isSelectionMode", "Video Selected $isSelectionMode")
                            onTapLambda()
                        }
                    )
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
fun DisplayAudio(mediaUri: String?, context: Context, onLongPressLambda: () -> Unit, isSelectionMode: Boolean, onTapLambda: () -> Unit) {
    mediaUri?.let { audioUriString ->
        val audioUri = Uri.parse(audioUriString)
        AudioPlayer(
            audioUri = audioUri,
            modifier = Modifier
                .widthIn(min = 300.dp, max = 1000.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPressLambda() },
                        onTap = {
                            onTapLambda()
                        }
                    )
                }
        )
    } ?: Text(
        text = "Audio not available",
        color = MaterialTheme.colorScheme.onPrimary
    )
}
@Composable
fun DisplayGistInvite(
    topic: String?,
    shape: Shape,
    onLongPressLambda: () -> Unit,
    isSelectionMode: Boolean,
    onTapLambda: () -> Unit,
    onJoinClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPressLambda() },
                    onTap = { onTapLambda() }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background, shape)
                .padding(8.dp)
        ) {
            if (topic != null) {
                Text(
                    text = "Gist Invite: $topic",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Text(
                text = "Join",
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary), // Pink color
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(onClick = { onJoinClick() })
            )
        }
    }
}
@Composable
fun ClickableMessageText(
    messageText: String,
    isSelectionMode: Boolean,
    onLongPressLambda: () -> Unit,
    onTapLambda: () -> Unit
) {
    val context = LocalContext.current
    val annotatedString = createAnnotatedString(messageText)
    val defaultTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onBackground,
        textDecoration = TextDecoration.None
    )

    var layoutResult: TextLayoutResult? = remember { null }

    Text(
        text = annotatedString,
        style = defaultTextStyle,
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    // Trigger long press (selection mode)
                    onLongPressLambda()
                    Log.d("Websocket", "Long press detected")
                },
                onTap = { offset ->
                    layoutResult?.let { textLayoutResult ->
                        // Convert the Offset (tap position) to a character index
                        val position = textLayoutResult.getOffsetForPosition(offset)
                        if (isSelectionMode) {
                            onTapLambda()
                        } else {
                            annotatedString.getStringAnnotations("URL", position, position)
                                .firstOrNull()?.let { annotation ->
                                    val url = if (annotation.item.startsWith("https")) {
                                        annotation.item
                                    } else {
                                        "https://${annotation.item}" // Ensure valid HTTPS URL
                                    }
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(Intent.createChooser(intent, "Open link with"))
                                } ?: run {
                                // Handle non-URL tap
                                onTapLambda()
                            }
                        }
                    }
                }
            )
        },
        onTextLayout = { layoutResult = it } // Capture the TextLayoutResult
    )
}
@Composable
fun createAnnotatedString(text: String): AnnotatedString {
    val urlRegex = Regex("(https://[a-zA-Z0-9./?=_-]+|[a-zA-Z0-9_-]+\\.com)")
    return buildAnnotatedString {
        append(text)
        val matches = urlRegex.findAll(text)
        matches.forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1

            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.secondary
                ),
                start = start,
                end = end
            )
            addStringAnnotation(
                tag = "URL",
                annotation = match.value,
                start = start,
                end = end
            )
        }
    }
}