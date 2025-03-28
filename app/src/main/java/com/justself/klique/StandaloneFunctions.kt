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
fun getCustomRelativeTimeSpanString(time: Long, now: Long): String {
    val diff = now - time
    val SECOND_MILLIS = 1000L
    val MINUTE_MILLIS = 60 * SECOND_MILLIS
    val HOUR_MILLIS = 60 * MINUTE_MILLIS
    val DAY_MILLIS = 24 * HOUR_MILLIS
    val WEEK_MILLIS = 7 * DAY_MILLIS
    val MONTH_MILLIS = 30 * DAY_MILLIS
    val YEAR_MILLIS = 365 * DAY_MILLIS

    return when {
        diff < MINUTE_MILLIS -> "now"
        diff < HOUR_MILLIS -> {
            val minutes = diff / MINUTE_MILLIS
            "$minutes min${if (minutes > 1) "s" else ""}"
        }
        diff < DAY_MILLIS -> {
            val hours = diff / HOUR_MILLIS
            "$hours hr${if (hours > 1) "s" else ""}"
        }
        diff < WEEK_MILLIS -> {
            val days = diff / DAY_MILLIS
            "$days day${if (days > 1) "s" else ""}"
        }
        diff < MONTH_MILLIS -> {
            val weeks = diff / WEEK_MILLIS
            "$weeks wk${if (weeks > 1) "s" else ""}"
        }
        diff < YEAR_MILLIS -> {
            val months = diff / MONTH_MILLIS
            "$months mth${if (months > 1) "s" else ""}"
        }
        else -> {
            val years = diff / YEAR_MILLIS
            "$years yr${if (years > 1) "s" else ""}"
        }
    }
}