package com.aistudio.trueidle.engine

import com.aistudio.trueidle.model.*
import kotlin.random.Random

class GameEngine(initialState: GameState = GameState()) {

    var state: GameState = initialState
        private set

    private var logCounter = 100L

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
            newLogs.add(LogEntry(logCounter++, "Slain ${m.name}! Gained +$goldGained Gold, +$xpGained XP", "gain"))

            // Player XP & Level
            var curXp = p.xp + xpGained
            var curLvl = p.lvl
            val xpReq = Balance.playerXpReq(curLvl)
            if (curXp >= xpReq) {
                curXp -= xpReq
                curLvl++
                newLogs.add(LogEntry(logCounter++, "LEVEL UP! Reached Player Level $curLvl!", "system"))
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
                    beasts = beasts.beasts + newBeast,
                    xp = beasts.xp + 25L
                )
                newLogs.add(LogEntry(logCounter++, "TAMED BEAST: You captured a $beastName!", "loot"))
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
                newLogs.add(LogEntry(logCounter++, "Loot dropped: $dropRes", "loot"))
            }

            // Floor Advancement
            var nextFloor = p.currentFloor
            var nextMaxUnlocked = p.maxUnlockedFloor
            var newKillsOnFloor = killsOnFloor

            if (killsOnFloor >= p.killsRequired) {
                newKillsOnFloor = 0
                if (nextFloor >= nextMaxUnlocked) {
                    nextMaxUnlocked = (nextFloor + 1).coerceAtMost(100)
                }
                if (p.autoAdvance) {
                    nextFloor = (nextFloor + 1).coerceAtMost(100)
                    newLogs.add(LogEntry(logCounter++, "Advancing to Floor $nextFloor!", "system"))
                }
            }

            // Respawn Monster
            m = Balance.generateMonsterForFloor(nextFloor)

            p = p.copy(
                gold = p.gold + goldGained,
                xp = curXp,
                lvl = curLvl,
                hp = p.maxHp, // small recovery on kill
                shield = p.maxShield,
                currentFloor = nextFloor,
                maxUnlockedFloor = nextMaxUnlocked,
                killsOnFloor = newKillsOnFloor
            )
        } else {
            // Monster Still Alive
            m = m.copy(hp = mHp.coerceAtLeast(0.0), shield = mShield.coerceAtLeast(0.0))
        }

        // 5. Check Player Death
        if (pHp <= 0) {
            newLogs.add(LogEntry(logCounter++, "You were defeated by ${m.name}! Resting and respawning...", "combat"))
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

    fun navFloor(delta: Int): GameState {
        val target = (state.player.currentFloor + delta).coerceIn(1, state.player.maxUnlockedFloor)
        if (target != state.player.currentFloor) {
            val newMonster = Balance.generateMonsterForFloor(target)
            state = state.copy(
                player = state.player.copy(currentFloor = target, killsOnFloor = 0),
                monster = newMonster
            )
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
        state = state.copy(player = state.player.copy(autoAdvance = !state.player.autoAdvance))
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
            val newLogs = state.logs + LogEntry(logCounter++, "Harvested 2x $herbName!", "loot")
            state = state.copy(
                gathering = state.gathering.copy(
                    plots = plots,
                    farmingXp = state.gathering.farmingXp + 20L
                ),
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
            val newLogs = state.logs + LogEntry(logCounter++, "Mined 3x $oreName!", "loot")
            state = state.copy(
                gathering = state.gathering.copy(
                    veins = veins,
                    miningXp = state.gathering.miningXp + 25L
                ),
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
            val newLogs = state.logs + LogEntry(logCounter++, "Summoned $type into undead army!", "system")
            state = state.copy(
                necro = state.necro.copy(
                    boneEssence = state.necro.boneEssence - cost,
                    army = army,
                    xp = state.necro.xp + 15L
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
            val newLogs = state.logs + LogEntry(logCounter++, "MERGE SUCCESS: Upgraded ${upgraded.name} to ⭐${upgraded.stars}!", "system")
            state = state.copy(
                beasts = state.beasts.copy(beasts = beasts, xp = state.beasts.xp + 50L),
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
                val newLogs = state.logs + LogEntry(logCounter++, "Trained ${node.name} to Rank ${node.rank + 1}!", "system")
                state = state.copy(
                    player = p,
                    training = state.training.copy(nodes = nodes, xp = state.training.xp + 30L),
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
                    p = p.copy(weaponSlot = item.name, weaponDmg = item.tier * 12.0)
                } else if (item.slot == "Armor") {
                    p = p.copy(armorSlot = item.name, armorDef = item.tier * 8.0)
                }
                val newLogs = state.logs + LogEntry(logCounter++, "Equipped ${item.name} in ${item.slot} slot!", "system")
                state = state.copy(player = p, logs = newLogs.takeLast(40))
            } else if (item.category == "Potion") {
                val healed = (state.player.hp + 50.0).coerceAtMost(state.player.maxHp)
                if (item.count > 1) {
                    inv[idx] = item.copy(count = item.count - 1)
                } else {
                    inv.removeAt(idx)
                }
                val newLogs = state.logs + LogEntry(logCounter++, "Used ${item.name}! Healed to ${healed.toInt()} HP", "gain")
                state = state.copy(
                    player = state.player.copy(hp = healed),
                    inventory = inv,
                    logs = newLogs.takeLast(40)
                )
            }
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
            inv.add(InventoryItem("craft_${System.currentTimeMillis()}", name, "Equipment", 1, tier, slot, statDesc, tier * 35L))
            val newLogs = state.logs + LogEntry(logCounter++, "SMITHE SUCCESS: Forged $name (Tier $tier)!", "gain")
            state = state.copy(
                inventory = inv,
                crafting = state.crafting.copy(smithingXp = state.crafting.smithingXp + 45L),
                logs = newLogs.takeLast(40)
            )
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
            val newLogs = state.logs + LogEntry(logCounter++, "ALCHEMY SUCCESS: Brewed $name!", "gain")
            state = state.copy(
                inventory = inv,
                crafting = state.crafting.copy(herbloreXp = state.crafting.herbloreXp + 35L),
                logs = newLogs.takeLast(40)
            )
        }
        return state
    }

    fun clearLogs(): GameState {
        state = state.copy(logs = emptyList())
        return state
    }
}
