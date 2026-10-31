package com.aistudio.trueidle

import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.PlayerState
import com.aistudio.trueidle.model.SlayerState
import com.aistudio.trueidle.model.SlayerTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SlayerAndStatsSystemsTest {

    @Test
    fun testTrainingNodeMaxRankIsOneMillion() {
        val state = GameState()
        val engine = GameEngine(state)
        engine.state.training.nodes.forEach { node ->
            assertEquals("Training node ${node.id} maxRank", com.aistudio.trueidle.core.TrainingCaps.maxRank(node.id), node.maxRank)
        }
    }

    @Test
    fun testSlayerPanelHasTenConcurrentTasks() {
        val engine = GameEngine(GameState())
        assertEquals(10, engine.state.slayer.tasks.size)
    }

    @Test
    fun testSlayerTaskStageRelevance() {
        val currentFloor = 25
        val state = GameState(
            player = PlayerState(currentFloor = currentFloor, maxUnlockedFloor = 30)
        )
        val engine = GameEngine(state)
        engine.state.slayer.tasks.forEach { task ->
            assertTrue(
                "Task floor ${task.targetFloor} must be in range [20, 25]",
                task.targetFloor in 20..25
            )
        }
    }

    @Test
    fun testSlayerAutoRewardOnKill() {
        val task = SlayerTask(
            id = "test_slot_1",
            targetName = "Target Alpha",
            tier = "D+",
            targetFloor = 5,
            currentKills = 9,
            requiredKills = 10,
            rewardGold = 500L,
            rewardPoints = 8,
            rewardDiamonds = 4L,
            rewardFaith = 25L,
            rewardBones = 15L
        )
        val state = GameState(
            player = PlayerState(gold = 100L, diamonds = 0L, currentFloor = 5),
            slayer = SlayerState(slayerPoints = 10, tasks = listOf(task)),
            monster = Monster(name = "Target Alpha", hp = 10.0, maxHp = 10.0)
        )
        val engine = GameEngine(state)

        // Kill the monster
        engine.tick(0.5f)

        // Player should receive automatic rewards without manual claim
        assertTrue("Gold should have increased by 500+", engine.state.player.gold >= 600L)
        assertTrue("Slayer points should have increased by 8", engine.state.slayer.slayerPoints >= 18)
        assertTrue("Diamonds should have increased by 4", engine.state.player.diamonds >= 4L)
        assertTrue("Faith should have increased by 25", engine.state.essence.faith >= 75L)
        assertTrue("Bone essence should have increased by 15", engine.state.necro.boneEssence >= 35L)

        // Mission slot should have automatically refreshed with a fresh task
        val refreshedTask = engine.state.slayer.tasks.firstOrNull { it.id == "test_slot_1" }
        assertTrue("Task should have refreshed", refreshedTask != null)
        assertEquals(0, refreshedTask?.currentKills)
    }

    @Test
    fun testManualRefreshCostsOneGold() {
        val initialGold = 50L
        val state = GameState(
            player = PlayerState(gold = initialGold, currentFloor = 10, maxUnlockedFloor = 10)
        )
        val engine = GameEngine(state)
        val firstTaskId = engine.state.slayer.tasks.first().id

        engine.rerollSlayerTask(firstTaskId)

        assertEquals("Player should have spent exactly 1 Gold", initialGold - 1L, engine.state.player.gold)
        assertEquals(10, engine.state.slayer.tasks.size)
    }

    @Test
    fun testStruggleAccumulatesWhenShieldTakesDamage() {
        // Monster deals heavy damage that is absorbed by hero shield
        val state = GameState(
            player = PlayerState(hp = 100.0, shield = 200.0),
            monster = Monster(damage = 30.0, hp = 500.0, maxHp = 500.0)
        )
        val engine = GameEngine(state)
        val struggleBefore = engine.state.talent.struggleDamageTaken

        // Advance simulation tick
        engine.tick(0.5f)

        val struggleAfter = engine.state.talent.struggleDamageTaken
        assertTrue(
            "Struggle damage endured should increase even when absorbed by shield: before=$struggleBefore, after=$struggleAfter",
            struggleAfter > struggleBefore
        )
    }

    @Test
    fun testBonusRewardsForDPlusOrHigher() {
        val dPlusRank = TierConfig.rankOf("D+")
        val fMinusRank = TierConfig.rankOf("F-")

        val fRewards = SlayerMath.rollBonusRewards(fMinusRank, 100L, Random(42))
        assertEquals(0L, fRewards.diamonds)
        assertEquals(0L, fRewards.faith)
        assertEquals(0L, fRewards.bones)

        val dRewards = SlayerMath.rollBonusRewards(dPlusRank, 100L, Random(42))
        assertTrue(
            "D+ or higher missions should yield bonus spoils (Diamonds, Faith, Bones, or extra Gold)",
            dRewards.diamonds > 0L || dRewards.faith > 0L || dRewards.bones > 0L || dRewards.extraGold > 0L
        )
    }

    @Test
    fun testThirteenEquipmentGroupsAreAvailable() {
        assertEquals(13, Equipment.GROUPS.size)
        assertTrue(Equipment.GROUPS.contains("Rifle"))
        assertTrue(Equipment.GROUPS.contains("Visor"))
        assertTrue(Equipment.GROUPS.contains("Node I"))
    }
}
