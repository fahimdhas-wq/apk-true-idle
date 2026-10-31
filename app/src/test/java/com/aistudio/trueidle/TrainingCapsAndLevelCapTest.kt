package com.aistudio.trueidle

import com.aistudio.trueidle.core.BeastSalvage
import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.TrainingCaps
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.engine.StateSanitizer
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.TrainingNode
import com.aistudio.trueidle.model.TrainingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingCapsAndLevelCapTest {

    private fun stateWith(vararg nodes: TrainingNode) =
        GameState(training = TrainingState(nodes = nodes.toList()))

    @Test
    fun defaultNodesCarryPerIdCaps() {
        val engine = GameEngine(ContentCatalog.reconcile(GameState()))
        val byId = engine.state.training.nodes.associateBy { it.id }
        assertEquals(TrainingCaps.CRIT_MAX_RANK, byId.getValue("crit_bonus").maxRank)
        assertEquals(TrainingCaps.SPEED_MAX_RANK, byId.getValue("speed_bonus").maxRank)
        assertEquals(TrainingCaps.LUCK_MAX_RANK, byId.getValue("luck_bonus").maxRank)
        assertEquals(TrainingCaps.XP_MAX_RANK, byId.getValue("xp_bonus").maxRank)
    }

    @Test
    fun maxedNodesReachExactlyOneHundredPercent() {
        val d = DerivedStats.of(
            stateWith(
                TrainingNode(id = "crit_bonus", rank = TrainingCaps.CRIT_MAX_RANK, maxRank = TrainingCaps.CRIT_MAX_RANK),
                TrainingNode(id = "speed_bonus", rank = TrainingCaps.SPEED_MAX_RANK, maxRank = TrainingCaps.SPEED_MAX_RANK),
                TrainingNode(id = "luck_bonus", rank = TrainingCaps.LUCK_MAX_RANK, maxRank = TrainingCaps.LUCK_MAX_RANK)
            )
        )
        assertEquals(1.0, d.critChance, 1e-9)
        assertEquals(1.0, d.lootChance, 1e-9)
        assertEquals(2.0, d.atkSpeed, 1e-9)
    }

    @Test
    fun xpBonusNodeIsLimitedToOneHundredThousandPercent() {
        val base = DerivedStats.of(GameState()).xpGain
        val atCap = DerivedStats.of(
            stateWith(TrainingNode(id = "xp_bonus", rank = TrainingCaps.XP_MAX_RANK, maxRank = TrainingCaps.XP_MAX_RANK))
        ).xpGain
        val overCap = DerivedStats.of(
            stateWith(TrainingNode(id = "xp_bonus", rank = 900_000, maxRank = 1_000_000))
        ).xpGain
        assertEquals(base + 1000.0, atCap, 1e-6)
        assertEquals(atCap, overCap, 1e-6)
    }

    @Test
    fun overRankedSaveIsStillBoundedAndSanitized() {
        val raw = stateWith(
            TrainingNode(id = "crit_bonus", rank = 900_000, maxRank = 1_000_000),
            TrainingNode(id = "speed_bonus", rank = 900_000, maxRank = 1_000_000),
            TrainingNode(id = "luck_bonus", rank = 900_000, maxRank = 1_000_000),
            TrainingNode(id = "xp_bonus", rank = 900_000, maxRank = 1_000_000)
        )
        val d = DerivedStats.of(raw)
        assertEquals(1.0, d.critChance, 1e-9)
        assertEquals(1.0, d.lootChance, 1e-9)
        assertEquals(2.0, d.atkSpeed, 1e-9)

        val clean = ContentCatalog.reconcile(StateSanitizer.sanitize(raw))
        val byId = clean.training.nodes.associateBy { it.id }
        assertEquals(TrainingCaps.CRIT_MAX_RANK, byId.getValue("crit_bonus").rank)
        assertEquals(TrainingCaps.SPEED_MAX_RANK, byId.getValue("speed_bonus").rank)
        assertEquals(TrainingCaps.LUCK_MAX_RANK, byId.getValue("luck_bonus").rank)
        assertEquals(TrainingCaps.XP_MAX_RANK, byId.getValue("xp_bonus").rank)
    }

    @Test
    fun sanitizerClampsEveryLevelToMax() {
        val raw = GameState(
            player = GameState().player.copy(lvl = 5_000),
            training = TrainingState(lvl = 400),
            necro = GameState().necro.copy(lvl = 101),
            slayer = GameState().slayer.copy(lvl = 250)
        )
        val clean = StateSanitizer.sanitize(raw)
        assertEquals(GameCaps.MAX_LEVEL, clean.player.lvl)
        assertEquals(GameCaps.MAX_LEVEL, clean.training.lvl)
        assertEquals(GameCaps.MAX_LEVEL, clean.necro.lvl)
        assertEquals(GameCaps.MAX_LEVEL, clean.slayer.lvl)
    }

    @Test
    fun trainingNodePurchaseStopsAtCap() {
        val base = GameState()
        val node = base.training.nodes.first { it.id == "speed_bonus" }
        val maxed = base.copy(
            player = base.player.copy(gold = Long.MAX_VALUE / 4),
            training = base.training.copy(
                nodes = base.training.nodes.map { if (it.id == "speed_bonus") it.copy(rank = node.maxRank) else it }
            )
        )
        val engine = GameEngine(maxed)
        val after = engine.trainNode("speed_bonus")
        assertEquals(node.maxRank, after.training.nodes.first { it.id == "speed_bonus" }.rank)
        assertEquals(maxed.player.gold, after.player.gold)
    }

    @Test
    fun salvageConvertsBeastsIntoBonesAndGold() {
        val a = Beast(id = "a", name = "Wolf", rank = 2, stars = 1)
        val b = Beast(id = "b", name = "Wolf", rank = 3, stars = 2)
        val c = Beast(id = "c", name = "Bear", rank = 1, stars = 1)
        val base = GameState()
        val start = base.copy(beasts = BeastTamingState(beasts = listOf(a, b, c)))
        val engine = GameEngine(start)
        val after = engine.salvageBeasts(setOf("a", "b", "missing"))

        assertEquals(listOf("c"), after.beasts.beasts.map { it.id })
        assertEquals(start.necro.boneEssence + BeastSalvage.bones(a) + BeastSalvage.bones(b), after.necro.boneEssence)
        assertEquals(start.player.gold + BeastSalvage.gold(a) + BeastSalvage.gold(b), after.player.gold)
        assertTrue(BeastSalvage.bones(b) > BeastSalvage.bones(a))
    }

    @Test
    fun salvageWithNoMatchIsNoOp() {
        val start = GameState().copy(beasts = BeastTamingState(beasts = listOf(Beast(id = "a", name = "Wolf"))))
        val engine = GameEngine(start)
        assertEquals(start.beasts, engine.salvageBeasts(emptySet()).beasts)
        assertEquals(start.beasts, engine.salvageBeasts(setOf("nope")).beasts)
    }

    @Test
    fun classCannotExceedMaxLevelOrBankXp() {
        val base = GameState()
        val start = base.copy(
            player = base.player.copy(gold = 1_000_000L),
            training = base.training.copy(lvl = GameCaps.MAX_LEVEL - 1, xp = Long.MAX_VALUE / 2)
        )
        val after = GameEngine(start).trainNode("str_bonus")
        assertEquals(GameCaps.MAX_LEVEL, after.training.lvl)
        assertEquals(0L, after.training.xp)
    }
}
