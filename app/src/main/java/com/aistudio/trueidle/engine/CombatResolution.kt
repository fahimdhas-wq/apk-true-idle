package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.model.EssenceState
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.SlayerTask
import kotlin.random.Random

/**
 * The combat tick plus kill/death resolution and slayer contract payouts. Engine-owned side effects
 * (potion deployment, task generation, respawn gap) are injected, so this class only edits a Frame.
 */
internal class CombatResolution(
    private val random: Random,
    private val logger: (String, String) -> LogEntry,
    private val loot: LootSystem,
    private val deployPotion: (Frame, String) -> Boolean,
    private val ensureSlayerTasks: (List<SlayerTask>) -> List<SlayerTask>,
    private val refreshTask: (String) -> SlayerTask,
    private val setRespawnGap: (Double) -> Unit
) {

    private fun log(type: String, text: String): LogEntry = logger(type, text)

    fun tick(frame: Frame, dt: Double) {
        val d = frame.derived()
        val monster = frame.monster

        val totalDps = CombatMath.totalDps(frame.view(), d)
        val dealt = totalDps * CombatMath.mitigation(monster.armor) * dt
        val dealtSplit = CombatOps.splitDamage(monster.shield, monster.hp, dealt)
        frame.monster = monster.copy(shield = dealtSplit.shield, hp = dealtSplit.hp)

        // Minion plating soaks a bounded share of incoming damage.
        val soakRank = frame.necro.upgrades.firstOrNull { it.id == "necro_soak" }?.rank ?: 0
        val soak = frame.necro.army.sumOf { it.soak * it.count }
        val soakReduction = CombatOps.necroSoakReduction(soak, soakRank)
        val aegisMult = 1.0 - CombatMath.aegisReduction(frame.essence)
        val incomingRate = CombatMath.incomingPerSec(monster, d, 0.0) * aegisMult
        val monsterDamage = incomingRate * dt * (1.0 - soakReduction)

        val taken = CombatOps.splitDamage(frame.player.shield, frame.player.hp, monsterDamage)
        var playerShield = taken.shield
        var playerHp = taken.hp

        // Lifesteal restores health on damage dealt
        if (d.lifesteal > 0.0 && dealt > 0.0) {
            val stolen = dealt * d.lifesteal
            playerHp = (playerHp + stolen).coerceAtMost(d.maxHp)
        }

        // Auto-potion triggers
        if (frame.potionSettings.hpAutoThresholdPct > 0 && d.maxHp > 0.0) {
            val hpPct = (playerHp / d.maxHp) * 100.0
            if (hpPct <= frame.potionSettings.hpAutoThresholdPct) {
                deployPotion(frame, frame.potionSettings.selectedHpPotion)
                playerHp = frame.player.hp
                playerShield = frame.player.shield
            }
        }
        if (frame.potionSettings.mpAutoThresholdPct > 0 && d.maxMana > 0.0) {
            val mpPct = (frame.player.mana / d.maxMana) * 100.0
            if (mpPct <= frame.potionSettings.mpAutoThresholdPct) {
                deployPotion(frame, frame.potionSettings.selectedMpPotion)
            }
        }

        // Struggle passives count all damage sustained in combat (both absorbed by shield and HP damage),
        // ensuring talents and struggle adaptation build reliably even when shield absorbs the blow.
        val totalDamageEndured = monsterDamage.coerceAtLeast(0.0)
        if (totalDamageEndured > 0.0) {
            val accumulated = frame.talent.struggleDamageFraction + totalDamageEndured
            val whole = accumulated.toLong()
            frame.talent = frame.talent.copy(
                struggleDamageFraction = accumulated - whole,
                struggleDamageTaken = frame.talent.struggleDamageTaken + whole
            )
        }

        frame.player = frame.player.copy(hp = playerHp, shield = playerShield)

        val killedThisTick = frame.monster.hp <= 0.0
        if (killedThisTick) {
            val monsterEhp = (monster.maxHp + monster.maxShield).coerceAtLeast(1.0)
            val overkillRatio = dealt / monsterEhp
            val hitsPerSec = d.atkSpeed
            // When attack speed > 10/s or overkill ratio >= 2, batch kills into an analytical pass to prevent lag
            val batchCount = if ((hitsPerSec > 10.0 || overkillRatio >= 2.0) && !monster.isBoss) {
                overkillRatio.toInt().coerceIn(1, 200)
            } else 1

            resolveKill(frame, d, batchCount)
        }
        // A kill and a defeat can land in the same tick; the kill wins, so the death path is
        // skipped rather than resetting the floor the reward just advanced.
        if (!killedThisTick && frame.player.hp <= 0.0) {
            resolveDeath(frame)
        }
    }

    private fun resolveKill(frame: Frame, d: Derived, batchCount: Int = 1) {
            val monster = frame.monster
            var player = frame.player
            val count = batchCount.coerceAtLeast(1)
            val xpGained = maxOf(1L, (monster.xpReward * d.xpGain).toLong()) * count
            val goldGained = monster.goldReward * count
            frame.lifetime = frame.lifetime.copy(totalKills = frame.lifetime.totalKills + count)
            if (count > 1) {
                frame.logs += log(
                    LogType.GAIN,
                    "HIGH-SPEED ENGAGEMENT: x$count ${monster.name.uppercase()} NEUTRALIZED (+$goldGained CREDITS, +$xpGained XP)"
                )
            } else {
                frame.logs += log(
                    LogType.GAIN,
                    "HOSTILE NEUTRALIZED: ${monster.name.uppercase()} (+$goldGained CREDITS, +$xpGained XP)"
                )
            }

            var xp = player.xp + xpGained
            var lvl = player.lvl
            var levels = 0
            while (xp >= Balance.playerXpReq(lvl)) {
                xp -= Balance.playerXpReq(lvl)
                lvl++
                levels++
            }
            player = player.copy(
                gold = player.gold + goldGained,
                xp = xp,
                lvl = lvl,
                killsOnFloor = player.killsOnFloor + count
            )
            if (levels > 0) {
                val after = DerivedStats.of(frame.view().copy(player = player))
                player = player.copy(hp = after.maxHp, shield = after.maxShield, mana = after.maxMana)
                frame.talent = frame.talent.copy(talentPoints = frame.talent.talentPoints + levels)
                frame.logs += log(LogType.SKILL, "PLAYER LEVEL UP: Advanced to Level $lvl!")
            }
            if (monster.isBoss) {
                frame.talent = frame.talent.copy(talentPoints = frame.talent.talentPoints + 2 * count)
            }
            frame.player = player

            // Bone essence honours Soul Siphon offline and live.
            val boneMult = 1.0 + 0.25 * (frame.necro.upgrades.firstOrNull { it.id == "necro_harvest" }?.rank ?: 0)
            val bones = (random.nextLong(1L, 4L) * boneMult).toLong().coerceAtLeast(1L) * count
            frame.necro = frame.necro.copy(boneEssence = frame.necro.boneEssence + bones)

            frame.essence = grantFaith(frame.essence, (if (monster.isBoss) 5L else 1L) * count)
            frame.essence = grantEssenceXp(frame.essence, (if (monster.isBoss) 25L else 5L) * count, frame)

            grantSlayerProgress(frame, monster, count)
            loot.rollTame(frame, monster, d)
            loot.rollLoot(frame, d)
            if (monster.isBoss) loot.grantTrophy(frame)
            advanceFloor(frame, monster)
            if (frame.base.automation.autoMergeBeasts) loot.autoMergeBeasts(frame)
            frame.invalidateDerived()
        }

    private fun resolveDeath(frame: Frame) {
            val player = frame.player
            val failures = player.failuresInRow + 1
            frame.lifetime = frame.lifetime.copy(defeats = frame.lifetime.defeats + 1)
            frame.logs += log(LogType.COMBAT, "SYSTEMS REBOOTING: Hull compromised. Initializing emergency stasis.")

            var floor = player.currentFloor
            var newFailures = failures
            // Retreat engages regardless of auto-advance, otherwise an unwinnable floor is a death loop.
            if (failures >= Balance.RETREAT_FAILURE_THRESHOLD && player.currentFloor > 1) {
                floor = player.currentFloor - 1
                newFailures = 0
                frame.logs += log(
                    LogType.SYSTEM,
                    "RETREAT PROTOCOL ENGAGED: Descending to Sector $floor for strategic regroup."
                )
            }

            frame.player = player.copy(
                hp = 0.0,
                recoverLeft = Balance.RECOVERY_SECONDS,
                failuresInRow = newFailures,
                currentFloor = floor,
                killsRequired = Balance.killsRequiredFor(floor)
            )
            setRespawnGap(0.0)
            frame.monster = Balance.generateMonsterForFloor(floor)
        }

    private fun advanceFloor(frame: Frame, monster: Monster) {
            var player = frame.player
            val required = Balance.killsRequiredFor(player.currentFloor)
            var floor = player.currentFloor
            var maxUnlocked = player.maxUnlockedFloor

            if (player.killsOnFloor >= required) {
                // Floor 100 has nothing beyond it: re-clearing must not re-award diamonds forever.
                if (floor >= maxUnlocked && maxUnlocked < Balance.MAX_FLOOR) {
                    maxUnlocked = (floor + 1).coerceAtMost(Balance.MAX_FLOOR)
                    frame.logs += log(LogType.MILESTONE, "SECTOR UNLOCKED: Sector $floor secured!")
                    if (monster.isBoss) {
                        player = player.copy(diamonds = player.diamonds + 5L)
                        frame.logs += log(LogType.MILESTONE, "DIAMONDS EARNED: +5 for a boss first clear.")
                    }
                }
                player = player.copy(killsOnFloor = 0, failuresInRow = 0)
                if (player.autoAdvance) floor = (floor + 1).coerceAtMost(Balance.MAX_FLOOR)
            }

            frame.player = player.copy(
                currentFloor = floor,
                maxUnlockedFloor = maxUnlocked,
                killsRequired = Balance.killsRequiredFor(floor)
            )
            setRespawnGap(Balance.RESPAWN_GAP_SECONDS)
            frame.monster = Balance.generateMonsterForFloor(floor)
        }

    private fun grantFaith(essence: EssenceState, amount: Long): EssenceState =
        essence.copy(faith = essence.faith + amount)

    private fun grantEssenceXp(essence: EssenceState, amount: Long, frame: Frame): EssenceState {
            var xp = essence.xp + amount
            var lvl = essence.lvl
            var leveled = false
            while (xp >= Balance.skillXpReq(lvl)) {
                xp -= Balance.skillXpReq(lvl)
                lvl++
                leveled = true
            }
            if (leveled) frame.logs += log(LogType.SKILL, "CLASS LEVEL UP: Essence reached Level $lvl!")
            return essence.copy(xp = xp, lvl = lvl)
        }

    private fun grantSlayerProgress(frame: Frame, monster: Monster, count: Int = 1) {
            val tasks = ensureSlayerTasks(frame.slayer.tasks)
            var slayer = frame.slayer
            var player = frame.player
            var essence = frame.essence
            var necro = frame.necro
            var tasksUpdated = false

            val newTasks = tasks.map { task ->
                if (task.isCompleted) {
                    task
                } else {
                    tasksUpdated = true
                    val kills = task.currentKills + count
                    if (kills < task.requiredKills) {
                        task.copy(currentKills = kills)
                    } else {
                        // Automatic mission rewards upon completion
                        player = player.copy(
                            gold = player.gold + task.rewardGold,
                            diamonds = player.diamonds + task.rewardDiamonds
                        )
                        slayer = slayer.copy(slayerPoints = slayer.slayerPoints + task.rewardPoints)
                        if (task.rewardFaith > 0L) {
                            essence = essence.copy(faith = essence.faith + task.rewardFaith)
                        }
                        if (task.rewardBones > 0L) {
                            necro = necro.copy(boneEssence = necro.boneEssence + task.rewardBones)
                        }

                        val tierRank = TierConfig.rankOf(task.tier).coerceAtLeast(0)
                        val xpEarned = (100L * (1.0 + tierRank * 0.25)).toLong()
                        var sxp = slayer.xp + xpEarned
                        var slvl = slayer.lvl
                        while (sxp >= Balance.slayerXpReq(slvl)) {
                            sxp -= Balance.slayerXpReq(slvl)
                            slvl++
                            frame.logs += log(LogType.SKILL, "CLASS LEVEL UP: Slayer reached Level $slvl!")
                        }
                        slayer = slayer.copy(xp = sxp, lvl = slvl)

                        var bonusDesc = ""
                        if (task.rewardDiamonds > 0L) bonusDesc += ", +${task.rewardDiamonds} DIA"
                        if (task.rewardFaith > 0L) bonusDesc += ", +${task.rewardFaith} FAITH"
                        if (task.rewardBones > 0L) bonusDesc += ", +${task.rewardBones} BONE"

                        frame.logs += log(
                            LogType.MILESTONE,
                            "SLAYER CONTRACT COMPLETE [${task.tier}]: ${task.targetName} (+${task.rewardGold} CR, +${task.rewardPoints} PTS$bonusDesc)"
                        )

                        // Instant auto-refresh upon completion
                        refreshTask(task.id)
                    }
                }
            }

            if (tasksUpdated) {
                frame.player = player
                frame.slayer = slayer.copy(
                    tasks = newTasks,
                    currentTask = newTasks.firstOrNull() ?: com.aistudio.trueidle.model.SlayerTask()
                )
                frame.essence = essence
                frame.necro = necro
            }
        }
}
