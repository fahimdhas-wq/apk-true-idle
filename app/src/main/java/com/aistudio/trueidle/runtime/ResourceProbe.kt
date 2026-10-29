package com.aistudio.trueidle.runtime

import android.os.Debug
import android.os.Process
import android.os.SystemClock

/** One live reading of this process's own cost. */
data class ResourceSample(val cpuPercent: Double, val ramMb: Double)

/**
 * Measures this process only: CPU time consumed per wall-clock time (percent of one core) and
 * proportional set size. Stateful: each [sample] reports the interval since the previous call.
 */
class ResourceProbe {
    private var lastCpuMs = Process.getElapsedCpuTime()
    private var lastWallMs = SystemClock.elapsedRealtime()

    fun sample(): ResourceSample {
        val cpu = Process.getElapsedCpuTime()
        val wall = SystemClock.elapsedRealtime()
        val dWall = (wall - lastWallMs).coerceAtLeast(1L)
        val pct = ((cpu - lastCpuMs).coerceAtLeast(0L).toDouble() / dWall) * 100.0
        lastCpuMs = cpu
        lastWallMs = wall
        return ResourceSample(cpuPercent = pct, ramMb = Debug.getPss() / 1024.0)
    }
}
