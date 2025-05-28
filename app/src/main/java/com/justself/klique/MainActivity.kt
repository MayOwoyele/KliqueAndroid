package com.justself.klique

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.Config
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.requestUpdateFlow
import com.justself.klique.DroidAppUpdateManager.ASK_AFTER
import com.justself.klique.DroidAppUpdateManager.FORCE_AFTER
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import kotlinx.coroutines.launch

private const val REQ_IMMEDIATE_UPDATE = 1001
private const val REQ_FLEX_UPDATE      = 1002
class MainActivity : FragmentActivity() {

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
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        setContent {
            val navController = rememberNavController()
            MyAppTheme {
                Surface {
                    MainScreen(navController = navController)
                }
            }
        }

        Logger.d("Permissions", "Requesting permissions...")
        if (!allPermissionsGranted(requiredPermissions)) {
            Logger.d("Permissions", "Requesting required permissions...")
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            Logger.d("Permissions", "Required permissions already granted")
        }
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
                    .collect { result ->
                        if (result is AppUpdateResult.Available) {
                            val info      = result.updateInfo
                            val elapsed   = System.currentTimeMillis() - DroidAppUpdateManager.getFirstSeen()
                            val needForce = elapsed >= FORCE_AFTER
                            val needAsk   = elapsed in ASK_AFTER until FORCE_AFTER

                            when {
                                needForce && info.isImmediateUpdateAllowed -> {
                                    result.startImmediateUpdate(this@MainActivity, REQ_IMMEDIATE_UPDATE)
                                }
                                needAsk && info.isFlexibleUpdateAllowed &&
                                        !DroidAppUpdateManager.updateDismissedFlow.value -> {
                                    result.startFlexibleUpdate(this@MainActivity, REQ_FLEX_UPDATE)
                                }
                            }
                        }
                    }
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