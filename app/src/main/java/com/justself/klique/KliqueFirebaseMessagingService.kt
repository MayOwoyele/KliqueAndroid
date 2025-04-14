package com.justself.klique

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class KliqueFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        const val FIREBASE_PREFS_KEY = "firebase_prefs"
        const val FIREBASE_TOKEN_KEY = "firebase_token"
        // Maintain a mapping from enemyId to list of notification IDs for pChat messages.
        val pChatNotificationIds = mutableMapOf<Int, MutableList<Int>>()
    }

    private val notificationList = mutableListOf<String>()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("Firebase", "Firebase triggered")
        if (remoteMessage.notification != null) {
            Log.d("Firebase", "Notification + Data Message: ${remoteMessage.notification}")
            handleNotificationWithData(remoteMessage.notification!!)
            return
        }
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("Firebase", "Data Message Received: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }
    }

    private fun handleNotificationWithData(notification: RemoteMessage.Notification) {
        val title = notification.title ?: "Klique Klique"
        val messageBody = notification.body ?: "You have new notifications"
        sendIndividualNotification(title, messageBody)
    }

    private fun sendIndividualNotification(title: String, messageBody: String) {
        val deepLinkUri = Uri.parse("kliqueklique://home")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val channelId = "individual_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    fun handleDataMessage(data: Map<String, String>) {
        val title = data["title"] ?: "Klique Klique"
        val messageBody = data["body"] ?: "You have new notifications"
        sendNotification(title, messageBody, data)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        data: Map<String, String>
    ) {
        val groupKey = "com.justself.klique.NOTIFICATION_GROUP"
        notificationList.add(messageBody)
        Log.d("Firebase", "Notification List: $notificationList")
        val destination = data["destination"] ?: "home"
        val pChat = "pChat"
        val enemyId = if (destination == pChat) {
            data["enemyId"]?.toIntOrNull() ?: 0
        } else {
            0
        }
        if (destination == pChat){
            ChatVMObject.callFetch()
        }
        val deepLinkUri = when (destination) {
            pChat -> {
                Uri.parse("kliqueklique://messageScreen/$enemyId")
            }
            "gist" -> {
                val gistId = data["gistId"]
                val commentId = data["commentId"]
                val uriString = if (!gistId.isNullOrBlank()) {
                    if (!commentId.isNullOrBlank()) {
                        "kliqueklique://home?gistId=${gistId}&commentId=${commentId}"
                    } else {
                        "kliqueklique://home?gistId=${gistId}"
                    }
                } else {
                    "kliqueklique://home"
                }
                Uri.parse(uriString)
            }
            "shot" -> {
                val theEnemyId = data["enemyId"]?.toIntOrNull() ?: 0
                val enemyName = data["enemyName"] ?: "No Name"
                Uri.parse("kliqueklique://dmChatScreen/$theEnemyId/${Uri.encode(enemyName)}")
            }
            else -> {
                Uri.parse("kliqueklique://$destination")
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (destination == pChat && enemyId != 0) {
                putExtra("enemyId", enemyId)
            }
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
        val finalGroupKey = if (destination == pChat && enemyId != 0) "pChat_$enemyId" else groupKey
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.klique_icon)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setGroup(finalGroupKey)
            .setContentIntent(pendingIntent)
        notificationManager.notify(notificationId, notificationBuilder.build())

        // If this is a pChat notification, record its ID.
        if (destination == pChat && enemyId != 0) {
            val list = pChatNotificationIds.getOrPut(enemyId) { mutableListOf() }
            list.add(notificationId)
            val summaryText = "You have ${list.size} messages"
            val inboxStyle = NotificationCompat.InboxStyle()
            notificationList.forEach { inboxStyle.addLine(it) }
            inboxStyle.setSummaryText(summaryText)
            val summaryBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.klique_icon)
                .setContentTitle("Klique Notifications")
                .setContentText(summaryText)
                .setStyle(inboxStyle)
                .setGroup(finalGroupKey)
                .setGroupSummary(true)
                .setContentIntent(pendingIntent)
            notificationManager.notify(enemyId, summaryBuilder.build())
        } else {
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