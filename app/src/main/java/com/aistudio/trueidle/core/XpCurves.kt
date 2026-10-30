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
     * Gathering skill levels are capped at 100. Their requirements use Double because actual
     * proficiency-adjusted yields can exceed Long.MAX_VALUE and global skill XP now follows the
     * actual number of items received.
     *
     * Both curves use a 1.2 multiplicative increase per level after the level^2.3 base. The
     * coefficients are separately calibrated against the current 250 ms game step, tick-rounded
     * resource durations, all available plots/veins, level-gated mining unlocks, and level-10
     * auto-collect. The calibration assumes every resource has reached proficiency 100 (2^99 yield),
     * no speed/yield upgrade ranks, one best-XP-per-second queued resource before auto-collect,
     * and all resources running in auto-collect from level 10 onward. Under that deliberately
     * fastest sustained-production model, both skills reach level 100 in approximately 4.5 years.
     *
     * Actual saves start at proficiency 1; their time will be longer until proficiency improves.
     */
    const val GATHERING_SKILL_MAX_LEVEL: Int = 100
    private const val GATHERING_XP_GROWTH_PER_LEVEL: Double = 1.2
    private const val FARMING_XP_BASE: Double = 9.159602962618268E27
    private const val MINING_XP_BASE: Double = 1.5464029483610142E28

    fun farmingSkillXp(lvl: Int): Double = gatheringRequirement(FARMING_XP_BASE, lvl)

    fun miningSkillXp(lvl: Int): Double = gatheringRequirement(MINING_XP_BASE, lvl)

    private fun gatheringRequirement(base: Double, lvl: Int): Double {
        val level = lvl.coerceIn(1, GATHERING_SKILL_MAX_LEVEL)
        val requirement = base *
            level.toDouble().pow(2.3) *
            GATHERING_XP_GROWTH_PER_LEVEL.pow((level - 1).toDouble())
        return if (requirement.isFinite()) requirement else Double.MAX_VALUE
    }

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
