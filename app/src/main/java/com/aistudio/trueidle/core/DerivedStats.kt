package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.GameState
import kotlin.math.pow

/**
 * Every value here is derived from [GameState] on demand. Nothing is cached in `PlayerState`,
 * so there is a single source of truth (the inventory and the discipline states).
 *
 * Defaults are neutral (0 / 1.0): test-constructed [Derived] instances must not inherit
 * placeholder numbers from the HTML mock this UI was ported from.
 */
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
    val ringBonus: Double = 0.0,
    val dmgMultiplier: Double = 1.0,
    val critChance: Double = 0.0,
    val critDamage: Double = 1.5,
    val xpGain: Double = 1.0,
    val physicalDamage: Double = 0.0,
    val gearScore: Int = 0,
    val healthRegen: Double = 0.0,
    val shieldRegen: Double = 0.0,
    val struggleHpBonus: Double = 0.0,
    val struggleDefBonus: Double = 0.0,
    val struggleManaBonus: Double = 0.0,
    val struggleAtkBonus: Double = 0.0,
    val struggleShieldBonus: Double = 0.0
)

object DerivedStats {
    fun of(s: GameState): Derived {
        fun rank(id: String) = (s.training.nodes.firstOrNull { it.id == id }?.rank ?: 0).toDouble()
        val lvl = s.player.lvl
        val bonusMult = 1.0 + s.promotion.bonusPercent / 100.0

        val equippedWeapons = s.inventory.filter { it.isEquipped && Equipment.isWeapon(it.slot) }
        val equippedArmorAndNodes = s.inventory.filter { it.isEquipped && Equipment.isArmorOrNode(it.slot) }
        val rawWeaponDmg = if (equippedWeapons.isNotEmpty()) {
            equippedWeapons.sumOf { Equipment.baseAttack(it) }
        } else {
            2.0
        }
        val rawArmorDef = equippedArmorAndNodes.sumOf { Equipment.baseDefense(it) }
        val rawItemVit = equippedArmorAndNodes.sumOf { Equipment.baseVit(it) }

        val str = (10.0 + 5.0 * rank("str_bonus") + 1.0 * (lvl - 1)) * bonusMult
        val luck = (5.0 + 5.0 * rank("luck_bonus")) * bonusMult
        // Dojo "Iron Constitution" / "Arcane Mind" genuinely raise VIT and INT instead of being
        // displayed as constants on the Stats screen, and both grow with the hero's level so
        // levelling actually adds power. Equipped armor and nodes add rawItemVit directly to VIT.
        val vit = (10.0 + 5.0 * rank("vit_bonus") + 0.5 * (lvl - 1) + rawItemVit) * bonusMult
        val intVal = (10.0 + 5.0 * rank("int_bonus") + 0.5 * (lvl - 1)) * bonusMult

        // Struggle passives scale with damage endured.
        val struggleDamage = s.talent.struggleDamageTaken
        fun struggleRank(id: String) = (s.talent.struggleTalents.firstOrNull { it.id == id }?.rank ?: 0).toDouble()
        // The per-increment thresholds come from the talent state so the talent description, the
        // model and this maths cannot disagree. Each is floored at 1 to survive a corrupt save.
        // Whole increments only: the description promises "+3 Max Mana per 150 DMG taken", so 1000
        // damage taken is 6 increments (18 mana), not 6.67 increments (20 mana).
        val hpStep = s.talent.struggleHpThreshold.coerceAtLeast(1L)
        val defStep = s.talent.struggleArmorThreshold.coerceAtLeast(1L)
        val manaStep = s.talent.struggleManaThreshold.coerceAtLeast(1L)
        val atkStep = s.talent.struggleAtkThreshold.coerceAtLeast(1L)
        val struggleHp = (struggleDamage / hpStep).toDouble() * 5.0 * struggleRank("struggle_hp")
        val struggleDef = (struggleDamage / defStep).toDouble() * 1.0 * struggleRank("struggle_def")
        val struggleMana = (struggleDamage / manaStep).toDouble() * 3.0 * struggleRank("struggle_mana")
        val struggleAtk = (struggleDamage / atkStep).toDouble() * 2.0 * struggleRank("struggle_atk")
        val shieldStep = s.talent.struggleShieldThreshold.coerceAtLeast(1L)
        val struggleShield = (struggleDamage / shieldStep).toDouble() * 2.0 * struggleRank("struggle_shield")

        // Active talent buffs.
        val berserk = s.talent.activeTalents.firstOrNull { it.id == "berserk" }
        val enchant = s.talent.activeTalents.firstOrNull { it.id == "arcane_enchant" }
        val isBerserk = berserk?.isActive == true
        val isEnchant = enchant?.isActive == true
        // Rank 1 = listed values; each further rank strengthens the buff by 10%.
        val berserkRank = (berserk?.rank ?: 1).coerceAtLeast(1)
        val enchantRank = (enchant?.rank ?: 1).coerceAtLeast(1)
        val speedMultiplier = if (isBerserk) 1.0 + 0.5 * (1.0 + 0.1 * (berserkRank - 1)) else 1.0
        val dmgMultiplier = if (isBerserk) 1.0 + 0.35 * (1.0 + 0.1 * (berserkRank - 1)) else 1.0
        val extraCrit = if (isEnchant) 0.25 * (1.0 + 0.1 * (enchantRank - 1)) else 0.0
        val extraCritDmg = if (isEnchant) 0.50 * (1.0 + 0.1 * (enchantRank - 1)) else 0.0

        val ring = s.inventory.firstOrNull { it.isEquipped && it.slot == Equipment.SLOT_RING }
        val amulet = s.inventory.firstOrNull { it.isEquipped && it.slot == Equipment.SLOT_AMULET }

        // forge_quality is applied here and nowhere else (craftEquipment bakes in no multiplier).
        val forgeQualityRank = s.armory.upgrades.firstOrNull { it.id == "forge_quality" }?.rank ?: 0
        val gearMult = 1.0 + (0.12 * forgeQualityRank)

        val weaponDmg = rawWeaponDmg * gearMult * bonusMult
        val armorDef = (rawArmorDef + struggleDef) * gearMult * bonusMult
        val ringBonus = (ring?.atk ?: 0.0) * bonusMult
        val amuletBonus = (amulet?.def ?: 0.0) * bonusMult

        val atkSpeed = s.player.let { 1.0 * 1.05.pow(rank("speed_bonus")) * speedMultiplier }
        val critChance = (0.124 + luck * 0.001 + extraCrit + 0.005 * rank("crit_bonus")).coerceIn(0.0, 1.0)
        val critDamage = 1.85 + extraCritDmg
        // Smite is decoupled from Aegis, and Solar Lance scales the burst per rank.
        val smiteRank = s.essence.upgrades.firstOrNull { it.id == "smite_overdrive" }?.rank ?: 0
        val smiteFocusRank = s.essence.upgrades.firstOrNull { it.id == "smite_focus" }?.rank ?: 0
        val smiteBonus = s.essence.smiteLevel * 3.0 * (1.0 + 0.30 * smiteRank) * (1.0 + 0.05 * smiteFocusRank)

        // Tame rate: base + luck, plus the Pheromone Baits discipline.
        val tameRateRank = s.beasts.upgrades.firstOrNull { it.id == "beast_tame_rate" }?.rank ?: 0
        val tameChance = 0.10 + luck / 100.0 + 0.05 * tameRateRank

        val maxHp = (40.0 + 5.0 * vit + 10.0 * lvl + 25.0 * rank("vit_bonus")) * bonusMult + struggleHp
        val shieldRank = rank("shield_bonus")
        val aegisReserveRank = s.essence.upgrades.firstOrNull { it.id == "aegis_reserve" }?.rank ?: 0
        val maxShield = (20.0 + 5.0 * lvl) * bonusMult *
            (1.0 + 0.05 * shieldRank) * (1.0 + 0.08 * aegisReserveRank) + struggleShield
        val maxMana = (30.0 + 2.0 * intVal + 15.0 * rank("int_bonus")) * bonusMult + struggleMana

        val physicalDmg = CombatMath.heroDps(
            str = str,
            weaponDmg = weaponDmg,
            ringBonus = ringBonus,
            smiteBonus = smiteBonus,
            struggleAtk = struggleAtk,
            atkSpeed = atkSpeed,
            dmgMultiplier = dmgMultiplier,
            critChance = critChance,
            critDamage = critDamage
        )
        val computedGearScore = 1200 + (lvl * 15) + (weaponDmg * 20).toInt() +
            (armorDef * 20).toInt() + (((ringBonus + amuletBonus) * 15) + (rawItemVit * 10)).toInt()

        return Derived(
            str = str,
            intVal = intVal,
            vit = vit,
            luck = luck,
            atkSpeed = atkSpeed,
            maxHp = maxHp,
            maxShield = maxShield,
            maxMana = maxMana,
            weaponDmg = weaponDmg,
            armorDef = armorDef,
            lootChance = 0.30 + luck / 100.0,
            tameChance = tameChance,
            smiteBonus = smiteBonus,
            ringBonus = ringBonus,
            dmgMultiplier = dmgMultiplier,
            critChance = critChance,
            critDamage = critDamage,
            xpGain = 1.0 + 0.02 * lvl + 0.02 * rank("xp_bonus"),
            physicalDamage = physicalDmg,
            gearScore = computedGearScore,
            healthRegen = maxHp * 0.015 * (1.0 + 0.04 * rank("regen_bonus")),
            shieldRegen = maxShield * 0.05 * (1.0 + 0.04 * rank("regen_bonus")),
            struggleHpBonus = struggleHp,
            struggleDefBonus = struggleDef,
            struggleManaBonus = struggleMana,
            struggleAtkBonus = struggleAtk,
            struggleShieldBonus = struggleShield
        )
    }
}
