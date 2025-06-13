package com.justself.klique

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.widget.VideoView
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resumeWithException
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultAssetLoaderFactory
import androidx.media3.transformer.DefaultDecoderFactory
import kotlin.coroutines.resume

object VideoUtils {
    private const val TAG = "VideoUtils"

    private fun getVideoResolution(context: Context, uri: Uri): Pair<Int, Int>? {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val width =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toInt()
            val height =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toInt()
            return if (width != null && height != null) {
                Log.i(TAG, "Video resolution retrieved: Width=$width, Height=$height")
                Pair(width, height)
            } else {
                Log.w(TAG, "Failed to retrieve video resolution.")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving video metadata: ${e.message}", e)
            return null
        } finally {
            retriever.release()
        }
    }

    /**
     * Down-scale a video so that its *longest* edge is `maxDimension` pixels
     * and write H-264/AAC MP4 into cache.  Suspends until the export is
     * **actually finished** or throws on failure.
     *
     * Requires:
     *   implementation("androidx.media3:media3-transformer:1.3.1")  // or newer
     *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.8.0")
     */
    @OptIn(UnstableApi::class)
    suspend fun downscaleVideo(
        context: Context,
        inputUri: Uri,
        maxDimension: Int = 480
    ): Uri = withContext(Dispatchers.Main) {
        // 0️⃣ Probe the source
        Logger.d("onTrim", "Downscaling video")
        val retriever = MediaMetadataRetriever()
        val (srcW, srcH, rotation) = try {
            retriever.setDataSource(context, inputUri)
            val w =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
                    ?: error("No width")
            val h =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
                    ?: error("No height")
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toInt()
                ?: 0
            Triple(w, h, rot)
        } finally {
            retriever.release()
        }
        Logger.d("onTrim", "Source video resolution: $srcW x $srcH, rotation: $rotation")
        val (effectiveW, effectiveH) =
            if (rotation == 90 || rotation == 270) srcH to srcW else srcW to srcH
        if (maxOf(effectiveW, effectiveH) <= maxDimension) return@withContext inputUri

        val scale = maxDimension.toFloat() / maxOf(effectiveW, effectiveH)
        val scaleEffect = ScaleAndRotateTransformation.Builder()
            .setScale(scale, scale)
            .build()
        val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setEffects(Effects(emptyList(), listOf(scaleEffect)))
            .build()

        val outputFile = withContext(Dispatchers.IO) {
            File.createTempFile("scaled_", ".mp4", context.cacheDir)
        }
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(
                VideoEncoderSettings.Builder().setBitrate(3_000_000).build()
            )
            .build()
        Logger.d("onTrim", "Encoder factory: $encoderFactory")

        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .setEncoderFactory(encoderFactory)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(
                        composition: Composition,
                        result: ExportResult
                    ) {
                        Logger.d("onTrim", "Transformer completed")
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException
                    ) {
                        Logger.d("onTrim", "Transformer error codes: ${exception.errorCode}, ${exception.errorCodeName}")
                        if (cont.isActive) cont.resumeWithException(exception)
                    }

                    override fun onFallbackApplied(
                        composition: Composition,
                        originalRequest: TransformationRequest,
                        fallbackRequest: TransformationRequest
                    ) {
                        Logger.d("onTrim", "Transformer fallback applied")
                        // you can log or ignore—doesn't affect resume
                    }
                })
                .build()

            transformer.start(editedItem, outputFile.absolutePath)
            cont.invokeOnCancellation { transformer.cancel() }
        }
        Logger.d("onTrim", "Transformer finished")
        // 5️⃣ Return the new Uri
        Uri.fromFile(outputFile)
    }

    fun getVideoThumbnail(context: Context, videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }
}

fun createPlaceholderImage(width: Int, height: Int, backgroundColor: Int, textColor: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.color = textColor
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = 50f
    canvas.drawText("Video", width / 2f, height / 2f, paint)
    return bitmap
}

@Composable
fun VideoTrimmingScreen(
    appContext: Context,
    uri: Uri,
    onCancel: () -> Unit,
    sourceScreen: String,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    var videoDuration by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(30000L) } // 30 seconds by default
    val maxTrimDuration = 30000L // 30 seconds
    val minTrimDuration = 1000L
    var isPlaying by remember { mutableStateOf(false) }
    val handler = Handler(Looper.getMainLooper())
    val decodedPath = Uri.decode(uri.toString())
    val videoView = remember {
        VideoView(appContext).apply {
            setVideoURI(decodedPath.toUri())
            setOnErrorListener { mp, what, extra ->
                Log.e("VideoTrimmingScreen", "Error playing video: what=$what, extra=$extra")
                Logger.d("VideoTrimmingScreen", "URI: $uri")
                true
            }
            setOnPreparedListener { mp ->
                videoDuration = mp.duration.toLong()
                endMs = minOf(
                    maxTrimDuration,
                    videoDuration
                ) // Adjust endMs based on video duration
                seekTo(startMs.toInt())
                start()
                isPlaying = true
            }
            setOnCompletionListener {
                isPlaying = false
                seekTo(startMs.toInt())
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
        }
    }
    // Playback control functions
    val playVideo = {
        if (!isPlaying) {
            videoView.start()
            isPlaying = true

            // Ensure video stops at endMs
            handler.post(object : Runnable {
                override fun run() {
                    if (videoView.currentPosition >= endMs.toInt()) {
                        videoView.pause()
                        isPlaying = false
                        videoView.seekTo(startMs.toInt())
                    } else {
                        handler.postDelayed(this, 100)
                    }
                }
            })
        }
    }

    val pauseVideo = {
        if (isPlaying) {
            videoView.pause()
            isPlaying = false
        }
    }

    val stopVideo = {
        videoView.pause()
        videoView.seekTo(startMs.toInt())
        isPlaying = false
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val backgroundColor = MaterialTheme.colorScheme.background
    val typography = MaterialTheme.typography

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Trim Video",
            style = typography.displayLarge,
            color = onPrimaryColor
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Placeholder for Video Player
        AndroidView(
            factory = { videoView },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .background(Color.Black)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Single Slider for start and end time
        if (videoDuration >= minTrimDuration) {
            Text(
                text = "Start: ${startMs / 1000}s | End: ${endMs / 1000}s",
                style = typography.bodyLarge,
                color = onPrimaryColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            RangeSlider(
                value = startMs.toFloat()..endMs.toFloat(),
                onValueChange = { values ->
                    val newStart = values.start.toLong()
                    val newEnd = values.endInclusive.toLong()
                    when {
                        // If the new range exceeds maxTrimDuration
                        newEnd - newStart > maxTrimDuration -> {
                            if (newEnd > endMs) {
                                startMs = newEnd - maxTrimDuration
                                endMs = newEnd
                            } else {
                                endMs = newStart + maxTrimDuration
                                startMs = newStart
                            }
                        }

                        // If the new range is less than minTrimDuration
                        newEnd - newStart < minTrimDuration -> {
                            if (newEnd != endMs) {
                                // End handle is being dragged
                                startMs = newEnd - minTrimDuration
                                endMs = newEnd
                            } else {
                                // Start handle is being dragged
                                endMs = newStart + minTrimDuration
                                startMs = newStart
                            }
                        }

                        // If the new range is within valid range
                        else -> {
                            startMs = newStart
                            endMs = newEnd
                        }
                    }
                },
                valueRange = 0f..videoDuration.toFloat(),
                steps = (videoDuration / 1000).toInt(),
                modifier = Modifier.padding(horizontal = 16.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White
                )
            )
        } else {
            Text(
                text = "Video too short to trim",
                style = typography.bodyLarge,
                color = onPrimaryColor
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (isPlaying) {
                        pauseVideo()
                    } else {
                        playVideo()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
            ) {
                Text(
                    text = if (isPlaying) "Pause" else "Play",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Button(
                onClick = stopVideo,
                colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
            ) {
                Text(text = "Stop", color = onPrimaryColor)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    launch {
                        MediaVM.preparePerformTrimmingAndDownscaling(
                            appContext,
                            uri,
                            startMs,
                            endMs,
                            sourceScreen
                        )
                    }
                    //onTrimComplete(null, sourceScreen)
                    navController.popBackStack()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
            enabled = videoDuration >= minTrimDuration
        ) {
            Text(text = "Trim and Send", color = onPrimaryColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onCancel() },
            colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
        ) {
            Text(text = "Cancel", color = onPrimaryColor)
        }
    }
}
/*
suspend fun performTrimmingAndDownscaling(
    context: Context,
    uri: Uri,
    startMs: Long,
    endMs: Long
): Uri? {
    return withContext(Dispatchers.IO) {
        val trimmedUri = performTrimming(context, uri, startMs, endMs)
        trimmedUri?.let {
            VideoUtils.downscaleVideo(context, it)
        }
    }
}

 */

@OptIn(UnstableApi::class)
suspend fun performTrimming(
    context: Context,
    inputUri: Uri,
    startMs: Long,
    endMs: Long
): Uri = withContext(Dispatchers.Main) {
    require(endMs > startMs) { "endMs must be after startMs" }

    // 1️⃣ Build a clipped MediaItem
    val clippingConfig = MediaItem.ClippingConfiguration.Builder()
        .setStartPositionMs(startMs)
        .setEndPositionMs(endMs)
        .build()
    val raw = inputUri.toString()                               // "file%3A%2F%2F%2Fdata%2F…"
    val decoded = Uri.decode(raw)                               // "file:///data/user/0/…"
    val realUri = decoded.toUri()
    val mediaItem = MediaItem.Builder()
        .setUri(realUri)
        .setClippingConfiguration(clippingConfig)
        .build()
    val editedItem = EditedMediaItem.Builder(mediaItem).build()
    Logger.d("onTrim", "Input URI: $inputUri, scheme=${inputUri.scheme}, path=${inputUri.path}")

    val outputFile = withContext(Dispatchers.IO) {
        File.createTempFile("trimmed_", ".mp4", context.cacheDir)
    }
    val decoderFactory = DefaultDecoderFactory.Builder(context)
        .setEnableDecoderFallback(true)
        .setListener { codecName, codecInitializationExceptions ->
            Log.d("DecoderFactory", "Codec initialized: $codecName")
            if (codecInitializationExceptions.isNotEmpty()) {
                Log.w("DecoderFactory", "Issues: ${codecInitializationExceptions.joinToString()}")
            }
        }
        .build()

    val mediaSourceFactory = DefaultMediaSourceFactory(context)
    val assetLoaderFactory = DefaultAssetLoaderFactory(
        context,
        decoderFactory,
        Clock.DEFAULT,
        mediaSourceFactory,
        DataSourceBitmapLoader(context)
    )
    val resultUri: Uri = suspendCancellableCoroutine { cont ->
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setAssetLoaderFactory(assetLoaderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(
                    composition: Composition,
                    exportResult: ExportResult
                ) {
                    Logger.d("onTrim", "Transformer completed")
                    if (cont.isActive) {
                        cont.resume(Uri.fromFile(outputFile))
                    }
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    val code = exportException.errorCode
                    val codeName = exportException.errorCodeName
                    Logger.d("onTrim", "Transformer error codes: $code, $codeName")
                    if (cont.isActive) {
                        Logger.d("onTrim", "Transformer error: ${exportException.message}")
                        cont.resumeWithException(exportException)
                    } else {
                        Logger.d("onTrim", "Transformer error: Coroutine is not active")
                    }
                }

                override fun onFallbackApplied(
                    composition: Composition,
                    originalRequest: TransformationRequest,
                    fallbackRequest: TransformationRequest
                ) {
                    Logger.d("onTrim", "Transformer fallback applied")
                    // no-op
                }
            })
            .build()

        // start on the Main thread (so it has a Looper to post callbacks into)
        transformer.start(editedItem, outputFile.absolutePath)

        // if the coroutine is cancelled, cancel the transform
        cont.invokeOnCancellation { transformer.cancel() }
    }

    // 4️⃣ Return the completed file’s Uri
    resultUri
}