package com.justself.klique

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.screens.RegistrationScreen
import com.justself.klique.Authentication.ui.viewModels.AppState
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

@Composable
fun MainScreen(
    authViewModel: AuthViewModel = viewModel(),
    userDetailsViewModel: UserDetailsViewModel = viewModel(),
    navController: NavHostController
) {
    var navigateToClique by remember { mutableStateOf(false) }
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
    var snackBarVisible by remember { mutableStateOf(false) }
    var currentSnackBarData by remember { mutableStateOf<SnackBarMessageData?>(null) }

    LaunchedEffect(Unit) {
        GlobalEventBus.snackBarEvent.collect { messageData ->
            currentSnackBarData = messageData
            snackBarVisible = true
            Logger.d("SnackBar", "Broadcast received")
        }
    }
    val context = LocalContext.current
    val navigationRoute by NotificationIntentManager.navigationRoute.collectAsState()
    LaunchedEffect(navigationRoute) {
        Logger.d("Navigated", "Navigated")
        if (navigationRoute != null) {
            delay(1000)
            val navigationLambda =
                { navController.navigate(navigationRoute!!) }
            NotificationIntentManager.executeNavigation(navigationLambda)
        }
    }
    LaunchedEffect(Unit) {
        checkAndSendToken(context)
    }
    LaunchedEffect(searchText) {
        if (searchText.length > 2) {
            val results = userDetailsViewModel.searchUsers(searchText)
            if (results.isNotEmpty()) {
                Logger.d("KliqueSearch", "Results")
                searchResults = results
            } else {
                Logger.d("KliqueSearch", "Empty Results")
                cannotFindUser = true
            }
        } else {
            searchResults = emptyList()
        }
    }
    val mainScreenTopPadding = 90.dp
    var gistStarterName by remember { mutableStateOf("") }
    var gistStarterId by remember { mutableIntStateOf(0) }
    val contactsOffloaded by SessionManager.contactsOffloadedFlow.collectAsState()
    when {
        appState == AppState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        appState == AppState.LoggedIn && !contactsOffloaded -> {
            OffloadContactsScreen { navigateToClique = true }
        }

        appState == AppState.LoggedIn -> Scaffold(
            topBar = {
                if (!(messageScreenGuy || bioScreenGuy || imageViewer || videoViewer || chatRoomGuy)) {
                    CustomAppBar(
                        leftDrawerState,
                        rightDrawerState,
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
                    userDetailsViewModel,
                    imeVisible,
                    showEmojiPicker,
                    onEmojiPickerVisibilityChange = {
                        Logger.d("EmojiVisibility", "To test visibility of: $it")
                        showEmojiPicker = it
                    },
                    onEmojiSelected = { emoji -> selectedEmoji = emoji },
                    selectedEmoji,
                    resetSelectedEmoji = { selectedEmoji = "" },
                    onDisplayTextChange = { theText, userId ->
                        gistStarterName = theText; gistStarterId = userId
                    },
                    navigateToClique
                )
                Logger.d("KliqueSearch", "${searchResults.isNotEmpty()}, $cannotFindUser")
                if (isSearchMode && (searchResults.isNotEmpty() || cannotFindUser)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = mainScreenTopPadding)
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
                                    Logger.d("KliqueSearch", "UI 1")
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
                IncomingMessageSnackBar(
                    messageData = currentSnackBarData,
                    visible = snackBarVisible,
                    onDismiss = { snackBarVisible = false },
                    mainScreenTopPadding,
                    onClick = { snackBarData ->
                        Screen.MessageScreen.navigate(navController, enemyId = snackBarData.enemyId)
                        snackBarVisible = false
                    }
                )
            }
        }

        appState == AppState.LoggedOut -> {
            RegistrationScreen()
        }

//        AppState.UpdateRequired -> {
//            UpdateRequiredScreen(navController)
//        }
    }
}

@Composable
fun MainContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    leftDrawerState: MutableState<Boolean>,
    rightDrawerState: MutableState<Boolean>,
    userDetailsViewModel: UserDetailsViewModel,
    imeVisible: Boolean,
    showEmojiPicker: Boolean,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    onEmojiSelected: (String) -> Unit,
    selectedEmoji: String,
    resetSelectedEmoji: () -> Unit,
    onDisplayTextChange: (String, Int) -> Unit,
    navigateToClique: Boolean
) {
    val customerId by SessionManager.customerId.collectAsState()
    val fullName by SessionManager.fullName.collectAsState()
    Logger.d("Names", "Full Name: $fullName")
    val context = LocalContext.current
    val webSocketUrl = context.getString(R.string.websocket_url)
//    val lifecycleOwner = LocalLifecycleOwner.current
    Logger.d("WebSocketURL", "WebSocket URL: $webSocketUrl")
    LaunchedEffect(key1 = customerId) {
        if (customerId != 0) {
            userDetailsViewModel.fetchCustomerDetails(customerId)
        }
    }

    LaunchedEffect(key1 = customerId, key2 = fullName) {
        if (customerId != 0) {
            Logger.d(
                "WebSocket",
                "Attempting to connect to WebSocket at $webSocketUrl with customer ID $customerId"
            )
            if (!WebSocketManager.isConnected.value) {
                WebSocketManager.connect(customerId, fullName, context, "Main")
            }
            SessionManager.sendDeviceTokenToServer()
        }
    }
    LaunchedEffect(Unit){
        if (navigateToClique) {
            Screen.Clique.navigate(
                navController,
                CliqueScreenState.MY_CLIQUE.title
            )
        }
    }
//    DisposableEffect(lifecycleOwner) {
//        val observer = LifecycleEventObserver { _, event ->
//            when (event) {
//                Lifecycle.Event.ON_STOP -> {
//                    if (WebSocketManager.isConnected.value) {
//                        WebSocketManager.close()
//                        Logger.d("LifecycleEvent", "App moved to background. WebSocket closed.")
//                    }
//                }
//
//                Lifecycle.Event.ON_START -> {
//                    if (customerId != 0 && fullName.isNotBlank() && !WebSocketManager.isConnected.value) {
//                        WebSocketManager.connect(
//                            customerId,
//                            fullName,
//                            context,
//                            "Main"
//                        )
//                        Logger.d("LifecycleEvent", "App moved to foreground. WebSocket reconnected.")
//                    }
//                }
//
//                else -> Unit
//            }
//        }
//        lifecycleOwner.lifecycle.addObserver(observer)
//
//        onDispose {
//            lifecycleOwner.lifecycle.removeObserver(observer)
//            WebSocketManager.close()
//            Logger.d("onDispose", "WebSocket closed. Composable disposed.")
//        }
//    }
    Logger.d(
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
            true,
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
        if (showEmojiPicker) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.BottomCenter)
            ) {
                EmojiPickerView(onEmojiSelected = {
                    Logger.d("EmojiVisibility", "The emojiVisible: $it")
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
    Logger.d("CurrentRoute", "The current route is: $currentRoute")
    val unreadMessages by GlobalEventBus.unreadMessageCount.collectAsState()
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onPrimary,
        fontFamily = AfacadFamily,
        fontSize = 12.sp
    )
    val iconColor = Color.Gray

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = "Home",
                    tint = if (currentRoute == Screen.Home.route) MaterialTheme.colorScheme.primary else iconColor
                )
            },
            label = { Text("Home", style = textStyle) },
            selected = currentRoute == Screen.Home.route,
            onClick = { if (currentRoute != Screen.Home.route) Screen.Home.navigate(navController) },
        )
        NavigationBarItem(
            icon = {
                Box {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chats",
                        tint = if (currentRoute == Screen.Chats.route) MaterialTheme.colorScheme.primary else iconColor
                    )
                    if (unreadMessages > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.background,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadMessages > 99) "99+" else unreadMessages.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            },
            label = { Text("Chats", style = textStyle) },
            selected = currentRoute == Screen.Chats.route,
            onClick = { if (currentRoute != Screen.Chats.route) Screen.Chats.navigate(navController) }
        )
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Filled.Book,
                    contentDescription = "Bookshelf",
                    tint = if (currentRoute == Screen.Bookshelf.route) MaterialTheme.colorScheme.primary else iconColor
                )
            },
            label = { Text("Bookshelf", style = textStyle) },
            selected = currentRoute == Screen.Bookshelf.route,
            onClick = {
                if (currentRoute != Screen.Bookshelf.route) Screen.Bookshelf.navigate(
                    navController
                )
            },
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
fun VerifiedBadge(isVerified: Boolean, color: Color = Color.Blue) {
    if (isVerified) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Verified",
            tint = color,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun IncomingMessageSnackBar(
    messageData: SnackBarMessageData?,
    visible: Boolean,
    onDismiss: () -> Unit,
    topScreenPadding: Dp,
    onClick: (SnackBarMessageData) -> Unit,
) {
    LaunchedEffect(Unit) {
        Logger.d("SnackBar", "The snack bar display: $visible")
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -40 }),
        exit = slideOutVertically(targetOffsetY = { -40 }),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (messageData != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topScreenPadding)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onClick(messageData) }
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize() // fill the parent box
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                        .blur(16.dp)
                )
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = messageData.name,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.displayLarge,
                    )
                    Text(
                        text = messageData.message,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }

    if (visible) {
        LaunchedEffect(messageData) {
            delay(3000)
            onDismiss()
        }
    }
}

// Function to check if token is already sent and send if not
private fun checkAndSendToken(context: Context) {
    val sharedPreferences = context.getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
    val token = sharedPreferences.getString("firebase_token", null)

    if (!token.isNullOrEmpty()) {
        Logger.d("TokenCheck", "Token exists and has not been sent. Sending to server: $token")
        sendDeviceTokenToServer(token)
    } else if (token.isNullOrEmpty()) {
        Logger.d("TokenCheck", "No token found in SharedPreferences.")
    } else {
        Logger.d("TokenCheck", "Token already sent to the server.")
    }
}

private fun sendDeviceTokenToServer(token: String) {
    val serviceScope = CoroutineScope(Dispatchers.IO)
    val params = mapOf("token" to token)
    serviceScope.launch {
        try {
            NetworkUtils.makeRequest("onFireBaseToken", params = params)
        } catch (e: Exception) {
            Log.e("SendToken", "Failed to send token to server: ${e.message}")
        }
    }
}

@Composable
fun OffloadContactsScreen(shouldNavigate: () -> Unit) {
    val roundedCornerShape = RoundedCornerShape(16.dp)
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text(
            "Form your digital clique with your real life friends",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            textAlign = TextAlign.Center
        )
        Text(
            "Start Clique",
            Modifier
                .clickable {
                    SessionManager.markContactsOffloaded()
                    shouldNavigate()
                }
                .align(Alignment.CenterHorizontally)
                .background(MaterialTheme.colorScheme.primary, roundedCornerShape)
                .padding(16.dp),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.background
        )
        Text(
            "Skip for now",
            Modifier
                .clickable {
                    SessionManager.markContactsOffloaded()
                }
                .padding(10.dp)
                .background(MaterialTheme.colorScheme.onPrimary, roundedCornerShape)
                .align(Alignment.CenterHorizontally)
                .padding(16.dp)
                .padding(horizontal = 50.dp),
            color = MaterialTheme.colorScheme.background,
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(Modifier.weight(1f))
    }
}