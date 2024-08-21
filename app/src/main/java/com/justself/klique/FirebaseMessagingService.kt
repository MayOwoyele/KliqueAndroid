package com.justself.klique

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.data.isNotEmpty().let {
            handleDataMessage(remoteMessage.data)
        }
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val isSilent = data["isSilent"]?.toBoolean() ?: false
        val customerId = data["customerId"] ?: ""
        val name = data["name"] ?: ""
        val profilePhoto = data["profilePhoto"] ?: ""
        val isVerified = data["isVerified"]?.toBoolean() ?: false
        if (isSilent) {
            passSilentUpdateToNavigationHost(customerId, name, profilePhoto, isVerified)
        } else {
            val title = data["title"] ?: "Klique Notification"
            val messageBody = data["messageBody"] ?: "Default"
            val notificationType = data["notificationType"] ?: "chat"
            sendNotification(title, messageBody, notificationType, customerId, name)
        }
    }

    private fun passSilentUpdateToNavigationHost(
        customerId: String,
        name: String,
        profilePhoto: String,
        isVerified: Boolean
    ) {
        val intent = Intent(this, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("customerId", customerId)
            putExtra("contactName", name)
            putExtra("profilePhoto", profilePhoto)
            putExtra("isVerified", isVerified)
        }
        startActivity(intent)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        notificationType: String,
        customerId: String = "",
        name: String = ""
    ) {
        val route = when (notificationType) {
            "chat" -> "messageScreen/$customerId/$name"
            "dm" -> "dmList"
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
        sendTokenToServer(token)
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private fun sendTokenToServer(token: String) {
        Log.d("Token", "The token is $token")
        val params = mapOf("token" to token)
        serviceScope.launch {
            NetworkUtils.makeRequest("onFireBaseToken", params = params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}