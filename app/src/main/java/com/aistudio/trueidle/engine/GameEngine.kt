package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.BigNumberFormatter
import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.DailyLoginDetector
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.GameClock
import com.aistudio.trueidle.core.HarvestXp
import com.aistudio.trueidle.core.Ids
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.data.NoOpSaveStore
import com.aistudio.trueidle.data.SaveRepository
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.EssenceState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.LogEntry
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

    private val slayerSystem = SlayerSystem(random, ::log)

    init {
        state = state.copy(
            slayer = state.slayer.copy(
                tasks = ensureSlayerTasks(state.slayer.tasks),
                currentTask = state.slayer.tasks.firstOrNull() ?: state.slayer.currentTask
            )
        )
    }

    private val ids = Ids(nextIdFrom(initialState))
    private val potions = PotionSystem(ids::next)
    private val harvestSystem = HarvestSystem(ids::next, ::log)
    private val lootSystem = LootSystem(random, ids::next, ::log)
    private val offlineClaim = OfflineClaimSystem(
        nextId = ids::next,
        logger = ::log,
        ensureSlayerTasks = { tasks -> ensureSlayerTasks(tasks) },
        freshSlayerTask = { id -> newSlayerTask(id) },
        createBeast = lootSystem::tamedBeast
    )

    private val combat = CombatResolution(
        random = random,
        logger = ::log,
        loot = lootSystem,
        deployPotion = { frame, name -> potions.apply(frame, name) },
        ensureSlayerTasks = { tasks -> ensureSlayerTasks(tasks) },
        refreshTask = { id -> newSlayerTask(id) },
        setRespawnGap = { gap -> respawnLeft = gap }
    )

    private val economy = EconomySystem(
        logger = ::log,
        deployPotion = { frame, name -> potions.apply(frame, name) }
    )

    private val crafting = CraftingSystem(
        random = random,
        nextId = ids::next,
        logger = ::log,
        grantSkillXp = { xp, lvl, amount, skill, text -> grantSkillXp(xp, lvl, amount, skill, text) },
        autoEquip = { s -> economy.autoEquipBestGear(s) }
    )
    private val progression = ProgressionSystem(
        nextId = ids::next,
        logger = ::log,
        grantSkillXp = { xp, lvl, amount, skill, text -> grantSkillXp(xp, lvl, amount, skill, text) }
    )
    private val devTools = DevTools()

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
        state = state.copy(
            display = state.display.copy(notation = applyNotation(state.display.notation).mode.name),
            slayer = state.slayer.copy(
                tasks = ensureSlayerTasks(state.slayer.tasks),
                currentTask = state.slayer.tasks.firstOrNull() ?: state.slayer.currentTask
            )
        )
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
        checkDayRollover()
    }

    private fun checkDayRollover() {
        val nowMs = nowWallMs()
        val currentDay = DailyLoginDetector.currentEpochDay(nowMs)
        val daily = state.dailyLogin
        if (daily.lastLoginEpochDay != currentDay) {
            val isFirstInit = daily.lastLoginEpochDay == -1L
            state = state.copy(
                dailyLogin = daily.copy(
                    lastLoginEpochDay = currentDay,
                    totalLogins = if (isFirstInit) daily.totalLogins else daily.totalLogins + 1
                ),
                logs = if (!isFirstInit) {
                    (state.logs + log(LogType.MILESTONE, "A NEW DAY HAS ARRIVED: Daily login rewards are available to claim!"))
                        .takeLast(Balance.LOG_CAP)
                } else state.logs
            )
        }
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
    state = offlineClaim.claim(state, loot)
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

    fun tick(deltaSeconds: Float): GameState {
        checkDayRollover()
        // Dev speed scales simulated time for every subsystem (combat, regeneration, harvesting).
        val speed = state.dev.gameSpeed.coerceIn(DEV_MIN_SPEED, DEV_MAX_SPEED)
        val scaledSeconds = deltaSeconds * speed
        val dt = scaledSeconds.toDouble()
        val frame = Frame(state)
        var derivedCache: Derived? = null

        // Subsystems that must keep running while the hero is down or waiting to respawn.
        tickPlayTime(frame, dt)
        tickBuffs(frame, dt)
        tickTalentTimers(frame, dt)
        autoTriggerTalents(frame)
        tickRegeneration(frame, dt)
        tickAegisDrain(frame, dt)
        harvestSystem.tick(frame, scaledSeconds)

        if (frame.player.recoverLeft > 0.0) {
            tickRecovery(frame, dt)
            state = frame.commit()
            return state
        }

        if (respawnLeft > 0.0) {
            respawnLeft = (respawnLeft - dt).coerceAtLeast(0.0)
            state = frame.commit()
            return state
        }

        tickCombat(frame, dt)
        state = frame.commit()
        if (frame.logs.isNotEmpty()) evaluateAchievements()
        return state
    }

    private fun tickBuffs(frame: Frame, dt: Double) {
        if (frame.activeBuffs.isEmpty()) return
        val remaining = mutableListOf<com.aistudio.trueidle.model.ActiveBuff>()
        var changed = false
        for (b in frame.activeBuffs) {
            val dur = b.durationRemaining - dt
            if (dur > 0.0) {
                remaining.add(b.copy(durationRemaining = dur))
            } else {
                changed = true
            }
        }
        frame.activeBuffs = remaining
        if (changed) frame.invalidateDerived()
    }

    private fun autoTriggerTalents(frame: Frame) {
    if (frame.player.recoverLeft > 0.0 || respawnLeft > 0.0) return
    val d = frame.derived()
    var player = frame.player
    var monster = frame.monster
    var talentsChanged = false

    val updatedTalents = frame.talent.activeTalents.map { talent ->
        if (talent.rank > 0 && talent.cooldownRemaining <= 0.0) {
            val manaCost = Balance.talentManaCost(talent.id)
            if (player.mana >= manaCost) {
                talentsChanged = true
                player = player.copy(mana = player.mana - manaCost)
                val rank = talent.rank.coerceAtLeast(1)
                val effect = TalentSystem.apply(talent.id, rank, d, player, monster)
                player = effect.player
                monster = effect.monster

                talent.copy(
                    isActive = talent.durationSeconds > 0.0,
                    durationRemaining = TalentSystem.durationSeconds(talent, rank),
                    cooldownRemaining = TalentSystem.cooldownSeconds(talent, rank)
                )
            } else talent
        } else talent
    }

    if (talentsChanged) {
        frame.player = player
        frame.monster = monster
        frame.talent = frame.talent.copy(activeTalents = updatedTalents)
        frame.invalidateDerived()
    }
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
    frame.talent = frame.talent.copy(activeTalents = TalentSystem.tickTimers(talents, dt))
}

    private fun tickRegeneration(frame: Frame, dt: Double) {
        if (frame.player.recoverLeft > 0.0) return // a downed hero does not regenerate
        val d = frame.derived()
        frame.player = frame.player.copy(
            hp = (frame.player.hp + d.healthRegen * dt).coerceAtMost(d.maxHp),
            shield = (frame.player.shield + d.shieldRegen * dt).coerceAtMost(d.maxShield),
            mana = (frame.player.mana + d.manaRegen * dt).coerceAtMost(d.maxMana)
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

        private fun tickCombat(frame: Frame, dt: Double) = combat.tick(frame, dt)

                private fun grantFaith(essence: EssenceState, amount: Long): EssenceState =
        essence.copy(faith = essence.faith + amount)

            private fun autoMergeBeasts(frame: Frame) =
        lootSystem.autoMergeBeasts(frame)

    private fun addToInventory(inventory: MutableList<InventoryItem>, name: String, qty: Double) =
        InventoryOps.add(inventory, name, qty, ids::next)

    private fun addToInventory(inventory: MutableList<InventoryItem>, name: String, qty: Int) =
        addToInventory(inventory, name, qty.toDouble())

    private fun advanceProficiency(currentLvl: Int, currentXp: Long, gain: Long): Pair<Int, Long> =
        ProgressOps.advanceProficiency(currentLvl, currentXp, gain)

    private fun safeAddNonNegative(current: Double, amount: Double): Double =
        ProgressOps.safeAddNonNegative(current, amount)

    private fun grantGatheringSkillXp(
        currentXp: Double,
        currentLvl: Int,
        amount: Double,
        skillName: String,
        successText: String,
        xpRequirement: (Int) -> Double
    ): Triple<Double, Int, List<LogEntry>> {
        val maxLevel = XpCurves.GATHERING_SKILL_MAX_LEVEL
        var lvl = currentLvl.coerceIn(1, maxLevel)
        var xp = if (lvl >= maxLevel) 0.0 else safeAddNonNegative(currentXp, amount)
        val logs = mutableListOf(log(LogType.LOOT, successText))
        while (lvl < maxLevel && xp >= xpRequirement(lvl)) {
            xp -= xpRequirement(lvl)
            lvl++
            logs += log(LogType.SKILL, "CLASS LEVEL UP: $skillName reached Level $lvl!")
        }
        if (lvl >= maxLevel) xp = 0.0
        return Triple(xp, lvl, logs)
    }

    // ------------------------------------------------------------------ harvest
    // ------------------------------------------------------------------ harvest

    fun harvestPlot(plotId: Int): GameState {
        val plots = state.harvest.plots.toMutableList()
        val idx = plots.indexOfFirst { it.id == plotId }
        if (idx < 0 || state.harvest.farmingLvl < plots[idx].reqLvl || !plots[idx].isReady) return state
        val plot = plots[idx]
        val extraYield = state.harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
        val yield = (2.0 + extraYield) * XpCurves.proficiencyYieldMultiplier(plot.proficiencyLevel)
        val proficiencyGain = yield.toLong().coerceAtLeast(1L)
        val skillXpReward = if (state.harvest.farmingLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL) HarvestXp.forYield(plot.herbName, yield) else 0.0

        val inventory = state.inventory.toMutableList()
        addToInventory(inventory, plot.herbName, yield)
        val (newProfLvl, newProfXp) = advanceProficiency(plot.proficiencyLevel, plot.proficiencyXp, proficiencyGain)
        plots[idx] = plot.copy(growthProgress = 0f, isReady = false, proficiencyLevel = newProfLvl, proficiencyXp = newProfXp)

        val yieldStr = NumberFormatter.short(yield)
        val (xp, lvl, logs) = grantGatheringSkillXp(
            state.harvest.farmingXp,
            state.harvest.farmingLvl,
            skillXpReward,
            "Farming",
            "BIO-HARVEST: Collected ${yieldStr}x ${plot.herbName} (+${NumberFormatter.short(skillXpReward)} Farming XP, +${NumberFormatter.short(proficiencyGain)} Proficiency)",
            xpRequirement = Balance::farmingSkillXpReq
        )
        state = state.copy(
            harvest = state.harvest.copy(plots = plots, farmingXp = xp, farmingLvl = lvl, autoCollectUnlocked = lvl >= 10 || state.harvest.miningLvl >= 10),
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
            state = state.copy(logs = (state.logs + log(
                LogType.SYSTEM,
                "EXTRACTION REFUSED: ${vein.oreName} requires Mining level ${vein.reqLvl}."
            )).takeLast(Balance.LOG_CAP))
            return state
        }
        if (!vein.isReady) return state

        val extraYield = state.harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
        val extraOreYield = state.harvest.upgrades.firstOrNull { it.id == "mining_yield" }?.rank ?: 0
        val yield = (3.0 + extraYield + extraOreYield) * XpCurves.proficiencyYieldMultiplier(vein.proficiencyLevel)
        val proficiencyGain = yield.toLong().coerceAtLeast(1L)
        val skillXpReward = if (state.harvest.miningLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL) HarvestXp.forYield(vein.oreName, yield) else 0.0

        val inventory = state.inventory.toMutableList()
        addToInventory(inventory, vein.oreName, yield)
        val (newProfLvl, newProfXp) = advanceProficiency(vein.proficiencyLevel, vein.proficiencyXp, proficiencyGain)
        veins[idx] = vein.copy(miningProgress = 0f, isReady = false, proficiencyLevel = newProfLvl, proficiencyXp = newProfXp)

        val yieldStr = NumberFormatter.short(yield)
        val (xp, lvl, logs) = grantGatheringSkillXp(
            state.harvest.miningXp,
            state.harvest.miningLvl,
            skillXpReward,
            "Mining",
            "ORE EXTRACTED: Excavated ${yieldStr}x ${vein.oreName} (+${NumberFormatter.short(skillXpReward)} Mining XP, +${NumberFormatter.short(proficiencyGain)} Proficiency)",
            xpRequirement = Balance::miningSkillXpReq
        )
        state = state.copy(
            harvest = state.harvest.copy(veins = veins, miningXp = xp, miningLvl = lvl, autoCollectUnlocked = state.harvest.farmingLvl >= 10 || lvl >= 10),
            inventory = inventory,
            lifetime = state.lifetime.copy(oresMined = state.lifetime.oresMined + 1L),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        evaluateAchievements(force = true)
        return state
    }

    fun togglePlotAutoQueue(plotId: Int): GameState {
        val harvest = state.harvest
        val target = harvest.plots.find { it.id == plotId } ?: return state
        if (harvest.farmingLvl < target.reqLvl) return state
        val enable = !target.autoQueue
        val capacity = 1 + harvest.farmingLvl / 10
        val active = harvest.plots.count { it.autoQueue }
        if (enable && active >= capacity) return state
        val plots = harvest.plots.map { plot ->
            if (plot.id == plotId) plot.copy(autoQueue = enable) else plot
        }
        state = state.copy(harvest = harvest.copy(plots = plots))
        return state
    }

    fun toggleVeinAutoQueue(veinId: Int): GameState {
        val harvest = state.harvest
        val target = harvest.veins.find { it.id == veinId } ?: return state
        if (harvest.miningLvl < target.reqLvl) return state
        val enable = !target.autoQueue
        val capacity = 1 + harvest.miningLvl / 10
        val active = harvest.veins.count { it.autoQueue }
        if (enable && active >= capacity) return state
        val veins = harvest.veins.map { vein ->
            if (vein.id == veinId) vein.copy(autoQueue = enable) else vein
        }
        state = state.copy(harvest = harvest.copy(veins = veins))
        return state
    }

    /** Loops skill level ups and returns the new xp/level plus the log entries to append. */
    private fun grantSkillXp(
        currentXp: Long,
        currentLvl: Int,
        amount: Long,
        skillName: String,
        successText: String
    ): Triple<Long, Int, List<LogEntry>> {
        var lvl = currentLvl.coerceIn(1, GameCaps.MAX_LEVEL)
        var xp = if (lvl >= GameCaps.MAX_LEVEL) 0L else (if (Long.MAX_VALUE - currentXp.coerceAtLeast(0L) < amount.coerceAtLeast(0L)) Long.MAX_VALUE else currentXp.coerceAtLeast(0L) + amount.coerceAtLeast(0L))
        val logs = mutableListOf(log(LogType.LOOT, successText))
        while (lvl < GameCaps.MAX_LEVEL && xp >= Balance.skillXpReq(lvl)) {
            xp -= Balance.skillXpReq(lvl)
            lvl++
            logs += log(LogType.SKILL, "CLASS LEVEL UP: $skillName reached Level $lvl!")
        }
        if (lvl >= GameCaps.MAX_LEVEL) xp = 0L
        return Triple(xp, lvl, logs)
    }

    // ------------------------------------------------------------------ economy

    fun craftEquipment(name: String, tier: Int, slot: String, reqOre: String, reqQty: Int): GameState {
        val outcome = crafting.craftEquipment(state, name, tier, slot, reqOre, reqQty)
        state = outcome.state
        if (outcome.completed) evaluateAchievements(force = true)
        return state
    }

    fun brewPotion(name: String, reqHerb: String, reqQty: Int): GameState {
        val outcome = crafting.brewPotion(state, name, reqHerb, reqQty)
        state = outcome.state
        if (outcome.completed) evaluateAchievements(force = true)
        return state
    }

    fun sellItem(itemId: String, count: Int = 1): GameState =
        economy.sellItem(state, itemId, count).also { state = it }

    fun equipItem(itemId: String): GameState =
        economy.equipItem(state, itemId).also { state = it }

    fun unequipItem(itemId: String): GameState =
        economy.unequipItem(state, itemId).also { state = it }

    fun usePotion(itemId: String): GameState =
        economy.usePotion(state, itemId).also { state = it }

    /**
     * Consumes one potion called [name] using the full 20-herb table: direct percentage heals of the
     * missing pool, timed regeneration, temporary shields, damage reduction, lifesteal and heals.
     * Auto-deploy and the combat quick buttons share this path, so manual and automatic use match.
     */
    fun usePotionByName(name: String): GameState =
        economy.usePotionByName(state, name).also { state = it }

    /** 0 turns auto-deploy off; the UI also offers 30/50/75 percent. */
    fun setHpAutoThreshold(pct: Int): GameState =
        economy.setHpAutoThreshold(state, pct).also { state = it }

    fun setMpAutoThreshold(pct: Int): GameState =
        economy.setMpAutoThreshold(state, pct).also { state = it }

    fun selectHpPotion(name: String): GameState =
        economy.selectHpPotion(state, name).also { state = it }

    fun selectMpPotion(name: String): GameState =
        economy.selectMpPotion(state, name).also { state = it }

    fun autoEquipBestGear(): GameState =
        economy.autoEquipBestGear(state).also { state = it }

    fun summonMinion(type: String): GameState {
        val outcome = progression.summonMinion(state, type)
        state = outcome.state
        if (outcome.completed) evaluateAchievements(force = true)
        return state
    }

    fun mergeBeasts(beastId1: String, beastId2: String): GameState {
        state = progression.mergeBeasts(state, beastId1, beastId2)
        return state
    }

    fun salvageBeasts(ids: Collection<String>): GameState {
        val outcome = progression.salvageBeasts(state, ids)
        state = outcome.state
        if (outcome.completed) evaluateAchievements(force = true)
        return state
    }

    fun trainNode(nodeId: String): GameState {
        state = progression.trainNode(state, nodeId)
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
    val effect = TalentSystem.apply(
        talentId,
        rank,
        derived,
        state.player.copy(mana = state.player.mana - manaCost),
        state.monster
    )
    val duration = TalentSystem.durationSeconds(talent, rank)
    val cooldown = TalentSystem.cooldownSeconds(talent, rank)
    state = state.copy(
        player = effect.player,
        monster = effect.monster,
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

    fun rerollSlayerTask(taskId: String = ""): GameState =
        slayerSystem.reroll(state, taskId).also { state = it }

    /**
     * Generates a fresh directive matching the player's current stage range [max(1, currentFloor - 5), currentFloor]
     * and tier appropriate for player's Slayer level.
     */
    fun newSlayerTask(
        slotId: String = "slayer_${random.nextInt(1000, 9999)}",
        floorOverride: Int? = null
    ): com.aistudio.trueidle.model.SlayerTask = slayerSystem.newTask(state, slotId, floorOverride)

    fun ensureSlayerTasks(tasks: List<com.aistudio.trueidle.model.SlayerTask>): List<com.aistudio.trueidle.model.SlayerTask> =
        slayerSystem.ensure(state, tasks)

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
            state = frame.commit()
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

    fun claimDailyLogin(isSprint: Boolean): GameState {
        val daily = state.dailyLogin
        val nowMs = nowWallMs()
        val currentDay = DailyLoginDetector.currentEpochDay(nowMs)

        if (isSprint) {
            if (!DailyLoginDetector.canClaimSprint(daily, nowMs)) return state
        } else {
            if (!DailyLoginDetector.canClaimCalendar(daily, nowMs)) return state
        }

        val nextDay = if (isSprint) daily.sprint7ClaimedDays + 1 else daily.calendar30ClaimedDays + 1
        val maxDays = if (isSprint) 7 else 30
        if (nextDay > maxDays) return state

        val reward = if (isSprint) {
            com.aistudio.trueidle.core.DailyRewardCatalog.SPRINT_7_REWARDS.getOrNull(nextDay - 1) ?: return state
        } else {
            com.aistudio.trueidle.core.DailyRewardCatalog.CALENDAR_30_REWARDS.getOrNull(nextDay - 1) ?: return state
        }

        var player = state.player.copy(
            gold = state.player.gold + reward.gold,
            diamonds = state.player.diamonds + reward.diamonds
        )
        var essence = state.essence
        if (reward.faith > 0L) {
            essence = essence.copy(faith = essence.faith + reward.faith)
        }
        var necro = state.necro
        if (reward.bones > 0L) {
            necro = necro.copy(boneEssence = necro.boneEssence + reward.bones)
        }
        val inventory = state.inventory.toMutableList()
        if (reward.potionName.isNotEmpty() && reward.potionCount > 0) {
            addToInventory(inventory, reward.potionName, reward.potionCount)
        }

        val updatedDaily = if (isSprint) {
            daily.copy(
                sprint7ClaimedDays = nextDay,
                lastClaimWallMs = nowMs,
                lastSprintClaimEpochDay = currentDay,
                lastLoginEpochDay = currentDay
            )
        } else {
            daily.copy(
                calendar30ClaimedDays = nextDay,
                lastClaimWallMs = nowMs,
                lastCalendarClaimEpochDay = currentDay,
                lastLoginEpochDay = currentDay
            )
        }

        state = state.copy(
            player = player,
            essence = essence,
            necro = necro,
            inventory = inventory,
            dailyLogin = updatedDaily,
            logs = (state.logs + log(
                LogType.MILESTONE,
                "DAILY REWARD CLAIMED [${if (isSprint) "7-DAY SPRINT" else "30-DAY CALENDAR"}]: " +
                    "Day $nextDay (${reward.title}) -> +${NumberFormatter.short(reward.gold)} CR, +${reward.diamonds} DIA"
            )).takeLast(Balance.LOG_CAP)
        )
        return state
    }

    /** Simulates rolling over to the next calendar day to test daily login rewards. */
    fun simulateNextDay(): GameState {
        val daily = state.dailyLogin
        val currentDay = DailyLoginDetector.currentEpochDay(nowWallMs())
        val nextDay = if (daily.lastLoginEpochDay > 0L) daily.lastLoginEpochDay + 1 else currentDay + 1
        val updatedDaily = daily.copy(
            lastSprintClaimEpochDay = -1L,
            lastCalendarClaimEpochDay = -1L,
            lastLoginEpochDay = nextDay,
            totalLogins = daily.totalLogins + 1
        )
        state = state.copy(
            dailyLogin = updatedDaily,
            logs = (state.logs + log(
                LogType.SYSTEM,
                "CALENDAR ADVANCE: Day rollover simulated! New daily rewards ready to claim."
            )).takeLast(Balance.LOG_CAP)
        )
        return state
    }

    // ------------------------------------------------------------------ dev panel

    fun setGameSpeed(multiplier: Int): GameState =
        devTools.setSpeed(state, multiplier).also { state = it }

    /** Grants [amount] of a dev resource: GOLD, DIAMONDS, FAITH, BONES, SLAYER or TALENT. */
    fun devAddCurrency(kind: String, amount: Long): GameState =
        devTools.addCurrency(state, kind, amount).also { state = it }

    fun devMaxCurrency(kind: String): GameState = devAddCurrency(kind, DEV_MAX_CURRENCY)

    /** Unlocks every sector, achievement, discipline rank, talent and class tier for testing. */
    fun devUnlockAll(): GameState =
        devTools.unlockAll(state).also { state = it }

    /** Wipes the save and starts a clean game (the panel's hard reset). */
    fun devHardReset(): GameState = resetGame()

    // ------------------------------------------------------------------ achievements

    /**
     * Recomputes progress only for achievements whose underlying counter changed, so a 4 Hz tick no
     * longer rebuilds the whole list (and the whole state object) on every frame.
     */
    private fun evaluateAchievements(force: Boolean = false) {
        state = AchievementSystem.evaluate(state, force) ?: return
    }

    // ------------------------------------------------------------------ save helpers

    companion object {
        // Canonical values live in StateSanitizer; the facade keeps re-exposing them for callers.
        const val DEV_MIN_SPEED = StateSanitizer.DEV_MIN_SPEED
        const val DEV_MAX_SPEED = StateSanitizer.DEV_MAX_SPEED
        const val DEV_MAX_CURRENCY = StateSanitizer.DEV_MAX_CURRENCY
        const val DEV_MAX_SKILL_LEVEL = StateSanitizer.DEV_MAX_SKILL_LEVEL
        val AUTO_POTION_THRESHOLDS = StateSanitizer.AUTO_POTION_THRESHOLDS

        /** Highest issued id across logs, inventory, beasts and minions, plus the reserved floor. */
        fun nextIdFrom(source: GameState): Long = StateSanitizer.nextIdFrom(source)

        /** Clamps a loaded or imported state into a legal, self-consistent shape. */
        fun sanitize(input: GameState): GameState = StateSanitizer.sanitize(input)

    }
}
