package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.PlayerStats
import kotlin.math.pow

data class Derived(
    val str: Double,
    val intVal: Double,
    val vit: Double,
    val luck: Double,
    val atkSpeed: Double,
    val maxHp: Double,
    val maxShield: Double,
    val maxMana: Double,
    val weaponDmg: Double,
    val armorDef: Double,
    val lootChance: Double,
    val tameChance: Double,
    val smiteBonus: Double,
    val critChance: Double = 0.124,
    val critDamage: Double = 1.85,
    val xpGain: Double = 1.18,
    val physicalDamage: Double = 412.0,
    val gearScore: Int = 1840
)

object DerivedStats {
    fun of(s: GameState): Derived {
        val base = PlayerStats()
        fun rank(id: String) = (s.training.nodes.firstOrNull { it.id == id }?.rank ?: 0).toDouble()
        val lvl = s.player.lvl
        val str = base.str + 5.0 * rank("str_bonus")
        val luck = base.luck + 5.0 * rank("luck_bonus")
        val weapon = s.inventory.firstOrNull { it.isEquipped && it.slot == "Weapon" }
        val armor = s.inventory.firstOrNull { it.isEquipped && it.slot == "Armor" }
        val ring = s.inventory.firstOrNull { it.isEquipped && it.slot == "Ring" }
        val amulet = s.inventory.firstOrNull { it.isEquipped && it.slot == "Amulet" }

        val weaponDmg = weapon?.let { if (it.atk > 0.0) it.atk else it.tier * 12.0 } ?: 2.0
        val armorDef = armor?.let { if (it.def > 0.0) it.def else it.tier * 8.0 } ?: 0.0
        val ringBonus = ring?.atk ?: 0.0
        val amuletBonus = amulet?.def ?: 0.0

        val physicalDmg = (str * 1.2 + weaponDmg + ringBonus) * (base.atkSpeed * 1.05.pow(rank("speed_bonus")))
        val computedGearScore = 1200 + (lvl * 15) + (weaponDmg * 20).toInt() + (armorDef * 20).toInt() + ((ringBonus + amuletBonus) * 15).toInt()

        return Derived(
            str = str,
            intVal = base.intVal,
            vit = base.vit,
            luck = luck,
            atkSpeed = base.atkSpeed * 1.05.pow(rank("speed_bonus")),
            maxHp = 40.0 + 5.0 * base.vit + 10.0 * lvl + 50.0 * rank("vit_bonus"), // 100 at lvl 1
            maxShield = 20.0 + 5.0 * lvl,                                          // 25 at lvl 1
            maxMana = 30.0 + 2.0 * base.intVal + 25.0 * rank("int_bonus"),         // 50 at lvl 1
            weaponDmg = weaponDmg,
            armorDef = armorDef,
            lootChance = 0.30 + luck / 100.0,                                      // 0.35 at luck 5
            tameChance = 0.10 + luck / 100.0,                                      // 0.15 at luck 5
            smiteBonus = if (s.sanctified.aegisActive) s.sanctified.smiteLevel * 3.0 else 0.0,
            critChance = 0.124 + (luck * 0.001),
            critDamage = 1.85,
            xpGain = 1.18,
            physicalDamage = physicalDmg,
            gearScore = computedGearScore
        )
    }
}
