package com.justself.klique

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
    private val _homeScreenUri = MutableLiveData<Uri?>()
    val homeScreenUri: LiveData<Uri?> get() = _homeScreenUri

    private val _messageScreenUri = MutableLiveData<Uri?>()
    val messageScreenUri: LiveData<Uri?> get() = _messageScreenUri

    init {
        valueOfHomeScreenUri()
    }

    private fun valueOfHomeScreenUri() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                Log.d("HomeScreenUri", "current value $_homeScreenUri")
            }
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        _bitmap.value = bitmap
    }

    fun setBitmapFromUri(uri: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = loadBitmapFromUri(uri, context)
            withContext(Dispatchers.Main) {
                _bitmap.value = bitmap
            }
        }
    }

    private fun loadBitmapFromUri(uri: String, context: Context): Bitmap? {
        return try {
            val parsedUri = Uri.parse(uri)
            val source = ImageDecoder.createSource(context.contentResolver, parsedUri)
            ImageDecoder.decodeBitmap(source)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearBitmap() {
        _bitmap.postValue(null)
    }

    fun preparePerformTrimmingAndDownscaling(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        sourceScreen: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            performTrimmingAndDownscaling(context, uri, startMs, endMs, sourceScreen)
        }
    }

    suspend fun performTrimmingAndDownscaling(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long,
        sourceScreen: String
    ) {
        val result = withContext(Dispatchers.IO) {
            val trimmedUri = performTrimming(context, uri, startMs, endMs)
            Log.d("onTrim", "Trimming result: $trimmedUri")

            // Ensure the trimmed file exists before proceeding
            if (trimmedUri != null) {
                val file = trimmedUri.path?.let { File(it) }
                if (file != null) {
                    if (file.exists()) {
                    } else {
                        return@withContext null
                    }
                }
            }
            val downscaledUri = trimmedUri?.let {
                val downscaled = VideoUtils.downscaleVideo(context, it)
                downscaled
            }
            downscaledUri
        }
        // Ensure result is not null before posting it
        if (result != null) {
            when (sourceScreen) {
                "HomeScreen" -> _homeScreenUri.postValue(result)
                "MessageScreen" -> _messageScreenUri.postValue(result)
            }
        } else {
            Log.e("onTrim", "Result is null, not posting to LiveData")
        }
    }
    fun clearUris(){
        _homeScreenUri.postValue(null)
        _messageScreenUri.postValue(null)
    }

}

class MediaViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}