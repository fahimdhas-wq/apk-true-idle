package com.aistudio.trueidle.engine

import com.aistudio.trueidle.model.Monster

/** Damage-split arithmetic shared by combat ticks and talent strikes. */
internal object CombatOps {

    data class Split(val shield: Double, val hp: Double)

    /** Shields absorb first; whatever is left lands on HP. */
    fun splitDamage(shield: Double, hp: Double, amount: Double): Split {
        if (shield <= 0.0) return Split(0.0, hp - amount)
        val absorbed = amount.coerceAtMost(shield)
        return Split((shield - absorbed).coerceAtLeast(0.0), hp - (amount - absorbed))
    }

    fun damageMonster(monster: Monster, amount: Double): Monster {
        val split = splitDamage(monster.shield, monster.hp, amount)
        return monster.copy(shield = split.shield, hp = split.hp.coerceAtLeast(0.0))
    }

    /** Undead plating soaks a bounded share of incoming damage. */
    fun necroSoakReduction(soak: Double, rank: Int): Double =
        if (soak > 0.0) {
            ((soak / (soak + 100.0)) * (1.0 + 0.2 * rank)).coerceAtMost(0.5)
        } else 0.0
}
