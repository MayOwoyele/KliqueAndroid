package com.justself.klique

import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.Authentication.ui.screens.LoginScreen
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModelFactory


val Pink700 = Color(0xFFFF759C)
val Pink400 = Color(0xFFC73868)
val LightBackground = Color(0xFFFFFFFF)
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF121212) // Dark grey
val LightSurface = Color(0xFFFFFFFF) // White
val AfacadFamily = FontFamily(
    Font(R.font.afacad_variablefont_wght, weight = FontWeight.Normal)
    // You can add more weight variations if available
)
val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = AfacadFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    displayLarge = TextStyle(fontFamily = AfacadFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    labelLarge = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp),
    displayMedium = TextStyle(fontFamily = AfacadFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = AfacadFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp)

)

private val DarkColorScheme = darkColorScheme(
    primary = Pink400,
    surface = DarkSurface,
    background = DarkBackground,
    onPrimary = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Pink700,
    surface = LightSurface,
    background = LightBackground,
    onPrimary = Color.Black
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
    authViewModel: AuthViewModel = viewModel(),
    productViewModel: ProductViewModel = viewModel(),
    userDetailsViewModel: UserDetailsViewModel = viewModel()
){
    val navController = rememberNavController()
    val leftDrawerState = remember { mutableStateOf(false) }
    val rightDrawerState = remember { mutableStateOf(false) }
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val cartItemCount by productViewModel.cartItemCount.observeAsState(0) // Ensure it's being observed here
    Log.d("DebuggerCartItemCount", "Composable fully re-composed with Cart item count: $cartItemCount")
    // Track keyboard visibility
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var showEmojiPicker by remember { mutableStateOf(false)}
    var selectedEmoji by remember { mutableStateOf("")}


    if (isLoggedIn) {
        Scaffold(
            topBar = { CustomAppBar(leftDrawerState, rightDrawerState, cartItemCount) },
            bottomBar = {
                // Conditionally render the bottom navigation bar
                if (!imeVisible && !showEmojiPicker) {
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            MainContent(
                navController, innerPadding, leftDrawerState, rightDrawerState,
                productViewModel, authViewModel, userDetailsViewModel, imeVisible, isLoggedIn, showEmojiPicker,
                onEmojiPickerVisibilityChange = { Log.d("EmojiVisibility", "To test visibility of: $it")
                    showEmojiPicker = it}, onEmojiSelected = { emoji -> selectedEmoji = emoji}, selectedEmoji )
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
    selectedEmoji: String
) {
    val customerId = authViewModel.customerId.collectAsState().value
    val firstName = userDetailsViewModel.firstName.collectAsState().value
    val lastName = userDetailsViewModel.lastName.collectAsState().value
    val fullName = "$firstName $lastName".trim()
    val commentViewModel: CommentsViewModel = viewModel()

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
            Log.d("WebSocket", "Attempting to connect to WebSocket at $webSocketUrl with customer ID $customerId")
            WebSocketManager.connect(webSocketUrl, customerId, fullName)
        }
    }
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
        NavigationHost(navController, isLoggedIn, productViewModel, customerId, fullName, commentViewModel, onEmojiPickerVisibilityChange, selectedEmoji, showEmojiPicker)
        LeftDrawer(leftDrawerState, Modifier.align(Alignment.CenterStart))
        RightDrawer(rightDrawerState, Modifier.align(Alignment.CenterEnd))

        if (showEmojiPicker) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .align(Alignment.BottomCenter) // Align at the bottom center
            ) {
                EmojiPickerView(onEmojiSelected = {
                    Log.d("EmojiVisibility", "The emojiVisible: $it")
                    onEmojiSelected(it)
                    onEmojiPickerVisibilityChange(true)
                })
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavigationHost(navController: NavHostController, isLoggedIn: Boolean, productViewModel: ProductViewModel, customerId: Int, fullName: String,
                   commentViewModel: CommentsViewModel, onEmojiPickerVisibilityChange: (Boolean) -> Unit,
                   selectedEmoji: String, showEmojiPicker: Boolean) {
    val application = LocalContext.current.applicationContext as Application
    val sharedCliqueViewModel: SharedCliqueViewModel = viewModel(
        factory = SharedCliqueViewModelFactory(application, customerId)
    )
    val chatDatabaseHelper = ChatDatabaseHelper(LocalContext.current)
    // Initialize the ViewModel using the factory
    val chatScreenViewModel: ChatScreenViewModel = viewModel(
        factory = ChatViewModelFactory(chatDatabaseHelper)
    )

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("home") { HomeScreen(customerId, fullName, sharedCliqueViewModel, onEmojiPickerVisibilityChange, selectedEmoji, showEmojiPicker) }
        composable("chats") { ChatsScreen(navController, chatScreenViewModel) }
        composable("markets") { MarketsScreen(navController) }
        composable("bookshelf") { BookshelfScreen() }
        composable("orders") { OrdersScreen() }
        composable("product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull() ?: throw IllegalStateException("Product must be provided")
            ProductCommentsScreen(productId, commentViewModel, navController, customerId) }
        composable("marketProduct/{marketId}/{encodedMarketName}") { backStackEntry ->
            val marketId = backStackEntry.arguments?.getString("marketId")?.toIntOrNull()
                ?: throw IllegalStateException("Market Id must be provided")
            val encodedMarketName = backStackEntry.arguments?.getString("encodedMarketName")?.let { Uri.decode(it) }
                ?: throw IllegalStateException("Market Name must be provided")
            MarketProductsScreen(productViewModel, navController, customerId, marketId, encodedMarketName )
        }
        composable("shop_details/{shopId}") { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId")?.toIntOrNull() ?: throw IllegalStateException("Shop ID must be provided")
            ShopOwnerScreen(shopId, navController, productViewModel)
        }
        composable("dmScreen/{ownerId}/{shopName}") { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getString("ownerId")?.toIntOrNull()
                ?: throw IllegalStateException("Owner ID must be provided")
            val shopName = backStackEntry.arguments?.getString("shopName")?.let { Uri.decode(it) }
                ?: throw IllegalStateException("Shop Name must be provided")
            DMChatScreen(navController, customerId, shopName, ownerId)
        }
    }
}
@Composable
fun CustomAppBar(
    leftDrawerState: MutableState<Boolean>,
    rightDrawerState: MutableState<Boolean>,
    cartItemCount: Int  // Cart item count is still a parameter
) {
    LaunchedEffect(cartItemCount) {
        Log.d("CustomAppBar", "Custom App Bar Cart item count updated to: $cartItemCount")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)  // Standard app bar height
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = {
            leftDrawerState.value = !leftDrawerState.value
            rightDrawerState.value = false  // Ensure only one drawer is open at a time
        },
            modifier = Modifier.padding(top = 40.dp)) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.weight(1f))

        // Conditional display of cart counter
        if (cartItemCount > 0) {
            Text(
                text = "$cartItemCount items in cart",
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(end = 8.dp, top = 40.dp)  // Adjust padding to position the text left of the icon
            )
        }

        IconButton(onClick = {
            rightDrawerState.value = !rightDrawerState.value
            leftDrawerState.value = false  // Ensure only one drawer is open at a time
        },
            modifier = Modifier.padding(top = 40.dp)) {
            Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val textStyle = TextStyle(color = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary, fontFamily = AfacadFamily, fontSize = 12.sp)
    val iconColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimary

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
            icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chats", tint = iconColor) },
            label = { Text("Chats", style = textStyle) },
            selected = currentRoute == "chats",
            onClick = { if (currentRoute != "chats") navController.navigate("chats") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Market", tint = iconColor) },
            label = { Text("Market", style = textStyle) },
            selected = currentRoute == "markets",
            onClick = { if (currentRoute != "markets") navController.navigate("markets") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Book, contentDescription = "Bookshelf", tint = iconColor) },
            label = { Text("Bookshelf", style = textStyle) },
            selected = currentRoute == "bookshelf",
            onClick = { if (currentRoute != "bookshelf") navController.navigate("bookshelf") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.ShoppingBag, contentDescription = "Orders", tint = iconColor) },
            label = { Text("Orders", style = textStyle) },
            selected = currentRoute == "orders",
            onClick = { if (currentRoute != "orders") navController.navigate("orders") }
        )
    }
}

@Composable
fun OrdersScreen() {
    Text("Dashboard boardyboard")
}
@Composable
fun BookshelfScreen() {
    Text("The Bookshelf, coming soon")
}


@Composable
fun EmojiPickerView(onEmojiSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
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
            modifier = Modifier.height(300.dp).fillMaxWidth()
        )
    }
}