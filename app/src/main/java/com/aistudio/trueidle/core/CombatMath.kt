package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.EssenceState
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

    /** Corps DPS, scaled by the Grave Momentum discipline when it has been researched. */
    fun armyDps(n: NecromancyState): Double {
        val rank = n.upgrades.firstOrNull { it.id == "necro_dps" }?.rank ?: 0
        val bonus = 1.0 + 0.10 * rank
        return n.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) } * bonus
    }

    fun totalDps(s: GameState, d: Derived): Double {
        val supportOverclock = s.talent.activeTalents.firstOrNull { it.id == "support_overclock" }?.isActive == true
        val minionMultiplier = if (supportOverclock) 3.0 else 1.0
        val ferocityRank = s.beasts.upgrades.firstOrNull { it.id == "beast_ferocity" }?.rank ?: 0
        val ferocity = 1.0 + 0.15 * ferocityRank
        return heroDps(d) + (beastDps(s.beasts, ferocity) + armyDps(s.necro)) * minionMultiplier
    }

    /**
     * Effective Aegis damage reduction for this essence state: the base barrier value scaled by the
     * Radiant Capacitor upgrade and capped short of total immunity. Zero while the barrier is down.
     */
    fun aegisReduction(essence: EssenceState): Double {
        if (!essence.aegisActive) return 0.0
        val rank = essence.upgrades.firstOrNull { it.id == "essence_shield" }?.rank ?: 0
        return (GameCaps.AEGIS_DAMAGE_REDUCTION * (1.0 + 0.20 * rank)).coerceIn(0.0, 0.80)
    }

    /**
     * Damage per second the hero takes after armor and the Aegis barrier. The reduction is passed in
     * rather than assumed, so the barrier upgrade cannot be honoured in the engine and forgotten in
     * the UI, and the barrier maths lives in exactly one place.
     */
    fun incomingPerSec(m: Monster, d: Derived, aegisReduction: Double = 0.0): Double {
        val raw = m.damage * (1.0 - aegisReduction.coerceIn(0.0, 0.9)) * (1.0 - d.damageReduction.coerceIn(0.0, 0.9))
        return (raw - d.armorDef * 0.3).coerceAtLeast(1.0)
    }
}
