package com.aistudio.trueidle.core

interface GameClock {
    fun wallMs(): Long
    fun elapsedMs(): Long
    fun bootCount(): Int
}

data class ClockSnapshot(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)

fun GameClock.snapshot() = ClockSnapshot(wallMs(), elapsedMs(), bootCount())
