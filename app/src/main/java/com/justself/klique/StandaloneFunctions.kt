package com.justself.klique

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerifiedIcon(modifier: Modifier = Modifier, paddingFigure: Int = 0) {
    Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = "Verified",
        tint = Color.Blue,
        modifier = modifier
            .padding(bottom = paddingFigure.dp, end = paddingFigure.dp)
            .size(16.dp)
    )
}