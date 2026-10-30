package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry

/** Necromancy, beast fusion and dojo training actions. Each returns the next state. */
internal class ProgressionSystem(
    private val nextId: () -> Long,
    private val logger: (String, String) -> LogEntry,
    private val grantSkillXp: (Long, Int, Long, String, String) -> Triple<Long, Int, List<LogEntry>>
) {

    private fun log(type: String, text: String): LogEntry = logger(type, text)

    fun summonMinion(current: GameState, type: String): ActionOutcome {
        var state = current
        val kind = Balance.minionTypeFor(type)
        val cost = Balance.minionCost(kind)
        val armySize = state.necro.army.sumOf { it.count }
        if (armySize >= Balance.MINION_CAP) {
            state = state.copy(
                logs = (state.logs + log(LogType.SYSTEM, "REANIMATION REFUSED: legion at ${Balance.MINION_CAP}/${Balance.MINION_CAP}."))
                    .takeLast(Balance.LOG_CAP)
            )
            return ActionOutcome(state, false)
        }
        if (state.necro.boneEssence < cost) return ActionOutcome(state, false)

        val army = state.necro.army.toMutableList()
        val existing = army.indexOfFirst { it.type == kind }
        if (existing >= 0) {
            army[existing] = army[existing].copy(count = army[existing].count + 1)
        } else {
            army.add(
                com.aistudio.trueidle.model.Minion(
                    id = "minion_${nextId()}",
                    type = kind,
                    count = 1,
                    lvl = 1,
                    dps = Balance.minionDps(kind),
                    soak = Balance.MINION_SOAK
                )
            )
        }

        var xp = state.necro.xp + 20L
        var lvl = state.necro.lvl
        while (xp >= Balance.skillXpReq(lvl)) {
            xp -= Balance.skillXpReq(lvl)
            lvl++
        }
        state = state.copy(
            necro = state.necro.copy(
                boneEssence = state.necro.boneEssence - cost,
                army = army,
                xp = xp,
                lvl = lvl
            ),
            logs = (state.logs + log(LogType.SYSTEM, "NECRO ANIMATION: reanimated $kind for $cost BONE ESSENCE."))
                .takeLast(Balance.LOG_CAP)
        )
        return ActionOutcome(state, true)
    }

    fun mergeBeasts(current: GameState, beastId1: String, beastId2: String): GameState {
        var state = current
        val beasts = state.beasts.beasts.toMutableList()
        val first = beasts.find { it.id == beastId1 }
        val second = beasts.find { it.id == beastId2 }
        if (first == null || second == null) return state
        if (first.name != second.name || first.stars != second.stars) return state

        beasts.remove(first)
        beasts.remove(second)
        val upgraded = first.copy(
            stars = first.stars + 1,
            dps = first.dps * 1.6,
            maxHp = first.maxHp * 1.5,
            hp = first.maxHp * 1.5
        )
        beasts.add(upgraded)

        val (xp, lvl, logs) = grantSkillXp(
            state.beasts.xp,
            state.beasts.lvl,
            60L,
            "Beast Taming",
            "BEAST SYNTHESIS: ${upgraded.name} upgraded to ${upgraded.stars} stars."
        )
        state = state.copy(
            beasts = state.beasts.copy(beasts = beasts, xp = xp, lvl = lvl),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun trainNode(current: GameState, nodeId: String): GameState {
        var state = current
        val nodes = state.training.nodes.toMutableList()
        val idx = nodes.indexOfFirst { it.id == nodeId }
        if (idx < 0) return state
        val node = nodes[idx]
        if (node.rank >= node.maxRank || state.player.gold < node.costGold) return state

        val goldCost = node.costGold
        val nextCost = (node.costGold.toDouble() * 1.4)
            .coerceAtMost(Long.MAX_VALUE.toDouble() - 10_000)
            .toLong()
            .coerceAtLeast(1L)
        nodes[idx] = node.copy(rank = node.rank + 1, costGold = nextCost)
        val (xp, lvl, logs) = grantSkillXp(
            state.training.xp,
            state.training.lvl,
            40L,
            "Training",
            "DOJO ENHANCEMENT: ${node.name} upgraded to Rank ${node.rank + 1}."
        )
        state = state.copy(
            player = state.player.copy(gold = state.player.gold - goldCost),
            training = state.training.copy(nodes = nodes, xp = xp, lvl = lvl),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        return state
    }
}
