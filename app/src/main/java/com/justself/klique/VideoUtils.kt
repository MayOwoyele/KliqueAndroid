package com.justself.klique

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import java.io.File

object VideoUtils {
    fun getVideoResolution(context: Context, uri: Uri): Pair<Int, Int>? {
        val retriever = android.media.MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val width = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
        val height = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
        retriever.release()
        return if (width != null && height != null) Pair(width, height) else null
    }

    fun downscaleVideo(context: Context, uri: Uri): Uri? {
        val resolution = getVideoResolution(context, uri) ?: return null
        if (resolution.first <= 480 && resolution.second <= 480) {
            // No downscaling needed
            return uri
        }

        val inputPath = FileUtils.getPath(context, uri) ?: return null
        val outputFile = File(context.cacheDir, "scaled_video.mp4")
        val outputPath = outputFile.absolutePath

        val scale = if (resolution.first > resolution.second) "480:-2" else "-2:480"
        val command = arrayOf(
            "-i", inputPath,
            "-vf", "scale=$scale",
            "-c:v", "libx264",
            "-crf", "28",
            "-preset", "veryfast",
            "-c:a", "aac",
            "-strict", "experimental",
            "-b:a", "192k",
            "-y", outputPath
        )

        val rc = FFmpeg.execute(command)
        return if (rc == Config.RETURN_CODE_SUCCESS) {
            Uri.fromFile(outputFile)
        } else {
            null
        }
    }
}

@Composable
fun VideoTrimmingScreen(
    appContext: Context,
    uri: Uri,
    onTrimComplete: (Uri?) -> Unit,
    onCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var videoDuration by remember { mutableStateOf(0L) }
    var startMs by remember { mutableStateOf(0L) }
    var endMs by remember { mutableStateOf(30000L) } // 30 seconds by default
    val maxTrimDuration = 30000L // 30 seconds
    val minTrimDuration = 1000L // 1 second

    // Use AndroidView to get the video duration
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    videoDuration = mp.duration.toLong()
                    endMs = minOf(maxTrimDuration, videoDuration) // Adjust endMs based on video duration
                }
            }
        },
        update = {}
    )

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
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(uri)
                    start()
                }
            },
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
                    if (newEnd - newStart > maxTrimDuration) {
                        if (newStart != startMs) {
                            startMs = newEnd - maxTrimDuration
                            endMs = newEnd
                        } else {
                            endMs = newStart + maxTrimDuration
                            startMs = newStart
                        }
                    } else if (newEnd - newStart < minTrimDuration) {
                        if (newStart != startMs) {
                            startMs = newEnd - minTrimDuration
                        } else {
                            endMs = newStart + minTrimDuration
                        }
                    } else {
                        startMs = newStart
                        endMs = newEnd
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

        // Trim Button
        Button(
            onClick = {
                coroutineScope.launch {
                    val trimmedUri = performTrimming(appContext, uri, startMs, endMs)
                    onTrimComplete(trimmedUri)
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

suspend fun performTrimming(context: Context, uri: Uri, startMs: Long, endMs: Long): Uri? {
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