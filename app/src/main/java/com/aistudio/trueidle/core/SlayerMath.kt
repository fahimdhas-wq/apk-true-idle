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

    /** Maximum concurrent active directives displayed on the board. */
    const val MAX_TASKS = 10

    /**
     * Maximum tier rank unlocked for a given Slayer level.
     * Tier 0 is "F-", rank 8 is "D+", up to rank 69 ("Infinite EX").
     * Slayer level 1 starts with access to low tiers, scaling smoothly as level rises.
     */
    fun maxTierRankForLevel(slayerLvl: Int): Int {
        val lvl = slayerLvl.coerceAtLeast(1)
        return (lvl - 1).coerceIn(0, TierConfig.ids.size - 1)
    }

    /**
     * Scaled rewards container for higher-ranked missions (D+ and above).
     */
    data class BonusRewards(
        val diamonds: Long = 0L,
        val faith: Long = 0L,
        val bones: Long = 0L,
        val extraGold: Long = 0L
    )

    /**
     * Calculate additional rewards (Diamonds, Faith, Bone Essences, or extra Gold) for missions
     * ranked D+ (rank 8) or higher. Quantities scale progressively with tier rank.
     */
    fun rollBonusRewards(
        tierRank: Int,
        baseGold: Long,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): BonusRewards {
        // D+ is rank 8 in TierConfig
        if (tierRank < 8) return BonusRewards()

        val diff = (tierRank - 7).toLong()
        var diamonds = 0L
        var faith = 0L
        var bones = 0L
        var extraGold = 0L

        // Random bonus resource distribution as requested by user
        when (random.nextInt(4)) {
            0 -> {
                diamonds = (diff * 2L).coerceAtLeast(1L)
                if (tierRank >= 16) extraGold = (baseGold * 0.4).toLong()
            }
            1 -> {
                faith = (diff * 25L).coerceAtLeast(10L)
                if (tierRank >= 16) diamonds = (diff * 1L).coerceAtLeast(1L)
            }
            2 -> {
                bones = (diff * 20L).coerceAtLeast(10L)
                if (tierRank >= 16) diamonds = (diff * 1L).coerceAtLeast(1L)
            }
            else -> {
                extraGold = (baseGold * 0.5 * (1.0 + diff * 0.05)).toLong()
                diamonds = (diff * 1L).coerceAtLeast(1L)
            }
        }

        // Higher tiers scale quantities
        if (tierRank >= 25) {
            diamonds = diamonds.coerceAtLeast(diff * 3L)
            faith = faith.coerceAtLeast(diff * 30L)
            bones = bones.coerceAtLeast(diff * 25L)
        }

        return BonusRewards(
            diamonds = diamonds,
            faith = faith,
            bones = bones,
            extraGold = extraGold
        )
    }
}
