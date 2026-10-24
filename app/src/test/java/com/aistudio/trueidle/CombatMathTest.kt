package com.aistudio.trueidle

import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.Minion
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.NecromancyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatMathTest {

    @Test
    fun testMitigationCurve() {
        assertEquals(1.0, CombatMath.mitigation(0.0), 1e-6)
        assertEquals(0.5, CombatMath.mitigation(100.0), 1e-6)
        assertEquals(1.0 / 3.0, CombatMath.mitigation(200.0), 1e-6)
        // Negative armor clamped to 0
        assertEquals(1.0, CombatMath.mitigation(-50.0), 1e-6)
    }

    @Test
    fun testHeroDpsCalculation() {
        val derived = Derived(
            str = 10.0,
            intVal = 10.0,
            vit = 10.0,
            luck = 5.0,
            atkSpeed = 1.0,
            maxHp = 100.0,
            maxShield = 25.0,
            maxMana = 50.0,
            weaponDmg = 8.0,
            armorDef = 4.0,
            lootChance = 0.35,
            tameChance = 0.15,
            smiteBonus = 0.0
        )
        // (str * 1.2 + weaponDmg + smiteBonus) * atkSpeed = (12 + 8 + 0) * 1.0 = 20.0
        assertEquals(20.0, CombatMath.heroDps(derived), 1e-6)
    }

    @Test
    fun testTotalDpsSumsAllComponents() {
        val derived = Derived(
            str = 10.0,
            intVal = 10.0,
            vit = 10.0,
            luck = 5.0,
            atkSpeed = 1.0,
            maxHp = 100.0,
            maxShield = 25.0,
            maxMana = 50.0,
            weaponDmg = 8.0,
            armorDef = 4.0,
            lootChance = 0.35,
            tameChance = 0.15,
            smiteBonus = 0.0
        )
        val beasts = BeastTamingState(
            beasts = listOf(
                Beast(id = "b1", name = "Wolf", rank = 1, stars = 1, dps = 10.0)
            )
        )
        val necro = NecromancyState(
            army = listOf(
                Minion(id = "m1", type = "Skeleton", count = 2, lvl = 1, dps = 5.0)
            )
        )
        val state = GameState(beasts = beasts, necro = necro)

        val heroDps = CombatMath.heroDps(derived) // 20.0
        val beastDps = CombatMath.beastDps(beasts) // 10.0
        val armyDps = CombatMath.armyDps(necro) // 10.0
        val expected = heroDps + beastDps + armyDps // 40.0

        assertEquals(40.0, CombatMath.totalDps(state, derived), 1e-6)
    }

    @Test
    fun testIncomingDamageFloorsToOne() {
        val m = Monster(damage = 1.0)
        val derived = Derived(
            str = 10.0, intVal = 10.0, vit = 10.0, luck = 5.0, atkSpeed = 1.0,
            maxHp = 100.0, maxShield = 25.0, maxMana = 50.0,
            weaponDmg = 8.0, armorDef = 100.0,
            lootChance = 0.35, tameChance = 0.15, smiteBonus = 0.0
        )
        // raw - armorDef * 0.3 = 1.0 - 30.0 = -29.0 -> coerced to 1.0
        assertEquals(1.0, CombatMath.incomingPerSec(m, derived, aegis = false), 1e-6)
    }
}
