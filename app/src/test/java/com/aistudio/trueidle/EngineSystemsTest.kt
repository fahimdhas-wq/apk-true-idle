package com.aistudio.trueidle

import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.engine.SlayerSystem
import com.aistudio.trueidle.engine.StateSanitizer
import com.aistudio.trueidle.model.DevSettings
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.PlayerState
import com.aistudio.trueidle.model.PotionSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Unit tests for the engine systems that GameEngine now delegates to. These assert the contracts
 * the facade relies on (deterministic RNG use, exact gold costs, save-migration clamping) without
 * going through a full tick.
 */
class EngineSystemsTest {

    private fun slayer(seed: Int) = SlayerSystem(Random(seed)) { type, text -> LogEntry(text = text, type = type) }

    private fun baseState(gold: Long = 10L, floor: Int = 12) = GameState(
        player = PlayerState(gold = gold, currentFloor = floor, maxUnlockedFloor = floor)
    )

    @Test
    fun ensureFillsEveryEmptySlotDeterministically() {
        val first = slayer(7).ensure(GameState(), emptyList())
        val second = slayer(7).ensure(GameState(), emptyList())

        assertEquals(SlayerMath.MAX_TASKS, first.size)
        assertEquals(first, second)
        assertEquals(first.size, first.map { it.id }.toSet().size)
    }

    @Test
    fun ensureKeepsExistingTasksAndOnlyTopsUp() {
        val system = slayer(3)
        val existing = system.newTask(baseState(), "kept_slot", floorOverride = 4)

        val filled = system.ensure(baseState(), listOf(existing))

        assertEquals(SlayerMath.MAX_TASKS, filled.size)
        assertEquals(existing, filled.first())
    }

    @Test
    fun rerollSpendsExactlyOneGoldAndReusesTheSlotId() {
        val system = slayer(11)
        val base = baseState(gold = 5L)
        val tasks = system.ensure(base, emptyList())
        val target = tasks[2]
        val state = base.copy(slayer = base.slayer.copy(tasks = tasks, currentTask = tasks.first()))

        val rolled = system.reroll(state, target.id)

        assertEquals(4L, rolled.player.gold)
        assertEquals(tasks.size, rolled.slayer.tasks.size)
        assertEquals(target.id, rolled.slayer.tasks[2].id)
        assertNotEquals(target, rolled.slayer.tasks[2])
        assertTrue(rolled.logs.last().text.contains("REROLLED"))
    }

    @Test
    fun rerollIsRefusedWithoutGoldAndLeavesStateUntouched() {
        val system = slayer(5)
        val state = baseState(gold = 0L)

        val rolled = system.reroll(state, "any")

        assertEquals(state, rolled)
        assertTrue(rolled.slayer.tasks.isEmpty())
    }

    @Test
    fun rerollOfAnUnknownSlotStillCostsNothing() {
        val system = slayer(5)
        val base = baseState(gold = 5L)
        val state = base.copy(slayer = base.slayer.copy(tasks = system.ensure(base, emptyList())))

        val rolled = system.reroll(state, "does_not_exist")

        assertEquals(state, rolled)
        assertEquals(5L, rolled.player.gold)
    }

    @Test
    fun newTaskStaysOnReachableFloors() {
        val state = baseState(floor = 3)
        repeat(25) { seed ->
            val task = slayer(seed).newTask(state, "slot", null)
            assertTrue("floor ${task.targetFloor} out of range", task.targetFloor in 1..3)
            assertTrue(task.requiredKills >= 1)
            assertEquals(0, task.currentKills)
            assertEquals(false, task.isCompleted)
        }
    }

    @Test
    fun sanitizeClampsHostileSaveValues() {
        val dirty = GameState(
            player = PlayerState(
                lvl = 0,
                xp = -10L,
                gold = -5L,
                diamonds = -2L,
                hp = -1.0,
                currentFloor = 900,
                maxUnlockedFloor = 4,
                killsOnFloor = -3,
                failuresInRow = -1
            ),
            dev = DevSettings(gameSpeed = 500),
            potionSettings = PotionSettings(
                hpAutoThresholdPct = 37,
                selectedHpPotion = "Not A Potion"
            )
        )

        val clean = StateSanitizer.sanitize(dirty)

        assertEquals(1, clean.player.lvl)
        assertEquals(0L, clean.player.xp)
        assertEquals(0L, clean.player.gold)
        assertEquals(0L, clean.player.diamonds)
        assertEquals(0, clean.player.hp.compareTo(0.0))
        assertEquals(4, clean.player.currentFloor)
        assertEquals(0, clean.player.killsOnFloor)
        assertEquals(0, clean.player.failuresInRow)
        assertEquals(Balance.killsRequiredFor(4), clean.player.killsRequired)
        assertEquals(StateSanitizer.DEV_MAX_SPEED, clean.dev.gameSpeed)
        assertEquals(0, clean.potionSettings.hpAutoThresholdPct)
        assertEquals("Minor Health Flask", clean.potionSettings.selectedHpPotion)
        assertTrue(clean.meta.nextId >= Balance.RESERVED_ID_FLOOR)
    }

    @Test
    fun sanitizeKeepsValuesThatAreAlreadyLegal() {
        val legal = GameState(
            player = PlayerState(lvl = 7, gold = 42L, currentFloor = 3, maxUnlockedFloor = 9),
            potionSettings = PotionSettings(
                hpAutoThresholdPct = 50,
                mpAutoThresholdPct = 30,
                selectedHpPotion = "Minor Health Flask"
            )
        )

        val clean = StateSanitizer.sanitize(legal)

        assertEquals(7, clean.player.lvl)
        assertEquals(42L, clean.player.gold)
        assertEquals(3, clean.player.currentFloor)
        assertEquals(50, clean.potionSettings.hpAutoThresholdPct)
        assertEquals(30, clean.potionSettings.mpAutoThresholdPct)
        assertEquals("Minor Health Flask", clean.potionSettings.selectedHpPotion)
    }

    @Test
    fun nextIdRewindsPastEveryExistingIdentifier() {
        val state = GameState(
            logs = listOf(LogEntry(id = 5_000L, text = "hi"))
        )

        val next = StateSanitizer.nextIdFrom(state)

        assertTrue(next > 5_000L)
        assertTrue(next >= Balance.RESERVED_ID_FLOOR)
    }
}
