package com.justself.klique

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ImageCropTool(
    navController: NavController,
    sourceScreen: SourceScreen = SourceScreen.STATUS,
    customerId: Int
) {
    val bitmap by MediaVM.bitmap.observeAsState()
    val toCropImageUri by MediaVM.toCropImageURi.collectAsState()
    val scale = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }
    val doubleTapScale = 2f
    val showBackArrow = remember { mutableStateOf(true) }
    var aspectRatio by remember { mutableFloatStateOf(1f) }
    val cropRect = remember { mutableStateOf(RectF()) }
    val context = LocalContext.current
    val isUploading by MediaVM.isUploading.collectAsState()
    bitmap?.let {
        if (cropRect.value.isEmpty) {
            val bitmapWidthPx = it.width.toFloat()
            val bitmapHeightPx = it.height.toFloat()
            val imageAspectRatio = bitmapWidthPx / bitmapHeightPx

            val cropAspectRatio = 1f

            val initialWidth: Float
            val initialHeight: Float

            if (imageAspectRatio >= cropAspectRatio) {
                initialHeight = bitmapHeightPx / 2
                initialWidth = initialHeight * cropAspectRatio
            } else {
                initialWidth = bitmapWidthPx / 2 // Example: Use half of the image width
                initialHeight = initialWidth / cropAspectRatio
            }

            // Calculate the positions of the edges
            val left = (bitmapWidthPx - initialWidth) / 2
            val top = (bitmapHeightPx - initialHeight) / 2
            val right = left + initialWidth
            val bottom = top + initialHeight

            // Initialize the crop rectangle
            cropRect.value = RectF(left, top, right, bottom)
            aspectRatio = initialWidth / initialHeight
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            MediaVM.clearBitmap()
            MediaVM.clearCroppedBitmap()
            MediaVM.setIsUploading(false)
            MediaVM.resetToCropImageUri()
        }
    }
    LaunchedEffect(key1 = bitmap) {
        Log.d("kliqueBitmap", "${bitmap == null}")
    }

    bitmap?.let {
        val bitmapWidthPx = it.width.toFloat()
        val bitmapHeightPx = it.height.toFloat()
        Log.d("FullScreenImage", "Bitmap width is $bitmapWidthPx, bitmap height is $bitmapHeightPx")

        Box(modifier = Modifier.fillMaxSize()) {
            val onPrimary = MaterialTheme.colorScheme.onPrimary
            val croppedBitmap = remember { mutableStateOf<Bitmap?>(null) }
            val isCropping = remember { mutableStateOf(true) }
            if (isCropping.value) {
                Canvas(modifier = Modifier
                    .fillMaxSize()
                    .transformable(
                        state = rememberTransformableState { zoomChange, _, _ ->
                            val horizontalResize = (cropRect.value.width() * (zoomChange - 1)) / 2
                            val verticalResize = horizontalResize / aspectRatio

                            var newLeft = cropRect.value.left - horizontalResize
                            var newTop = cropRect.value.top - verticalResize
                            var newRight = cropRect.value.right + horizontalResize
                            var newBottom = cropRect.value.bottom + verticalResize

                            // Flags to track if any boundary is hit
                            var leftBoundaryHit = false
                            var topBoundaryHit = false
                            var rightBoundaryHit = false
                            var bottomBoundaryHit = false

                            // Check and adjust for boundaries
                            if (newLeft < 0) {
                                newLeft = 0f
                                leftBoundaryHit = true
                            }
                            if (newTop < 0) {
                                newTop = 0f
                                topBoundaryHit = true
                            }
                            if (newRight > bitmapWidthPx) {
                                newRight = bitmapWidthPx
                                rightBoundaryHit = true
                            }
                            if (newBottom > bitmapHeightPx) {
                                newBottom = bitmapHeightPx
                                bottomBoundaryHit = true
                            }

                            // Adjust proportional scaling if only one boundary is hit
                            if (leftBoundaryHit || rightBoundaryHit) {
                                val width = newRight - newLeft
                                val height = width / aspectRatio
                                newBottom = newTop + height
                                if (leftBoundaryHit) newRight = newLeft + width
                                if (rightBoundaryHit) newLeft = newRight - width
                            }
                            if (topBoundaryHit || bottomBoundaryHit) {
                                val height = newBottom - newTop
                                val width = height * aspectRatio
                                newRight = newLeft + width
                                if (topBoundaryHit) newBottom = newTop + height
                                if (bottomBoundaryHit) newTop = newBottom - height
                            }

                            if ((leftBoundaryHit && rightBoundaryHit) || (topBoundaryHit && bottomBoundaryHit)) {
                                return@rememberTransformableState
                            }

                            cropRect.value = RectF(newLeft, newTop, newRight, newBottom)
                        }
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val horizontalResize = (cropRect.value.width() * (zoom - 1)) / 2
                            val verticalResize = horizontalResize / aspectRatio

                            var newLeft = cropRect.value.left - horizontalResize
                            var newTop = cropRect.value.top - verticalResize
                            var newRight = cropRect.value.right + horizontalResize
                            var newBottom = cropRect.value.bottom + verticalResize

                            var leftBoundaryHit = false
                            var topBoundaryHit = false
                            var rightBoundaryHit = false
                            var bottomBoundaryHit = false

                            if (newLeft <= 0) {
                                newLeft = 0f
                                leftBoundaryHit = true
                            }
                            if (newTop <= 0) {
                                newTop = 0f
                                topBoundaryHit = true
                            }
                            if (newRight >= bitmapWidthPx) {
                                newRight = bitmapWidthPx
                                rightBoundaryHit = true
                            }
                            if (newBottom >= bitmapHeightPx) {
                                newBottom = bitmapHeightPx
                                bottomBoundaryHit = true
                            }

                            // Adjust proportional scaling if only one boundary is hit
                            if (leftBoundaryHit || rightBoundaryHit) {
                                val width = newRight - newLeft
                                val height = width / aspectRatio
                                newBottom = newTop + height
                                if (leftBoundaryHit) newRight = newLeft + width
                                if (rightBoundaryHit) newLeft = newRight - width
                            }
                            if (topBoundaryHit || bottomBoundaryHit) {
                                val height = newBottom - newTop
                                val width = height * aspectRatio
                                newRight = newLeft + width
                                if (topBoundaryHit) newBottom = newTop + height
                                if (bottomBoundaryHit) newTop = newBottom - height
                            }
                            if ((leftBoundaryHit && rightBoundaryHit) || (topBoundaryHit && bottomBoundaryHit)) {
                                return@detectTransformGestures
                            }

                            cropRect.value = RectF(newLeft, newTop, newRight, newBottom)

                            val dragX = (pan.x / scale.value).coerceIn(
                                -cropRect.value.left,
                                bitmapWidthPx - cropRect.value.right
                            )
                            val dragY = (pan.y / scale.value).coerceIn(
                                -cropRect.value.top,
                                bitmapHeightPx - cropRect.value.bottom
                            )

                            cropRect.value = RectF(
                                cropRect.value.left + dragX,
                                cropRect.value.top + dragY,
                                cropRect.value.right + dragX,
                                cropRect.value.bottom + dragY
                            )
                        }
                    }) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val widthScaleFactor = canvasWidth / bitmapWidthPx
                    val heightScaleFactor = canvasHeight / bitmapHeightPx
                    val scaleBitmapToFit = minOf(widthScaleFactor, heightScaleFactor) * scale.floatValue

                    val scaledBitmapWidth = bitmapWidthPx * scaleBitmapToFit
                    val scaledBitmapHeight = bitmapHeightPx * scaleBitmapToFit

                    val offsetXForFit = (canvasWidth - scaledBitmapWidth) / 2 + offsetX.floatValue
                    val offsetYForFit = (canvasHeight - scaledBitmapHeight) / 2 + offsetY.floatValue

                    with(drawContext.canvas.nativeCanvas) {
                        save()
                        translate(offsetXForFit, offsetYForFit)
                        scale(scaleBitmapToFit, scaleBitmapToFit)
                        drawImage(
                            image = it.asImageBitmap(),
                            topLeft = Offset.Zero
                        )
                        restore()
                    }
                    val translatedCropLeft =
                        offsetXForFit + (cropRect.value.left * scaleBitmapToFit)
                    val translatedCropTop = offsetYForFit + (cropRect.value.top * scaleBitmapToFit)
                    val translatedCropRight =
                        offsetXForFit + (cropRect.value.right * scaleBitmapToFit)
                    val translatedCropBottom =
                        offsetYForFit + (cropRect.value.bottom * scaleBitmapToFit)
                    drawRect(
                        color = onPrimary,
                        topLeft = Offset(translatedCropLeft, translatedCropTop),
                        size = Size(
                            translatedCropRight - translatedCropLeft,
                            translatedCropBottom - translatedCropTop
                        ),
                        style = Stroke(width = 5f)
                    )
                }
            } else {
                if (!isUploading) {
                    croppedBitmap.value?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Cropped Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                                .size(200.dp)
                        )
                    }
                }
            }
            // Conditionally display the back arrow button
            if (showBackArrow.value) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Text("Klique Cropper")
                }
            }
            if (!isCropping.value) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onPrimary
                                ), shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                isCropping.value = true
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            "Cancel",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Box(
                        modifier = Modifier
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onPrimary
                                ), shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                croppedBitmap.value?.let { bitmap ->
                                    when (sourceScreen) {
                                        SourceScreen.STATUS -> {
                                            isCropping.value = false
                                            MediaVM.setIsUploading(true)
                                            MediaVM.uploadCroppedImage(
                                                context,
                                                bitmap,
                                                customerId
                                            )
                                            navController.popBackStack()
                                        }

                                        SourceScreen.PROFILE -> {
                                            MediaVM.setCroppedBitmap(bitmap)
                                            navController.popBackStack()
                                        }
                                    }
                                }
                            }
                            .padding(16.dp)
                    ) {
                        Text("Post", color = onPrimary, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            if (isCropping.value) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            bitmap?.let { originalBitmap ->
                                croppedBitmap.value = cropBitmap(originalBitmap, toCropImageUri ?: "", cropRect.value)
                                isCropping.value = false
                            }
                        }
                        .padding(16.dp)
                ) {
                    Text("Preview", color = onPrimary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}


fun cropBitmap(bitmap: Bitmap, uriString: String, cropRect: RectF): Bitmap {
//    val imageUri = Uri.parse(uriString)
    val left = cropRect.left.toInt()
    val top = cropRect.top.toInt()
    val width = cropRect.width().toInt()
    val height = cropRect.height().toInt()

    return Bitmap.createBitmap(bitmap, left, top, width, height)
}

enum class SourceScreen {
    STATUS, PROFILE
}