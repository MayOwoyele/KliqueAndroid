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
    emojiPickerHeight: (Dp) -> Unit
) {

    val chatDao = remember { DatabaseProvider.getChatListDatabase(application).chatDao() }
    val personalChatDao =
        remember { DatabaseProvider.getPersonalChatDatabase(application).personalChatDao() }
    val viewModelFactory =
        remember { ChatViewModelFactory(chatDao = chatDao, personalChatDao = personalChatDao) }
    val chatScreenViewModel: ChatScreenViewModel = viewModel(factory = viewModelFactory)
    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(application))
    var theTrimmedUri by remember { mutableStateOf<Uri?>(null) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "login"
    ) {
        composable("home") {
            HomeScreen(
                customerId, fullName, sharedCliqueViewModel,
                onEmojiPickerVisibilityChange, selectedEmoji, showEmojiPicker,
                onNavigateToTrimScreen = { uri ->
                    navController.navigate(
                        "VideoTrimScreen/${
                            Uri.encode(
                                uri
                            )
                        }/HomeScreen"
                    )
                }, navController, resetSelectedEmoji, mediaViewModel, emojiPickerHeight
            )
        }
        composable("chats") { ChatListScreen(navController, chatScreenViewModel, customerId) }
        composable("markets") { MarketsScreen(navController) }
        composable("bookshelf") { BookshelfScreen(navController, chatScreenViewModel, customerId) }
        composable("orders") { OrdersScreen(navController, chatScreenViewModel,) }
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
            "messageScreen/{enemyId}/{contactName}",
            arguments = listOf(
                navArgument("enemyId") { type = NavType.IntType },
                navArgument("contactName") { type = NavType.StringType })
        ) { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the enemyId?")
            val contactName = backStackEntry.arguments?.getString("contactName")
                ?: throw IllegalStateException("where is the contactName?")
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
                contactName, mediaViewModel, resetSelectedEmoji, emojiPickerHeight, theTrimmedUri, { theTrimmedUri = null}
            )
        }
        composable(
            "VideoTrimScreen/{videoUri}/{sourceScreen}",
            arguments = listOf(
                navArgument("videoUri") { type = NavType.StringType },
                navArgument("sourceScreen") {type = NavType.StringType
                })
        ) { backStackEntry ->
            val videoUri = Uri.parse(backStackEntry.arguments?.getString("videoUri"))
            val sourceScreen = backStackEntry.arguments?.getString("sourceScreen")?: ""
            VideoTrimmingScreen(
                appContext = LocalContext.current,
                uri = videoUri,
                onTrimComplete = { trimmedUri, screen ->
                    when (screen) {
                        "HomeScreen" -> {sharedCliqueViewModel.handleTrimmedVideo(trimmedUri)}
                        "MessageScreen" -> {theTrimmedUri = trimmedUri}
                }
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
                sourceScreen = sourceScreen
            )
        }
        composable(
            "bioScreen/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
        ) {
            val profileId = it.arguments?.getInt("customerId")
                ?: throw IllegalStateException("where is the profileId?")
            BioScreen(profileId, navController)
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
            FullScreenVideo(videoUri = videoUriString, navController = navController)
        }
    }
}