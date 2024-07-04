package com.justself.klique

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import java.io.*

object FileUtils {

    fun getPath(context: Context, uri: Uri): String? {
        return if (uri.scheme == "file") {
            uri.path
        } else if (uri.scheme == "content") {
            copyFileToInternalStorage(context, uri)
        } else {
            null
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
}