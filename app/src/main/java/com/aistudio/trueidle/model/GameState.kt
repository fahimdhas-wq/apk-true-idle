package com.aistudio.trueidle.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayerStats(
    val str: Double = 10.0,
    val intVal: Double = 10.0,
    val vit: Double = 10.0,
    val luck: Double = 5.0,
    val atkSpeed: Double = 1.0
)

@Serializable
data class PlayerState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val gold: Long = 100L,
    val diamonds: Long = 0L,
    val hp: Double = 100.0,
    val maxHp: Double = 100.0,
    val shield: Double = 25.0,
    val maxShield: Double = 25.0,
    val mana: Double = 50.0,
    val maxMana: Double = 50.0,
    val currentFloor: Int = 1,
    val maxUnlockedFloor: Int = 1,
    val killsOnFloor: Int = 0,
    val killsRequired: Int = 10,
    val autoAdvance: Boolean = true,
    val floorTimeLeft: Double = 60.0,
    val maxFloorTime: Double = 60.0,
    val stats: PlayerStats = PlayerStats(),
    val weaponSlot: String = "Rusty Blade",
    val armorSlot: String = "Tattered Leather",
    val weaponDmg: Double = 8.0,
    val armorDef: Double = 4.0,
    val recoverLeft: Double = 0.0,
    val failuresInRow: Int = 0
)

@Serializable
data class Monster(
    val name: String = "Cave Rat",
    val lvl: Int = 1,
    val hp: Double = 50.0,
    val maxHp: Double = 50.0,
    val shield: Double = 0.0,
    val maxShield: Double = 0.0,
    val damage: Double = 5.0,
    val armor: Double = 2.0,
    val tag: String = "Normal",
    val isBoss: Boolean = false,
    val goldReward: Long = 5L,
    val xpReward: Long = 15L
)

@Serializable
data class Beast(
    val id: String,
    val name: String,
    val rank: Int = 1,
    val stars: Int = 1,
    val dps: Double = 4.0,
    val hp: Double = 40.0,
    val maxHp: Double = 40.0,
    val trait: String = "Fierce",
    val element: String = "Physical"
)

@Serializable
data class Minion(
    val id: String,
    val type: String, // "Skeleton", "Bone Mage", "Dread Knight", "Bone Lich"
    val count: Int = 1,
    val lvl: Int = 1,
    val dps: Double = 3.0,
    val soak: Double = 5.0
)

@Serializable
data class NecromancyState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val boneEssence: Long = 20L,
    val army: List<Minion> = listOf(
        Minion(id = "minion_1", type = "Skeleton", count = 2, lvl = 1, dps = 5.0, soak = 10.0)
    ),
    val activeSpells: List<String> = emptyList(),
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "necro_soak", name = "Hardened Bone Plating", description = "+20% Minion damage absorption per rank", rank = 0, baseCost = 30L, costCurrency = "BONE"),
        DisciplineUpgrade(id = "necro_harvest", name = "Soul Siphon", description = "+25% Bone essence salvage from fallen foes", rank = 0, baseCost = 50L, costCurrency = "BONE")
    )
)

@Serializable
data class BeastTamingState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val beasts: List<Beast> = listOf(
        Beast(id = "beast_1", name = "Dusk Wolf", rank = 1, stars = 1, dps = 6.0, hp = 50.0, maxHp = 50.0, trait = "Pack Hunter")
    ),
    val trophies: Long = 0L,
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "beast_ferocity", name = "Predatory Conditioning", description = "+15% Companion beast DPS per rank", rank = 0, baseCost = 150L),
        DisciplineUpgrade(id = "beast_tame_rate", name = "Pheromone Baits", description = "+5% Beast taming encounter chance per floor", rank = 0, baseCost = 200L)
    )
)

@Serializable
data class DisciplineUpgrade(
    val id: String,
    val name: String,
    val description: String,
    val rank: Int = 0,
    val maxRank: Int = 10,
    val baseCost: Long = 100L,
    val costMultiplier: Double = 1.5,
    val costCurrency: String = "GOLD" // "GOLD", "FAITH", "BONE", "SLAYER"
)

@Serializable
data class Plot(
    val id: Int,
    val herbName: String,
    val growthProgress: Float = 0f,
    val durationSeconds: Float = 10f,
    val isReady: Boolean = false,
    val isCorrupted: Boolean = false
)

@Serializable
data class MiningVein(
    val id: Int,
    val oreName: String,
    val reqLvl: Int,
    val hardness: Int,
    val miningProgress: Float = 0f,
    val durationSeconds: Float = 5f,
    val isReady: Boolean = false
)

@Serializable
data class HarvestState(
    val farmingLvl: Int = 1,
    val farmingXp: Long = 0L,
    val miningLvl: Int = 1,
    val miningXp: Long = 0L,
    val plots: List<Plot> = listOf(
        Plot(id = 0, herbName = "Felweed", growthProgress = 0.4f, durationSeconds = 8f),
        Plot(id = 1, herbName = "Bloodroot", growthProgress = 0.1f, durationSeconds = 12f),
        Plot(id = 2, herbName = "Moonflower", growthProgress = 0f, durationSeconds = 15f)
    ),
    val veins: List<MiningVein> = listOf(
        MiningVein(id = 0, oreName = "Copper Ore", reqLvl = 1, hardness = 1, durationSeconds = 5f),
        MiningVein(id = 1, oreName = "Tin Ore", reqLvl = 3, hardness = 2, durationSeconds = 7f),
        MiningVein(id = 2, oreName = "Iron Ore", reqLvl = 5, hardness = 3, durationSeconds = 10f)
    ),
    val autoCollectUnlocked: Boolean = false,
    val autoCollectEnabled: Boolean = true,
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "harvest_speed", name = "Accelerated Bio-Growth", description = "-8% Farm plot growth duration per rank", rank = 0, baseCost = 80L),
        DisciplineUpgrade(id = "mining_speed", name = "Sonic Laser Drills", description = "-8% Ore extraction time per rank", rank = 0, baseCost = 100L),
        DisciplineUpgrade(id = "harvest_yield", name = "Rich Bio-Extracts", description = "+1 additional resource yield per harvest", rank = 0, baseCost = 150L)
    )
)
typealias GatheringState = HarvestState

@Serializable
data class ArmoryState(
    val smithingLvl: Int = 1,
    val smithingXp: Long = 0L,
    val herbloreLvl: Int = 1,
    val herbloreXp: Long = 0L,
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "forge_quality", name = "Molecular Tempering", description = "+12% ATK & DEF rating on forged equipment", rank = 0, baseCost = 120L),
        DisciplineUpgrade(id = "alchemy_potency", name = "Nanite Catalyst", description = "+25% Potion healing & regeneration potency", rank = 0, baseCost = 100L),
        DisciplineUpgrade(id = "resource_saving", name = "Alloy Recycling", description = "12% chance to conserve crafting materials", rank = 0, baseCost = 180L)
    )
)
typealias CraftingState = ArmoryState

@Serializable
data class TrainingNode(
    val id: String,
    val name: String,
    val stat: String,
    val rank: Int = 0,
    val maxRank: Int = 20,
    val costGold: Long = 50L,
    val costPoints: Int = 1
)

@Serializable
data class TrainingState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val talentPoints: Int = 2,
    val talentPointsBought: Int = 0,
    val nodes: List<TrainingNode> = listOf(
        TrainingNode(id = "str_bonus", name = "Titan Strength", stat = "+5 STR per rank", rank = 0, maxRank = 30, costGold = 50L),
        TrainingNode(id = "vit_bonus", name = "Iron Constitution", stat = "+50 Max HP per rank", rank = 0, maxRank = 30, costGold = 50L),
        TrainingNode(id = "int_bonus", name = "Arcane Mind", stat = "+25 Max Mana per rank", rank = 0, maxRank = 30, costGold = 50L),
        TrainingNode(id = "speed_bonus", name = "Swift Strikes", stat = "+5% Attack Speed per rank", rank = 0, maxRank = 15, costGold = 100L),
        TrainingNode(id = "luck_bonus", name = "Fortune's Favor", stat = "+5% Drop Chance per rank", rank = 0, maxRank = 20, costGold = 75L)
    )
)

@Serializable
data class EssenceState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val faith: Long = 50L,
    val aegisActive: Boolean = true,
    val smiteLevel: Int = 1,
    val auraName: String = "Holy Light",
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "essence_shield", name = "Radiant Capacitor", description = "+20% Aegis barrier absorption efficiency", rank = 0, baseCost = 40L, costCurrency = "FAITH"),
        DisciplineUpgrade(id = "smite_overdrive", name = "Solar Lance", description = "+30% Smite burst damage per rank", rank = 0, baseCost = 50L, costCurrency = "FAITH"),
        DisciplineUpgrade(id = "faith_preserve", name = "Aura Efficiency", description = "-15% Faith drain rate during Aegis deployment", rank = 0, baseCost = 60L, costCurrency = "FAITH")
    )
)
typealias SanctifiedState = EssenceState

@Serializable
data class Talent(
    val id: String,
    val name: String,
    val description: String,
    val type: String, // "ACTIVE", "PASSIVE_STRUGGLE"
    val rank: Int = 0,
    val maxRank: Int = 10,
    val cooldownSeconds: Double = 30.0,
    val cooldownRemaining: Double = 0.0,
    val durationSeconds: Double = 10.0,
    val durationRemaining: Double = 0.0,
    val isActive: Boolean = false,
    val costPoints: Int = 1
)

@Serializable
data class TalentState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val talentPoints: Int = 5,
    val struggleDamageTaken: Long = 0L,
    val struggleHpThreshold: Long = 100L,
    val struggleArmorThreshold: Long = 500L,
    val struggleManaThreshold: Long = 150L,
    val struggleAtkThreshold: Long = 250L,
    val activeTalents: List<Talent> = listOf(
        Talent(id = "berserk", name = "Berserk Overdrive", description = "+50% ATK Speed & +35% Damage for 10s", type = "ACTIVE", rank = 1, cooldownSeconds = 30.0, durationSeconds = 10.0),
        Talent(id = "nanite_heal", name = "Nanite Repair", description = "Instantly restores 40% Max HP & 25% Shield", type = "ACTIVE", rank = 1, cooldownSeconds = 25.0, durationSeconds = 0.0),
        Talent(id = "support_overclock", name = "Legion Overclock", description = "Minions and Beasts deal 300% DPS for 12s", type = "ACTIVE", rank = 1, cooldownSeconds = 40.0, durationSeconds = 12.0),
        Talent(id = "mana_rampage", name = "Mana Tempest", description = "Consumes 25 Mana to strike enemy with 250% Arcane Damage", type = "ACTIVE", rank = 1, cooldownSeconds = 20.0, durationSeconds = 0.0),
        Talent(id = "aegis_bastion", name = "Aegis Bastion", description = "Deploys a defensive kinetic barrier (+60% Max Shield)", type = "ACTIVE", rank = 1, cooldownSeconds = 35.0, durationSeconds = 0.0),
        Talent(id = "arcane_enchant", name = "Enchantment Matrix", description = "+25% Critical Strike Chance & +50% Crit DMG for 12s", type = "ACTIVE", rank = 1, cooldownSeconds = 30.0, durationSeconds = 12.0)
    ),
    val struggleTalents: List<Talent> = listOf(
        Talent(id = "struggle_hp", name = "Pain Adaptation (HP)", description = "+5 Max HP per 100 DMG taken per rank", type = "PASSIVE_STRUGGLE", rank = 1, costPoints = 1),
        Talent(id = "struggle_def", name = "Kinetic Hardening (DEF)", description = "+1 DEF per 500 DMG taken per rank", type = "PASSIVE_STRUGGLE", rank = 1, costPoints = 1),
        Talent(id = "struggle_mana", name = "Neural Siphon (MANA)", description = "+3 Max Mana per 150 DMG taken per rank", type = "PASSIVE_STRUGGLE", rank = 1, costPoints = 1),
        Talent(id = "struggle_atk", name = "Vengeful Rebound (ATK)", description = "+2 Base ATK per 250 DMG taken per rank", type = "PASSIVE_STRUGGLE", rank = 1, costPoints = 1)
    )
)

@Serializable
data class SlayerTask(
    val targetName: String = "Cave Goblin",
    val currentKills: Int = 0,
    val requiredKills: Int = 15,
    val rewardGold: Long = 200L,
    val rewardPoints: Int = 5,
    val isCompleted: Boolean = false
)

@Serializable
data class SlayerState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val slayerPoints: Int = 10,
    val currentTask: SlayerTask = SlayerTask()
)

@Serializable
data class InventoryItem(
    val id: String,
    val name: String,
    val category: String, // "Equipment", "Potion", "Resource", "Trophy"
    val count: Int = 1,
    val tier: Int = 1,
    val slot: String = "", // "Weapon", "Armor", ""
    val statDesc: String = "",
    val sellValue: Long = 10L,
    val isEquipped: Boolean = false,
    val atk: Double = 0.0,
    val def: Double = 0.0
)

@Serializable
data class LogEntry(
    val id: Long,
    val text: String,
    val type: String = "info", // "combat", "loot", "system", "gain", "milestone"
    val timestampMs: Long = 0L
)

@Serializable
data class LootItem(
    val name: String,
    val count: Int
)

@Serializable
data class OfflineLoot(
    val timeAwaySeconds: Long = 0L,
    val kills: Long = 0L,
    val goldEarned: Long = 0L,
    val xpEarned: Long = 0L,
    val bonesEarned: Long = 0L,
    val playerLevelsGained: Int = 0,
    val floorsGained: Int = 0,
    val startFloor: Int = 1,
    val finalFloor: Int = 1,
    val efficiency: Double = 0.65,
    val capApplied: Boolean = false,
    val killsPerHour: Long = 0L,
    val goldPerHour: Long = 0L,
    val xpPerHour: Long = 0L,
    val items: List<LootItem> = emptyList(),
    val beastsTamed: Int = 0,
    val farmingGainsSummary: String = "",
    val miningGainsSummary: String = "",
    val dropsSummary: List<String> = emptyList(),
    val timelineLog: List<String> = emptyList()
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val progress: Int,
    val target: Int,
    val isUnlocked: Boolean = false,
    val isClaimed: Boolean = false,
    val rewardGold: Long = 100L
)

@Serializable
data class AutomationSettings(
    val autoEquipBestGear: Boolean = true,
    val autoMergeBeasts: Boolean = false,
    val craftMaterialUsagePct: Int = 75
)

@Serializable
data class PromotionState(
    val tier: Int = 5,
    val maxTier: Int = 20,
    val tierName: String = "Dawnbreaker",
    val requiredFloor: Int = 60,
    val requiredGearScore: Int = 2200,
    val bonusPercent: Int = 8
)

@Serializable
data class SaveMeta(
    val savedWallMs: Long = 0L,
    val savedElapsedMs: Long = 0L,
    val bootCount: Int = 0,
    val offlineAppliedUntilWallMs: Long = 0L,
    val nextId: Long = 100L,
    val rngSeed: Long = 0L
)

@Serializable
data class LifetimeStats(
    val totalKills: Long = 0L,
    val oresMined: Long = 0L,
    val herbsHarvested: Long = 0L,
    val beastsTamed: Long = 0L,
    val forged: Long = 0L,
    val brewed: Long = 0L,
    val defeats: Long = 0L,
    val playTimeSeconds: Long = 0L
)

@Serializable
data class GameState(
    val meta: SaveMeta = SaveMeta(),
    val lifetime: LifetimeStats = LifetimeStats(),
    val player: PlayerState = PlayerState(),
    val monster: Monster = Monster(),
    val beasts: BeastTamingState = BeastTamingState(),
    val necro: NecromancyState = NecromancyState(),
    val harvest: HarvestState = HarvestState(),
    val armory: ArmoryState = ArmoryState(),
    val training: TrainingState = TrainingState(),
    val essence: EssenceState = EssenceState(),
    val slayer: SlayerState = SlayerState(),
    val talent: TalentState = TalentState(),
    val automation: AutomationSettings = AutomationSettings(),
    val promotion: PromotionState = PromotionState(),
    val inventory: List<InventoryItem> = listOf(
        InventoryItem(id = "item_iron_sword", name = "Iron Greatsword", category = "Equipment", count = 1, tier = 2, slot = "Weapon", statDesc = "+18 Damage", sellValue = 45L, isEquipped = false, atk = 18.0),
        InventoryItem(id = "item_steel_armor", name = "Steel Plate", category = "Equipment", count = 1, tier = 2, slot = "Armor", statDesc = "+12 Defense", sellValue = 50L, isEquipped = false, def = 12.0),
        InventoryItem(id = "item_rusty_blade", name = "Rusty Blade", category = "Equipment", count = 1, tier = 1, slot = "Weapon", statDesc = "+8 Damage", sellValue = 10L, isEquipped = true, atk = 8.0),
        InventoryItem(id = "item_tattered_leather", name = "Tattered Leather", category = "Equipment", count = 1, tier = 1, slot = "Armor", statDesc = "+4 Defense", sellValue = 10L, isEquipped = true, def = 4.0),
        InventoryItem(id = "item_ember_band", name = "Ember Band", category = "Equipment", count = 1, tier = 2, slot = "Ring", statDesc = "Crit +2.1%", sellValue = 60L, isEquipped = true, atk = 6.0),
        InventoryItem(id = "item_warden_sigil", name = "Warden Sigil", category = "Equipment", count = 1, tier = 2, slot = "Amulet", statDesc = "Vit +18", sellValue = 60L, isEquipped = true, def = 6.0),
        InventoryItem(id = "item_hp_pot", name = "Lesser Healing Potion", category = "Potion", count = 5, tier = 1, statDesc = "Restores 50 HP", sellValue = 10L),
        InventoryItem(id = "item_copper", name = "Copper Ore", category = "Resource", count = 12, tier = 1, statDesc = "Crafting Ore", sellValue = 2L),
        InventoryItem(id = "item_felweed", name = "Felweed", category = "Resource", count = 8, tier = 1, statDesc = "Alchemy Herb", sellValue = 3L),
        InventoryItem(id = "item_wolf_fang", name = "Wolf Fang", category = "Trophy", count = 3, tier = 1, statDesc = "Beast Trophy", sellValue = 15L)
    ),
    val logs: List<LogEntry> = listOf(
        LogEntry(id = 1L, text = "SYSTEM INITIALIZED: Welcome to True IDLE Sector 01.", type = "system"),
        LogEntry(id = 2L, text = "RADAR CONTACT: Cave Rat engaged.", type = "combat")
    ),
    val pendingOfflineLoot: OfflineLoot? = null,
    val achievements: List<Achievement> = listOf(
        Achievement(id = "ach_first_blood", title = "First Blood", desc = "Defeat your first monster", progress = 0, target = 1, isUnlocked = false, rewardGold = 50L),
        Achievement(id = "ach_floor_10", title = "Dungeon Crawler", desc = "Reach Floor 10", progress = 1, target = 10, isUnlocked = false, rewardGold = 250L),
        Achievement(id = "ach_tamer", title = "Beast Whisperer", desc = "Tame 3 companion beasts", progress = 1, target = 3, isUnlocked = false, rewardGold = 200L),
        Achievement(id = "ach_necro", title = "Lord of Bones", desc = "Summon an army of 10 minions", progress = 2, target = 10, isUnlocked = false, rewardGold = 300L),
        Achievement(id = "ach_miner", title = "Master Miner", desc = "Mine 50 ores", progress = 0, target = 50, isUnlocked = false, rewardGold = 150L)
    ),
    val totalKills: Long = 0L,
    val rank: Int = 0,
    val lastSaveTimestamp: Long = 0L
) {
    val gathering: HarvestState get() = harvest
    val crafting: ArmoryState get() = armory
    val sanctified: EssenceState get() = essence
}
