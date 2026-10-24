package com.aistudio.trueidle

import com.aistudio.trueidle.core.ClockSnapshot
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.engine.OfflineEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.SaveMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OfflineEngineTest {

    @Test
    fun testTrustedAwayMsMonotonic() {
        val meta = SaveMeta(
            savedWallMs = 1000L,
            savedElapsedMs = 5000L,
            bootCount = 1
        )
        val now = ClockSnapshot(
            wallMs = 1500L,
            elapsedMs = 15000L,
            bootCount = 1
        )
        val away = OfflineEngine.trustedAwayMs(meta, now)
        assertEquals(10000L, away) // 15000 - 5000
    }

    @Test
    fun testTrustedAwayMsNegativeOnClockRewind() {
        val meta = SaveMeta(
            savedWallMs = 10000L,
            savedElapsedMs = 5000L,
            bootCount = 1
        )
        // System rebooted (bootCount 2) but wall clock went backwards
        val now = ClockSnapshot(
            wallMs = 5000L,
            elapsedMs = 2000L,
            bootCount = 2
        )
        val away = OfflineEngine.trustedAwayMs(meta, now)
        assertEquals(-1L, away)
    }

    @Test
    fun testOfflineComputeAwardsRewards() {
        val state = GameState()
        val derived = DerivedStats.of(state)
        val rnd = Random(42)

        // 10 minutes away (600,000 ms)
        val loot = OfflineEngine.compute(state, derived, 600_000L, 1.0, rnd)

        assertTrue("Expected kills > 0", loot.kills > 0)
        assertTrue("Expected gold > 0", loot.goldEarned > 0)
        assertTrue("Expected xp > 0", loot.xpEarned > 0)
        assertEquals(loot.kills * 2, loot.bonesEarned)
    }
}
