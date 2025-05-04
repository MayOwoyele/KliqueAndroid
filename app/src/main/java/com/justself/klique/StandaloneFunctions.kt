package com.justself.klique

import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

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
object AckBatcher {
    private val messageIds = ConcurrentLinkedQueue<String>()
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private const val BATCH_DELAY_MS = 5000L

    fun add(messageId: String) {
        messageIds.add(messageId)

        if (timerJob == null) {
            timerJob = scope.launch {
                delay(BATCH_DELAY_MS)
                flush()
            }
        }
    }

    private suspend fun flush() {
        val batch = mutableListOf<String>()
        while (messageIds.isNotEmpty()) {
            messageIds.poll()?.let { batch.add(it) }
        }
        if (batch.isNotEmpty()) {
            ChatVMObject.acknowledgeMessages(batch)
        }

        timerJob = if (messageIds.isNotEmpty()) {
            scope.launch {
                delay(BATCH_DELAY_MS)
                flush()
            }
        } else {
            null
        }
    }

    fun clear() {
        messageIds.clear()
        timerJob?.cancel()
        timerJob = null
    }
}
object Logger {
    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i(tag, message)
    }

    fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.e(tag, message)
    }
}