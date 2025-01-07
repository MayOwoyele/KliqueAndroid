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

class FirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        const val FIREBASE_PREFS_KEY = "firebase_prefs"
        const val FIREBASE_TOKEN_KEY = "firebase_token"
    }

    private val notificationList = mutableListOf<String>()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("Firebase", "Firebase triggered")
        if (remoteMessage.notification != null) {
            Log.d("Firebase", "Notification + Data Message: ${remoteMessage.notification}")
            handleNotificationWithData(remoteMessage.notification!!, remoteMessage.data)
            return
        }

        if (remoteMessage.data.isNotEmpty()) {
            Log.d("Firebase", "Data Message Received: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
    }
    private fun handleNotificationWithData(notification: RemoteMessage.Notification, data: Map<String, String>) {
        val title = notification.title ?: "Klique Klique"
        val messageBody = notification.body ?: "You have new notifications"
        val destination = data["destination"] ?: "home"
        sendIndividualNotification(title, messageBody, destination)
    }
    private fun sendIndividualNotification(
        title: String,
        messageBody: String,
        destination: String
    ) {
        val route = when (destination) {
            "chats" -> "chats"
            "shots" -> "dmList"
            "home" -> "home"
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

        val channelId = "individual_channel"

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Individual Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = System.currentTimeMillis().toInt()
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.klique_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Klique Klique"
        val messageBody = data["body"] ?: "You have new notifications"
        val destination = data["destination"] ?: "home"

        sendNotification(title, messageBody, destination)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        destination: String
    ) {
        val groupKey = "com.justself.klique.NOTIFICATION_GROUP"
        notificationList.add(messageBody)
        val route = when (destination) {
            "chats" -> "chats"
            "shots" -> "dmList"
            "home" -> "home"
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

        val channelId = when (destination) {
            "shots" -> "dm_channel"
            "chats" -> "chat_channel"
            else -> "default_channel"
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            when (destination) {
                "shots" -> "Direct Messages"
                "chats" -> "Chat Messages"
                else -> "General Notifications"
            },
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = System.currentTimeMillis().toInt()
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.klique_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setGroup(groupKey)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notificationId, notificationBuilder.build())
        val summaryText = if (notificationList.size > 1) {
            "+${notificationList.size - 1} more"
        } else {
            "1 new message"
        }

        val inboxStyle = NotificationCompat.InboxStyle()
        notificationList.forEach { inboxStyle.addLine(it) }
        inboxStyle.setSummaryText(summaryText)
        val summaryBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.klique_icon)
            .setContentTitle("Klique Notifications")
            .setContentText("You have ${notificationList.size} new messages")
            .setStyle(inboxStyle)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, summaryBuilder.build())
    }

    override fun onNewToken(token: String) {
        Log.d("NewToken", "on new token called")
        saveTokenToPreferences(token)
        SessionManager.sendDeviceTokenToServer()
    }

    private fun saveTokenToPreferences(token: String) {
        val sharedPreferences = getSharedPreferences(FIREBASE_PREFS_KEY, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(FIREBASE_TOKEN_KEY, token)
        editor.apply()
        Log.d("TokenSave", "Token saved: $token")
    }

    override fun onDestroy() {
        super.onDestroy()
        CoroutineScope(Dispatchers.IO).cancel()
    }
}