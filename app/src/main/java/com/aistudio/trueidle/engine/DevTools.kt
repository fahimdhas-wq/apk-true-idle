package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState

/** Developer panel actions: speed, currency grants and unlock-everything. */
internal class DevTools {

    fun setSpeed(current: GameState, multiplier: Int): GameState {
        var state = current
        state = state.copy(
            dev = state.dev.copy(gameSpeed = multiplier.coerceIn(StateSanitizer.DEV_MIN_SPEED, StateSanitizer.DEV_MAX_SPEED))
        )
        return state
    }

    fun addCurrency(current: GameState, kind: String, amount: Long): GameState {
        var state = current
        val delta = amount.coerceAtLeast(0L)
        if (delta == 0L) return state
        state = when (kind.uppercase()) {
            "GOLD" -> state.copy(player = state.player.copy(gold = state.player.gold + delta))
            "DIAMONDS" -> state.copy(player = state.player.copy(diamonds = state.player.diamonds + delta))
            "FAITH" -> state.copy(
                essence = state.essence.copy(
                    faith = state.essence.faith + delta,
                    aegisActive = true
                )
            )
            "BONES" -> state.copy(necro = state.necro.copy(boneEssence = state.necro.boneEssence + delta))
            "SLAYER" -> state.copy(slayer = state.slayer.copy(slayerPoints = state.slayer.slayerPoints + delta.toInt()))
            "TALENT" -> state.copy(talent = state.talent.copy(talentPoints = state.talent.talentPoints + delta.toInt()))
            else -> state
        }
        return state
    }

    fun unlockAll(current: GameState): GameState {
        var state = current
        fun maxed(list: List<DisciplineUpgrade>) = list.map { it.copy(rank = it.maxRank) }
        val skillLevel = StateSanitizer.DEV_MAX_SKILL_LEVEL
        state = state.copy(
            player = state.player.copy(maxUnlockedFloor = Balance.MAX_FLOOR),
            harvest = state.harvest.copy(
                farmingLvl = maxOf(state.harvest.farmingLvl, skillLevel),
                miningLvl = maxOf(state.harvest.miningLvl, skillLevel),
                plots = state.harvest.plots.map { it.copy(reqLvl = 1, isReady = true) },
                veins = state.harvest.veins.map { it.copy(reqLvl = 1, isReady = true) },
                upgrades = maxed(state.harvest.upgrades)
            ),
            armory = state.armory.copy(
                smithingLvl = maxOf(state.armory.smithingLvl, skillLevel),
                herbloreLvl = maxOf(state.armory.herbloreLvl, skillLevel),
                upgrades = maxed(state.armory.upgrades)
            ),
            training = state.training.copy(
                nodes = state.training.nodes.map { it.copy(rank = it.maxRank) }
            ),
            essence = state.essence.copy(
                smiteLevel = maxOf(state.essence.smiteLevel, skillLevel),
                upgrades = maxed(state.essence.upgrades)
            ),
            beasts = state.beasts.copy(
                lvl = maxOf(state.beasts.lvl, skillLevel),
                upgrades = maxed(state.beasts.upgrades)
            ),
            necro = state.necro.copy(
                lvl = maxOf(state.necro.lvl, skillLevel),
                upgrades = maxed(state.necro.upgrades)
            ),
            slayer = state.slayer.copy(lvl = maxOf(state.slayer.lvl, skillLevel)),
            talent = state.talent.copy(
                activeTalents = state.talent.activeTalents.map {
                    it.copy(rank = it.maxRank, cooldownRemaining = 0.0)
                },
                struggleTalents = state.talent.struggleTalents.map { it.copy(rank = it.maxRank) }
            ),
            achievements = state.achievements.map { it.copy(isUnlocked = true, isClaimed = false) },
            dev = state.dev.copy(unlockedAll = true)
        )
        return state
    }
}
