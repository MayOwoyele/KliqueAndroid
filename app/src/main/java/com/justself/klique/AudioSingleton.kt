package com.justself.klique

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File
import java.io.IOException
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.app.ActivityCompat
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.FileOutputStream

object AudioRecorder {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private var audioFile: File? = null

    private const val sampleRateInHz = 44100
    private const val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private const val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startRecording(context: Context) {
        if (isRecording) {
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRateInHz,
            channelConfig,
            audioFormat
        )

        audioFile = File(context.getExternalFilesDir(null), "recording_${System.currentTimeMillis()}.pcm")
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateInHz,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecord", "AudioRecord initialization failed")
            return
        }

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val audioBuffer = ByteArray(bufferSize)
            val outputStream = FileOutputStream(audioFile)

            while (isRecording) {
                val read = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    outputStream.write(audioBuffer, 0, read)
                }
            }

            outputStream.close()
        }
        recordingThread?.start()
    }

    fun stopRecording(context: Context): File? {
        if (!isRecording) {
            return null
        }

        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        recordingThread = null

        return audioFile?.let { pcmFile ->
            val m4aFile = File(context.getExternalFilesDir(null), "recording_${System.currentTimeMillis()}.m4a")
            val conversionSuccess = convertPcmToM4a(pcmFile.absolutePath, m4aFile.absolutePath)
            if (conversionSuccess) {
                m4aFile
            } else {
                null
            }
        }
    }

    private fun convertPcmToM4a(pcmFilePath: String, m4aFilePath: String): Boolean {
        val command = "-y -f s16le -ar 44100 -ac 1 -i $pcmFilePath -c:a aac -ar 44100 -b:a 160k $m4aFilePath"
        val rc = FFmpeg.execute(command)
        return rc == 0
    }
}
@Composable
fun AudioPlayer(
    audioUri: Uri,
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean?
) {
    var isPlaying by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }
    var progress by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        onDispose {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        }
    }
    LaunchedEffect(audioUri) {
        try {
            mediaPlayer.setDataSource(context, audioUri)
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.prepare()
            duration = mediaPlayer.duration
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error preparing media player", e)
        }
    }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            mediaPlayer.start()

            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                progress = 0f
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
        modifier = modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .widthIn(max = 50.dp)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        if (isSelectionMode == false || isSelectionMode == null) {
            IconButton(
                onClick = {
                    isPlaying = !isPlaying
                }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Slider(
            value = progress,
            onValueChange = { newProgress ->
                            progress = newProgress
                val newPosition = (newProgress * duration).toInt()
                mediaPlayer.seekTo(newPosition)
            },
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            enabled = isSelectionMode != true
        )
        Text(
            text = "${mediaPlayer.currentPosition / 1000}s / ${duration / 1000}s",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}