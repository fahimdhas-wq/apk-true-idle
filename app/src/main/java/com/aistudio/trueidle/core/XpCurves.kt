package com.aistudio.trueidle.core

import kotlin.math.floor
import kotlin.math.pow

/**
 * XP requirement curves. These live in `core` so the UI can read the same numbers the engine uses
 * without importing `engine.Balance` (see ArchitectureTest).
 */
object XpCurves {
    /**
     * The old engine curve was `1000 * lvl^3`, which needed roughly 81,000 manual harvests to reach
     * level 10 — the level that unlocks auto-collect. This keeps a long tail that is still reachable.
     */
    fun skill(lvl: Int): Long =
        floor(90.0 * lvl.coerceAtLeast(1).toDouble().pow(2.3)).toLong().coerceAtLeast(100L)

    /**
     * Farming and mining are rewarded repeatedly during idle play, so they use a slower curve.
     * The XP required at each level is five times the general skill curve, without slowing
     * unrelated disciplines.
     */
    fun gatheringSkill(lvl: Int): Long =
        floor(450.0 * lvl.coerceAtLeast(1).toDouble().pow(2.3)).toLong().coerceAtLeast(450L)

    fun slayer(lvl: Int): Long =
        floor(25.0 * lvl.coerceAtLeast(1).toDouble().pow(2.3)).toLong().coerceAtLeast(50L)

    fun player(lvl: Int): Long {
        val level = lvl.coerceAtLeast(1)
        return floor(60.0 * level * level + 40.0 * level).toLong().coerceAtLeast(100L)
    }

    /**
     * Individual item proficiency curve for herbs and ores (Levels 1 to 100).
     */
    fun proficiency(lvl: Int): Long {
        val level = lvl.coerceAtLeast(1)
        return floor(30.0 * level.toDouble().pow(1.8) + 20.0 * level).toLong().coerceAtLeast(40L)
    }

    /**
     * Yield multiplier doubling each level (1x at lvl 1, 2x at lvl 2, 4x at lvl 3 ... up to lvl 100).
     */
    fun proficiencyYieldMultiplier(lvl: Int): Double {
        val cappedLvl = lvl.coerceIn(1, 100)
        return 2.0.pow((cappedLvl - 1).toDouble())
    }
}
