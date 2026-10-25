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
    val gearScore: Int = 1840,
    val struggleHpBonus: Double = 0.0,
    val struggleDefBonus: Double = 0.0,
    val struggleManaBonus: Double = 0.0,
    val struggleAtkBonus: Double = 0.0
)

object DerivedStats {
    fun of(s: GameState): Derived {
        val base = PlayerStats()
        fun rank(id: String) = (s.training.nodes.firstOrNull { it.id == id }?.rank ?: 0).toDouble()
        val lvl = s.player.lvl
        val str = base.str + 5.0 * rank("str_bonus")
        val luck = base.luck + 5.0 * rank("luck_bonus")

        // Struggle Passives calculation
        val struggleDamage = s.talent.struggleDamageTaken
        val hpRank = s.talent.struggleTalents.firstOrNull { it.id == "struggle_hp" }?.rank ?: 0
        val defRank = s.talent.struggleTalents.firstOrNull { it.id == "struggle_def" }?.rank ?: 0
        val manaRank = s.talent.struggleTalents.firstOrNull { it.id == "struggle_mana" }?.rank ?: 0
        val atkRank = s.talent.struggleTalents.firstOrNull { it.id == "struggle_atk" }?.rank ?: 0

        val struggleHp = (struggleDamage / 100) * (5.0 * hpRank)
        val struggleDef = (struggleDamage / 500) * (1.0 * defRank)
        val struggleMana = (struggleDamage / 150) * (3.0 * manaRank)
        val struggleAtk = (struggleDamage / 250) * (2.0 * atkRank)

        // Active talent buffs
        val isBerserk = s.talent.activeTalents.firstOrNull { it.id == "berserk" }?.isActive == true
        val isEnchant = s.talent.activeTalents.firstOrNull { it.id == "arcane_enchant" }?.isActive == true

        val speedMultiplier = if (isBerserk) 1.5 else 1.0
        val dmgMultiplier = if (isBerserk) 1.35 else 1.0
        val extraCrit = if (isEnchant) 0.25 else 0.0
        val extraCritDmg = if (isEnchant) 0.50 else 0.0

        val weapon = s.inventory.firstOrNull { it.isEquipped && it.slot == "Weapon" }
        val armor = s.inventory.firstOrNull { it.isEquipped && it.slot == "Armor" }
        val ring = s.inventory.firstOrNull { it.isEquipped && it.slot == "Ring" }
        val amulet = s.inventory.firstOrNull { it.isEquipped && it.slot == "Amulet" }

        val forgeQualityRank = s.armory.upgrades.firstOrNull { it.id == "forge_quality" }?.rank ?: 0
        val gearMult = 1.0 + (0.12 * forgeQualityRank)

        val rawWeaponDmg = weapon?.let { if (it.atk > 0.0) it.atk else it.tier * 12.0 } ?: 2.0
        val rawArmorDef = armor?.let { if (it.def > 0.0) it.def else it.tier * 8.0 } ?: 0.0

        val weaponDmg = rawWeaponDmg * gearMult
        val armorDef = (rawArmorDef + struggleDef) * gearMult
        val ringBonus = ring?.atk ?: 0.0
        val amuletBonus = amulet?.def ?: 0.0

        val effAtkSpeed = base.atkSpeed * 1.05.pow(rank("speed_bonus")) * speedMultiplier
        val physicalDmg = (str * 1.2 + weaponDmg + ringBonus + struggleAtk) * effAtkSpeed * dmgMultiplier
        val computedGearScore = 1200 + (lvl * 15) + (weaponDmg * 20).toInt() + (armorDef * 20).toInt() + ((ringBonus + amuletBonus) * 15).toInt()

        return Derived(
            str = str,
            intVal = base.intVal,
            vit = base.vit,
            luck = luck,
            atkSpeed = effAtkSpeed,
            maxHp = 40.0 + 5.0 * base.vit + 10.0 * lvl + 50.0 * rank("vit_bonus") + struggleHp,
            maxShield = 20.0 + 5.0 * lvl,
            maxMana = 30.0 + 2.0 * base.intVal + 25.0 * rank("int_bonus") + struggleMana,
            weaponDmg = weaponDmg,
            armorDef = armorDef,
            lootChance = 0.30 + luck / 100.0,
            tameChance = 0.10 + luck / 100.0,
            smiteBonus = if (s.essence.aegisActive) s.essence.smiteLevel * 3.0 else 0.0,
            critChance = 0.124 + (luck * 0.001) + extraCrit,
            critDamage = 1.85 + extraCritDmg,
            xpGain = 1.18,
            physicalDamage = physicalDmg,
            gearScore = computedGearScore,
            struggleHpBonus = struggleHp,
            struggleDefBonus = struggleDef,
            struggleManaBonus = struggleMana,
            struggleAtkBonus = struggleAtk
        )
    }
}
