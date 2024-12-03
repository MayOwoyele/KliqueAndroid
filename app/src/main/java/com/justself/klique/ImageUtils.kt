import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.absoluteValue
import kotlin.math.sin

object ImageUtils {

    @Throws(IOException::class)
    fun getImageFromDevice(context: Context, uri: Uri): Bitmap? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }

    @Throws(IOException::class)
    fun downscaleImage(
        bitmap: Bitmap,
        maxSize: Int = 1080,
        shouldReturnOriginal: (Boolean) -> Unit
    ): Bitmap {
        val largerDimension = maxOf(bitmap.width, bitmap.height)

        if (largerDimension <= maxSize) {
            shouldReturnOriginal(true)
            return bitmap
        }

        val scaleRatio: Float = maxSize / largerDimension.toFloat()
        val newHeight = bitmap.height * scaleRatio
        val newWidth = bitmap.width * scaleRatio
        val newBitmap = lanczosResample(
            src = bitmap,
            newWidth = newWidth.toInt(),
            newHeight = newHeight.toInt(),
            a = 2.0f
        )
        return newBitmap
    }

    private fun lanczosKernel(x: Float, a: Float): Float {
        return if (x == 0f) {
            1f
        } else if (x.absoluteValue < a) {
            val piX = Math.PI * x
            (sin(piX) / piX * sin(piX / a) / (piX / a)).toFloat()
        } else {
            0f
        }
    }

    private fun lanczosResample(src: Bitmap, newWidth: Int, newHeight: Int, a: Float): Bitmap {
        val dst = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val scaleX = src.width.toFloat() / newWidth
        val scaleY = src.height.toFloat() / newHeight

        for (y in 0 until newHeight) {
            for (x in 0 until newWidth) {
                var r = 0f
                var g = 0f
                var b = 0f
                var weightSum = 0f

                for (dy in -a.toInt()..a.toInt()) {
                    for (dx in -a.toInt()..a.toInt()) {
                        val srcX = (x * scaleX + dx).toInt().coerceIn(0, src.width - 1)
                        val srcY = (y * scaleY + dy).toInt().coerceIn(0, src.height - 1)

                        val weightX = lanczosKernel((x * scaleX - srcX), a)
                        val weightY = lanczosKernel((y * scaleY - srcY), a)
                        val weight = weightX * weightY

                        val color = src.getPixel(srcX, srcY)
                        r += Color.red(color) * weight
                        g += Color.green(color) * weight
                        b += Color.blue(color) * weight
                        weightSum += weight
                    }
                }

                val finalR = (r / weightSum).coerceIn(0f, 255f).toInt()
                val finalG = (g / weightSum).coerceIn(0f, 255f).toInt()
                val finalB = (b / weightSum).coerceIn(0f, 255f).toInt()

                dst.setPixel(x, y, Color.rgb(finalR, finalG, finalB))
            }
        }

        return dst
    }

    @Throws(IOException::class)
    fun processImageToByteArray(
        context: Context,
        inputUri: Uri? = null,
        maxSize: Int = 1080,
        inputBitmap: Bitmap? = null
    ): ByteArray {
        val bitmap = inputBitmap ?: getImageFromDevice(context, inputUri!!)
        ?: throw IOException("Failed to decode bitmap from URI: $inputUri")
        val correctedBitmap = inputUri?.let { correctBitmapOrientation(context, it, bitmap) } ?: bitmap

        var shouldReturnOriginal = false
        val downscaledBitmap = downscaleImage(correctedBitmap, maxSize) {
            shouldReturnOriginal = it
        }

        return if (shouldReturnOriginal && inputUri != null) {
            Log.d("ImageLoad", "this is called for")
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: throw IOException("Failed to read original JPEG data from URI: $inputUri")
        } else {
            Log.d("ImageLoad", "this is also called for")
            val byteArrayOutputStream = ByteArrayOutputStream()
            downscaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            if (downscaledBitmap != bitmap && downscaledBitmap != correctedBitmap) {
                downscaledBitmap.recycle()
            }
            if (correctedBitmap != bitmap) {
                correctedBitmap.recycle()
            }
            bitmap.recycle()

            Log.d("ImageLoad", "Image size is ${byteArray.size}")
            byteArray
        }
    }

    private fun correctBitmapOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri)
        val exif = inputStream?.let { ExifInterface(it) }
        inputStream?.close()

        val orientation =
            exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val rotationMatrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotationMatrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotationMatrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotationMatrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> rotationMatrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> rotationMatrix.preScale(1f, -1f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotationMatrix, true)
    }

    fun calculateAverageColor(bitmap: Bitmap): Int {
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

        red /= pixelCount
        green /= pixelCount
        blue /= pixelCount

        return Color.rgb(red, green, blue)
    }
}
