package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.HarvestXp
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.LogEntry
import kotlin.math.pow

/** Advances plots and veins, auto-collects, grants yield and class XP. */
internal class HarvestSystem(
    private val nextId: () -> Long,
    private val logger: (String, String) -> LogEntry
) {
    fun tick(frame: Frame, dt: Float) {
        val speedRank = frame.harvest.upgrades.firstOrNull { it.id == "harvest_speed" }?.rank ?: 0
        val miningRank = frame.harvest.upgrades.firstOrNull { it.id == "mining_speed" }?.rank ?: 0
        val extraYield = frame.harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
        val extraOreYield = frame.harvest.upgrades.firstOrNull { it.id == "mining_yield" }?.rank ?: 0
        val farmMult = (0.97.pow((frame.harvest.farmingLvl - 1).toDouble()) * 0.92.pow(speedRank)).coerceAtLeast(0.20).toFloat()
        val mineMult = (0.97.pow((frame.harvest.miningLvl - 1).toDouble()) * 0.92.pow(miningRank)).coerceAtLeast(0.20).toFloat()
        val autoCollect = frame.harvest.autoCollectUnlocked && frame.harvest.autoCollectEnabled

        val herbGains = mutableMapOf<String, Double>()
        val oreGains = mutableMapOf<String, Double>()
        var farmingXpGained = 0.0
        var miningXpGained = 0.0

        val plots = frame.harvest.plots.map { plot ->
            if (frame.harvest.farmingLvl < plot.reqLvl) {
                return@map plot.copy(growthProgress = 0f, isReady = false, autoQueue = false)
            }
            val duration = (plot.durationSeconds * farmMult).coerceAtLeast(0.1f)
            val shouldAuto = plot.autoQueue || autoCollect
            val baseYield = (2.0 + extraYield) * XpCurves.proficiencyYieldMultiplier(plot.proficiencyLevel)
            val proficiencyGain = baseYield.toLong().coerceAtLeast(1L)
            if (plot.isReady) {
                if (shouldAuto) {
                    herbGains[plot.herbName] = (herbGains[plot.herbName] ?: 0.0) + baseYield
                    farmingXpGained = safeAddNonNegative(farmingXpGained, HarvestXp.forYield(plot.herbName, baseYield))
                    val (lvl, xp) = advanceProficiency(plot.proficiencyLevel, plot.proficiencyXp, proficiencyGain)
                    plot.copy(growthProgress = 0f, isReady = false, proficiencyLevel = lvl, proficiencyXp = xp)
                } else plot
            } else {
                val progress = plot.growthProgress + dt / duration
                if (progress >= 1f) {
                    if (shouldAuto) {
                        herbGains[plot.herbName] = (herbGains[plot.herbName] ?: 0.0) + baseYield
                        farmingXpGained = safeAddNonNegative(farmingXpGained, HarvestXp.forYield(plot.herbName, baseYield))
                        val (lvl, xp) = advanceProficiency(plot.proficiencyLevel, plot.proficiencyXp, proficiencyGain)
                        plot.copy(growthProgress = 0f, isReady = false, proficiencyLevel = lvl, proficiencyXp = xp)
                    } else plot.copy(growthProgress = 1f, isReady = true)
                } else plot.copy(growthProgress = progress)
            }
        }

        val veins = frame.harvest.veins.map { vein ->
            if (frame.harvest.miningLvl < vein.reqLvl) {
                vein.copy(miningProgress = 0f, isReady = false, autoQueue = false)
            } else {
                val duration = (vein.durationSeconds * mineMult).coerceAtLeast(0.1f)
                val shouldAuto = vein.autoQueue || autoCollect
                val baseYield = (3.0 + extraYield + extraOreYield) * XpCurves.proficiencyYieldMultiplier(vein.proficiencyLevel)
                val proficiencyGain = baseYield.toLong().coerceAtLeast(1L)
                if (vein.isReady) {
                    if (shouldAuto) {
                        oreGains[vein.oreName] = (oreGains[vein.oreName] ?: 0.0) + baseYield
                        miningXpGained = safeAddNonNegative(miningXpGained, HarvestXp.forYield(vein.oreName, baseYield))
                        val (lvl, xp) = advanceProficiency(vein.proficiencyLevel, vein.proficiencyXp, proficiencyGain)
                        vein.copy(miningProgress = 0f, isReady = false, proficiencyLevel = lvl, proficiencyXp = xp)
                    } else vein
                } else {
                    val progress = vein.miningProgress + dt / duration
                    if (progress >= 1f) {
                        if (shouldAuto) {
                            oreGains[vein.oreName] = (oreGains[vein.oreName] ?: 0.0) + baseYield
                            miningXpGained = safeAddNonNegative(miningXpGained, HarvestXp.forYield(vein.oreName, baseYield))
                            val (lvl, xp) = advanceProficiency(vein.proficiencyLevel, vein.proficiencyXp, proficiencyGain)
                            vein.copy(miningProgress = 0f, isReady = false, proficiencyLevel = lvl, proficiencyXp = xp)
                        } else vein.copy(miningProgress = 1f, isReady = true)
                    } else vein.copy(miningProgress = progress)
                }
            }
        }

        var farmingXp = frame.harvest.farmingXp.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        var miningXp = frame.harvest.miningXp.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
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
        while (farmingLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL && farmingXp >= Balance.farmingSkillXpReq(farmingLvl)) {
            farmingXp -= Balance.farmingSkillXpReq(farmingLvl)
            farmingLvl++
            frame.logs += log(LogType.SKILL, "CLASS LEVEL UP: Farming reached Level $farmingLvl!")
        }
        while (miningLvl < XpCurves.GATHERING_SKILL_MAX_LEVEL && miningXp >= Balance.miningSkillXpReq(miningLvl)) {
            miningXp -= Balance.miningSkillXpReq(miningLvl)
            miningLvl++
            frame.logs += log(LogType.SKILL, "CLASS LEVEL UP: Mining reached Level $miningLvl!")
        }
        if (farmingLvl >= XpCurves.GATHERING_SKILL_MAX_LEVEL) farmingXp = 0.0
        if (miningLvl >= XpCurves.GATHERING_SKILL_MAX_LEVEL) miningXp = 0.0

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

    private fun log(type: String, text: String): LogEntry = logger(type, text)

    private fun addToInventory(inventory: MutableList<InventoryItem>, name: String, qty: Double) =
        InventoryOps.add(inventory, name, qty, nextId)

    private fun advanceProficiency(currentLvl: Int, currentXp: Long, gain: Long): Pair<Int, Long> =
        ProgressOps.advanceProficiency(currentLvl, currentXp, gain)

    private fun safeAddNonNegative(current: Double, amount: Double): Double =
        ProgressOps.safeAddNonNegative(current, amount)
}
