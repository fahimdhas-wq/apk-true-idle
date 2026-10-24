package com.aistudio.trueidle.engine

import com.aistudio.trueidle.model.Monster
import kotlin.math.floor
import kotlin.math.pow

object Balance {
    fun skillXpReq(lvl: Int): Long = floor(1000.0 * lvl.toDouble().pow(3.0)).toLong().coerceAtLeast(100L)

    fun slayerXpReq(lvl: Int): Long = floor(300.0 * lvl.toDouble().pow(3.0)).toLong().coerceAtLeast(50L)

    fun playerXpReq(lvl: Int): Long = floor(1830.0 * lvl.toDouble().pow(3.0)).toLong().coerceAtLeast(100L)

    private val MOB_NAMES = listOf(
        "Cave Rat", "Goblin Scout", "Crypt Spider", "Skeletal Raider",
        "Bandit Marauder", "Dungeon Ogre", "Shadow Wraith", "Infernal Hound",
        "Stone Golem", "Dark Sorcerer", "Abyssal Drake", "Void Fiend"
    )

    private val BOSS_NAMES = listOf(
        "Broodmother Spider", "Goblin King Gorm", "Bone Dragon Malgok",
        "Overlord of Embers", "Arch-Lich Valok", "Hydra of the Deeps",
        "Void Reaver Titan", "Grand Patriarch Xur",
        "Chronos Singularity", "Omega Prime Sovereign"
    )

    fun generateMonsterForFloor(floor: Int): Monster {
        val isBoss = (floor % 10 == 0)
        val baseIndex = ((floor - 1) % MOB_NAMES.size)
        val name = if (isBoss) {
            BOSS_NAMES[((floor / 10) - 1) % BOSS_NAMES.size]
        } else {
            MOB_NAMES[baseIndex]
        }

        val scale = 1.12.pow((floor - 1).toDouble().coerceAtLeast(0.0))
        val baseHp = 40.0 * scale + (floor * 15.0)
        val finalHp = if (isBoss) baseHp * 3.5 else baseHp
        val baseDmg = 5.0 * 1.09.pow((floor - 1).toDouble()) + (floor * 2.0)
        val finalDmg = if (isBoss) baseDmg * 2.2 else baseDmg
        val shield = if (isBoss) finalHp * 0.3 else (if (floor > 5) finalHp * 0.15 else 0.0)

        val gold = (5L * floor * (if (isBoss) 5L else 1L)).coerceAtLeast(5L)
        val xp = (15L * floor * (if (isBoss) 4L else 1L)).coerceAtLeast(15L)

        return Monster(
            name = name,
            lvl = floor,
            hp = finalHp,
            maxHp = finalHp,
            shield = shield,
            maxShield = shield,
            damage = finalDmg,
            armor = (floor * 1.5).coerceAtLeast(1.0),
            tag = if (isBoss) "BOSS" else "Normal",
            isBoss = isBoss,
            goldReward = gold,
            xpReward = xp
        )
    }
}
