package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.InventoryItem

/**
 * Central item catalog. Every resource has exactly one identity, one sell value and one
 * description, no matter which screen or code path produced it.
 */
object ItemCatalog {
    const val COPPER_ORE = "Copper Ore"
    const val TIN_ORE = "Tin Ore"
    const val IRON_ORE = "Iron Ore"
    const val FELWEED = "Felweed"
    const val BLOODROOT = "Bloodroot"
    const val MOONFLOWER = "Moonflower"
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

    val ORES = listOf(COPPER_ORE, TIN_ORE, IRON_ORE)
    val HERBS = listOf(FELWEED, BLOODROOT, MOONFLOWER)

    private val catalog: Map<String, ResourceDef> = buildMap {
        put(COPPER_ORE, ResourceDef(COPPER_ORE, 2L, "Crafting metal ore"))
        put(TIN_ORE, ResourceDef(TIN_ORE, 4L, "Crafting metal ore"))
        put(IRON_ORE, ResourceDef(IRON_ORE, 7L, "Crafting metal ore"))
        put(FELWEED, ResourceDef(FELWEED, 3L, "Alchemy herb"))
        put(BLOODROOT, ResourceDef(BLOODROOT, 5L, "Alchemy herb"))
        put(MOONFLOWER, ResourceDef(MOONFLOWER, 8L, "Alchemy herb"))
        put(BOSS_TROPHY, ResourceDef(BOSS_TROPHY, 120L, "Proof of a boss kill", CATEGORY_TROPHY))
    }

    fun resource(name: String): ResourceDef =
        catalog[name] ?: ResourceDef(name, 5L, "Salvaged resource")

    fun isOre(name: String): Boolean = ORES.any { it.equals(name, ignoreCase = true) }
    fun isHerb(name: String): Boolean = HERBS.any { it.equals(name, ignoreCase = true) }

    fun newResource(id: String, name: String, count: Int): InventoryItem {
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

    /** Potion recipes: name -> (required herb, herb count, heal amount). */
    data class PotionDef(val name: String, val herb: String, val herbCost: Int, val heal: Double)

    /**
     * The engine charges these costs and the Armory screen renders this same table, so a recipe can
     * never quote one price and take another.
     */
    val POTIONS = listOf(
        PotionDef("Minor Health Flask", FELWEED, 4, 50.0),
        PotionDef("Bloodroot Tonic", BLOODROOT, 6, 80.0),
        PotionDef("Moonlight Philter", MOONFLOWER, 8, 150.0)
    )

    fun potion(name: String): PotionDef? = POTIONS.firstOrNull { it.name == name }

    fun potionHeal(name: String): Double = potion(name)?.heal ?: 75.0
}
