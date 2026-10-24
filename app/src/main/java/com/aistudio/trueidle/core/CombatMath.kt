package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.NecromancyState

object CombatMath {
    const val ARMOR_K = 100.0 // mitigation = K / (K + armor)

    fun mitigation(armor: Double): Double =
        ARMOR_K / (ARMOR_K + armor.coerceAtLeast(0.0))

    fun heroDps(d: Derived): Double =
        (d.str * 1.2 + d.weaponDmg + d.smiteBonus) * d.atkSpeed

    fun beastDps(b: BeastTamingState): Double =
        b.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }

    fun armyDps(n: NecromancyState): Double =
        n.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }

    fun totalDps(s: GameState, d: Derived): Double =
        heroDps(d) + beastDps(s.beasts) + armyDps(s.necro)

    /** Damage per second the hero takes after armor, before shield. Floored at 1/s as today. */
    fun incomingPerSec(m: Monster, d: Derived, aegis: Boolean): Double {
        val raw = m.damage * if (aegis) 0.8 else 1.0
        return (raw - d.armorDef * 0.3).coerceAtLeast(1.0)
    }
}
