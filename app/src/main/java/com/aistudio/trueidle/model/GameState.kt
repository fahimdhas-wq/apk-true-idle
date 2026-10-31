package com.aistudio.trueidle.model

import kotlinx.serialization.Serializable

/**
 * Save model.
 *
 * Every property has a default so saves written by an older build (before a field existed) still
 * deserialize. Serialization also runs with `coerceInputValues`, which maps a mismatched value onto
 * the declared default instead of throwing.
 */

@Serializable
data class PlayerState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val gold: Long = 100L,
    val diamonds: Long = 0L,
    val hp: Double = 100.0,
    val shield: Double = 25.0,
    val mana: Double = 50.0,
    val currentFloor: Int = 1,
    val maxUnlockedFloor: Int = 1,
    val killsOnFloor: Int = 0,
    val killsRequired: Int = 10,
    val autoAdvance: Boolean = true,
    val recoverLeft: Double = 0.0,
    val failuresInRow: Int = 0
)
// NOTE: maxHp/maxShield/maxMana/weaponDmg/armorDef/weaponSlot/armorSlot/stats/floorTimeLeft/
// maxFloorTime used to live here as stale duplicates. They are all derived on demand from the
// inventory and discipline states by `DerivedStats`, so nothing can drift out of sync.

@Serializable
data class Monster(
    val name: String = "Cave Rat",
    val lvl: Int = 1,
    // Matches Balance.generateMonsterForFloor(1): 55 HP, 7.0 damage, 1.5 armor, 15 XP.
    val hp: Double = 55.0,
    val maxHp: Double = 55.0,
    val shield: Double = 0.0,
    val maxShield: Double = 0.0,
    val damage: Double = 7.0,
    val armor: Double = 1.5,
    val tag: String = "Normal",
    val isBoss: Boolean = false,
    val goldReward: Long = 5L,
    val xpReward: Long = 15L,
    val killsRequired: Int = 10
)

@Serializable
data class Beast(
    val id: String = "",
    val name: String = "",
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
    val id: String = "",
    val type: String = "Skeleton",
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
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "beast_ferocity", name = "Predatory Conditioning", description = "+15% Companion beast DPS per rank", rank = 0, baseCost = 150L),
        DisciplineUpgrade(id = "beast_tame_rate", name = "Pheromone Baits", description = "+5% Beast taming encounter chance per floor", rank = 0, baseCost = 200L)
    )
)

@Serializable
data class DisciplineUpgrade(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val rank: Int = 0,
    val maxRank: Int = 10,
    val baseCost: Long = 100L,
    val costMultiplier: Double = 1.5,
    val costCurrency: String = "GOLD" // "GOLD", "FAITH", "BONE", "SLAYER"
)

@Serializable
data class Plot(
    val id: Int = 0,
    val herbName: String = "Felweed",
    val reqLvl: Int = 1,
    val growthProgress: Float = 0f,
    val durationSeconds: Float = 10f,
    val isReady: Boolean = false,
    val autoQueue: Boolean = false,
    val proficiencyLevel: Int = 1,
    val proficiencyXp: Long = 0L
)

@Serializable
data class MiningVein(
    val id: Int = 0,
    val oreName: String = "Copper Ore",
    val reqLvl: Int = 1,
    val miningProgress: Float = 0f,
    val durationSeconds: Float = 5f,
    val isReady: Boolean = false,
    val autoQueue: Boolean = false,
    val proficiencyLevel: Int = 1,
    val proficiencyXp: Long = 0L
)

@Serializable
data class HarvestState(
    val farmingLvl: Int = 1,
    val farmingXp: Double = 0.0,
    val miningLvl: Int = 1,
    val miningXp: Double = 0.0,
    val plots: List<Plot> = listOf(
        Plot(id = 0, herbName = "Healing Herb", reqLvl = 1, durationSeconds = 4f),
        Plot(id = 1, herbName = "Lesser Healing Herb", reqLvl = 5, durationSeconds = 5f),
        Plot(id = 2, herbName = "Mana Herb", reqLvl = 10, durationSeconds = 6f),
        Plot(id = 3, herbName = "Antidote Herb", reqLvl = 15, durationSeconds = 7f),
        Plot(id = 4, herbName = "Vitality Herb", reqLvl = 20, durationSeconds = 8f),
        Plot(id = 5, herbName = "Spirit Grass", reqLvl = 25, durationSeconds = 10f),
        Plot(id = 6, herbName = "Moonshade Herb", reqLvl = 30, durationSeconds = 12f),
        Plot(id = 7, herbName = "Sunroot Herb", reqLvl = 35, durationSeconds = 14f),
        Plot(id = 8, herbName = "Silverleaf Herb", reqLvl = 40, durationSeconds = 16f),
        Plot(id = 9, herbName = "Windwhisper Grass", reqLvl = 45, durationSeconds = 18f),
        Plot(id = 10, herbName = "Bloodroot", reqLvl = 50, durationSeconds = 20f),
        Plot(id = 11, herbName = "Frostbloom", reqLvl = 55, durationSeconds = 22f),
        Plot(id = 12, herbName = "Emberleaf", reqLvl = 60, durationSeconds = 24f),
        Plot(id = 13, herbName = "Dreamer's Sage", reqLvl = 65, durationSeconds = 26f),
        Plot(id = 14, herbName = "Golden Ginseng", reqLvl = 70, durationSeconds = 28f),
        Plot(id = 15, herbName = "Moonlight Blossom", reqLvl = 75, durationSeconds = 30f),
        Plot(id = 16, herbName = "Sacred Lotus", reqLvl = 80, durationSeconds = 32f),
        Plot(id = 17, herbName = "Mandrake Root", reqLvl = 85, durationSeconds = 35f),
        Plot(id = 18, herbName = "Spiritbloom", reqLvl = 90, durationSeconds = 38f),
        Plot(id = 19, herbName = "Worldtree Leaf", reqLvl = 95, durationSeconds = 42f)
    ),
    val veins: List<MiningVein> = listOf(
        MiningVein(id = 0, oreName = "Stone", reqLvl = 1, durationSeconds = 3f),
        MiningVein(id = 1, oreName = "Coal", reqLvl = 5, durationSeconds = 4f),
        MiningVein(id = 2, oreName = "Copper Ore", reqLvl = 10, durationSeconds = 5f),
        MiningVein(id = 3, oreName = "Iron Ore", reqLvl = 15, durationSeconds = 6f),
        MiningVein(id = 4, oreName = "Tin Ore", reqLvl = 20, durationSeconds = 7f),
        MiningVein(id = 5, oreName = "Silver Ore", reqLvl = 25, durationSeconds = 8f),
        MiningVein(id = 6, oreName = "Gold Ore", reqLvl = 30, durationSeconds = 10f),
        MiningVein(id = 7, oreName = "Quartz", reqLvl = 35, durationSeconds = 12f),
        MiningVein(id = 8, oreName = "Diamond", reqLvl = 40, durationSeconds = 14f),
        MiningVein(id = 9, oreName = "Emerald", reqLvl = 45, durationSeconds = 16f),
        MiningVein(id = 10, oreName = "Ruby", reqLvl = 50, durationSeconds = 18f),
        MiningVein(id = 11, oreName = "Sapphire", reqLvl = 55, durationSeconds = 20f),
        MiningVein(id = 12, oreName = "Lapis Lazuli", reqLvl = 60, durationSeconds = 22f),
        MiningVein(id = 13, oreName = "Redstone", reqLvl = 65, durationSeconds = 24f),
        MiningVein(id = 14, oreName = "Obsidian", reqLvl = 70, durationSeconds = 26f),
        MiningVein(id = 15, oreName = "Platinum Ore", reqLvl = 75, durationSeconds = 28f),
        MiningVein(id = 16, oreName = "Titanium Ore", reqLvl = 80, durationSeconds = 30f),
        MiningVein(id = 17, oreName = "Cobalt Ore", reqLvl = 85, durationSeconds = 32f),
        MiningVein(id = 18, oreName = "Uranium Ore", reqLvl = 90, durationSeconds = 35f),
        MiningVein(id = 19, oreName = "Mithril Ore", reqLvl = 95, durationSeconds = 40f)
    ),
    val autoCollectUnlocked: Boolean = false,
    val autoCollectEnabled: Boolean = true,
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "harvest_speed", name = "Accelerated Bio-Growth", description = "-8% Farm plot growth duration per rank", rank = 0, baseCost = 80L),
        DisciplineUpgrade(id = "mining_speed", name = "Sonic Laser Drills", description = "-8% Ore extraction time per rank", rank = 0, baseCost = 100L),
        DisciplineUpgrade(id = "harvest_yield", name = "Rich Bio-Extracts", description = "+1 additional resource yield per harvest", rank = 0, baseCost = 150L)
    )
)

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

@Serializable
data class TrainingNode(
    val id: String = "",
    val name: String = "",
    val stat: String = "",
    val rank: Int = 0,
    val maxRank: Int = 1_000_000,
    val costGold: Long = 50L
)

@Serializable
data class TrainingState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val nodes: List<TrainingNode> = listOf(
        TrainingNode(id = "str_bonus", name = "Titan Strength", stat = "+5 STR per rank", rank = 0, maxRank = 1_000_000, costGold = 50L),
        TrainingNode(id = "vit_bonus", name = "Iron Constitution", stat = "+5 VIT per rank", rank = 0, maxRank = 1_000_000, costGold = 50L),
        TrainingNode(id = "int_bonus", name = "Arcane Mind", stat = "+5 INT per rank", rank = 0, maxRank = 1_000_000, costGold = 50L),
        TrainingNode(id = "speed_bonus", name = "Swift Strikes", stat = "+5% Attack Speed per rank (max +100%)", rank = 0, maxRank = 20, costGold = 100L),
        TrainingNode(id = "luck_bonus", name = "Fortune's Favor", stat = "+5% Drop Chance per rank (max 100%)", rank = 0, maxRank = 13, costGold = 75L)
    )
)

@Serializable
data class EssenceState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val faith: Long = 50L,
    /** Sub-unit remainder so the Aegis drain is not truncated to a whole faith every tick. */
    val faithRemainder: Double = 0.0,
    val aegisActive: Boolean = true,
    val smiteLevel: Int = 1,
    val upgrades: List<DisciplineUpgrade> = listOf(
        DisciplineUpgrade(id = "essence_shield", name = "Radiant Capacitor", description = "+20% Aegis barrier absorption efficiency", rank = 0, baseCost = 40L, costCurrency = "FAITH"),
        DisciplineUpgrade(id = "smite_overdrive", name = "Solar Lance", description = "+30% Smite burst damage per rank", rank = 0, baseCost = 50L, costCurrency = "FAITH"),
        DisciplineUpgrade(id = "faith_preserve", name = "Aura Efficiency", description = "-15% Faith drain rate during Aegis deployment", rank = 0, baseCost = 60L, costCurrency = "FAITH")
    )
)

@Serializable
data class Talent(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: String = "ACTIVE", // "ACTIVE", "PASSIVE_STRUGGLE"
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
    val talentPoints: Int = 5,
    val struggleDamageTaken: Long = 0L,
    /** Sub-unit remainder of damage endured, so struggle scaling is not truncated every tick. */
    val struggleDamageFraction: Double = 0.0,
    val struggleHpThreshold: Long = 100L,
    val struggleArmorThreshold: Long = 500L,
    val struggleManaThreshold: Long = 150L,
    val struggleAtkThreshold: Long = 250L,
    val struggleShieldThreshold: Long = 200L,
    val activeTalents: List<Talent> = listOf(
        Talent(id = "berserk", name = "Berserk Overdrive", description = "+50% ATK Speed & +35% Damage for 10s (20 Mana)", type = "ACTIVE", rank = 1, cooldownSeconds = 30.0, durationSeconds = 10.0),
        Talent(id = "nanite_heal", name = "Nanite Repair", description = "Instantly restores 40% Max HP & 25% Shield (15 Mana)", type = "ACTIVE", rank = 1, cooldownSeconds = 25.0, durationSeconds = 0.0),
        Talent(id = "support_overclock", name = "Legion Overclock", description = "Minions and Beasts deal 300% DPS for 12s (30 Mana)", type = "ACTIVE", rank = 1, cooldownSeconds = 40.0, durationSeconds = 12.0),
        Talent(id = "mana_rampage", name = "Mana Tempest", description = "Consumes 25 Mana to strike enemy with 250% Arcane Damage", type = "ACTIVE", rank = 1, cooldownSeconds = 20.0, durationSeconds = 0.0),
        Talent(id = "aegis_bastion", name = "Aegis Bastion", description = "Deploys a defensive kinetic barrier (+60% Max Shield, 20 Mana)", type = "ACTIVE", rank = 1, cooldownSeconds = 35.0, durationSeconds = 0.0),
        Talent(id = "arcane_enchant", name = "Enchantment Matrix", description = "+25% Critical Strike Chance & +50% Crit DMG for 12s (25 Mana)", type = "ACTIVE", rank = 1, cooldownSeconds = 30.0, durationSeconds = 12.0)
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
    val id: String = "slayer_1",
    val targetName: String = "Any Hostile",
    val tier: String = "F-",
    val targetFloor: Int = 1,
    val currentKills: Int = 0,
    val requiredKills: Int = 15,
    val rewardGold: Long = 200L,
    val rewardPoints: Int = 5,
    val rewardDiamonds: Long = 0L,
    val rewardFaith: Long = 0L,
    val rewardBones: Long = 0L,
    val isCompleted: Boolean = false
)

@Serializable
data class SlayerState(
    val lvl: Int = 1,
    val xp: Long = 0L,
    val slayerPoints: Int = 10,
    val currentTask: SlayerTask = SlayerTask(),
    val tasks: List<SlayerTask> = emptyList()
)

@Serializable
data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val category: String = "Resource", // "Equipment", "Potion", "Resource", "Trophy"
    val count: Double = 1.0,
    val tier: Int = 1,
    val slot: String = "", // "Rifle", "Sidearm", "Blade", "Visor", "Vest", "Grips", "Pants", "Boots", "Duster", "Rig", "Node I", "Node II", "Node III"
    val statDesc: String = "",
    val sellValue: Long = 10L,
    val isEquipped: Boolean = false,
    val atk: Double = 0.0,
    val def: Double = 0.0,
    val vit: Double = 0.0
) {
    constructor(
        id: String,
        name: String,
        category: String,
        count: Int,
        tier: Int = 1,
        slot: String = "",
        statDesc: String = "",
        sellValue: Long = 10L,
        isEquipped: Boolean = false,
        atk: Double = 0.0,
        def: Double = 0.0,
        vit: Double = 0.0
    ) : this(id, name, category, count.toDouble(), tier, slot, statDesc, sellValue, isEquipped, atk, def, vit)

    constructor(
        id: String,
        name: String,
        category: String,
        count: Double,
        tier: Int,
        slot: String,
        statDesc: String,
        sellValue: Long,
        isEquipped: Boolean,
        atk: Double,
        def: Double
    ) : this(id, name, category, count, tier, slot, statDesc, sellValue, isEquipped, atk, def, 0.0)
}

@Serializable
data class LogEntry(
    val id: Long = 0L,
    val text: String = "",
    val type: String = "info", // "combat", "loot", "system", "gain", "milestone", "skill", "tame"
    val timestampMs: Long = 0L
)

@Serializable
data class LootItem(
    val name: String = "",
    val count: Int = 0
)

@Serializable
data class OfflineLoot(
    val timeAwaySeconds: Long = 0L,
    val kills: Long = 0L,
    val goldEarned: Long = 0L,
    val xpEarned: Long = 0L,
    val bonesEarned: Long = 0L,
    val playerLevelsGained: Int = 0,
    val talentPointsGained: Int = 0,
    val floorsGained: Int = 0,
    val startFloor: Int = 1,
    val finalFloor: Int = 1,
    /** Per-floor kill progress carried out of the simulation, so the live monster matches. */
    val killsOnFloor: Int = 0,
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
    val timelineLog: List<String> = emptyList(),
    /** Real wall time away, before the offline cap. */
    val rawAwaySeconds: Long = 0L,
    /** Combat time actually simulated: capped away time scaled by efficiency. */
    val effectiveSeconds: Long = 0L,
    val faithEarned: Long = 0L,
    val faithDrained: Long = 0L,
    val bossKills: Int = 0,
    val floorsLost: Int = 0,
    val slayerKills: Int = 0,
    val aegisCoveragePct: Int = 0,
    val dps: Double = 0.0,
    val xpMultiplier: Double = 1.0
)

@Serializable
data class Achievement(
    val id: String = "",
    val title: String = "",
    val desc: String = "",
    val progress: Int = 0,
    val target: Int = 1,
    val isUnlocked: Boolean = false,
    val isClaimed: Boolean = false,
    val rewardGold: Long = 100L
)

/**
 * Presentation preferences that must survive a restart.
 *
 * These never change stored gameplay values: [notation] only selects how numbers are rendered, and
 * an unknown/missing value decodes to the default (old saves load unchanged).
 */
@Serializable
data class DisplaySettings(
    /** `ALPHABETIC` (default), `COMPACT`, `GROUPED` or `SCIENTIFIC`; see BigNumberFormatter. */
    val notation: String = "ALPHABETIC"
)

@Serializable
data class AutomationSettings(
    val autoEquipBestGear: Boolean = true,
    val autoMergeBeasts: Boolean = false,
    val craftMaterialUsagePct: Int = 75,
    /** Keeps the game loop running (via a foreground service) while the app is not on screen. */
    val runInBackground: Boolean = false
)

@Serializable
data class PromotionState(
    val tier: Int = 1,
    val maxTier: Int = 20,
    val tierName: String = "Novice Vanguard",
    val requiredFloor: Int = 5,
    val requiredGearScore: Int = 0,
    val bonusPercent: Int = 0
)

@Serializable
data class SaveMeta(
    val savedWallMs: Long = 0L,
    val savedElapsedMs: Long = 0L,
    val bootCount: Int = 0,
    val nextId: Long = 100L
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
data class ActiveBuff(
    val id: String = "",
    val name: String = "",
    val type: String = "", // "REGEN_HP", "REGEN_MANA", "SHIELD", "DAMAGE_REDUCTION", "LIFESTEAL", "HEAL_BOOST"
    val value: Double = 0.0,
    val durationRemaining: Double = 0.0,
    val totalDuration: Double = 0.0
)

@Serializable
data class PotionSettings(
    val hpAutoThresholdPct: Int = 0, // 0 = OFF, 30 = 30%, 50 = 50%, etc.
    val mpAutoThresholdPct: Int = 0,
    val selectedHpPotion: String = "Minor Health Flask",
    val selectedMpPotion: String = "Mana Infusion Tonic"
)

@Serializable
data class DailyLoginState(
    val lastClaimWallMs: Long = 0L,
    val lastSprintClaimEpochDay: Long = -1L,
    val lastCalendarClaimEpochDay: Long = -1L,
    val sprint7ClaimedDays: Int = 0,
    val calendar30ClaimedDays: Int = 0,
    val totalLogins: Int = 1,
    val lastLoginEpochDay: Long = -1L
)

/**
 * Dev-only overrides used by the debug Dev Panel. Persisted so a tester's game speed survives a
 * restart; release builds never expose the panel that writes them.
 */
@Serializable
data class DevSettings(
    val gameSpeed: Int = 1,
    val unlockedAll: Boolean = false
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
    val display: DisplaySettings = DisplaySettings(),
    val promotion: PromotionState = PromotionState(),
    val potionSettings: PotionSettings = PotionSettings(),
    val activeBuffs: List<ActiveBuff> = emptyList(),
    val dailyLogin: DailyLoginState = DailyLoginState(),
    val dev: DevSettings = DevSettings(),
    val inventory: List<InventoryItem> = listOf(
        InventoryItem(id = "item_rifle_f", name = "F- Rifle", category = "Equipment", count = 1, tier = 1, slot = "Rifle", statDesc = "+12 ATK", sellValue = 35L, isEquipped = true, atk = 12.0),
        InventoryItem(id = "item_vest_f", name = "F- Vest", category = "Equipment", count = 1, tier = 1, slot = "Vest", statDesc = "+8 DEF, +4 VIT", sellValue = 35L, isEquipped = true, def = 8.0, vit = 4.0),
        InventoryItem(id = "item_blade_f", name = "F- Blade", category = "Equipment", count = 1, tier = 1, slot = "Blade", statDesc = "+12 ATK", sellValue = 35L, isEquipped = true, atk = 12.0),
        InventoryItem(id = "item_visor_f", name = "F- Visor", category = "Equipment", count = 1, tier = 1, slot = "Visor", statDesc = "+8 DEF, +4 VIT", sellValue = 35L, isEquipped = true, def = 8.0, vit = 4.0),
        InventoryItem(id = "item_node1_f", name = "F- Node I", category = "Equipment", count = 1, tier = 1, slot = "Node I", statDesc = "+8 DEF, +4 VIT", sellValue = 35L, isEquipped = true, def = 8.0, vit = 4.0),
        InventoryItem(id = "item_node2_f", name = "F- Node II", category = "Equipment", count = 1, tier = 1, slot = "Node II", statDesc = "+8 DEF, +4 VIT", sellValue = 35L, isEquipped = true, def = 8.0, vit = 4.0),
        InventoryItem(id = "item_hp_flask", name = "Minor Health Flask", category = "Potion", count = 5, tier = 1, statDesc = "Restores 5% missing HP", sellValue = 10L),
        InventoryItem(id = "item_mp_tonic", name = "Mana Infusion Tonic", category = "Potion", count = 5, tier = 1, statDesc = "Restores 25% missing mana", sellValue = 15L),
        InventoryItem(id = "item_copper", name = "Copper Ore", category = "Resource", count = 12, tier = 1, statDesc = "Conductive red metal ore", sellValue = 3L),
        InventoryItem(id = "item_healing_herb", name = "Healing Herb", category = "Resource", count = 8, tier = 1, statDesc = "Cellular regenerative herb", sellValue = 2L)
    ),
    val logs: List<LogEntry> = listOf(
        LogEntry(id = 1L, text = "SYSTEM INITIALIZED: Welcome to True IDLE Sector 01.", type = "system"),
        LogEntry(id = 2L, text = "RADAR CONTACT: Hostiles detected in perimeter.", type = "combat")
    ),
    val pendingOfflineLoot: OfflineLoot? = null,
    val achievements: List<Achievement> = listOf(
        Achievement(id = "ach_first_blood", title = "First Blood", desc = "Defeat your first monster", progress = 0, target = 1, isUnlocked = false, rewardGold = 50L),
        Achievement(id = "ach_floor_10", title = "Dungeon Crawler", desc = "Reach Floor 10", progress = 1, target = 10, isUnlocked = false, rewardGold = 250L),
        Achievement(id = "ach_tamer", title = "Beast Whisperer", desc = "Tame 3 companion beasts", progress = 0, target = 3, isUnlocked = false, rewardGold = 200L),
        Achievement(id = "ach_necro", title = "Lord of Bones", desc = "Summon an army of 10 minions", progress = 2, target = 10, isUnlocked = false, rewardGold = 300L),
        Achievement(id = "ach_miner", title = "Master Miner", desc = "Mine 50 ores", progress = 0, target = 50, isUnlocked = false, rewardGold = 150L)
    )
)
