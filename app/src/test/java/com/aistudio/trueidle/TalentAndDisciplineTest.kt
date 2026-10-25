package com.aistudio.trueidle

import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
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
        // Berserk costs 20 mana. Ensure hero has 50 mana.
        val initial = engine.activateTalent("berserk")

        val berserk = initial.talent.activeTalents.first { it.id == "berserk" }
        assertTrue("Talent should be active", berserk.isActive)
        assertTrue("Talent should have cooldown remaining", berserk.cooldownRemaining > 0.0)

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
        val engine = GameEngine()
        // Give hero 1000 gold
        engine.state = engine.state.copy(
            player = engine.state.player.copy(gold = 1000L)
        )

        // Upgrade Harvest "hydroponic_efficiency"
        val harvestUpgraded = engine.upgradeDiscipline("harvest", "hydroponic_efficiency")
        val upg = harvestUpgraded.harvest.upgrades.first { it.id == "hydroponic_efficiency" }
        assertEquals(1, upg.rank)
        assertTrue(harvestUpgraded.player.gold < 1000L)

        // Upgrade Armory "forge_quality"
        val armoryUpgraded = engine.upgradeDiscipline("armory", "forge_quality")
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
}
