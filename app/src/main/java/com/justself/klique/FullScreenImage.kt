package com.justself.klique

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel

@Composable
fun FullScreenImage(viewModel: SharedCliqueViewModel, navController: NavController) {
    val bitmap by viewModel.bitmap.observeAsState()
    val scale = remember { mutableStateOf(1f) }
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    val doubleTapScale = 2f
    bitmap?.let {
        val bitmapWidthPx = it.width.toFloat()
        val bitmapHeightPx = it.height.toFloat()
        Log.d("FullScreenImage", "Bitmap width is $bitmapWidthPx, bitmap height is $bitmapHeightPx")

        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures(
                    onGesture = { _, pan, zoom, _ ->
                        // Update scale
                        val newScale = (scale.value * zoom).coerceIn(1f, 3f)
                        scale.value = newScale

                        // Calculate necessary values for panning
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val widthScaleFactor = canvasWidth / bitmapWidthPx
                        val heightScaleFactor = canvasHeight / bitmapHeightPx
                        val scaleBitmapToFit = minOf(widthScaleFactor, heightScaleFactor) * newScale

                        val scaledBitmapWidth = bitmapWidthPx * scaleBitmapToFit
                        val scaledBitmapHeight = bitmapHeightPx * scaleBitmapToFit

                        // Calculate maximum offsets to prevent panning out of bounds
                        val maxXOffset = ((scaledBitmapWidth - canvasWidth) / 2).coerceAtLeast(0f)
                        val maxYOffset = ((scaledBitmapHeight - canvasHeight) / 2).coerceAtLeast(0f)

                        // Update offsets with bounds checking
                        offsetX.value = (offsetX.value + pan.x).coerceIn(-maxXOffset, maxXOffset)
                        offsetY.value = (offsetY.value + pan.y).coerceIn(-maxYOffset, maxYOffset)
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tapOffset ->
                    scale.value = if (scale.value == 1f) doubleTapScale else 1f

                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val widthScaleFactor = canvasWidth / bitmapWidthPx
                    val heightScaleFactor = canvasHeight / bitmapHeightPx

                    val newScale = scale.value
                    val scaleBitmapToFit = minOf(widthScaleFactor, heightScaleFactor) * newScale

                    val scaledBitmapWidth = bitmapWidthPx * scaleBitmapToFit
                    val scaledBitmapHeight = bitmapHeightPx * scaleBitmapToFit
                    val drawOffsetX = (canvasWidth - scaledBitmapWidth) / 2
                    val drawOffsetY = (canvasHeight - scaledBitmapHeight) / 2

                    val maxXOffset = ((scaledBitmapWidth - canvasWidth) / 2).coerceAtLeast(0f)
                    val maxYOffset = ((scaledBitmapHeight - canvasHeight) / 2).coerceAtLeast(0f)
                    offsetX.value = if (newScale == doubleTapScale) {
                        ((canvasWidth / 2 - tapOffset.x * newScale).coerceIn(
                            -maxXOffset,
                            maxXOffset
                        ))
                    } else {
                        0f
                    }
                    offsetY.value = if (newScale == doubleTapScale) {
                        ((canvasHeight / 2 - tapOffset.x * newScale)).coerceIn(
                            -maxYOffset,
                            maxYOffset
                        )
                    } else {
                        0f
                    }
                })
            }) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val widthScaleFactor = canvasWidth / bitmapWidthPx
            val heightScaleFactor = canvasHeight / bitmapHeightPx

            // Calculate the scale to fit the bitmap to the canvas, considering the current scale
            val scaleBitmapToFit = minOf(widthScaleFactor, heightScaleFactor) * scale.value

            val scaledBitmapWidth = bitmapWidthPx * scaleBitmapToFit
            val scaledBitmapHeight = bitmapHeightPx * scaleBitmapToFit

            // Calculate the offsets to center the bitmap within the canvas
            val offsetXForFit = (canvasWidth - scaledBitmapWidth) / 2 + offsetX.value
            val offsetYForFit = (canvasHeight - scaledBitmapHeight) / 2 + offsetY.value

            with(drawContext.canvas.nativeCanvas) {
                save()
                translate(offsetXForFit, offsetYForFit) // Ensure this conversion is correct
                scale(scaleBitmapToFit, scaleBitmapToFit)
                drawImage(
                    image = it.asImageBitmap(),
                    topLeft = Offset.Zero
                )
                restore()
            }
        }
    }
}