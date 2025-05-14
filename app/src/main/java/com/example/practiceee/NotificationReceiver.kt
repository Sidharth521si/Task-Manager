package com.example.practiceee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val task = intent.getStringExtra("task") ?: "Task Reminder"
        val soundEnabled = intent.getBooleanExtra("sound_enabled", true) // <-- Default to true

        val channelId = "task_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Task Reminder",
                if (soundEnabled) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_LOW
            )
            if (!soundEnabled) {
                channel.setSound(null, null)
                channel.enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Task Reminder")
            .setContentText(task)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)

        if (soundEnabled) {
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        } else {
            builder.setDefaults(0)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}




