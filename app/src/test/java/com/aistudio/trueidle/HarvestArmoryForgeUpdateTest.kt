package com.aistudio.trueidle

import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.MiningVein
import com.aistudio.trueidle.model.Plot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HarvestArmoryForgeUpdateTest {

    @Test
    fun gatheringSkillsRequireMoreXpThanGeneralSkills() {
        for (level in listOf(1, 2, 5, 10, 25, 50, 100)) {
            val regular = XpCurves.skill(level)
            val gathering = XpCurves.gatheringSkill(level)
            assertTrue("gathering XP should be at least 3x at level $level", gathering >= regular * 3L)
            assertTrue("gathering XP should stay within rounding of 3x at level $level", gathering <= regular * 3L + 2L)
        }
        assertEquals(270L, XpCurves.gatheringSkill(1))
        assertTrue(XpCurves.gatheringSkill(10) > XpCurves.gatheringSkill(9))
    }

    @Test
    fun boostedFarmingYieldDoesNotMultiplyFarmingSkillXp() {
        val base = GameState()
        val state = base.copy(harvest = base.harvest.copy(
            plots = base.harvest.plots.map { if (it.id == 0) it.copy(isReady = true, proficiencyLevel = 5, proficiencyXp = 0L) else it },
            farmingXp = 0L,
            farmingLvl = 1
        ))
        val engine = GameEngine(state)

        engine.harvestPlot(0)

        val plot = engine.state.harvest.plots.first { it.id == 0 }
        assertEquals(2L, engine.state.harvest.farmingXp)
        assertEquals(32L, plot.proficiencyXp)
    }

    @Test
    fun boostedMiningYieldDoesNotMultiplyMiningSkillXp() {
        val base = GameState()
        val state = base.copy(harvest = base.harvest.copy(
            veins = base.harvest.veins.map { if (it.id == 0) it.copy(isReady = true, proficiencyLevel = 5, proficiencyXp = 0L) else it },
            miningXp = 0L,
            miningLvl = 1
        ))
        val engine = GameEngine(state)

        engine.mineVein(0)

        val vein = engine.state.harvest.veins.first { it.id == 0 }
        assertEquals(3L, engine.state.harvest.miningXp)
        assertEquals(48L, vein.proficiencyXp)
    }

    // -------------------------------------------------------------------------
    // ITEM 3: 20 MINING ORES IN EXACT CANONICAL ORDER
    // -------------------------------------------------------------------------
    @Test
    fun testMiningOresExactOrder() {
        val expectedOres = listOf(
            "Stone", "Coal", "Copper Ore", "Iron Ore", "Tin Ore",
            "Silver Ore", "Gold Ore", "Quartz", "Diamond", "Emerald",
            "Ruby", "Sapphire", "Lapis Lazuli", "Redstone", "Obsidian",
            "Platinum Ore", "Titanium Ore", "Cobalt Ore", "Uranium Ore", "Mithril Ore"
        )
        assertEquals(20, ItemCatalog.ORES.size)
        assertEquals(expectedOres, ItemCatalog.ORES)

        val state = GameEngine().state
        assertEquals(20, state.harvest.veins.size)
        val stateOreNames = state.harvest.veins.map { it.oreName }
        assertEquals(expectedOres, stateOreNames)
    }

    // -------------------------------------------------------------------------
    // ITEM 4: 20 FARMING HERBS IN EXACT CANONICAL ORDER
    // -------------------------------------------------------------------------
    @Test
    fun testFarmingHerbsExactOrder() {
        val expectedHerbs = listOf(
            "Healing Herb", "Lesser Healing Herb", "Mana Herb", "Antidote Herb", "Vitality Herb",
            "Spirit Grass", "Moonshade Herb", "Sunroot Herb", "Silverleaf Herb", "Windwhisper Grass",
            "Bloodroot", "Frostbloom", "Emberleaf", "Dreamer's Sage", "Golden Ginseng",
            "Moonlight Blossom", "Sacred Lotus", "Mandrake Root", "Spiritbloom", "Worldtree Leaf"
        )
        assertEquals(20, ItemCatalog.HERBS.size)
        assertEquals(expectedHerbs, ItemCatalog.HERBS)

        val state = GameEngine().state
        assertEquals(20, state.harvest.plots.size)
        val stateHerbNames = state.harvest.plots.map { it.herbName }
        assertEquals(expectedHerbs, stateHerbNames)
    }

    // -------------------------------------------------------------------------
    // ITEM 2: REWARD MATH (XP AND PROFICIENCY GRANTED IN EQUAL AMOUNTS TO YIELD)
    // -------------------------------------------------------------------------
    @Test
    fun testEqualRewardMathForFarming() {
        // Base plot yield = 2.0. With proficiency lvl 1, multiplier = 1.0 -> yield = 2.0 items.
        // Gaining 2 items must grant exactly 2 Farming XP and 2 Proficiency XP.
        val baseState = GameState().copy(
            harvest = GameState().harvest.copy(
                plots = GameState().harvest.plots.map {
                    if (it.id == 0) it.copy(isReady = true, proficiencyLevel = 1, proficiencyXp = 0L) else it
                },
                farmingXp = 0L,
                farmingLvl = 1
            )
        )
        val engine = GameEngine(baseState)
        val initialItems = engine.state.inventory.find { it.name == ItemCatalog.HEALING_HERB }?.count ?: 0.0

        engine.harvestPlot(0)

        val afterItems = engine.state.inventory.find { it.name == ItemCatalog.HEALING_HERB }?.count ?: 0.0
        val itemsGained = afterItems - initialItems
        val plot = engine.state.harvest.plots[0]

        // Yield is 2.0
        assertEquals(2.0, itemsGained, 1e-6)
        // Farming XP gained is 2L (equal to items gained)
        assertEquals(2L, engine.state.harvest.farmingXp)
        // Proficiency XP gained is 2L (equal to items gained and equal to skill XP)
        assertEquals(2L, plot.proficiencyXp)
    }

    @Test
    fun testEqualRewardMathForMining() {
        // Base vein yield = 3.0. With proficiency lvl 1, multiplier = 1.0 -> yield = 3.0 items.
        // Gaining 3 items must grant exactly 3 Mining XP and 3 Proficiency XP.
        val baseState = GameState().copy(
            harvest = GameState().harvest.copy(
                veins = GameState().harvest.veins.map {
                    if (it.id == 0) it.copy(isReady = true, proficiencyLevel = 1, proficiencyXp = 0L) else it
                },
                miningXp = 0L,
                miningLvl = 1
            )
        )
        val engine = GameEngine(baseState)
        val initialItems = engine.state.inventory.find { it.name == ItemCatalog.STONE }?.count ?: 0.0

        engine.mineVein(0)

        val afterItems = engine.state.inventory.find { it.name == ItemCatalog.STONE }?.count ?: 0.0
        val itemsGained = afterItems - initialItems
        val vein = engine.state.harvest.veins[0]

        // Yield is 3.0
        assertEquals(3.0, itemsGained, 1e-6)
        // Mining XP gained is 3L (equal to items gained)
        assertEquals(3L, engine.state.harvest.miningXp)
        // Proficiency XP gained is 3L (equal to items gained and equal to skill XP)
        assertEquals(3L, vein.proficiencyXp)
    }

    // -------------------------------------------------------------------------
    // ITEM 5: ARMORY 13 EQUIPMENT GROUPS & LEGACY SAVE MIGRATION
    // -------------------------------------------------------------------------
    @Test
    fun testArmoryEquipmentGroupsAndStats() {
        assertEquals(3, Equipment.WEAPON_SLOTS.size)
        assertTrue(Equipment.WEAPON_SLOTS.containsAll(listOf(Equipment.SLOT_RIFLE, Equipment.SLOT_SIDEARM, Equipment.SLOT_BLADE)))

        assertEquals(7, Equipment.ARMOR_SLOTS.size)
        assertTrue(Equipment.ARMOR_SLOTS.containsAll(listOf(
            Equipment.SLOT_VISOR, Equipment.SLOT_VEST, Equipment.SLOT_GRIPS,
            Equipment.SLOT_PANTS, Equipment.SLOT_BOOTS, Equipment.SLOT_DUSTER, Equipment.SLOT_RIG
        )))

        assertEquals(3, Equipment.NODE_SLOTS.size)
        assertTrue(Equipment.NODE_SLOTS.containsAll(listOf(Equipment.SLOT_NODE_I, Equipment.SLOT_NODE_II, Equipment.SLOT_NODE_III)))

        assertEquals(13, Equipment.GROUPS.size)

        // Equipping a Rifle grants ATK
        val rifle = InventoryItem(id = "r1", name = "Test Rifle", category = "Equipment", slot = Equipment.SLOT_RIFLE, atk = 50.0, isEquipped = true)
        // Equipping a Vest grants DEF and VIT
        val vest = InventoryItem(id = "v1", name = "Test Vest", category = "Equipment", slot = Equipment.SLOT_VEST, def = 30.0, vit = 20.0, isEquipped = true)
        // Equipping Node I grants DEF and VIT
        val node1 = InventoryItem(id = "n1", name = "Test Node I", category = "Equipment", slot = Equipment.SLOT_NODE_I, def = 15.0, vit = 10.0, isEquipped = true)

        val state = GameState(inventory = listOf(rifle, vest, node1))
        val d = DerivedStats.of(state)

        assertEquals(50.0, d.weaponDmg, 1e-6)
        assertEquals(45.0, d.armorDef, 1e-6) // 30 + 15
        assertEquals(40.0, d.vit, 1e-6) // base 10 + item vit (20 + 10)
    }

    @Test
    fun testLegacySaveEquipmentSlotMigration() {
        // Player with legacy slots "Weapon", "Armor", "Ring", "Amulet"
        val oldItems = listOf(
            InventoryItem(id = "w_old", name = "Old Sword", category = "Equipment", slot = "Weapon", tier = 2, isEquipped = true),
            InventoryItem(id = "a_old", name = "Old Plate", category = "Equipment", slot = "Armor", tier = 2, isEquipped = true),
            InventoryItem(id = "r_old", name = "Old Band", category = "Equipment", slot = "Ring", tier = 2, isEquipped = true),
            InventoryItem(id = "m_old", name = "Old Talisman", category = "Equipment", slot = "Amulet", tier = 2, isEquipped = true)
        )
        val oldState = GameState(inventory = oldItems)
        val engine = GameEngine(oldState)

        val rifle = engine.state.inventory.first { it.id == "w_old" }
        assertEquals(Equipment.SLOT_RIFLE, rifle.slot)
        assertTrue(rifle.isEquipped)

        val vest = engine.state.inventory.first { it.id == "a_old" }
        assertEquals(Equipment.SLOT_VEST, vest.slot)
        assertTrue(vest.isEquipped)

        val node1 = engine.state.inventory.first { it.id == "r_old" }
        assertEquals(Equipment.SLOT_NODE_I, node1.slot)
        assertTrue(node1.isEquipped)

        val node2 = engine.state.inventory.first { it.id == "m_old" }
        assertEquals(Equipment.SLOT_NODE_II, node2.slot)
        assertTrue(node2.isEquipped)
    }

    // -------------------------------------------------------------------------
    // ITEM 6 & 7: FORGE TAB EQUIPMENT GROUPS SEPARATION & HERB CRAFTING
    // -------------------------------------------------------------------------
    @Test
    fun testForgeEquipmentGroupsSeparation() {
        assertEquals(13, ContentCatalog.RECIPES_BY_GROUP.size)
        Equipment.GROUPS.forEach { group ->
            val recipes = ContentCatalog.RECIPES_BY_GROUP[group]
            assertNotNull("Recipes for group $group should exist", recipes)
            assertEquals(TierConfig.size, recipes!!.size)
            recipes.forEach { recipe ->
                assertEquals(group, recipe.slot)
                assertTrue(recipe.name.endsWith(group))
            }
        }
    }

    @Test
    fun testHerbCraftingPotionsDistinctFromEquipment() {
        assertEquals(20, ItemCatalog.POTIONS.size)
        // All potions use one of the 20 canonical herbs
        ItemCatalog.POTIONS.forEach { potion ->
            assertTrue("${potion.herb} is not in HERBS", ItemCatalog.HERBS.contains(potion.herb))
            assertTrue(potion.heal > 0.0)
            assertTrue(potion.herbCost > 0)
        }
    }
}
