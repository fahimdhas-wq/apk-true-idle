package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.OfflineLoot
import com.aistudio.trueidle.model.SlayerTask

/**
 * Applies an offline report atomically: floor, monster, kills-on-floor, level ups, talent points
 * and lifetime stats all move together. Pure state transformation; achievements are evaluated by
 * the caller once the new state is installed.
 */
internal class OfflineClaimSystem(
    private val nextId: () -> Long,
    private val logger: (String, String) -> LogEntry,
    private val ensureSlayerTasks: (List<SlayerTask>) -> List<SlayerTask>,
    private val freshSlayerTask: (String) -> SlayerTask,
    private val createBeast: (Monster, String) -> Beast
) {

    fun claim(current: GameState, loot: OfflineLoot): GameState {
        val finalFloor = loot.finalFloor.coerceIn(1, Balance.MAX_FLOOR)
        // The monster on screen must belong to the destination floor.
        val monster = Balance.generateMonsterForFloor(finalFloor)

        val inventory = current.inventory.toMutableList()
        var herbs = 0
        var ores = 0
        for (item in loot.items) {
            InventoryOps.add(inventory, item.name, item.count.toDouble(), nextId)
            if (ItemCatalog.isHerb(item.name)) herbs += item.count
            if (ItemCatalog.isOre(item.name)) ores += item.count
        }

        var beasts = current.beasts
        if (loot.beastsTamed > 0) {
            val tamed = mutableListOf<Beast>()
            repeat(loot.beastsTamed) {
                if (beasts.beasts.size + tamed.size < Balance.BEAST_CAP) {
                    tamed += createBeast(monster, "Autonomous Scout")
                }
            }
            beasts = beasts.copy(beasts = beasts.beasts + tamed)
        }

        var lvl = current.player.lvl.coerceIn(1, GameCaps.MAX_LEVEL)
        var xp = if (lvl >= GameCaps.MAX_LEVEL) 0L else current.player.xp + loot.xpEarned
        var levels = 0
        while (lvl < GameCaps.MAX_LEVEL && xp >= Balance.playerXpReq(lvl)) {
            xp -= Balance.playerXpReq(lvl)
            lvl++
            levels++
        }
        if (lvl >= GameCaps.MAX_LEVEL) xp = 0L

        // Slayer kills earned offline, resolved across active tasks
        var slayer = current.slayer
        var slayerGold = 0L
        var slayerDiamonds = 0L
        var slayerFaith = 0L
        var slayerBones = 0L
        var remainingOfflineKills = loot.slayerKills
        if (remainingOfflineKills > 0) {
            val tasks = ensureSlayerTasks(slayer.tasks).toMutableList()
            for (i in tasks.indices) {
                if (remainingOfflineKills <= 0) break
                val task = tasks[i]
                if (!task.isCompleted) {
                    val killsNeeded = (task.requiredKills - task.currentKills).coerceAtLeast(0)
                    if (remainingOfflineKills < killsNeeded) {
                        tasks[i] = task.copy(currentKills = task.currentKills + remainingOfflineKills)
                        remainingOfflineKills = 0
                    } else {
                        remainingOfflineKills -= killsNeeded
                        slayerGold += task.rewardGold
                        slayerDiamonds += task.rewardDiamonds
                        slayerFaith += task.rewardFaith
                        slayerBones += task.rewardBones
                        val tierRank = TierConfig.rankOf(task.tier).coerceAtLeast(0)
                        val xpEarned = (100L * (1.0 + tierRank * 0.25)).toLong()
                        var slvl = slayer.lvl.coerceIn(1, GameCaps.MAX_LEVEL)
                        var sxp = if (slvl >= GameCaps.MAX_LEVEL) 0L else slayer.xp + xpEarned
                        while (slvl < GameCaps.MAX_LEVEL && sxp >= Balance.slayerXpReq(slvl)) {
                            sxp -= Balance.slayerXpReq(slvl)
                            slvl++
                        }
                        if (slvl >= GameCaps.MAX_LEVEL) sxp = 0L
                        slayer = slayer.copy(
                            lvl = slvl,
                            xp = sxp,
                            slayerPoints = slayer.slayerPoints + task.rewardPoints
                        )
                        tasks[i] = freshSlayerTask(task.id)
                    }
                }
            }
            slayer = slayer.copy(tasks = tasks, currentTask = tasks.firstOrNull() ?: SlayerTask())
        }

        var player = current.player.copy(
            gold = current.player.gold + loot.goldEarned + slayerGold,
            diamonds = current.player.diamonds + slayerDiamonds,
            xp = xp,
            lvl = lvl,
            currentFloor = finalFloor,
            maxUnlockedFloor = maxOf(current.player.maxUnlockedFloor, finalFloor),
            killsOnFloor = loot.killsOnFloor.coerceIn(0, Balance.killsRequiredFor(finalFloor)),
            killsRequired = Balance.killsRequiredFor(finalFloor),
            failuresInRow = 0
        )
        if (levels > 0) {
            // Level ups restore resources, exactly as they do on the live path.
            val d = DerivedStats.of(current.copy(player = player))
            player = player.copy(hp = d.maxHp, shield = d.maxShield, mana = d.maxMana)
        }

        return current.copy(
            player = player,
            monster = monster,
            necro = current.necro.copy(boneEssence = current.necro.boneEssence + loot.bonesEarned + slayerBones),
            essence = current.essence.copy(faith = current.essence.faith + loot.faithEarned + slayerFaith),
            slayer = slayer,
            beasts = beasts,
            inventory = inventory,
            talent = current.talent.copy(
                talentPoints = current.talent.talentPoints + levels + loot.talentPointsGained
            ),
            lifetime = current.lifetime.copy(
                totalKills = current.lifetime.totalKills + loot.kills,
                beastsTamed = current.lifetime.beastsTamed + loot.beastsTamed,
                herbsHarvested = current.lifetime.herbsHarvested + herbs,
                oresMined = current.lifetime.oresMined + ores
            ),
            pendingOfflineLoot = null,
            logs = (current.logs + logger(
                LogType.MILESTONE,
                "OFFLINE REPORT PROCESSED: +${loot.goldEarned} CREDITS, +${loot.xpEarned} XP, " +
                    "+${loot.bonesEarned} BONE ESSENCE, +${loot.faithEarned} FAITH (${loot.kills} kills)."
            )).takeLast(Balance.LOG_CAP)
        )
    }
}
