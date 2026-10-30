package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.BigNumberFormatter
import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.GameClock
import com.aistudio.trueidle.core.HarvestXp
import com.aistudio.trueidle.core.Ids
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.data.NoOpSaveStore
import com.aistudio.trueidle.data.SaveRepository
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.EssenceState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.MiningVein
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.NecromancyState
import com.aistudio.trueidle.model.PlayerState
import com.aistudio.trueidle.model.Plot
import com.aistudio.trueidle.model.Talent
import com.aistudio.trueidle.model.TalentState
import kotlin.math.pow
import kotlin.random.Random

/**
 * The game engine. `tick` is ordered so that non-combat subsystems (play time, talent timers,
 * regeneration, Aegis drain and harvesting) always run, even while the hero is recovering or
 * waiting to respawn.
 */
class GameEngine(
    initialState: GameState = GameState(),
    private val clock: GameClock? = null,
    private val random: Random = Random.Default,
    private val saves: SaveRepository = SaveRepository(NoOpSaveStore)
) {
    var state: GameState = ContentCatalog.reconcile(sanitize(initialState))
        private set

    private val ids = Ids(nextIdFrom(initialState))

    private var respawnLeft = 0.0
    private var eightHourIdleAlertPending = false
    private var playTimeSubSec = 0.0

    // ------------------------------------------------------------------ lifecycle

    private fun nowWallMs(): Long = clock?.wallMs() ?: System.currentTimeMillis()

    private fun nowSnapshot(): ClockSnapshot = ClockSnapshot(
        wallMs = clock?.wallMs() ?: System.currentTimeMillis(),
        elapsedMs = clock?.elapsedMs() ?: System.nanoTime() / 1_000_000L,
        bootCount = clock?.bootCount() ?: 0
    )

    private fun log(type: String, text: String): LogEntry =
        LogEntry(ids.next(), text, type, nowWallMs())

    private fun reseedIds(from: GameState) {
        ids.reset(nextIdFrom(from))
    }

    fun restore(newState: GameState) {
        // A save written before a content expansion gains the new entries here, once, by id.
        state = ContentCatalog.reconcile(sanitize(newState))
        // The saved notation preference drives the shared formatter, and the stored name is
        // normalised so an unknown/legacy value cannot leave the UI in a state the engine does not
        // recognise.
        state = state.copy(display = state.display.copy(notation = applyNotation(state.display.notation).mode.name))
        reseedIds(state)
        respawnLeft = 0.0
        playTimeSubSec = 0.0
        eightHourIdleAlertPending = false
    }

    fun notifyRecovered(slot: String) {
        // Recovery is silent; the UI surfaces it through the recovery banner when it matters.
    }

    fun consumeEightHourIdleAlert(): Boolean {
        val pending = eightHourIdleAlertPending
        eightHourIdleAlertPending = false
        return pending
    }

    fun snapshotForSave(now: ClockSnapshot): GameState {
        state = state.copy(
            meta = state.meta.copy(
                savedWallMs = now.wallMs,
                savedElapsedMs = now.elapsedMs,
                bootCount = now.bootCount,
                nextId = ids.current()
            )
        )
        return state
    }

    /**
     * Called on every foreground. Never replaces an unclaimed offline report, and re-anchors the
     * away window when the clock is not trustworthy.
     */
    fun resume(now: ClockSnapshot, lastActiveElapsedMs: Long? = null) {
        // Background-run mode: the loop was live until [lastActiveElapsedMs], so only the gap since
        // then (device asleep, process frozen) is offline time. Re-anchoring the save meta to that
        // moment lets the normal away-time maths below handle it.
        if (lastActiveElapsedMs != null && lastActiveElapsedMs <= now.elapsedMs) {
            val gap = now.elapsedMs - lastActiveElapsedMs
            state = state.copy(
                meta = state.meta.copy(
                    savedWallMs = now.wallMs - gap,
                    savedElapsedMs = lastActiveElapsedMs,
                    bootCount = now.bootCount
                )
            )
        }

        fun reanchor() {
            state = state.copy(
                meta = state.meta.copy(
                    savedWallMs = now.wallMs,
                    savedElapsedMs = now.elapsedMs,
                    bootCount = now.bootCount,
                    nextId = ids.current()
                )
            )
        }

        // An unclaimed report stays pending; otherwise the player loses it on the next foreground.
        if (state.pendingOfflineLoot != null) {
            reanchor()
            return
        }

        val awayMs = OfflineEngine.trustedAwayMs(state.meta, now)
        if (awayMs < 0L || awayMs < Balance.OFFLINE_MIN_AWAY_MS) {
            reanchor()
            return
        }

        eightHourIdleAlertPending = awayMs >= Balance.OFFLINE_CAP_MS
        val rawAwayMs = OfflineEngine.trustedAwayMs(state.meta, now, Long.MAX_VALUE)
        val derived = DerivedStats.of(state)
        val computed = OfflineEngine.compute(state, derived, awayMs, Balance.OFFLINE_EFFICIENCY, random)
        val drainedEssence = drainFaith(state.essence, awayMs)
        val loot = computed.copy(
            rawAwaySeconds = maxOf(rawAwayMs, awayMs) / 1000L,
            faithDrained = (state.essence.faith - drainedEssence.faith).coerceAtLeast(0L)
        )

        val harvest = if (state.harvest.autoCollectUnlocked && state.harvest.autoCollectEnabled) {
            state.harvest.copy(
                plots = state.harvest.plots.map { it.copy(growthProgress = 0f, isReady = false) },
                veins = state.harvest.veins.map { it.copy(miningProgress = 0f, isReady = false) }
            )
        } else {
            state.harvest
        }

        state = state.copy(
            meta = state.meta.copy(
                savedWallMs = now.wallMs,
                savedElapsedMs = now.elapsedMs,
                bootCount = now.bootCount,
                nextId = ids.current()
            ),
            pendingOfflineLoot = loot,
            harvest = harvest,
            essence = drainedEssence
        )
    }

    private fun drainFaith(essence: EssenceState, awayMs: Long): EssenceState {
        if (!essence.aegisActive) return essence
        val rank = essence.upgrades.firstOrNull { it.id == "faith_preserve" }?.rank ?: 0
        val mult = (1.0 - 0.15 * rank).coerceAtLeast(0.2)
        val drain = (awayMs / 1000.0 * Balance.AEGIS_DRAIN_PER_SEC * mult).toLong()
        val faith = (essence.faith - drain).coerceAtLeast(0L)
        return essence.copy(faith = faith, faithRemainder = 0.0, aegisActive = faith > 0L)
    }

    /**
     * Applies a pending report atomically: floor, monster, kills-on-floor, level ups, talent points
     * and lifetime stats all move together.
     */
    fun claimOfflineLoot(): GameState {
        val loot = state.pendingOfflineLoot ?: return state
        val finalFloor = loot.finalFloor.coerceIn(1, Balance.MAX_FLOOR)
        // The monster on screen must belong to the destination floor.
        val monster = Balance.generateMonsterForFloor(finalFloor)

        val inventory = state.inventory.toMutableList()
        var herbs = 0
        var ores = 0
        for (item in loot.items) {
            addToInventory(inventory, item.name, item.count)
            if (ItemCatalog.isHerb(item.name)) herbs += item.count
            if (ItemCatalog.isOre(item.name)) ores += item.count
        }

        var beasts = state.beasts
        if (loot.beastsTamed > 0) {
            val tamed = mutableListOf<Beast>()
            repeat(loot.beastsTamed) {
                if (beasts.beasts.size + tamed.size < Balance.BEAST_CAP) {
                    tamed += tamedBeast(monster, "Autonomous Scout")
                }
            }
            beasts = beasts.copy(beasts = beasts.beasts + tamed)
        }

        var xp = state.player.xp + loot.xpEarned
        var lvl = state.player.lvl
        var levels = 0
        while (xp >= Balance.playerXpReq(lvl)) {
            xp -= Balance.playerXpReq(lvl)
            lvl++
            levels++
        }

        // Slayer kills earned offline, resolved exactly like the live path.
        var slayer = state.slayer
        var slayerGold = 0L
        val task = slayer.currentTask
        if (loot.slayerKills > 0 && !task.isCompleted) {
            val kills = task.currentKills + loot.slayerKills
            if (kills < task.requiredKills) {
                slayer = slayer.copy(currentTask = task.copy(currentKills = kills))
            } else {
                var sxp = slayer.xp + 100L
                var slvl = slayer.lvl
                while (sxp >= Balance.slayerXpReq(slvl)) {
                    sxp -= Balance.slayerXpReq(slvl)
                    slvl++
                }
                slayer = slayer.copy(
                    lvl = slvl,
                    xp = sxp,
                    slayerPoints = slayer.slayerPoints + task.rewardPoints,
                    currentTask = task.copy(currentKills = kills, isCompleted = true)
                )
                slayerGold = task.rewardGold
            }
        }

        var player = state.player.copy(
            gold = state.player.gold + loot.goldEarned + slayerGold,
            xp = xp,
            lvl = lvl,
            currentFloor = finalFloor,
            maxUnlockedFloor = maxOf(state.player.maxUnlockedFloor, finalFloor),
            killsOnFloor = loot.killsOnFloor.coerceIn(0, Balance.killsRequiredFor(finalFloor)),
            killsRequired = Balance.killsRequiredFor(finalFloor),
            failuresInRow = 0
        )
        if (levels > 0) {
            // Level ups restore resources, exactly as they do on the live path.
            val d = DerivedStats.of(state.copy(player = player))
            player = player.copy(hp = d.maxHp, shield = d.maxShield, mana = d.maxMana)
        }

        state = state.copy(
            player = player,
            monster = monster,
            necro = state.necro.copy(boneEssence = state.necro.boneEssence + loot.bonesEarned),
            essence = state.essence.copy(faith = state.essence.faith + loot.faithEarned),
            slayer = slayer,
            beasts = beasts,
            inventory = inventory,
            talent = state.talent.copy(
                talentPoints = state.talent.talentPoints + levels + loot.talentPointsGained
            ),
            lifetime = state.lifetime.copy(
                totalKills = state.lifetime.totalKills + loot.kills,
                beastsTamed = state.lifetime.beastsTamed + loot.beastsTamed,
                herbsHarvested = state.lifetime.herbsHarvested + herbs,
                oresMined = state.lifetime.oresMined + ores
            ),
            pendingOfflineLoot = null,
            logs = (state.logs + log(
                LogType.MILESTONE,
                "OFFLINE REPORT PROCESSED: +${loot.goldEarned} CREDITS, +${loot.xpEarned} XP, " +
                    "+${loot.bonesEarned} BONE ESSENCE, +${loot.faithEarned} FAITH (${loot.kills} kills)."
            )).takeLast(Balance.LOG_CAP)
        )
        evaluateAchievements(force = true)
        return state
    }

    fun simulateOfflineRecon(hours: Int = 8): GameState {
        val awayMs = hours * 3600_000L
        val derived = DerivedStats.of(state)
        val loot = OfflineEngine.compute(state, derived, awayMs, Balance.OFFLINE_EFFICIENCY, random)
        state = state.copy(pendingOfflineLoot = loot)
        return state
    }

    // ------------------------------------------------------------------ tick

    /**
     * Working copy of one simulation frame. Systems mutate this and [commit] writes it back once.
     */
    private class Frame(val base: GameState) {
        var player: PlayerState = base.player
        var monster: Monster = base.monster
        var necro: NecromancyState = base.necro
        var beasts: BeastTamingState = base.beasts
        var harvest = base.harvest
        var armory = base.armory
        var essence: EssenceState = base.essence
        var slayer = base.slayer
        var talent: TalentState = base.talent
        var lifetime = base.lifetime
        var inventory: MutableList<InventoryItem> = base.inventory.toMutableList()
        val logs: MutableList<LogEntry> = mutableListOf()

        private var cachedDerived: Derived? = null
        fun derived(): Derived = cachedDerived ?: DerivedStats.of(view()).also { cachedDerived = it }
        fun invalidateDerived() { cachedDerived = null }

        fun view(): GameState = base.copy(
            player = player,
            monster = monster,
            necro = necro,
            beasts = beasts,
            harvest = harvest,
            armory = armory,
            essence = essence,
            slayer = slayer,
            talent = talent,
            lifetime = lifetime,
            inventory = inventory
        )

        fun commit(engine: GameEngine) {
            engine.state = view().copy(logs = (base.logs + logs).takeLast(Balance.LOG_CAP))
        }
    }

    fun tick(deltaSeconds: Float): GameState {
        val dt = deltaSeconds.toDouble()
        val frame = Frame(state)
        var derivedCache: Derived? = null

        // Subsystems that must keep running while the hero is down or waiting to respawn.
        tickPlayTime(frame, dt)
        tickTalentTimers(frame, dt)
        tickRegeneration(frame, dt)
        tickAegisDrain(frame, dt)
        tickHarvest(frame, deltaSeconds)

        if (frame.player.recoverLeft > 0.0) {
            tickRecovery(frame, dt)
            frame.commit(this)
            return state
        }

        if (respawnLeft > 0.0) {
            respawnLeft = (respawnLeft - dt).coerceAtLeast(0.0)
            frame.commit(this)
            return state
        }

        tickCombat(frame, dt)
        frame.commit(this)
        if (frame.logs.isNotEmpty()) evaluateAchievements()
        return state
    }

    private fun tickPlayTime(frame: Frame, dt: Double) {
        playTimeSubSec += dt
        if (playTimeSubSec >= 1.0) {
            val whole = playTimeSubSec.toLong()
            playTimeSubSec -= whole
            frame.lifetime = frame.lifetime.copy(
                playTimeSeconds = frame.lifetime.playTimeSeconds + whole
            )
        }
    }

    private fun tickTalentTimers(frame: Frame, dt: Double) {
        val talents = frame.talent.activeTalents
        if (talents.none { it.cooldownRemaining > 0.0 || it.durationRemaining > 0.0 || it.isActive }) return
        frame.talent = frame.talent.copy(
            activeTalents = talents.map { talent ->
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
        )
    }

    private fun tickRegeneration(frame: Frame, dt: Double) {
        if (frame.player.recoverLeft > 0.0) return // a downed hero does not regenerate
        val d = frame.derived()
        frame.player = frame.player.copy(
            hp = (frame.player.hp + d.healthRegen * dt).coerceAtMost(d.maxHp),
            shield = (frame.player.shield + d.shieldRegen * dt).coerceAtMost(d.maxShield),
            mana = (frame.player.mana + Balance.MANA_REGEN_PER_SEC * dt).coerceAtMost(d.maxMana)
        )
    }

    private fun tickAegisDrain(frame: Frame, dt: Double) {
        if (!frame.essence.aegisActive) return
        val wasActive = frame.essence.aegisActive
        val rank = frame.essence.upgrades.firstOrNull { it.id == "faith_preserve" }?.rank ?: 0
        val mult = (1.0 - 0.15 * rank).coerceAtLeast(0.2)
        // Accumulate the fractional drain so 0.5/s stays 0.5/s instead of rounding up to 4/s.
        val accumulated = frame.essence.faithRemainder + Balance.AEGIS_DRAIN_PER_SEC * mult * dt
        val whole = accumulated.toLong()
        frame.essence = if (frame.essence.faith <= 0L) {
            frame.essence.copy(aegisActive = false, faithRemainder = 0.0)
        } else {
            val faith = (frame.essence.faith - whole).coerceAtLeast(0L)
            frame.essence.copy(
                faith = faith,
                faithRemainder = if (faith == 0L) 0.0 else (accumulated - whole),
                aegisActive = faith > 0L
            )
        }
        if (wasActive != frame.essence.aegisActive) {
            frame.invalidateDerived()
        }
    }

    private fun tickRecovery(frame: Frame, dt: Double) {
        val remaining = (frame.player.recoverLeft - dt).coerceAtLeast(0.0)
        if (remaining == 0.0) {
            val d = DerivedStats.of(frame.view())
            frame.player = frame.player.copy(
                recoverLeft = 0.0,
                hp = d.maxHp,
                shield = d.maxShield
            )
            frame.logs += log(LogType.SYSTEM, "SYSTEMS REBOOTED: Defensive shields and vital reserves restored.")
        } else {
            frame.player = frame.player.copy(recoverLeft = remaining)
        }
    }

    // ------------------------------------------------------------------ combat

    private fun tickCombat(frame: Frame, dt: Double) {
        val d = frame.derived()
        val monster = frame.monster

        val totalDps = CombatMath.totalDps(frame.view(), d)
        val dealt = totalDps * CombatMath.mitigation(monster.armor) * dt
        var monsterShield = monster.shield
        var monsterHp = monster.hp
        if (monsterShield > 0.0) {
            val absorbed = dealt.coerceAtMost(monsterShield)
            monsterShield -= absorbed
            monsterHp -= (dealt - absorbed)
        } else {
            monsterHp -= dealt
        }
        frame.monster = monster.copy(
            shield = monsterShield.coerceAtLeast(0.0),
            hp = monsterHp
        )

        // Minion plating soaks a bounded share of incoming damage.
        val soakRank = frame.necro.upgrades.firstOrNull { it.id == "necro_soak" }?.rank ?: 0
        val soak = frame.necro.army.sumOf { it.soak * it.count }
        val soakReduction = if (soak > 0.0) {
            ((soak / (soak + 100.0)) * (1.0 + 0.2 * soakRank)).coerceAtMost(0.5)
        } else 0.0
        val aegisMult = 1.0 - CombatMath.aegisReduction(frame.essence)
        val incomingRate = CombatMath.incomingPerSec(monster, d, 0.0) * aegisMult
        val monsterDamage = incomingRate * dt * (1.0 - soakReduction)

        val hpBefore = frame.player.hp
        var playerShield = frame.player.shield
        var playerHp = frame.player.hp
        if (playerShield > 0.0) {
            val absorbed = monsterDamage.coerceAtMost(playerShield)
            playerShield -= absorbed
            playerHp -= (monsterDamage - absorbed)
        } else {
            playerHp -= monsterDamage
        }

        // Struggle passives only count damage that reached HP, accumulated without truncation.
        val hpDamage = (hpBefore - playerHp).coerceAtLeast(0.0)
        if (hpDamage > 0.0) {
            val accumulated = frame.talent.struggleDamageFraction + hpDamage
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
        rollTame(frame, monster, d)
        rollLoot(frame, d)
        if (monster.isBoss) grantTrophy(frame)
        advanceFloor(frame, monster)
        if (frame.base.automation.autoMergeBeasts) autoMergeBeasts(frame)
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
        respawnLeft = 0.0
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
        respawnLeft = Balance.RESPAWN_GAP_SECONDS
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
        if (leveled) frame.logs += log(LogType.SKILL, "SKILL LEVEL UP: Essence reached Level $lvl!")
        return essence.copy(xp = xp, lvl = lvl)
    }

    private fun grantSlayerProgress(frame: Frame, monster: Monster, count: Int = 1) {
        val task = frame.slayer.currentTask
        if (task.isCompleted) return
        if (!task.targetName.equals(monster.name, ignoreCase = true)) return
        val kills = task.currentKills + count
        if (kills < task.requiredKills) {
            frame.slayer = frame.slayer.copy(currentTask = task.copy(currentKills = kills))
            return
        }
        // Completed tasks stay visible (and claimable) until the player rolls the next one.
        frame.slayer = frame.slayer.copy(
            currentTask = task.copy(currentKills = kills, isCompleted = true),
            slayerPoints = frame.slayer.slayerPoints + task.rewardPoints
        )
        frame.player = frame.player.copy(gold = frame.player.gold + task.rewardGold)
        frame.logs += log(
            LogType.MILESTONE,
            "SLAYER DIRECTIVE FULFILLED: ${task.targetName} (+${task.rewardGold} CREDITS, +${task.rewardPoints} PTS)"
        )
        var xp = frame.slayer.xp + 100L
        var lvl = frame.slayer.lvl
        while (xp >= Balance.slayerXpReq(lvl)) {
            xp -= Balance.slayerXpReq(lvl)
            lvl++
        }
        frame.slayer = frame.slayer.copy(xp = xp, lvl = lvl)
    }

    private fun rollTame(frame: Frame, monster: Monster, d: Derived) {
        if (frame.beasts.beasts.size >= Balance.BEAST_CAP) return
        if (random.nextDouble() >= d.tameChance) return
        val beast = tamedBeast(monster, "Tamed Hunter")
        frame.beasts = frame.beasts.copy(beasts = frame.beasts.beasts + beast)
        frame.lifetime = frame.lifetime.copy(beastsTamed = frame.lifetime.beastsTamed + 1)
        frame.logs += log(LogType.LOOT, "BEAST TAMED: Secured ${beast.name} into the squad!")
    }

    /**
     * Tamed beasts keep the plain species name. Embedding "Companion"/"Scout" in the name used to
     * make beasts from the live and offline paths impossible to merge together.
     */
    private fun tamedBeast(monster: Monster, trait: String) = Beast(
        id = "beast_${ids.next()}",
        name = monster.name,
        rank = 1,
        stars = 1,
        dps = (monster.damage * 0.6).coerceAtLeast(4.0),
        hp = monster.maxHp * 0.5,
        maxHp = monster.maxHp * 0.5,
        trait = trait
    )

    private fun autoMergeBeasts(frame: Frame) {
        var beasts = frame.beasts.beasts
        var merged = true
        while (merged) {
            merged = false
            val pair = beasts.firstOrNull { a ->
                beasts.any { b -> b.id != a.id && b.name == a.name && b.stars == a.stars }
            } ?: break
            val partner = beasts.first { it.id != pair.id && it.name == pair.name && it.stars == pair.stars }
            beasts = beasts - pair - partner
            beasts = beasts + pair.copy(stars = pair.stars + 1, dps = pair.dps * 1.6, hp = pair.maxHp * 1.5, maxHp = pair.maxHp * 1.5)
            frame.logs += log(LogType.MILESTONE, "AUTO-MERGE: ${pair.name} upgraded to ${pair.stars + 1} stars.")
            merged = true
        }
        frame.beasts = frame.beasts.copy(beasts = beasts)
    }

    private fun rollLoot(frame: Frame, d: Derived) {
        if (random.nextDouble() >= d.lootChance) return
        val resource = if (random.nextBoolean()) ItemCatalog.COPPER_ORE else ItemCatalog.FELWEED
        addToInventory(frame.inventory, resource, 1)
        frame.logs += log(LogType.LOOT, "SALVAGE ACQUIRED: $resource")
    }

    private fun grantTrophy(frame: Frame) {
        addToInventory(frame.inventory, ItemCatalog.BOSS_TROPHY, 1)
        frame.logs += log(LogType.LOOT, "TROPHY CLAIMED: ${ItemCatalog.BOSS_TROPHY}")
    }

    private fun addToInventory(inventory: MutableList<InventoryItem>, name: String, qty: Double) {
        val idx = inventory.indexOfFirst {
            it.name == name && it.category != ItemCatalog.CATEGORY_EQUIPMENT
        }
        if (idx >= 0) {
            inventory[idx] = inventory[idx].copy(count = inventory[idx].count + qty)
        } else {
            inventory.add(ItemCatalog.newResource("item_${ids.next()}", name, qty))
        }
    }

    private fun addToInventory(inventory: MutableList<InventoryItem>, name: String, qty: Int) {
        addToInventory(inventory, name, qty.toDouble())
    }

    /**
     * Saturating non-negative addition for XP banks. Long overflow must never turn progression
     * counters negative.
     */
    private fun safeAddNonNegative(current: Long, amount: Long): Long {
        val base = current.coerceAtLeast(0L)
        val increment = amount.coerceAtLeast(0L)
        return if (Long.MAX_VALUE - base < increment) Long.MAX_VALUE else base + increment
    }

    private fun advanceProficiency(currentLvl: Int, currentXp: Long, gain: Long): Pair<Int, Long> {
        var lvl = currentLvl.coerceIn(1, 100)
        var xp = safeAddNonNegative(currentXp, gain)
        while (lvl < 100 && xp >= XpCurves.proficiency(lvl)) {
            xp -= XpCurves.proficiency(lvl)
            lvl++
        }
        return Pair(lvl.coerceIn(1, 100), xp)
    }

    // ------------------------------------------------------------------ harvest

    private fun tickHarvest(frame: Frame, dt: Float) {
        val speedRank = frame.harvest.upgrades.firstOrNull { it.id == "harvest_speed" }?.rank ?: 0
        val miningRank = frame.harvest.upgrades.firstOrNull { it.id == "mining_speed" }?.rank ?: 0
        val extraYield = frame.harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
        val extraOreYield = frame.harvest.upgrades.firstOrNull { it.id == "mining_yield" }?.rank ?: 0
        val farmMult = (0.97.pow((frame.harvest.farmingLvl - 1).toDouble()) * 0.92.pow(speedRank))
            .coerceAtLeast(0.20).toFloat()
        val mineMult = (0.97.pow((frame.harvest.miningLvl - 1).toDouble()) * 0.92.pow(miningRank))
            .coerceAtLeast(0.20).toFloat()
        val autoCollect = frame.harvest.autoCollectUnlocked && frame.harvest.autoCollectEnabled

        // Global skill XP tracks the guaranteed batch plus explicit yield-upgrade units.
        // Proficiency's 2x-per-level bonus is intentionally separate: it scales actual inventory
        // yield and proficiency XP, but not the long-term Farming/Mining skill curve.
        val farmingSkillYield = (2.0 + extraYield).toLong().coerceAtLeast(1L)
        val miningSkillYield = (3.0 + extraYield + extraOreYield).toLong().coerceAtLeast(1L)

        val herbGains = mutableMapOf<String, Double>()
        val oreGains = mutableMapOf<String, Double>()
        var farmingXpGained = 0L
        var miningXpGained = 0L

        val plots = frame.harvest.plots.map { plot ->
            val duration = (plot.durationSeconds * farmMult).coerceAtLeast(0.1f)
            val shouldAuto = plot.autoQueue || autoCollect
            val profMult = XpCurves.proficiencyYieldMultiplier(plot.proficiencyLevel)
            val baseYield = (2.0 + extraYield) * profMult
            val rewardAmt = baseYield.toLong().coerceAtLeast(1L)
            if (plot.isReady) {
                if (shouldAuto) {
                    herbGains[plot.herbName] = (herbGains[plot.herbName] ?: 0.0) + baseYield
                    farmingXpGained = safeAddNonNegative(
                        farmingXpGained,
                        HarvestXp.forBaseBatch(plot.herbName, farmingSkillYield)
                    )
                    val (newLvl, newXp) = advanceProficiency(plot.proficiencyLevel, plot.proficiencyXp, rewardAmt)
                    plot.copy(growthProgress = 0f, isReady = false, proficiencyLevel = newLvl, proficiencyXp = newXp)
                } else plot
            } else {
                val progress = plot.growthProgress + dt / duration
                if (progress >= 1f) {
                    if (shouldAuto) {
                        herbGains[plot.herbName] = (herbGains[plot.herbName] ?: 0.0) + baseYield
                        farmingXpGained = safeAddNonNegative(
                            farmingXpGained,
                            HarvestXp.forBaseBatch(plot.herbName, farmingSkillYield)
                        )
                        val (newLvl, newXp) = advanceProficiency(plot.proficiencyLevel, plot.proficiencyXp, rewardAmt)
                        plot.copy(growthProgress = 0f, isReady = false, proficiencyLevel = newLvl, proficiencyXp = newXp)
                    } else {
                        plot.copy(growthProgress = 1f, isReady = true)
                    }
                } else {
                    plot.copy(growthProgress = progress)
                }
            }
        }

        val veins = frame.harvest.veins.map { vein ->
            // Mining level gates the vein: Iron Ore must not be farmable at level 1.
            if (frame.harvest.miningLvl < vein.reqLvl) {
                vein.copy(miningProgress = 0f, isReady = false)
            } else {
                val duration = (vein.durationSeconds * mineMult).coerceAtLeast(0.1f)
                val shouldAuto = vein.autoQueue || autoCollect
                val profMult = XpCurves.proficiencyYieldMultiplier(vein.proficiencyLevel)
                val baseYield = (3.0 + extraYield + extraOreYield) * profMult
                val rewardAmt = baseYield.toLong().coerceAtLeast(1L)
                if (vein.isReady) {
                    if (shouldAuto) {
                        oreGains[vein.oreName] = (oreGains[vein.oreName] ?: 0.0) + baseYield
                        miningXpGained = safeAddNonNegative(
                            miningXpGained,
                            HarvestXp.forBaseBatch(vein.oreName, miningSkillYield)
                        )
                        val (newLvl, newXp) = advanceProficiency(vein.proficiencyLevel, vein.proficiencyXp, rewardAmt)
                        vein.copy(miningProgress = 0f, isReady = false, proficiencyLevel = newLvl, proficiencyXp = newXp)
                    } else vein
                } else {
                    val progress = vein.miningProgress + dt / duration
                    if (progress >= 1f) {
                        if (shouldAuto) {
                            oreGains[vein.oreName] = (oreGains[vein.oreName] ?: 0.0) + baseYield
                            miningXpGained = safeAddNonNegative(
                                miningXpGained,
                                HarvestXp.forBaseBatch(vein.oreName, miningSkillYield)
                            )
                            val (newLvl, newXp) = advanceProficiency(vein.proficiencyLevel, vein.proficiencyXp, rewardAmt)
                            vein.copy(miningProgress = 0f, isReady = false, proficiencyLevel = newLvl, proficiencyXp = newXp)
                        } else {
                            vein.copy(miningProgress = 1f, isReady = true)
                        }
                    } else {
                        vein.copy(miningProgress = progress)
                    }
                }
            }
        }

        var farmingXp = frame.harvest.farmingXp
        var miningXp = frame.harvest.miningXp
        var farmingLvl = frame.harvest.farmingLvl.coerceIn(1, XpCurves.GATHERING_SKILL_MAX_LEVEL)
        var miningLvl = frame.harvest.miningLvl.coerceIn(1, XpCurves.GATHERING_SKILL_MAX_LEVEL)

        if (herbGains.isNotEmpty()) {
            val countItems = herbGains.size.toLong()
            herbGains.forEach { (name, qty) -> addToInventory(frame.inventory, name, qty) }
            frame.lifetime = frame.lifetime.copy(herbsHarvested = frame.lifetime.herbsHarvested + countItems)
            farmingXp = safeAddNonNegative(farmingXp, farmingXpGained)
        }
        if (oreGains.isNotEmpty()) {
            val countItems = oreGains.size.toLong()
            oreGains.forEach { (name, qty) -> addToInventory(frame.inventory, name, qty) }
            frame.lifetime = frame.lifetime.copy(oresMined = frame.lifetime.oresMined + countItems)
            miningXp = safeAddNonNegative(miningXp, miningXpGained)
        }
        while (
            farmingLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL &&
            farmingXp >= Balance.gatheringSkillXpReq(farmingLvl)
        ) {
            farmingXp -= Balance.gatheringSkillXpReq(farmingLvl)
            farmingLvl++
            frame.logs += log(LogType.SKILL, "SKILL LEVEL UP: Farming reached Level $farmingLvl!")
        }
        while (
            miningLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL &&
            miningXp >= Balance.gatheringSkillXpReq(miningLvl)
        ) {
            miningXp -= Balance.gatheringSkillXpReq(miningLvl)
            miningLvl++
            frame.logs += log(LogType.SKILL, "SKILL LEVEL UP: Mining reached Level $miningLvl!")
        }
        if (farmingLvl >= XpCurves.GATHERING_SKILL_MAX_LEVEL) farmingXp = 0L
        if (miningLvl >= XpCurves.GATHERING_SKILL_MAX_LEVEL) miningXp = 0L

        frame.harvest = frame.harvest.copy(
            plots = plots,
            veins = veins,
            farmingXp = farmingXp,
            farmingLvl = farmingLvl,
            miningXp = miningXp,
            miningLvl = miningLvl,
            autoCollectUnlocked = farmingLvl >= 10 || miningLvl >= 10
        )
    }

    fun harvestPlot(plotId: Int): GameState {
        val plots = state.harvest.plots.toMutableList()
        val idx = plots.indexOfFirst { it.id == plotId }
        if (idx < 0 || !plots[idx].isReady) return state

        val plot = plots[idx]
        val herbName = plot.herbName
        val extraYield = state.harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
        val profMult = XpCurves.proficiencyYieldMultiplier(plot.proficiencyLevel)
        val baseYield = 2.0 + extraYield
        val yield = baseYield * profMult
        val rewardAmt = yield.toLong().coerceAtLeast(1L)
        val skillXpReward = if (state.harvest.farmingLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL) {
            HarvestXp.forBaseBatch(herbName, baseYield.toLong())
        } else 0L
        val inventory = state.inventory.toMutableList()
        addToInventory(inventory, herbName, yield)
        val (newLvl, newXp) = advanceProficiency(plot.proficiencyLevel, plot.proficiencyXp, rewardAmt)
        plots[idx] = plot.copy(growthProgress = 0f, isReady = false, proficiencyLevel = newLvl, proficiencyXp = newXp)

        val yieldStr = NumberFormatter.short(yield)
        val (xp, lvl, logs) = grantSkillXp(
            state.harvest.farmingXp,
            state.harvest.farmingLvl,
            skillXpReward,
            "Farming",
            "BIO-HARVEST: Collected ${yieldStr}x $herbName (+${NumberFormatter.short(skillXpReward)} Farming XP, +${NumberFormatter.short(rewardAmt)} Proficiency)",
            xpRequirement = Balance::gatheringSkillXpReq,
            maxLevel = XpCurves.GATHERING_SKILL_MAX_LEVEL
        )
        state = state.copy(
            harvest = state.harvest.copy(plots = plots, farmingXp = xp, farmingLvl = lvl),
            inventory = inventory,
            lifetime = state.lifetime.copy(herbsHarvested = state.lifetime.herbsHarvested + 1L),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        evaluateAchievements(force = true)
        return state
    }

    fun mineVein(veinId: Int): GameState {
        val veins = state.harvest.veins.toMutableList()
        val idx = veins.indexOfFirst { it.id == veinId }
        if (idx < 0) return state
        val vein = veins[idx]
        if (state.harvest.miningLvl < vein.reqLvl) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "EXTRACTION REFUSED: ${vein.oreName} requires Mining level ${vein.reqLvl}."
                )).takeLast(Balance.LOG_CAP)
            )
            return state
        }
        if (!vein.isReady) return state

        val extraYield = state.harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
        val extraOreYield = state.harvest.upgrades.firstOrNull { it.id == "mining_yield" }?.rank ?: 0
        val profMult = XpCurves.proficiencyYieldMultiplier(vein.proficiencyLevel)
        val baseYield = 3.0 + extraYield + extraOreYield
        val yield = baseYield * profMult
        val rewardAmt = yield.toLong().coerceAtLeast(1L)
        val skillXpReward = if (state.harvest.miningLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL) {
            HarvestXp.forBaseBatch(vein.oreName, baseYield.toLong())
        } else 0L
        val inventory = state.inventory.toMutableList()
        addToInventory(inventory, vein.oreName, yield)
        val (newLvl, newXp) = advanceProficiency(vein.proficiencyLevel, vein.proficiencyXp, rewardAmt)
        veins[idx] = vein.copy(miningProgress = 0f, isReady = false, proficiencyLevel = newLvl, proficiencyXp = newXp)

        val yieldStr = NumberFormatter.short(yield)
        val (xp, lvl, logs) = grantSkillXp(
            state.harvest.miningXp,
            state.harvest.miningLvl,
            skillXpReward,
            "Mining",
            "ORE EXTRACTED: Excavated ${yieldStr}x ${vein.oreName} (+${NumberFormatter.short(skillXpReward)} Mining XP, +${NumberFormatter.short(rewardAmt)} Proficiency)",
            xpRequirement = Balance::gatheringSkillXpReq,
            maxLevel = XpCurves.GATHERING_SKILL_MAX_LEVEL
        )
        state = state.copy(
            harvest = state.harvest.copy(veins = veins, miningXp = xp, miningLvl = lvl),
            inventory = inventory,
            lifetime = state.lifetime.copy(oresMined = state.lifetime.oresMined + 1L),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        evaluateAchievements(force = true)
        return state
    }

    fun togglePlotAutoQueue(plotId: Int): GameState {
        val current = state.harvest.plots.find { it.id == plotId }?.autoQueue ?: false
        val newTarget = !current
        val plots = state.harvest.plots.map {
            if (it.id == plotId) it.copy(autoQueue = newTarget)
            else it.copy(autoQueue = false)
        }
        state = state.copy(harvest = state.harvest.copy(plots = plots))
        return state
    }

    fun toggleVeinAutoQueue(veinId: Int): GameState {
        val current = state.harvest.veins.find { it.id == veinId }?.autoQueue ?: false
        val newTarget = !current
        val veins = state.harvest.veins.map {
            if (it.id == veinId) it.copy(autoQueue = newTarget)
            else it.copy(autoQueue = false)
        }
        state = state.copy(harvest = state.harvest.copy(veins = veins))
        return state
    }

    /** Loops skill level ups and returns the new xp/level plus the log entries to append. */
    private fun grantSkillXp(
        currentXp: Long,
        currentLvl: Int,
        amount: Long,
        skillName: String,
        successText: String,
        xpRequirement: (Int) -> Long = Balance::skillXpReq,
        maxLevel: Int = Int.MAX_VALUE
    ): Triple<Long, Int, List<LogEntry>> {
        val safeMaxLevel = maxLevel.coerceAtLeast(1)
        var lvl = currentLvl.coerceIn(1, safeMaxLevel)
        var xp = if (lvl >= safeMaxLevel) 0L else safeAddNonNegative(currentXp, amount)
        val logs = mutableListOf(log(LogType.LOOT, successText))
        while (lvl < safeMaxLevel && xp >= xpRequirement(lvl)) {
            xp -= xpRequirement(lvl)
            lvl++
            logs += log(LogType.SKILL, "SKILL LEVEL UP: $skillName reached Level $lvl!")
        }
        if (lvl >= safeMaxLevel) xp = 0L
        return Triple(xp, lvl, logs)
    }

    // ------------------------------------------------------------------ economy

    fun craftEquipment(name: String, tier: Int, slot: String, reqOre: String, reqQty: Int): GameState {
        val inventory = state.inventory.toMutableList()
        val oreIdx = inventory.indexOfFirst { it.name == reqOre }
        val owned = inventory.getOrNull(oreIdx)?.count ?: 0.0
        if (oreIdx < 0 || owned < reqQty) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "FORGE FAILED: requires ${reqQty}x $reqOre (have ${NumberFormatter.short(owned)})."
                )).takeLast(Balance.LOG_CAP)
            )
            return state
        }

        // "Max % of materials auto-crafters may use": the forge keeps a reserve instead of
        // consuming the last of a stack. At 0% no reserve is enforced.
        val usagePct = state.automation.craftMaterialUsagePct
        val remainingAfter = owned - reqQty
        val reserve = owned * (100 - usagePct) / 100.0
        if (usagePct > 0 && remainingAfter < reserve) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "FORGE REFUSED: $usagePct% material-usage cap would be exceeded for $reqOre."
                )).takeLast(Balance.LOG_CAP)
            )
            return state
        }

        val recycleRank = state.armory.upgrades.firstOrNull { it.id == "resource_saving" }?.rank ?: 0
        val conserved = recycleRank > 0 && random.nextDouble() < (0.12 * recycleRank).coerceAtMost(0.60)
        if (!conserved) {
            val ore = inventory[oreIdx]
            if (ore.count > reqQty) inventory[oreIdx] = ore.copy(count = ore.count - reqQty)
            else inventory.removeAt(oreIdx)
        }

        // forge_quality is applied once, in DerivedStats. Baking it into the item as well used to
        // double it, and left statDesc stale after every upgrade.
        val isWep = Equipment.isWeapon(slot)
        val atk = if (isWep) tier * 12.0 else 0.0
        val def = if (!isWep) tier * 8.0 else 0.0
        val vit = if (!isWep) tier * 4.0 else 0.0
        val statDesc = if (isWep) "+${atk.toInt()} ATK" else "+${def.toInt()} DEF, +${vit.toInt()} VIT"
        inventory.add(
            InventoryItem(
                id = "craft_${ids.next()}",
                name = name,
                category = ItemCatalog.CATEGORY_EQUIPMENT,
                count = 1,
                tier = tier,
                slot = slot,
                statDesc = statDesc,
                sellValue = tier * 35L,
                atk = atk,
                def = def,
                vit = vit
            )
        )

        val (xp, lvl, logs) = grantSkillXp(
            state.armory.smithingXp,
            state.armory.smithingLvl,
            50L,
            "Armory Smithing",
            "CYBER FORGE SUCCESS: Forged $name (Tier $tier)."
        )
        state = state.copy(
            inventory = inventory,
            armory = state.armory.copy(smithingXp = xp, smithingLvl = lvl),
            lifetime = state.lifetime.copy(forged = state.lifetime.forged + 1),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        if (state.automation.autoEquipBestGear) autoEquipBestGear()
        evaluateAchievements(force = true)
        return state
    }

    fun brewPotion(name: String, reqHerb: String, reqQty: Int): GameState {
        // The catalog is authoritative for a known potion; the caller only supplies the fallback.
        val recipe = ItemCatalog.potion(name)
        val herbName = if (recipe != null) {
            if (recipe.herb == ItemCatalog.HEALING_HERB && state.inventory.none { it.name == ItemCatalog.HEALING_HERB } && state.inventory.any { it.name == ItemCatalog.FELWEED }) {
                ItemCatalog.FELWEED
            } else recipe.herb
        } else reqHerb
        val herbCost = recipe?.herbCost ?: reqQty
        val inventory = state.inventory.toMutableList()
        val herbIdx = inventory.indexOfFirst { it.name == herbName }
        val owned = inventory.getOrNull(herbIdx)?.count ?: 0.0
        if (herbIdx < 0 || owned < herbCost) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "ALCHEMY FAILED: requires ${herbCost}x $herbName (have ${NumberFormatter.short(owned)})."
                )).takeLast(Balance.LOG_CAP)
            )
            return state
        }
        val herb = inventory[herbIdx]
        if (herb.count > herbCost) inventory[herbIdx] = herb.copy(count = herb.count - herbCost)
        else inventory.removeAt(herbIdx)

        // Brewed potions stack instead of piling up as separate one-count entries.
        val heal = ItemCatalog.potionHeal(name)
        val existing = inventory.indexOfFirst { it.name == name && it.category == ItemCatalog.CATEGORY_POTION }
        if (existing >= 0) {
            inventory[existing] = inventory[existing].copy(count = inventory[existing].count + 1)
        } else {
            inventory.add(
                InventoryItem(
                    id = "potion_${ids.next()}",
                    name = name,
                    category = ItemCatalog.CATEGORY_POTION,
                    count = 1,
                    tier = 1,
                    statDesc = "Restores ${heal.toInt()} HP",
                    sellValue = 15L
                )
            )
        }

        val (xp, lvl, logs) = grantSkillXp(
            state.armory.herbloreXp,
            state.armory.herbloreLvl,
            40L,
            "Herblore",
            "ALCHEMY SYNTHESIS: Brewed $name."
        )
        state = state.copy(
            inventory = inventory,
            armory = state.armory.copy(herbloreXp = xp, herbloreLvl = lvl),
            lifetime = state.lifetime.copy(brewed = state.lifetime.brewed + 1),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        evaluateAchievements(force = true)
        return state
    }

    fun sellItem(itemId: String, count: Int = 1): GameState {
        val inventory = state.inventory.toMutableList()
        val idx = inventory.indexOfFirst { it.id == itemId }
        if (idx < 0) return state
        val item = inventory[idx]
        // The engine refuses to sell worn gear; the UI hiding the button was not enough.
        if (item.isEquipped) {
            state = state.copy(
                logs = (state.logs + log(LogType.SYSTEM, "SALE REFUSED: ${item.name} is equipped."))
                    .takeLast(Balance.LOG_CAP)
            )
            return state
        }
        val actual = count.toDouble().coerceIn(1.0, item.count)
        val gold = (actual * item.sellValue).toLong()
        if (item.count > actual) inventory[idx] = item.copy(count = item.count - actual)
        else inventory.removeAt(idx)

        state = state.copy(
            player = state.player.copy(gold = state.player.gold + gold),
            inventory = inventory,
            logs = (state.logs + log(LogType.GAIN, "ITEM SOLD: ${NumberFormatter.short(actual)} x ${item.name} for +$gold CREDITS."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun equipItem(itemId: String): GameState {
        val inventory = state.inventory.toMutableList()
        val idx = inventory.indexOfFirst { it.id == itemId }
        if (idx < 0) return state
        val item = inventory[idx]
        if (item.category == ItemCatalog.CATEGORY_POTION) return usePotion(itemId)
        if (!Equipment.isEquipment(item) || item.slot.isBlank()) return state

        // Rings and amulets are real slots too; they used to have no equip branch at all.
        for (i in inventory.indices) {
            if (inventory[i].slot.equals(item.slot, ignoreCase = true) && inventory[i].isEquipped) {
                inventory[i] = inventory[i].copy(isEquipped = false)
            }
        }
        inventory[idx] = item.copy(isEquipped = true)
        state = state.copy(
            inventory = inventory,
            logs = (state.logs + log(LogType.SYSTEM, "GEAR EQUIPPED: ${item.name.uppercase()} -> ${item.slot}."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun unequipItem(itemId: String): GameState {
        val inventory = state.inventory.toMutableList()
        val idx = inventory.indexOfFirst { it.id == itemId }
        if (idx < 0 || !inventory[idx].isEquipped) return state
        val item = inventory[idx]
        inventory[idx] = item.copy(isEquipped = false)
        state = state.copy(
            inventory = inventory,
            logs = (state.logs + log(LogType.SYSTEM, "GEAR UNEQUIPPED: ${item.name.uppercase()}."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun usePotion(itemId: String): GameState {
        val inventory = state.inventory.toMutableList()
        val idx = inventory.indexOfFirst { it.id == itemId }
        if (idx < 0) return state
        val item = inventory[idx]
        if (item.category != ItemCatalog.CATEGORY_POTION) return state
        // A downed hero cannot drink, and a potion must not be consumed at full health.
        if (state.player.recoverLeft > 0.0) return state
        val derived = DerivedStats.of(state)
        if (state.player.hp >= derived.maxHp) {
            state = state.copy(
                logs = (state.logs + log(LogType.SYSTEM, "POTION HELD: already at full health."))
                    .takeLast(Balance.LOG_CAP)
            )
            return state
        }
        val potencyRank = state.armory.upgrades.firstOrNull { it.id == "alchemy_potency" }?.rank ?: 0
        val potency = 1.0 + 0.25 * potencyRank
        val heal = ItemCatalog.potionHeal(item.name) * potency
        val healed = (state.player.hp + heal).coerceAtMost(derived.maxHp)
        if (item.count > 1) inventory[idx] = item.copy(count = item.count - 1)
        else inventory.removeAt(idx)

        state = state.copy(
            player = state.player.copy(hp = healed),
            inventory = inventory,
            logs = (state.logs + log(LogType.GAIN, "POTION DEPLOYED: restored ${heal.toInt()} HP."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun autoEquipBestGear(): GameState {
        val inventory = state.inventory.toMutableList()
        var changed = false
        for (slot in Equipment.SLOTS) {
            val candidates = inventory.filter {
                Equipment.isEquipment(it) && it.slot.equals(slot, ignoreCase = true)
            }
            if (candidates.isEmpty()) continue
            // Uses the same tier fallback as DerivedStats instead of the raw atk field.
            val best = candidates.maxByOrNull {
                if (Equipment.isWeapon(slot)) {
                    Equipment.baseAttack(it)
                } else {
                    Equipment.baseDefense(it) + Equipment.baseVit(it)
                }
            } ?: continue
            if (best.isEquipped) continue
            for (i in inventory.indices) {
                if (inventory[i].slot.equals(slot, ignoreCase = true)) {
                    inventory[i] = inventory[i].copy(isEquipped = inventory[i].id == best.id)
                }
            }
            changed = true
        }
        if (changed) {
            state = state.copy(
                inventory = inventory,
                logs = (state.logs + log(LogType.SYSTEM, "AUTO-EQUIP: best available gear equipped."))
                    .takeLast(Balance.LOG_CAP)
            )
        }
        return state
    }

    fun summonMinion(type: String): GameState {
        val kind = Balance.minionTypeFor(type)
        val cost = Balance.minionCost(kind)
        val armySize = state.necro.army.sumOf { it.count }
        if (armySize >= Balance.MINION_CAP) {
            state = state.copy(
                logs = (state.logs + log(LogType.SYSTEM, "REANIMATION REFUSED: legion at ${Balance.MINION_CAP}/${Balance.MINION_CAP}."))
                    .takeLast(Balance.LOG_CAP)
            )
            return state
        }
        if (state.necro.boneEssence < cost) return state

        val army = state.necro.army.toMutableList()
        val existing = army.indexOfFirst { it.type == kind }
        if (existing >= 0) {
            army[existing] = army[existing].copy(count = army[existing].count + 1)
        } else {
            army.add(
                com.aistudio.trueidle.model.Minion(
                    id = "minion_${ids.next()}",
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
        evaluateAchievements(force = true)
        return state
    }

    fun mergeBeasts(beastId1: String, beastId2: String): GameState {
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

    fun trainNode(nodeId: String): GameState {
        val nodes = state.training.nodes.toMutableList()
        val idx = nodes.indexOfFirst { it.id == nodeId }
        if (idx < 0) return state
        val node = nodes[idx]
        if (node.rank >= node.maxRank || state.player.gold < node.costGold) return state

        val goldCost = node.costGold
        nodes[idx] = node.copy(rank = node.rank + 1, costGold = (node.costGold * 1.4).toLong())
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

    // ------------------------------------------------------------------ talents

    fun activateTalent(talentId: String): GameState {
        val talent = state.talent.activeTalents.firstOrNull { it.id == talentId } ?: return state
        if (talent.cooldownRemaining > 0.0) return state
        // No talent may be used while downed or waiting to respawn.
        if (state.player.recoverLeft > 0.0) return state
        if (respawnLeft > 0.0) return state

        val derived = DerivedStats.of(state)
        val manaCost = Balance.talentManaCost(talentId)
        if (state.player.mana < manaCost) return state

        val rank = talent.rank.coerceAtLeast(1)
        val rankScale = 1.0 + 0.1 * (rank - 1)
        var player = state.player.copy(mana = state.player.mana - manaCost)
        var monster = state.monster

        when (talentId) {
            "nanite_heal" -> {
                val healHp = derived.maxHp * 0.40 * rankScale
                val healShield = derived.maxShield * 0.25 * rankScale
                player = player.copy(
                    hp = (player.hp + healHp).coerceAtMost(derived.maxHp),
                    shield = (player.shield + healShield).coerceAtMost(derived.maxShield)
                )
            }
            "mana_rampage" -> {
                // A single strike, not a per-second figure, and armour still mitigates it.
                val burst = CombatMath.heroHitDamage(derived) * 2.5 * rankScale *
                    CombatMath.mitigation(monster.armor)
                var shield = monster.shield
                var hp = monster.hp
                if (shield > 0.0) {
                    val absorbed = burst.coerceAtMost(shield)
                    shield -= absorbed
                    hp -= (burst - absorbed)
                } else {
                    hp -= burst
                }
                monster = monster.copy(shield = shield.coerceAtLeast(0.0), hp = hp.coerceAtLeast(0.0))
            }
            "aegis_bastion" -> {
                val barrier = derived.maxShield * 0.60 * rankScale
                player = player.copy(shield = (player.shield + barrier).coerceAtMost(derived.maxShield * 1.5))
            }
            "phoenix_protocol" -> {
                player = player.copy(
                    hp = derived.maxHp,
                    shield = (player.shield + derived.maxShield * 0.40 * rankScale)
                        .coerceAtMost(derived.maxShield)
                )
            }
            "void_pulse" -> {
                val burst = CombatMath.heroHitDamage(derived) * 3.0 * rankScale *
                    CombatMath.mitigation(monster.armor)
                var shield = monster.shield
                var hp = monster.hp
                if (shield > 0.0) {
                    val absorbed = burst.coerceAtMost(shield)
                    shield -= absorbed
                    hp -= (burst - absorbed)
                } else {
                    hp -= burst
                }
                monster = monster.copy(shield = shield.coerceAtLeast(0.0), hp = hp.coerceAtLeast(0.0))
            }
            else -> Unit // Timed buffs are picked up by DerivedStats while active.
        }

        val duration = talent.durationSeconds * rankScale
        val cooldown = (talent.cooldownSeconds * (1.0 - 0.05 * (rank - 1))).coerceAtLeast(5.0)
        state = state.copy(
            player = player,
            monster = monster,
            talent = state.talent.copy(
                activeTalents = state.talent.activeTalents.map {
                    if (it.id == talentId) {
                        it.copy(
                            isActive = talent.durationSeconds > 0.0,
                            durationRemaining = duration,
                            cooldownRemaining = cooldown
                        )
                    } else it
                }
            ),
            logs = (state.logs + log(LogType.MILESTONE, "TALENT DEPLOYED: ${talent.name} (Rank $rank)."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun upgradeTalent(talentId: String): GameState {
        if (state.talent.talentPoints <= 0) return state
        val active = state.talent.activeTalents.firstOrNull { it.id == talentId }
        if (active != null) {
            if (active.rank >= active.maxRank) return state
            state = state.copy(
                talent = state.talent.copy(
                    talentPoints = (state.talent.talentPoints - active.costPoints).coerceAtLeast(0),
                    activeTalents = state.talent.activeTalents.map {
                        if (it.id == talentId) it.copy(rank = it.rank + 1) else it
                    }
                )
            )
            return state
        }
        val struggle = state.talent.struggleTalents.firstOrNull { it.id == talentId } ?: return state
        if (struggle.rank >= struggle.maxRank) return state
        state = state.copy(
            talent = state.talent.copy(
                talentPoints = (state.talent.talentPoints - struggle.costPoints).coerceAtLeast(0),
                struggleTalents = state.talent.struggleTalents.map {
                    if (it.id == talentId) it.copy(rank = it.rank + 1) else it
                }
            )
        )
        return state
    }

    // ------------------------------------------------------------------ disciplines

    fun upgradeDiscipline(category: DisciplineCategory, upgradeId: String): GameState {
        val upgrades: List<DisciplineUpgrade> = when (category) {
            DisciplineCategory.HARVEST -> state.harvest.upgrades
            DisciplineCategory.ARMORY -> state.armory.upgrades
            DisciplineCategory.GENESIS_BEAST -> state.beasts.upgrades
            DisciplineCategory.GENESIS_NECRO -> state.necro.upgrades
            DisciplineCategory.ESSENCE -> state.essence.upgrades
        }
        val upgrade = upgrades.firstOrNull { it.id == upgradeId } ?: return state
        if (upgrade.rank >= upgrade.maxRank) return state
        val cost = Balance.upgradeCost(upgrade)
        val currency = upgrade.costCurrency.uppercase()
        val available = when (currency) {
            "FAITH" -> state.essence.faith
            "BONE" -> state.necro.boneEssence
            "SLAYER" -> state.slayer.slayerPoints.toLong()
            else -> state.player.gold
        }
        if (available < cost) return state

        val updated = upgrades.map { if (it.id == upgradeId) it.copy(rank = it.rank + 1) else it }
        state = when (category) {
            DisciplineCategory.HARVEST -> state.copy(
                player = if (currency == "GOLD") state.player.copy(gold = state.player.gold - cost) else state.player,
                harvest = state.harvest.copy(upgrades = updated)
            )
            DisciplineCategory.ARMORY -> state.copy(
                player = if (currency == "GOLD") state.player.copy(gold = state.player.gold - cost) else state.player,
                armory = state.armory.copy(upgrades = updated)
            )
            DisciplineCategory.GENESIS_BEAST -> state.copy(
                player = if (currency == "GOLD") state.player.copy(gold = state.player.gold - cost) else state.player,
                beasts = state.beasts.copy(upgrades = updated)
            )
            DisciplineCategory.GENESIS_NECRO -> state.copy(
                necro = state.necro.copy(boneEssence = state.necro.boneEssence - cost, upgrades = updated)
            )
            DisciplineCategory.ESSENCE -> state.copy(
                essence = state.essence.copy(faith = state.essence.faith - cost, upgrades = updated)
            )
        }
        state = state.copy(
            logs = (state.logs + log(LogType.MILESTONE, "UPGRADE COMPLETE: ${upgrade.name} -> Rank ${upgrade.rank + 1}."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun toggleEssenceAegis(): GameState {
        val next = !state.essence.aegisActive
        if (next && state.essence.faith <= 0L) return state
        state = state.copy(essence = state.essence.copy(aegisActive = next))
        return state
    }

    fun upgradeEssenceSmite(): GameState {
        val cost = Balance.smiteUpgradeCost(state.essence.smiteLevel)
        if (state.essence.faith < cost) return state
        state = state.copy(
            essence = state.essence.copy(
                faith = state.essence.faith - cost,
                smiteLevel = state.essence.smiteLevel + 1
            )
        )
        return state
    }

    fun upgradeDiscipline(category: String, upgradeId: String): GameState =
        DisciplineCategory.entries.firstOrNull { it.name == category.uppercase() }
            ?.let { upgradeDiscipline(it, upgradeId) }
            ?: state

    // ------------------------------------------------------------------ slayer

    fun rerollSlayerTask(): GameState {
        val task = state.slayer.currentTask
        // Fulfilling a completed directive is free; rerolling an open one costs a point.
        if (!task.isCompleted) {
            if (state.slayer.slayerPoints < 1) return state
        }
        val next = newSlayerTask()
        state = state.copy(
            slayer = state.slayer.copy(
                slayerPoints = if (task.isCompleted) state.slayer.slayerPoints else state.slayer.slayerPoints - 1,
                currentTask = next
            ),
            logs = (state.logs + log(LogType.SYSTEM, "SLAYER DIRECTIVE: new target ${next.targetName}."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    /**
     * Picks a target the player can actually reach: the floor window is clamped to the unlocked
     * range, so the directive is never impossible.
     */
    private fun newSlayerTask(): com.aistudio.trueidle.model.SlayerTask {
        val reachable = state.player.maxUnlockedFloor.coerceIn(1, Balance.MAX_FLOOR)
        val offset = random.nextInt(-3, 4) // symmetric window
        val floor = (state.player.currentFloor + offset).coerceIn(1, reachable)
        val monster = Balance.generateMonsterForFloor(floor)
        return com.aistudio.trueidle.model.SlayerTask(
            targetName = monster.name,
            currentKills = 0,
            requiredKills = random.nextInt(10, 21),
            rewardGold = SlayerMath.scaledGold((floor * 25L).coerceAtLeast(100L), state.slayer.lvl),
            rewardPoints = SlayerMath.scaledPoints((floor / 5 + 3).coerceIn(3, 15), state.slayer.lvl),
            isCompleted = false
        )
    }

    // ------------------------------------------------------------------ misc actions

    fun navFloor(delta: Int): GameState {
        if (state.player.recoverLeft > 0.0) return state
        val target = (state.player.currentFloor + delta).coerceIn(1, reachableFloor())
        if (target == state.player.currentFloor) return state
        return moveToFloor(target, "SECTOR TRANSFER: traversing to Sector $target.")
    }

    fun setFloor(floor: Int): GameState {
        if (state.player.recoverLeft > 0.0) return state
        return moveToFloor(floor.coerceIn(1, reachableFloor()), null)
    }

    private fun reachableFloor(): Int =
        state.player.maxUnlockedFloor.coerceIn(1, Balance.MAX_FLOOR)

    /**
     * Floor changes always consume a spawn gap and clear the death streak, so hopping floors cannot
     * be used to dodge damage or reset a boss for free.
     */
    private fun moveToFloor(target: Int, logText: String?): GameState {
        respawnLeft = Balance.RESPAWN_GAP_SECONDS
        state = state.copy(
            player = state.player.copy(
                currentFloor = target,
                killsOnFloor = 0,
                failuresInRow = 0,
                killsRequired = Balance.killsRequiredFor(target)
            ),
            monster = Balance.generateMonsterForFloor(target),
            logs = if (logText != null) {
                (state.logs + log(LogType.SYSTEM, logText)).takeLast(Balance.LOG_CAP)
            } else state.logs
        )
        return state
    }

    fun toggleAutoAdvance(): GameState {
        state = state.copy(player = state.player.copy(autoAdvance = !state.player.autoAdvance))
        return state
    }

    fun toggleAutoCollect(): GameState {
        if (!state.harvest.autoCollectUnlocked) return state
        state = state.copy(harvest = state.harvest.copy(autoCollectEnabled = !state.harvest.autoCollectEnabled))
        return state
    }

    fun toggleAutoEquipBestGear(): GameState {
        val next = !state.automation.autoEquipBestGear
        state = state.copy(automation = state.automation.copy(autoEquipBestGear = next))
        if (next) autoEquipBestGear()
        return state
    }

    fun toggleAutoMergeBeasts(): GameState {
        val next = !state.automation.autoMergeBeasts
        state = state.copy(automation = state.automation.copy(autoMergeBeasts = next))
        if (next) {
            val frame = Frame(state)
            autoMergeBeasts(frame)
            frame.commit(this)
        }
        return state
    }

    fun toggleRunInBackground(): GameState {
        state = state.copy(
            automation = state.automation.copy(runInBackground = !state.automation.runInBackground)
        )
        return state
    }

    fun setCraftMaterialUsage(pct: Int): GameState {
        state = state.copy(automation = state.automation.copy(craftMaterialUsagePct = pct.coerceIn(0, 100)))
        return state
    }

    /**
     * Switches the large-number notation for the whole game and persists the choice in the save.
     *
     * Only the display changes: stored values, offline rewards and combat maths are untouched.
     */
    fun setNotation(notation: String): GameState {
        val applied = applyNotation(notation)
        state = state.copy(display = state.display.copy(notation = applied.mode.name))
        return state
    }

    private fun applyNotation(notation: String): BigNumberFormatter.Config {
        val config = BigNumberFormatter.Config.byName(notation)
        BigNumberFormatter.setConfig(config)
        return config
    }

    fun fightPromotionBoss(): GameState {
        val promotion = state.promotion
        val derived = DerivedStats.of(state)
        // Uses the deepest unlocked floor, not the floor the hero happens to stand on.
        val floorOk = state.player.maxUnlockedFloor >= promotion.requiredFloor
        val gearOk = derived.gearScore >= promotion.requiredGearScore
        if (!floorOk || !gearOk) return state

        val nextTier = (promotion.tier + 1).coerceAtMost(promotion.maxTier)
        if (nextTier == promotion.tier) return state
        val nextBonus = promotion.bonusPercent + 8

        state = state.copy(
            promotion = promotion.copy(
                tier = nextTier,
                tierName = Balance.tierName(nextTier),
                requiredFloor = Balance.requiredFloorFor(nextTier + 1),
                requiredGearScore = Balance.requiredGearScoreFor(nextTier + 1),
                bonusPercent = nextBonus
            ),
            logs = (state.logs + log(
                LogType.MILESTONE,
                "PROMOTION ACHIEVED: ascended to ${Balance.tierName(nextTier)} (+$nextBonus% all stats)."
            )).takeLast(Balance.LOG_CAP)
        )
        return state
    }

    fun claimAchievement(achId: String): GameState {
        val achievements = state.achievements.toMutableList()
        val idx = achievements.indexOfFirst { it.id == achId }
        if (idx < 0) return state
        val achievement = achievements[idx]
        if (!achievement.isUnlocked || achievement.isClaimed) return state
        achievements[idx] = achievement.copy(isClaimed = true)
        state = state.copy(
            achievements = achievements,
            player = state.player.copy(gold = state.player.gold + achievement.rewardGold)
        )
        return state
    }

    fun clearLogs(): GameState {
        state = state.copy(logs = emptyList())
        return state
    }

    fun exportSaveJson(): String = saves.exportText(snapshotForSave(nowSnapshot()))

    fun importSaveJson(jsonString: String): Boolean {
        val imported = saves.decode(jsonString.trim().toByteArray(Charsets.UTF_8)) ?: return false
        restore(imported)
        state = state.copy(
            pendingOfflineLoot = null,
            logs = (state.logs + log(LogType.SYSTEM, "SAVE RESTORED: game state imported from archive."))
                .takeLast(Balance.LOG_CAP)
        )
        return true
    }

    fun resetGame(): GameState {
        restore(GameState())
        state = state.copy(
            logs = (state.logs + log(LogType.SYSTEM, "SYSTEM RESET: factory protocol restored."))
                .takeLast(Balance.LOG_CAP)
        )
        return state
    }

    // ------------------------------------------------------------------ achievements

    /**
     * Recomputes progress only for achievements whose underlying counter changed, so a 4 Hz tick no
     * longer rebuilds the whole list (and the whole state object) on every frame.
     */
    private fun evaluateAchievements(force: Boolean = false) {
        // Called every tick: bail out before allocating a new list when nothing moved.
        if (!force && state.achievements.all { a ->
                a.isUnlocked ||
                    (minOf(progressFor(a.id), a.target) == a.progress && a.progress < a.target)
            }
        ) return
        var changed = false
        val updated = state.achievements.map { achievement ->
            if (achievement.isUnlocked) {
                achievement
            } else {
                val original = achievement.progress
                val progress = minOf(progressFor(achievement.id), achievement.target)
                val unlocked = progress >= achievement.target
                if (!force && progress == original && !unlocked) {
                    achievement
                } else {
                    if (progress != original || unlocked) changed = true
                    achievement.copy(progress = progress, isUnlocked = unlocked)
                }
            }
        }
        if (changed) state = state.copy(achievements = updated)
    }

    private fun progressFor(id: String): Int = when (id) {
        "ach_first_blood" -> state.lifetime.totalKills.toInt()
        "ach_floor_10" -> state.player.maxUnlockedFloor
        // The starting Dusk Wolf counts; the old lifetime-only figure never included it.
        "ach_tamer" -> state.lifetime.beastsTamed.toInt() + 1
        "ach_necro" -> state.necro.army.sumOf { it.count }
        "ach_miner" -> state.lifetime.oresMined.toInt()
        else -> 0
    }

    // ------------------------------------------------------------------ save helpers

    companion object {
        private val ID_SUFFIX = Regex("""[_-](\d+)$""")

        /** Highest issued id across logs, inventory, beasts and minions, plus the reserved floor. */
        fun nextIdFrom(source: GameState): Long {
            var max = Balance.RESERVED_ID_FLOOR - 1L
            source.logs.forEach { max = maxOf(max, it.id) }
            fun scan(ids: List<String>) {
                ids.forEach { id ->
                    ID_SUFFIX.find(id)?.groupValues?.get(1)?.toLongOrNull()?.let { max = maxOf(max, it) }
                }
            }
            scan(source.inventory.map { it.id })
            scan(source.beasts.beasts.map { it.id })
            scan(source.necro.army.map { it.id })
            return maxOf(max + 1L, source.meta.nextId, Balance.RESERVED_ID_FLOOR)
        }

        /** Clamps a loaded or imported state into a legal, self-consistent shape. */
        fun sanitize(input: GameState): GameState {
            val maxFloor = input.player.maxUnlockedFloor.coerceIn(1, Balance.MAX_FLOOR)
            val currentFloor = input.player.currentFloor.coerceIn(1, maxFloor)
            val required = Balance.killsRequiredFor(currentFloor)

            val player = input.player.copy(
                lvl = input.player.lvl.coerceAtLeast(1),
                xp = input.player.xp.coerceAtLeast(0L),
                gold = input.player.gold.coerceAtLeast(0L),
                diamonds = input.player.diamonds.coerceAtLeast(0L),
                hp = input.player.hp.coerceAtLeast(0.0),
                shield = input.player.shield.coerceAtLeast(0.0),
                mana = input.player.mana.coerceAtLeast(0.0),
                currentFloor = currentFloor,
                maxUnlockedFloor = maxFloor,
                killsOnFloor = input.player.killsOnFloor.coerceIn(0, required),
                killsRequired = required,
                recoverLeft = input.player.recoverLeft.coerceIn(0.0, Balance.RECOVERY_SECONDS),
                failuresInRow = input.player.failuresInRow.coerceAtLeast(0)
            )

            val army = mutableListOf<com.aistudio.trueidle.model.Minion>()
            var remaining = Balance.MINION_CAP
            for (minion in input.necro.army) {
                if (remaining <= 0) break
                val count = minion.count.coerceIn(1, remaining)
                remaining -= count
                army += minion.copy(count = count, lvl = minion.lvl.coerceAtLeast(1))
            }

            fun clampUpgrades(list: List<DisciplineUpgrade>) =
                list.map { it.copy(rank = it.rank.coerceIn(0, it.maxRank)) }

            val sanitizedInventory = run {
                val seenEquippedSlots = mutableSetOf<String>()
                input.inventory.map { item ->
                    val migratedSlot = when (item.slot) {
                        Equipment.SLOT_WEAPON -> Equipment.SLOT_RIFLE
                        Equipment.SLOT_ARMOR -> Equipment.SLOT_VEST
                        Equipment.SLOT_RING -> Equipment.SLOT_NODE_I
                        Equipment.SLOT_AMULET -> Equipment.SLOT_NODE_II
                        else -> item.slot
                    }
                    val isEquip = Equipment.isEquipment(item) || Equipment.GROUPS.any { it.equals(migratedSlot, ignoreCase = true) }
                    if (isEquip) {
                        val isWep = Equipment.isWeapon(migratedSlot)
                        val atk = if (isWep) (if (item.atk > 0.0) item.atk else item.tier * 18.0) else 0.0
                        val def = if (!isWep) (if (item.def > 0.0) item.def else item.tier * 8.0) else 0.0
                        val vit = if (!isWep) (if (item.vit > 0.0) item.vit else item.tier * 4.0) else 0.0
                        val statDesc = if (isWep) "+${atk.toInt()} ATK" else "+${def.toInt()} DEF, +${vit.toInt()} VIT"
                        val equipped = item.isEquipped && seenEquippedSlots.add(migratedSlot.lowercase())
                        item.copy(
                            slot = migratedSlot,
                            atk = atk,
                            def = def,
                            vit = vit,
                            statDesc = statDesc,
                            isEquipped = equipped
                        )
                    } else {
                        item
                    }
                }
            }

            return input.copy(
                player = player,
                monster = if (input.monster.hp <= 0.0) Balance.generateMonsterForFloor(currentFloor) else input.monster,
                inventory = sanitizedInventory,
                necro = input.necro.copy(
                    lvl = input.necro.lvl.coerceAtLeast(1),
                    xp = input.necro.xp.coerceAtLeast(0L),
                    boneEssence = input.necro.boneEssence.coerceAtLeast(0L),
                    army = army,
                    upgrades = clampUpgrades(input.necro.upgrades)
                ),
                harvest = input.harvest.copy(
                    farmingLvl = input.harvest.farmingLvl.coerceAtLeast(1),
                    farmingXp = input.harvest.farmingXp.coerceAtLeast(0L),
                    miningLvl = input.harvest.miningLvl.coerceAtLeast(1),
                    miningXp = input.harvest.miningXp.coerceAtLeast(0L),
                    plots = run {
                        val firstAuto = input.harvest.plots.indexOfFirst { it.autoQueue }
                        val basePlots = if (input.harvest.plots.size == ItemCatalog.HERBS.size &&
                            input.harvest.plots.map { it.herbName } == ItemCatalog.HERBS) {
                            input.harvest.plots
                        } else {
                            ItemCatalog.HERBS.mapIndexed { idx, herb ->
                                val existing = input.harvest.plots.find { it.herbName.equals(herb, ignoreCase = true) }
                                Plot(
                                    id = idx,
                                    herbName = herb,
                                    growthProgress = existing?.growthProgress ?: 0f,
                                    durationSeconds = 10f + idx * 3f,
                                    isReady = existing?.isReady ?: false,
                                    proficiencyLevel = existing?.proficiencyLevel ?: 1,
                                    proficiencyXp = existing?.proficiencyXp ?: 0L,
                                    autoQueue = existing?.autoQueue ?: false
                                )
                            }
                        }
                        basePlots.mapIndexed { idx, p ->
                            p.copy(
                                growthProgress = p.growthProgress.coerceIn(0f, 1f),
                                autoQueue = idx == firstAuto,
                                proficiencyLevel = p.proficiencyLevel.coerceIn(1, 100),
                                proficiencyXp = p.proficiencyXp.coerceAtLeast(0L)
                            )
                        }
                    },
                    veins = run {
                        val firstAuto = input.harvest.veins.indexOfFirst { it.autoQueue }
                        val baseVeins = if (input.harvest.veins.size == ItemCatalog.ORES.size &&
                            input.harvest.veins.map { it.oreName } == ItemCatalog.ORES) {
                            input.harvest.veins
                        } else {
                            ItemCatalog.ORES.mapIndexed { idx, ore ->
                                val existing = input.harvest.veins.find { it.oreName.equals(ore, ignoreCase = true) }
                                MiningVein(
                                    id = idx,
                                    oreName = ore,
                                    reqLvl = 1,
                                    miningProgress = existing?.miningProgress ?: 0f,
                                    durationSeconds = 8f + idx * 2.5f,
                                    isReady = existing?.isReady ?: false,
                                    proficiencyLevel = existing?.proficiencyLevel ?: 1,
                                    proficiencyXp = existing?.proficiencyXp ?: 0L,
                                    autoQueue = existing?.autoQueue ?: false
                                )
                            }
                        }
                        baseVeins.mapIndexed { idx, v ->
                            v.copy(
                                miningProgress = v.miningProgress.coerceIn(0f, 1f),
                                reqLvl = 1,
                                autoQueue = idx == firstAuto,
                                proficiencyLevel = v.proficiencyLevel.coerceIn(1, 100),
                                proficiencyXp = v.proficiencyXp.coerceAtLeast(0L)
                            )
                        }
                    },
                    upgrades = clampUpgrades(input.harvest.upgrades)
                ),
                armory = input.armory.copy(
                    smithingLvl = input.armory.smithingLvl.coerceAtLeast(1),
                    herbloreLvl = input.armory.herbloreLvl.coerceAtLeast(1),
                    smithingXp = input.armory.smithingXp.coerceAtLeast(0L),
                    herbloreXp = input.armory.herbloreXp.coerceAtLeast(0L),
                    upgrades = clampUpgrades(input.armory.upgrades)
                ),
                training = input.training.copy(
                    lvl = input.training.lvl.coerceAtLeast(1),
                    xp = input.training.xp.coerceAtLeast(0L),
                    nodes = input.training.nodes.map { it.copy(rank = it.rank.coerceIn(0, it.maxRank)) }
                ),
                essence = input.essence.copy(
                    lvl = input.essence.lvl.coerceAtLeast(1),
                    xp = input.essence.xp.coerceAtLeast(0L),
                    faith = input.essence.faith.coerceAtLeast(0L),
                    faithRemainder = input.essence.faithRemainder.coerceIn(0.0, 1.0),
                    smiteLevel = input.essence.smiteLevel.coerceAtLeast(1),
                    upgrades = clampUpgrades(input.essence.upgrades)
                ),
                beasts = input.beasts.copy(
                    lvl = input.beasts.lvl.coerceAtLeast(1),
                    xp = input.beasts.xp.coerceAtLeast(0L),
                    beasts = input.beasts.beasts.take(Balance.BEAST_CAP),
                    upgrades = clampUpgrades(input.beasts.upgrades)
                ),
                talent = input.talent.copy(
                    talentPoints = input.talent.talentPoints.coerceAtLeast(0),
                    struggleDamageTaken = input.talent.struggleDamageTaken.coerceAtLeast(0L),
                    struggleDamageFraction = input.talent.struggleDamageFraction.coerceIn(0.0, 1.0),
                    activeTalents = clampTalents(input.talent.activeTalents),
                    struggleTalents = clampTalents(input.talent.struggleTalents)
                ),
                slayer = input.slayer.copy(
                    lvl = input.slayer.lvl.coerceAtLeast(1),
                    xp = input.slayer.xp.coerceAtLeast(0L),
                    slayerPoints = input.slayer.slayerPoints.coerceAtLeast(0),
                    currentTask = input.slayer.currentTask.copy(
                        currentKills = input.slayer.currentTask.currentKills.coerceAtLeast(0),
                        requiredKills = input.slayer.currentTask.requiredKills.coerceAtLeast(1)
                    )
                ),
                promotion = input.promotion.copy(
                    tier = input.promotion.tier.coerceIn(1, input.promotion.maxTier),
                    bonusPercent = input.promotion.bonusPercent.coerceAtLeast(0),
                    requiredGearScore = input.promotion.requiredGearScore.coerceAtLeast(0)
                ),
                automation = input.automation.copy(
                    craftMaterialUsagePct = input.automation.craftMaterialUsagePct.coerceIn(0, 100)
                ),
                meta = input.meta.copy(
                    nextId = nextIdFrom(input).coerceAtLeast(Balance.RESERVED_ID_FLOOR)
                )
            )
        }

        private fun clampTalents(list: List<Talent>) = list.map {
            it.copy(
                rank = it.rank.coerceIn(0, it.maxRank),
                cooldownRemaining = it.cooldownRemaining.coerceAtLeast(0.0),
                durationRemaining = it.durationRemaining.coerceAtLeast(0.0)
            )
        }
    }
}
