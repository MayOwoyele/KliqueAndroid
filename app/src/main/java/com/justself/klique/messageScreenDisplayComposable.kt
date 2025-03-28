package com.justself.klique

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DisplayImage(
    mediaUri: String?,
    shape: Shape,
    onLongPressLambda: () -> Unit,
    onTapLambda: () -> Unit
) {
    mediaUri?.let {
        val context = LocalContext.current
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }

        LaunchedEffect(mediaUri) {
            try {
                withContext(Dispatchers.IO) {
                    val uri = Uri.parse(mediaUri)
                    val decodedBitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, uri)
                    )
                    bitmap = decodedBitmap
                }
            } catch (e: Exception) {
                Log.e("ImageDecoder", "Error decoding image: ${e.message}", e)
            }
        }
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .sizeIn(maxWidth = 250.dp, maxHeight = 200.dp)
                    .aspectRatio(
                        bmp.width.toFloat() / bmp.height.toFloat(),
                        matchHeightConstraintsFirst = true
                    )
                    .clip(shape)
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
            text = "Issues displaying the image",
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun DisplayVideo(
    mediaUri: String?,
    shape: Shape,
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
                        onTap = {
                            Log.d("isSelectionMode", "Video Selected $isSelectionMode")
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
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } ?: Text(text = "Issues displaying the video", color = MaterialTheme.colorScheme.onPrimary)
}

@Composable
fun DisplayAudio(
    mediaUri: String?,
    onLongPressLambda: () -> Unit,
    isSelectionMode: Boolean,
    onTapLambda: () -> Unit
) {
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
                },
            isSelectionMode = isSelectionMode
        )
    } ?: Text(
        text = "Issues displaying the audio",
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
                    .clickable(onClick = {
                        if (!isSelectionMode) {
                            onJoinClick()
                        }
                    })
            )
        }
    }
}

@Composable
fun DisplayGistCreator(
    gist: String?,
    shape: Shape,
    onLongPressLambda: () -> Unit,
    isSelectionMode: Boolean,
    onTapLambda: () -> Unit,
    onJoinClick: () -> Unit,
    iSentTheMessage: Boolean
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
            if (gist != null) {
                Text(
                    text = "Create gist with me message: $gist",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            if (!iSentTheMessage) {
                Text(
                    text = "Start gist",
                    style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable(onClick = {
                            if (!isSelectionMode) {
                                onJoinClick()
                            }
                        })
                )
            }
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
    // Pass isSelectionMode so the annotated string is rebuilt accordingly.
    val annotatedString = createAnnotatedString(messageText, isSelectionMode)
    val defaultTextStyle = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.background,
        textDecoration = TextDecoration.None
    )
    val clipboardManager = LocalClipboardManager.current

    var layoutResult: TextLayoutResult? = remember { null }

    Text(
        text = annotatedString,
        style = defaultTextStyle,
        modifier = Modifier
            // Adding isSelectionMode as a key to force re-creation of the pointerInput block when it changes.
            .pointerInput(isSelectionMode) {
                detectTapGestures(
                    onLongPress = { offset ->
                        layoutResult?.let { textLayoutResult ->
                            val position = textLayoutResult.getOffsetForPosition(offset)
                            if (isSelectionMode) {
                                onLongPressLambda()
                            } else {
                                annotatedString.getStringAnnotations("PHONE", position, position)
                                    .firstOrNull()?.let { annotation ->
                                        val phoneNumber = annotation.item.filter { it.isDigit() || it == '+' }
                                        clipboardManager.setText(AnnotatedString(phoneNumber))
                                        Toast.makeText(context, "Copied phone number", Toast.LENGTH_SHORT).show()
                                    } ?: run {
                                    onLongPressLambda()
                                }
                            }
                        }
                    },
                    onTap = { offset ->
                        layoutResult?.let { textLayoutResult ->
                            val position = textLayoutResult.getOffsetForPosition(offset)
                            if (isSelectionMode) {
                                onTapLambda()
                            } else {
                                annotatedString.getStringAnnotations("URL", position, position)
                                    .firstOrNull()?.let { annotation ->
                                        val url = if (annotation.item.startsWith("http")) {
                                            annotation.item
                                        } else {
                                            "https://${annotation.item}"
                                        }
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                "Open link with"
                                            )
                                        )
                                    }
                                    ?: annotatedString.getStringAnnotations("PHONE", position, position)
                                        .firstOrNull()?.let { annotation ->
                                            val phoneNumber = annotation.item.filter { it.isDigit() || it == '+' }
                                            val intent = Intent(
                                                Intent.ACTION_DIAL,
                                                Uri.parse("tel:$phoneNumber")
                                            )
                                            context.startActivity(intent)
                                        }
                                    ?: run {
                                        onTapLambda()
                                    }
                            }
                        }
                    }
                )
            },
        onTextLayout = { layoutResult = it }
    )
}

@Composable
fun createAnnotatedString(text: String, isSelectionMode: Boolean): AnnotatedString {
    if (isSelectionMode) {
        // Just plain text when selection mode is active.
        return AnnotatedString(text)
    }

    val urlRegex = Regex("((https?://)?(www\\.)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(?:/[a-zA-Z0-9./?=_-]*)?)")
    val phoneRegex = Regex("""\d+""")

    return buildAnnotatedString {
        append(text)
        val urlMatches = urlRegex.findAll(text)
        urlMatches.forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            addStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.secondary),
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
        val phoneMatches = phoneRegex.findAll(text)
        phoneMatches.forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            addStyle(
                style = SpanStyle(color = MaterialTheme.colorScheme.secondary),
                start = start,
                end = end
            )
            addStringAnnotation(
                tag = "PHONE",
                annotation = match.value,
                start = start,
                end = end
            )
        }
    }
}