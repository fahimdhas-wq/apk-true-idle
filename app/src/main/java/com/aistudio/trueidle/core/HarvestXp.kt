package com.aistudio.trueidle.core

import java.util.Locale

/**
 * Skill XP paid by gathered resource units.
 *
 * XP is calculated from the guaranteed extraction batch (2 herbs or 3 ores, plus explicit
 * yield-upgrade bonuses), before the per-item proficiency multiplier is applied. Proficiency
 * continues to multiply inventory yield and its own proficiency XP; it does not recursively
 * multiply the global skill XP reward. This keeps the 2x-per-proficiency-level economy from
 * making a gathering skill jump to its cap in a handful of actions.
 */
object HarvestXp {
    const val XP_PER_TIER_UNIT: Long = 12L

    private val oreTiers: Map<String, Int> =
        ItemCatalog.ORES.mapIndexed { index, name ->
            name.lowercase(Locale.ROOT) to index + 1
        }.toMap()

    private val herbTiers: Map<String, Int> =
        ItemCatalog.HERBS.mapIndexed { index, name ->
            name.lowercase(Locale.ROOT) to index + 1
        }.toMap()

    /** Tier 1 Stone / first herb gives 12 XP per guaranteed item; each higher tier gives more. */
    fun xpPerItem(resourceName: String): Long {
        val key = resourceName.lowercase(Locale.ROOT)
        val tier = oreTiers[key] ?: herbTiers[key] ?: 1
        return XP_PER_TIER_UNIT * tier.toLong()
    }

    /**
     * XP for the guaranteed base batch. E.g. 3 Stone = 36 XP; 4 Stone = 48 XP;
     * 3 Coal = 72 XP. Multiplication saturates instead of wrapping negative.
     */
    fun forBaseBatch(resourceName: String, itemCount: Long): Long {
        val count = itemCount.coerceAtLeast(0L)
        val perItem = xpPerItem(resourceName)
        return if (count > Long.MAX_VALUE / perItem) Long.MAX_VALUE else count * perItem
    }
}
