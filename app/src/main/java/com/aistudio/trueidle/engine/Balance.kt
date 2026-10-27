package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.MinionCatalog
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.Monster
import kotlin.math.pow

/**
 * The single balance table. Every tuning number the engine and the UI share lives here, so the
 * formula in a screen can never drift from the formula in the engine.
 */
object Balance {
    // ---- Idle / offline ----

    /** Hard cap on offline/idle accumulation. Hours come from the shared cap table. */
    const val OFFLINE_CAP_MS = GameCaps.OFFLINE_CAP_HOURS * 3600_000L

    /** Minimum time away before the offline report is produced. */
    const val OFFLINE_MIN_AWAY_MS = 5 * 60_000L

    /** Fraction of time away converted into rewards while offline. */
    const val OFFLINE_EFFICIENCY = GameCaps.OFFLINE_EFFICIENCY_PCT / 100.0

    // ---- Combat ----

    /** Seconds the hero is out of action after a defeat. */
    const val RECOVERY_SECONDS = 6.0

    /** Seconds between monster spawns (death respawn and floor changes). */
    const val RESPAWN_GAP_SECONDS = 0.5

    /** Ordinary floors require this many kills; boss floors require a single kill. */
    const val KILLS_PER_FLOOR = 10
    const val BOSS_KILLS_PER_FLOOR = 1

    /** Consecutive defeats before the retreat protocol engages. */
    const val RETREAT_FAILURE_THRESHOLD = 3

    /** Mana regenerated per second, capped by derived max mana. */
    const val MANA_REGEN_PER_SEC = 2.0

    /** Faith drained per second while Aegis is deployed (offline uses the same constant). */
    const val AEGIS_DRAIN_PER_SEC = 0.5

    /** Damage reduction the Aegis barrier provides (base value, scaled by Radiant Capacitor). */
    const val AEGIS_DAMAGE_REDUCTION = GameCaps.AEGIS_DAMAGE_REDUCTION

    // ---- Caps ----

    const val BEAST_CAP = GameCaps.BEAST
    const val MINION_CAP = GameCaps.MINION
    const val LOG_CAP = GameCaps.LOG
    const val MAX_FLOOR = GameCaps.MAX_FLOOR

    /** Ids below this are reserved by the default save (beast_1, minion_1, log ids 1-2). */
    const val RESERVED_ID_FLOOR = GameCaps.RESERVED_ID_FLOOR

    // ---- Progression curves ----

    /**
     * Skill XP curve. The old `1000 * lvl^3` needed ~81,000 manual harvests for level 10, which put
     * the auto-collect unlock out of reach; this curve keeps a long tail without being impossible.
     */
    fun skillXpReq(lvl: Int): Long = XpCurves.skill(lvl)

    fun slayerXpReq(lvl: Int): Long = XpCurves.slayer(lvl)

    fun playerXpReq(lvl: Int): Long = XpCurves.player(lvl)

    // ---- Shared cost formulas ----

    fun upgradeCost(upgrade: DisciplineUpgrade): Long =
        UpgradeCosts.nextRank(upgrade.baseCost, upgrade.rank, upgrade.costMultiplier)

    fun smiteUpgradeCost(smiteLevel: Int): Long = UpgradeCosts.smite(smiteLevel)

    fun talentManaCost(talentId: String): Double = when (talentId) {
        "berserk" -> 20.0
        "nanite_heal" -> 15.0
        "support_overclock" -> 30.0
        "mana_rampage" -> 25.0
        "aegis_bastion" -> 20.0
        "arcane_enchant" -> 25.0
        else -> 0.0
    }

    // ---- Minions (single source for engine and UI copy) ----

    fun minionTypeFor(type: String): String = MinionCatalog.normalized(type)

    fun minionDps(type: String): Double = MinionCatalog.dps(type)

    fun minionCost(type: String): Long = MinionCatalog.cost(type)

    const val MINION_SOAK = MinionCatalog.SOAK

    // ---- Floors ----

    fun isBossFloor(floor: Int): Boolean = floor % 10 == 0

    fun killsRequiredFor(floor: Int): Int =
        if (isBossFloor(floor)) BOSS_KILLS_PER_FLOOR else KILLS_PER_FLOOR

    val MOB_NAMES = listOf(
        "Cave Rat", "Goblin Scout", "Crypt Spider", "Skeletal Raider",
        "Bandit Marauder", "Dungeon Ogre", "Shadow Wraith", "Infernal Hound",
        "Stone Golem", "Dark Sorcerer", "Abyssal Drake", "Void Fiend"
    )

    val BOSS_NAMES = listOf(
        "Broodmother Spider", "Goblin King Gorm", "Bone Dragon Malgok",
        "Overlord of Embers", "Arch-Lich Valok", "Hydra of the Deeps",
        "Void Reaver Titan", "Grand Patriarch Xur",
        "Chronos Singularity", "Omega Prime Sovereign"
    )

    fun generateMonsterForFloor(floor: Int): Monster {
        val safeFloor = floor.coerceIn(1, MAX_FLOOR)
        val isBoss = isBossFloor(safeFloor)
        val name = if (isBoss) {
            BOSS_NAMES[((safeFloor / 10) - 1) % BOSS_NAMES.size]
        } else {
            MOB_NAMES[(safeFloor - 1) % MOB_NAMES.size]
        }

        val scale = 1.12.pow((safeFloor - 1).toDouble())
        val baseHp = 40.0 * scale + (safeFloor * 15.0)
        val finalHp = if (isBoss) baseHp * 3.5 else baseHp
        val baseDmg = 5.0 * 1.09.pow((safeFloor - 1).toDouble()) + (safeFloor * 2.0)
        val finalDmg = if (isBoss) baseDmg * 2.2 else baseDmg
        val shield = if (isBoss) finalHp * 0.3 else (if (safeFloor > 5) finalHp * 0.15 else 0.0)

        return Monster(
            name = name,
            lvl = safeFloor,
            hp = finalHp,
            maxHp = finalHp,
            shield = shield,
            maxShield = shield,
            damage = finalDmg,
            armor = (safeFloor * 1.5).coerceAtLeast(1.0),
            tag = if (isBoss) "BOSS" else "Normal",
            isBoss = isBoss,
            goldReward = (5L * safeFloor * (if (isBoss) 5L else 1L)).coerceAtLeast(5L),
            xpReward = (15L * safeFloor * (if (isBoss) 4L else 1L)).coerceAtLeast(15L),
            killsRequired = killsRequiredFor(safeFloor)
        )
    }

    // ---- Promotion ----

    val TIER_NAMES = listOf(
        "Novice Vanguard", "Ironbound Warden", "Crypt Cleaver", "Aether Sentinel",
        "Dawnbreaker", "Void Sovereign", "Abyssal Juggernaut", "Eclipse Titan",
        "Solar Warlord", "Apex Immortal", "Storm Herald", "Rift Strider",
        "Obsidian Marshal", "Crimson Arbiter", "Nova Ascendant", "Eternal Sentinel",
        "Starforged Champion", "Dread Colossus", "Celestial Overlord", "Singularity Prime"
    )

    fun tierName(tier: Int): String = TIER_NAMES.getOrElse(tier - 1) { "Apex Rank $tier" }

    /**
     * Promotion requirements grow with the tier. `requiredFloor` saturates at the deepest floor,
     * so gear score carries the progression past that point.
     */
    fun requiredFloorFor(tier: Int): Int = (5 + 6 * (tier - 1)).coerceIn(1, MAX_FLOOR)

    fun requiredGearScoreFor(tier: Int): Int = (tier - 1) * 400
}
