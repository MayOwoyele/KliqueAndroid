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
fun OrdersScreen( navController: NavController, viewModel: ChatScreenViewModel, customerId: Int, mediaViewModel: MediaViewModel) {
    StatusSelectionScreen(navController = navController, mediaViewModel = mediaViewModel)
}