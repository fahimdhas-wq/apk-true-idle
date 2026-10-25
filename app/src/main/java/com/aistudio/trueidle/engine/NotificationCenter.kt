package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.Ids
import com.aistudio.trueidle.model.GameNotification
import com.aistudio.trueidle.model.GameState

class NotificationCenter(private val ids: Ids) {

    fun push(
        state: GameState,
        title: String,
        message: String,
        type: String = "info",
        status: String = "SUCCESS",
        detail: String = "",
        wallMs: Long = 0L
    ): GameState {
        val now = if (wallMs > 0L) wallMs else state.meta.savedWallMs
        val notif = GameNotification(
            id = ids.next(),
            title = title,
            message = message,
            type = type,
            count = 1,
            timestamp = now,
            status = status,
            detail = detail
        )

        // Record to full history archive (capped at 300)
        val history = (state.notificationHistory + notif).takeLast(300)

        // 1. If notification banners are disabled, strictly silence ALL floating popups
        if (!state.notificationBannersEnabled) {
            return state.copy(notifications = emptyList(), notificationHistory = history)
        }

        // 2. Reduce notification pressure: Only milestone/major alerts qualify for floating banner popups
        val isMilestoneAlert = status.equals("ALERT", ignoreCase = true) ||
                status.equals("CRITICAL", ignoreCase = true) ||
                type.equals("level", ignoreCase = true) ||
                type.equals("milestone", ignoreCase = true) ||
                type.equals("tame", ignoreCase = true) ||
                title.contains("LEVEL", ignoreCase = true) ||
                title.contains("CLEARED", ignoreCase = true) ||
                title.contains("RETREAT", ignoreCase = true) ||
                title.contains("PROMOTION", ignoreCase = true) ||
                title.contains("ANOMALY", ignoreCase = true)

        if (!isMilestoneAlert) {
            // Low-pressure events stay in history archive & combat/system log without popping up
            return state.copy(notificationHistory = history)
        }

        val currentList = state.notifications.toMutableList()
        // Anti-stacking: match exact (type, title, message) within 2000ms
        val existingIndex = currentList.indexOfFirst {
            it.type == type && it.title == title && it.message == message &&
                    (now - it.timestamp) < 2000L
        }

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(
                count = existing.count + 1,
                timestamp = now,
                status = status,
                detail = detail
            )
        } else {
            currentList.add(notif)
        }

        return state.copy(
            notifications = currentList.takeLast(3),
            notificationHistory = history
        )
    }
}
