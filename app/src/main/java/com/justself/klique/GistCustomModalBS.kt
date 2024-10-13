package com.justself.klique

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CustomBottomSheet(
    visible: Boolean, onDismissRequest: () -> Unit, content: @Composable () -> Unit
) {
    if (visible) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background scrim to dismiss the bottom sheet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f))
                    .clickable(onClick = onDismissRequest)
            )

            // Bottom sheet content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(
                        MaterialTheme.colorScheme.background
                    )
            ) {
                content()
            }
        }
    }
}


@Composable
fun CommentSection(viewModel: SharedCliqueViewModel, navController: NavController, customerId: Int) {
    var showRepliesForCommentId by remember { mutableStateOf<String?>(null) }
    var showKCDonationDialog by remember { mutableStateOf(false) }
    val comments by viewModel.comments.collectAsState()
    val listState = rememberLazyListState()
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var lastCommentId by remember { mutableStateOf<String?>(null)}
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 56.dp, bottom = 56.dp)
        ) {
            if (showRepliesForCommentId == null) {
                items(comments) { comment ->
                    CommentItem(
                        comment = comment,
                        onReplyClick = { showRepliesForCommentId = comment.id },
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            } else {
                val selectedComment = comments.find { it.id == showRepliesForCommentId }
                selectedComment?.replies?.let { replies ->
                    items(replies) { reply ->
                        ReplyItem(reply = reply, navController = navController)
                    }
                }
            }
        }
        LaunchedEffect(listState) {
            snapshotFlow { listState.layoutInfo }
                .map { layoutInfo ->
                    val isScrollable = layoutInfo.totalItemsCount > layoutInfo.visibleItemsInfo.size
                    val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    if (isScrollable) {
                        lastVisibleItemIndex
                    } else {
                        null
                    }
                }
                .distinctUntilChanged().filter { it == comments.size - 1 }.collect {
                    if (!loading) {
                        loading = true
                        coroutineScope.launch {
                            viewModel.fetchGistComments(true, lastCommentId)
                            loading = false
                        }
                    }
                }
        }
        LaunchedEffect(comments.size) {
            if (comments.isNotEmpty()){
                lastCommentId = comments.last().id
            }
        }

        // Top row with topic and wallet icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showRepliesForCommentId != null) {
                IconButton(onClick = { showRepliesForCommentId = null }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
            Text(if (showRepliesForCommentId == null) "Comments" else "Replies")
            IconButton(onClick = { showKCDonationDialog = true }) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet, contentDescription = "Wallet"
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background) // Optional: background to distinguish the input area
                .padding(1.dp)
                .align(Alignment.BottomCenter)
                .imePadding(), // Align at the bottom
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val text = remember { mutableStateOf("") }
            TextField(
                value = text.value,
                onValueChange = { text.value = it },
                modifier = Modifier
                    .weight(1f)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.onPrimary),
                placeholder = { Text("Add a comment") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
            Log.d("text", text.value)
            IconButton(onClick = {
                if (text.value.isNotEmpty()) {
                    viewModel.sendGistComment(
                        text.value,
                        showRepliesForCommentId != null,
                        showRepliesForCommentId,
                        userId = customerId
                    )
                }
            }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send icon")
            }
        }
        if (showKCDonationDialog) {
            KCDialog(onDismissRequest = { showKCDonationDialog = false }, onDonate = {})
        }
    }
}


@Composable
fun CommentItem(
    comment: GistComment,
    onReplyClick: () -> Unit,
    viewModel: SharedCliqueViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = comment.fullName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { navController.navigate("bioScreen/${comment.customerId}") })
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = comment.comment, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))

        // Thumbs up icon below the comment
        Text(text = "👍 ${comment.upVotes}",
            modifier = Modifier
                .clickable { viewModel.sendUpVotes(comment.id) }
                .align(Alignment.Start)
        )

        if (comment.replies.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${comment.replies.size} replies",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onReplyClick)
            )
        }
    }
}

@Composable
fun ReplyItem(reply: Reply, navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = reply.fullName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { navController.navigate("bioScreen/${reply.customerId}") })
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = reply.reply, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun KCDialog(onDismissRequest: () -> Unit, onDonate: (Int) -> Unit) {
    var kcAmount by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Throw Klique Coins", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (kcAmount > 0) kcAmount-- }) {
                        Text("-", style = MaterialTheme.typography.bodyLarge)
                    }
                    TextField(
                        value = if (kcAmount > 0) kcAmount.toString() else "",
                        onValueChange = { kcAmount = it.toIntOrNull() ?: 0 },
                        modifier = Modifier.width(100.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true,
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    IconButton(onClick = { kcAmount++ }) {
                        Text("+", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    onDonate(kcAmount)
                    onDismissRequest()
                }) {
                    Text("Throw Coins")
                }
            }
        }
    }
}