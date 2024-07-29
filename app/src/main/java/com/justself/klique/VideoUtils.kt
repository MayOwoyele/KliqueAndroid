package com.justself.klique

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.VideoView
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.max
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoUtils {
    private const val TAG = "VideoUtils"

    fun getVideoResolution(context: Context, uri: Uri): Pair<Int, Int>? {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val width =
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toInt()
            val height =
                retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
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

    fun downscaleVideo(context: Context, uri: Uri): Uri? {
        Log.d("onTrim", "downscaling called")
        val resolution = getVideoResolution(context, uri)
        if (resolution == null) {
            Log.e(TAG, "No resolution found, cannot downscale.")
            return null
        }

        if (resolution.first <= 480 && resolution.second <= 480) {
            Log.i(TAG, "No downscaling needed as video is within size limits.")
            return uri
        }

        val inputPath = FileUtils.getPath(context, uri)
        if (inputPath == null) {
            Log.e(TAG, "Failed to get path from URI.")
            return null
        }

        val outputFile = File(context.cacheDir, "scaled_video.mp4")
        val outputPath = outputFile.absolutePath
        val scale = if (resolution.first > resolution.second) "480:-2" else "-2:480"

        val command = arrayOf(
            "-i", inputPath,
            "-vf", "scale=$scale",
            "-c:v", "mpeg4",
            "-crf", "28",
            "-preset", "veryfast",
            "-c:a", "aac",
            "-strict", "experimental",
            "-b:a", "192k",
            "-y", outputPath
        )

        Log.i(TAG, "Starting video downscaling. Command: ${command.joinToString(" ")}")

        // Setting up the log callback to capture FFmpeg logs
        Config.enableLogCallback { message ->
            Log.e(TAG, message.text)
        }

        val rc = FFmpeg.execute(command)
        if (rc == Config.RETURN_CODE_SUCCESS) {
            Log.i(TAG, "Video downscaling successful. Output: $outputPath")
            Log.d("onTrim", "Success! $outputPath")
            return Uri.fromFile(outputFile)
        } else {
            Log.e(TAG, "Video downscaling failed. FFmpeg return code: $rc")
            return null
        }
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
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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
    mediaViewModel: MediaViewModel,
    navController: NavController
) {
    val coroutineScope = rememberCoroutineScope()
    var videoDuration by remember { mutableLongStateOf(0L) }
    var startMs by remember { mutableLongStateOf(0L) }
    var endMs by remember { mutableLongStateOf(30000L) } // 30 seconds by default
    val maxTrimDuration = 30000L // 30 seconds
    val minTrimDuration = 1000L // 1 second
    var isPlaying by remember { mutableStateOf(false)}
    val handler = Handler(Looper.getMainLooper())
    val videoView = remember {
        VideoView(appContext).apply {
            setVideoURI(uri)
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

    // Use AndroidView to get the video duration
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
                                // End handle is being dragged to the right, exceeding max duration
                                startMs = newEnd - maxTrimDuration
                                endMs = newEnd
                            } else {
                                // Start handle is being dragged
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
                Text(text = if (isPlaying) "Pause" else "Play", color = MaterialTheme.colorScheme.onPrimary)
            }
            Button(onClick = stopVideo, colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)) {
                Text(text = "Stop", color = onPrimaryColor)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Trim Button
        Button(
            onClick = {
                coroutineScope.launch {
                    launch {
                        mediaViewModel.preparePerformTrimmingAndDownscaling(
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
        // Cancel Button
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

fun performTrimming(context: Context, uri: Uri, startMs: Long, endMs: Long): Uri? {
    val inputPath = FileUtils.getPath(context, uri)

    if (inputPath == null) {
        Log.e("performTrimming", "Failed to get input path")
        return null
    }

    // Check if the input file exists
    val inputFile = File(inputPath)
    if (!inputFile.exists()) {
        Log.e("performTrimming", "Input file does not exist: $inputPath")
        return null
    }

    val outputFile = File(context.cacheDir, "trimmed_video.mp4")
    val outputPath = outputFile.absolutePath

    val command = arrayOf(
        "-i", inputPath,
        "-ss", (startMs / 1000).toString(),
        "-to", (endMs / 1000).toString(),
        "-c:v", "mpeg4",
        "-b:v", "1500k",
        "-c:a", "aac",
        "-strict", "experimental",
        "-b:a", "192k",
        "-y", outputPath
    )

    Log.d("performTrimming", "Executing FFmpeg command: ${command.joinToString(" ")}")

    // Enable FFmpeg logs
    Config.enableLogCallback { logMessage ->
        Log.d("FFmpeg", logMessage.text)
    }

    // Execute the command
    val rc = FFmpeg.execute(command)
    return if (rc == Config.RETURN_CODE_SUCCESS) {
        Log.d("performTrimming", "Trimming successful, outputPath: $outputPath")
        Uri.fromFile(outputFile)
    } else {
        Log.e("performTrimming", "Trimming failed with return code: $rc")
        Log.e("performTrimming", "Error: ${Config.getLastCommandOutput()}")
        null
    }
}