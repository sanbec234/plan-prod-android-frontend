package com.hanghub.app.core.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.hanghub.app.MainActivity
import com.hanghub.app.R

/**
 * Receives FCM pushes and surfaces them as notifications carrying deep-link
 * extras. Dormant until Firebase is configured (no google-services.json yet) —
 * registered now so push works the moment the backend + Firebase are wired.
 */
class HangHubMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "HangHub"
        val body = message.notification?.body ?: data["body"] ?: ""
        showNotification(title, body, data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "HangHub", NotificationManager.IMPORTANCE_HIGH)
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data["chatId"]?.let { putExtra("chatId", it) }
            data["planId"]?.let { putExtra("planId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_nav_discovery)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "hh_default"
    }
}
