package com.aistudio.trueidle

import com.aistudio.trueidle.core.GameClock
import com.aistudio.trueidle.core.snapshot
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FakeClock : GameClock {
    var wall: Long = 1_000_000L
    var elapsed: Long = 10_000L
    var boots: Int = 1

    override fun wallMs(): Long = wall
    override fun elapsedMs(): Long = elapsed
    override fun bootCount(): Int = boots
}

class GameEngineTest {

    @Test
    fun testBossSpawnEvery10thFloor() {
        val clock = FakeClock()
        val engine = GameEngine(
            initialState = GameState(player = PlayerState(maxUnlockedFloor = 100)),
            clock = clock,
            random = Random(1234)
        )

        for (floor in 1..100) {
            engine.setFloor(floor)
            val isBoss = (floor % 10 == 0)
            assertEquals("Floor $floor boss status mismatch", isBoss, engine.state.monster.isBoss)
        }
    }

    @Test
    fun testHeroDefeatEntersRecoveryState() {
        val clock = FakeClock()
        val engine = GameEngine(
            initialState = GameState(
                player = PlayerState(hp = 1.0, shield = 0.0),
                monster = Monster(damage = 50.0, hp = 500.0)
            ),
            clock = clock,
            random = Random(1234)
        )

        // Tick 0.25s: Monster deals damage, pHp <= 0
        engine.tick(0.25f)

        // Should enter recovery
        assertTrue("Player should have recoverLeft > 0", engine.state.player.recoverLeft > 0.0)
        assertEquals(1, engine.state.player.failuresInRow)
    }

    @Test
    fun testRetreatProtocolAfterThreeFailures() {
        val clock = FakeClock()
        val engine = GameEngine(
            initialState = GameState(
                player = PlayerState(currentFloor = 5, maxUnlockedFloor = 5, failuresInRow = 2, autoAdvance = true, hp = 1.0, shield = 0.0),
                monster = Monster(damage = 50.0, hp = 500.0)
            ),
            clock = clock,
            random = Random(1234)
        )

        // Third failure
        engine.tick(0.25f)

        // Retreat protocol engaged: floor descended to 4
        assertEquals(4, engine.state.player.currentFloor)
        assertEquals(0, engine.state.player.failuresInRow)
    }

    @Test
    fun testAutomationSettingsTogglesAndCraftSlider() {
        val clock = FakeClock()
        val engine = GameEngine(clock = clock, random = Random(1234))

        val initialEquip = engine.state.automation.autoEquipBestGear
        engine.toggleAutoEquipBestGear()
        assertEquals(!initialEquip, engine.state.automation.autoEquipBestGear)

        val initialMerge = engine.state.automation.autoMergeBeasts
        engine.toggleAutoMergeBeasts()
        assertEquals(!initialMerge, engine.state.automation.autoMergeBeasts)

        engine.setCraftMaterialUsage(85)
        assertEquals(85, engine.state.automation.craftMaterialUsagePct)
    }

    @Test
    fun testPromotionBossAdvancesTierWhenRequirementsMet() {
        val clock = FakeClock()
        val engine = GameEngine(
            initialState = GameState(
                player = PlayerState(currentFloor = 65, maxUnlockedFloor = 65, lvl = 50),
                promotion = com.aistudio.trueidle.model.PromotionState(
                    tier = 5,
                    maxTier = 20,
                    tierName = "Dawnbreaker",
                    requiredFloor = 60,
                    requiredGearScore = 100,
                    bonusPercent = 8
                )
            ),
            clock = clock,
            random = Random(1234)
        )

        val newState = engine.fightPromotionBoss()
        assertEquals(6, newState.promotion.tier)
        assertEquals(16, newState.promotion.bonusPercent)
    }

    @Test
    fun testEightHourIdleAlertOnlyTriggersAtEightHours() {
        val clock = FakeClock().apply { elapsed = Balance.OFFLINE_CAP_MS + 10_000L }
        val saved = GameState().copy(
            meta = GameState().meta.copy(
                savedWallMs = clock.wall - Balance.OFFLINE_CAP_MS,
                savedElapsedMs = 10_000L,
                bootCount = clock.boots
            )
        )
        val engine = GameEngine(initialState = saved, clock = clock, random = Random(1234))

        engine.resume(clock.snapshot())

        assertTrue(engine.consumeEightHourIdleAlert())
        assertFalse(engine.consumeEightHourIdleAlert())
    }

    @Test
    fun testShortIdleDoesNotCreateEightHourAlert() {
        val clock = FakeClock().apply { elapsed = 2 * 3600_000L + 10_000L }
        val saved = GameState().copy(
            meta = GameState().meta.copy(
                savedWallMs = clock.wall - 2 * 3600_000L,
                savedElapsedMs = 10_000L,
                bootCount = clock.boots
            )
        )
        val engine = GameEngine(initialState = saved, clock = clock, random = Random(1234))

        engine.resume(clock.snapshot())

        assertFalse(engine.consumeEightHourIdleAlert())
    }

    @Test
    fun testOfflineSimulation() {
        val clock = FakeClock()
        val engine = GameEngine(clock = clock, random = Random(1234))

        // 8-hour offline simulation
        engine.simulateOfflineRecon(8)
        val loot = engine.state.pendingOfflineLoot
        assertTrue("Expected pending offline loot", loot != null)
        assertTrue("Expected 8h duration (28800s)", loot!!.timeAwaySeconds >= 28800L)
        assertTrue("Expected cap applied", loot.capApplied)
        assertTrue("Expected timeline log", loot.timelineLog.isNotEmpty())
    }

    @Test
    fun testPlayTimeAccumulationInLiveTick() {
        val clock = FakeClock()
        val engine = GameEngine(clock = clock, random = Random(1234))

        val initialTime = engine.state.lifetime.playTimeSeconds
        // Tick 4 times with 0.25s = 1.0s
        repeat(4) {
            engine.tick(0.25f)
        }
        assertEquals(initialTime + 1L, engine.state.lifetime.playTimeSeconds)
    }
}
