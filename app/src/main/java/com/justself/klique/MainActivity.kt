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
import com.google.firebase.FirebaseApp

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
        FirebaseApp.initializeApp(this)
        NetworkUtils.initialize(this)
        WebSocketManager.initialize(this)
        enableEdgeToEdge()
        val config: Config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
        setContent {
            MyAppTheme {
                Surface {
                    MainScreen(intent = intent)
                }
            }
        }

        Log.d("Permissions", "Requesting permissions...")
        if (!allPermissionsGranted(requiredPermissions)) {
            Log.d("Permissions", "Requesting required permissions...")
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            Log.d("Permissions", "Required permissions already granted")
            Toast.makeText(this, "Permissions already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted(permissions: Array<String>) = permissions.all {
        val granted =
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        Log.d("Permissions", "$it granted: $granted")
        granted
    }
}