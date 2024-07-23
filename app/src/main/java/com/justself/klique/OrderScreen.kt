package com.justself.klique

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import java.io.ByteArrayOutputStream
import java.io.IOException

@Composable
fun OrdersScreen( navController: NavController, viewModel: ChatScreenViewModel) {
    val context = LocalContext.current
    /*LaunchedEffect(Unit) {
        viewModel.testHandleIncomingPersonalMessage(context)
    } */
}
fun getBytesFromResource(context: Context, resourceId: Int): ByteArray? {
    return context.resources.openRawResource(resourceId).use { inputStream ->
        inputStream.readBytes()
    }
}
fun resizeBitmap(context: Context, data: ByteArray, targetWidth: Int, targetHeight: Int): ByteArray? {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width = if (bitmap.width > bitmap.height) targetWidth else (targetHeight * aspectRatio).toInt()
        val height = if (bitmap.height > bitmap.width) targetHeight else (targetWidth / aspectRatio).toInt()
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.toByteArray()
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}