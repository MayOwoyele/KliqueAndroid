package com.justself.klique

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaViewModel(application: Application): AndroidViewModel(application) {
    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> get() = _bitmap
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