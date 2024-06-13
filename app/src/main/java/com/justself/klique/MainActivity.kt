package com.justself.klique

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiCompat.Config
import androidx.emoji2.bundled.BundledEmojiCompatConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val config: Config = BundledEmojiCompatConfig(this)
        EmojiCompat.init(config)
        setContent {
            MyAppTheme {
                Surface {
                    MainScreen()  // Make MainScreen the root composable
                }
            }
        }

        // Request notification permission for Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
    }
}
