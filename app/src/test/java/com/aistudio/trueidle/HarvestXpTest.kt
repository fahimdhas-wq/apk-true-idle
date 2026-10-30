package com.aistudio.trueidle

import com.aistudio.trueidle.core.HarvestXp
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow

class HarvestXpTest {

    @Test
    fun tierOneStoneBatchRewardsTwelveXpPerItem() {
        assertEquals(12L, HarvestXp.xpPerItem(ItemCatalog.STONE))
        assertEquals(36L, HarvestXp.forBaseBatch(ItemCatalog.STONE, 3L))
        assertEquals(48L, HarvestXp.forBaseBatch(ItemCatalog.STONE, 4L))
    }

    @Test
    fun higherTierResourcesGrantMoreXpPerItem() {
        assertEquals(24L, HarvestXp.xpPerItem(ItemCatalog.COAL))
        assertEquals(36L, HarvestXp.xpPerItem(ItemCatalog.COPPER_ORE))
        assertEquals(12L, HarvestXp.xpPerItem(ItemCatalog.HEALING_HERB))
        assertEquals(240L, HarvestXp.xpPerItem(ItemCatalog.WORLDTREE_LEAF))
        assertTrue(HarvestXp.xpPerItem(ItemCatalog.COAL) > HarvestXp.xpPerItem(ItemCatalog.STONE))
        assertTrue(HarvestXp.xpPerItem(ItemCatalog.MITHRIL_ORE) > HarvestXp.xpPerItem(ItemCatalog.COAL))
    }

    @Test
    fun legacyResourceNamesKeepTheirOriginalTier() {
        assertEquals(12L, HarvestXp.xpPerItem("Felweed"))
        assertEquals(192L, HarvestXp.xpPerItem("Moonflower"))
        assertEquals(228L, HarvestXp.xpPerItem("Starbloom"))
        assertEquals(240L, HarvestXp.xpPerItem("Voidcap"))
        assertEquals(180L, HarvestXp.xpPerItem("Obsidian Ore"))
    }

    @Test
    fun batchXpSaturatesInsteadOfOverflowingNegative() {
        assertEquals(Long.MAX_VALUE, HarvestXp.forBaseBatch(ItemCatalog.MITHRIL_ORE, Long.MAX_VALUE))
        assertEquals(0L, HarvestXp.forBaseBatch(ItemCatalog.STONE, -5L))
    }

    @Test
    fun manualMiningAwardsTierAndBatchAdjustedXp() {
        val base = GameState()
        val readyCoal = base.copy(
            harvest = base.harvest.copy(
                veins = base.harvest.veins.map { it.copy(isReady = it.id == 1) }
            )
        )

        val after = GameEngine(readyCoal).mineVein(1)

        // Coal is tier 2: 24 XP per unit x the vein's guaranteed 3-unit batch.
        assertEquals(72L, after.harvest.miningXp)
    }

    @Test
    fun manualFarmingAwardsXpForEveryGuaranteedHerbUnit() {
        val base = GameState()
        val readyHerb = base.copy(
            harvest = base.harvest.copy(
                plots = base.harvest.plots.map { it.copy(isReady = it.id == 0) }
            )
        )

        val after = GameEngine(readyHerb).harvestPlot(0)

        // Healing Herb is tier 1: 12 XP x 2 guaranteed herb units.
        assertEquals(24L, after.harvest.farmingXp)
    }

    @Test
    fun explicitYieldUpgradesIncreaseSkillXpWithTheBatchSize() {
        val base = GameState()
        val upgradedHarvest = base.harvest.copy(
            upgrades = base.harvest.upgrades.map {
                when (it.id) {
                    "harvest_yield", "mining_yield" -> it.copy(rank = 1)
                    else -> it
                }
            },
            veins = base.harvest.veins.map { it.copy(isReady = it.id == 0) }
        )

        val after = GameEngine(base.copy(harvest = upgradedHarvest)).mineVein(0)

        // Stone batch rises from 3 to 5 units: 12 XP x 5 = 60 XP.
        assertEquals(60L, after.harvest.miningXp)
    }

    @Test
    fun proficiencyYieldDoesNotMultiplyTheLongTermSkillXpAgain() {
        val base = GameState()
        val state = base.copy(
            harvest = base.harvest.copy(
                plots = base.harvest.plots.map {
                    if (it.id == 0) it.copy(isReady = true, proficiencyLevel = 5, proficiencyXp = 0L)
                    else it
                }
            )
        )

        val after = GameEngine(state).harvestPlot(0)
        val harvestedPlot = after.harvest.plots.first { it.id == 0 }

        // The level-5 proficiency multiplier increases the 2-unit inventory/proficiency yield to
        // 32, but global Farming XP remains 12 x the guaranteed 2-unit base batch.
        assertEquals(24L, after.harvest.farmingXp)
        assertEquals(32L, harvestedPlot.proficiencyXp)
    }

    @Test
    fun autoCollectionUsesTheSameTierAndBatchXpRules() {
        val base = GameState()
        val harvest = base.harvest.copy(
            autoCollectUnlocked = true,
            autoCollectEnabled = true,
            plots = base.harvest.plots.map { it.copy(isReady = it.id == 0) },
            veins = base.harvest.veins.map { it.copy(isReady = it.id == 1) }
        )

        val after = GameEngine(base.copy(harvest = harvest)).tick(0f)

        assertEquals(24L, after.harvest.farmingXp)
        assertEquals(72L, after.harvest.miningXp)
    }

    @Test
    fun gatheringCurveIsMonotonicAndStopsAtLevelOneHundred() {
        assertEquals(100, XpCurves.GATHERING_SKILL_MAX_LEVEL)
        assertEquals(XpCurves.gatheringSkill(1), XpCurves.gatheringSkill(0))
        assertEquals(XpCurves.gatheringSkill(100), XpCurves.gatheringSkill(101))

        for (level in 1 until XpCurves.GATHERING_SKILL_MAX_LEVEL) {
            assertTrue(
                "Gathering XP must increase between levels $level and ${level + 1}",
                XpCurves.gatheringSkill(level + 1) > XpCurves.gatheringSkill(level)
            )
        }
    }

    @Test
    fun gatheringProgressionModelTargetsFourToFiveContinuousYears() {
        val years = estimateOptimizedContinuousHarvestYears()
        assertTrue("Expected 4-5 years, got $years", years in 4.0..5.0)
    }

    /**
     * Models the current game loop's 250 ms harvest step, the default 20 plots/veins and unlock
     * requirements, and the actual XP returned by HarvestXp. Before global auto-collect unlocks,
     * it assumes one best-XP-per-second auto-queued resource per skill. From level 10 onward, the
     * game's default-enabled auto-collect runs every unlocked resource. Yield/speed upgrade ranks
     * are zero and proficiency's exponential output multiplier is excluded from skill XP.
     */
    private fun estimateOptimizedContinuousHarvestYears(): Double {
        val harvest = GameState().harvest
        val farmingNodes = harvest.plots.map { plot ->
            HarvestNode(
                xpPerAction = HarvestXp.forBaseBatch(plot.herbName, 2L).toDouble(),
                durationSeconds = plot.durationSeconds.toDouble(),
                requiredLevel = 1
            )
        }
        val miningNodes = harvest.veins.map { vein ->
            HarvestNode(
                xpPerAction = HarvestXp.forBaseBatch(vein.oreName, 3L).toDouble(),
                durationSeconds = vein.durationSeconds.toDouble(),
                requiredLevel = vein.reqLvl
            )
        }

        var farmingLevel = 1
        var miningLevel = 1
        var farmingXp = 0.0
        var miningXp = 0.0
        var elapsedSeconds = 0.0
        var events = 0

        while (
            farmingLevel < XpCurves.GATHERING_SKILL_MAX_LEVEL ||
            miningLevel < XpCurves.GATHERING_SKILL_MAX_LEVEL
        ) {
            val autoCollect = farmingLevel >= 10 || miningLevel >= 10
            val farmingSpeed = 0.97.pow((farmingLevel - 1).toDouble()).coerceAtLeast(0.20).toFloat()
            val miningSpeed = 0.97.pow((miningLevel - 1).toDouble()).coerceAtLeast(0.20).toFloat()

            val farmingRates = if (farmingLevel < XpCurves.GATHERING_SKILL_MAX_LEVEL) {
                farmingNodes.map { node -> node.xpPerAction / tickAlignedPeriod(node.durationSeconds, farmingSpeed) }
            } else emptyList()
            val eligibleMiningNodes = miningNodes.filter { it.requiredLevel <= miningLevel }
            val miningRates = if (miningLevel < XpCurves.GATHERING_SKILL_MAX_LEVEL) {
                eligibleMiningNodes.map { node -> node.xpPerAction / tickAlignedPeriod(node.durationSeconds, miningSpeed) }
            } else emptyList()

            val farmingRate = when {
                farmingRates.isEmpty() -> 0.0
                autoCollect -> farmingRates.sum()
                else -> farmingRates.maxOrNull() ?: 0.0
            }
            val miningRate = when {
                miningRates.isEmpty() -> 0.0
                autoCollect -> miningRates.sum()
                else -> miningRates.maxOrNull() ?: 0.0
            }

            val farmingTimeToLevel = if (farmingRate > 0.0) {
                (XpCurves.gatheringSkill(farmingLevel).toDouble() - farmingXp) / farmingRate
            } else Double.POSITIVE_INFINITY
            val miningTimeToLevel = if (miningRate > 0.0) {
                (XpCurves.gatheringSkill(miningLevel).toDouble() - miningXp) / miningRate
            } else Double.POSITIVE_INFINITY
            val stepSeconds = min(farmingTimeToLevel, miningTimeToLevel)
            check(stepSeconds.isFinite() && stepSeconds >= 0.0)

            farmingXp += farmingRate * stepSeconds
            miningXp += miningRate * stepSeconds
            elapsedSeconds += stepSeconds

            while (
                farmingLevel < XpCurves.GATHERING_SKILL_MAX_LEVEL &&
                farmingXp >= XpCurves.gatheringSkill(farmingLevel).toDouble()
            ) {
                farmingXp -= XpCurves.gatheringSkill(farmingLevel).toDouble()
                farmingLevel++
            }
            while (
                miningLevel < XpCurves.GATHERING_SKILL_MAX_LEVEL &&
                miningXp >= XpCurves.gatheringSkill(miningLevel).toDouble()
            ) {
                miningXp -= XpCurves.gatheringSkill(miningLevel).toDouble()
                miningLevel++
            }

            events++
            check(events < 1_000) { "Progression model failed to converge" }
        }

        return elapsedSeconds / (365.25 * 24.0 * 60.0 * 60.0)
    }

    private fun tickAlignedPeriod(durationSeconds: Double, skillSpeed: Float): Double {
        val effectiveDuration = (durationSeconds * skillSpeed).coerceAtLeast(0.1)
        val tickCount = ceil(effectiveDuration / 0.25).coerceAtLeast(1.0)
        return tickCount * 0.25
    }

    private data class HarvestNode(
        val xpPerAction: Double,
        val durationSeconds: Double,
        val requiredLevel: Int
    )
}
