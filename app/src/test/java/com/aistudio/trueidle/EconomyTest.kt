package com.aistudio.trueidle

import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EconomyTest {

    private fun resource(name: String, count: Int) = InventoryItem(
        id = "test_${name.replace(' ', '_')}",
        name = name,
        category = ItemCatalog.CATEGORY_RESOURCE,
        count = count,
        tier = 1,
        statDesc = "test resource",
        sellValue = 1L
    )

    /** The material-usage cap defaults are off, so these tests exercise the plain economy path. */
    private fun engineWith(vararg items: InventoryItem) = GameEngine(
        initialState = GameState(
            inventory = items.toList(),
            automation = GameState().automation.copy(craftMaterialUsagePct = 0)
        ),
        clock = FakeClock(),
        random = Random(7)
    )

    @Test
    fun testForgeConsumesOreAndBakesTierStats() {
        val engine = engineWith(resource(ItemCatalog.COPPER_ORE, 10))

        engine.craftEquipment("Bronze Glaive", 1, Equipment.SLOT_WEAPON, ItemCatalog.COPPER_ORE, 6)

        assertEquals(4, engine.state.inventory.first { it.name == ItemCatalog.COPPER_ORE }.count)
        val forged = engine.state.inventory.first { it.name == "Bronze Glaive" }
        assertEquals(1, forged.tier)
        assertEquals(12.0, forged.atk, 1e-6)
        assertEquals(0.0, forged.def, 1e-6)
    }

    @Test
    fun testForgeRefusesWhenOreIsShort() {
        val engine = engineWith(resource(ItemCatalog.COPPER_ORE, 5))

        engine.craftEquipment("Bronze Glaive", 1, Equipment.SLOT_WEAPON, ItemCatalog.COPPER_ORE, 6)

        assertEquals(5, engine.state.inventory.first { it.name == ItemCatalog.COPPER_ORE }.count)
        assertNull(engine.state.inventory.firstOrNull { it.name == "Bronze Glaive" })
    }

    @Test
    fun testBrewChargesTheCatalogCost() {
        val engine = engineWith(resource(ItemCatalog.FELWEED, 4))

        engine.brewPotion("Minor Health Flask", ItemCatalog.FELWEED, 4)

        val potion = engine.state.inventory.firstOrNull { it.category == ItemCatalog.CATEGORY_POTION }
        assertNotNull("expected a brewed potion", potion)
        assertEquals(50.0, ItemCatalog.potionHeal(potion!!.name), 1e-6)
        assertNull(engine.state.inventory.firstOrNull { it.name == ItemCatalog.FELWEED })
    }

    /**
     * Regression guard: the engine used to charge whatever count the caller passed, so the Armory
     * screen and the catalog could quote different prices for the same potion.
     */
    @Test
    fun testBrewIgnoresACheaperCallerPrice() {
        val engine = engineWith(resource(ItemCatalog.FELWEED, 3))

        engine.brewPotion("Minor Health Flask", ItemCatalog.FELWEED, 1)

        assertNull(engine.state.inventory.firstOrNull { it.category == ItemCatalog.CATEGORY_POTION })
        assertEquals(3, engine.state.inventory.first { it.name == ItemCatalog.FELWEED }.count)
    }

    @Test
    fun testPotionHealsAndIsNotWastedAtFullHealth() {
        val potion = InventoryItem(
            id = "p1",
            name = "Minor Health Flask",
            category = ItemCatalog.CATEGORY_POTION,
            count = 2,
            tier = 1,
            statDesc = "Restores 50 HP",
            sellValue = 15L
        )
        val hurt = GameEngine(
            initialState = GameState(
                player = GameState().player.copy(hp = 10.0),
                inventory = listOf(potion)
            ),
            clock = FakeClock(),
            random = Random(7)
        )
        hurt.usePotion("p1")
        assertTrue("a potion should heal a wounded hero", hurt.state.player.hp > 10.0)
        assertEquals(1, hurt.state.inventory.first { it.id == "p1" }.count)

        val healthy = GameEngine(
            initialState = GameState(inventory = listOf(potion)),
            clock = FakeClock(),
            random = Random(7)
        )
        healthy.usePotion("p1")
        assertEquals("a potion must not be consumed at full health", 2, healthy.state.inventory.first { it.id == "p1" }.count)
    }

    @Test
    fun testWornGearCannotBeSold() {
        val worn = InventoryItem(
            id = "worn",
            name = "Rusty Blade",
            category = ItemCatalog.CATEGORY_EQUIPMENT,
            count = 1,
            tier = 1,
            slot = Equipment.SLOT_WEAPON,
            statDesc = "+8 Damage",
            sellValue = 10L,
            isEquipped = true,
            atk = 8.0
        )
        val engine = GameEngine(
            initialState = GameState(inventory = listOf(worn)),
            clock = FakeClock(),
            random = Random(7)
        )

        engine.sellItem("worn")

        assertEquals(1, engine.state.inventory.size)
        assertEquals(1, engine.state.inventory.first { it.id == "worn" }.count)
    }

    @Test
    fun testTalentUpgradeSpendsItsDeclaredPointCost() {
        val engine = GameEngine(clock = FakeClock(), random = Random(7))
        val before = engine.state.talent.talentPoints

        engine.upgradeTalent("berserk")

        assertEquals(before - 1, engine.state.talent.talentPoints)
        assertEquals(2, engine.state.talent.activeTalents.first { it.id == "berserk" }.rank)
    }
}
