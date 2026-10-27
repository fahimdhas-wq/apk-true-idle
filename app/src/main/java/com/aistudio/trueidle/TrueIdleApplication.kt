package com.aistudio.trueidle

import android.app.Application
import android.util.Log
import com.aistudio.trueidle.core.AppLog
import com.aistudio.trueidle.runtime.Notifications

class TrueIdleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Wire the logging seam once; the pure modules never import android.util.Log themselves.
        AppLog.logger = { tag, message, throwable -> Log.w(tag, message, throwable) }
        // The notification channel is created exactly once for the process.
        Notifications.ensureChannel(this)
    }
}
