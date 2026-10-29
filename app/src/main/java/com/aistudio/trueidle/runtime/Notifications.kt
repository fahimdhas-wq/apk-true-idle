package com.aistudio.trueidle.runtime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aistudio.trueidle.R

/** Notification channel and poster. The channel is created once, from the Application. */
object Notifications {
    const val CHANNEL_ID = "offline_recon"
    const val IDLE_NOTIFICATION_ID = 8001
    const val BACKGROUND_CHANNEL_ID = "background_run"
    const val BACKGROUND_NOTIFICATION_ID = 8002

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_offline_recon),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        if (manager.getNotificationChannel(BACKGROUND_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    BACKGROUND_CHANNEL_ID,
                    context.getString(R.string.notification_channel_background_run),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    /**
     * Posted by [IdleAlertReceiver] when the idle window elapses while the app is backgrounded — not
     * on foreground, where the user would already be looking at the report dialog.
     */
    fun postIdleReady(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        // POST_NOTIFICATIONS is a runtime permission on API 33+. Without this explicit check the
        // notify() call below would throw SecurityException once the user has denied it.
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launch?.let {
            PendingIntent.getActivity(
                context,
                IDLE_NOTIFICATION_ID,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            // Monochrome vector: a coloured layer-list renders as a solid blob in the status bar.
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_idle_title))
            .setContentText(context.getString(R.string.notification_idle_text))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_idle_big_text))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply { if (pendingIntent != null) setContentIntent(pendingIntent) }
            .build()

        runCatching { manager.notify(IDLE_NOTIFICATION_ID, notification) }
    }
}
