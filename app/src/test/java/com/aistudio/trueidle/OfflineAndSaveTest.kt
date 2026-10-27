package com.aistudio.trueidle

import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.snapshot
import com.aistudio.trueidle.data.SaveJson
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OfflineAndSaveTest {

    /**
     * Encoding is a fixed point of decode: re-encoding a decoded save reproduces the exact bytes, so
     * no field is dropped, mis-typed or given a different default by a round trip.
     */
    @Test
    fun testSaveEncodingIsStableAcrossARoundTrip() {
        val engine = GameEngine(clock = FakeClock(), random = Random(99))
        repeat(600) { engine.tick(0.25f) }

        val once = SaveJson.encodeToString(GameState.serializer(), engine.state)
        val decoded = SaveJson.decodeFromString(GameState.serializer(), once)
        val twice = SaveJson.encodeToString(GameState.serializer(), decoded)

        assertEquals(once, twice)
        assertEquals(engine.state.player, decoded.player)
        assertEquals(engine.state.inventory, decoded.inventory)
        assertEquals(engine.state.lifetime, decoded.lifetime)
    }

    /** Imported or corrupt values must be clamped rather than trusted. */
    @Test
    fun testRestoreClampsAnOutOfRangeState() {
        val engine = GameEngine(clock = FakeClock(), random = Random(7))

        engine.restore(
            GameState(
                player = PlayerState(
                    gold = -500L,
                    diamonds = -3L,
                    currentFloor = 999,
                    maxUnlockedFloor = 0,
                    killsOnFloor = 9999
                )
            )
        )

        val p = engine.state.player
        assertTrue("gold must not stay negative", p.gold >= 0L)
        assertTrue("diamonds must not stay negative", p.diamonds >= 0L)
        assertTrue("floor must be clamped into range", p.currentFloor in 1..GameCaps.MAX_FLOOR)
        assertTrue("unlocked floor must cover the current floor", p.maxUnlockedFloor >= p.currentFloor)
        assertTrue("kills must not exceed the floor requirement", p.killsOnFloor <= p.killsRequired)
    }

    /** An unclaimed report must survive a later resume instead of being replaced outright. */
    @Test
    fun testPendingReportIsNotDiscardedByALaterResume() {
        val clock = FakeClock()
        val engine = GameEngine(clock = clock, random = Random(7))

        engine.simulateOfflineRecon(4)
        val first = engine.state.pendingOfflineLoot!!
        clock.elapsed += 3 * 3600_000L
        engine.resume(clock.snapshot())

        val second = engine.state.pendingOfflineLoot
        assertNotNull("the report must still be claimable", second)
        assertTrue("unclaimed rewards must not shrink", second!!.goldEarned >= first.goldEarned)
        assertTrue("unclaimed xp must not shrink", second.xpEarned >= first.xpEarned)
    }

    /** Claiming must leave a coherent floor: its own monster and its own kill count. */
    @Test
    fun testClaimingLootRestoresTheFloorState() {
        val engine = GameEngine(clock = FakeClock(), random = Random(7))
        engine.setFloor(9)

        engine.simulateOfflineRecon(8)
        val loot = engine.state.pendingOfflineLoot!!
        engine.claimOfflineLoot()

        val after = engine.state
        assertEquals("claimed floor", loot.finalFloor, after.player.currentFloor)
        assertEquals("claimed kill count", loot.killsOnFloor, after.player.killsOnFloor)
        assertEquals(
            "the monster on screen must belong to the claimed floor",
            Balance.generateMonsterForFloor(after.player.currentFloor).name,
            after.monster.name
        )
    }

    @Test
    fun testBossFloorsRequireASingleKill() {
        assertTrue(Balance.isBossFloor(10))
        assertTrue(Balance.isBossFloor(100))
        assertEquals(Balance.BOSS_KILLS_PER_FLOOR, Balance.killsRequiredFor(10))
        assertEquals(Balance.KILLS_PER_FLOOR, Balance.killsRequiredFor(9))
    }
}
