package com.aistudio.trueidle.core

/**
 * Per-node rank limits for the Dojo. Single source for the model defaults, the content catalog,
 * the sanitizer and the Training/Stats screens.
 *
 * - crit_bonus : +0.5% crit chance per rank  -> 200 ranks reach 100%.
 * - speed_bonus: +5% attack speed per rank   -> 20 ranks reach +100%.
 * - luck_bonus : +5% drop chance per rank    -> 13 ranks reach 100% total drop chance
 *                (base 30% + 5% base luck + 5% per rank).
 * - xp_bonus   : +2% XP per rank -> 50,000 ranks reach the +100,000% limit.
 */
object TrainingCaps {
    const val DEFAULT_MAX_RANK = 1_000_000
    const val CRIT_MAX_RANK = 200
    const val SPEED_MAX_RANK = 20
    const val LUCK_MAX_RANK = 13
    const val XP_MAX_RANK = 50_000

    /** XP bonus from the node is capped at +100,000% (1000.0 as a fraction): 50,000 ranks of 2%. */
    const val XP_BONUS_CAP = 1000.0

    /** Attack speed from the node is capped at +100% (x2.0). */
    const val SPEED_BONUS_CAP = 1.0

    fun maxRank(id: String): Int = when (id) {
        "crit_bonus" -> CRIT_MAX_RANK
        "speed_bonus" -> SPEED_MAX_RANK
        "luck_bonus" -> LUCK_MAX_RANK
        "xp_bonus" -> XP_MAX_RANK
        else -> DEFAULT_MAX_RANK
    }

    /** True when the node's effect is bounded to a percentage ceiling of 100%. */
    fun isPercentCapped(id: String): Boolean =
        id == "crit_bonus" || id == "speed_bonus" || id == "luck_bonus"
}
