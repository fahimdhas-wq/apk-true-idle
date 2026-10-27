package com.aistudio.trueidle.core

import kotlin.math.floor
import kotlin.math.pow

/**
 * Upgrade price formulas. The engine, the four discipline screens and the Essence screen all quote
 * their numbers from here, so a purchase can never display one price and charge another.
 *
 * This lives in `core` rather than `engine.Balance` so the screens can use it without importing the
 * engine (see ArchitectureTest).
 */
object UpgradeCosts {
    /** Every discipline upgrade costs this much more than its previous rank. */
    const val COST_MULTIPLIER = 1.5

    /** Cost of buying the next rank: `baseCost * costMultiplier^rank`. */
    fun nextRank(baseCost: Long, rank: Int, costMultiplier: Double = COST_MULTIPLIER): Long =
        floor(baseCost.toDouble() * costMultiplier.pow(rank.coerceAtLeast(0))).toLong()

    /** Faith cost of raising Smite from [smiteLevel] to the next level. */
    fun smite(smiteLevel: Int): Long =
        floor(25.0 * COST_MULTIPLIER.pow(smiteLevel.coerceAtLeast(0))).toLong()
}
