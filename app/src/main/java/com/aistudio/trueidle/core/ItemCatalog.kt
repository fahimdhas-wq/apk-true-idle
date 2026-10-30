package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.InventoryItem

/**
 * Central item catalog. Every resource has exactly one identity, one sell value and one
 * description, no matter which screen or code path produced it.
 */
object ItemCatalog {
    // 20 Canonical Mining Ores in exact order
    const val STONE = "Stone"
    const val COAL = "Coal"
    const val COPPER_ORE = "Copper Ore"
    const val IRON_ORE = "Iron Ore"
    const val TIN_ORE = "Tin Ore"
    const val SILVER_ORE = "Silver Ore"
    const val GOLD_ORE = "Gold Ore"
    const val QUARTZ = "Quartz"
    const val DIAMOND = "Diamond"
    const val EMERALD = "Emerald"
    const val RUBY = "Ruby"
    const val SAPPHIRE = "Sapphire"
    const val LAPIS_LAZULI = "Lapis Lazuli"
    const val REDSTONE = "Redstone"
    const val OBSIDIAN = "Obsidian"
    const val PLATINUM_ORE = "Platinum Ore"
    const val TITANIUM_ORE = "Titanium Ore"
    const val COBALT_ORE = "Cobalt Ore"
    const val URANIUM_ORE = "Uranium Ore"
    const val MITHRIL_ORE = "Mithril Ore"

    // 20 Canonical Farming Herbs in exact order
    const val HEALING_HERB = "Healing Herb"
    const val LESSER_HEALING_HERB = "Lesser Healing Herb"
    const val MANA_HERB = "Mana Herb"
    const val ANTIDOTE_HERB = "Antidote Herb"
    const val VITALITY_HERB = "Vitality Herb"
    const val SPIRIT_GRASS = "Spirit Grass"
    const val MOONSHADE_HERB = "Moonshade Herb"
    const val SUNROOT_HERB = "Sunroot Herb"
    const val SILVERLEAF_HERB = "Silverleaf Herb"
    const val WINDWHISPER_GRASS = "Windwhisper Grass"
    const val BLOODROOT = "Bloodroot"
    const val FROSTBLOOM = "Frostbloom"
    const val EMBERLEAF = "Emberleaf"
    const val DREAMERS_SAGE = "Dreamer's Sage"
    const val GOLDEN_GINSENG = "Golden Ginseng"
    const val MOONLIGHT_BLOSSOM = "Moonlight Blossom"
    const val SACRED_LOTUS = "Sacred Lotus"
    const val MANDRAKE_ROOT = "Mandrake Root"
    const val SPIRITBLOOM = "Spiritbloom"
    const val WORLDTREE_LEAF = "Worldtree Leaf"

    // Legacy aliases for backward compatibility with existing tests and saves
    const val FELWEED = "Healing Herb"
    const val MOONFLOWER = "Moonlight Blossom"
    const val STARBLOOM = "Spiritbloom"
    const val VOIDCAP = "Worldtree Leaf"
    const val OBSIDIAN_ORE = "Obsidian"
    const val BOSS_TROPHY = "Boss Trophy"

    const val CATEGORY_RESOURCE = "Resource"
    const val CATEGORY_POTION = "Potion"
    const val CATEGORY_TROPHY = "Trophy"
    const val CATEGORY_EQUIPMENT = "Equipment"

    data class ResourceDef(
        val name: String,
        val sellValue: Long,
        val description: String,
        val category: String = CATEGORY_RESOURCE
    )

    val ORES = listOf(
        STONE, COAL, COPPER_ORE, IRON_ORE, TIN_ORE,
        SILVER_ORE, GOLD_ORE, QUARTZ, DIAMOND, EMERALD,
        RUBY, SAPPHIRE, LAPIS_LAZULI, REDSTONE, OBSIDIAN,
        PLATINUM_ORE, TITANIUM_ORE, COBALT_ORE, URANIUM_ORE, MITHRIL_ORE
    )

    val HERBS = listOf(
        HEALING_HERB, LESSER_HEALING_HERB, MANA_HERB, ANTIDOTE_HERB, VITALITY_HERB,
        SPIRIT_GRASS, MOONSHADE_HERB, SUNROOT_HERB, SILVERLEAF_HERB, WINDWHISPER_GRASS,
        BLOODROOT, FROSTBLOOM, EMBERLEAF, DREAMERS_SAGE, GOLDEN_GINSENG,
        MOONLIGHT_BLOSSOM, SACRED_LOTUS, MANDRAKE_ROOT, SPIRITBLOOM, WORLDTREE_LEAF
    )

    private val catalog: Map<String, ResourceDef> = buildMap {
        // 20 Ores
        put(STONE, ResourceDef(STONE, 1L, "Basic quarry stone"))
        put(COAL, ResourceDef(COAL, 2L, "Combustible mineral carbon"))
        put(COPPER_ORE, ResourceDef(COPPER_ORE, 3L, "Conductive red metal ore"))
        put(IRON_ORE, ResourceDef(IRON_ORE, 5L, "Heavy industrial iron ore"))
        put(TIN_ORE, ResourceDef(TIN_ORE, 7L, "Malleable alloying tin ore"))
        put(SILVER_ORE, ResourceDef(SILVER_ORE, 10L, "Precious conductive silver ore"))
        put(GOLD_ORE, ResourceDef(GOLD_ORE, 14L, "High-density noble gold ore"))
        put(QUARTZ, ResourceDef(QUARTZ, 18L, "Resonant crystalline quartz"))
        put(DIAMOND, ResourceDef(DIAMOND, 25L, "Super-hard carbon crystal"))
        put(EMERALD, ResourceDef(EMERALD, 32L, "Luminous beryl gemstone"))
        put(RUBY, ResourceDef(RUBY, 40L, "High-thermal corundum gemstone"))
        put(SAPPHIRE, ResourceDef(SAPPHIRE, 50L, "Cryo-resonant blue sapphire"))
        put(LAPIS_LAZULI, ResourceDef(LAPIS_LAZULI, 62L, "Arcane pigment mineral"))
        put(REDSTONE, ResourceDef(REDSTONE, 76L, "Energy-conductive red crystal"))
        put(OBSIDIAN, ResourceDef(OBSIDIAN, 92L, "Volcanic silicate blast-glass"))
        put(PLATINUM_ORE, ResourceDef(PLATINUM_ORE, 115L, "Ultra-dense platinum ore"))
        put(TITANIUM_ORE, ResourceDef(TITANIUM_ORE, 140L, "Aerospace structural titanium ore"))
        put(COBALT_ORE, ResourceDef(COBALT_ORE, 175L, "Magnetic cobalt superalloy ore"))
        put(URANIUM_ORE, ResourceDef(URANIUM_ORE, 220L, "Nuclear fissile isotope ore"))
        put(MITHRIL_ORE, ResourceDef(MITHRIL_ORE, 300L, "Legendary true-silver mithril ore"))

        // 20 Herbs
        put(HEALING_HERB, ResourceDef(HEALING_HERB, 2L, "Cellular regenerative herb"))
        put(LESSER_HEALING_HERB, ResourceDef(LESSER_HEALING_HERB, 3L, "Mild recovery botanical"))
        put(MANA_HERB, ResourceDef(MANA_HERB, 4L, "Neural energizing mana herb"))
        put(ANTIDOTE_HERB, ResourceDef(ANTIDOTE_HERB, 6L, "Toxin-cleansing neutralizing herb"))
        put(VITALITY_HERB, ResourceDef(VITALITY_HERB, 8L, "Constitution-bolstering vitality herb"))
        put(SPIRIT_GRASS, ResourceDef(SPIRIT_GRASS, 11L, "Ethereal spirit grass"))
        put(MOONSHADE_HERB, ResourceDef(MOONSHADE_HERB, 15L, "Night-blooming shadow herb"))
        put(SUNROOT_HERB, ResourceDef(SUNROOT_HERB, 20L, "Solar-charged energizing root"))
        put(SILVERLEAF_HERB, ResourceDef(SILVERLEAF_HERB, 26L, "Reflective silverleaf botanical"))
        put(WINDWHISPER_GRASS, ResourceDef(WINDWHISPER_GRASS, 33L, "Aero-resonant whispering grass"))
        put(BLOODROOT, ResourceDef(BLOODROOT, 42L, "Coagulating crimson bloodroot"))
        put(FROSTBLOOM, ResourceDef(FROSTBLOOM, 52L, "Sub-zero chilled frostbloom"))
        put(EMBERLEAF, ResourceDef(EMBERLEAF, 65L, "Thermal-retaining emberleaf"))
        put(DREAMERS_SAGE, ResourceDef(DREAMERS_SAGE, 80L, "Psychoactive visionary sage"))
        put(GOLDEN_GINSENG, ResourceDef(GOLDEN_GINSENG, 100L, "Miraculous tonic ginseng"))
        put(MOONLIGHT_BLOSSOM, ResourceDef(MOONLIGHT_BLOSSOM, 125L, "Luminescent lunar blossom"))
        put(SACRED_LOTUS, ResourceDef(SACRED_LOTUS, 155L, "Purifying sacred lotus"))
        put(MANDRAKE_ROOT, ResourceDef(MANDRAKE_ROOT, 190L, "Potent resonance mandrake"))
        put(SPIRITBLOOM, ResourceDef(SPIRITBLOOM, 235L, "Transcendent spiritbloom petal"))
        put(WORLDTREE_LEAF, ResourceDef(WORLDTREE_LEAF, 320L, "Primordial worldtree foliage"))

        // Legacy and trophy fallbacks
        put("Obsidian Ore", ResourceDef("Obsidian Ore", 92L, "Volcanic silicate blast-glass"))
        put("Felweed", ResourceDef("Felweed", 2L, "Cellular regenerative herb"))
        put("Moonflower", ResourceDef("Moonflower", 125L, "Luminescent lunar blossom"))
        put("Starbloom", ResourceDef("Starbloom", 235L, "Transcendent spiritbloom petal"))
        put("Voidcap", ResourceDef("Voidcap", 320L, "Primordial worldtree foliage"))
        put(BOSS_TROPHY, ResourceDef(BOSS_TROPHY, 120L, "Proof of a boss kill", CATEGORY_TROPHY))
    }

    fun resource(name: String): ResourceDef =
        catalog[name] ?: ResourceDef(name, 5L, "Salvaged resource")

    fun isOre(name: String): Boolean = ORES.any { it.equals(name, ignoreCase = true) } || name.equals("Obsidian Ore", ignoreCase = true)
    fun isHerb(name: String): Boolean = HERBS.any { it.equals(name, ignoreCase = true) } ||
        listOf("Felweed", "Moonflower", "Starbloom", "Voidcap").any { it.equals(name, ignoreCase = true) }

    fun newResource(id: String, name: String, count: Double): InventoryItem {
        val def = resource(name)
        return InventoryItem(
            id = id,
            name = name,
            category = def.category,
            count = count,
            tier = 1,
            statDesc = def.description,
            sellValue = def.sellValue
        )
    }

    fun newResource(id: String, name: String, count: Int): InventoryItem =
        newResource(id, name, count.toDouble())

    /** Potion recipes with exact percentage and duration effects matching canonical 20 herbs. */
    data class PotionDef(
        val name: String,
        val herb: String,
        val herbCost: Int,
        val effectDesc: String,
        val directMissingHpPct: Double = 0.0,
        val directMissingManaPct: Double = 0.0,
        val directMaxHpPct: Double = 0.0,
        val cleanseNegatives: Boolean = false,
        val regenHpPctPerSec: Double = 0.0,
        val regenManaPctPerSec: Double = 0.0,
        val regenDurationSec: Double = 0.0,
        val shieldPctMaxHp: Double = 0.0,
        val shieldDurationSec: Double = 0.0,
        val damageReductionPct: Double = 0.0,
        val damageReductionDurationSec: Double = 0.0,
        val lifestealPct: Double = 0.0,
        val lifestealDurationSec: Double = 0.0,
        val healBoostPct: Double = 0.0,
        val healBoostDurationSec: Double = 0.0,
        val grantsPermanentMaxHpWhileCarried: Boolean = false
    ) {
        /** True when the brew restores health in any form (direct, max-HP or regeneration). */
        val restoresHp: Boolean
            get() = directMissingHpPct > 0.0 || directMaxHpPct > 0.0 || regenHpPctPerSec > 0.0

        /** True when the brew restores mana in any form. */
        val restoresMana: Boolean
            get() = directMissingManaPct > 0.0 || regenManaPctPerSec > 0.0
    }

    val POTIONS = listOf(
        PotionDef(
            name = "Minor Health Flask",
            herb = HEALING_HERB,
            herbCost = 4,
            effectDesc = "Restores 5% of current missing HP.",
            directMissingHpPct = 0.05
        ),
        PotionDef(
            name = "Lesser Health Draught",
            herb = LESSER_HEALING_HERB,
            herbCost = 4,
            effectDesc = "Restores 10% of current missing HP.",
            directMissingHpPct = 0.10
        ),
        PotionDef(
            name = "Mana Infusion Tonic",
            herb = MANA_HERB,
            herbCost = 4,
            effectDesc = "Restores 25% of current missing mana.",
            directMissingManaPct = 0.25
        ),
        PotionDef(
            name = "Cleansing Antidote",
            herb = ANTIDOTE_HERB,
            herbCost = 5,
            effectDesc = "Cleanses negative status and restores 5% Max HP.",
            cleanseNegatives = true,
            directMaxHpPct = 0.05
        ),
        PotionDef(
            name = "Vitality Elixir",
            herb = VITALITY_HERB,
            herbCost = 5,
            effectDesc = "Restores 15% missing HP, +2.5% max HP/s regen for 15s.",
            directMissingHpPct = 0.15,
            regenHpPctPerSec = 0.025,
            regenDurationSec = 15.0
        ),
        PotionDef(
            name = "Spirit Grass Philter",
            herb = SPIRIT_GRASS,
            herbCost = 5,
            effectDesc = "Restores 20% missing HP, +4% max mana/s regen for 12s.",
            directMissingHpPct = 0.20,
            regenManaPctPerSec = 0.04,
            regenDurationSec = 12.0
        ),
        PotionDef(
            name = "Moonshade Concoction",
            herb = MOONSHADE_HERB,
            herbCost = 6,
            effectDesc = "Restores 25% missing HP, +12% max HP shield for 15s.",
            directMissingHpPct = 0.25,
            shieldPctMaxHp = 0.12,
            shieldDurationSec = 15.0
        ),
        PotionDef(
            name = "Sunroot Extract",
            herb = SUNROOT_HERB,
            herbCost = 6,
            effectDesc = "Restores 30% missing HP, +1.5% max mana/s regen for 12s.",
            directMissingHpPct = 0.30,
            regenManaPctPerSec = 0.015,
            regenDurationSec = 12.0
        ),
        PotionDef(
            name = "Silverleaf Cordial",
            herb = SILVERLEAF_HERB,
            herbCost = 6,
            effectDesc = "Restores 35% missing HP, reduces incoming damage by 6% for 14s.",
            directMissingHpPct = 0.35,
            damageReductionPct = 0.06,
            damageReductionDurationSec = 14.0
        ),
        PotionDef(
            name = "Windwhisper Brew",
            herb = WINDWHISPER_GRASS,
            herbCost = 7,
            effectDesc = "Restores 40% missing HP, +5% max mana/s regen for 12s.",
            directMissingHpPct = 0.40,
            regenManaPctPerSec = 0.05,
            regenDurationSec = 12.0
        ),
        PotionDef(
            name = "Bloodroot Restorative",
            herb = BLOODROOT,
            herbCost = 7,
            effectDesc = "Restores 45% missing HP, 8% damage dealt back as HP for 10s.",
            directMissingHpPct = 0.45,
            lifestealPct = 0.08,
            lifestealDurationSec = 10.0
        ),
        PotionDef(
            name = "Frostbloom Draught",
            herb = FROSTBLOOM,
            herbCost = 7,
            effectDesc = "Restores 50% missing HP, reduces incoming damage by 8% for 14s.",
            directMissingHpPct = 0.50,
            damageReductionPct = 0.08,
            damageReductionDurationSec = 14.0
        ),
        PotionDef(
            name = "Emberleaf Tincture",
            herb = EMBERLEAF,
            herbCost = 8,
            effectDesc = "Restores 55% missing HP, increases healing received by 10% for 12s.",
            directMissingHpPct = 0.55,
            healBoostPct = 0.10,
            healBoostDurationSec = 12.0
        ),
        PotionDef(
            name = "Dreamer's Balm",
            herb = DREAMERS_SAGE,
            herbCost = 8,
            effectDesc = "Restores 20% missing HP and 20% missing mana, +3% max mana/s for 10s.",
            directMissingHpPct = 0.20,
            directMissingManaPct = 0.20,
            regenManaPctPerSec = 0.03,
            regenDurationSec = 10.0
        ),
        PotionDef(
            name = "Golden Ginseng Panacea",
            herb = GOLDEN_GINSENG,
            herbCost = 8,
            effectDesc = "Restores 70% missing HP, +6% max HP/s regen for 18s.",
            directMissingHpPct = 0.70,
            regenHpPctPerSec = 0.06,
            regenDurationSec = 18.0
        ),
        PotionDef(
            name = "Moonlight Blossom Philter",
            herb = MOONLIGHT_BLOSSOM,
            herbCost = 9,
            effectDesc = "Restores 75% missing HP, +7% max mana/s regen for 16s.",
            directMissingHpPct = 0.75,
            regenManaPctPerSec = 0.07,
            regenDurationSec = 16.0
        ),
        PotionDef(
            name = "Sacred Lotus Ambrosia",
            herb = SACRED_LOTUS,
            herbCost = 9,
            effectDesc = "Restores 85% missing HP, +18% max HP shield for 20s.",
            directMissingHpPct = 0.85,
            shieldPctMaxHp = 0.18,
            shieldDurationSec = 20.0
        ),
        PotionDef(
            name = "Mandrake Elixir",
            herb = MANDRAKE_ROOT,
            herbCost = 9,
            effectDesc = "Restores 90% missing HP, +8% max mana/s for 18s, reduces damage by 10% for 14s.",
            directMissingHpPct = 0.90,
            regenManaPctPerSec = 0.08,
            damageReductionPct = 0.10,
            damageReductionDurationSec = 14.0,
            regenDurationSec = 18.0
        ),
        PotionDef(
            name = "Spiritbloom Nectar",
            herb = SPIRITBLOOM,
            herbCost = 10,
            effectDesc = "Restores 95% missing HP and 25% missing mana, +9% max HP/s for 20s.",
            directMissingHpPct = 0.95,
            directMissingManaPct = 0.25,
            regenHpPctPerSec = 0.09,
            regenDurationSec = 20.0
        ),
        PotionDef(
            name = "Worldtree Dew",
            herb = WORLDTREE_LEAF,
            herbCost = 10,
            effectDesc = "Restores 100% missing HP and 40% missing mana, +10% max mana/s for 20s. Grants +1% Max HP permanently while carried in inventory.",
            directMissingHpPct = 1.00,
            directMissingManaPct = 0.40,
            regenManaPctPerSec = 0.10,
            regenDurationSec = 20.0,
            grantsPermanentMaxHpWhileCarried = true
        )
    )

    /** Health brews offered by the combat quick-deploy selector, weakest first. */
    val HP_POTIONS: List<PotionDef> = POTIONS.filter { it.restoresHp }

    /** Mana brews offered by the combat quick-deploy selector, weakest first. */
    val MANA_POTIONS: List<PotionDef> = POTIONS.filter { it.restoresMana }

    fun potion(name: String): PotionDef? = POTIONS.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun potionHeal(name: String): Double = potion(name)?.directMissingHpPct ?: 0.25
}
