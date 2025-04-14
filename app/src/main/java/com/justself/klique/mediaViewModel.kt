package com.justself.klique

import ImageUtils
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.justself.klique.JWTNetworkCaller.performReusableNetworkCalls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object MediaVM {
    val scope = CoroutineScope(Dispatchers.IO)
    private var _textStatusSubmissionResult = MutableStateFlow<Boolean?>(null)
    val textStatusSubmissionResult = _textStatusSubmissionResult.asStateFlow()

    private var _audioStatusSubmissionResult = MutableStateFlow<Boolean?>(null)
    val audioStatusSubmissionResult = _audioStatusSubmissionResult.asStateFlow()

    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
    private val _toCropImageUri = MutableStateFlow<String?>(null)
    val toCropImageURi = _toCropImageUri.asStateFlow()
    private val _homeScreenUri = MutableLiveData<Uri?>()
    val homeScreenUri: LiveData<Uri?> get() = _homeScreenUri

    private val _messageScreenUri = MutableLiveData<Uri?>()
    val messageScreenUri: LiveData<Uri?> get() = _messageScreenUri
    private val _croppedBitmap = MutableLiveData<Bitmap?>()
    val croppedBitmap: LiveData<Bitmap?> get() = _croppedBitmap
    val customerId = MutableStateFlow<Int?>(null)
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()
    private val _isDownScalingImageForUpdateProfile = MutableStateFlow(false)
    val isDownScalingImageForUpdateProfile = _isDownScalingImageForUpdateProfile.asStateFlow()

    fun setCustomerId(value: Int) {
        customerId.value = value
    }

    init {
        valueOfHomeScreenUri()
    }

    private fun valueOfHomeScreenUri() {
        scope.launch {
            while (true) {
                delay(3000)
                Log.d("HomeScreenUri", "current value $_homeScreenUri")
            }
        }
    }

    fun setBitmap(bitmap: Bitmap) {
        _bitmap.value = bitmap
        Log.d("BitmapSet", "Bitmap Set")
    }

    fun setBitmapFromUri(uri: Uri, context: Context) {
        scope.launch(Dispatchers.IO) {
            val bitmap = loadBitmapFromUri(uri, context)
            withContext(Dispatchers.Main) {
                _bitmap.value = bitmap
                _toCropImageUri.value = uri.toString()
            }
        }
    }

    fun resetToCropImageUri() {
        _toCropImageUri.value = null
    }

    private fun loadBitmapFromUri(uri: Uri, context: Context): Bitmap? {
        return try {
            val bitmap = ImageUtils.getImageFromDevice(context, uri)!!
            bitmap
        } catch (e: Exception) {
            Log.d("StackTrace", e.toString())
            null
        }
    }

    fun clearBitmap() {
        if (!_isUploading.value) {
            _bitmap.postValue(null)
        }
    }

    fun setIsUploading(bool: Boolean) {
        _isUploading.value = bool
    }

    fun preparePerformTrimmingAndDownscaling(
        context: Context, uri: Uri, startMs: Long, endMs: Long, sourceScreen: String
    ) {
        scope.launch(Dispatchers.IO) {
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

            videoBytes?.let {
                scope.launch {
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
                        val fields = listOf(userIdField, videoFileField)

                        performReusableNetworkCalls(
                            response = {
                                NetworkUtils.makeMultipartRequest(
                                    "sendVideoStatus",
                                    fields
                                )
                            },
                            action = { response ->
                                Log.d("KliqueVideoStatus", "Video uploaded successfully")
                                Toast.makeText(
                                    context,
                                    "Video has successfully been uploaded",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            errorAction = { response ->
                                Log.e(
                                    "KliqueVideoStatus",
                                    "Failed to upload video: ${response.toNetworkTriple().second}, ${response.toNetworkTriple().third}"
                                )
                                Toast.makeText(
                                    context,
                                    "Error uploading the video",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("Video Status", "Error uploading video", e)
                    }
                }
            } ?: run {
                Log.e("Video Status", "Error: Video file is null or invalid")
                Toast.makeText(
                    context,
                    "Error preparing the video file. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } ?: run {
            Log.e("Video Status", "Error: Video URI is null")
            Toast.makeText(
                context,
                "Invalid video URI. Please select a valid video.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun uploadCroppedImage(context: Context, bitmap: Bitmap, customerId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                val byteArray =
                    ImageUtils.processImageToByteArray(context = context, inputBitmap = bitmap)
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

                performReusableNetworkCalls(
                    response = { NetworkUtils.makeMultipartRequest("sendImageStatus", fields) },
                    action = { response ->
                        Log.d(
                            "UploadSuccess",
                            "Image uploaded successfully: ${response.toNetworkTriple().second}"
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Image uploaded successfully!",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    },
                    errorAction = { response ->
                        if (response is NetworkUtils.JwtTriple.Value) {
                            Log.e(
                                "UploadError",
                                "Failed to upload image: ${response.responseCode} - ${response.responseCode}"
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Failed to upload image. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                )

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
        scope.launch(Dispatchers.IO) {
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
                    val fields = listOf(userIdField, audioFileField)

                    performReusableNetworkCalls(
                        response = { NetworkUtils.makeMultipartRequest("sendAudioStatus", fields) },
                        action = { response ->
                            Log.d(
                                "UploadSuccess",
                                "Audio file uploaded successfully: ${response.toNetworkTriple().second}"
                            )
                            withContext(Dispatchers.Main) {
                                _audioStatusSubmissionResult.value = true
                            }
                        },
                        errorAction = { response ->
                            if (response is NetworkUtils.JwtTriple.Value) {
                                Log.e(
                                    "UploadError",
                                    "Failed to upload audio file: ${response.responseCode} - ${response.response}"
                                )
                            }
                            withContext(Dispatchers.Main) {
                                _audioStatusSubmissionResult.value = false
                                Toast.makeText(
                                    context,
                                    "Failed with status code ${response.toNetworkTriple().second}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context, "Failed to load audio file.", Toast.LENGTH_LONG
                        ).show()
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
        scope.launch()
        {
            try {
                NetworkUtils.makeJwtRequest("sendTextStatus",
                    KliqueHttpMethod.POST,
                    emptyMap(),
                    jsonBody,
                    action = { response ->
                        _textStatusSubmissionResult.value = response.toNetworkTriple().first
                    },
                    errorAction = { response ->
                        Log.e(
                            "NetworkUtils",
                            "Failed to send text status: ${response.toNetworkTriple().second}"
                        )
                        _textStatusSubmissionResult.value = false
                    })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    fun setCroppedBitmap(bitmap: Bitmap) {
        scope.launch {
            setIsDownScalingImageForUpdateProfile(true)
            val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) // Copy to a compatible config
            } else {
                bitmap
            }
            var shouldReturnOriginal = false
            val downscaledBitmap = ImageUtils.downscaleImage(safeBitmap, 720) {
                shouldReturnOriginal = it
            }
            withContext(Dispatchers.Main) {
                _croppedBitmap.value = if (shouldReturnOriginal) safeBitmap else downscaledBitmap
            }
        }
    }

    fun setIsDownScalingImageForUpdateProfile(theBool: Boolean) {
        _isDownScalingImageForUpdateProfile.value = theBool
    }

    fun clearCroppedBitmap() {
        _croppedBitmap.value = null
    }

    fun resetSubmissionResult() {
        _textStatusSubmissionResult.value = null
    }
}