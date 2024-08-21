package com.justself.klique

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ButtonColors
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
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.screens.LoginScreen
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModelFactory


val Pink700 = Color(0xFFFF759C)
val Pink400 = Color(0xFFC73868)
val Pink200 = Color(0xFFFCA4C2)
val LightBackground = Color(0xFFFFFFFF)
val CultPink = Color(0xFF6D1E1E)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF121212) // Dark grey
val LightSurface = Color(0xFFFFFFFF) // White
val AfacadFamily = FontFamily(
    Font(R.font.afacad_variablefont_wght, weight = FontWeight.Normal)
    // You can add more weight variations if available
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
    secondary = Pink400,
    onSecondary = Pink700
)

@Composable
fun MyAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Automatically uses system theme setting
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
    productViewModel: ProductViewModel = viewModel(),
    userDetailsViewModel: UserDetailsViewModel = viewModel()
) {
    val navController = rememberNavController()
    val leftDrawerState = remember { mutableStateOf(false) }
    val rightDrawerState = remember { mutableStateOf(false) }
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val cartItemCount by productViewModel.cartItemCount.observeAsState(0) // Ensure it's being observed here
    Log.d(
        "DebuggerCartItemCount",
        "Composable fully re-composed with Cart item count: $cartItemCount"
    )
    // Track keyboard visibility
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var showEmojiPicker by remember { mutableStateOf(false) }
    var selectedEmoji by remember { mutableStateOf("") }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val messageScreenGuy = currentRoute?.startsWith("messageScreen/") == true
    val bioScreenGuy = currentRoute?.startsWith("bioScreen/") == true
    val imageViewer = currentRoute?.startsWith("fullScreenImage") == true
    val videoViewer = currentRoute?.startsWith("fullScreenVideo") == true
    var isSearchMode by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(listOf<SearchUser>()) }
    val notificationViewModel: NotificationViewModel = viewModel()
    val notifications by notificationViewModel.notifications.collectAsState()
    val hasNewNotifications = notifications.any { !it.seen }
    var profileUpdateData by remember { mutableStateOf<ProfileUpdateData?>(null)}
    LaunchedEffect(intent) {
        val route = intent.getStringExtra("route")
        if (!route.isNullOrEmpty()) {
            navController.navigate(route)
        }
        val customerId = intent.getStringExtra("customerId") ?: ""
        val contactName = intent.getStringExtra("contactName") ?: ""
        val profilePhoto = intent.getStringExtra("profilePhoto") ?: ""
        val isVerified = intent.getBooleanExtra("isVerified", false)
        if (customerId.isNotEmpty() && contactName.isNotEmpty() && profilePhoto.isNotEmpty()) {
            profileUpdateData =
                ProfileUpdateData(customerId.toInt(), contactName, profilePhoto, isVerified)
        }
    }
    LaunchedEffect(searchText) {
        if (searchText.length > 2) {
            // Make a search request using userDetailsViewModel
            val results = userDetailsViewModel.searchUsers(searchText)
            searchResults = results
        } else {
            searchResults = emptyList()
        }
    }
    if (isLoggedIn) {
        Scaffold(
            topBar = {
                if (!(messageScreenGuy || bioScreenGuy || imageViewer || videoViewer)) {
                    CustomAppBar(
                        leftDrawerState,
                        rightDrawerState,
                        hasNewNotifications,
                        onRightDrawer = { notificationViewModel.markNotificationsAsSeen() },
                        isSearchMode = isSearchMode,
                        onSearchModeChange = { isSearchMode = it },
                        searchText = searchText,
                        onSearchTextChange = { searchText = it }
                    )
                }
            },
            bottomBar = {
                // Conditionally render the bottom navigation bar
                if (!(imeVisible || showEmojiPicker || messageScreenGuy || bioScreenGuy || imageViewer || videoViewer)) {
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
                    productViewModel,
                    authViewModel,
                    userDetailsViewModel,
                    imeVisible,
                    isLoggedIn,
                    showEmojiPicker,
                    onEmojiPickerVisibilityChange = {
                        Log.d("EmojiVisibility", "To test visibility of: $it")
                        showEmojiPicker = it
                    },
                    onEmojiSelected = { emoji -> selectedEmoji = emoji },
                    selectedEmoji,
                    resetSelectedEmoji = { selectedEmoji = "" },
                    notificationViewModel = notificationViewModel,
                    profileUpdateData = profileUpdateData
                )
                if (isSearchMode && searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(top = 90.dp)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        searchResults.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clickable {
                                        navController.navigate("bioScreen/${user.userId}")
                                        searchResults = emptyList()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(user.profilePictureUrl),
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
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
                    }
                }
            }
        }

    } else {
        LoginScreen(navController = navController)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MainContent(
    navController: NavHostController,
    innerPadding: PaddingValues,
    leftDrawerState: MutableState<Boolean>,
    rightDrawerState: MutableState<Boolean>,
    productViewModel: ProductViewModel,
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
    profileUpdateData: ProfileUpdateData?
) {
    val customerId = authViewModel.customerId.collectAsState().value
    val firstName = userDetailsViewModel.firstName.collectAsState().value
    val lastName = userDetailsViewModel.lastName.collectAsState().value
    val fullName = "$firstName $lastName".trim()
    val commentViewModel: CommentsViewModel = viewModel()
    Log.d("Names", "Full Name: $fullName")

    val context = LocalContext.current
    val webSocketUrl = context.getString(R.string.websocket_url)
    Log.d("WebSocketURL", "WebSocket URL: $webSocketUrl")
    LaunchedEffect(key1 = customerId) {
        if (customerId != 0) {
            userDetailsViewModel.fetchCustomerDetails(customerId)
        }
    }

    LaunchedEffect(key1 = customerId, key2 = firstName, key3 = lastName) {
        if (customerId != 0 && firstName.isNotEmpty() && lastName.isNotEmpty()) {
            Log.d(
                "WebSocket",
                "Attempting to connect to WebSocket at $webSocketUrl with customer ID $customerId"
            )
            WebSocketManager.connect(webSocketUrl, customerId, fullName)
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
        val sharedCliqueViewModel: SharedCliqueViewModel = viewModel(
            factory = SharedCliqueViewModelFactory(application, customerId, contactDao)
        )
        var emojiPickerHeight by remember { mutableStateOf(0.dp) }
        NavigationHost(
            navController, isLoggedIn, productViewModel, customerId,
            fullName, commentViewModel, onEmojiPickerVisibilityChange, selectedEmoji,
            showEmojiPicker, application, sharedCliqueViewModel, resetSelectedEmoji, profileUpdateData
        ) { height -> emojiPickerHeight = height }
        LeftDrawer(
            leftDrawerState,
            Modifier.align(Alignment.CenterStart),
            navController,
            customerId
        )
        RightDrawer(
            rightDrawerState,
            Modifier.align(Alignment.CenterEnd),
            notificationViewModel,
            navController
        )

        if (showEmojiPicker) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.BottomCenter) // Align at the bottom center
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
    hasNewNotifications: Boolean,
    onRightDrawer: () -> Unit,
    isSearchMode: Boolean,
    onSearchModeChange: (Boolean) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)  // Standard app bar height
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchMode) {
            TextField(
                value = searchText,
                onValueChange = { onSearchTextChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                placeholder = { Text("Type the alias...") },
                trailingIcon = {
                    IconButton(onClick = {
                        onSearchModeChange(false)
                        onSearchTextChange("")
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel Search")
                    }
                },
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(KeyboardCapitalization.Sentences)
            )
        } else {
            IconButton(
                onClick = {
                    leftDrawerState.value = !leftDrawerState.value
                    rightDrawerState.value = false  // Ensure only one drawer is open at a time
                },
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box {
                IconButton(
                    onClick = {
                        rightDrawerState.value = !rightDrawerState.value
                        leftDrawerState.value = false
                        if (rightDrawerState.value) {
                            onRightDrawer()
                        }
                    },
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (hasNewNotifications) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .offset(4.dp, 4.dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    onSearchModeChange(true)
                    leftDrawerState.value = false
                    rightDrawerState.value = false
                },
                modifier = Modifier.padding(top = 40.dp)
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