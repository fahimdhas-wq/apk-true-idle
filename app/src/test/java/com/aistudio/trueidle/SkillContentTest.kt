package com.aistudio.trueidle

import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.CombatMath
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.MinionCatalog
import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.MiningVein
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The skill content expansion: every addition must be reachable, mechanically wired, and applied to
 * saves that predate it exactly once.
 */
class SkillContentTest {

    private val reconciled = ContentCatalog.reconcile(GameState())

    // ------------------------------------------------------------------ catalog shapes

    @Test
    fun `expanded harvest content is present`() {
        assertEquals(5, reconciled.harvest.plots.size)
        assertEquals(5, reconciled.harvest.veins.size)
        assertEquals(5, reconciled.harvest.veins.map { it.id }.toSet().size)
        assertEquals(listOf("Starbloom", "Voidcap"), reconciled.harvest.plots.map { it.herbName }.drop(3))
        assertEquals(listOf("Silver Ore", "Obsidian Ore"), reconciled.harvest.veins.map { it.oreName }.drop(3))
    }

    @Test
    fun `each new herb and ore has a real sell value`() {
        assertTrue(ItemCatalog.ORES.containsAll(listOf(ItemCatalog.SILVER_ORE, ItemCatalog.OBSIDIAN_ORE)))
        assertTrue(ItemCatalog.HERBS.containsAll(listOf(ItemCatalog.STARBLOOM, ItemCatalog.VOIDCAP)))
        assertTrue(ItemCatalog.resource(ItemCatalog.SILVER_ORE).sellValue > 0L)
        assertTrue(ItemCatalog.resource(ItemCatalog.OBSIDIAN_ORE).sellValue > 0L)
        assertTrue(ItemCatalog.resource(ItemCatalog.STARBLOOM).sellValue > 0L)
        assertTrue(ItemCatalog.resource(ItemCatalog.VOIDCAP).sellValue > 0L)
        assertTrue(ItemCatalog.resource("Unknown Salvage").sellValue > 0L)
    }

    @Test
    fun `forge recipes cover tiers one to six and quote the stats the engine forges`() {
        assertEquals(12, ContentCatalog.FORGE_RECIPES.size)
        assertEquals(12, ContentCatalog.FORGE_RECIPES.map { it.name }.toSet().size)
        ContentCatalog.FORGE_RECIPES.forEach { recipe ->
            assertTrue("${recipe.reqOre} is not a known ore", ItemCatalog.ORES.contains(recipe.reqOre))
            val expected = if (recipe.slot == Equipment.SLOT_WEAPON) {
                "+${recipe.tier * 12} ATK"
            } else {
                "+${recipe.tier * 8} DEF"
            }
            assertEquals(recipe.name, expected, recipe.statPreview)
        }
        assertEquals((1..6).toSet(), ContentCatalog.FORGE_RECIPES.map { it.tier }.toSet())
    }

    @Test
    fun `potion table grows and stays unique`() {
        assertEquals(5, ItemCatalog.POTIONS.size)
        assertEquals(5, ItemCatalog.POTIONS.map { it.name }.toSet().size)
        assertTrue(ItemCatalog.POTIONS.all { it.herbCost > 0 && it.heal > 0.0 })
        assertTrue(ItemCatalog.potionHeal("Voidcap Draught") > ItemCatalog.potionHeal("Starbloom Elixir"))
    }

    @Test
    fun `minion table grows monotonically in power and price`() {
        assertEquals(7, MinionCatalog.TYPES.size)
        val dpss = MinionCatalog.TYPES.map { MinionCatalog.dps(it) }
        val costs = MinionCatalog.TYPES.map { MinionCatalog.cost(it) }
        assertEquals(dpss.sorted(), dpss)
        assertEquals(costs.sorted(), costs)
    }

    @Test
    fun `reconcile is idempotent`() {
        assertSame(reconciled, ContentCatalog.reconcile(reconciled))
        val twice = ContentCatalog.reconcile(ContentCatalog.reconcile(reconciled))
        assertEquals(reconciled, twice)
        assertEquals(9, twice.training.nodes.size)
        assertEquals(8, twice.talent.activeTalents.size)
        assertEquals(5, twice.talent.struggleTalents.size)
        assertEquals(5, twice.essence.upgrades.size)
        assertEquals(3, twice.necro.upgrades.size)
    }

    // ------------------------------------------------------------------ engine wiring

    @Test
    fun `an old save gains the new content on load`() {
        val engine = GameEngine(GameState())
        assertEquals(5, engine.state.harvest.plots.size)
        assertEquals(5, engine.state.harvest.veins.size)
        assertEquals(9, engine.state.training.nodes.size)
        assertEquals(8, engine.state.talent.activeTalents.size)
        assertEquals(5, engine.state.talent.struggleTalents.size)
        // A restored save reconciles through the same path, without duplicating anything.
        engine.restore(GameState())
        assertEquals(9, engine.state.training.nodes.size)
    }

    @Test
    fun `mining yield upgrade raises ore per extraction`() {
        val base = GameState().copy(
            harvest = GameState().harvest.copy(
                veins = listOf(
                    MiningVein(id = 0, oreName = ItemCatalog.COPPER_ORE, isReady = true, durationSeconds = 5f)
                )
            )
        )
        val plain = GameEngine(base).mineVein(0)
        assertEquals(12.0 + 3.0, plain.inventory.first { it.name == ItemCatalog.COPPER_ORE }.count, 1e-9)

        val seeded = GameEngine(base).state
        val upgraded = seeded.copy(
            harvest = seeded.harvest.copy(
                upgrades = seeded.harvest.upgrades.map {
                    if (it.id == "mining_yield") it.copy(rank = 1) else it
                }
            )
        )
        val rich = GameEngine(upgraded).mineVein(0)
        assertEquals(12.0 + 4.0, rich.inventory.first { it.name == ItemCatalog.COPPER_ORE }.count, 1e-9)
    }

    @Test
    fun `new potion can be brewed`() {
        val state = GameState().copy(
            inventory = GameState().inventory + InventoryItem(
                id = "test_voidcap",
                name = ItemCatalog.VOIDCAP,
                category = ItemCatalog.CATEGORY_RESOURCE,
                count = 20
            )
        )
        val after = GameEngine(state).brewPotion("Voidcap Draught", ItemCatalog.VOIDCAP, 12)
        assertNotNull(after.inventory.firstOrNull { it.name == "Voidcap Draught" })
        assertEquals(8.0, after.inventory.first { it.name == ItemCatalog.VOIDCAP }.count, 1e-9)
    }

    @Test
    fun `new minion type is summoned with catalogue stats`() {
        val state = GameState().copy(necro = GameState().necro.copy(boneEssence = 1_000L))
        val after = GameEngine(state).summonMinion("Void Herald")
        val minion = after.necro.army.firstOrNull { it.type == "Void Herald" }
        assertNotNull(minion)
        assertEquals(MinionCatalog.dps("Void Herald"), minion!!.dps, 1e-9)
        assertEquals(1_000L - MinionCatalog.cost("Void Herald"), after.necro.boneEssence)
    }

    // ------------------------------------------------------------------ derived stats

    @Test
    fun `new training nodes reach the derived stats`() {
        val base = DerivedStats.of(reconciled)
        fun trained(id: String, rank: Int) = DerivedStats.of(
            reconciled.copy(
                training = reconciled.training.copy(
                    nodes = reconciled.training.nodes.map { if (it.id == id) it.copy(rank = rank) else it }
                )
            )
        )

        assertEquals(base.critChance + 0.02, trained("crit_bonus", 4).critChance, 1e-9)
        assertEquals(base.maxShield * 1.10, trained("shield_bonus", 2).maxShield, 1e-6)
        assertEquals(base.xpGain + 0.04, trained("xp_bonus", 2).xpGain, 1e-9)
        assertEquals(base.healthRegen * 1.08, trained("regen_bonus", 2).healthRegen, 1e-6)
    }

    @Test
    fun `new essence and necromancy upgrades reach combat`() {
        val focus = reconciled.copy(
            essence = reconciled.essence.copy(
                upgrades = reconciled.essence.upgrades.map {
                    if (it.id == "smite_focus") it.copy(rank = 1) else it
                }
            )
        )
        assertEquals(
            DerivedStats.of(reconciled).smiteBonus * 1.05,
            DerivedStats.of(focus).smiteBonus,
            1e-9
        )

        val lattice = reconciled.copy(
            essence = reconciled.essence.copy(
                upgrades = reconciled.essence.upgrades.map {
                    if (it.id == "aegis_reserve") it.copy(rank = 1) else it
                }
            )
        )
        assertEquals(DerivedStats.of(reconciled).maxShield * 1.08, DerivedStats.of(lattice).maxShield, 1e-6)

        val momentum = reconciled.copy(
            necro = reconciled.necro.copy(
                upgrades = reconciled.necro.upgrades.map {
                    if (it.id == "necro_dps") it.copy(rank = 1) else it
                }
            )
        )
        assertEquals(
            CombatMath.armyDps(reconciled.necro) * 1.10,
            CombatMath.armyDps(momentum.necro),
            1e-9
        )
    }

    @Test
    fun `struggle shield talent adds shield from damage endured`() {
        val base = DerivedStats.of(reconciled)
        val endured = reconciled.copy(
            talent = reconciled.talent.copy(struggleDamageTaken = 1_000L)
        )
        assertEquals(
            base.maxShield + 5.0 * 2.0,
            DerivedStats.of(endured).maxShield,
            1e-6
        )
        assertEquals(10.0, DerivedStats.of(endured).struggleShieldBonus, 1e-9)
    }

    // ------------------------------------------------------------------ talents

    @Test
    fun `phoenix protocol heals to full and spends mana`() {
        val state = GameState().copy(player = GameState().player.copy(mana = 100.0, hp = 5.0))
        val after = GameEngine(state).activateTalent("phoenix_protocol")
        val derived = DerivedStats.of(after)
        assertEquals(derived.maxHp, after.player.hp, 1e-6)
        assertEquals(100.0 - 35.0, after.player.mana, 1e-9)
        assertTrue(after.talent.activeTalents.first { it.id == "phoenix_protocol" }.cooldownRemaining > 0.0)
    }

    @Test
    fun `void pulse damages the current monster`() {
        val state = GameState().copy(player = GameState().player.copy(mana = 100.0))
        val engine = GameEngine(state)
        val before = engine.state.monster.hp
        val after = engine.activateTalent("void_pulse")
        assertTrue(after.monster.hp < before)
        assertEquals(100.0 - 35.0, after.player.mana, 1e-9)
    }

    // ------------------------------------------------------------------ slayer

    @Test
    fun `slayer mastery scales bounty the engine pays`() {
        assertEquals(0, SlayerMath.bonusPoints(1))
        assertEquals(2, SlayerMath.bonusPoints(21))
        assertEquals(7, SlayerMath.scaledPoints(5, 21))
        assertEquals(0, SlayerMath.bonusGoldPercent(1))
        assertEquals(150L, SlayerMath.scaledGold(100L, 26))
        assertEquals(31, SlayerMath.nextBonusPointLevel(21))
        // The scaling must never shrink a contract.
        assertTrue(SlayerMath.scaledPoints(3, 1) >= 3)
        assertTrue(SlayerMath.scaledGold(100L, 1) >= 100L)
    }
}
