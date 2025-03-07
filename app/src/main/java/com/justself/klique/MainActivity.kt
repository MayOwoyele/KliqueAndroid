package com.justself.klique

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.navigation.compose.rememberNavController
import java.util.concurrent.Executor
import java.util.concurrent.Executors



class MainActivity : ComponentActivity() {

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
                Log.d("Permissions", "$permission granted: $granted")
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

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val executor: Executor = Executors.newSingleThreadExecutor() // 4 threads in the pool
        val config: Config = BundledEmojiCompatConfig(this, executor)
        EmojiCompat.init(config)
        val notificationRoute = intent?.getStringExtra("route")
        if (notificationRoute != null) {
            NotificationIntentManager.updateNavigationRoute(notificationRoute)
        }
        setContent {
            val navController = rememberNavController()
            MyAppTheme {
                Surface {
                    MainScreen(notificationRoute, navController = navController)
                }
            }
        }

        Log.d("Permissions", "Requesting permissions...")
        if (!allPermissionsGranted(requiredPermissions)) {
            Log.d("Permissions", "Requesting required permissions...")
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            Log.d("Permissions", "Required permissions already granted")
        }
    }

    private fun allPermissionsGranted(permissions: Array<String>) = permissions.all {
        val granted =
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        Log.d("Permissions", "$it granted: $granted")
        granted
    }
}