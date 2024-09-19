package com.justself.klique

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.*
import java.io.File

object FileUtils {

    fun getPath(context: Context, uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> {
                uri.path
            }
            "content" -> {
                copyFileToInternalStorage(context, uri)
            }
            else -> {
                null
            }
        }
    }

    private fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val fileName = getFileName(context, uri)
        val tempFile = File(context.cacheDir, fileName)

        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        val path = tempFile.absolutePath
        Log.d("FileUtils", "Copied File Path: $path")
        return path
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = ""
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        Log.d("FileUtils", "File Name: $name")
        return name
    }
    @Throws(IOException::class)
    fun fileToByteArray(file: File): ByteArray {
        return file.readBytes()
    }
    fun saveMedia(
        context: Context,
        data: ByteArray,
        mediaType: MediaType,  // Now we use MediaType enum
        toPublic: Boolean = false
    ): Uri? {
        val fileName = mediaType.generateFileName()

        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, mediaType)
        } else {
            saveToInternalStorage(context, data, fileName, mediaType)
        }
    }
    fun saveToInternalStorage(
        context: Context,
        data: ByteArray,
        fileName: String,
        mediaType: MediaType
    ): Uri? {
        return try {
            val directory = File(context.filesDir, mediaType.type)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            file.writeBytes(data)
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    fun saveImage(context: Context, data: ByteArray, toPublic: Boolean = false): Uri? {
        val fileName = "kliqueImage_${System.currentTimeMillis()}.jpg"
        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, MediaType.IMAGE)
        } else {
            saveToInternalStorage(context, data, fileName, MediaType.IMAGE)
        }
    }

    fun saveVideo(context: Context, data: ByteArray, toPublic: Boolean = false): Uri? {
        val fileName = "kliqueVideo_${System.currentTimeMillis()}.mp4"
        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, MediaType.VIDEO)
        } else {
            saveToInternalStorage(context, data, fileName, MediaType.VIDEO)
        }
    }

    fun saveAudio(context: Context, data: ByteArray, toPublic: Boolean = false): Uri? {
        val fileName = "kliqueAudio_${System.currentTimeMillis()}.mp3"
        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, MediaType.AUDIO)
        } else {
            saveToInternalStorage(context, data, fileName, MediaType.AUDIO)
        }
    }
    fun saveToPublicDirectory(
        context: Context,
        data: ByteArray,
        fileName: String,
        mediaType: MediaType
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToPublicDirectoryQAndAbove(context, data, fileName, mediaType)
        } else {
            saveToPublicDirectoryLegacy(context, data, fileName, mediaType)
        }
    }

    fun getContentUri(mediaType: String): Uri {
        return when (mediaType) {
            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri("external")
        }
    }
    fun getPublicDirectory(mediaType: String): String {
        return when (mediaType) {
            "image" -> Environment.DIRECTORY_PICTURES + File.separator + "klique images"
            "video" -> Environment.DIRECTORY_MOVIES + File.separator + "klique videos"
            "audio" -> Environment.DIRECTORY_MUSIC + File.separator + "klique audio"
            else -> Environment.DIRECTORY_DOWNLOADS
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    fun saveToPublicDirectoryQAndAbove(
        context: Context,
        data: ByteArray,
        fileName: String,
        mediaType: MediaType
    ): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, mediaType.getPublicDirectory())
        }

        val resolver = context.contentResolver
        val contentUri = mediaType.getContentUri()
        return resolver.insert(contentUri, contentValues)?.also { uri ->
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(data)
            }
        }
    }

    fun saveToPublicDirectoryLegacy(
        context: Context,
        data: ByteArray,
        fileName: String,
        mediaType: MediaType
    ): Uri? {
        return try {
            val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), mediaType.getPublicDirectory())
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, fileName)
            file.writeBytes(data)

            // Trigger media scan
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)

            Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
    fun loadFileAsByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.readBytes()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }
    fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        return outputStream.toByteArray()
    }
}
enum class MediaType(val type: String) {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio");

    // Helper to get the correct public directory based on the media type
    fun getPublicDirectory(): String {
        return when (this) {
            IMAGE -> Environment.DIRECTORY_PICTURES + File.separator + "klique images"
            VIDEO -> Environment.DIRECTORY_MOVIES + File.separator + "klique videos"
            AUDIO -> Environment.DIRECTORY_MUSIC + File.separator + "klique audio"
        }
    }

    // Helper to get the correct content URI for MediaStore
    fun getContentUri(): Uri {
        return when (this) {
            IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
    }

    // Helper to generate the correct file extension based on the media type
    fun generateFileName(): String {
        return when (this) {
            IMAGE -> "kliqueImage_${System.currentTimeMillis()}.jpg"
            VIDEO -> "kliqueVideo_${System.currentTimeMillis()}.mp4"
            AUDIO -> "kliqueAudio_${System.currentTimeMillis()}.mp3"
        }
    }
}