package com.aistudio.trueidle.engine

import com.aistudio.trueidle.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class GameEngine(initialState: GameState = GameState()) {

    var state: GameState = initialState
        private set

    private var logCounter = 100L
    private var notifCounter = 1000L

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    init {
        checkOfflineProgress()
    }

    private fun checkOfflineProgress() {
        val now = System.currentTimeMillis()
        val diffMs = now - state.lastSaveTimestamp
        // If more than 30 seconds have passed, simulate offline gains
        if (diffMs > 30_000L) {
            val seconds = (diffMs / 1000L).coerceAtMost(86400L) // Cap at 24 hours
            val playerDps = (state.player.stats.str * 1.2 + state.player.weaponDmg) * state.player.stats.atkSpeed
            val beastDps = state.beasts.beasts.sumOf { it.dps * it.rank }
            val armyDps = state.necro.army.sumOf { it.dps * it.count }
            val totalDps = (playerDps + beastDps + armyDps).coerceAtLeast(10.0)

            val timePerKill = (state.monster.maxHp / totalDps).coerceAtLeast(3.0)
            val estKills = (seconds / timePerKill).toLong().coerceAtLeast(1L)
            val estGold = estKills * state.monster.goldReward
            val estXp = estKills * state.monster.xpReward
            val estBones = estKills * 2L

            val oreDrops = (seconds / 60L).toInt().coerceAtLeast(1)
            val herbDrops = (seconds / 45L).toInt().coerceAtLeast(1)

            val offline = OfflineLoot(
                timeAwaySeconds = seconds,
                kills = estKills,
                goldEarned = estGold,
                xpEarned = estXp,
                bonesEarned = estBones,
                dropsSummary = listOf("$oreDrops Copper Ore", "$herbDrops Felweed")
            )

            state = state.copy(pendingOfflineLoot = offline, lastSaveTimestamp = now)
        }
    }

    fun claimOfflineLoot(): GameState {
        val loot = state.pendingOfflineLoot ?: return state
        val inv = state.inventory.toMutableList()

        // Add ore & herb
        val oreIdx = inv.indexOfFirst { it.name == "Copper Ore" }
        if (oreIdx >= 0) {
            inv[oreIdx] = inv[oreIdx].copy(count = inv[oreIdx].count + 5)
        } else {
            inv.add(InventoryItem("item_${System.currentTimeMillis()}_ore", "Copper Ore", "Resource", 5, 1, "", "Crafting metal ore", 2L))
        }

        val herbIdx = inv.indexOfFirst { it.name == "Felweed" }
        if (herbIdx >= 0) {
            inv[herbIdx] = inv[herbIdx].copy(count = inv[herbIdx].count + 5)
        } else {
            inv.add(InventoryItem("item_${System.currentTimeMillis()}_herb", "Felweed", "Resource", 5, 1, "", "Alchemy herb", 3L))
        }

        var curXp = state.player.xp + loot.xpEarned
        var curLvl = state.player.lvl
        val reqXp = Balance.playerXpReq(curLvl)
        if (curXp >= reqXp) {
            curXp -= reqXp
            curLvl++
        }

        val newLogs = state.logs + LogEntry(
            id = logCounter++,
            text = "OFFLINE REPORT: Recovered ${loot.goldEarned} 🪙, ${loot.xpEarned} XP, ${loot.kills} kills.",
            type = "gain"
        )

        pushNotification(
            title = "OFFLINE GAINS CLAIMED",
            message = "Added ${loot.goldEarned} 🪙, ${loot.xpEarned} XP, and ${loot.bonesEarned} 💀 to reserves.",
            type = "gain"
        )

        state = state.copy(
            player = state.player.copy(
                gold = state.player.gold + loot.goldEarned,
                xp = curXp,
                lvl = curLvl
            ),
            necro = state.necro.copy(boneEssence = state.necro.boneEssence + loot.bonesEarned),
            inventory = inv,
            pendingOfflineLoot = null,
            logs = newLogs.takeLast(40)
        )
        return state
    }

    fun pushNotification(title: String, message: String, type: String = "info") {
        val currentList = state.notifications.toMutableList()
        // Anti-stacking: check if identical notification occurred in recent queue
        val existingIndex = currentList.indexOfFirst { it.title == title && it.message == message }
        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(
                count = existing.count + 1,
                timestamp = System.currentTimeMillis()
            )
        } else {
            currentList.add(
                GameNotification(
                    id = notifCounter++,
                    title = title,
                    message = message,
                    type = type,
                    count = 1,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        // Retain only latest 3 notifications to prevent clutter
        state = state.copy(notifications = currentList.takeLast(3))
    }

    fun dismissNotification(id: Long): GameState {
        state = state.copy(notifications = state.notifications.filterNot { it.id == id })
        return state
    }

    fun tick(deltaSeconds: Float): GameState {
        var p = state.player
        var m = state.monster
        var necro = state.necro
        var beasts = state.beasts
        var gathering = state.gathering
        var crafting = state.crafting
        var training = state.training
        var slayer = state.slayer
        var inventory = state.inventory.toMutableList()
        val newLogs = mutableListOf<LogEntry>()

        // 1. Calculate Player + Army DPS
        val strBonus = p.stats.str * 1.2
        val weaponDmg = p.weaponDmg
        val sanctifiedBonus = if (state.sanctified.aegisActive) state.sanctified.smiteLevel * 3.0 else 0.0
        val playerDamagePerSec = (strBonus + weaponDmg + sanctifiedBonus) * p.stats.atkSpeed
        val beastDamagePerSec = beasts.beasts.sumOf { it.dps * it.rank * (1.0 + (it.stars - 1) * 0.25) }
        val minionDamagePerSec = necro.army.sumOf { it.dps * it.count * (1.0 + (it.lvl - 1) * 0.2) }

        val totalDps = playerDamagePerSec + beastDamagePerSec + minionDamagePerSec
        val damageDealtThisTick = totalDps * deltaSeconds

        // 2. Apply Damage to Monster
        var remainingMonsterDmg = damageDealtThisTick
        var mShield = m.shield
        var mHp = m.hp

        if (mShield > 0) {
            val shieldDmg = remainingMonsterDmg.coerceAtMost(mShield)
            mShield -= shieldDmg
            remainingMonsterDmg -= shieldDmg
        }
        mHp -= remainingMonsterDmg

        // 3. Monster Attacks Player
        val monsterDmgThisTick = m.damage * deltaSeconds
        var pShield = p.shield
        var pHp = p.hp
        var pMana = (p.mana + (2.0 * deltaSeconds)).coerceAtMost(p.maxMana)

        if (pShield > 0) {
            val absorb = monsterDmgThisTick.coerceAtMost(pShield)
            pShield -= absorb
            val bleed = monsterDmgThisTick - absorb
            pHp -= (bleed - p.armorDef * 0.2).coerceAtLeast(1.0 * deltaSeconds)
        } else {
            pHp -= (monsterDmgThisTick - p.armorDef * 0.3).coerceAtLeast(1.0 * deltaSeconds)
        }

        // 4. Check Monster Death
        if (mHp <= 0) {
            val goldGained = m.goldReward
            val xpGained = m.xpReward
            val killsOnFloor = p.killsOnFloor + 1
            val totalKills = state.totalKills + 1
            newLogs.add(LogEntry(logCounter++, "HOSTILE NEUTRALIZED: ${m.name.uppercase()} (+${goldGained} 🪙, +${xpGained} XP)", "gain"))

            // Player XP & Level
            var curXp = p.xp + xpGained
            var curLvl = p.lvl
            val xpReq = Balance.playerXpReq(curLvl)
            if (curXp >= xpReq) {
                curXp -= xpReq
                curLvl++
                newLogs.add(LogEntry(logCounter++, "PLAYER LEVEL UP: Advanced to Level $curLvl!", "milestone"))
                pushNotification("PLAYER LEVEL UP", "You reached Level $curLvl! Health & Mana replenished.", "level")
            }

            // Bone essence to Necromancy
            val bonesGained = Random.nextLong(1, 4)
            necro = necro.copy(boneEssence = necro.boneEssence + bonesGained)

            // Beast Taming Chance (15%)
            if (Random.nextFloat() < 0.15f && beasts.beasts.size < 8) {
                val beastName = "${m.name} Companion"
                val newBeast = Beast(
                    id = "beast_${System.currentTimeMillis()}_${Random.nextInt(1000)}",
                    name = beastName,
                    rank = 1,
                    stars = 1,
                    dps = (m.damage * 0.6).coerceAtLeast(4.0),
                    hp = m.maxHp * 0.5,
                    maxHp = m.maxHp * 0.5,
                    trait = "Tamed Hunter"
                )
                beasts = beasts.copy(
                    beasts = beasts.beasts + newBeast
                )
                // Gain Beast Taming XP
                beasts = gainBeastTamingXpInternal(beasts, 25L, newLogs)
                newLogs.add(LogEntry(logCounter++, "BEAST TAMED: Captured $beastName into roster!", "loot"))
                pushNotification("BEAST TAMED", "Secured $beastName (${newBeast.dps.toInt()} DPS)", "tame")
            }

            // Loot Drop Chance
            if (Random.nextFloat() < 0.35f) {
                val dropRes = if (Random.nextBoolean()) "Copper Ore" else "Felweed"
                val existing = inventory.indexOfFirst { it.name == dropRes }
                if (existing >= 0) {
                    val item = inventory[existing]
                    inventory[existing] = item.copy(count = item.count + 1)
                } else {
                    inventory.add(InventoryItem("item_${System.currentTimeMillis()}", dropRes, "Resource", 1, 1, "", "Crafting material", 5L))
                }
                newLogs.add(LogEntry(logCounter++, "SALVAGE ACQUIRED: $dropRes", "loot"))
            }

            // Floor Advancement
            var nextFloor = p.currentFloor
            var nextMaxUnlocked = p.maxUnlockedFloor
            var newKillsOnFloor = killsOnFloor

            if (killsOnFloor >= p.killsRequired) {
                newKillsOnFloor = 0
                if (nextFloor >= nextMaxUnlocked) {
                    nextMaxUnlocked = (nextFloor + 1).coerceAtMost(100)
                    newLogs.add(LogEntry(logCounter++, "SECTOR UNLOCKED: Cleared Sector $nextFloor!", "milestone"))
                    pushNotification("SECTOR CLEARED", "Sector $nextFloor neutralized. Next sector available.", "milestone")
                }
                if (p.autoAdvance) {
                    nextFloor = (nextFloor + 1).coerceAtMost(100)
                }
            }

            // Respawn Monster
            m = Balance.generateMonsterForFloor(nextFloor)

            p = p.copy(
                gold = p.gold + goldGained,
                xp = curXp,
                lvl = curLvl,
                hp = p.maxHp,
                shield = p.maxShield,
                currentFloor = nextFloor,
                maxUnlockedFloor = nextMaxUnlocked,
                killsOnFloor = newKillsOnFloor
            )
        } else {
            m = m.copy(hp = mHp.coerceAtLeast(0.0), shield = mShield.coerceAtLeast(0.0))
        }

        // 5. Check Player Defeat
        if (pHp <= 0) {
            newLogs.add(LogEntry(logCounter++, "TACTICAL RETREAT: Hero neutralized by ${m.name.uppercase()}. Respawning...", "combat"))
            pushNotification("TACTICAL RETREAT", "Hero defeated by ${m.name}. Resetting sector.", "combat")
            pHp = p.maxHp
            pShield = p.maxShield
            m = Balance.generateMonsterForFloor(p.currentFloor)
            p = p.copy(killsOnFloor = 0)
        }

        p = p.copy(hp = pHp, shield = pShield, mana = pMana)

        // 6. Gathering Progress Tick (Plots & Veins)
        val updatedPlots = gathering.plots.map { plot ->
            if (plot.isReady) plot
            else {
                val newProg = plot.growthProgress + (deltaSeconds / plot.durationSeconds)
                if (newProg >= 1f) plot.copy(growthProgress = 1f, isReady = true)
                else plot.copy(growthProgress = newProg)
            }
        }
        val updatedVeins = gathering.veins.map { vein ->
            if (vein.isReady) vein
            else {
                val newProg = vein.miningProgress + (deltaSeconds / vein.durationSeconds)
                if (newProg >= 1f) vein.copy(miningProgress = 1f, isReady = true)
                else vein.copy(miningProgress = newProg)
            }
        }
        gathering = gathering.copy(plots = updatedPlots, veins = updatedVeins)

        // Keep last 40 logs
        val mergedLogs = (state.logs + newLogs).takeLast(40)

        state = state.copy(
            player = p,
            monster = m,
            necro = necro,
            beasts = beasts,
            gathering = gathering,
            crafting = crafting,
            training = training,
            slayer = slayer,
            inventory = inventory,
            logs = mergedLogs,
            lastSaveTimestamp = System.currentTimeMillis()
        )
        return state
    }

    private fun gainBeastTamingXpInternal(beasts: BeastTamingState, amount: Long, logs: MutableList<LogEntry>): BeastTamingState {
        var xp = beasts.xp + amount
        var lvl = beasts.lvl
        val req = Balance.skillXpReq(lvl)
        if (xp >= req) {
            xp -= req
            lvl++
            logs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Beast Taming reached Level $lvl!", "milestone"))
            pushNotification("SKILL LEVEL UP", "Beast Taming reached Level $lvl!", "level")
        }
        return beasts.copy(lvl = lvl, xp = xp)
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
                inv.add(InventoryItem("herb_${System.currentTimeMillis()}", herbName, "Resource", 2, 1, "", "Harvested herb", 8L))
            }
            plots[idx] = plots[idx].copy(growthProgress = 0f, isReady = false)

            // Farming XP & Level up check
            var fXp = state.gathering.farmingXp + 25L
            var fLvl = state.gathering.farmingLvl
            val reqXp = Balance.skillXpReq(fLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(logCounter++, "BIO-HARVEST: Collected 2x $herbName (+25 Farming XP)", "loot"))

            if (fXp >= reqXp) {
                fXp -= reqXp
                fLvl++
                newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Farming reached Level $fLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Farming reached Level $fLvl! Faster crop synthesis.", "level")
            } else {
                pushNotification("HARVEST COMPLETE", "Harvested 2x $herbName", "craft")
            }

            state = state.copy(
                gathering = state.gathering.copy(plots = plots, farmingXp = fXp, farmingLvl = fLvl),
                inventory = inv,
                logs = newLogs.takeLast(40)
            )
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
                inv.add(InventoryItem("ore_${System.currentTimeMillis()}", oreName, "Resource", 3, 1, "", "Mined metal ore", 10L))
            }
            veins[idx] = veins[idx].copy(miningProgress = 0f, isReady = false)

            // Mining XP & Level up check
            var mXp = state.gathering.miningXp + 30L
            var mLvl = state.gathering.miningLvl
            val reqXp = Balance.skillXpReq(mLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(logCounter++, "ORE EXTRACTED: Excavated 3x $oreName (+30 Mining XP)", "loot"))

            if (mXp >= reqXp) {
                mXp -= reqXp
                mLvl++
                newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Mining reached Level $mLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Mining reached Level $mLvl! Unlocked deeper veins.", "level")
            } else {
                pushNotification("EXTRACTION COMPLETE", "Extracted 3x $oreName", "craft")
            }

            state = state.copy(
                gathering = state.gathering.copy(veins = veins, miningXp = mXp, miningLvl = mLvl),
                inventory = inv,
                logs = newLogs.takeLast(40)
            )
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
                army.add(Minion("minion_${System.currentTimeMillis()}", type, 1, 1, dps, 15.0))
            }

            // Necro XP & Level up
            var nXp = state.necro.xp + 20L
            var nLvl = state.necro.lvl
            val req = Balance.skillXpReq(nLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(logCounter++, "NECRO ANIMATION: Reanimated $type into Legion!", "system"))

            if (nXp >= req) {
                nXp -= req
                nLvl++
                newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Necromancy reached Level $nLvl!", "milestone"))
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
            newLogs.add(LogEntry(logCounter++, "BEAST SYNTHESIS: Upgraded ${upgraded.name} to ⭐${upgraded.stars}!", "milestone"))

            if (bXp >= req) {
                bXp -= req
                bLvl++
                newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Beast Taming reached Level $bLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Beast Taming reached Level $bLvl!", "level")
            } else {
                pushNotification("BEAST MERGE SUCCESS", "${upgraded.name} upgraded to ⭐${upgraded.stars} (+${upgraded.dps.toInt()} DPS)", "tame")
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
                var p = state.player.copy(gold = state.player.gold - node.costGold)
                when (node.id) {
                    "str_bonus" -> p = p.copy(stats = p.stats.copy(str = p.stats.str + 5.0))
                    "vit_bonus" -> p = p.copy(maxHp = p.maxHp + 50.0, hp = p.hp + 50.0)
                    "int_bonus" -> p = p.copy(maxMana = p.maxMana + 25.0, mana = p.mana + 25.0)
                    "speed_bonus" -> p = p.copy(stats = p.stats.copy(atkSpeed = p.stats.atkSpeed * 1.05))
                    "luck_bonus" -> p = p.copy(stats = p.stats.copy(luck = p.stats.luck + 5.0))
                }

                var tXp = state.training.xp + 40L
                var tLvl = state.training.lvl
                val req = Balance.skillXpReq(tLvl)
                val newLogs = state.logs.toMutableList()
                newLogs.add(LogEntry(logCounter++, "DOJO ENHANCEMENT: ${node.name} upgraded to Rank ${node.rank + 1}!", "system"))

                if (tXp >= req) {
                    tXp -= req
                    tLvl++
                    newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Training reached Level $tLvl!", "milestone"))
                    pushNotification("SKILL LEVEL UP", "Training reached Level $tLvl!", "level")
                } else {
                    pushNotification("ENHANCEMENT COMPLETE", "${node.name} Rank ${node.rank + 1} acquired (${node.stat})", "gear")
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
                    // Unequip any existing weapon
                    for (i in inv.indices) {
                        if (inv[i].slot == "Weapon" && inv[i].isEquipped) {
                            inv[i] = inv[i].copy(isEquipped = false)
                        }
                    }
                    inv[idx] = item.copy(isEquipped = true)
                    val newDmg = item.tier * 12.0
                    p = p.copy(weaponSlot = item.name, weaponDmg = newDmg)
                    pushNotification("EQUIP SUCCESS", "Equipped ${item.name} (+${newDmg.toInt()} DMG) in Main Hand", "gear")
                    val newLogs = state.logs + LogEntry(logCounter++, "GEAR EQUIPPED: ${item.name.uppercase()} (+${newDmg.toInt()} DMG)", "system")
                    state = state.copy(player = p, inventory = inv, logs = newLogs.takeLast(40))
                } else if (item.slot == "Armor") {
                    // Unequip any existing armor
                    for (i in inv.indices) {
                        if (inv[i].slot == "Armor" && inv[i].isEquipped) {
                            inv[i] = inv[i].copy(isEquipped = false)
                        }
                    }
                    inv[idx] = item.copy(isEquipped = true)
                    val newDef = item.tier * 8.0
                    p = p.copy(armorSlot = item.name, armorDef = newDef)
                    pushNotification("EQUIP SUCCESS", "Equipped ${item.name} (+${newDef.toInt()} DEF) in Body Armor", "gear")
                    val newLogs = state.logs + LogEntry(logCounter++, "GEAR EQUIPPED: ${item.name.uppercase()} (+${newDef.toInt()} DEF)", "system")
                    state = state.copy(player = p, inventory = inv, logs = newLogs.takeLast(40))
                }
            } else if (item.category == "Potion") {
                val healed = (state.player.hp + 50.0).coerceAtMost(state.player.maxHp)
                if (item.count > 1) {
                    inv[idx] = item.copy(count = item.count - 1)
                } else {
                    inv.removeAt(idx)
                }
                pushNotification("FLASK DEPLOYED", "${item.name} applied (+50 HP restored)", "gain")
                val newLogs = state.logs + LogEntry(logCounter++, "FLASK DEPLOYED: Restored 50 HP (Now ${healed.toInt()}/${state.player.maxHp.toInt()} HP)", "gain")
                state = state.copy(
                    player = state.player.copy(hp = healed),
                    inventory = inv,
                    logs = newLogs.takeLast(40)
                )
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
            val newLogs = state.logs + LogEntry(logCounter++, "GEAR UNEQUIPPED: ${item.name.uppercase()} returned to Arsenal", "system")
            state = state.copy(player = p, inventory = inv, logs = newLogs.takeLast(40))
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
            val statDesc = if (slot == "Weapon") "+${tier * 12} Damage" else "+${tier * 8} Armor"
            val newItem = InventoryItem("craft_${System.currentTimeMillis()}", name, "Equipment", 1, tier, slot, statDesc, tier * 35L, isEquipped = false)
            inv.add(newItem)

            var sXp = state.crafting.smithingXp + 50L
            var sLvl = state.crafting.smithingLvl
            val req = Balance.skillXpReq(sLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(logCounter++, "CYBER FORGE SUCCESS: Forged $name (Tier $tier)!", "milestone"))

            if (sXp >= req) {
                sXp -= req
                sLvl++
                newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Smithing reached Level $sLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Smithing reached Level $sLvl!", "level")
            } else {
                pushNotification("FORGE SUCCESS", "Forged $name ($statDesc, Slot: $slot)", "craft")
            }

            state = state.copy(
                inventory = inv,
                crafting = state.crafting.copy(smithingXp = sXp, smithingLvl = sLvl),
                logs = newLogs.takeLast(40)
            )
        } else {
            pushNotification("FORGE FAILED", "Insufficient materials ($reqQty $reqOre required)", "combat")
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
            inv.add(InventoryItem("potion_${System.currentTimeMillis()}", name, "Potion", 1, 1, "", "Restores 75 HP", 15L))

            var hXp = state.crafting.herbloreXp + 40L
            var hLvl = state.crafting.herbloreLvl
            val req = Balance.skillXpReq(hLvl)
            val newLogs = state.logs.toMutableList()
            newLogs.add(LogEntry(logCounter++, "ALCHEMY SYNTHESIS: Brewed $name!", "milestone"))

            if (hXp >= req) {
                hXp -= req
                hLvl++
                newLogs.add(LogEntry(logCounter++, "SKILL LEVEL UP: Herblore reached Level $hLvl!", "milestone"))
                pushNotification("SKILL LEVEL UP", "Herblore reached Level $hLvl!", "level")
            } else {
                pushNotification("SYNTHESIS SUCCESS", "Brewed $name successfully", "craft")
            }

            state = state.copy(
                inventory = inv,
                crafting = state.crafting.copy(herbloreXp = hXp, herbloreLvl = hLvl),
                logs = newLogs.takeLast(40)
            )
        } else {
            pushNotification("SYNTHESIS FAILED", "Insufficient materials ($reqQty $reqHerb required)", "combat")
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
            pushNotification("SAVE RESTORED", "Game state successfully imported from archive", "system")
            true
        } catch (e: Exception) {
            pushNotification("IMPORT ERROR", "Invalid save format: ${e.localizedMessage ?: "Parse failure"}", "combat")
            false
        }
    }

    fun resetGame(): GameState {
        state = GameState()
        pushNotification("SYSTEM RESET", "Game state reset to initial factory protocol", "system")
        return state
    }
}
