package com.justself.klique

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        sendTokenIfExists()
    }
    private fun sendTokenIfExists() {
        val sharedPreferences = getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("firebase_token", null)

        if (!token.isNullOrEmpty()) {
            Log.d("TokenCheck", "Token exists, sending to server: $token")
            sendTokenToServer(token)
        } else {
            Log.d("TokenCheck", "No token found in SharedPreferences.")
        }
    }
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("Firebase", "Firebase triggered")
        remoteMessage.notification?.let {
            val title = it.title ?: "Klique Notification"
            val messageBody = it.body ?: "You have new notifications"
            val notificationType = remoteMessage.data["notificationType"] ?: "chat"
            val customerId = remoteMessage.data["customerId"] ?: ""
            val name = remoteMessage.data["name"] ?: ""
            sendNotification(title, messageBody, notificationType, customerId, name)
        }
        remoteMessage.data.isNotEmpty().let {
            handleDataMessage(remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val customerId = data["customerId"] ?: ""
        val name = data["name"] ?: ""
        val profilePhoto = data["profilePhoto"] ?: ""
        val isVerified = data["isVerified"]?.toBoolean() ?: false
        val profileData = ProfileUpdateData(customerId.toInt(), name, profilePhoto, isVerified)
        ProfileRepository.updateProfileData(profileData)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        notificationType: String,
        customerId: String = "",
        name: String = ""
    ) {
        val route = when {
            notificationType == "chat" && customerId.isNotEmpty() && name.isNotEmpty() -> {
                "messageScreen/$customerId/$name"
            }
            notificationType == "dm" -> "dmList"
            else -> "home"
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("route", route)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = when (notificationType) {
            "dm" -> "dm_channel"
            "chat" -> "chat_channel"
            else -> "default_channel"
        }
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.klique_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            when (notificationType) {
                "dm" -> "Direct Messages"
                "chat" -> "Chat Messages"
                else -> "General Notifications"
            },
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d("NewToken", "on new token called")
        saveTokenToPreferences(token)
        sendTokenToServer(token)
        logTokenData()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private fun sendTokenToServer(token: String) {
        Log.d("Token", "The token is $token")
        val params = mapOf("token" to token)
        serviceScope.launch {
            try {
                NetworkUtils.makeRequest("onFireBaseToken", params = params)
            } catch (e: Exception) {
                Log.e("SendToken", "Failed to send token to server: ${e.message}")
            }
        }
    }
    private fun saveTokenToPreferences(token: String) {
        val sharedPreferences = getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("firebase_token", token)
        editor.apply()
        Log.d("TokenSave", "Token saved: $token")
    }
    private fun logTokenData() {
        val sharedPreferences = getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("firebase_token", null)

        if (token == null) {
            Log.d("TokenData", "Firebase Token is null. This might be the first launch or token hasn't been generated yet.")
        } else {
            Log.d("TokenData", "Current Firebase Token: $token")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}