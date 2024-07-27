package com.justself.klique

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun StatusSelectionScreen(navController: NavController, mediaViewModel: MediaViewModel) {
    val color = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        MediaBlock(label = "Video", color = color, onClick = { /* Handle Video Click */ })
        MediaBlock(label = "Image", color = color, onClick = { /* Handle Image Click */ })
        MediaBlock(label = "Text", color = color, onClick = { /* Handle Text Click */ })
        MediaBlock(label = "Audio", color = color, onClick = { /* Handle Audio Click */ })
    }
}

@Composable
fun MediaBlock(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.displayLarge, fontSize = 24.sp, color = MaterialTheme.colorScheme.background)
        }
    }
}