package com.justself.klique

import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.justself.klique.Bookshelf.Contacts.ui.ContactsScreen
import com.justself.klique.Bookshelf.ui.BookshelfScreen
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavigationHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
    productViewModel: ProductViewModel,
    customerId: Int,
    fullName: String,
    commentViewModel: CommentsViewModel,
    onEmojiPickerVisibilityChange: (Boolean) -> Unit,
    selectedEmoji: String,
    showEmojiPicker: Boolean,
    application: Application,
    sharedCliqueViewModel: SharedCliqueViewModel,
    resetSelectedEmoji: () -> Unit,
    profileUpdateData: ProfileUpdateData?,
    emojiPickerHeight: (Dp) -> Unit
) {

    val chatDao = remember { DatabaseProvider.getChatListDatabase(application).chatDao() }
    val personalChatDao =
        remember { DatabaseProvider.getPersonalChatDatabase(application).personalChatDao() }
    val viewModelFactory =
        remember { ChatViewModelFactory(chatDao = chatDao, personalChatDao = personalChatDao) }
    val chatScreenViewModel: ChatScreenViewModel = viewModel(factory = viewModelFactory)
    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(application))
    profileUpdateData?.let {
        chatScreenViewModel.updateProfile(
            enemyId = it.customerId,
            contactName = it.contactName,
            profilePhoto = it.profilePhoto,
            isVerified = it.isVerified
        )
    }
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("home") {
            HomeScreen(
                customerId,
                fullName,
                sharedCliqueViewModel,
                onEmojiPickerVisibilityChange,
                selectedEmoji,
                showEmojiPicker,
                onNavigateToTrimScreen = { uri ->
                    navController.navigate(
                        "VideoTrimScreen/${
                            Uri.encode(
                                uri
                            )
                        }/HomeScreen"
                    )
                },
                navController,
                resetSelectedEmoji,
                mediaViewModel,
                emojiPickerHeight,
                chatScreenViewModel
            )
        }
        composable("chats") { ChatListScreen(navController, chatScreenViewModel, customerId) }
        composable("bookshelf") { BookshelfScreen(navController, chatScreenViewModel, customerId) }
        composable("product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")?.toIntOrNull()
                ?: throw IllegalStateException("Product must be provided")
            ProductCommentsScreen(productId, commentViewModel, navController, customerId)
        }
        composable("marketProduct/{marketId}/{encodedMarketName}") { backStackEntry ->
            val marketId = backStackEntry.arguments?.getString("marketId")?.toIntOrNull()
                ?: throw IllegalStateException("Market Id must be provided")
            val encodedMarketName =
                backStackEntry.arguments?.getString("encodedMarketName")?.let { Uri.decode(it) }
                    ?: throw IllegalStateException("Market Name must be provided")
            MarketProductsScreen(
                productViewModel,
                navController,
                customerId,
                marketId,
                encodedMarketName
            )
        }
        composable("shop_details/{shopId}") { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId")?.toIntOrNull()
                ?: throw IllegalStateException("Shop ID must be provided")
            ShopOwnerScreen(shopId, navController, productViewModel)
        }
        composable("dmScreen/{ownerId}/{shopName}") { backStackEntry ->
            val ownerId = backStackEntry.arguments?.getString("ownerId")?.toIntOrNull()
                ?: throw IllegalStateException("Owner ID must be provided")
            val shopName = backStackEntry.arguments?.getString("shopName")?.let { Uri.decode(it) }
                ?: throw IllegalStateException("Shop Name must be provided")
            DMChatScreen(navController, customerId, shopName, ownerId)
        }
        composable(
            "messageScreen/{enemyId}/{contactName}?isVerified={isVerified}",
            arguments = listOf(
                navArgument("enemyId") { type = NavType.IntType },
                navArgument("contactName") { type = NavType.StringType },
                navArgument("isVerified") {
                    type = NavType.IntType
                    defaultValue = 0 // Make the argument optional
                })
        ) { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the enemyId?")
            val contactName = backStackEntry.arguments?.getString("contactName")
                ?.let { Uri.decode(it) }
                ?: throw IllegalStateException("Where is the contactName?")
            val isVerified = (backStackEntry.arguments?.getInt("isVerified") ?: 0)
            Log.d("Message Screen", "$enemyId, $contactName, $isVerified")
            MessageScreen(
                navController,
                enemyId,
                chatScreenViewModel,
                onNavigateToTrimScreen = { uri ->
                    navController.navigate(
                        "VideoTrimScreen/${
                            Uri.encode(
                                uri
                            )
                        }/MessageScreen"
                    )
                },
                onEmojiPickerVisibilityChange,
                selectedEmoji,
                showEmojiPicker,
                contactName,
                mediaViewModel,
                resetSelectedEmoji,
                emojiPickerHeight,
                isVerified == 1
            )
        }
        composable(
            "VideoTrimScreen/{videoUri}/{sourceScreen}",
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType },
                navArgument("sourceScreen") {
                    type = NavType.StringType
                })
        ) { backStackEntry ->
            val videoUri = Uri.parse(backStackEntry.arguments?.getString("videoUri"))
            val sourceScreen = backStackEntry.arguments?.getString("sourceScreen") ?: ""
            Log.d("Video Status", "$videoUri, $sourceScreen")
            VideoTrimmingScreen(
                appContext = LocalContext.current,
                uri = videoUri,
                onCancel = {
                    navController.popBackStack()
                },
                sourceScreen = sourceScreen,
                mediaViewModel = mediaViewModel,
                navController = navController
            )
        }
        composable(
            "mediaPickerScreen/{source}",
            arguments = listOf(navArgument("source") { type = NavType.StringType })
        ) {
            val source = it.arguments?.getString("source")
                ?: throw IllegalStateException("where is the source code?")
            MediaPickerScreen(navController, source, mediaViewModel)
        }
        composable(
            "bioScreen/{enemyId}",
            arguments = listOf(navArgument("enemyId") { type = NavType.IntType })
        ) {
            val enemyId = it.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the profileId?")
            BioScreen(enemyId, navController)
        }
        composable("gistSettings/{gistId}") { backStackEntry ->
            val gistId = backStackEntry.arguments?.getString("gistId")
                ?: throw IllegalStateException("where is the gistId")
            Log.d("GistId", "The gist Id is: $gistId")
            GistSettings(navController, sharedCliqueViewModel)
        }
        composable("fullScreenImage") { backStackEntry ->
            FullScreenImage(viewModel = mediaViewModel, navController = navController)
        }
        composable("fullScreenVideo/{videoUri}") { backStackEntry ->
            val videoUriString = backStackEntry.arguments?.getString("videoUri")
                ?: throw IllegalStateException("where is the videoUri")
            Log.d("VideoView", "video uri $videoUriString")
            FullScreenVideo(videoUri = videoUriString, navController = navController)
        }
        composable("forwardChatsScreen") {
            ForwardChatsScreen(
                navController = navController,
                viewModel = chatScreenViewModel,
                customerId = customerId
            )
        }
        composable(
            route = "imageEditScreen/{sourceScreen}",
            arguments = listOf(navArgument("sourceScreen") {
                defaultValue = SourceScreen.STATUS.name
            })
        ) { backStackEntry ->
            val sourceScreen = backStackEntry.arguments?.getString("sourceScreen")?.let {
                SourceScreen.valueOf(it)
            } ?: SourceScreen.STATUS // Fallback to default
            ImageCropTool(
                viewModel = mediaViewModel,
                navController = navController,
                sourceScreen = sourceScreen
            )
        }
        composable("statusAudioScreen") {
            StatusAudio(viewModel = mediaViewModel, navController = navController)
        }
        composable("statusTextScreen") {
            StatusText(viewModel = mediaViewModel, navController = navController)
        }
        composable("campuses") {
            Log.d("Navigated", "Navigated")
            ChatRoomsCategory(navController = navController, campuses = true)
        }
        composable("interests") {
            ChatRoomsCategory(navController = navController, interests = true)
        }
        composable("categoryOptions/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getInt("categoryId")
                ?: throw IllegalStateException("where is the categoryId")
            ChatRoomOptions(navController, categoryId)
        }
        composable("chatRoom/{chatRoomId}") { backStackEntry ->
            val optionId = backStackEntry.arguments?.getInt("chatRoomId")
                ?: throw IllegalStateException("where is the chatRoomId")
            ChatRoom(
                navController,
                chatRoomId = optionId,
                myId = customerId,
                mediaViewModel = mediaViewModel,
                contactName = fullName
            )
        }
        composable("dmList") { DmList(navController) }
        composable("dmChatScreen/{enemyId}/{enemyName}") { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the enemyId?")
            val enemyName = backStackEntry.arguments?.getString("enemyName")
                ?: throw IllegalStateException("where is the enemyName?")
            DmRoom(
                navController = navController,
                myId = customerId,
                enemyId = enemyId,
                enemyName = enemyName,
                mediaViewModel = mediaViewModel
            )
        }
        composable("updateProfile") {
            UpdateProfileScreen(
                navController = navController,
                mediaViewModel = mediaViewModel,
                chatScreenViewModel = chatScreenViewModel
            )
        }
        composable("contactsScreen") {
            ContactsScreen(
                navController = navController,
                chatScreenViewModel = chatScreenViewModel,
                customerId = customerId
            )
        }
        composable("statusSelectionScreen") {
            StatusSelectionScreen(navController = navController, mediaViewModel = mediaViewModel)
        }
    }
}