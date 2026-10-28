package com.aistudio.trueidle.core

/**
 * Slayer mastery reward scaling. The engine writes the scaled bounty into the issued contract and
 * the Slayer screen quotes this same table, so the screen can never advertise a bonus the engine
 * does not pay.
 */
object SlayerMath {
    /** Every this many Slayer levels grants one extra bounty point per contract. */
    const val LEVELS_PER_BONUS_POINT = 10

    fun bonusPoints(level: Int): Int = (level.coerceAtLeast(1) - 1) / LEVELS_PER_BONUS_POINT

    /** Credits bonus in percent: +2% per Slayer level above 1. */
    fun bonusGoldPercent(level: Int): Int = (level.coerceAtLeast(1) - 1) * 2

    fun scaledPoints(basePoints: Int, level: Int): Int = basePoints + bonusPoints(level)

    fun scaledGold(baseGold: Long, level: Int): Long =
        baseGold * (100L + bonusGoldPercent(level)) / 100L

    /** Slayer level at which the next bonus point is awarded. */
    fun nextBonusPointLevel(level: Int): Int =
        (bonusPoints(level) + 1) * LEVELS_PER_BONUS_POINT + 1
}
