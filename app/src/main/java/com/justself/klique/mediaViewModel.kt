package com.justself.klique

import ImageUtils
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
    private val _homeScreenUri = MutableLiveData<Uri?>()
    val homeScreenUri: LiveData<Uri?> get() = _homeScreenUri

    private val _messageScreenUri = MutableLiveData<Uri?>()
    val messageScreenUri: LiveData<Uri?> get() = _messageScreenUri
    private val _croppedBitmap = MutableLiveData<Bitmap?>()
    val croppedBitmap: LiveData<Bitmap?> get() = _croppedBitmap

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
        context: Context, uri: Uri, startMs: Long, endMs: Long, sourceScreen: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("Video Status", "This function called")
            performTrimmingAndDownscaling(context, uri, startMs, endMs, sourceScreen)
        }
    }

    private suspend fun performTrimmingAndDownscaling(
        context: Context, uri: Uri, startMs: Long, endMs: Long, sourceScreen: String
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
                "VideoStatus" -> sendStatusVideo(result, context)
            }
        } else {
            Log.e("onTrim", "Result is null, not posting to LiveData")
        }
    }

    fun clearUris() {
        _homeScreenUri.postValue(null)
        _messageScreenUri.postValue(null)
    }

    private fun sendStatusVideo(videoUri: Uri?, context: Context) {
        videoUri?.let { uri ->
            Log.d("Video Status", "Video Uri: $videoUri")
            val videoBytes = FileUtils.loadFileAsByteArray(context, uri)
            videoBytes?.let {
                val type = "statusVideo"
                val typeBytes = type.toByteArray(Charsets.UTF_8)

                val outputStream = ByteArrayOutputStream()
                outputStream.write(ByteBuffer.allocate(4).putInt(typeBytes.size).array())
                outputStream.write(typeBytes)
                outputStream.write(it)

                val message = outputStream.toByteArray()
                WebSocketManager.sendBinary(message)
            }
        }
    }

    fun uploadCroppedImage(context: Context, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val byteArray = ImageUtils.processImageToByteArray(context = context, inputBitmap = bitmap)
                val type = "statusImage"
                val typeBytes = type.toByteArray(Charsets.UTF_8)

                val outputStream = ByteArrayOutputStream()
                outputStream.write(ByteBuffer.allocate(4).putInt(typeBytes.size).array())
                outputStream.write(typeBytes)
                outputStream.write(byteArray)

                val binaryImageToUpload = outputStream.toByteArray()
                WebSocketManager.sendBinary(binaryImageToUpload)
            } catch (e: IOException) {
                Log.e("UploadError", "Failed to upload image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Failed to upload image. Please try again.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    fun uploadAudioFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val byteArray = FileUtils.loadFileAsByteArray(context, uri)
                if (byteArray != null) {
                    val type = "statusAudioFile"
                    val typeBytes = type.toByteArray(Charsets.UTF_8)

                    val outputStream = ByteArrayOutputStream()
                    outputStream.write(ByteBuffer.allocate(4).putInt(typeBytes.size).array())
                    outputStream.write(typeBytes)
                    outputStream.write(byteArray)

                    val binaryAudioToUpload = outputStream.toByteArray()
                    WebSocketManager.sendBinary(binaryAudioToUpload)
                }
            } catch (e: IOException) {
                Log.e("UploadError", "Failed to upload audio file", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context, "Failed to upload audio file. Please try again.", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    fun sendTextStatus(text: String){
    }
    fun setCroppedBitmap(bitmap: Bitmap) {
        _croppedBitmap.value = bitmap
        Log.d("Bitmap", "${_croppedBitmap.value}")
    }
    fun clearCroppedBitmap(){
        _croppedBitmap.value = null
    }
}

class MediaViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST") return MediaViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}