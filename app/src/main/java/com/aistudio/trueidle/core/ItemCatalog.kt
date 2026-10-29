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

    /** Potion recipes: name -> (required herb, herb count, heal amount). */
    data class PotionDef(val name: String, val herb: String, val herbCost: Int, val heal: Double)

    /**
     * The engine charges these costs and the Forge screen renders this same table, so a recipe can
     * never quote one price and take another.
     */
    val POTIONS = listOf(
        PotionDef("Minor Health Flask", HEALING_HERB, 4, 50.0),
        PotionDef("Lesser Health Draught", LESSER_HEALING_HERB, 4, 100.0),
        PotionDef("Mana Infusion Tonic", MANA_HERB, 4, 150.0),
        PotionDef("Cleansing Antidote", ANTIDOTE_HERB, 5, 220.0),
        PotionDef("Vitality Elixir", VITALITY_HERB, 5, 300.0),
        PotionDef("Spirit Grass Philter", SPIRIT_GRASS, 5, 400.0),
        PotionDef("Moonshade Concoction", MOONSHADE_HERB, 6, 550.0),
        PotionDef("Sunroot Extract", SUNROOT_HERB, 6, 750.0),
        PotionDef("Silverleaf Cordial", SILVERLEAF_HERB, 6, 1000.0),
        PotionDef("Windwhisper Brew", WINDWHISPER_GRASS, 7, 1400.0),
        PotionDef("Bloodroot Restorative", BLOODROOT, 7, 1900.0),
        PotionDef("Frostbloom Draught", FROSTBLOOM, 7, 2600.0),
        PotionDef("Emberleaf Tincture", EMBERLEAF, 8, 3500.0),
        PotionDef("Dreamer's Balm", DREAMERS_SAGE, 8, 4800.0),
        PotionDef("Golden Ginseng Panacea", GOLDEN_GINSENG, 8, 6500.0),
        PotionDef("Moonlight Blossom Philter", MOONLIGHT_BLOSSOM, 9, 9000.0),
        PotionDef("Sacred Lotus Ambrosia", SACRED_LOTUS, 9, 12500.0),
        PotionDef("Mandrake Elixir", MANDRAKE_ROOT, 9, 17500.0),
        PotionDef("Spiritbloom Nectar", SPIRITBLOOM, 10, 25000.0),
        PotionDef("Worldtree Dew", WORLDTREE_LEAF, 10, 40000.0)
    )

    fun potion(name: String): PotionDef? = POTIONS.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun potionHeal(name: String): Double = potion(name)?.heal ?: 75.0
}
