package com.justself.klique

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ProductCommentsScreen(productId: Int, commentViewModel: CommentsViewModel = viewModel(), navController: NavController, customerId: Int) {
    // Ensure comments are fetched when Composable is called
    LaunchedEffect(productId) {
        commentViewModel.fetchComments(productId)
    }

    val comments by commentViewModel.comments.observeAsState(emptyList())
    val error by commentViewModel.error.observeAsState("")
    val (commentText, setCommentText) = remember { mutableStateOf("") }

    Column {
        CustomTopBar(navController, "Comments for Product $productId")
        if (comments.isNotEmpty()) {
            comments.forEach { comment ->
                // Displaying commenter's name and comment
                Text("${comment.lastName}: ${comment.comment}", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (error.isNotEmpty()) {
            Text("Failed to load comments: $error", style = MaterialTheme.typography.bodyLarge)
        } else {
            Text("No comments yet", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = commentText,
            onValueChange = setCommentText,
            label = { Text("Add a comment", style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                // Create NewComment object
                val newComment = NewComment(
                    productId = productId,
                    customerId = customerId,  // Assuming you pass the customerId somehow (maybe from a ViewModel or saved state)
                    comment = commentText
                )
                commentViewModel.addComment(newComment)
                setCommentText("")  // Clear the input box after sending the comment
            },
            modifier = Modifier.align(Alignment.End).padding(8.dp)
        ) {
            Text("Send")
        }
    }
}

@Composable
fun CustomTopBar(navController: NavController, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height(56.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
