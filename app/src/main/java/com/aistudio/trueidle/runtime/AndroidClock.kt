package com.aistudio.trueidle.runtime

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.aistudio.trueidle.core.GameClock

class AndroidClock(private val context: Context) : GameClock {
    override fun wallMs(): Long = System.currentTimeMillis()
    override fun elapsedMs(): Long = SystemClock.elapsedRealtime()
    override fun bootCount(): Int =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}
