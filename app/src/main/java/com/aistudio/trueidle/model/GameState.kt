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
    val activeSpells: List<String> = emptyList()
)

@Serializable
data class BeastTamingState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val beasts: List<Beast> = listOf(
        Beast(id = "beast_1", name = "Dusk Wolf", rank = 1, stars = 1, dps = 6.0, hp = 50.0, maxHp = 50.0, trait = "Pack Hunter")
    ),
    val trophies: Long = 0L
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
data class GatheringState(
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
    val autoCollectEnabled: Boolean = true
)

@Serializable
data class CraftingState(
    val smithingLvl: Int = 1,
    val smithingXp: Long = 0L,
    val herbloreLvl: Int = 1,
    val herbloreXp: Long = 0L
)

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
data class SanctifiedState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val faith: Long = 50L,
    val aegisActive: Boolean = true,
    val smiteLevel: Int = 1,
    val auraName: String = "Holy Light"
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
    val type: String = "info" // "combat", "loot", "system", "gain", "milestone"
)

@Serializable
data class GameNotification(
    val id: Long,
    val title: String,
    val message: String,
    val type: String = "info", // "gear", "craft", "level", "tame", "save", "milestone"
    val count: Int = 1,
    val timestamp: Long = 0L,
    val status: String = "SUCCESS", // "SUCCESS", "FAILED", "INFO", "ALERT"
    val detail: String = ""
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
    val finalFloor: Int = 1,
    val efficiency: Double = 0.65,
    val capApplied: Boolean = false,
    val items: List<LootItem> = emptyList(),
    val beastsTamed: Int = 0,
    val farmingGainsSummary: String = "",
    val miningGainsSummary: String = "",
    val dropsSummary: List<String> = emptyList()
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
    val defeats: Long = 0L
)

@Serializable
data class GameState(
    val meta: SaveMeta = SaveMeta(),
    val lifetime: LifetimeStats = LifetimeStats(),
    val player: PlayerState = PlayerState(),
    val monster: Monster = Monster(),
    val beasts: BeastTamingState = BeastTamingState(),
    val necro: NecromancyState = NecromancyState(),
    val gathering: GatheringState = GatheringState(),
    val crafting: CraftingState = CraftingState(),
    val training: TrainingState = TrainingState(),
    val sanctified: SanctifiedState = SanctifiedState(),
    val slayer: SlayerState = SlayerState(),
    val inventory: List<InventoryItem> = listOf(
        InventoryItem(id = "item_iron_sword", name = "Iron Greatsword", category = "Equipment", count = 1, tier = 2, slot = "Weapon", statDesc = "+18 Damage", sellValue = 45L, isEquipped = false, atk = 18.0),
        InventoryItem(id = "item_steel_armor", name = "Steel Plate", category = "Equipment", count = 1, tier = 2, slot = "Armor", statDesc = "+12 Defense", sellValue = 50L, isEquipped = false, def = 12.0),
        InventoryItem(id = "item_rusty_blade", name = "Rusty Blade", category = "Equipment", count = 1, tier = 1, slot = "Weapon", statDesc = "+8 Damage", sellValue = 10L, isEquipped = true, atk = 8.0),
        InventoryItem(id = "item_tattered_leather", name = "Tattered Leather", category = "Equipment", count = 1, tier = 1, slot = "Armor", statDesc = "+4 Defense", sellValue = 10L, isEquipped = true, def = 4.0),
        InventoryItem(id = "item_hp_pot", name = "Lesser Healing Potion", category = "Potion", count = 5, tier = 1, statDesc = "Restores 50 HP", sellValue = 10L),
        InventoryItem(id = "item_copper", name = "Copper Ore", category = "Resource", count = 12, tier = 1, statDesc = "Crafting Ore", sellValue = 2L),
        InventoryItem(id = "item_felweed", name = "Felweed", category = "Resource", count = 8, tier = 1, statDesc = "Alchemy Herb", sellValue = 3L),
        InventoryItem(id = "item_wolf_fang", name = "Wolf Fang", category = "Trophy", count = 3, tier = 1, statDesc = "Beast Trophy", sellValue = 15L)
    ),
    val logs: List<LogEntry> = listOf(
        LogEntry(id = 1L, text = "SYSTEM INITIALIZED: Welcome to True IDLE Sector 01.", type = "system"),
        LogEntry(id = 2L, text = "RADAR CONTACT: Cave Rat engaged.", type = "combat")
    ),
    val notifications: List<GameNotification> = emptyList(),
    val notificationHistory: List<GameNotification> = emptyList(),
    val notificationBannersEnabled: Boolean = true,
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
)
