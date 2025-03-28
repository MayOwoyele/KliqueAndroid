package com.justself.klique

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.justself.klique.ContactsBlock.Contacts.ui.ContactsScreen
import com.justself.klique.ContactsBlock.ui.ConditionalBookshelfScreen
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel

@Composable
fun NavigationHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
    customerId: Int,
    fullName: String,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    application: Application,
    sharedCliqueViewModel: SharedCliqueViewModel,
    resetSelectedEmoji: () -> Unit,
    onDisplayTextChange: (String, Int) -> Unit,
    emojiPickerHeight: (Dp) -> Unit
) {
    val profileUpdateData by ProfileRepository.profileUpdateData.observeAsState()
    val chatDao = remember { DatabaseProvider.getChatListDatabase(application).chatDao() }
    val personalChatDao = remember { DatabaseProvider.getPersonalChatDatabase(application).personalChatDao() }
    val context = LocalContext.current.applicationContext as Application
    val viewModelFactory = remember {
        ChatViewModelFactory(
            chatDao = chatDao,
            personalChatDao = personalChatDao,
            application = context
        )
    }
    val chatScreenViewModel: ChatScreenViewModel = viewModel(factory = viewModelFactory)
//    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(application))
    profileUpdateData?.let {
        Log.d("Worked", "It worked")
        ProfileRepository.clearProfileData()
    }
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(chatScreenViewModel))
    WebSocketManager.setChatScreenViewModel(chatScreenViewModel)
    WebSocketManager.setSharedCliqueViewModel(sharedCliqueViewModel)

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Registration.route
    ) {
        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("gistId") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Home.deepLink })
        ) { backStackEntry ->
            val gistId = backStackEntry.arguments?.getString("gistId")
            HomeScreen(
                customerId = customerId,
                fullName = fullName,
                viewModel = sharedCliqueViewModel,
                onEmojiPickerVisibilityChange = onEmojiPickerVisibilityChange,
                selectedEmoji = selectedEmoji,
                showEmojiPicker = showEmojiPicker,
                onNavigateToTrimScreen = { uri ->
                    Screen.VideoTrimScreen.navigate(navController, uri, "HomeScreen")
                },
                navController = navController,
                resetSelectedEmoji = resetSelectedEmoji,
                emojiPickerHeight = emojiPickerHeight,
                chatScreenViewModel = chatScreenViewModel,
                onDisplayTextChange = onDisplayTextChange,
                gistId = gistId
            )
        }
        composable(
            route = Screen.Chats.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Chats.deepLink })
        ) {
            ChatListScreen(navController, chatScreenViewModel, customerId)
        }
        composable(
            route = Screen.Bookshelf.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Bookshelf.deepLink })
        ) { ConditionalBookshelfScreen() }
        composable(
            route = Screen.MessageScreen.route,
            arguments = listOf(
                navArgument("enemyId") { type = NavType.IntType }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.MessageScreen.deepLink })
        ) { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the enemyId?")
            MessageScreen(
                navController,
                enemyId,
                chatScreenViewModel,
                onNavigateToTrimScreen = { uri ->
                    Screen.VideoTrimScreen.navigate(navController, Uri.encode(uri), "MessageScreen")
                },
                onEmojiPickerVisibilityChange,
                selectedEmoji,
                showEmojiPicker,
                resetSelectedEmoji,
                emojiPickerHeight,
            )
        }
        composable(
            route = Screen.VideoTrimScreen.route,
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType },
                navArgument("sourceScreen") { type = NavType.StringType }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.VideoTrimScreen.deepLink })
        ) { backStackEntry ->
            val videoUri = Uri.parse(backStackEntry.arguments?.getString("videoUri"))
            val sourceScreen = backStackEntry.arguments?.getString("sourceScreen") ?: ""
            Log.d("Video Status", "$videoUri, $sourceScreen")
            VideoTrimmingScreen(
                appContext = LocalContext.current,
                uri = videoUri,
                onCancel = { navController.popBackStack() },
                sourceScreen = sourceScreen,
                navController = navController
            )
        }
        composable(
            route = Screen.MediaPickerScreen.route,
            arguments = listOf(navArgument("source") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.MediaPickerScreen.deepLink })
        ) {
            val source = it.arguments?.getString("source")
                ?: throw IllegalStateException("where is the source code?")
            MediaPickerScreen(navController, source, customerId)
        }
        composable(
            route = Screen.BioScreen.route,
            arguments = listOf(navArgument("enemyId") { type = NavType.IntType }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.BioScreen.deepLink })
        ) {
            val enemyId = it.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the profileId?")
            BioScreen(enemyId, navController, customerId)
        }
        composable(
            route = Screen.GistSettings.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.GistSettings.deepLink })
        ) { backStackEntry ->
            val gistId = backStackEntry.arguments?.getString("gistId")
                ?: throw IllegalStateException("where is the gistId")
            Log.d("GistId", "The gist Id is: $gistId")
            GistSettings(navController, sharedCliqueViewModel)
        }
        composable(
            route = Screen.FullScreenImage.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.FullScreenImage.deepLink })
        ) {
            FullScreenImage(navController = navController)
        }
        composable(
            route = Screen.FullScreenVideo.route,
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.FullScreenVideo.deepLink })
        ) { backStackEntry ->
            val videoUriString = backStackEntry.arguments?.getString("videoUri")
                ?: throw IllegalStateException("where is the videoUri")
            Log.d("VideoView", "video uri $videoUriString")
            FullScreenVideo(videoUri = videoUriString, navController = navController)
        }
        composable(
            route = Screen.ForwardChatsScreen.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.ForwardChatsScreen.deepLink })
        ) {
            ForwardChatsScreen(
                navController = navController,
                viewModel = chatScreenViewModel,
                customerId = customerId
            )
        }
        composable(
            route = Screen.ImageEditScreen.route,
            arguments = listOf(navArgument("sourceScreen") { defaultValue = SourceScreen.STATUS.name }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.ImageEditScreen.deepLink })
        ) { backStackEntry ->
            val sourceScreen = backStackEntry.arguments?.getString("sourceScreen")?.let { SourceScreen.valueOf(it) }
                ?: SourceScreen.STATUS
            ImageCropTool(
                navController = navController,
                sourceScreen = sourceScreen,
                customerId = customerId
            )
        }
        composable(
            route = Screen.StatusAudioScreen.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.StatusAudioScreen.deepLink })
        ) {
            StatusAudio(navController = navController)
        }
        composable(
            route = Screen.StatusTextScreen.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.StatusTextScreen.deepLink })
        ) {
            StatusText(navController = navController, customerId)
        }
        composable(
            route = Screen.Campuses.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Campuses.deepLink })
        ) {
            Log.d("Navigated", "Navigated")
            ChatRoomsCategory(navController = navController, campuses = true)
        }
        composable(
            route = Screen.Interests.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Interests.deepLink })
        ) {
            ChatRoomsCategory(navController = navController, interests = true)
        }
        composable(
            route = Screen.CategoryOptions.route,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.CategoryOptions.deepLink })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
                ?: throw IllegalStateException("where is the categoryId")
            val categoryIdInt = categoryId.toIntOrNull()
            if (categoryIdInt != null) {
                ChatRoomOptions(navController, categoryIdInt)
            }
        }
        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(navArgument("chatRoomId") { type = NavType.StringType }),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.ChatRoom.deepLink })
        ) { backStackEntry ->
            val optionId = backStackEntry.arguments?.getString("chatRoomId")
                ?: throw IllegalStateException("where is the chatRoomId")
            val chatRoomOptionId = optionId.toIntOrNull()
            Log.d("ChatRoom", "The id is $optionId")
            if (chatRoomOptionId != null) {
                ChatRoom(
                    navController,
                    chatRoomId = chatRoomOptionId,
                    myId = customerId,
                    contactName = fullName
                )
            }
        }
        composable(
            route = Screen.DmList.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.DmList.deepLink })
        ) { DmList(navController, customerId) }
        composable(
            route = Screen.DmChatScreen.route,
            arguments = listOf(
                navArgument("enemyId") { type = NavType.StringType },
                navArgument("enemyName") { type = NavType.StringType }
            ),
            deepLinks = listOf(navDeepLink { uriPattern = Screen.DmChatScreen.deepLink })
        ) { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getString("enemyId")?.toIntOrNull()
                ?: throw IllegalStateException("where is the enemyId?")
            val enemyName = backStackEntry.arguments?.getString("enemyName")
                ?: throw IllegalStateException("where is the enemyName?")
            DmRoom(
                navController = navController,
                myId = customerId,
                enemyId = enemyId,
                enemyName = enemyName
            )
        }
        composable(
            route = Screen.UpdateProfile.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.UpdateProfile.deepLink })
        ) {
            UpdateProfileScreen(
                navController = navController,
                customerId = customerId,
                viewModel = profileViewModel
            )
        }
        composable(
            route = Screen.ContactsScreen.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.ContactsScreen.deepLink })
        ) {
            ContactsScreen(
                navController = navController,
                chatScreenViewModel = chatScreenViewModel,
                customerId = customerId
            )
        }
        composable(
            route = Screen.StatusSelectionScreen.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.StatusSelectionScreen.deepLink })
        ) {
            StatusSelectionScreen(navController = navController)
        }
        composable(
            route = Screen.Settings.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Settings.deepLink })
        ) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = Screen.TopGists.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.TopGists.deepLink })
        ) {
            TopGistsScreen(
                navController = navController,
                viewModel = sharedCliqueViewModel
            )
        }
        composable(
            route = Screen.Registration.route,
            deepLinks = listOf(navDeepLink { uriPattern = Screen.Registration.deepLink })
        ) {
            // Registration screen contents
        }
    }
}

sealed class Screen(val route: String, val deepLink: String) {
    data object Home : Screen("home?gistId={gistId}", "kliqueklique://home?gistId={gistId}") {
        fun createRoute(gistId: String? = null): String {
            return if (gistId.isNullOrEmpty()) "home" else "home?gistId=${Uri.encode(gistId)}"
        }
        fun navigate(navController: NavController, gistId: String? = null) {
            navController.navigate(createRoute(gistId))
        }
    }
    data object Chats : Screen("chats", "kliqueklique://chats") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object Bookshelf : Screen("bookshelf", "kliqueklique://bookshelf") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object MessageScreen : Screen("messageScreen/{enemyId}", "kliqueklique://messageScreen/{enemyId}") {
        fun createRoute(enemyId: Int) = "messageScreen/$enemyId"
        fun navigate(navController: NavController, enemyId: Int) {
            navController.navigate(createRoute(enemyId))
        }
    }
    data object VideoTrimScreen : Screen("VideoTrimScreen/{videoUri}/{sourceScreen}", "kliqueklique://VideoTrimScreen/{videoUri}/{sourceScreen}") {
        fun createRoute(videoUri: String, sourceScreen: String) =
            "VideoTrimScreen/${Uri.encode(videoUri)}/$sourceScreen"
        fun navigate(navController: NavController, videoUri: String, sourceScreen: String) {
            Log.d("Video Status", "$videoUri, $sourceScreen")
            navController.navigate(createRoute(videoUri, sourceScreen))
        }
    }
    data object MediaPickerScreen : Screen("mediaPickerScreen/{source}", "kliqueklique://mediaPickerScreen/{source}") {
        fun createRoute(source: String) = "mediaPickerScreen/$source"
        fun navigate(navController: NavController, source: String) {
            navController.navigate(createRoute(source))
        }
    }
    data object BioScreen : Screen("bioScreen/{enemyId}", "kliqueklique://bioScreen/{enemyId}") {
        fun createRoute(enemyId: Int) = "bioScreen/$enemyId"
        fun navigate(navController: NavController, enemyId: Int) {
            navController.navigate(createRoute(enemyId))
        }
    }
    data object GistSettings : Screen("gistSettings/{gistId}", "kliqueklique://gistSettings/{gistId}") {
        fun createRoute(gistId: String) = "gistSettings/$gistId"
        fun navigate(navController: NavController, gistId: String) {
            navController.navigate(createRoute(gistId))
        }
    }
    data object FullScreenImage : Screen("fullScreenImage", "kliqueklique://fullScreenImage") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object FullScreenVideo : Screen("fullScreenVideo/{videoUri}", "kliqueklique://fullScreenVideo/{videoUri}") {
        fun createRoute(videoUri: String) = "fullScreenVideo/${Uri.encode(videoUri)}"
        fun navigate(navController: NavController, videoUri: String) {
            navController.navigate(createRoute(videoUri))
        }
    }
    data object ForwardChatsScreen : Screen("forwardChatsScreen", "kliqueklique://forwardChatsScreen") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object ImageEditScreen : Screen("imageEditScreen/{sourceScreen}", "kliqueklique://imageEditScreen/{sourceScreen}") {
        fun createRoute(sourceScreen: String) = "imageEditScreen/$sourceScreen"
        fun navigate(navController: NavController, sourceScreen: String) {
            navController.navigate(createRoute(sourceScreen))
        }
    }
    data object StatusAudioScreen : Screen("statusAudioScreen", "kliqueklique://statusAudioScreen") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object StatusTextScreen : Screen("statusTextScreen", "kliqueklique://statusTextScreen") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object Campuses : Screen("campuses", "kliqueklique://campuses") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object Interests : Screen("interests", "kliqueklique://interests") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object CategoryOptions : Screen("categoryOptions/{categoryId}", "kliqueklique://categoryOptions/{categoryId}") {
        fun createRoute(categoryId: Int) = "categoryOptions/$categoryId"
        fun navigate(navController: NavController, categoryId: Int) {
            navController.navigate(createRoute(categoryId))
        }
    }
    data object ChatRoom : Screen("chatRoom/{chatRoomId}", "kliqueklique://chatRoom/{chatRoomId}") {
        fun createRoute(chatRoomId: Int) = "chatRoom/$chatRoomId"
        fun navigate(navController: NavController, chatRoomId: Int) {
            navController.navigate(createRoute(chatRoomId))
        }
    }
    data object DmList : Screen("dmList", "kliqueklique://dmList") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object DmChatScreen : Screen("dmChatScreen/{enemyId}/{enemyName}", "kliqueklique://dmChatScreen/{enemyId}/{enemyName}") {
        fun createRoute(enemyId: Int, enemyName: String) =
            "dmChatScreen/$enemyId/${Uri.encode(enemyName)}"
        fun navigate(navController: NavController, enemyId: Int, enemyName: String) {
            navController.navigate(createRoute(enemyId, enemyName))
        }
    }
    data object UpdateProfile : Screen("updateProfile", "kliqueklique://updateProfile") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object ContactsScreen : Screen("contactsScreen", "kliqueklique://contactsScreen") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object StatusSelectionScreen : Screen("statusSelectionScreen", "kliqueklique://statusSelectionScreen") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object Settings : Screen("settings", "kliqueklique://settings") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object Registration : Screen("registration", "kliqueklique://registration") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
    data object TopGists : Screen("topGists", "kliqueklique://topGists") {
        fun navigate(navController: NavController) {
            navController.navigate(route)
        }
    }
}