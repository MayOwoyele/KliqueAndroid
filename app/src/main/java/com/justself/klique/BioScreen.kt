package com.justself.klique

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.navigation.NavController
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

@Composable
fun BioScreen(
    enemyId: Int,
    navController: NavController
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val contactsRepository = ContactsRepository(contentResolver, context)
    val bioViewModel: BioViewModel = viewModel(
        factory = BioViewModelFactory(contactsRepository)
    )
    val profile by bioViewModel.fetchProfile(enemyId).observeAsState()
    val contactName by bioViewModel.checkIfContact(enemyId).observeAsState()

    var showClassSection by remember { mutableStateOf(false) }
    var expandedPostId by remember { mutableStateOf<String?>(null) }
    val postComments by bioViewModel.fetchPostComments(expandedPostId ?: "")
        .observeAsState(emptyList())
    val animatedPadding = remember { Animatable(initialValue = 370f) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, scrollOffset) ->
                val targetPadding = if (index > 0 || scrollOffset > 0) 0f else 380f
                coroutineScope.launch {
                    animatedPadding.animateTo(
                        targetPadding,
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            }
    }

    profile?.let { profileData ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Profile Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(profileData.backgroundColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .wrapContentSize()
                                .align(Alignment.TopCenter)
                                .padding(bottom = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(profileData.bioImage),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RectangleShape)
                                        .padding(top = 16.dp)
                                        .background(profileData.backgroundColor)
                                        .border(1.dp, color = MaterialTheme.colorScheme.primary),
                                    contentScale = ContentScale.Crop
                                )

                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                val iconWidthDp = 24.dp // Actual icon width, commonly 24 dp
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Centered Name Text with Padding for Icon
                                    Text(
                                        text = profileData.fullName,
                                        style = MaterialTheme.typography.displayLarge,
                                        modifier = Modifier
                                            .padding(start = iconWidthDp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )

                                    // Icon placed after the text
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Information",
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.clickable { showClassSection = true }
                                    )
                                }
                                if (contactName != null) {
                                    Text(
                                        text = "Saved as $contactName",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        text = "Not your contact",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            var isBioExpanded by remember { mutableStateOf(false) }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (profileData.bioText.length > 35 && !isBioExpanded) {
                                        "Bio: ${profileData.bioText.take(35)}..."
                                    } else {
                                        "Bio: ${profileData.bioText.take(35)}..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clickable { isBioExpanded = !isBioExpanded },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Direct Message",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.CenterVertically)
                                        .clickable { },
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(40.dp))

                                if (isBioExpanded && profileData.bioText.length > 35) {
                                    Popup(
                                        alignment = Alignment.Center,
                                        onDismissRequest = { isBioExpanded = false }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(180.dp)
                                                .background(Color.Black.copy(alpha = 0.5f))
                                                .clickable { isBioExpanded = false }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .padding(16.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.onSecondary)
                                                    .padding(16.dp)
                                            ) {
                                                Text(
                                                    text = profileData.bioText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${bioViewModel.formatSpectatorCount(profileData.seatedCount)} spectators",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Button(
                                    onClick = {
                                        if (profileData.isSpectator) {
                                            bioViewModel.leaveSeat() // Call this function if the user is a spectator
                                        } else {
                                            bioViewModel.takeSeat() // Call this function if the user is not a spectator
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (profileData.isSpectator) "LEAVE" else "TAKE A SEAT",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (showClassSection) {
                AlertDialog(
                    onDismissRequest = { showClassSection = false },
                    title = {
                        Text(
                            text = "Class Information",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    text = { Text(text = profileData.classSection) },
                    confirmButton = {
                        Button(onClick = { showClassSection = false }) {
                            Text("OK")
                        }
                    }
                )
            }
            Box(
                modifier = Modifier
                    .wrapContentSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(top = animatedPadding.value.dp)
                        .fillMaxSize()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.onSecondary)

                ) {
                    Log.d("Animated Padding", "${animatedPadding.value.dp}")
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
                        }
                    }

                    items(profileData.posts) { post ->
                        PostItem(
                            post = post,
                            navController = navController,
                            onViewAllComments = { postId ->
                                expandedPostId = if (expandedPostId == postId) null else postId
                            }
                        )
                    }
                }
                IconButton(
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            expandedPostId?.let {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSecondary)
                ) {
                    var commentText by remember { mutableStateOf("") }
                    Column {
                        // Back arrow icon to close the comments section
                        IconButton(onClick = { expandedPostId = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = "Comments",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(bottom = 8.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                        // LazyColumn for displaying comments
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            items(postComments) { comment ->
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = Color.Black)) {
                                            append("${comment.name}: ")
                                        }
                                        append(comment.text)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .padding(bottom = 4.dp)
                                        .clickable { navController.navigate("bioScreen/${comment.customerId}") },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        // Input field and send button at the bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                                .imePadding(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.onSecondary,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .weight(1f)
                            ) {
                                TextField(
                                    value = commentText,
                                    onValueChange = { newText ->
                                        if (newText.length <= 500) {
                                            commentText = newText
                                        }
                                    },
                                    placeholder = { Text("Add a comment...") },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.onSecondary)
                                        .fillMaxWidth(),
                                    maxLines = 1,
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        cursorColor = MaterialTheme.colorScheme.primary,
                                    )
                                )
                            }
                            IconButton(onClick = {
                                bioViewModel.sendComment(commentText)
                                commentText = ""
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(post: Post, navController: NavController, onViewAllComments: (String) -> Unit) {
    val context = LocalContext.current
    val videoCacheDir = File(context.cacheDir, "video_cache")
    val audioCacheDir = File(context.cacheDir, "audio_cache")
    val audioCacheFile = File(audioCacheDir, "${post.id.hashCode()}.mp3")
    val videoCacheFile = File(videoCacheDir, "${post.id.hashCode()}.mp4")
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var isDownloaded by remember {
        mutableStateOf(audioCacheFile.exists())
    }
    var isMuted by remember { mutableStateOf(false) }
    var isVideoCached by remember { mutableStateOf(videoCacheFile.exists()) }
    val videoView = remember { VideoView(context) }
    var mediaPlayerInstance: MediaPlayer? by remember { mutableStateOf(null) }
    val painter = rememberAsyncImagePainter(model = post.thumbnail)
    val painterState = painter.state
    // Calculate aspect ratio based on the image's intrinsic size
    val aspectRatio = if (painterState is AsyncImagePainter.State.Success) {
        val size = painterState.painter.intrinsicSize
        if (size.width > 0 && size.height > 0) size.width / size.height else 1f
    } else 1f
    LaunchedEffect(Unit) {
        if (!videoCacheDir.exists()) videoCacheDir.mkdirs()
        if (!audioCacheDir.exists()) audioCacheDir.mkdirs()
        if (!isVideoCached) {
            val success = downloadVideoFile(post.content, videoCacheFile)
            isVideoCached = success
        }
    }
    LaunchedEffect(Unit) {
        if (!isVideoCached) {
            val success = downloadVideoFile(post.content, videoCacheFile)
            if (success) {
                isVideoCached = true
            } else {
                Log.e("PostItem", "Failed to download video file")
            }
        }
    }
    // This launched effect is for calculating aspect ratio
    DisposableEffect(Unit) {
        onDispose {
            videoView.stopPlayback()
            isPlaying = false
        }
    }
    LaunchedEffect(isVideoCached) {
        if (isVideoCached) {
            val videoUri = Uri.fromFile(videoCacheFile)
            videoView.setVideoURI(videoUri)
            videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = true
                mediaPlayerInstance = mediaPlayer
                isBuffering = false
                videoView.start()
                isPlaying = true
                mediaPlayer.setVolume(if (isMuted) 0f else 1f, if (isMuted) 0f else 1f)
            }
            videoView.setOnCompletionListener { isPlaying = false }
            videoView.setOnInfoListener { _, what, _ ->
                isBuffering = (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                true
            }
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Log.d("logging states", "$isPlaying, $isVideoCached")
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

            "video" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .clickable {
                            val videoUriString = if (isVideoCached) {
                                Uri
                                    .fromFile(videoCacheFile)
                                    .toString()
                            } else {
                                post.content
                            }
                            navController.navigate("fullScreenVideo/${Uri.encode(videoUriString)}")
                        }
                ) {
                    if (isVideoCached) {
                        // Ensure the video view is prepared and plays automatically
                        AndroidView(
                            factory = {
                                videoView.apply {
                                    setVideoURI(Uri.fromFile(videoCacheFile))
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { view ->
                                if (!view.isPlaying) {
                                    view.start()
                                    isPlaying = true
                                }
                            }
                        )
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                mediaPlayerInstance?.setVolume(
                                    if (isMuted) 0f else 1f,
                                    if (isMuted) 0f else 1f
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                    shape = CircleShape
                                )
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        // Show thumbnail if video is not cached
                        Image(
                            painter = rememberAsyncImagePainter(post.thumbnail),
                            contentDescription = "Video thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Play icon overlay when the video is not playing
                        if (!isPlaying) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(60.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                                        CircleShape
                                    )
                                    .padding(16.dp)
                                    .clickable {
                                        videoView.start()
                                        isPlaying = true
                                    },
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            "audio" -> {
                if (!isDownloaded) {
                    LaunchedEffect(Unit) {
                        val success = downloadAudioFile(post.content, audioCacheFile)
                        if (success) {
                            isDownloaded = true
                        } else {
                            Log.e("PostItem", "Failed to download audio file")
                        }
                    }
                }
                if (isDownloaded) {
                    CustomAudioPlayer(audioUri = Uri.fromFile(audioCacheFile))
                } else {
                    Text(
                        "Downloading status audio...",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            // Handle other types (audio, video) if needed
        }
        Text(
            text = "View all ${post.totalComments} comments",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(vertical = 8.dp)
                .clickable { onViewAllComments(post.id) }
        )
        post.topComments.forEach { comment ->
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.background)) {
                        append("${comment.name}: ")
                    }
                    append(comment.text)
                },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { navController.navigate("bioScreen/${comment.customerId}") }
            )
        }
    }
}

suspend fun downloadAudioFile(url: String, destination: File): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = URL(url).openStream()
            FileOutputStream(destination).use { output ->
                inputStream.copyTo(output)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

suspend fun downloadVideoFile(url: String, destination: File): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = URL(url).openStream()
            FileOutputStream(destination).use { output ->
                inputStream.copyTo(output)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}