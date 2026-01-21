package com.justself.klique

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Surface
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.ContextCompat
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.Config
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.requestUpdateFlow
import com.justself.klique.DroidAppUpdateManager.ASK_AFTER
import com.justself.klique.DroidAppUpdateManager.FORCE_AFTER
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.justself.klique.ContactsBlock.Contacts.repository.ContactsRepository
import com.justself.klique.ContactsBlock.Contacts.ui.ContactsViewModel
import kotlinx.coroutines.launch
import com.justself.klique.Authentication.ui.screens.RegistrationScreen
import com.justself.klique.Authentication.ui.viewModels.AppState
import com.justself.klique.Authentication.ui.viewModels.AuthViewModel
import com.justself.klique.databinding.ActivityMainBinding
import com.justself.klique.nav.NavigationManager
import com.justself.klique.nav.KliqueVMStore
import com.justself.klique.screenControllers.BaseScreen
import kotlinx.coroutines.flow.catch

private const val REQ_IMMEDIATE_UPDATE = 1001
private const val REQ_FLEX_UPDATE      = 1002
class MainActivity : AppCompatActivity() {
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }

    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { (permission, granted) ->
                Logger.d("Permissions", "$permission granted: $granted")
            }
            val allPermissionsGranted = permissions.entries.all { it.value }

            if (allPermissionsGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        }
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeInAppUpdates()
        val executor: Executor = Executors.newSingleThreadExecutor()
        val config: Config = BundledEmojiCompatConfig(this, executor)
        EmojiCompat.init(config)
        val notificationRoute = intent?.getStringExtra("route")
        if (notificationRoute != null) {
            NotificationIntentManager.updateNavigationRoute(notificationRoute)
        }
        val contactsRepository by lazy { ContactsRepository(contentResolver, this) }
        val authViewModel: AuthViewModel by viewModels()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            authViewModel.appState
                .collect { state ->
                    when (state) {
                        AppState.Loading -> {
                            // Optionally show a splash/loading indicator; default is no-op
                        }
                        AppState.LoggedOut -> {
                            showLoginCompose()
                        }
                        AppState.LoggedIn -> {
                            showLoggedInNavHost()
                        }
                    }
                }
        }
//        setContent {
//            val contactViewModel: ContactsViewModel = viewModel(
//                factory = ContactsViewModelFactory(contactsRepository)
//            )
//            CompositionLocalProvider(
//                LocalContactsViewModel provides contactViewModel
//            ) {
//                val navController = rememberNavController()
//                MyAppTheme {
//                    Surface {
//                        MainScreen(navController = navController)
//                    }
//                }
//            }
//        }

        Logger.d("Permissions", "Requesting permissions...")
        if (!allPermissionsGranted(requiredPermissions)) {
            Logger.d("Permissions", "Requesting required permissions...")
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            Logger.d("Permissions", "Required permissions already granted")
        }
    }
    private fun showLoginCompose() {
        binding.layoutContainer.visibility = View.GONE
        binding.loginComposeView.apply {
            visibility = View.VISIBLE
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            setContent {
                Surface {
                    RegistrationScreen()
                }
            }
        }
    }

    /** Replace the ComposeView with a NavHostFragment that drives logged‐in navigation. */
    private lateinit var navManager: NavigationManager

    private fun showLoggedInNavHost() {
        binding.loginComposeView.visibility = View.GONE

        binding.layoutContainer.visibility = View.VISIBLE

        navManager = NavigationManager(
            theContainer  = binding.layoutContainer,
            viewModelStore = KliqueVMStore()
        )

        val base = BaseScreen(
            context        = this,
            nav            = navManager
        )
        Logger.d("NavManager", "is running")
        navManager.goTo(base)
    }
    override fun onResume() {
        super.onResume()
        DroidAppUpdateManager.resumeFlexibleUpdates()
    }

    private fun observeInAppUpdates() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppUpdateManagerFactory
                    .create(this@MainActivity)
                    .requestUpdateFlow()
                    .catch { e ->
                        // This will catch InstallException(-6) and any other errors from the flow
                        if (e is InstallException &&
                            e.errorCode == InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED
                        ) {
                            Logger.e("UpdateManager", "🚫 Update not allowed by device state")
                        } else {
                            Logger.e("UpdateManager", "🔥 Unexpected update error")
                        }
                        // swallow it so it doesn't crash
                    }
                    .collect { result ->
                        if (result is AppUpdateResult.Available) {
                            val elapsed = System.currentTimeMillis() - DroidAppUpdateManager.getFirstSeen()
                            val force   = elapsed >= FORCE_AFTER
                            val ask     = elapsed in ASK_AFTER until FORCE_AFTER

                            // Now your previous runCatching is a nice extra guard,
                            // but the flow.catch will already have intercepted the crash.
                            result.tryStartUpdate(this@MainActivity, force)
                        }
                    }
            }
        }
    }
    private fun AppUpdateResult.Available.tryStartUpdate(
        activity: MainActivity,
        force: Boolean
    ) {
        runCatching {
            if (force && updateInfo.isImmediateUpdateAllowed) {
                startImmediateUpdate(activity, REQ_IMMEDIATE_UPDATE)
            } else if (!force &&
                updateInfo.isFlexibleUpdateAllowed &&
                !DroidAppUpdateManager.updateDismissedFlow.value
            ) {
                startFlexibleUpdate(activity, REQ_FLEX_UPDATE)
            } else {

            }
        }.onFailure { e ->
            if (e is InstallException &&
                e.errorCode == InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED
            ) {
                Logger.e("UpdateManager", "🚫 Update blocked by device state – battery, storage, etc.")
                // optional: Toast.makeText(activity, "...", LENGTH_LONG).show()
            } else {
                Logger.e("UpdateManager", "❌ startUpdate failed: ${e.message}")
            }
        }
    }

    private fun allPermissionsGranted(permissions: Array<String>) = permissions.all {
        val granted =
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        Logger.d("Permissions", "$it granted: $granted")
        granted
    }
}
val LocalContactsViewModel = staticCompositionLocalOf<ContactsViewModel> {
    error("No ContactsViewModel provided")
}
enum class TabRoots(val tag: String){
    Home("Home"),
    Chat("Chat"),
    BookShelf("BookShelf");
    companion object {
        fun fromTag(tag: String): TabRoots? = TabRoots.entries.find { it.tag == tag }
    }
}