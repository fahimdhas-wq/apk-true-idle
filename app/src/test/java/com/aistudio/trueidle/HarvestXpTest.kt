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
import kotlin.math.pow

class HarvestXpTest {

    @Test
    fun actualYieldIsTheSkillXpMultiplier() {
        assertEquals(12.0, HarvestXp.forYield(ItemCatalog.STONE, 1.0), 0.0)
        assertEquals(24.0, HarvestXp.forYield(ItemCatalog.STONE, 2.0), 0.0)
        assertEquals(36.0, HarvestXp.forYield(ItemCatalog.STONE, 3.0), 0.0)
        assertEquals(48.0, HarvestXp.forYield(ItemCatalog.STONE, 4.0), 0.0)
    }

    @Test
    fun suffixScaleYieldMultipliesTheDefaultTierXpWithoutLongOverflow() {
        val oneFItems = 1.0E27
        assertEquals(1.2E28, HarvestXp.forYield(ItemCatalog.STONE, oneFItems), 1.0E13)
        assertEquals(2.4E28, HarvestXp.forYield(ItemCatalog.COAL, oneFItems), 1.0E13)
        assertTrue(HarvestXp.forYield(ItemCatalog.STONE, oneFItems) > Long.MAX_VALUE.toDouble())
    }

    @Test
    fun higherTierResourcesGrantMoreXpPerItem() {
        assertEquals(12L, HarvestXp.xpPerItem(ItemCatalog.STONE))
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
    fun invalidAndNonFiniteYieldsCannotCreateNegativeXp() {
        assertEquals(0.0, HarvestXp.forYield(ItemCatalog.STONE, -5.0), 0.0)
        assertEquals(0.0, HarvestXp.forYield(ItemCatalog.STONE, Double.NaN), 0.0)
        assertEquals(Double.MAX_VALUE, HarvestXp.forYield(ItemCatalog.STONE, Double.POSITIVE_INFINITY), 0.0)
        assertEquals(Double.MAX_VALUE, HarvestXp.forYield(ItemCatalog.MITHRIL_ORE, Double.MAX_VALUE), 0.0)
    }

    @Test
    fun manualMiningAwardsXpForActualYieldAndOreTier() {
        val base = GameState()
        val readyCoal = base.copy(
            harvest = base.harvest.copy(
                veins = base.harvest.veins.map { it.copy(isReady = it.id == 1) }
            )
        )

        val after = GameEngine(readyCoal).mineVein(1)

        // Coal is tier 2: 3 actual items x (12 x tier 2) XP per item.
        assertEquals(72.0, after.harvest.miningXp, 0.0)
    }

    @Test
    fun manualFarmingAwardsXpForEveryActualHerbUnit() {
        val base = GameState()
        val readyHerb = base.copy(
            harvest = base.harvest.copy(
                plots = base.harvest.plots.map { it.copy(isReady = it.id == 0) }
            )
        )

        val after = GameEngine(readyHerb).harvestPlot(0)

        // Healing Herb is tier 1: 2 actual items x 12 XP per item.
        assertEquals(24.0, after.harvest.farmingXp, 0.0)
    }

    @Test
    fun explicitYieldUpgradesIncreaseXpWithTheActualBatch() {
        val base = GameEngine().state
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

        // Stone batch rises from 3 to 5 actual items: 5 x 12 = 60 XP.
        assertEquals(60.0, after.harvest.miningXp, 0.0)
    }

    @Test
    fun proficiencyYieldMultipliesGlobalSkillXpByTheActualItemCount() {
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

        // Proficiency level 5 gives 16x yield: 2 base items become 32 actual items.
        // Every actual item contributes the tier-1 rate of 12 XP: 32 x 12 = 384 XP.
        assertEquals(384.0, after.harvest.farmingXp, 0.0)
        assertEquals(32L, harvestedPlot.proficiencyXp)
    }

    @Test
    fun autoCollectionUsesTheSameActualYieldXpRule() {
        val base = GameState()
        val harvest = base.harvest.copy(
            autoCollectUnlocked = true,
            autoCollectEnabled = true,
            plots = base.harvest.plots.map { it.copy(isReady = it.id == 0) },
            veins = base.harvest.veins.map { it.copy(isReady = it.id == 1) }
        )

        val after = GameEngine(base.copy(harvest = harvest)).tick(0f)

        assertEquals(24.0, after.harvest.farmingXp, 0.0)
        assertEquals(72.0, after.harvest.miningXp, 0.0)
    }

    @Test
    fun gatheringCurvesAreMonotonicAndStopAtLevelOneHundred() {
        assertEquals(100, XpCurves.GATHERING_SKILL_MAX_LEVEL)
        assertEquals(XpCurves.farmingSkillXp(1), XpCurves.farmingSkillXp(0), 0.0)
        assertEquals(XpCurves.miningSkillXp(1), XpCurves.miningSkillXp(0), 0.0)
        assertEquals(XpCurves.farmingSkillXp(100), XpCurves.farmingSkillXp(101), 0.0)
        assertEquals(XpCurves.miningSkillXp(100), XpCurves.miningSkillXp(101), 0.0)

        for (level in 1 until XpCurves.GATHERING_SKILL_MAX_LEVEL) {
            assertTrue(XpCurves.farmingSkillXp(level + 1) > XpCurves.farmingSkillXp(level))
            assertTrue(XpCurves.miningSkillXp(level + 1) > XpCurves.miningSkillXp(level))
        }
    }

    @Test
    fun maximumProficiencyYieldModelTargetsFourToFiveContinuousYears() {
        val farmingYears = estimateOptimizedContinuousHarvestYears(isMining = false)
        val miningYears = estimateOptimizedContinuousHarvestYears(isMining = true)

        assertTrue("Expected 4-5 Farming years, got $farmingYears", farmingYears in 4.0..5.0)
        assertTrue("Expected 4-5 Mining years, got $miningYears", miningYears in 4.0..5.0)
    }

    /**
     * Models the fastest sustained-output case for the current 250 ms game loop:
     * - every plot/vein is already at proficiency 100 (2^99 item yield);
     * - one best-XP-per-second node is manually queued before auto-collect unlocks at skill level 10;
     * - all plots or all currently level-unlocked veins auto-collect from level 10 onward;
     * - speed/yield upgrade ranks are zero and every loop uses the game's tick-rounded durations.
     *
     * Coefficients are tuned so even this maximum-yield production model takes about 4.5 years.
     * Fresh saves start at proficiency 1, so their actual initial time will be longer.
     */
    private fun estimateOptimizedContinuousHarvestYears(isMining: Boolean): Double {
        val harvest = GameState().harvest
        val nodes = if (isMining) {
            harvest.veins.map { vein ->
                HarvestNode(
                    xpPerAction = HarvestXp.forYield(
                        vein.oreName,
                        (3.0 * XpCurves.proficiencyYieldMultiplier(100))
                    ),
                    durationSeconds = vein.durationSeconds.toDouble(),
                    requiredLevel = vein.reqLvl
                )
            }
        } else {
            harvest.plots.map { plot ->
                HarvestNode(
                    xpPerAction = HarvestXp.forYield(
                        plot.herbName,
                        (2.0 * XpCurves.proficiencyYieldMultiplier(100))
                    ),
                    durationSeconds = plot.durationSeconds.toDouble(),
                    requiredLevel = 1
                )
            }
        }

        val requirement: (Int) -> Double = if (isMining) {
            { level -> XpCurves.miningSkillXp(level) }
        } else {
            { level -> XpCurves.farmingSkillXp(level) }
        }

        var elapsedSeconds = 0.0
        for (level in 1 until XpCurves.GATHERING_SKILL_MAX_LEVEL) {
            val speed = 0.97.pow((level - 1).toDouble()).coerceAtLeast(0.20).toFloat()
            val eligibleRates = nodes
                .filter { it.requiredLevel <= level }
                .map { node -> node.xpPerAction / tickAlignedPeriod(node.durationSeconds, speed) }
            val xpPerSecond = when {
                eligibleRates.isEmpty() -> 0.0
                level >= 10 -> eligibleRates.sum()
                else -> eligibleRates.maxOrNull() ?: 0.0
            }
            check(xpPerSecond > 0.0)
            elapsedSeconds += requirement(level) / xpPerSecond
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
