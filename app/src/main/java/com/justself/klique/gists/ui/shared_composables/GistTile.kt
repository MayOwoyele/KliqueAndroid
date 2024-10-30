package com.justself.klique.gists.ui.shared_composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter

@Composable
fun GistTile(
    gistId: String,
    customerId: Int,
    title: String,
    description: String,
    image: String,
    activeSpectators: Int,
    onTap: () -> Unit,
    onHoldClick: (() -> Unit)? = null
) {
    var isDialogVisible by remember { mutableStateOf(false)}
    Surface(modifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    onTap()
                },
                onLongPress = {
                    if (onHoldClick != null) {
                        isDialogVisible = true
                    }
                }
            )
        }
        .height(141.dp)
        .border(
            1.dp,
            MaterialTheme.colorScheme.onPrimary,
            RoundedCornerShape(bottomStart = 50.dp)
        )
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {

            Surface(
                modifier = Modifier
                    .size((150).dp)
                    .padding(vertical = 8.dp, horizontal = 8.dp)
                    .clip(CircleShape.copy(CornerSize(150.dp)))
                    .weight(5F)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(image),
                    contentDescription = "",
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(9F)) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 17.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(4F)
                )
                Text(
                    text = "Description: $description",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(4.5F)
                )

                Text(
                    text = "Active Spectators: $activeSpectators",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1.5F)
                )
//                Button(onClick = { /*TODO*/ }) {

//                    Text(text = "View Gist")
//                }
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