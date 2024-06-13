
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

object ImageUtils {

    @Throws(IOException::class)
    fun getImageFromDevice(context: Context, uri: Uri): Bitmap? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    @Throws(IOException::class)
    fun downscaleImage(bitmap: Bitmap, maxSize: Int = 1080): Bitmap {
        val width: Int = bitmap.width
        val height: Int = bitmap.height

        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            // Landscape
            val ratio: Float = maxSize.toFloat() / width.toFloat()
            newWidth = maxSize
            newHeight = (height * ratio).toInt()
        } else {
            // Portrait or square
            val ratio: Float = maxSize.toFloat() / height.toFloat()
            newHeight = maxSize
            newWidth = (width * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    @Throws(IOException::class)
    fun processImageToByteArray(context: Context, inputUri: Uri, maxSize: Int = 1080): ByteArray {
        val bitmap = getImageFromDevice(context, inputUri)
            ?: throw IOException("Failed to decode bitmap from URI: $inputUri")

        val downscaledBitmap = downscaleImage(bitmap, maxSize)

        val byteArrayOutputStream = ByteArrayOutputStream()
        downscaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Clean up
        if (downscaledBitmap != bitmap) {
            downscaledBitmap.recycle()
        }
        bitmap.recycle()

        return byteArray
    }
}
