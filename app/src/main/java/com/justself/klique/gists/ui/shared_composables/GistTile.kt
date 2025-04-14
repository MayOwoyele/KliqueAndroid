package com.justself.klique.gists.ui.shared_composables

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.GistType
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.NetworkUtils
import com.justself.klique.SessionManager
import com.justself.klique.gists.data.models.GistModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

@Composable
fun GistTile(
    customerId: Int,
    onTap: () -> Unit,
    onHoldClick: (() -> Unit)? = null,
    lastPostList: List<LastGistComments>,
    postImage: String?,
    postVideo: String?,
    gist: GistModel
) {
    val image = gist.image?.let { NetworkUtils.fixLocalHostUrl(it) }
    val frameBitmaps = remember { mutableStateListOf<ImageBitmap>() }
    var currentFrameIndex by remember { mutableIntStateOf(0) }
    val isCacheReady = remember { mutableStateOf(false) }
    LaunchedEffect(postVideo) {
        if (postVideo != null) {
            VideoDecodingLimiter.semaphore.withPermit {
                val retriever = MediaMetadataRetriever()
                try {
                    val context = appContext
                    val decodedVideoPath = Uri.decode(postVideo)
                    val formattedVideoPath = if (decodedVideoPath.startsWith("file://")) {
                        decodedVideoPath
                    } else {
                        "file://$decodedVideoPath"
                    }
                    Log.d("BackgroundCaller", "GistTile: formatted postVideo $formattedVideoPath")

                    val file = File(Uri.parse(formattedVideoPath).path ?: "")
                    if (!file.exists()) {
                        Log.e(
                            "BackgroundCaller",
                            "Video file does not exist at $formattedVideoPath"
                        )
                    }

                    retriever.setDataSource(context, Uri.parse(formattedVideoPath))
                    val durationStr =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    Log.d("BackgroundCaller", "Video durationStr: $durationStr")
                    val duration = durationStr?.toLongOrNull() ?: 0L
                    Log.d("BackgroundCaller", "Video duration: $duration ms")

                    val targetFps = 12
                    val frameIntervalUs = 1000000L / targetFps
                    val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
                    val nativeFps = fpsStr?.toFloatOrNull() ?: 12f
                    Log.d("BackgroundCaller", "Native FPS: $nativeFps")
                    val scaleFactor = nativeFps / targetFps
                    val samplingDurationUs = min(duration * 1000, 5_000_000L)
                    val maxFrames = 60
                    var currentTimeUs = 0L
                    var loadedFrames = 0

                    while (currentTimeUs <= samplingDurationUs && loadedFrames < maxFrames) {
                        val bitmap = withContext(Dispatchers.IO) {
                            retriever.getFrameAtTime(
                                currentTimeUs,
                                MediaMetadataRetriever.OPTION_CLOSEST
                            )
                        }
                        if (bitmap != null) {
                            frameBitmaps.add(bitmap.asImageBitmap())
                            Log.d("BackgroundCaller", "Loaded frame at time $currentTimeUs µs")
                            if (!isCacheReady.value && frameBitmaps.size >= 24) {  // minFramesToSwitch now 24
                                isCacheReady.value = true
                            }
                        } else {
                            Log.e("BackgroundCaller", "Null frame at time $currentTimeUs µs")
                        }
                        val testBitmap = retriever.getFrameAtTime(
                            1_000_000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                        if (testBitmap == null) {
                            Log.e("BackgroundCaller", "Still no frame at 1 sec.")
                        } else {
                            Log.e("BackgroundCaller", "Frame found")
                        }
                        currentTimeUs += (frameIntervalUs * scaleFactor).toLong()
                        loadedFrames++
                    }
                    Log.d("BackgroundCaller", "Preloaded ${frameBitmaps.size} frames")
                    isCacheReady.value = true
                    Log.d("BackgroundCaller", "Preloaded ${frameBitmaps.size} frames")
                    isCacheReady.value = true
                } catch (e: Exception) {
                    Log.e("BackgroundCaller", "Error preloading video frames", e)
                } finally {
                    retriever.release()
                }
            }
        }
    }
    val roundedCornerShape = RoundedCornerShape(20.dp)
    var isDialogVisible by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition()
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val fixedHeight = 310.dp

    Box(
        modifier = Modifier
            .height(fixedHeight)
            .padding(horizontal = 16.dp)
            .clip(roundedCornerShape)
    ) {
        if (!postVideo.isNullOrEmpty()) {
            if (isCacheReady.value && frameBitmaps.isNotEmpty()) {
                LaunchedEffect(isCacheReady.value, frameBitmaps.size) {
                    while (true) {
                        delay(1000L / 24)
                        currentFrameIndex = (currentFrameIndex + 1) % frameBitmaps.size
                    }
                }
                Image(
                    bitmap = frameBitmaps[currentFrameIndex],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (frameBitmaps.isNotEmpty()) {
                Image(
                    bitmap = frameBitmaps.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                EmptyBackground()
            }
        } else if (!postImage.isNullOrEmpty()) {
            val formattedPath =
                if (postImage.startsWith("file://")) postImage else "file://$postImage"
            Log.d("BackgroundCaller", "GistTile: formatted postImage $formattedPath")
            Image(
                painter = rememberAsyncImagePainter(formattedPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            EmptyBackground()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = if (postImage != null) animatedAlpha else 0f))
        )
        val primaryColor = MaterialTheme.colorScheme.primary
        val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
        val highlightWidth = 0.1f
        val animatedHighlight by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f - highlightWidth,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )
        val gradientStops: Array<out Pair<Float, Color>> = arrayOf(
            0f to onPrimaryColor,
            animatedHighlight to onPrimaryColor,
            (animatedHighlight + highlightWidth) to primaryColor,
            1f to onPrimaryColor
        )
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onLongPress = {
                            if (onHoldClick != null && customerId == SessionManager.customerId.value) {
                                isDialogVisible = true
                            }
                        }
                    )
                }
                .drawBehind {
                    val strokeWidthPx = 3.dp.toPx()
                    val halfStroke = strokeWidthPx / 2
                    val adjustedSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            *gradientStops,
                            center = size.center
                        ),
                        size = adjustedSize,
                        style = Stroke(width = strokeWidthPx),
                        topLeft = Offset(halfStroke, halfStroke),
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                }
                .clip(roundedCornerShape),
            color = Color.Transparent
        ) {
            Column {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(150.dp)
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                            .clip(CircleShape.copy(CornerSize(150.dp)))
                    ) {
                        if (image != null) {
                            Image(
                                painter = rememberAsyncImagePainter(image),
                                contentDescription = "",
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                    Column {
//                        Text(
//                            text = gist.topic,
//                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 17.sp),
//                            maxLines = 2,
//                            overflow = TextOverflow.Ellipsis
//                        )
                        Text(
                            text = gist.description,
                            style = MaterialTheme.typography.displayLarge,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Active Spectators: ${gist.activeSpectators}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (gist.gistType == GistType.Private){
                            Box(Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)).padding(5.dp)){
                                Text(
                                    text = "News",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .weight(10f)
                ) {
                    Text("..recent posts", color = MaterialTheme.colorScheme.onPrimary)
                    lastPostList.forEach { comment ->
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(3.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.Gray)
                                .padding(3.dp)
                        ) {
                            Text(
                                text = "${comment.senderName}: ${comment.comment}",
                                color = MaterialTheme.colorScheme.background,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (isDialogVisible) {
                Dialog(onDismissRequest = { isDialogVisible = false }) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Actions", style = MaterialTheme.typography.displayLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                onHoldClick?.invoke()
                                isDialogVisible = false
                            }) {
                                Text("Float Gist")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { isDialogVisible = false }) {
                                Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun EmptyBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}
data class LastGistComments(
    val senderName: String,
    val comment: String,
    val userId: Int
)

object VideoDecodingLimiter {
    val semaphore = Semaphore(1)
}
