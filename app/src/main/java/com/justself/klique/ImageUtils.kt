import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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

        if (width <= maxSize && height <= maxSize) {
            // No need to downscale
            return bitmap
        }

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
    fun processImageToByteArray(
        context: Context,
        inputUri: Uri? = null,
        maxSize: Int = 2160,
        inputBitmap: Bitmap? = null
    ): ByteArray {
        val bitmap = inputBitmap?: getImageFromDevice(context, inputUri!!)
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
    fun calculateAverageColor(bitmap: Bitmap): String {
        var red = 0
        var green = 0
        var blue = 0
        val pixelCount = bitmap.width * bitmap.height

        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
            }
        }

        // Calculate the average of each color component
        red /= pixelCount
        green /= pixelCount
        blue /= pixelCount

        // Convert to hex format
        return String.format("#%02X%02X%02X", red, green, blue)
    }
}
