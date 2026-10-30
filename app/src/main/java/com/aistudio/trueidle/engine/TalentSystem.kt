package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.PlayerState
import com.aistudio.trueidle.model.Talent

/**
 * Talent effects, timing and cooldown maths. Kept free of GameState so a manual activation and the
 * engine's auto-trigger pass share exactly the same rules instead of duplicating the formulas.
 */
internal object TalentSystem {

    data class Effect(val player: PlayerState, val monster: Monster)

    fun apply(
        talentId: String,
        rank: Int,
        derived: Derived,
        player: PlayerState,
        monster: Monster
    ): Effect {
        val rankScale = rankScale(rank)
        return when (talentId) {
            "nanite_heal" -> Effect(
                player.copy(
                    hp = (player.hp + derived.maxHp * 0.40 * rankScale).coerceAtMost(derived.maxHp),
                    shield = (player.shield + derived.maxShield * 0.25 * rankScale)
                        .coerceAtMost(derived.maxShield)
                ),
                monster
            )
            // Bursts are a single strike, not a per-second figure, and armour still mitigates them.
            "mana_rampage" -> Effect(player, strike(monster, derived, 2.5 * rankScale))
            "aegis_bastion" -> Effect(
                player.copy(
                    shield = (player.shield + derived.maxShield * 0.60 * rankScale)
                        .coerceAtMost(derived.maxShield * 1.5)
                ),
                monster
            )
            "phoenix_protocol" -> Effect(
                player.copy(
                    hp = derived.maxHp,
                    shield = (player.shield + derived.maxShield * 0.40 * rankScale)
                        .coerceAtMost(derived.maxShield)
                ),
                monster
            )
            "void_pulse" -> Effect(player, strike(monster, derived, 3.0 * rankScale))
            else -> Effect(player, monster) // Timed buffs are picked up by DerivedStats while active.
        }
    }

    fun durationSeconds(talent: Talent, rank: Int): Double =
        talent.durationSeconds * rankScale(rank)

    fun cooldownSeconds(talent: Talent, rank: Int): Double =
        (talent.cooldownSeconds * (1.0 - 0.05 * (rank.coerceAtLeast(1) - 1))).coerceAtLeast(5.0)

    /** Decays duration/cooldown for every talent that is still running. */
    fun tickTimers(talents: List<Talent>, dt: Double): List<Talent> = talents.map { talent ->
        if (talent.cooldownRemaining <= 0.0 && talent.durationRemaining <= 0.0 && !talent.isActive) {
            talent
        } else {
            val duration = (talent.durationRemaining - dt).coerceAtLeast(0.0)
            talent.copy(
                cooldownRemaining = (talent.cooldownRemaining - dt).coerceAtLeast(0.0),
                durationRemaining = duration,
                isActive = duration > 0.0
            )
        }
    }

    private fun rankScale(rank: Int): Double = 1.0 + 0.1 * (rank.coerceAtLeast(1) - 1)

    private fun strike(monster: Monster, derived: Derived, multiplier: Double): Monster {
        val burst = CombatMath.heroHitDamage(derived) * multiplier *
            CombatMath.mitigation(monster.armor)
        return CombatOps.damageMonster(monster, burst)
    }
}
