package com.justself.klique

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.screens.RegistrationScreen
import com.justself.klique.Authentication.ui.viewModels.AppState
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


val Pink700 = Color(0xFFFF759C)
val Pink400 = Color(0xFFC73868)
val Pink200 = Color(0xFFFCA4C2)
val LightBackground = Color(0xFFFFFFFF)
val CultPink = Color(0xFF6D1E1E)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF121212)
val LightSurface = Color(0xFFFFFFFF)
val AfacadFamily = FontFamily(
    Font(R.font.afacad_variablefont_wght, weight = FontWeight.Normal)
)
val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    displayLarge = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    ),
    displayMedium = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = Pink700,
    surface = DarkSurface,
    background = DarkBackground,
    onPrimary = Color.White,
    secondary = Pink700,
    onSecondary = CultPink
)

private val LightColorScheme = lightColorScheme(
    primary = Pink700,
    surface = LightSurface,
    background = LightBackground,
    onPrimary = Color.Black,
    secondary = CultPink,
    onSecondary = Pink700
)

@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainScreen(
    intent: Intent,
    authViewModel: AuthViewModel = viewModel(),
    userDetailsViewModel: UserDetailsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val leftDrawerState = remember { mutableStateOf(false) }
    val rightDrawerState = remember { mutableStateOf(false) }
    val appState by authViewModel.appState.collectAsState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf("") }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val messageScreenGuy = currentRoute?.startsWith("messageScreen/") == true
    val bioScreenGuy = currentRoute?.startsWith("bioScreen/") == true
    val imageViewer = currentRoute?.startsWith("fullScreenImage") == true
    val videoViewer = currentRoute?.startsWith("fullScreenVideo") == true
    val chatRoomGuy = currentRoute?.startsWith("chatRoom") == true
    var isSearchMode by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<SearchUser>()) }
    var cannotFindUser by remember {
        mutableStateOf(false)
    }
    val notificationViewModel: NotificationViewModel = viewModel()
    val notifications by notificationViewModel.notifications.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(intent) {
        val route = intent.getStringExtra("route")
        if (!route.isNullOrEmpty()) {
            Log.d("Firebase", "Route called is $route")
            navController.navigate(route)
        } else {
            Log.e("Firebase", "Route is null or empty")
        }
    }
    LaunchedEffect(Unit) {
        checkAndSendToken(context)
    }
    LaunchedEffect(searchText) {
        if (searchText.length > 2) {
            val results = userDetailsViewModel.searchUsers(searchText)
            if (results.isNotEmpty()) {
                Log.d("KliqueSearch", "Results")
                searchResults = results
            } else {
                Log.d("KliqueSearch", "Empty Results")
                cannotFindUser = true
            }
        } else {
            searchResults = emptyList()
        }
    }
    var gistStarterName by remember { mutableStateOf("") }
    var gistStarterId by remember { mutableIntStateOf(0) }
    when (appState) {
        AppState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        AppState.LoggedIn -> Scaffold(
            topBar = {
                if (!(messageScreenGuy || bioScreenGuy || imageViewer || videoViewer || chatRoomGuy)) {
                    CustomAppBar(
                        leftDrawerState,
                        rightDrawerState,
                        onRightDrawer = { notificationViewModel.markNotificationsAsSeen() },
                        isSearchMode = isSearchMode,
                        onSearchModeChange = { isSearchMode = it },
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        displayText = gistStarterName,
                        displayId = gistStarterId,
                        navController = navController,
                        cannotFindUser = { cannotFindUser = it }
                    )
                }
            },
            bottomBar = {
                if (!(imeVisible || showEmojiPicker || messageScreenGuy || bioScreenGuy || imageViewer || videoViewer || chatRoomGuy)) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            Box {
                MainContent(
                    navController,
                    innerPadding,
                    leftDrawerState,
                    rightDrawerState,
                    authViewModel,
                    userDetailsViewModel,
                    imeVisible,
                    true,
                    showEmojiPicker,
                    onEmojiPickerVisibilityChange = {
                        Log.d("EmojiVisibility", "To test visibility of: $it")
                        showEmojiPicker = it
                    },
                    onEmojiSelected = { emoji -> selectedEmoji = emoji },
                    selectedEmoji,
                    resetSelectedEmoji = { selectedEmoji = "" },
                    notificationViewModel = notificationViewModel,
                    onDisplayTextChange = { theText, userId ->
                        gistStarterName = theText; gistStarterId = userId
                    }
                )
                Log.d("KliqueSearch", "${searchResults.isNotEmpty()}, $cannotFindUser")
                if (isSearchMode && (searchResults.isNotEmpty() || cannotFindUser)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 90.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (!cannotFindUser || searchResults.isNotEmpty()) {
                            searchResults.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                        .clickable {
                                            isSearchMode = false
                                            searchResults = emptyList()
                                            navController.navigate("bioScreen/${user.userId}")
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Log.d("KliqueSearch", "UI 1")
                                    Image(
                                        painter = rememberAsyncImagePainter(user.profilePictureUrl),
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.2f
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Row {
                                        Text(
                                            text = user.userAlias,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadge(isVerified = user.isVerified)
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Cannot find user",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        AppState.LoggedOut -> {
            RegistrationScreen(navController = navController)
        }

        AppState.UpdateRequired -> {
            UpdateRequiredScreen(navController)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    leftDrawerState: MutableState<Boolean>,
    rightDrawerState: MutableState<Boolean>,
    authViewModel: AuthViewModel,
    userDetailsViewModel: UserDetailsViewModel,
    imeVisible: Boolean,
    isLoggedIn: Boolean,
    showEmojiPicker: Boolean,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    onEmojiSelected: (String) -> Unit,
    selectedEmoji: String,
    resetSelectedEmoji: () -> Unit,
    notificationViewModel: NotificationViewModel,
    onDisplayTextChange: (String, Int) -> Unit
) {
    val customerId by SessionManager.customerId.collectAsState()
    val fullName = userDetailsViewModel.name.collectAsState().value
    Log.d("Names", "Full Name: $fullName")

    val context = LocalContext.current
    val webSocketUrl = context.getString(R.string.websocket_url)
    Log.d("WebSocketURL", "WebSocket URL: $webSocketUrl")
    LaunchedEffect(key1 = customerId) {
        if (customerId != 0) {
            userDetailsViewModel.fetchCustomerDetails(customerId)
        }
    }

    LaunchedEffect(key1 = customerId, key2 = fullName) {
        if (customerId != 0) {
            Log.d(
                "WebSocket",
                "Attempting to connect to WebSocket at $webSocketUrl with customer ID $customerId"
            )
            if (!WebSocketManager.isConnected.value) {
                WebSocketManager.connect(webSocketUrl, customerId, fullName, context, "Main")
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            WebSocketManager.close()
        }
    }
    Log.d(
        "InnerPadding",
        "start: ${innerPadding.calculateStartPadding(LocalLayoutDirection.current)}, top: ${innerPadding.calculateTopPadding()}, end: ${
            innerPadding.calculateEndPadding(LocalLayoutDirection.current)
        }, bottom: ${innerPadding.calculateBottomPadding()}"
    )
    val bottomPadding = when {
        imeVisible -> 0.dp
        showEmojiPicker -> 0.dp
        else -> innerPadding.calculateBottomPadding()
    }

    val layoutDirection = LocalLayoutDirection.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = innerPadding.calculateStartPadding(layoutDirection),
                top = innerPadding.calculateTopPadding(),
                end = innerPadding.calculateEndPadding(layoutDirection),
                bottom = bottomPadding
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    leftDrawerState.value = false
                    rightDrawerState.value = false
                }
            )
    ) {
        val application = LocalContext.current.applicationContext as Application
        val contactDao = remember { DatabaseProvider.getContactsDatabase(application).contactDao() }
        val getGistStateDao =
            remember { DatabaseProvider.getGistRoomCreatedBase(application).gistStateDao() }
        val sharedCliqueViewModel: SharedCliqueViewModel = viewModel(
            factory = SharedCliqueViewModelFactory(
                application,
                customerId,
                contactDao,
                getGistStateDao
            )
        )
        var emojiPickerHeight by remember { mutableStateOf(0.dp) }
        NavigationHost(
            navController,
            isLoggedIn,
            customerId,
            fullName,
            onEmojiPickerVisibilityChange,
            selectedEmoji,
            showEmojiPicker,
            application,
            sharedCliqueViewModel,
            resetSelectedEmoji,
            onDisplayTextChange
        ) { height -> emojiPickerHeight = height }
        LeftDrawer(
            leftDrawerState,
            Modifier.align(Alignment.CenterStart),
            navController,
            customerId
        )
//        RightDrawer(
//            rightDrawerState,
//            Modifier.align(Alignment.CenterEnd),
//            notificationViewModel,
//            navController
//        )
        if (showEmojiPicker) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.BottomCenter)
            ) {
                EmojiPickerView(onEmojiSelected = {
                    Log.d("EmojiVisibility", "The emojiVisible: $it")
                    onEmojiSelected(it)
                    onEmojiPickerVisibilityChange(true)
                }, emojiPickerHeight = emojiPickerHeight)
            }
        }
    }
}

@Composable
fun CustomAppBar(
    leftDrawerState: MutableState<Boolean>,
    rightDrawerState: MutableState<Boolean>,
    onRightDrawer: () -> Unit,
    isSearchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    displayText: String,
    displayId: Int,
    navController: NavController,
    cannotFindUser: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isSearchMode,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 })
        )
        {
            TextField(
                value = searchText,
                onValueChange = {
                    onSearchTextChange(it)
                    if (it.length <= 2) {
                        cannotFindUser(false)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                placeholder = { Text("Type the alias...") },
                trailingIcon = {
                    IconButton(onClick = {
                        onSearchModeChange(false)
                        onSearchTextChange("")
                        cannotFindUser(false)
                    }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Cancel Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences)
            )
        }
        if (!isSearchMode) {
            val topPadding = Modifier.padding(top = 40.dp)
            IconButton(
                onClick = {
                    leftDrawerState.value = !leftDrawerState.value
                    rightDrawerState.value = false
                },
                modifier = topPadding
            ) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box {
                val isConnected by WebSocketManager.isConnected.collectAsState()
                Icon(
                    Icons.Filled.Wifi,
                    contentDescription = "Network Status Icon",
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = topPadding
                )
            }
            Text(
                text = displayText,
                modifier = Modifier
                    .padding(top = 40.dp, start = 10.dp)
                    .align(Alignment.CenterVertically)
                    .clickable {
                        if (displayId != 0) {
                            navController.navigate("bioScreen/$displayId")
                        }
                    },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    onSearchModeChange(true)
                    leftDrawerState.value = false
                    rightDrawerState.value = false
                },
                modifier = topPadding
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val textStyle = TextStyle(
        color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary,
        fontFamily = AfacadFamily,
        fontSize = 12.sp
    )
    val iconColor =
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Home, contentDescription = "Home", tint = iconColor) },
            label = { Text("Home", style = textStyle) },
            selected = currentRoute == "home",
            onClick = { if (currentRoute != "home") navController.navigate("home") }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Chats",
                    tint = iconColor
                )
            },
            label = { Text("Chats", style = textStyle) },
            selected = currentRoute == "chats",
            onClick = { if (currentRoute != "chats") navController.navigate("chats") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Book, contentDescription = "Bookshelf", tint = iconColor) },
            label = { Text("Bookshelf", style = textStyle) },
            selected = currentRoute == "bookshelf",
            onClick = { if (currentRoute != "bookshelf") navController.navigate("bookshelf") }
        )
    }
}

@Composable
fun EmojiPickerView(onEmojiSelected: (String) -> Unit, emojiPickerHeight: Dp) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AndroidView(
            factory = { context ->
                androidx.emoji2.emojipicker.EmojiPickerView(context).apply {
                    setOnEmojiPickedListener { emoji ->
                        onEmojiSelected(emoji.emoji)
                    }
                }
            },
            modifier = Modifier
                .height(emojiPickerHeight)
                .fillMaxWidth()
        )
    }
}

@Composable
fun VerifiedBadge(isVerified: Boolean) {
    if (isVerified) {
        Icon(
            imageVector = Icons.Default.CheckCircle, // Replace with the desired icon
            contentDescription = "Verified",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
    }
}

// Function to check if token is already sent and send if not
private fun checkAndSendToken(context: Context) {
    val sharedPreferences = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
    val token = sharedPreferences.getString("firebase_token", null)

    if (!token.isNullOrEmpty()) {
        Log.d("TokenCheck", "Token exists and has not been sent. Sending to server: $token")
        sendTokenToServer(token)
    } else if (token.isNullOrEmpty()) {
        Log.d("TokenCheck", "No token found in SharedPreferences.")
    } else {
        Log.d("TokenCheck", "Token already sent to the server.")
    }
}

// Function to send the token to the server
private fun sendTokenToServer(token: String) {
    val serviceScope = CoroutineScope(Dispatchers.IO)
    val params = mapOf("token" to token)
    serviceScope.launch {
        try {
            NetworkUtils.makeRequest("onFireBaseToken", params = params)
            // Mark the token as sent in SharedPreferences
        } catch (e: Exception) {
            Log.e("SendToken", "Failed to send token to server: ${e.message}")
        }
    }
}