package com.justself.klique

import android.annotation.TargetApi
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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
    fun saveToInternalStorage(context: Context, data: ByteArray, fileName: String, subdirectory: String): Uri? {
        return try {
            val directory = File(context.filesDir, subdirectory)
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
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, "klique images")
        } else {
            saveToInternalStorage(context, data, fileName, "images")
        }
    }

    fun saveVideo(context: Context, data: ByteArray, toPublic: Boolean = false): Uri? {
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, "klique videos")
        } else {
            saveToInternalStorage(context, data, fileName, "videos")
        }
    }

    fun saveAudio(context: Context, data: ByteArray, toPublic: Boolean = false): Uri? {
        val fileName = "audio_${System.currentTimeMillis()}.mp3"
        return if (toPublic) {
            saveToPublicDirectory(context, data, fileName, "klique audio")
        } else {
            saveToInternalStorage(context, data, fileName, "audio")
        }
    }
    fun saveToPublicDirectory(context: Context, data: ByteArray, fileName: String, subdirectory: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10 (Q) and above
            saveToPublicDirectoryQAndAbove(context, data, fileName, subdirectory)
        } else {
            // Use traditional method for Android 9 (Pie) and below
            saveToPublicDirectoryLegacy(context, data, fileName, subdirectory)
        }
    }

    @TargetApi(Build.VERSION_CODES.Q)
    fun saveToPublicDirectoryQAndAbove(context: Context, data: ByteArray, fileName: String, subdirectory: String): Uri? {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + subdirectory)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return uri?.also {
            resolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(data)
            }
        }
    }

    fun saveToPublicDirectoryLegacy(context: Context, data: ByteArray, fileName: String, subdirectory: String): Uri? {
        return try {
            val directory = Environment.getExternalStoragePublicDirectory(subdirectory)
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

    fun getMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }
}