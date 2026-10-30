package com.aistudio.trueidle

import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.PlayerState
import org.junit.Assert.*
import org.junit.Test

class TalentAndDisciplineTest {

    @Test
    fun testTheStrugglePassivesScaleWithDamageTaken() {
        val baseState = GameState()
        val baseStats = DerivedStats.of(baseState)

        // Damage taken = 1000L. Rank = 1 by default.
        val damagedState = baseState.copy(
            talent = baseState.talent.copy(struggleDamageTaken = 1000L)
        )
        val damagedStats = DerivedStats.of(damagedState)

        // 1000 / 100 = 10 * 5.0 = 50.0
        assertEquals(50.0, damagedStats.struggleHpBonus, 1e-6)
        // 1000 / 500 = 2 * 1.0 = 2.0
        assertEquals(2.0, damagedStats.struggleDefBonus, 1e-6)
        // 1000 / 150 = 6 * 3.0 = 18.0
        assertEquals(18.0, damagedStats.struggleManaBonus, 1e-6)
        // 1000 / 250 = 4 * 2.0 = 8.0
        assertEquals(8.0, damagedStats.struggleAtkBonus, 1e-6)

        // Verify total stats reflect struggle bonuses
        assertEquals(baseStats.maxHp + 50.0, damagedStats.maxHp, 1e-6)
        assertEquals(baseStats.armorDef + 2.0, damagedStats.armorDef, 1e-6)
        assertEquals(baseStats.maxMana + 18.0, damagedStats.maxMana, 1e-6)
    }

    @Test
    fun testActivateTalentAppliesBuffAndCooldown() {
        val engine = GameEngine()
        // Berserk costs 20 mana. The starting hero has 50 mana.
        val initial = engine.activateTalent("berserk")

        val berserk = initial.talent.activeTalents.first { it.id == "berserk" }
        assertTrue("Talent should be active", berserk.isActive)
        assertTrue("Talent should have cooldown remaining", berserk.cooldownRemaining > 0.0)
        assertEquals("Berserk should have spent its 20 mana", 30.0, initial.player.mana, 1e-6)

        // When Berserk is active, DerivedStats atkSpeed is multiplied by 1.5
        val stats = DerivedStats.of(initial)
        assertTrue(stats.atkSpeed >= 1.5)
    }

    @Test
    fun testUpgradeTalentConsumesTalentPoints() {
        val engine = GameEngine()
        val initialPoints = engine.state.talent.talentPoints
        assertTrue("Should have initial talent points", initialPoints > 0)

        val updated = engine.upgradeTalent("berserk")
        assertEquals(initialPoints - 1, updated.talent.talentPoints)
        val upgradedTalent = updated.talent.activeTalents.first { it.id == "berserk" }
        assertEquals(2, upgradedTalent.rank)
    }

    @Test
    fun testDisciplineUpgrades() {
        // GameEngine.state has a private setter, so seed the gold through the constructor.
        val engine = GameEngine(
            initialState = GameState(player = PlayerState(gold = 1000L))
        )

        // Upgrade the Harvest "Accelerated Bio-Growth" discipline.
        val harvestUpgraded = engine.upgradeDiscipline(DisciplineCategory.HARVEST, "harvest_speed")
        val upg = harvestUpgraded.harvest.upgrades.first { it.id == "harvest_speed" }
        assertEquals(1, upg.rank)
        assertTrue(harvestUpgraded.player.gold < 1000L)

        // Upgrade the Armory "Molecular Tempering" discipline.
        val armoryUpgraded = engine.upgradeDiscipline(DisciplineCategory.ARMORY, "forge_quality")
        val armUpg = armoryUpgraded.armory.upgrades.first { it.id == "forge_quality" }
        assertEquals(1, armUpg.rank)
    }

    @Test
    fun testSaveSerializationCompatibility() {
        val engine = GameEngine()
        val exported = engine.exportSaveJson()
        assertNotNull(exported)
        assertTrue(exported.contains("harvest"))
        assertTrue(exported.contains("armory"))
        assertTrue(exported.contains("essence"))
        assertTrue(exported.contains("talent"))

        val importSuccess = engine.importSaveJson(exported)
        assertTrue("Save state should deserialize cleanly", importSuccess)
    }

    @Test
    fun testMiningVeinsProgressAndExtract() {
        val engine = GameEngine()
        for (i in 1..25) {
            engine.tick(0.25f)
        }
        val stone = engine.state.harvest.veins[0]
        assertTrue("Stone vein should be ready after 25 ticks (6.25s)", stone.isReady)
        val initialOres = engine.state.inventory.find { it.name == "Stone" }?.count ?: 0.0
        engine.mineVein(0)
        val afterOres = engine.state.inventory.find { it.name == "Stone" }?.count ?: 0.0
        assertTrue("Should have collected stone", afterOres > initialOres)
    }

    @Test
    fun testHarvestAutoQueueRespectsUnlocksAndCapacity() {
        val base = GameState()
        val levelFive = base.copy(
            harvest = base.harvest.copy(farmingLvl = 5, miningLvl = 5)
        )
        val engine = GameEngine(levelFive)

        engine.togglePlotAutoQueue(0)
        assertTrue(engine.state.harvest.plots[0].autoQueue)
        engine.togglePlotAutoQueue(1)
        assertTrue(engine.state.harvest.plots[0].autoQueue)
        assertFalse(engine.state.harvest.plots[1].autoQueue)

        engine.togglePlotAutoQueue(0)
        engine.togglePlotAutoQueue(1)
        assertFalse(engine.state.harvest.plots[0].autoQueue)
        assertTrue(engine.state.harvest.plots[1].autoQueue)

        engine.toggleVeinAutoQueue(0)
        assertTrue(engine.state.harvest.veins[0].autoQueue)
        engine.toggleVeinAutoQueue(1)
        assertTrue(engine.state.harvest.veins[0].autoQueue)
        assertFalse(engine.state.harvest.veins[1].autoQueue)

        engine.togglePlotAutoQueue(2)
        engine.toggleVeinAutoQueue(2)
        assertFalse(engine.state.harvest.plots[2].autoQueue)
        assertFalse(engine.state.harvest.veins[2].autoQueue)

        val levelTen = base.copy(
            harvest = base.harvest.copy(farmingLvl = 10, miningLvl = 10)
        )
        engine.restore(levelTen)
        engine.togglePlotAutoQueue(0)
        engine.togglePlotAutoQueue(1)
        engine.toggleVeinAutoQueue(0)
        engine.toggleVeinAutoQueue(1)
        assertTrue(engine.state.harvest.plots[0].autoQueue)
        assertTrue(engine.state.harvest.plots[1].autoQueue)
        assertTrue(engine.state.harvest.veins[0].autoQueue)
        assertTrue(engine.state.harvest.veins[1].autoQueue)
    }

    @Test
    fun testProficiencyYieldScaling()    @Test
    fun testProficiencyYieldScaling() {
        // Level 1 multiplier = 1.0 (2^0)
        assertEquals(1.0, XpCurves.proficiencyYieldMultiplier(1), 1e-6)
        // Level 2 multiplier = 2.0 (2^1)
        assertEquals(2.0, XpCurves.proficiencyYieldMultiplier(2), 1e-6)
        // Level 3 multiplier = 4.0 (2^2)
        assertEquals(4.0, XpCurves.proficiencyYieldMultiplier(3), 1e-6)
        // Level 4 multiplier = 8.0 (2^3)
        assertEquals(8.0, XpCurves.proficiencyYieldMultiplier(4), 1e-6)
        // Level 100 multiplier = 2^99
        val maxMult = XpCurves.proficiencyYieldMultiplier(100)
        assertTrue("Max proficiency yield multiplier should exceed 1e29", maxMult > 6.0e29)
    }

    @Test
    fun testCombatKillBatching() {
        val engine = GameEngine()
        // Give player immense damage to trigger overkill batching
        val strongState = engine.state.copy(
            player = engine.state.player.copy(hp = 1000.0),
            training = engine.state.training.copy(
                nodes = engine.state.training.nodes.map {
                    if (it.id == "str_bonus") it.copy(rank = 100) else it
                }
            )
        )
        val testEngine = GameEngine(initialState = strongState)
        val initialKills = testEngine.state.lifetime.totalKills
        testEngine.tick(0.25f)
        // Kill batching resolves multiple monster kills in a single analytical tick without lag
        assertTrue("Kill batching should record kills", testEngine.state.lifetime.totalKills > initialKills)
    }
}
