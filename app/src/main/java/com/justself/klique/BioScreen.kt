package com.justself.klique

import ImageUtils.calculateAverageColor
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Popup
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import com.justself.klique.gists.ui.shared_composables.GistTile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

@Composable
fun BioScreen(
    enemyId: Int,
    navController: NavController,
    customerId: Int
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val contactsRepository = ContactsRepository(contentResolver, context)
    val bioViewModel: BioViewModel = viewModel(
        factory = BioViewModelFactory(contactsRepository)
    )
    val cliqueMembers by bioViewModel.theClique.collectAsState()
    val paddingFloat = 370f
    val profile by bioViewModel.profile.collectAsState()
    val bioGists by bioViewModel.gistList.collectAsState()
    val contactName by bioViewModel.checkIfContact(enemyId).observeAsState()
    var showClassSection by remember { mutableStateOf(false) }
    var expandedPostId by remember { mutableStateOf<String?>(null) }
    val postComments by bioViewModel.postComments.collectAsState()
    val animatedPadding = remember { Animatable(initialValue = paddingFloat) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val isMyProfile = enemyId == customerId
    val isOpen = remember {
        mutableStateOf(false)
    }
    val cachedMedia by GlobalEventBus.cachedMediaPaths.collectAsState()
    LaunchedEffect(key1 = Unit) {
        bioViewModel.fetchProfile(enemyId, customerId)
        bioViewModel.fetchMyGists(enemyId)
    }
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }
    var imagePainter by remember { mutableStateOf<BitmapPainter?>(null) }
    var presentScreen by remember {
        mutableStateOf(BioDisplayable.Gists)
    }

    LaunchedEffect(profile?.bioImage) {
        profile?.bioImage?.let { bioImageUrl ->
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(bioImageUrl)
                .allowHardware(false)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val drawable = result.drawable
                val bitmap = if (drawable is BitmapDrawable) {
                    drawable.bitmap
                } else {
                    drawable.toBitmap()
                }
                imagePainter = BitmapPainter(bitmap.asImageBitmap())
                val color = calculateAverageColor(bitmap)
                backgroundColor = Color(color)
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (profile != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                backgroundColor
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.05f))
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.05f))
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
                                if (imagePainter != null) {
                                    Image(
                                        painter = imagePainter!!,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RectangleShape)
                                            .padding(top = 16.dp)
                                            .background(backgroundColor)
                                            .border(
                                                1.dp,
                                                color = MaterialTheme.colorScheme.onSecondary
                                            ),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (profile!!.isVerified) {
                                    VerifiedIcon(
                                        Modifier.align(Alignment.BottomEnd),
                                        paddingFigure = 8
                                    )
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                val iconWidthDp = 24.dp
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = profile!!.fullName,
                                        style = MaterialTheme.typography.displayLarge,
                                        modifier = Modifier
                                            .padding(start = iconWidthDp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
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
                                    text = if (profile!!.bioText.length > 35 && !isBioExpanded) {
                                        "Bio: ${profile!!.bioText.take(35)}..."
                                    } else {
                                        "Bio: ${profile!!.bioText.take(35)}..."
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .clickable { isBioExpanded = !isBioExpanded },
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (!isMyProfile) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Direct Message",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.CenterVertically)
                                            .clickable {
                                                Screen.DmChatScreen.navigate(
                                                    navController,
                                                    profile!!.customerId, profile!!.fullName
                                                )
                                            },
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(40.dp))

                                if (isBioExpanded && profile!!.bioText.length > 35) {
                                    Popup(
                                        alignment = Alignment.Center,
                                        onDismissRequest = { isBioExpanded = false },
                                        content = {
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
                                                        text = profile!!.bioText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }
                                        }
                                    )
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
                                    text = "${bioViewModel.formatSpectatorCount(profile!!.seatedCount)} ${if (profile!!.seatedCount == 1) "spectator" else "spectators"}",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                if (!isMyProfile) {
                                    Button(
                                        onClick = {
                                            if (profile!!.isSpectator) {
                                                bioViewModel.leaveSeat(enemyId, customerId)
                                            } else {
                                                bioViewModel.takeSeat(enemyId, customerId)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onSecondary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (profile!!.isSpectator) "LEAVE" else "TAKE A SEAT",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
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
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    text = {
                        Text(
                            text = profile!!.classSection,
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    },
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
                LaunchedEffect(bioGists) {
                    GlobalEventBus.fetchGistBackground(bioGists)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .padding(top = animatedPadding.value.dp)
                        .fillMaxSize()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.onSecondary)

                ) {
                    Logger.d("Animated Padding", "${animatedPadding.value.dp}")
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            ScreenRow(
                                { presentScreen = BioDisplayable.Gists },
                                presentScreen = presentScreen,
                                thisScreen = BioDisplayable.Gists
                            )
                            Spacer(modifier = Modifier.weight(0.3f))
                            ScreenRow(
                                {
                                    if (!bioViewModel.hasLoadedCliqueMembers) {
                                        bioViewModel.fetchCliqueMembers(enemyId)
                                    }
                                    presentScreen = BioDisplayable.Clique
                                },
                                presentScreen = presentScreen,
                                thisScreen = BioDisplayable.Clique
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    isOpen.value = !isOpen.value
                                    val targetPadding = if (isOpen.value) 0f else paddingFloat
                                    coroutineScope.launch {
                                        animatedPadding.animateTo(
                                            targetPadding,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isOpen.value) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = if (isOpen.value) "Close details" else "Open details"
                                )
                            }
                        }
                    }
                    when (presentScreen) {
                        BioDisplayable.Gists -> {
                            items(bioGists) { gist ->
                                val mediaPaths = cachedMedia[gist.gistId]
                                GistTile(
                                    enemyId,
                                    onTap = {
                                        Screen.Home.navigate(
                                            navController, gist.gistId
                                        )
                                    },
                                    onHoldClick = { bioViewModel.floatGist(gist.gistId) },
                                    lastPostList = gist.lastGistComments,
                                    postImage = mediaPaths?.postImage,
                                    postVideo = mediaPaths?.postVideo,
                                    gist = gist
                                )
                            }
                        }

                        BioDisplayable.Clique -> {
                            items(cliqueMembers) { clique ->
                                CliqueTile(clique, navController)
                            }
                        }
                    }
                    items(profile!!.posts) { post ->
                        PostItem(
                            post = post,
                            navController = navController,
                            onViewAllComments = { postId ->
                                expandedPostId = if (expandedPostId == postId) null else postId
                            }
                        )
                    }
                }
            }
//            expandedPostId?.let { postId ->
//                bioViewModel.fetchPostComments(postId)
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(MaterialTheme.colorScheme.onSecondary)
//                ) {
//                    var commentText by remember { mutableStateOf("") }
//                    Column {
//                        var replyingTo by remember { mutableStateOf<String?>(null) }
//                        var replyingToId by remember { mutableStateOf<Int?>(null) }
//                        IconButton(onClick = { expandedPostId = null }) {
//                            Icon(
//                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
//                                contentDescription = "Back",
//                                tint = MaterialTheme.colorScheme.background
//                            )
//                        }
//                        Text(
//                            text = "Comments",
//                            style = MaterialTheme.typography.bodyLarge,
//                            modifier = Modifier.padding(bottom = 8.dp),
//                            color = MaterialTheme.colorScheme.onPrimary
//                        )
//
//                        // LazyColumn for displaying comments
//                        LazyColumn(
//                            modifier = Modifier
//                                .weight(1f)
//                                .fillMaxWidth()
//                        ) {
//                            items(postComments) { comment ->
//                                var textLayoutResult by remember {
//                                    mutableStateOf<TextLayoutResult?>(
//                                        null
//                                    )
//                                }
//                                Text(
//                                    text = buildAnnotatedString {
//                                        pushStringAnnotation(
//                                            tag = "COMMENTER",
//                                            annotation = comment.customerId.toString()
//                                        )
//                                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
//                                            append("${comment.name}: ")
//                                        }
//                                        pop()
//                                        comment.replyingTo?.let { replyingTo ->
//                                            pushStringAnnotation(
//                                                tag = "REPLYING_TO",
//                                                annotation = comment.replyingToId.toString()
//                                            )
//                                            withStyle(style = SpanStyle(color = Color.Red)) {
//                                                append("@$replyingTo ")
//                                            }
//                                            pop()
//                                        }
//                                        append(comment.text)
//                                    },
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    modifier = Modifier
//                                        .padding(bottom = 4.dp)
//                                        .pointerInput(Unit) {
//                                            detectTapGestures { tapOffset ->
//                                                textLayoutResult?.let { layoutResult ->
//                                                    val position =
//                                                        layoutResult.getOffsetForPosition(tapOffset)
//                                                    val annotations =
//                                                        layoutResult.layoutInput.text.getStringAnnotations(
//                                                            position,
//                                                            position
//                                                        )
//                                                    annotations
//                                                        .firstOrNull()
//                                                        ?.let { annotation ->
//                                                            when (annotation.tag) {
//                                                                "COMMENTER" -> navController.navigate(
//                                                                    "bioScreen/${annotation.item}"
//                                                                )
//
//                                                                "REPLYING_TO" -> navController.navigate(
//                                                                    "bioScreen/${annotation.item}"
//                                                                )
//                                                            }
//                                                        }
//                                                }
//                                            }
//                                        },
//                                    onTextLayout = { layoutResult ->
//                                        textLayoutResult = layoutResult
//                                    },
//                                    color = MaterialTheme.colorScheme.onPrimary,
//                                )
//                                Text(
//                                    text = "Reply ${comment.name}",
//                                    style = MaterialTheme.typography.bodyLarge,
//                                    color = Color.Red,
//                                    modifier = Modifier
//                                        .padding(start = 20.dp)
//                                        .clickable {
//                                            replyingTo = comment.name
//                                            replyingToId = comment.customerId
//                                            commentText = ""
//                                        }
//                                )
//                            }
//                        }
//
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(horizontal = 4.dp, vertical = 4.dp)
//                                .imePadding(),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Box(
//                                modifier = Modifier
//                                    .background(
//                                        MaterialTheme.colorScheme.onSecondary,
//                                        shape = RoundedCornerShape(4.dp)
//                                    )
//                                    .weight(1f)
//                            ) {
//
//                                TextField(
//                                    value = (replyingTo?.let { "$it " } ?: "") + commentText,
//                                    onValueChange = { newText ->
//                                        val sanitizedText = newText.replace("\n", "")
//                                        if (replyingTo != null) {
//                                            val prefix = "$replyingTo "
//                                            if (!sanitizedText.startsWith(prefix)) {
//                                                replyingTo = null
//                                                replyingToId = null
//                                                commentText = sanitizedText.trimStart()
//                                            } else {
//                                                commentText = sanitizedText.removePrefix(prefix)
//                                            }
//                                        } else {
//                                            commentText = sanitizedText
//                                        }
//                                    },
//                                    placeholder = { Text("Add a comment...") },
//                                    modifier = Modifier
//                                        .background(MaterialTheme.colorScheme.onSecondary)
//                                        .fillMaxWidth(),
//                                    maxLines = 1,
//                                    colors = TextFieldDefaults.colors(
//                                        focusedIndicatorColor = Color.Transparent,
//                                        unfocusedIndicatorColor = Color.Transparent,
//                                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
//                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
//                                        cursorColor = MaterialTheme.colorScheme.primary,
//                                    )
//                                )
//                            }
//                            IconButton(onClick = {
//                                if (commentText.isNotEmpty()) {
//                                    val comment = JSONObject().apply {
//                                        put("replyingToId", replyingToId)
//                                        put("replyingTo", replyingTo)
//                                        put("postId", postId)
//                                        put("userId", customerId)
//                                        put("commentText", commentText)
//                                    }.toString()
//                                    bioViewModel.sendComment(
//                                        comment,
//                                        customerId,
//                                        commentText,
//                                        replyingTo,
//                                        replyingToId,
//                                        postId
//                                    )
//                                }
//                                commentText = ""
//                                replyingTo = null
//                                replyingToId = null
//                            }) {
//                                Icon(
//                                    imageVector = Icons.AutoMirrored.Filled.Send,
//                                    contentDescription = "Send",
//                                    tint = MaterialTheme.colorScheme.background
//                                )
//                            }
//                        }
//                    }
//                }
//            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                var message by remember { mutableStateOf("Loading Profile") }
                LaunchedEffect(key1 = Unit) {
                    delay(5000)
                    message =
                        "This is taking a bit too long, perhaps you should check on your network?"
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        if (expandedPostId == null) {
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
    val screenWidth =
        LocalContext.current.resources.displayMetrics.widthPixels.dp / LocalDensity.current.density
    val videoHeight = screenWidth / aspectRatio
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
    Column(modifier = Modifier) {
        Logger.d("logging states", "$isPlaying, $isVideoCached")
        when (post.type) {
            "image" -> {
                Image(
                    painter = rememberAsyncImagePainter(post.content),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .width(400.dp)
                        .padding(vertical = 16.dp),
                    contentScale = ContentScale.Crop
                )
            }

            "text" -> {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }

            "video" -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(videoHeight)
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
                        .padding(vertical = 16.dp)
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
                                    mediaPlayerInstance?.setVolume(0f, 0f)
                                    isMuted = true
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
                        Image(
                            painter = rememberAsyncImagePainter(post.thumbnail),
                            contentDescription = "Video thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        if (painterState is AsyncImagePainter.State.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

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
        }
        Text(
            text = if (post.totalComments == 0) "No comments" else "View all ${post.totalComments} comments",
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
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { navController.navigate("bioScreen/${comment.customerId}") }
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

enum class BioDisplayable(val text: String) {
    Gists("Gists"),
    Clique("Clique")
}

@Composable
fun ScreenRow(
    onClick: () -> Unit,
    presentScreen: BioDisplayable,
    thisScreen: BioDisplayable
) {
    val backgroundColor = if (presentScreen == thisScreen) {
        MaterialTheme.colorScheme.background
    } else {
        Color.Transparent
    }
    Text(
        text = thisScreen.text,
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.displayLarge,
        modifier = Modifier
            .clickable { onClick() }
            .background(backgroundColor, shape = RoundedCornerShape(10.dp))
            .padding(10.dp)
    )
}

@Composable
fun CliqueTile(person: Clique, navController: NavController) {
    val profileImage = NetworkUtils.fixLocalHostUrl(person.profileImage)
    Row(modifier = Modifier
        .clickable { Screen.BioScreen.navigate(navController, person.userId) }
        .padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(profileImage, 40.dp)
        Text(
            person.name,
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        VerifiedBadge(person.isVerified, MaterialTheme.colorScheme.onPrimary)
    }
}