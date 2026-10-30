package com.aistudio.trueidle.engine

import com.aistudio.trueidle.model.GameState

/** Recomputes achievement progress, touching only entries whose counter or lock state moved. */
internal object AchievementSystem {

    /** Returns the new state, or null when nothing changed. */
    fun evaluate(state: GameState, force: Boolean): GameState? {
        // Called every tick: bail out before allocating a new list when nothing moved.
        if (!force && state.achievements.all { a ->
                a.isUnlocked ||
                    (minOf(progressFor(state, a.id), a.target) == a.progress && a.progress < a.target)
            }
        ) return null

        var changed = false
        val updated = state.achievements.map { achievement ->
            if (achievement.isUnlocked) {
                achievement
            } else {
                val original = achievement.progress
                val progress = minOf(progressFor(state, achievement.id), achievement.target)
                val unlocked = progress >= achievement.target
                if (!force && progress == original && !unlocked) {
                    achievement
                } else {
                    if (progress != original || unlocked) changed = true
                    achievement.copy(progress = progress, isUnlocked = unlocked)
                }
            }
        }
        return if (changed) state.copy(achievements = updated) else null
    }

    private fun progressFor(state: GameState, id: String): Int = when (id) {
        "ach_first_blood" -> state.lifetime.totalKills.toInt()
        "ach_floor_10" -> state.player.maxUnlockedFloor
        // The starting Dusk Wolf counts; the old lifetime-only figure never included it.
        "ach_tamer" -> state.lifetime.beastsTamed.toInt() + 1
        "ach_necro" -> state.necro.army.sumOf { it.count }
        "ach_miner" -> state.lifetime.oresMined.toInt()
        else -> 0
    }
}
