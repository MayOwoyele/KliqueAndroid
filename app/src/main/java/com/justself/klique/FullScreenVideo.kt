package com.justself.klique

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController

@Composable
fun FullScreenVideo(videoUri: String, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable { navController.popBackStack() }) {
        AndroidView(
            factory = {
                VideoView(it).apply {
                    setVideoURI(Uri.parse(videoUri))
                    start()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}