package com.justself.klique

import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
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
    resetSelectedEmoji: () -> Unit
) {

    val chatDao = remember { DatabaseProvider.getDatabase(application).chatDao() }
    val viewModelFactory = remember { ChatViewModelFactory(chatDao) }
    val chatScreenViewModel: ChatScreenViewModel = viewModel(factory = viewModelFactory)

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
                        }"
                    )
                }, navController, resetSelectedEmoji
            )
        }
        composable("chats") { ChatsScreen(navController, chatScreenViewModel, customerId) }
        composable("markets") { MarketsScreen(navController) }
        composable("bookshelf") { BookshelfScreen() }
        composable("orders") { OrdersScreen() }
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
            arguments = listOf(navArgument("enemyId") { type = NavType.IntType })
        ) { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the enemyId?")
            val contactName = backStackEntry.arguments?.getString("contactName")
                ?: throw IllegalStateException("where is the contactName")
            MessageScreen(navController, enemyId, contactName)
        }
        composable(
            "VideoTrimScreen/{videoUri}",
            arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val videoUri = Uri.parse(backStackEntry.arguments?.getString("videoUri"))
            VideoTrimmingScreen(
                appContext = LocalContext.current,
                uri = videoUri,
                onTrimComplete = { trimmedUri ->
                    // Navigate back to the chat room and handle the trimmed video
                    navController.popBackStack()
                    sharedCliqueViewModel.handleTrimmedVideo(trimmedUri)
                },
                onCancel = {
                    navController.popBackStack()
                }
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
            GistSettings(navController, gistId, sharedCliqueViewModel)
        }
        composable("fullScreenImage") { backStackEntry ->
            FullScreenImage(viewModel = sharedCliqueViewModel, navController = navController)
        }
        composable("fullScreenVideo/{videoUri}") { backStackEntry ->
            val videoUriString = backStackEntry.arguments?.getString("videoUri")
                ?: throw IllegalStateException("where is the videoUri")
            FullScreenVideo(videoUri = videoUriString, navController = navController)
        }
    }
}