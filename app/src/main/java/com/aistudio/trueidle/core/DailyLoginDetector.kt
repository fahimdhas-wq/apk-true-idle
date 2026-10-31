package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.DailyLoginState
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Utility for detecting calendar day transitions, countdown to midnight, and daily claim eligibility.
 */
object DailyLoginDetector {

    /**
     * Calculates the local calendar epoch day (days since Jan 1 1970 in user's local timezone).
     */
    fun currentEpochDay(nowMs: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): Long {
        val offset = timeZone.getOffset(nowMs)
        return (nowMs + offset) / (24L * 60L * 60L * 1000L)
    }

    /**
     * Calculates milliseconds remaining until midnight (the start of the next calendar day).
     */
    fun msUntilNextDay(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return (cal.timeInMillis - nowMs).coerceAtLeast(0L)
    }

    /**
     * Formats milliseconds into a readable HH:MM:SS countdown.
     */
    fun formatCountdown(ms: Long): String {
        val totalSecs = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSecs / 3600L
        val minutes = (totalSecs % 3600L) / 60L
        val seconds = totalSecs % 60L
        return String.format(Locale.ROOT, "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    /**
     * Checks if the 7-day sprint track is claimable today.
     */
    fun canClaimSprint(daily: DailyLoginState, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (daily.sprint7ClaimedDays >= 7) return false
        val currentDay = currentEpochDay(nowMs)
        return daily.lastSprintClaimEpochDay != currentDay
    }

    /**
     * Checks if the 30-day calendar track is claimable today.
     */
    fun canClaimCalendar(daily: DailyLoginState, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (daily.calendar30ClaimedDays >= 30) return false
        val currentDay = currentEpochDay(nowMs)
        return daily.lastCalendarClaimEpochDay != currentDay
    }

    /**
     * Returns true if either the sprint or calendar reward is available to claim today.
     */
    fun hasAnyUnclaimedReward(daily: DailyLoginState, nowMs: Long = System.currentTimeMillis()): Boolean {
        return canClaimSprint(daily, nowMs) || canClaimCalendar(daily, nowMs)
    }
}
