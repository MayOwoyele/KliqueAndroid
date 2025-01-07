package com.justself.klique

import android.app.Application
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.justself.klique.ContactsBlock.Contacts.ui.ContactsScreen
import com.justself.klique.ContactsBlock.ui.BookshelfScreen
import com.justself.klique.ContactsBlock.ui.ConditionalBookshelfScreen
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModelFactory

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
    notificationRoute: String?,
    emojiPickerHeight: (Dp) -> Unit
) {
    val profileUpdateData by ProfileRepository.profileUpdateData.observeAsState()
    val chatDao = remember { DatabaseProvider.getChatListDatabase(application).chatDao() }
    val personalChatDao =
        remember { DatabaseProvider.getPersonalChatDatabase(application).personalChatDao() }
    val context = LocalContext.current.applicationContext as Application
    val viewModelFactory =
        remember { ChatViewModelFactory(chatDao = chatDao, personalChatDao = personalChatDao, application = context) }
    val chatScreenViewModel: ChatScreenViewModel = viewModel(factory = viewModelFactory)
    val mediaViewModel: MediaViewModel = viewModel(factory = MediaViewModelFactory(application))
    profileUpdateData?.let {
        Log.d("Worked", "It worked")
//        chatScreenViewModel.updateProfile(
//            enemyId = it.senderId,
//            contactName = it.contactName,
//            profilePhoto = it.profilePhoto,
//            isVerified = it.isVerified
//        )
        ProfileRepository.clearProfileData()
    }
    val profileViewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(chatScreenViewModel)
    )
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
    WebSocketManager.setChatScreenViewModel(chatScreenViewModel)
    WebSocketManager.setSharedCliqueViewModel(sharedCliqueViewModel)
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) "home" else "registration"
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
                chatScreenViewModel,
                onDisplayTextChange
            )
        }
        composable("chats") { ChatListScreen(navController, chatScreenViewModel, customerId) }
        composable("bookshelf") { ConditionalBookshelfScreen() }
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
            MediaPickerScreen(navController, source, mediaViewModel, customerId)
        }
        composable(
            "bioScreen/{enemyId}",
            arguments = listOf(navArgument("enemyId") { type = NavType.IntType })
        ) {
            val enemyId = it.arguments?.getInt("enemyId")
                ?: throw IllegalStateException("where is the profileId?")
            BioScreen(enemyId, navController, customerId)
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
            } ?: SourceScreen.STATUS
            ImageCropTool(
                viewModel = mediaViewModel,
                navController = navController,
                sourceScreen = sourceScreen,
                customerId = customerId
            )
        }
        composable("statusAudioScreen") {
            StatusAudio(viewModel = mediaViewModel, navController = navController)
        }
        composable("statusTextScreen") {
            StatusText(viewModel = mediaViewModel, navController = navController, customerId)
        }
        composable("campuses") {
            Log.d("Navigated", "Navigated")
            ChatRoomsCategory(navController = navController, campuses = true)
        }
        composable("interests") {
            ChatRoomsCategory(navController = navController, interests = true)
        }
        composable("categoryOptions/{categoryId}") { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
                ?: throw IllegalStateException("where is the categoryId")
            val categoryIdInt = categoryId.toIntOrNull()
            if (categoryIdInt != null) {
                ChatRoomOptions(navController, categoryIdInt)
            }
        }
        composable("chatRoom/{chatRoomId}") { backStackEntry ->
            val optionId = backStackEntry.arguments?.getString("chatRoomId")
                ?: throw IllegalStateException("where is the chatRoomId")
            val chatRoomOptionId = optionId.toIntOrNull()
            Log.d("ChatRoom", "The id is $optionId")
            if (chatRoomOptionId != null) {
                ChatRoom(
                    navController,
                    chatRoomId = chatRoomOptionId,
                    myId = customerId,
                    mediaViewModel = mediaViewModel,
                    contactName = fullName
                )
            }
        }
        composable("dmList") { DmList(navController, customerId) }
        composable("dmChatScreen/{enemyId}/{enemyName}") { backStackEntry ->
            val enemyId = backStackEntry.arguments?.getString("enemyId")?.toIntOrNull()
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
                customerId = customerId,
                viewModel = profileViewModel
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
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("registration") {
        }
    }
}