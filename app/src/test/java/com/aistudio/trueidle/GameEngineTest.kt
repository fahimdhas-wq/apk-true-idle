package com.aistudio.trueidle

import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.GameClock
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
    fun testNotificationCoalescingKeepsDistinctEvents() {
        val clock = FakeClock()
        val engine = GameEngine(clock = clock, random = Random(1234))

        engine.pushNotification(title = "SKILL UP", message = "Farming level 2", type = "level")
        engine.pushNotification(title = "SKILL UP", message = "Mining level 2", type = "level")

        // Two distinct messages should not collapse into one
        assertEquals(2, engine.state.notifications.size)
    }
}
