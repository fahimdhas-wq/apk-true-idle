package com.aistudio.trueidle.runtime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Schedules the eight-hour idle alert while the app is in the background and cancels it on return,
 * so the notification fires when the window actually elapses instead of on foreground.
 */
object IdleAlertScheduler {
    private const val REQUEST_CODE = 8001

    fun schedule(context: Context, triggerAtMs: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        // Inexact-but-idle-friendly: needs no SCHEDULE_EXACT_ALARM permission and survives Doze.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMs,
            pendingIntent(context)
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, IdleAlertReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class IdleAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notifications.ensureChannel(context)
        Notifications.postIdleReady(context)
    }
}
