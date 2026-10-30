package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.model.ActiveBuff

/** Applies one potion to a frame: direct heals, cleanse and timed buffs. */
internal class PotionSystem(private val nextId: () -> Long) {
    fun apply(frame: Frame, potionName: String): Boolean {
        val inventory = frame.inventory
        val idx = inventory.indexOfFirst {
            it.name.equals(potionName, ignoreCase = true) && it.category == ItemCatalog.CATEGORY_POTION
        }
        if (idx < 0) return false
        val item = inventory[idx]
        val def = ItemCatalog.potion(item.name) ?: return false
        val d = frame.derived()

        val missingHp = (d.maxHp - frame.player.hp).coerceAtLeast(0.0)
        var healedHp = missingHp * def.directMissingHpPct * (1.0 + d.healBoost)
        if (def.directMaxHpPct > 0.0) {
            healedHp += d.maxHp * def.directMaxHpPct * (1.0 + d.healBoost)
        }

        val missingMana = (d.maxMana - frame.player.mana).coerceAtLeast(0.0)
        val healedMana = missingMana * def.directMissingManaPct

        val newFailures = if (def.cleanseNegatives) 0 else frame.player.failuresInRow

        frame.player = frame.player.copy(
            hp = (frame.player.hp + healedHp).coerceAtMost(d.maxHp),
            mana = (frame.player.mana + healedMana).coerceAtMost(d.maxMana),
            failuresInRow = newFailures
        )

        if (def.regenHpPctPerSec > 0.0) {
            frame.activeBuffs.removeAll { it.type == "REGEN_HP" && it.name == "${item.name} Regen" }
            frame.activeBuffs.add(
                ActiveBuff(
                    id = "buff_hp_${nextId()}",
                    name = "${item.name} Regen",
                    type = "REGEN_HP",
                    value = def.regenHpPctPerSec,
                    durationRemaining = def.regenDurationSec,
                    totalDuration = def.regenDurationSec
                )
            )
        }
        if (def.regenManaPctPerSec > 0.0) {
            frame.activeBuffs.removeAll { it.type == "REGEN_MANA" && it.name == "${item.name} Mana Regen" }
            frame.activeBuffs.add(
                ActiveBuff(
                    id = "buff_mp_${nextId()}",
                    name = "${item.name} Mana Regen",
                    type = "REGEN_MANA",
                    value = def.regenManaPctPerSec,
                    durationRemaining = def.regenDurationSec,
                    totalDuration = def.regenDurationSec
                )
            )
        }
        if (def.shieldPctMaxHp > 0.0) {
            frame.activeBuffs.removeAll { it.type == "SHIELD" && it.name == "${item.name} Shield" }
            frame.activeBuffs.add(
                ActiveBuff(
                    id = "buff_shd_${nextId()}",
                    name = "${item.name} Shield",
                    type = "SHIELD",
                    value = def.shieldPctMaxHp,
                    durationRemaining = def.shieldDurationSec,
                    totalDuration = def.shieldDurationSec
                )
            )
        }
        if (def.damageReductionPct > 0.0) {
            frame.activeBuffs.removeAll { it.type == "DAMAGE_REDUCTION" && it.name == "${item.name} Protection" }
            frame.activeBuffs.add(
                ActiveBuff(
                    id = "buff_red_${nextId()}",
                    name = "${item.name} Protection",
                    type = "DAMAGE_REDUCTION",
                    value = def.damageReductionPct,
                    durationRemaining = def.damageReductionDurationSec,
                    totalDuration = def.damageReductionDurationSec
                )
            )
        }
        if (def.lifestealPct > 0.0) {
            frame.activeBuffs.removeAll { it.type == "LIFESTEAL" && it.name == "${item.name} Lifesteal" }
            frame.activeBuffs.add(
                ActiveBuff(
                    id = "buff_ls_${nextId()}",
                    name = "${item.name} Lifesteal",
                    type = "LIFESTEAL",
                    value = def.lifestealPct,
                    durationRemaining = def.lifestealDurationSec,
                    totalDuration = def.lifestealDurationSec
                )
            )
        }
        if (def.healBoostPct > 0.0) {
            frame.activeBuffs.removeAll { it.type == "HEAL_BOOST" && it.name == "${item.name} Heal Boost" }
            frame.activeBuffs.add(
                ActiveBuff(
                    id = "buff_hb_${nextId()}",
                    name = "${item.name} Heal Boost",
                    type = "HEAL_BOOST",
                    value = def.healBoostPct,
                    durationRemaining = def.healBoostDurationSec,
                    totalDuration = def.healBoostDurationSec
                )
            )
        }

        if (item.count > 1.0) {
            inventory[idx] = item.copy(count = item.count - 1.0)
        } else {
            inventory.removeAt(idx)
        }
        frame.invalidateDerived()
        return true
    }
}
