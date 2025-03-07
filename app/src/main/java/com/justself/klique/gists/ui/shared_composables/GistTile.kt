package com.justself.klique.gists.ui.shared_composables

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.SessionManager

@Composable
fun GistTile(
    gistId: String,
    customerId: Int,
    title: String,
    description: String,
    image: String,
    activeSpectators: Int,
    onTap: () -> Unit,
    onHoldClick: (() -> Unit)? = null,
    lastPostList: List<LastGistComments>
) {
    val roundedCornerShape = RoundedCornerShape(20.dp)
    LaunchedEffect(Unit) {
        Log.d("ImageAsync", image)
    }
    var isDialogVisible by remember { mutableStateOf(false) }
    Surface(modifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    onTap()
                },
                onLongPress = {
                    if (onHoldClick != null && customerId == SessionManager.customerId.value) {
                        isDialogVisible = true
                    }
                }
            )
        }
        .height(310.dp)
        .border(
            1.dp,
            MaterialTheme.colorScheme.onPrimary,
            roundedCornerShape
        )
        .clip(roundedCornerShape)
    ) {
        Column {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Surface(
                    modifier = Modifier
                        .size((150).dp)
                        .padding(vertical = 8.dp, horizontal = 8.dp)
                        .clip(CircleShape.copy(CornerSize(150.dp)))
//                        .weight(5F)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(image),
                        contentDescription = "",
                        contentScale = ContentScale.Crop
                    )
                }

                Column(/*modifier = Modifier.weight(9F)*/) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 17.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier.weight(4F)
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier.weight(4.5F)
                    )
                    Text(
                        text = "Active Spectators: $activeSpectators",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.weight(1.5F)
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .weight(10F)
            ) {
                Text("..recent posts", color = MaterialTheme.colorScheme.onPrimary)
                lastPostList.forEach { comment ->
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(3.dp)
                            .clip(shape = RoundedCornerShape(5.dp))
                            .background(Color.Gray)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "${comment.senderName}: ${comment.comment}",
                            color = MaterialTheme.colorScheme.background,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        if (isDialogVisible) {
            Dialog(onDismissRequest = { isDialogVisible = false }) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Actions", style = MaterialTheme.typography.displayLarge)

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            onHoldClick?.invoke()
                            isDialogVisible = false
                        }) {
                            Text("Float Gist")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(onClick = { isDialogVisible = false }) {
                            Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

data class LastGistComments(
    val senderName: String,
    val comment: String,
    val userId: Int
)