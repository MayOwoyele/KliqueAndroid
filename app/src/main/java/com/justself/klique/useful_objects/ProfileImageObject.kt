package com.justself.klique.useful_objects

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import com.justself.klique.BuildConfig
import com.justself.klique.MyKliqueApp.Companion.appContext
import com.justself.klique.R
import com.justself.klique.downloadFromUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ProfileImageObject {
    private fun cacheNewThumb(
        userId: Int,
        imageUrl: String,
        bitmap: Bitmap,
        targetWidth: Int = 50,
        targetHeight: Int = 50
    ): Boolean {
        clearOldThumbs(userId)
        val file = getCachedThumbFile(userId, imageUrl)
        file.parentFile?.mkdirs()
        val scaledBitmap = downscaleBitmap(bitmap, targetWidth, targetHeight)
        FileOutputStream(file).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return true
    }

    fun getOrDownloadThumbnail(
        userId: Int,
        imageUrl: String,
        targetWidth: Int = 50,
        targetHeight: Int = 50,
        scope: CoroutineScope,
        performBindingAction:(File) -> Unit,
    ){
        val cachedFile = getCachedThumbFile(userId, imageUrl)
        if (cachedFile.exists()) {
            performBindingAction(cachedFile)
        } else {
            scope.launch {
                val downloadedBitmap = downloadImage(imageUrl)
                if (downloadedBitmap != null) {
                    cacheNewThumb(userId, imageUrl, downloadedBitmap, targetWidth, targetHeight)
                    performBindingAction(cachedFile)
                }
            }
        }
    }
    private fun hashUrl(url: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(url.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun getCachedThumbFile(userId: Int, imageUrl: String): File {
        val hash = hashUrl(imageUrl)
        return File(appContext.cacheDir, "thumbs/${userId}_$hash.jpg")
    }

    private fun clearOldThumbs(userId: Int) {
        val thumbsDir = File(appContext.cacheDir, "thumbs")
        thumbsDir.listFiles()?.filter { it.name.startsWith("${userId}_") }?.forEach { it.delete() }
    }

    private fun downscaleBitmap(bitmap: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scaleFactor = minOf(targetWidth / width.toFloat(), targetHeight / height.toFloat())
        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()
        return bitmap.scale(newWidth, newHeight)
    }
    private suspend fun downloadImage(url: String): Bitmap? {
        if (BuildConfig.DEBUG){
            return BitmapFactory.decodeResource(appContext.resources, R.drawable.book_background)
        }
        return try {
            val byteArray = downloadFromUrl(url)
            BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e(
                "GistPP",
                "Error converting downloaded file to Bitmap, url: $url, error: ${e.message}",
                e
            )
            null
        }
    }
}