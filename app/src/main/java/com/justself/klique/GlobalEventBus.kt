package com.justself.klique

import android.content.Context
import android.util.Log
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.gists.data.models.GistModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

object GlobalEventBus {
    private val _snackBarEvent = MutableSharedFlow<SnackBarMessageData>(replay = 0)
    val snackBarEvent = _snackBarEvent.asSharedFlow()
    private val _unreadMessageCount = MutableStateFlow(3)
    val unreadMessageCount = _unreadMessageCount.asStateFlow()

    suspend fun sendSnackBarMessage(messageData: SnackBarMessageData) {
        _snackBarEvent.emit(messageData)
    }

    fun updateUnreadMessageCount(count: Int) {
        Log.d("GlobalEventBus", "updateUnreadMessageCount called with $count")
        _unreadMessageCount.value = count
    }

    /**Cached GistMedia paths**/
    private val _cachedMediaPaths = MutableStateFlow<Map<String, MediaPaths>>(emptyMap())
    val cachedMediaPaths: StateFlow<Map<String, MediaPaths>> = _cachedMediaPaths

    fun updateMediaPaths(gistId: String, paths: MediaPaths) {
        _cachedMediaPaths.value = _cachedMediaPaths.value.toMutableMap().apply {
            put(gistId, paths)
        }
    }

    fun fetchGistBackground(gistModels: List<GistModel>) {
        for (gist in gistModels) {
            if (gist.postImage != null) {
                Log.d("BackgroundCaller", "fetchGistBackground called with ${gist.postImage}")
                cacheMediaForGist(appContext, gist.gistId, gist.postImage, GistBackMedia.IMAGE)
            }
            if (gist.postVideo != null) {
                cacheMediaForGist(appContext, gist.gistId, gist.postVideo, GistBackMedia.VIDEO )
            }
        }
    }
}

data class SnackBarMessageData(
    val name: String,
    val message: String,
    val enemyId: Int
)

data class MediaPaths(
    val postImage: String? = null,
    val postVideo: String? = null
)

enum class GistBackMedia {
    IMAGE,
    VIDEO
}

fun cacheMediaForGist(
    context: Context,
    gistId: String,
    remoteUrl: String,
    mediaType: GistBackMedia
) {
    Log.d("BackgroundCaller", "cacheMediaForGist called with $mediaType")
    val cacheDir = File(context.cacheDir, "gist_media").apply { if (!exists()) mkdirs() }
    val extension = when (mediaType) {
        GistBackMedia.IMAGE -> "jpg"
        GistBackMedia.VIDEO -> "mp4"
    }
    val fileName =
        "gist_${gistId}_${if (mediaType == GistBackMedia.IMAGE) "image" else "video"}.$extension"
    val file = File(cacheDir, fileName)

    if (file.exists()) {
        val localPath = file.absolutePath
        Log.d(
            "BackgroundCaller",
            "Local path: $localPath: GistBackMedia ${if (mediaType == GistBackMedia.IMAGE) "image" else "video"}"
        )
        if (mediaType == GistBackMedia.IMAGE) {
            GlobalEventBus.updateMediaPaths(gistId, MediaPaths(postImage = localPath))
        } else {
            GlobalEventBus.updateMediaPaths(gistId, MediaPaths(postVideo = localPath))
        }
    }

    downloadFile(NetworkUtils.fixLocalHostUrl(remoteUrl), file, MediaVM.scope) { success ->
        Log.d(
            "BackgroundCaller",
            "GistBackMedia ${if (mediaType == GistBackMedia.IMAGE) "image" else "video"}"
        )
        if (success && file.exists()) {
            Log.d("BackgroundCaller", "Success!")
            val localPath = file.absolutePath
            if (mediaType == GistBackMedia.IMAGE) {
                GlobalEventBus.updateMediaPaths(gistId, MediaPaths(postImage = localPath))
            } else {
                GlobalEventBus.updateMediaPaths(gistId, MediaPaths(postVideo = localPath))
            }
            Log.d("BackgroundCaller", "File length: ${file.length()} bytes")
        } else {
            Log.d("BackgroundCaller", "Failed!")
        }
    }
}

fun downloadFile(
    remoteUrl: String,
    destFile: File,
    scope: CoroutineScope,
    onComplete: (Boolean) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        val client = NetworkClient.okHttpClient
        val maxAttempts = 3
        var attempt = 0
        var success = false
        var delayMillis = 1000L
        while (attempt < maxAttempts && !success) {
            attempt++
            try {
                val request = Request.Builder().url(remoteUrl).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    destFile.outputStream().use { output ->
                        response.body?.byteStream()?.copyTo(output)
                    }
                    success = true
                    Log.d("BackgroundCaller", "Download successful on attempt $attempt")
                } else {
                    Log.d("BackgroundCaller", "Attempt $attempt failed: $response")
                }
            } catch (e: Exception) {
                Log.d("BackgroundCaller", "Attempt $attempt exception: $e")
            }
            if (!success && attempt < maxAttempts) {
                delay(delayMillis)
                delayMillis *= 2
            }
        }
        onComplete(success)
    }
}
object NetworkClient {
    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}