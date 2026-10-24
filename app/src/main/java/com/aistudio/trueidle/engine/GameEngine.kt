package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.GameClock
import com.aistudio.trueidle.core.Ids
import com.aistudio.trueidle.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.random.Random

class GameEngine(
    initialState: GameState = GameState(),
    private val clock: GameClock? = null,
    private val random: Random = Random.Default
) {
    var state: GameState = initialState
        private set

    val ids = Ids(maxOf(initialState.meta.nextId, (initialState.logs.maxOfOrNull { it.id } ?: 0L) + 1L, 100L))
    private val notifCenter = NotificationCenter(ids)

    private var respawnLeft: Double = 0.0

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private fun nowWallMs(): Long = clock?.wallMs() ?: System.currentTimeMillis()

    fun restore(newState: GameState) {
        state = newState
        val maxLogId = newState.logs.maxOfOrNull { it.id } ?: 0L
        ids.reset(maxOf(newState.meta.nextId, maxLogId + 1L, 100L))
    }

    fun notifyRecovered(slot: String) {
        pushNotification(
            title = "SAVE RECOVERED",
            message = "Restored previous backup save slot ($slot)",
            type = "system",
            status = "ALERT"
        )
    }

    fun snapshotForSave(now: ClockSnapshot): GameState {
        val meta = state.meta.copy(
            savedWallMs = now.wallMs,
            savedElapsedMs = now.elapsedMs,
            bootCount = now.bootCount,
            nextId = ids.current()
        )
        state = state.copy(meta = meta, lastSaveTimestamp = now.wallMs)
        return state
    }

    fun resume(now: ClockSnapshot) {
        val awayMs = OfflineEngine.trustedAwayMs(state.meta, now)
        if (awayMs < 0L) {
            pushNotification(
                title = "TIME ANOMALY",
                message = "CLOCK ANOMALY DETECTED — OFFLINE GAINS SUSPENDED",
                type = "combat",
                status = "ALERT"
            )
            return
        }

        if (awayMs >= 30_000L && now.wallMs > state.meta.offlineAppliedUntilWallMs) {
            val d = DerivedStats.of(state)
            val loot = OfflineEngine.compute(state, d, awayMs, 0.65, random)

            // Commit offline markers before showing dialog
            val updatedMeta = state.meta.copy(
                savedWallMs = now.wallMs,
                savedElapsedMs = now.elapsedMs,
                bootCount = now.bootCount,
                offlineAppliedUntilWallMs = now.wallMs,
                nextId = ids.current()
            )

            // Also check auto-collect for gathering while away
            val updatedGathering = if (state.gathering.autoCollectUnlocked && state.gathering.autoCollectEnabled) {
                val plots = state.gathering.plots.map { it.copy(growthProgress = 0f, isReady = false) }
                val veins = state.gathering.veins.map { it.copy(miningProgress = 0f, isReady = false) }
                state.gathering.copy(plots = plots, veins = veins)
            } else {
                state.gathering
            }

            // Sanctified faith drain if aegisActive
            var sanctified = state.sanctified
            if (sanctified.aegisActive) {
                val drain = (awayMs / 1000.0 * 0.5).toLong()
                val newFaith = (sanctified.faith - drain).coerceAtLeast(0L)
                sanctified = sanctified.copy(
                    faith = newFaith,
                    aegisActive = newFaith > 0L
                )
            }

            state = state.copy(
                meta = updatedMeta,
                pendingOfflineLoot = loot,
                gathering = updatedGathering,
                sanctified = sanctified
            )
        }
    }

    fun claimOfflineLoot(): GameState {
        val loot = state.pendingOfflineLoot ?: return state
        val inv = state.inventory.toMutableList()

        // Apply loot items directly from loot.items
        for (item in loot.items) {
            val idx = inv.indexOfFirst { it.name == item.name }
            if (idx >= 0) {
                inv[idx] = inv[idx].copy(count = inv[idx].count + item.count)
            } else {
                val (cat, desc, value) = when (item.name) {
                    "Copper Ore" -> Triple("Resource", "Crafting metal ore", 2L)
                    "Felweed" -> Triple("Resource", "Alchemy herb", 3L)
                    else -> Triple("Resource", "Salvaged resource", 5L)
                }
                inv.add(
                    InventoryItem(
                        id = "item_${ids.next()}",
                        name = item.name,
                        category = cat,
                        count = item.count,
                        tier = 1,
                        statDesc = desc,
                        sellValue = value
                    )
                )
            }
        }

        // Tamed beasts
        var beasts = state.beasts
        if (loot.beastsTamed > 0) {
            val currentMonster = state.monster
            val newBeasts = mutableListOf<Beast>()
            for (i in 0 until loot.beastsTamed) {
                if (beasts.beasts.size + newBeasts.size < 8) {
                    newBeasts.add(
                        Beast(
                            id = "beast_${ids.next()}",
                            name = "${currentMonster.name} Scout",
                            rank = 1,
                            stars = 1,
                            dps = (currentMonster.damage * 0.6).coerceAtLeast(4.0),
                            hp = currentMonster.maxHp * 0.5,
                            maxHp = currentMonster.maxHp * 0.5,
                            trait = "Autonomous Scout"
                        )
                    )
                }
            }
            beasts = beasts.copy(beasts = beasts.beasts + newBeasts)
        }

        // Loop level-ups
        var curXp = state.player.xp + loot.xpEarned
        var curLvl = state.player.lvl
        var lvlsGained = 0
        while (true) {
            val reqXp = Balance.playerXpReq(curLvl)
            if (curXp >= reqXp) {
                curXp -= reqXp
                curLvl++
                lvlsGained++
            } else {
                break
            }
        }

        val updatedLifetime = state.lifetime.copy(
            totalKills = state.lifetime.totalKills + loot.kills,
            beastsTamed = state.lifetime.beastsTamed + loot.beastsTamed
        )

        val newLogs = state.logs + LogEntry(
            id = ids.next(),
            text = "OFFLINE REPORT PROCESSED: +${loot.goldEarned} 🪙, +${loot.xpEarned} XP, +${loot.bonesEarned} 💀 (${loot.kills} kills).",
            type = "milestone"
        )

        state = state.copy(
            player = state.player.copy(
                gold = state.player.gold + loot.goldEarned,
                xp = curXp,
                lvl = curLvl,
                currentFloor = loot.finalFloor,
                maxUnlockedFloor = maxOf(state.player.maxUnlockedFloor, loot.finalFloor)
            ),
            necro = state.necro.copy(boneEssence = state.necro.boneEssence + loot.bonesEarned),
            beasts = beasts,
            inventory = inv,
            pendingOfflineLoot = null,
            lifetime = updatedLifetime,
            logs = newLogs.takeLast(40)
        )

        pushNotification(
            title = "OFFLINE CLAIMS RECEIVED",
            message = "Added ${loot.goldEarned} 🪙, ${loot.xpEarned} XP, ${loot.bonesEarned} 💀",
            type = "gain",
            status = "SUCCESS"
        )

        evaluateAchievements()
        return state
    }

    fun pushNotification(
        title: String,
        message: String,
        type: String = "info",
        status: String = "SUCCESS",
        detail: String = ""
    ) {
        state = notifCenter.push(
            state = state,
            title = title,
            message = message,
            type = type,
            status = status,
            detail = detail,
            wallMs = nowWallMs()
        )
    }

    fun toggleNotificationBanners(): GameState {
        val next = !state.notificationBannersEnabled
        state = state.copy(
            notificationBannersEnabled = next,
            notifications = if (next) state.notifications else emptyList()
        )
        pushNotification(
            title = "SYSTEM SETTINGS",
            message = if (next) "Notification Banners ENABLED" else "Notification Banners MUTED",
            type = "system",
            status = "INFO"
        )
        return state
    }

    fun clearNotificationHistory(): GameState {
        state = state.copy(notificationHistory = emptyList())
        return state
    }

    fun dismissNotification(id: Long): GameState {
        state = state.copy(notifications = state.notifications.filterNot { it.id == id })
        return state
    }

    fun tick(deltaSeconds: Float): GameState {
        val dt = deltaSeconds.toDouble()
        val derived = DerivedStats.of(state)

        var p = state.player
        var m = state.monster
        var necro = state.necro
        var beasts = state.beasts
        var gathering = state.gathering
        var crafting = state.crafting
        var training = state.training
        var sanctified = state.sanctified
        var slayer = state.slayer
        var lifetime = state.lifetime
        var inventory = state.inventory.toMutableList()
        val newLogs = mutableListOf<LogEntry>()

        // 1. Recovery state check (§4.5)
        if (p.recoverLeft > 0.0) {
            val remainingRecover = (p.recoverLeft - dt).coerceAtLeast(0.0)
            if (remainingRecover == 0.0) {
                // Completed recovery: restore HP & Shield to derived maxima
                p = p.copy(
                    recoverLeft = 0.0,
                    hp = derived.maxHp,
                    shield = derived.maxShield
                )
                newLogs.add(LogEntry(ids.next(), "SYSTEMS REBOOTED: Defensive shields and vital reserves restored.", "system"))
            } else {
                p = p.copy(recoverLeft = remainingRecover)
            }
            state = state.copy(player = p, logs = (state.logs + newLogs).takeLast(40))
            return state
        }

        // 2. Respawn gap check
        if (respawnLeft > 0.0) {
            respawnLeft = (respawnLeft - dt).coerceAtLeast(0.0)
            state = state.copy(player = p)
            return state
        }

        // 3. Hero + support damage to monster: dps * dt * mitigation(monster.armor); shield first, then HP
        val totalDps = CombatMath.totalDps(state, derived)
        val mitigation = CombatMath.mitigation(m.armor)
        val effDps = totalDps * mitigation
        val damageDealt = effDps * dt

        var mShield = m.shield
        var mHp = m.hp
        if (mShield > 0.0) {
            val shieldDmg = damageDealt.coerceAtMost(mShield)
            mShield -= shieldDmg
            val overflow = damageDealt - shieldDmg
            mHp -= overflow
        } else {
            mHp -= damageDealt
        }

        // 4. Monster damage to hero: incomingPerSec * dt; shield absorbs first; remainder to HP
        val incomingRate = CombatMath.incomingPerSec(m, derived, sanctified.aegisActive)
        val monsterDmg = incomingRate * dt

        var pShield = p.shield
        var pHp = p.hp
        if (pShield > 0.0) {
            val absorb = monsterDmg.coerceAtMost(pShield)
            pShield -= absorb
            val overflow = monsterDmg - absorb
            pHp -= overflow
        } else {
            pHp -= monsterDmg
        }

        // 5. Mana regen (+2/s, capped by derived maxMana) & Sanctified aegis drain
        var pMana = (p.mana + (2.0 * dt)).coerceAtMost(derived.maxMana)
        if (sanctified.aegisActive) {
            val drain = 0.5 * dt
            if (sanctified.faith > 0) {
                val newFaith = (sanctified.faith - drain.toLong().coerceAtLeast(1L)).coerceAtLeast(0L)
                sanctified = sanctified.copy(faith = newFaith)
                if (newFaith <= 0L) {
                    sanctified = sanctified.copy(aegisActive = false)
                    pushNotification("AEGIS DEPLETED", "Sanctified Aegis extinguished due to zero faith", "combat", "ALERT")
                }
            } else {
                sanctified = sanctified.copy(aegisActive = false)
            }
        }

        // 6. Kill resolution
        if (mHp <= 0.0) {
            val goldGained = m.goldReward
            val xpGained = m.xpReward
            val killsOnFloor = p.killsOnFloor + 1
            lifetime = lifetime.copy(totalKills = lifetime.totalKills + 1)
            p = p.copy(failuresInRow = 0) // Any kill resets failuresInRow

            newLogs.add(LogEntry(ids.next(), "HOSTILE NEUTRALIZED: ${m.name.uppercase()} (+${goldGained} 🪙, +${xpGained} XP)", "gain"))

            // XP loop (supports multi-level up)
            var curXp = p.xp + xpGained
            var curLvl = p.lvl
            var leveledUp = false
            while (true) {
                val xpReq = Balance.playerXpReq(curLvl)
                if (curXp >= xpReq) {
                    curXp -= xpReq
                    curLvl++
                    leveledUp = true
                    newLogs.add(LogEntry(ids.next(), "PLAYER LEVEL UP: Advanced to Level $curLvl!", "milestone"))
                    pushNotification("PLAYER LEVEL UP", "Advanced to Level $curLvl! Health, Shield & Mana replenished.", "level")
                } else {
                    break
                }
            }

            if (leveledUp) {
                // Derived max stats on level up
                val postLvlDerived = DerivedStats.of(state.copy(player = p.copy(lvl = curLvl)))
                pHp = postLvlDerived.maxHp
                pShield = postLvlDerived.maxShield
                pMana = postLvlDerived.maxMana
            }

            // Bone essence to Necromancy (1 to 3)
            val bonesGained = random.nextLong(1, 4)
            necro = necro.copy(boneEssence = necro.boneEssence + bonesGained)

            // Faith on kill (+1, +5 for boss)
            val faithGained = if (m.isBoss) 5L else 1L
            sanctified = sanctified.copy(faith = sanctified.faith + faithGained)

            // Slayer task progress
            if (slayer.currentTask.targetName.equals(m.name, ignoreCase = true) && !slayer.currentTask.isCompleted) {
                val taskKills = slayer.currentTask.currentKills + 1
                if (taskKills >= slayer.currentTask.requiredKills) {
                    slayer = slayer.copy(
                        currentTask = slayer.currentTask.copy(currentKills = taskKills, isCompleted = true),
                        slayerPoints = slayer.slayerPoints + slayer.currentTask.rewardPoints
                    )
                    p = p.copy(gold = p.gold + slayer.currentTask.rewardGold)
                    pushNotification(
                        title = "SLAYER DIRECTIVE COMPLETE",
                        message = "Eliminated ${slayer.currentTask.targetName} (+${slayer.currentTask.rewardGold} 🪙, +${slayer.currentTask.rewardPoints} Points)",
                        type = "milestone"
                    )
                    // Grant Slayer XP
                    var sXp = slayer.xp + 100L
                    var sLvl = slayer.lvl
                    val sReq = Balance.slayerXpReq(sLvl)
                    if (sXp >= sReq) {
                        sXp -= sReq
                        sLvl++
                    }
                    // Roll next task
                    val nextTargetFloor = (p.currentFloor + random.nextInt(-3, 2)).coerceIn(1, 100)
                    val nextMonster = Balance.generateMonsterForFloor(nextTargetFloor)
                    val nextTask = SlayerTask(
                        targetName = nextMonster.name,
                        currentKills = 0,
                        requiredKills = random.nextInt(10, 21),
                        rewardGold = (nextTargetFloor * 25L).coerceAtLeast(100L),
                        rewardPoints = (nextTargetFloor / 5 + 3).coerceIn(3, 15),
                        isCompleted = false
                    )
                    slayer = slayer.copy(lvl = sLvl, xp = sXp, currentTask = nextTask)
                } else {
                    slayer = slayer.copy(
                        currentTask = slayer.currentTask.copy(currentKills = taskKills)
                    )
                }
            }

            // Beast Taming roll (tameChance)
            if (random.nextDouble() < derived.tameChance && beasts.beasts.size < 8) {
                val beastName = "${m.name} Companion"
                val newBeast = Beast(
                    id = "beast_${ids.next()}",
                    name = beastName,
                    rank = 1,
                    stars = 1,
                    dps = (m.damage * 0.6).coerceAtLeast(4.0),
                    hp = m.maxHp * 0.5,
                    maxHp = m.maxHp * 0.5,
                    trait = "Tamed Hunter"
                )
                beasts = beasts.copy(beasts = beasts.beasts + newBeast)
                lifetime = lifetime.copy(beastsTamed = lifetime.beastsTamed + 1)
                newLogs.add(LogEntry(ids.next(), "BEAST TAMED: Secured $beastName into squad!", "loot"))
                pushNotification("BEAST TAMED", "Secured $beastName (${newBeast.dps.toInt()} DPS)", "tame")
            }

            // Loot roll (lootChance, 50/50 ore or herb)
            if (random.nextDouble() < derived.lootChance) {
                val dropRes = if (random.nextBoolean()) "Copper Ore" else "Felweed"
                val existing = inventory.indexOfFirst { it.name == dropRes }
                if (existing >= 0) {
                    inventory[existing] = inventory[existing].copy(count = inventory[existing].count + 1)
                } else {
                    inventory.add(
                        InventoryItem(
                            id = "item_${ids.next()}",
                            name = dropRes,
                            category = "Resource",
                            count = 1,
                            tier = 1,
                            statDesc = "Crafting material",
                            sellValue = 5L
                        )
                    )
                }
                newLogs.add(LogEntry(ids.next(), "SALVAGE ACQUIRED: $dropRes", "loot"))
            }

            // Floor Advancement
            var nextFloor = p.currentFloor
            var nextMaxUnlocked = p.maxUnlockedFloor
            var newKillsOnFloor = killsOnFloor

            if (killsOnFloor >= p.killsRequired) {
                newKillsOnFloor = 0
                if (nextFloor >= nextMaxUnlocked) {
                    nextMaxUnlocked = (nextFloor + 1).coerceAtMost(100)
                    newLogs.add(LogEntry(ids.next(), "SECTOR UNLOCKED: Sector $nextFloor Secured!", "milestone"))
                    pushNotification("SECTOR CLEARED", "Sector $nextFloor neutralized. Access code for Sector $nextMaxUnlocked granted.", "milestone")
                    // Diamonds on boss first clear
                    if (m.isBoss) {
                        p = p.copy(diamonds = p.diamonds + 5L)
                        pushNotification("DIAMONDS EARNED", "+5 Diamonds for boss first clear!", "gain")
                    }
                }
                if (p.autoAdvance) {
                    nextFloor = (nextFloor + 1).coerceAtMost(100)
                }
            }

            // Respawn gap
            respawnLeft = OfflineEngine.SPAWN_GAP_S
            m = Balance.generateMonsterForFloor(nextFloor)

            p = p.copy(
                gold = p.gold + goldGained,
                xp = curXp,
                lvl = curLvl,
                currentFloor = nextFloor,
                maxUnlockedFloor = nextMaxUnlocked,
                killsOnFloor = newKillsOnFloor
            )
        } else {
            m = m.copy(hp = mHp.coerceAtLeast(0.0), shield = mShield.coerceAtLeast(0.0))
        }

        // 7. Hero Defeat Check (§4.5)
        if (pHp <= 0.0) {
            val newFailures = p.failuresInRow + 1
            lifetime = lifetime.copy(defeats = lifetime.defeats + 1)
            newLogs.add(LogEntry(ids.next(), "SYSTEMS REBOOTING: Hull compromised. Initializing emergency stasis.", "combat"))
            pushNotification("TACTICAL RETREAT", "Systems rebooting. Emergency stasis engaged for 6.0s.", "combat", "ALERT")

            var floorToStay = p.currentFloor
            if (newFailures >= 3 && p.autoAdvance && p.currentFloor > 1) {
                floorToStay = p.currentFloor - 1
                newLogs.add(LogEntry(ids.next(), "RETREAT PROTOCOL ENGAGED: Descending to Sector $floorToStay for strategic regroup.", "system"))
                pushNotification("RETREAT PROTOCOL", "Engaged retreat protocol to Sector $floorToStay", "combat", "ALERT")
            }

            p = p.copy(
                hp = 0.0,
                recoverLeft = 6.0,
                killsOnFloor = 0,
                failuresInRow = if (newFailures >= 3) 0 else newFailures,
                currentFloor = floorToStay
            )
            m = Balance.generateMonsterForFloor(floorToStay)
        } else {
            p = p.copy(hp = pHp, shield = pShield, mana = pMana)
        }

        // 8. Gathering Progress Tick (Plots & Veins)
        // Farming/Mining duration scaled by: duration * 0.97^(lvl-1), floored at 40% of base
        val farmSpeedMult = (0.97.pow((gathering.farmingLvl - 1).toDouble())).coerceAtLeast(0.40).toFloat()
        val mineSpeedMult = (0.97.pow((gathering.miningLvl - 1).toDouble())).coerceAtLeast(0.40).toFloat()

        var autoCollectedHerbs = 0
        var autoCollectedOres = 0

        val updatedPlots = gathering.plots.map { plot ->
            val effectiveDuration = plot.durationSeconds * farmSpeedMult
            if (plot.isReady) {
                if (gathering.autoCollectUnlocked && gathering.autoCollectEnabled) {
                    autoCollectedHerbs += 2
                    plot.copy(growthProgress = 0f, isReady = false)
                } else plot
            } else {
                val newProg = plot.growthProgress + (deltaSeconds / effectiveDuration)
                if (newProg >= 1f) {
                    if (gathering.autoCollectUnlocked && gathering.autoCollectEnabled) {
                        autoCollectedHerbs += 2
                        plot.copy(growthProgress = 0f, isReady = false)
                    } else {
                        plot.copy(growthProgress = 1f, isReady = true)
                    }
                } else {
                    plot.copy(growthProgress = newProg)
                }
            }
        }

        val updatedVeins = gathering.veins.map { vein ->
            val effectiveDuration = vein.durationSeconds * mineSpeedMult
            if (vein.isReady) {
                if (gathering.autoCollectUnlocked && gathering.autoCollectEnabled) {
                    autoCollectedOres += 3
                    vein.copy(miningProgress = 0f, isReady = false)
                } else vein
            } else {
                val newProg = vein.miningProgress + (deltaSeconds / effectiveDuration)
                if (newProg >= 1f) {
                    if (gathering.autoCollectUnlocked && gathering.autoCollectEnabled) {
                        autoCollectedOres += 3
                        vein.copy(miningProgress = 0f, isReady = false)
                    } else {
                        vein.copy(miningProgress = 1f, isReady = true)
                    }
                } else {
                    vein.copy(miningProgress = newProg)
                }
            }
        }

        if (autoCollectedHerbs > 0) {
            val idx = inventory.indexOfFirst { it.name == "Felweed" }
            if (idx >= 0) {
                inventory[idx] = inventory[idx].copy(count = inventory[idx].count + autoCollectedHerbs)
            } else {
                inventory.add(InventoryItem("herb_${ids.next()}", "Felweed", "Resource", autoCollectedHerbs, 1, "", "Alchemy herb", 3L))
            }
            lifetime = lifetime.copy(herbsHarvested = lifetime.herbsHarvested + autoCollectedHerbs)
        }

        if (autoCollectedOres > 0) {
            val idx = inventory.indexOfFirst { it.name == "Copper Ore" }
            if (idx >= 0) {
                inventory[idx] = inventory[idx].copy(count = inventory[idx].count + autoCollectedOres)
            } else {
                inventory.add(InventoryItem("ore_${ids.next()}", "Copper Ore", "Resource", autoCollectedOres, 1, "", "Crafting metal ore", 2L))
            }
            lifetime = lifetime.copy(oresMined = lifetime.oresMined + autoCollectedOres)
        }

        // Auto-collect unlock thresholds (level 10 and 25)
        val canAutoCollect = gathering.farmingLvl >= 10 || gathering.miningLvl >= 10
        gathering = gathering.copy(
            plots = updatedPlots,
            veins = updatedVeins,
            autoCollectUnlocked = canAutoCollect
        )

        state = state.copy(
            player = p,
            monster = m,
            necro = necro,
            beasts = beasts,
            gathering = gathering,
            crafting = crafting,
            training = training,
            sanctified = sanctified,
            slayer = slayer,
            inventory = inventory,
            lifetime = lifetime,
            logs = (state.logs + newLogs).takeLast(40)
        )

        evaluateAchievements()
        return state
    }

    private fun evaluateAchievements() {
        val achievements = state.achievements.map { ach ->
            if (ach.isUnlocked) ach
            else {
                val currentProgress = when (ach.id) {
                    "ach_first_blood" -> state.lifetime.totalKills.toInt()
                    "ach_floor_10" -> state.player.maxUnlockedFloor
                    "ach_tamer" -> state.lifetime.beastsTamed.toInt()
                    "ach_necro" -> state.necro.army.sumOf { it.count }
                    "ach_miner" -> state.lifetime.oresMined.toInt()
                    else -> ach.progress
                }
                val unlocked = currentProgress >= ach.target
                if (unlocked && !ach.isUnlocked) {
                    pushNotification(
                        title = "ACHIEVEMENT UNLOCKED",
                        message = "${ach.title}: ${ach.desc}",
                        type = "milestone",
                        status = "SUCCESS"
                    )
                }
                ach.copy(
                    progress = currentProgress.coerceAtMost(ach.target),
                    isUnlocked = unlocked
                )
            }
        }
        state = state.copy(achievements = achievements)
    }

    fun claimAchievement(achId: String): GameState {
        val achs = state.achievements.toMutableList()
        val idx = achs.indexOfFirst { it.id == achId }
        if (idx >= 0 && achs[idx].isUnlocked && !achs[idx].isClaimed) {
            val ach = achs[idx]
            achs[idx] = ach.copy(isClaimed = true)
            state = state.copy(
                achievements = achs,
                player = state.player.copy(gold = state.player.gold + ach.rewardGold)
            )
            pushNotification(
                title = "ACHIEVEMENT CLAIMED",
                message = "Claimed reward of ${ach.rewardGold} 🪙 for ${ach.title}",
                type = "gain"
            )
        }
        return state
    }

    fun navFloor(delta: Int): GameState {
        val target = (state.player.currentFloor + delta).coerceIn(1, state.player.maxUnlockedFloor)
        if (target != state.player.currentFloor) {
            val newMonster = Balance.generateMonsterForFloor(target)
            state = state.copy(
                player = state.player.copy(currentFloor = target, killsOnFloor = 0),
                monster = newMonster
            )
            pushNotification("SECTOR TRANSFER", "Traversing to Sector ${target.toString().padStart(2, '0')}", "system")
        }
        return state
    }

    fun setFloor(floor: Int): GameState {
        val target = floor.coerceIn(1, state.player.maxUnlockedFloor)
        val newMonster = Balance.generateMonsterForFloor(target)
        state = state.copy(
            player = state.player.copy(currentFloor = target, killsOnFloor = 0),
            monster = newMonster
        )
        return state
    }

    fun toggleAutoAdvance(): GameState {
        val next = !state.player.autoAdvance
        state = state.copy(player = state.player.copy(autoAdvance = next))
        pushNotification("AUTO-ADVANCE", if (next) "Auto-advance ENABLED" else "Auto-advance DISABLED", "system")
        return state
    }

    fun toggleAutoCollect(): GameState {
        if (!state.gathering.autoCollectUnlocked) return state
        val next = !state.gathering.autoCollectEnabled
        state = state.copy(gathering = state.gathering.copy(autoCollectEnabled = next))
        pushNotification("AUTO-COLLECT", if (next) "Gathering auto-collect ENABLED" else "Gathering auto-collect DISABLED", "system")
        return state
    }

    fun toggleAutoEquipBestGear(): GameState {
        val next = !state.automation.autoEquipBestGear
        state = state.copy(automation = state.automation.copy(autoEquipBestGear = next))
        if (next) {
            autoEquipBestGear()
        }
        pushNotification("AUTO-EQUIP", if (next) "Auto-equip best gear ENABLED" else "Auto-equip best gear DISABLED", "system")
        return state
    }

    fun toggleAutoMergeBeasts(): GameState {
        val next = !state.automation.autoMergeBeasts
        state = state.copy(automation = state.automation.copy(autoMergeBeasts = next))
        pushNotification("AUTO-MERGE", if (next) "Auto-merge beasts ENABLED" else "Auto-merge beasts DISABLED", "system")
        return state
    }

    fun setCraftMaterialUsage(pct: Int): GameState {
        state = state.copy(automation = state.automation.copy(craftMaterialUsagePct = pct.coerceIn(0, 100)))
        return state
    }

    fun autoEquipBestGear(): GameState {
        val slots = listOf("Weapon", "Armor", "Ring", "Amulet")
        var inv = state.inventory.toMutableList()
        var changed = false
        for (slot in slots) {
            val itemsForSlot = inv.filter { it.category == "Equipment" && it.slot.equals(slot, ignoreCase = true) }
            if (itemsForSlot.isNotEmpty()) {
                val best = itemsForSlot.maxByOrNull { if (slot == "Weapon" || slot == "Ring") it.atk else it.def }
                if (best != null && !best.isEquipped) {
                    inv = inv.map {
                        if (it.slot.equals(slot, ignoreCase = true)) it.copy(isEquipped = (it.id == best.id))
                        else it
                    }.toMutableList()
                    changed = true
                }
            }
        }
        if (changed) {
            state = state.copy(inventory = inv)
            val d = DerivedStats.of(state)
            val weapon = inv.firstOrNull { it.isEquipped && it.slot == "Weapon" }
            val armor = inv.firstOrNull { it.isEquipped && it.slot == "Armor" }
            state = state.copy(
                player = state.player.copy(
                    weaponSlot = weapon?.name ?: state.player.weaponSlot,
                    armorSlot = armor?.name ?: state.player.armorSlot,
                    weaponDmg = d.weaponDmg,
                    armorDef = d.armorDef
                )
            )
        }
        return state
    }

    fun fightPromotionBoss(): GameState {
        val promo = state.promotion
        val d = DerivedStats.of(state)
        val floorOk = state.player.currentFloor >= promo.requiredFloor
        val gearOk = d.gearScore >= promo.requiredGearScore
        if (!floorOk || !gearOk) {
            pushNotification("PROMOTION BLOCKED", "Requirements not yet fulfilled (${state.player.currentFloor}/${promo.requiredFloor} Floor, ${d.gearScore}/${promo.requiredGearScore} GS)", "alert", "FAILED")
            return state
        }
        val nextTier = (promo.tier + 1).coerceAtMost(promo.maxTier)
        val tierNames = listOf(
            "Novice Vanguard", "Ironbound Warden", "Crypt Cleaver", "Aether Sentinel",
            "Dawnbreaker", "Void Sovereign", "Abyssal Juggernaut", "Eclipse Titan",
            "Solar Warlord", "Apex Immortal"
        )
        val nextName = tierNames.getOrElse(nextTier - 1) { "Apex Rank $nextTier" }
        val nextReqFloor = (promo.requiredFloor + 15).coerceAtMost(100)
        val nextReqGear = promo.requiredGearScore + 400
        val nextBonus = promo.bonusPercent + 8

        state = state.copy(
            promotion = promo.copy(
                tier = nextTier,
                tierName = nextName,
                requiredFloor = nextReqFloor,
                requiredGearScore = nextReqGear,
                bonusPercent = nextBonus
            ),
            logs = (state.logs + LogEntry(ids.next(), "PROMOTION ACHIEVED: Ascended to $nextName! Unlocked +${nextBonus}% All Stats Bonus.", "milestone")).takeLast(40)
        )
        pushNotification(
            title = "PROMOTION TRIUMPH",
            message = "Defeated Promotion Boss! Promoted to $nextName (+${nextBonus}% stats).",
            type = "milestone",
            status = "SUCCESS"
        )
        return state
    }

    fun harvestPlot(plotId: Int): GameState {
        val plots = state.gathering.plots.toMutableList()
        val idx = plots.indexOfFirst { it.id == plotId }
        if (idx >= 0 && plots[idx].isReady) {
            val herbName = plots[idx].herbName
            val inv = state.inventory.toMutableList()
            val existing = inv.indexOfFirst { it.name == herbName }
            if (existing >= 0) {
                inv[existing] = inv[existing].copy(count = inv[existing].count + 2)
            } else {
                inv.add(InventoryItem("herb_${ids.next()}", herbName, "Resource", 2, 1, "", "Harvested herb", 8L))
            }
            plots[idx] = plots[idx].copy(growthProgress = 0f, isReady = false)

            var fXp = state.gathering.farmingXp + 25L
            var fLvl = state.gathering.farmingLvl
            val reqXp = Balance.skillXpReq(fLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(ids.next(), "BIO-HARVEST: Collected 2x $herbName (+25 Farming XP)", "loot"))

            if (fXp >= reqXp) {
                fXp -= reqXp
                fLvl++
                newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Farming reached Level $fLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Farming reached Level $fLvl!", "level")
            } else {
                pushNotification("HARVEST COMPLETE", "Harvested 2x $herbName", "craft")
            }

            state = state.copy(
                gathering = state.gathering.copy(plots = plots, farmingXp = fXp, farmingLvl = fLvl),
                inventory = inv,
                lifetime = state.lifetime.copy(herbsHarvested = state.lifetime.herbsHarvested + 2),
                logs = newLogs.takeLast(40)
            )
            evaluateAchievements()
        }
        return state
    }

    fun mineVein(veinId: Int): GameState {
        val veins = state.gathering.veins.toMutableList()
        val idx = veins.indexOfFirst { it.id == veinId }
        if (idx >= 0 && veins[idx].isReady) {
            val oreName = veins[idx].oreName
            val inv = state.inventory.toMutableList()
            val existing = inv.indexOfFirst { it.name == oreName }
            if (existing >= 0) {
                inv[existing] = inv[existing].copy(count = inv[existing].count + 3)
            } else {
                inv.add(InventoryItem("ore_${ids.next()}", oreName, "Resource", 3, 1, "", "Mined metal ore", 10L))
            }
            veins[idx] = veins[idx].copy(miningProgress = 0f, isReady = false)

            var mXp = state.gathering.miningXp + 30L
            var mLvl = state.gathering.miningLvl
            val reqXp = Balance.skillXpReq(mLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(ids.next(), "ORE EXTRACTED: Excavated 3x $oreName (+30 Mining XP)", "loot"))

            if (mXp >= reqXp) {
                mXp -= reqXp
                mLvl++
                newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Mining reached Level $mLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Mining reached Level $mLvl!", "level")
            } else {
                pushNotification("EXTRACTION COMPLETE", "Extracted 3x $oreName", "craft")
            }

            state = state.copy(
                gathering = state.gathering.copy(veins = veins, miningXp = mXp, miningLvl = mLvl),
                inventory = inv,
                lifetime = state.lifetime.copy(oresMined = state.lifetime.oresMined + 3),
                logs = newLogs.takeLast(40)
            )
            evaluateAchievements()
        }
        return state
    }

    fun summonMinion(type: String): GameState {
        val cost = when (type) {
            "Bone Mage" -> 25L
            "Dread Knight" -> 50L
            "Bone Lich" -> 100L
            else -> 10L
        }
        if (state.necro.boneEssence >= cost) {
            val army = state.necro.army.toMutableList()
            val existing = army.indexOfFirst { it.type == type }
            if (existing >= 0) {
                army[existing] = army[existing].copy(count = army[existing].count + 1)
            } else {
                val dps = when (type) {
                    "Bone Mage" -> 8.0
                    "Dread Knight" -> 16.0
                    "Bone Lich" -> 35.0
                    else -> 4.0
                }
                army.add(Minion("minion_${ids.next()}", type, 1, 1, dps, 15.0))
            }

            var nXp = state.necro.xp + 20L
            var nLvl = state.necro.lvl
            val req = Balance.skillXpReq(nLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(ids.next(), "NECRO ANIMATION: Reanimated $type into Legion!", "system"))

            if (nXp >= req) {
                nXp -= req
                nLvl++
                newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Necromancy reached Level $nLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Necromancy reached Level $nLvl!", "level")
            } else {
                pushNotification("REANIMATION COMPLETE", "Animated $type ($cost 💀 consumed)", "craft")
            }

            state = state.copy(
                necro = state.necro.copy(
                    boneEssence = state.necro.boneEssence - cost,
                    army = army,
                    xp = nXp,
                    lvl = nLvl
                ),
                logs = newLogs.takeLast(40)
            )
            evaluateAchievements()
        }
        return state
    }

    fun mergeBeasts(beastId1: String, beastId2: String): GameState {
        val beasts = state.beasts.beasts.toMutableList()
        val b1 = beasts.find { it.id == beastId1 }
        val b2 = beasts.find { it.id == beastId2 }
        if (b1 != null && b2 != null && b1.name == b2.name && b1.stars == b2.stars) {
            beasts.remove(b1)
            beasts.remove(b2)
            val upgraded = b1.copy(
                stars = b1.stars + 1,
                dps = b1.dps * 1.6,
                maxHp = b1.maxHp * 1.5,
                hp = b1.maxHp * 1.5
            )
            beasts.add(upgraded)

            var bXp = state.beasts.xp + 60L
            var bLvl = state.beasts.lvl
            val req = Balance.skillXpReq(bLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(ids.next(), "BEAST SYNTHESIS: Upgraded ${upgraded.name} to ⭐${upgraded.stars}!", "milestone"))

            if (bXp >= req) {
                bXp -= req
                bLvl++
                newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Beast Taming reached Level $bLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Beast Taming reached Level $bLvl!", "level")
            } else {
                pushNotification("BEAST MERGE SUCCESS", "${upgraded.name} upgraded to ⭐${upgraded.stars}", "tame")
            }

            state = state.copy(
                beasts = state.beasts.copy(beasts = beasts, xp = bXp, lvl = bLvl),
                logs = newLogs.takeLast(40)
            )
        }
        return state
    }

    fun trainNode(nodeId: String): GameState {
        val nodes = state.training.nodes.toMutableList()
        val idx = nodes.indexOfFirst { it.id == nodeId }
        if (idx >= 0) {
            val node = nodes[idx]
            if (state.player.gold >= node.costGold && node.rank < node.maxRank) {
                nodes[idx] = node.copy(rank = node.rank + 1, costGold = (node.costGold * 1.4).toLong())
                val p = state.player.copy(gold = state.player.gold - node.costGold)

                var tXp = state.training.xp + 40L
                var tLvl = state.training.lvl
                val req = Balance.skillXpReq(tLvl)
                val newLogs = state.logs.toMutableList()
                newLogs.add(LogEntry(ids.next(), "DOJO ENHANCEMENT: ${node.name} upgraded to Rank ${node.rank + 1}!", "system"))

                if (tXp >= req) {
                    tXp -= req
                    tLvl++
                    newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Training reached Level $tLvl!", "milestone"))
                    pushNotification("SKILL LEVEL UP", "Training reached Level $tLvl!", "level")
                } else {
                    pushNotification("ENHANCEMENT COMPLETE", "${node.name} Rank ${node.rank + 1} acquired", "gear")
                }

                state = state.copy(
                    player = p,
                    training = state.training.copy(nodes = nodes, xp = tXp, lvl = tLvl),
                    logs = newLogs.takeLast(40)
                )
            }
        }
        return state
    }

    fun equipItem(itemId: String): GameState {
        val inv = state.inventory.toMutableList()
        val idx = inv.indexOfFirst { it.id == itemId }
        if (idx >= 0) {
            val item = inv[idx]
            if (item.category == "Equipment") {
                var p = state.player
                if (item.slot == "Weapon") {
                    val prevWeapon = p.weaponSlot
                    for (i in inv.indices) {
                        if (inv[i].slot == "Weapon" && inv[i].isEquipped) {
                            inv[i] = inv[i].copy(isEquipped = false)
                        }
                    }
                    inv[idx] = item.copy(isEquipped = true)
                    val atkVal = if (item.atk > 0.0) item.atk else item.tier * 12.0
                    p = p.copy(weaponSlot = item.name, weaponDmg = atkVal)
                    pushNotification("EQUIP: WEAPON", "Equipped ${item.name}", "gear", "SUCCESS", "+${atkVal.toInt()} ATK | Replaced $prevWeapon")
                    val newLogs = state.logs + LogEntry(ids.next(), "GEAR EQUIPPED: ${item.name.uppercase()} assigned to Weapon Slot", "system")
                    state = state.copy(player = p, inventory = inv, logs = newLogs.takeLast(40))
                } else if (item.slot == "Armor") {
                    val prevArmor = p.armorSlot
                    for (i in inv.indices) {
                        if (inv[i].slot == "Armor" && inv[i].isEquipped) {
                            inv[i] = inv[i].copy(isEquipped = false)
                        }
                    }
                    inv[idx] = item.copy(isEquipped = true)
                    val defVal = if (item.def > 0.0) item.def else item.tier * 8.0
                    p = p.copy(armorSlot = item.name, armorDef = defVal)
                    pushNotification("EQUIP: ARMOR", "Equipped ${item.name}", "gear", "SUCCESS", "+${defVal.toInt()} DEF | Replaced $prevArmor")
                    val newLogs = state.logs + LogEntry(ids.next(), "GEAR EQUIPPED: ${item.name.uppercase()} assigned to Armor Slot", "system")
                    state = state.copy(player = p, inventory = inv, logs = newLogs.takeLast(40))
                }
            } else if (item.category == "Potion") {
                usePotion(itemId)
            }
        }
        return state
    }

    fun unequipItem(itemId: String): GameState {
        val inv = state.inventory.toMutableList()
        val idx = inv.indexOfFirst { it.id == itemId }
        if (idx >= 0 && inv[idx].isEquipped) {
            val item = inv[idx]
            inv[idx] = item.copy(isEquipped = false)
            var p = state.player
            if (item.slot == "Weapon") {
                p = p.copy(weaponSlot = "Bare Hands", weaponDmg = 2.0)
            } else if (item.slot == "Armor") {
                p = p.copy(armorSlot = "None", armorDef = 0.0)
            }
            pushNotification("UNEQUIPPED", "Removed ${item.name} from ${item.slot} slot", "gear")
            val newLogs = state.logs + LogEntry(ids.next(), "GEAR UNEQUIPPED: ${item.name.uppercase()} unequipped", "system")
            state = state.copy(player = p, inventory = inv, logs = newLogs.takeLast(40))
        }
        return state
    }

    fun usePotion(itemId: String): GameState {
        val inv = state.inventory.toMutableList()
        val idx = inv.indexOfFirst { it.id == itemId }
        if (idx >= 0) {
            val item = inv[idx]
            val derived = DerivedStats.of(state)
            val healAmount = when (item.name) {
                "Minor Health Flask", "Lesser Healing Potion" -> 50.0
                "Bloodroot Tonic" -> 80.0
                "Moonlight Philter" -> 150.0
                else -> 75.0
            }
            val healed = (state.player.hp + healAmount).coerceAtMost(derived.maxHp)
            if (item.count > 1) {
                inv[idx] = item.copy(count = item.count - 1)
            } else {
                inv.removeAt(idx)
            }
            pushNotification("POTION DEPLOYED", "Restored ${healAmount.toInt()} HP", "gain")
            val newLogs = state.logs + LogEntry(ids.next(), "POTION DEPLOYED: Restored ${healAmount.toInt()} HP", "gain")
            state = state.copy(
                player = state.player.copy(hp = healed),
                inventory = inv,
                logs = newLogs.takeLast(40)
            )
        }
        return state
    }

    fun sellItem(itemId: String, count: Int = 1): GameState {
        val inv = state.inventory.toMutableList()
        val idx = inv.indexOfFirst { it.id == itemId }
        if (idx >= 0) {
            val item = inv[idx]
            val actualCount = count.coerceIn(1, item.count)
            val goldEarned = actualCount * item.sellValue
            if (item.count > actualCount) {
                inv[idx] = item.copy(count = item.count - actualCount)
            } else {
                inv.removeAt(idx)
            }
            state = state.copy(
                player = state.player.copy(gold = state.player.gold + goldEarned),
                inventory = inv
            )
            pushNotification("ITEM SOLD", "Sold $actualCount x ${item.name} for +$goldEarned 🪙", "gain")
        }
        return state
    }

    fun craftEquipment(name: String, tier: Int, slot: String, reqOre: String, reqQty: Int): GameState {
        val inv = state.inventory.toMutableList()
        val oreIdx = inv.indexOfFirst { it.name == reqOre && it.count >= reqQty }
        if (oreIdx >= 0) {
            val ore = inv[oreIdx]
            if (ore.count > reqQty) {
                inv[oreIdx] = ore.copy(count = ore.count - reqQty)
            } else {
                inv.removeAt(oreIdx)
            }
            val (atk, def, statDesc) = if (slot == "Weapon") {
                val atkVal = tier * 12.0
                Triple(atkVal, 0.0, "+${atkVal.toInt()} Damage")
            } else {
                val defVal = tier * 8.0
                Triple(0.0, defVal, "+${defVal.toInt()} Defense")
            }
            val newItem = InventoryItem(
                id = "craft_${ids.next()}",
                name = name,
                category = "Equipment",
                count = 1,
                tier = tier,
                slot = slot,
                statDesc = statDesc,
                sellValue = tier * 35L,
                isEquipped = false,
                atk = atk,
                def = def
            )
            inv.add(newItem)

            var sXp = state.crafting.smithingXp + 50L
            var sLvl = state.crafting.smithingLvl
            val req = Balance.skillXpReq(sLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(ids.next(), "CYBER FORGE SUCCESS: Forged $name (Tier $tier)!", "milestone"))

            if (sXp >= req) {
                sXp -= req
                sLvl++
                newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Smithing reached Level $sLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Smithing reached Level $sLvl!", "level")
            } else {
                pushNotification("FORGE SUCCESS", "Forged $name ($statDesc)", "craft")
            }

            state = state.copy(
                inventory = inv,
                crafting = state.crafting.copy(smithingXp = sXp, smithingLvl = sLvl),
                lifetime = state.lifetime.copy(forged = state.lifetime.forged + 1),
                logs = newLogs.takeLast(40)
            )
        } else {
            val have = inv.find { it.name == reqOre }?.count ?: 0
            pushNotification("FORGE FAILED", "REQUIRES ${reqQty}x $reqOre (HAVE $have)", "combat", "FAILED")
        }
        return state
    }

    fun brewPotion(name: String, reqHerb: String, reqQty: Int): GameState {
        val inv = state.inventory.toMutableList()
        val herbIdx = inv.indexOfFirst { it.name == reqHerb && it.count >= reqQty }
        if (herbIdx >= 0) {
            val herb = inv[herbIdx]
            if (herb.count > reqQty) {
                inv[herbIdx] = herb.copy(count = herb.count - reqQty)
            } else {
                inv.removeAt(herbIdx)
            }
            val healDesc = when (name) {
                "Minor Health Flask" -> "Restores 50 HP"
                "Bloodroot Tonic" -> "Restores 80 HP"
                "Moonlight Philter" -> "Restores 150 HP"
                else -> "Restores 75 HP"
            }
            inv.add(InventoryItem("potion_${ids.next()}", name, "Potion", 1, 1, "", healDesc, 15L))

            var hXp = state.crafting.herbloreXp + 40L
            var hLvl = state.crafting.herbloreLvl
            val req = Balance.skillXpReq(hLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(ids.next(), "ALCHEMY SYNTHESIS: Brewed $name!", "milestone"))

            if (hXp >= req) {
                hXp -= req
                hLvl++
                newLogs.add(LogEntry(ids.next(), "SKILL LEVEL UP: Herblore reached Level $hLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Herblore reached Level $hLvl!", "level")
            } else {
                pushNotification("ALCHEMY SUCCESS", "Brewed $name successfully", "craft")
            }

            state = state.copy(
                inventory = inv,
                crafting = state.crafting.copy(herbloreXp = hXp, herbloreLvl = hLvl),
                lifetime = state.lifetime.copy(brewed = state.lifetime.brewed + 1),
                logs = newLogs.takeLast(40)
            )
        } else {
            val have = inv.find { it.name == reqHerb }?.count ?: 0
            pushNotification("ALCHEMY FAILED", "REQUIRES ${reqQty}x $reqHerb (HAVE $have)", "combat", "FAILED")
        }
        return state
    }

    fun toggleSanctifiedAegis(): GameState {
        val next = !state.sanctified.aegisActive
        if (next && state.sanctified.faith <= 0) {
            pushNotification("SANCTIFIED AEGIS", "Insufficient faith to activate Aegis (0 Faith)", "combat", "ALERT")
            return state
        }
        state = state.copy(sanctified = state.sanctified.copy(aegisActive = next))
        pushNotification("SANCTIFIED AEGIS", if (next) "Aegis activated (-20% damage, -0.5 faith/s)" else "Aegis deactivated", "system")
        return state
    }

    fun upgradeSanctifiedSmite(): GameState {
        val cost = (25.0 * 1.5.pow(state.sanctified.smiteLevel.toDouble())).toLong()
        if (state.sanctified.faith >= cost) {
            val nextLvl = state.sanctified.smiteLevel + 1
            state = state.copy(
                sanctified = state.sanctified.copy(
                    faith = state.sanctified.faith - cost,
                    smiteLevel = nextLvl
                )
            )
            pushNotification("SMITE UPGRADE", "Holy Smite upgraded to Level $nextLvl (+${nextLvl * 3} DPS)", "milestone")
        } else {
            pushNotification("UPGRADE FAILED", "Requires $cost Faith (Have ${state.sanctified.faith})", "combat", "ALERT")
        }
        return state
    }

    fun rerollSlayerTask(): GameState {
        if (state.slayer.slayerPoints >= 1) {
            val nextTargetFloor = (state.player.currentFloor + random.nextInt(-3, 2)).coerceIn(1, 100)
            val nextMonster = Balance.generateMonsterForFloor(nextTargetFloor)
            val nextTask = SlayerTask(
                targetName = nextMonster.name,
                currentKills = 0,
                requiredKills = random.nextInt(10, 21),
                rewardGold = (nextTargetFloor * 25L).coerceAtLeast(100L),
                rewardPoints = (nextTargetFloor / 5 + 3).coerceIn(3, 15),
                isCompleted = false
            )
            state = state.copy(
                slayer = state.slayer.copy(
                    slayerPoints = state.slayer.slayerPoints - 1,
                    currentTask = nextTask
                )
            )
            pushNotification("SLAYER REROLL", "Contract rerolled to target ${nextTask.targetName}", "system")
        }
        return state
    }

    fun clearLogs(): GameState {
        state = state.copy(logs = emptyList())
        return state
    }

    fun exportSaveJson(): String {
        return jsonSerializer.encodeToString(state)
    }

    fun importSaveJson(jsonString: String): Boolean {
        return try {
            val imported = jsonSerializer.decodeFromString<GameState>(jsonString)
            state = imported.copy(notifications = emptyList(), pendingOfflineLoot = null)
            ids.reset(state.meta.nextId)
            pushNotification("SAVE RESTORED", "Game state successfully imported from archive", "system")
            true
        } catch (e: Exception) {
            pushNotification("IMPORT ERROR", "Invalid save format: ${e.localizedMessage ?: "Parse failure"}", "combat")
            false
        }
    }

    fun resetGame(): GameState {
        state = GameState()
        ids.reset(1L)
        pushNotification("SYSTEM RESET", "Game state reset to initial factory protocol", "system")
        return state
    }
}
