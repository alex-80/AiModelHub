package com.ai_model_hub.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ai_model_hub.MainActivity
import com.ai_model_hub.R

internal const val SERVICE_NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "ai_hub_service"

internal fun createServiceNotificationChannel(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (manager.getNotificationChannel(CHANNEL_ID) != null) return
    val channel = NotificationChannel(
        CHANNEL_ID,
        "AI Model Hub Service",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Keeps the AI inference service running in the background"
        setShowBadge(false)
    }
    manager.createNotificationChannel(channel)
}

internal fun buildServiceNotification(context: Context): Notification {
    val tapIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    return NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("AI Model Hub")
        .setContentText("Ready for AI inference")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentIntent(tapIntent)
        .setOngoing(true)
        .setSilent(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()
}
