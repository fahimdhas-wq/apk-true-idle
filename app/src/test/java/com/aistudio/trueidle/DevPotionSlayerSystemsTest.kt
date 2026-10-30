package com.aistudio.trueidle

import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.PlayerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the expansion's engine-level contracts: dev cheats, the 20-herb potion consumption path,
 * randomized any-hostile Slayer directives, legacy gear migration and batch selling.
 */
class DevPotionSlayerSystemsTest {

    @Test
    fun devCurrencyGrantsAndSpeedClamps() {
        val engine = GameEngine(GameState())
        val startGold = engine.state.player.gold

        engine.devAddCurrency("GOLD", 100L)
        assertEquals(startGold + 100L, engine.state.player.gold)

        engine.devAddCurrency("DIAMONDS", 7L)
        assertEquals(7L, engine.state.player.diamonds)

        // The panel exposes 1x/5x/10x/50x; anything else clamps into that range.
        engine.setGameSpeed(999)
        assertEquals(50, engine.state.dev.gameSpeed)
        engine.setGameSpeed(0)
        assertEquals(1, engine.state.dev.gameSpeed)
    }

    @Test
    fun devUnlockAllUnlocksSectorsAchievementsAndTalents() {
        val engine = GameEngine(GameState())
        engine.devUnlockAll()

        assertEquals(100, engine.state.player.maxUnlockedFloor)
        assertTrue("every achievement must be unlocked", engine.state.achievements.all { it.isUnlocked })
        assertTrue(
            "every power talent must be maxed",
            engine.state.talent.activeTalents.all { it.rank == it.maxRank }
        )
        assertTrue("unlock-all must be recorded in the dev settings", engine.state.dev.unlockedAll)
    }

    @Test
    fun manualPotionAppliesDirectHealAndRegenBuff() {
        val potion = InventoryItem(
            id = "potion_1",
            name = "Vitality Elixir",
            category = ItemCatalog.CATEGORY_POTION,
            count = 2,
            tier = 1,
            statDesc = "test",
            sellValue = 5L
        )
        val state = GameState(
            player = PlayerState(hp = 10.0, mana = 0.0),
            inventory = listOf(potion)
        )
        val engine = GameEngine(state)

        engine.usePotionByName("Vitality Elixir")

        assertTrue("direct missing-HP heal must apply", engine.state.player.hp > 10.0)
        val remaining = engine.state.inventory.firstOrNull { it.id == "potion_1" }
        assertNotNull("one potion must be consumed, not the stack", remaining)
        assertEquals(1.0, remaining!!.count, 0.0)
        assertTrue(
            "the 15s regeneration buff must be active",
            engine.state.activeBuffs.any { it.type == "REGEN_HP" && it.durationRemaining > 0.0 }
        )
    }

    @Test
    fun potionDamageMitigationAndShieldBrewsRegisterBuffs() {
        val shieldBrew = InventoryItem(
            id = "potion_shield",
            name = "Moonshade Concoction",
            category = ItemCatalog.CATEGORY_POTION,
            count = 1,
            tier = 1,
            statDesc = "test",
            sellValue = 5L
        )
        val state = GameState(
            player = PlayerState(hp = 10.0, mana = 0.0),
            inventory = listOf(shieldBrew)
        )
        val engine = GameEngine(state)

        engine.usePotionByName("Moonshade Concoction")

        assertTrue(
            "the shield buff must be tracked",
            engine.state.activeBuffs.any { it.type == "SHIELD" && it.value > 0.0 }
        )
        assertTrue("the potion stack must be spent", engine.state.inventory.isEmpty())
    }

    @Test
    fun slayerDirectivesRollRandomAnyHostileKillCounts() {
        val engine = GameEngine(GameState())
        val tasks = engine.state.slayer.tasks
        assertEquals(10, tasks.size)
        tasks.forEach { task ->
            assertEquals(SlayerMath.ANY_HOSTILE, task.targetName)
            assertTrue(
                "kill count ${task.requiredKills} must come from the thematic pool",
                task.requiredKills in SlayerMath.KILL_COUNTS
            )
        }
    }

    @Test
    fun legacyGearNamesMigrateToCanonicalGroups() {
        val legacy = InventoryItem(
            id = "legacy_1",
            name = "Iron Greatsword",
            category = "Equipment",
            count = 1,
            tier = 1,
            slot = "Weapon",
            statDesc = "+5 ATK",
            sellValue = 10L,
            atk = 5.0
        )
        val engine = GameEngine(GameState(inventory = listOf(legacy)))

        val migrated = engine.state.inventory.first { it.id == "legacy_1" }
        assertEquals(Equipment.SLOT_BLADE, migrated.slot)
        assertNotEquals("Iron Greatsword", migrated.name)
        assertEquals("F- ${Equipment.SLOT_BLADE}", migrated.name)
    }

    @Test
    fun batchSellClampsToOwnedQuantity() {
        val ore = InventoryItem(
            id = "res_1",
            name = "Copper Ore",
            category = "Resource",
            count = 5,
            tier = 1,
            statDesc = "test",
            sellValue = 3L
        )
        val engine = GameEngine(GameState(player = PlayerState(gold = 0L), inventory = listOf(ore)))
        val before = engine.state.player.gold

        // A 1000x batch sell on a 5-item stack sells only the 5 owned.
        engine.sellItem("res_1", 1000)

        assertTrue("stack must be sold out", engine.state.inventory.none { it.id == "res_1" })
        assertEquals(before + 5L * 3L, engine.state.player.gold)
    }

    @Test
    fun potionCatalogExposesHealthAndManaLoadouts() {
        assertEquals(20, ItemCatalog.POTIONS.size)
        assertTrue(ItemCatalog.HP_POTIONS.isNotEmpty())
        assertTrue(ItemCatalog.MANA_POTIONS.isNotEmpty())
        assertTrue(ItemCatalog.HP_POTIONS.all { it.restoresHp })
        assertTrue(ItemCatalog.MANA_POTIONS.all { it.restoresMana })
    }
}
