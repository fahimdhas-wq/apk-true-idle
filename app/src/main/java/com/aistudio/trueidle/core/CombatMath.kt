package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.NecromancyState

object CombatMath {
    const val ARMOR_K = 100.0 // mitigation = K / (K + armor)

    fun mitigation(armor: Double): Double =
        ARMOR_K / (ARMOR_K + armor.coerceAtLeast(0.0))

    /**
     * The single source of truth for hero damage. Both live combat and the Stats screen use this,
     * so ring ATK, the Vengeful Rebound (ATK) passive, Berserk's damage multiplier and critical
     * strikes all reach real combat. `DerivedStats` calls this same function to fill
     * `Derived.physicalDamage`.
     */
    fun heroDps(
        str: Double,
        weaponDmg: Double,
        ringBonus: Double,
        smiteBonus: Double,
        struggleAtk: Double,
        atkSpeed: Double,
        dmgMultiplier: Double,
        critChance: Double,
        critDamage: Double
    ): Double {
        val base = (str * 1.2 + weaponDmg + ringBonus + smiteBonus + struggleAtk) * atkSpeed * dmgMultiplier
        return base * critMultiplier(critChance, critDamage)
    }

    /** Expected damage multiplier from critical strikes. */
    fun critMultiplier(critChance: Double, critDamage: Double): Double =
        1.0 + critChance.coerceIn(0.0, 1.0) * (critDamage - 1.0).coerceAtLeast(0.0)

    fun heroDps(d: Derived): Double = heroDps(
        str = d.str,
        weaponDmg = d.weaponDmg,
        ringBonus = d.ringBonus,
        smiteBonus = d.smiteBonus,
        struggleAtk = d.struggleAtkBonus,
        atkSpeed = d.atkSpeed,
        dmgMultiplier = d.dmgMultiplier,
        critChance = d.critChance,
        critDamage = d.critDamage
    )

    /**
     * Damage of a single swing. `heroDps` is a per-second figure; burst abilities must not inherit
     * `atkSpeed` or they would be multiplied by it a second time.
     */
    fun heroHitDamage(d: Derived): Double = heroDps(d) / d.atkSpeed.coerceAtLeast(1e-6)

    fun beastDps(b: BeastTamingState, ferocityMultiplier: Double = 1.0): Double =
        b.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) } * ferocityMultiplier

    fun armyDps(n: NecromancyState): Double =
        n.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }

    fun totalDps(s: GameState, d: Derived): Double {
        val supportOverclock = s.talent.activeTalents.firstOrNull { it.id == "support_overclock" }?.isActive == true
        val minionMultiplier = if (supportOverclock) 3.0 else 1.0
        val ferocityRank = s.beasts.upgrades.firstOrNull { it.id == "beast_ferocity" }?.rank ?: 0
        val ferocity = 1.0 + 0.15 * ferocityRank
        return heroDps(d) + (beastDps(s.beasts, ferocity) + armyDps(s.necro)) * minionMultiplier
    }

    /** Damage per second the hero takes after armor, before shield. Floored at 1/s as today. */
    fun incomingPerSec(m: Monster, d: Derived, aegis: Boolean): Double {
        val raw = m.damage * if (aegis) 0.8 else 1.0
        return (raw - d.armorDef * 0.3).coerceAtLeast(1.0)
    }
}
