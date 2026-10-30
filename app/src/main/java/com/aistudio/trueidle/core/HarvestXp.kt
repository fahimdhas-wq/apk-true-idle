package com.aistudio.trueidle.core

import java.util.Locale

/**
 * XP awarded for the actual quantity of a harvested resource.
 *
 * Resource tier determines XP per item; the actual yielded quantity (including proficiency and
 * yield upgrades) is multiplied by that per-item value. Double is intentional here: a level-100
 * proficiency yield can exceed Long.MAX_VALUE, and the game's alphabetic number formatter supports
 * values such as 1F and far beyond.
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

    /** Tier 1 Stone / first herb gives 12 XP per item; each higher tier gives 12 more XP. */
    fun xpPerItem(resourceName: String): Long {
        val rawKey = resourceName.lowercase(Locale.ROOT)
        val key = when (rawKey) {
            "felweed" -> ItemCatalog.HEALING_HERB.lowercase(Locale.ROOT)
            "moonflower" -> ItemCatalog.MOONLIGHT_BLOSSOM.lowercase(Locale.ROOT)
            "starbloom" -> ItemCatalog.SPIRITBLOOM.lowercase(Locale.ROOT)
            "voidcap" -> ItemCatalog.WORLDTREE_LEAF.lowercase(Locale.ROOT)
            "obsidian ore" -> ItemCatalog.OBSIDIAN.lowercase(Locale.ROOT)
            else -> rawKey
        }
        val tier = oreTiers[key] ?: herbTiers[key] ?: 1
        return XP_PER_TIER_UNIT * tier.toLong()
    }

    /**
     * Global skill XP is based on the complete item quantity actually awarded.
     *
     * Examples: 3 Stone = 36 XP, 4 Stone = 48 XP, 3 Coal = 72 XP, and 1F Stone = 12F XP.
     * Invalid or non-positive quantities award zero; multiplication saturates at Double.MAX_VALUE.
     */
    fun forYield(resourceName: String, actualItemYield: Double): Double {
        if (actualItemYield.isNaN() || actualItemYield <= 0.0) return 0.0
        if (actualItemYield.isInfinite()) return Double.MAX_VALUE
        val xp = actualItemYield * xpPerItem(resourceName).toDouble()
        return if (xp.isFinite()) xp else Double.MAX_VALUE
    }
}
