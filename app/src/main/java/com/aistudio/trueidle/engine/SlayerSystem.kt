package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.SlayerTask
import kotlin.random.Random

/** Slayer contract generation, refill and paid rerolls. */
internal class SlayerSystem(
    private val random: Random,
    private val logger: (String, String) -> LogEntry
) {

    private fun log(type: String, text: String): LogEntry = logger(type, text)

    fun reroll(current: GameState, taskId: String = ""): GameState {
        var state = current
        // Manual refresh costs 1 Gold as specified by the user
        if (state.player.gold < 1L) return state
        val tasks = ensure(state, state.slayer.tasks).toMutableList()
        val idx = if (taskId.isNotEmpty()) tasks.indexOfFirst { it.id == taskId } else 0
        if (idx < 0) return state

        val oldTask = tasks[idx]
        val newTask = newTask(state, oldTask.id, null)
        tasks[idx] = newTask

        state = state.copy(
            player = state.player.copy(gold = state.player.gold - 1L),
            slayer = state.slayer.copy(
                tasks = tasks,
                currentTask = tasks.firstOrNull() ?: newTask
            ),
            logs = (state.logs + log(LogType.SYSTEM, "SLAYER CONTRACT REROLLED: ${newTask.targetName} [${newTask.tier}] for 1 Gold."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    /**
     * Generates a fresh directive matching the player's current stage range [max(1, currentFloor - 5), currentFloor]
     * and tier appropriate for player's Slayer level.
     */
    fun newTask(current: GameState, slotId: String, floorOverride: Int?): SlayerTask {
        var state = current
        val reachable = state.player.maxUnlockedFloor.coerceIn(1, Balance.MAX_FLOOR)
        val currentFloor = state.player.currentFloor.coerceIn(1, reachable)
        val minFloor = maxOf(1, currentFloor - 5)
        val maxFloor = currentFloor.coerceAtMost(reachable).coerceAtLeast(minFloor)
        val floor = floorOverride ?: if (maxFloor > minFloor) random.nextInt(minFloor, maxFloor + 1) else minFloor

        val maxRank = SlayerMath.maxTierRankForLevel(state.slayer.lvl)
        val minRank = maxOf(0, maxRank - 4)
        val tierRank = if (maxRank > minRank) random.nextInt(minRank, maxRank + 1) else minRank
        val tierId = TierConfig.idAt(tierRank) ?: "F-"

        // Kill-count bounties against any hostile: a contract never sends the player back to an
        // earlier sector hunting one species. Bigger totals roll in at higher tiers.
        val requiredKills = SlayerMath.killCountPool(tierRank).random(random)
        val tierMultiplier = 1.0 + (tierRank * 0.35)
        val killMultiplier = 1.0 + requiredKills / 100.0
        val rawBaseGold = ((floor * 35L + 100L) * tierMultiplier * killMultiplier).toLong()
        val rewardGold = SlayerMath.scaledGold(rawBaseGold, state.slayer.lvl)
        val basePoints = ((3 + (floor / 5) + (tierRank / 3)) * (1.0 + requiredKills / 250.0))
            .toInt()
            .coerceIn(3, 500)
        val rewardPoints = SlayerMath.scaledPoints(basePoints, state.slayer.lvl)

        val bonus = SlayerMath.rollBonusRewards(tierRank, rewardGold, random)

        return com.aistudio.trueidle.model.SlayerTask(
            id = slotId,
            targetName = SlayerMath.ANY_HOSTILE,
            tier = tierId,
            targetFloor = floor,
            currentKills = 0,
            requiredKills = requiredKills,
            rewardGold = rewardGold + bonus.extraGold,
            rewardPoints = rewardPoints,
            rewardDiamonds = bonus.diamonds,
            rewardFaith = bonus.faith,
            rewardBones = bonus.bones,
            isCompleted = false
        )
    }

    fun ensure(current: GameState, tasks: List<com.aistudio.trueidle.model.SlayerTask>): List<com.aistudio.trueidle.model.SlayerTask> {
        var state = current
        val list = tasks.toMutableList()
        while (list.size < SlayerMath.MAX_TASKS) {
            val id = "slayer_${list.size + 1}_${random.nextInt(1000, 9999)}"
            list.add(newTask(state, id, null))
        }
        return list.take(SlayerMath.MAX_TASKS)
    }
}
