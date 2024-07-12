package com.justself.klique

import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

@Composable
fun BioScreen(customerId: Int, navController: NavController, bioViewModel: BioViewModel = viewModel()) {
    val profile by bioViewModel.fetchProfile(customerId).observeAsState()

    profile?.let { profileData ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Profile Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(profileData.backgroundColor)
                    .padding(0.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .background(profileData.backgroundColor)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(profileData.bioImage),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RectangleShape)
                            .padding(16.dp)
                            .border(1.dp, color = MaterialTheme.colorScheme.primary),
                        contentScale = ContentScale.Crop
                    )

                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.onSecondary)

                ) {
                    item {
                        // Name and Conditional "Saved as" Text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = profileData.fullName,
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            if (profileData.isContact) {
                                Text(
                                    text = "Saved as Nancy Isime Chukwuka",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    item {
                        var isBioExpanded by remember { mutableStateOf(false) }
                        Box {
                            Text(
                                text = if (profileData.bioText.length > 35 && !isBioExpanded) {
                                    "Bio: ${profileData.bioText.take(35)}..."
                                } else {
                                    "Bio: ${profileData.bioText.take(35)}..."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { isBioExpanded = !isBioExpanded }
                            )

                            if (isBioExpanded && profileData.bioText.length > 35) {
                                Popup(
                                    alignment = Alignment.Center,
                                    onDismissRequest = { isBioExpanded = false }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable { isBioExpanded = false }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(16.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.Gray)
                                                .padding(16.dp)
                                        ) {
                                            Text(
                                                text = profileData.bioText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(profileData.posts) { post ->
                        PostItem(post = post)
                    }
                }
            }

            // Scrollable Content

        }
    }
}

@Composable
fun PostItem(post: Post) {
    Column(modifier = Modifier.padding(16.dp)) {
        when (post.type) {
            "image" -> {
                Image(
                    painter = rememberAsyncImagePainter(post.content),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .width(400.dp),
                    contentScale = ContentScale.Crop
                )
            }
            "text" -> {
                Text(text = post.content, style = MaterialTheme.typography.displayLarge)
            }
            // Handle other types (audio, video) if needed
        }
        Text(
            text = "View all ${post.totalComments} comments",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        post.topComments.forEach { comment ->
            Text(text = comment, style = MaterialTheme.typography.bodyLarge)
        }
    }
}