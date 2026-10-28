package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.MiningVein
import com.aistudio.trueidle.model.Plot
import com.aistudio.trueidle.model.Talent
import com.aistudio.trueidle.model.TrainingNode

/**
 * Content added after the first release.
 *
 * Saves written before this content existed only contain the original plots, veins, nodes, talents
 * and upgrades, so [reconcile] unions these additions in by id. That keeps old saves loadable without
 * a schema bump and without ever duplicating an entry the player already owns.
 *
 * Only *additions* live here. The original entries stay in their model defaults, so no id is defined
 * twice.
 */
object ContentCatalog {

    // ---- Armory -------------------------------------------------------------

    data class ForgeRecipe(
        val name: String,
        val tier: Int,
        val slot: String,
        val reqOre: String,
        val reqQty: Int,
        val statPreview: String
    )

    /** Weapons show tier*12 ATK and armor tier*8 DEF, which is exactly what craftEquipment bakes in. */
    val FORGE_RECIPES = listOf(
        ForgeRecipe("Bronze Glaive", 1, Equipment.SLOT_WEAPON, ItemCatalog.COPPER_ORE, 6, "+12 ATK"),
        ForgeRecipe("Heavy Plate", 1, Equipment.SLOT_ARMOR, ItemCatalog.COPPER_ORE, 8, "+8 DEF"),
        ForgeRecipe("Titan Edge", 2, Equipment.SLOT_WEAPON, ItemCatalog.TIN_ORE, 10, "+24 ATK"),
        ForgeRecipe("Cobalt Cuirass", 2, Equipment.SLOT_ARMOR, ItemCatalog.TIN_ORE, 12, "+16 DEF"),
        ForgeRecipe("Plasma Cleaver", 3, Equipment.SLOT_WEAPON, ItemCatalog.IRON_ORE, 15, "+36 ATK"),
        ForgeRecipe("Aegis Carapace", 3, Equipment.SLOT_ARMOR, ItemCatalog.IRON_ORE, 18, "+24 DEF"),
        ForgeRecipe("Silver Warblade", 4, Equipment.SLOT_WEAPON, ItemCatalog.SILVER_ORE, 18, "+48 ATK"),
        ForgeRecipe("Silver Bulwark", 4, Equipment.SLOT_ARMOR, ItemCatalog.SILVER_ORE, 22, "+32 DEF"),
        ForgeRecipe("Obsidian Reaver", 5, Equipment.SLOT_WEAPON, ItemCatalog.OBSIDIAN_ORE, 26, "+60 ATK"),
        ForgeRecipe("Obsidian Bastion", 5, Equipment.SLOT_ARMOR, ItemCatalog.OBSIDIAN_ORE, 30, "+40 DEF"),
        ForgeRecipe("Voidsteel Edge", 6, Equipment.SLOT_WEAPON, ItemCatalog.OBSIDIAN_ORE, 34, "+72 ATK"),
        ForgeRecipe("Voidsteel Aegis", 6, Equipment.SLOT_ARMOR, ItemCatalog.OBSIDIAN_ORE, 38, "+48 DEF")
    )

    // ---- Harvest ------------------------------------------------------------

    val EXTRA_PLOTS = listOf(
        Plot(id = 3, herbName = ItemCatalog.STARBLOOM, growthProgress = 0f, durationSeconds = 20f),
        Plot(id = 4, herbName = ItemCatalog.VOIDCAP, growthProgress = 0f, durationSeconds = 26f)
    )

    val EXTRA_VEINS = listOf(
        MiningVein(id = 3, oreName = ItemCatalog.SILVER_ORE, reqLvl = 1, miningProgress = 0f, durationSeconds = 14f),
        MiningVein(id = 4, oreName = ItemCatalog.OBSIDIAN_ORE, reqLvl = 1, miningProgress = 0f, durationSeconds = 20f)
    )

    val EXTRA_HARVEST_UPGRADES = listOf(
        DisciplineUpgrade(
            id = "mining_yield",
            name = "Resonant Ore Sieve",
            description = "+1 additional ore per extraction",
            rank = 0,
            baseCost = 200L
        )
    )

    // ---- Essence ------------------------------------------------------------

    val EXTRA_ESSENCE_UPGRADES = listOf(
        DisciplineUpgrade(
            id = "smite_focus",
            name = "Prism Focus",
            description = "+5% Solar Smite damage per rank",
            rank = 0,
            baseCost = 80L,
            costCurrency = "FAITH"
        ),
        DisciplineUpgrade(
            id = "aegis_reserve",
            name = "Barrier Lattice",
            description = "+8% Max Shield per rank",
            rank = 0,
            baseCost = 70L,
            costCurrency = "FAITH"
        )
    )

    // ---- Genesis ------------------------------------------------------------

    val EXTRA_NECRO_UPGRADES = listOf(
        DisciplineUpgrade(
            id = "necro_dps",
            name = "Grave Momentum",
            description = "+10% Reanimation Corps DPS per rank",
            rank = 0,
            baseCost = 80L,
            costCurrency = "BONE"
        )
    )

    // ---- Training -----------------------------------------------------------

    val EXTRA_TRAINING_NODES = listOf(
        TrainingNode(id = "crit_bonus", name = "Precision Targeting", stat = "+0.5% Crit Chance per rank", rank = 0, maxRank = 20, costGold = 150L),
        TrainingNode(id = "shield_bonus", name = "Layered Plating", stat = "+5% Max Shield per rank", rank = 0, maxRank = 20, costGold = 140L),
        TrainingNode(id = "regen_bonus", name = "Cellular Repair", stat = "+4% HP & Shield regen per rank", rank = 0, maxRank = 15, costGold = 120L),
        TrainingNode(id = "xp_bonus", name = "Data Assimilation", stat = "+2% XP gain per rank", rank = 0, maxRank = 20, costGold = 130L)
    )

    // ---- Talents ------------------------------------------------------------

    val EXTRA_ACTIVE_TALENTS = listOf(
        Talent(
            id = "phoenix_protocol",
            name = "Phoenix Protocol",
            description = "Restores 65% Max HP & 40% Shield instantly (35 Mana)",
            type = "ACTIVE",
            rank = 1,
            cooldownSeconds = 60.0,
            durationSeconds = 0.0
        ),
        Talent(
            id = "void_pulse",
            name = "Void Pulse",
            description = "Detonates a 300% attack pulse on the target (35 Mana)",
            type = "ACTIVE",
            rank = 1,
            cooldownSeconds = 45.0,
            durationSeconds = 0.0
        )
    )

    val EXTRA_STRUGGLE_TALENTS = listOf(
        Talent(
            id = "struggle_shield",
            name = "Barrier Memory (SHIELD)",
            description = "+2 Max Shield per 200 DMG taken per rank",
            type = "PASSIVE_STRUGGLE",
            rank = 1,
            costPoints = 1
        )
    )

    /**
     * Unions every catalog entry into [state] by id and returns the state unchanged when it already
     * holds all of it, so calling this on every load is free for up-to-date saves and idempotent.
     */
    fun reconcile(state: GameState): GameState {
        val plots = merge(state.harvest.plots, EXTRA_PLOTS) { it.id }
        val veins = merge(state.harvest.veins, EXTRA_VEINS) { it.id }
        val harvestUpgrades = merge(state.harvest.upgrades, EXTRA_HARVEST_UPGRADES) { it.id }
        val essenceUpgrades = merge(state.essence.upgrades, EXTRA_ESSENCE_UPGRADES) { it.id }
        val necroUpgrades = merge(state.necro.upgrades, EXTRA_NECRO_UPGRADES) { it.id }
        val nodes = merge(state.training.nodes, EXTRA_TRAINING_NODES) { it.id }
        val talents = merge(state.talent.activeTalents, EXTRA_ACTIVE_TALENTS) { it.id }
        val struggle = merge(state.talent.struggleTalents, EXTRA_STRUGGLE_TALENTS) { it.id }

        val unchanged = plots === state.harvest.plots &&
            veins === state.harvest.veins &&
            harvestUpgrades === state.harvest.upgrades &&
            essenceUpgrades === state.essence.upgrades &&
            necroUpgrades === state.necro.upgrades &&
            nodes === state.training.nodes &&
            talents === state.talent.activeTalents &&
            struggle === state.talent.struggleTalents
        if (unchanged) return state

        return state.copy(
            harvest = state.harvest.copy(plots = plots, veins = veins, upgrades = harvestUpgrades),
            essence = state.essence.copy(upgrades = essenceUpgrades),
            necro = state.necro.copy(upgrades = necroUpgrades),
            training = state.training.copy(nodes = nodes),
            talent = state.talent.copy(activeTalents = talents, struggleTalents = struggle)
        )
    }

    private fun <T> merge(existing: List<T>, additions: List<T>, key: (T) -> Any): List<T> {
        if (additions.isEmpty()) return existing
        val known = HashSet<Any>(existing.size + additions.size)
        existing.forEach { known.add(key(it)) }
        val missing = additions.filter { key(it) !in known }
        return if (missing.isEmpty()) existing else existing + missing
    }
}
