package com.justself.klique

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlin.math.roundToInt

@Composable
fun FullScreenImage(viewModel: SharedCliqueViewModel, navController: NavController) {
    val bitmap by viewModel.bitmap.observeAsState()


    bitmap?.let {
        val bitmapWidthPx = it.width.toFloat()
        val bitmapHeightPx = it.height.toFloat()
        val density = LocalDensity.current.density
        Log.d("FullScreenImage", "Bitmap Width in Pixels: ${bitmapWidthPx} px")
        Log.d("FullScreenImage", "Bitmap Height in Pixels: ${bitmapHeightPx} px")

        val bitmapWidthDp = bitmapWidthPx / density
        val bitmapHeightDp = bitmapHeightPx / density

        Log.d("FullScreenImage", "Bitmap Width: ${bitmapWidthDp} dp")
        Log.d("FullScreenImage", "Bitmap Height: ${bitmapHeightDp} dp")

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            Log.d("FullScreenImage", "Canvas Width: ${canvasWidth} dp")
            Log.d("FullScreenImage", "Canvas Height: ${canvasHeight} dp")

            val widthScaleFactor = canvasWidth / bitmapWidthPx
            val heightScaleFactor = canvasHeight / bitmapHeightPx

            val scale = minOf(widthScaleFactor, heightScaleFactor)

            val scaledBitmapWidth = bitmapWidthPx * scale
            val scaledBitmapHeight = bitmapHeightPx * scale
            val offsetX = (canvasWidth - scaledBitmapWidth) / 2
            val offsetY = (canvasHeight - scaledBitmapHeight) / 2

            Log.d("FullScreenImage", "OffsetX: ${offsetX} dp")
            Log.d("FullScreenImage", "OffsetY: ${offsetY} dp")
            Log.d("FullScreenImage", "Scale: ${scale}")

            with(drawContext.canvas.nativeCanvas) {
                save()
                translate(offsetX, offsetY) // Ensure this conversion is correct
                scale(scale, scale)
                drawImage(
                    image = it.asImageBitmap(),
                    topLeft = Offset.Zero
                )
                restore()
            }
        }
    }
}