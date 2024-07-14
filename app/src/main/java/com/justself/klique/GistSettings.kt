package com.justself.klique

import android.text.TextUtils.replace
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GistSettings(navController: NavController, gistId: String) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("Editable Text") }
    var editedText by remember { mutableStateOf(text) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                //.height(200.dp)
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            // Left Box with Image and Pencil Icon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter("https://images.pexels.com/photos/415829/pexels-photo-415829.jpeg"),
                    contentDescription = "Image with Pencil Icon",
                    modifier = Modifier.fillMaxWidth()
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Icon",
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }

            // Right Box with Text and Pencil Icon
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (isEditing) {
                    TextField(
                        value = editedText,
                        onValueChange = { newValue ->
                            if (newValue.length <= 150) {
                                editedText = newValue.replace("\n", "")
                            } },
                        modifier = Modifier.fillMaxWidth(1f).height(100.dp).padding(4.dp).align(alignment = Alignment.Center),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background, focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary)
                    )
                } else {
                    Text(text = text, modifier = Modifier.fillMaxSize(1f).padding(4.dp).wrapContentHeight(Alignment.CenterVertically).align(alignment = Alignment.Center), style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Icon"
                    )
                }
            }
        }

        // Save Button
        Button(
            onClick = {
                if (isEditing) {
                    text = editedText
                    isEditing = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(text = "Save")
        }

        // LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(2f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Lazy list content will be handled by you
        }
    }
}