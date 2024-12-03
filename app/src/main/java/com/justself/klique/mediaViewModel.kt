package com.justself.klique

import ImageUtils
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

class MediaViewModel(application: Application) : AndroidViewModel(application) {
    private var _textStatusSubmissionResult = MutableStateFlow<Boolean?>(null)
    val textStatusSubmissionResult = _textStatusSubmissionResult.asStateFlow()

    private var _audioStatusSubmissionResult = MutableStateFlow<Boolean?>(null)
    val audioStatusSubmissionResult = _audioStatusSubmissionResult.asStateFlow()

    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
    private val _homeScreenUri = MutableLiveData<Uri?>()
    val homeScreenUri: LiveData<Uri?> get() = _homeScreenUri

    private val _messageScreenUri = MutableLiveData<Uri?>()
    val messageScreenUri: LiveData<Uri?> get() = _messageScreenUri
    private val _croppedBitmap = MutableLiveData<Bitmap?>()
    val croppedBitmap: LiveData<Bitmap?> get() = _croppedBitmap
    val customerId = MutableStateFlow<Int?>(null)
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    fun setCustomerId(value: Int) {
        customerId.value = value
    }

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
        if (!_isUploading.value) {
            _bitmap.postValue(null)
        }
    }
    fun setIsUploading(bool: Boolean){
        _isUploading.value = bool
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
            if (trimmedUri != null) {
                val file = trimmedUri.path?.let { File(it) }
                if (file != null) {
                    if (!file.exists()) {
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
        Log.d("KliqueVideoStatus", SessionManager.customerId.value.toString())
        videoUri?.let { uri ->
            Log.d("Video Status", "Video Uri: $uri")
            val videoBytes = FileUtils.loadFileAsByteArray(context, uri)

            try {
                videoBytes?.let {
                    viewModelScope.launch {
                        try {
                            val userIdField = MultipartField(
                                name = "userId",
                                value = SessionManager.customerId.value.toString()
                            )
                            val videoFileField = MultipartField(
                                name = "file",
                                value = videoBytes,
                                fileName = "video.mp4",
                                mimeType = MimeType.VIDEO_MP4
                            )

                            val response = NetworkUtils.makeMultipartRequest(
                                endpoint = "sendVideoStatus",
                                fields = listOf(userIdField, videoFileField)
                            )
                            Log.d("KliqueVideoStatus", "Video Uri: $uri")
                            if (response.first) {
                                Toast.makeText(context, "Video has successfully been uploaded", Toast.LENGTH_LONG).show()
                            }
                            else {
                                Log.d("KliqueVideo", "${response.second}, ${response.third}")
                                Toast.makeText(context, "Error successfully uploading the video", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("Video Status", "Error uploading video", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Video Status", "Error preparing video file", e)
            }
        }
    }

    fun uploadCroppedImage(context: Context, bitmap: Bitmap, customerId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val byteArray = ImageUtils.processImageToByteArray(context = context, inputBitmap = bitmap)
                val fields = listOf(
                    MultipartField(
                        name = "userId",
                        value = customerId.toString()
                    ),
                    MultipartField(
                        name = "file",
                        value = byteArray,
                        mimeType = MimeType.IMAGE_JPEG,
                        fileName = "cropped_image.jpg"
                    )
                )
                val (isSuccessful, response, responseCode) = NetworkUtils.makeMultipartRequest(
                    endpoint = "sendImageStatus",
                    fields = fields
                )
                if (isSuccessful) {
                    Log.d("UploadSuccess", "Image uploaded successfully: $response")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Image uploaded successfully!", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e("UploadError", "Failed to upload image: $responseCode - $response")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to upload image. Please try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

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
        Log.d("Audio", "Called")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val byteArray = FileUtils.loadFileAsByteArray(context, uri)
                if (byteArray != null) {
                    val userIdField = MultipartField(
                        name = "userId",
                        value = SessionManager.customerId.value.toString()
                    )
                    val audioFileField = MultipartField(
                        name = "file",
                        value = byteArray,
                        fileName = "audio.m4a",
                        mimeType = MimeType.AUDIO_MPEG4
                    )

                    val response = NetworkUtils.makeMultipartRequest(
                        endpoint = "sendAudioStatus",
                        fields = listOf(userIdField, audioFileField)
                    )

                    // Handle the response
                    withContext(Dispatchers.Main) {
                        _audioStatusSubmissionResult.value = response.first
                        if (!response.first) {
                            Toast.makeText(
                                context, "Failed with status code ${response.third}", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
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

    fun sendTextStatus(text: String, customerId: Int) {
        val jsonBody = """
            {
            "userId": $customerId,
            "theText": "$text"
            }
        """.trimIndent()
        try {
            viewModelScope.launch()
            {
                val response = NetworkUtils.makeRequest(
                    "sendTextStatus",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody
                )
                _textStatusSubmissionResult.value = response.first
            }
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    fun setCroppedBitmap(bitmap: Bitmap) {
        val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) // Copy to a compatible config
        } else {
            bitmap
        }
        _croppedBitmap.value = safeBitmap
        Log.d("Bitmap", "${_croppedBitmap.value}")
    }

    fun clearCroppedBitmap() {
        _croppedBitmap.value = null
    }

    fun resetSubmissionResult() {
        _textStatusSubmissionResult.value = null
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