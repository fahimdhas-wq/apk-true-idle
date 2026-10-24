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

        // Record to full history (capped at 300)
        val history = (state.notificationHistory + notif).takeLast(300)

        val isCritical = status.equals("ALERT", ignoreCase = true) ||
                status.equals("CRITICAL", ignoreCase = true) ||
                type.equals("combat", ignoreCase = true)

        if (!state.notificationBannersEnabled && !isCritical) {
            return state.copy(notifications = emptyList(), notificationHistory = history)
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
