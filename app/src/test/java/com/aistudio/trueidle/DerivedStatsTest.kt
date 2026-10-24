package com.aistudio.trueidle

import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.TrainingNode
import com.aistudio.trueidle.model.TrainingState
import org.junit.Assert.assertEquals
import org.junit.Test

class DerivedStatsTest {

    @Test
    fun testDefaultsMatchStartingValues() {
        val state = GameState()
        val d = DerivedStats.of(state)

        assertEquals(100.0, d.maxHp, 1e-6)
        assertEquals(25.0, d.maxShield, 1e-6)
        assertEquals(50.0, d.maxMana, 1e-6)
        assertEquals(0.35, d.lootChance, 1e-6)
        assertEquals(0.15, d.tameChance, 1e-6)
        assertEquals(8.0, d.weaponDmg, 1e-6)
        assertEquals(4.0, d.armorDef, 1e-6)
    }

    @Test
    fun testDojoRankBoostsTargetStat() {
        val nodes = listOf(
            TrainingNode(id = "str_bonus", name = "Titan Strength", stat = "", rank = 2),
            TrainingNode(id = "vit_bonus", name = "Iron Constitution", stat = "", rank = 1),
            TrainingNode(id = "int_bonus", name = "Arcane Mind", stat = "", rank = 1),
            TrainingNode(id = "luck_bonus", name = "Fortune's Favor", stat = "", rank = 2),
            TrainingNode(id = "speed_bonus", name = "Swift Strikes", stat = "", rank = 1)
        )
        val state = GameState(training = TrainingState(nodes = nodes))
        val d = DerivedStats.of(state)

        // STR: base 10 + 5 * 2 = 20
        assertEquals(20.0, d.str, 1e-6)
        // Max HP: 40 + 5 * 10 + 10 * 1 + 50 * 1 = 150
        assertEquals(150.0, d.maxHp, 1e-6)
        // Max Mana: 30 + 2 * 10 + 25 * 1 = 75
        assertEquals(75.0, d.maxMana, 1e-6)
        // Luck: base 5 + 5 * 2 = 15 -> lootChance = 0.30 + 0.15 = 0.45
        assertEquals(0.45, d.lootChance, 1e-6)
        // Atk Speed: base 1.0 * 1.05^1 = 1.05
        assertEquals(1.05, d.atkSpeed, 1e-6)
    }

    @Test
    fun testGearSwapUpdatesWeaponAndArmor() {
        val inv = listOf(
            InventoryItem(id = "w1", name = "Sword", category = "Equipment", slot = "Weapon", atk = 24.0, isEquipped = true),
            InventoryItem(id = "a1", name = "Plate", category = "Equipment", slot = "Armor", def = 16.0, isEquipped = true)
        )
        val state = GameState(inventory = inv)
        val d = DerivedStats.of(state)

        assertEquals(24.0, d.weaponDmg, 1e-6)
        assertEquals(16.0, d.armorDef, 1e-6)
    }
}
