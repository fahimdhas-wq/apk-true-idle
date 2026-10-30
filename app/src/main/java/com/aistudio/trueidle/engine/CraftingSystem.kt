package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.LogEntry
import kotlin.random.Random

/** Forge and alchemy: consumes materials, grants skill xp and appends the resulting items. */
internal class CraftingSystem(
    private val random: Random,
    private val nextId: () -> Long,
    private val logger: (String, String) -> LogEntry,
    private val grantSkillXp: (Long, Int, Long, String, String) -> Triple<Long, Int, List<LogEntry>>,
    private val autoEquip: (GameState) -> GameState
) {

    private fun log(type: String, text: String): LogEntry = logger(type, text)

    fun craftEquipment(current: GameState, name: String, tier: Int, slot: String, reqOre: String, reqQty: Int): ActionOutcome {
        var state = current
        val inventory = state.inventory.toMutableList()
        val oreIdx = inventory.indexOfFirst { it.name == reqOre }
        val owned = inventory.getOrNull(oreIdx)?.count ?: 0.0
        if (oreIdx < 0 || owned < reqQty) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "FORGE FAILED: requires ${reqQty}x $reqOre (have ${NumberFormatter.short(owned)})."
                )).takeLast(Balance.LOG_CAP)
            )
            return ActionOutcome(state, false)
        }

        // "Max % of materials auto-crafters may use": the forge keeps a reserve instead of
        // consuming the last of a stack. At 0% no reserve is enforced.
        val usagePct = state.automation.craftMaterialUsagePct
        val remainingAfter = owned - reqQty
        val reserve = owned * (100 - usagePct) / 100.0
        if (usagePct > 0 && remainingAfter < reserve) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "FORGE REFUSED: $usagePct% material-usage cap would be exceeded for $reqOre."
                )).takeLast(Balance.LOG_CAP)
            )
            return ActionOutcome(state, false)
        }

        val recycleRank = state.armory.upgrades.firstOrNull { it.id == "resource_saving" }?.rank ?: 0
        val conserved = recycleRank > 0 && random.nextDouble() < (0.12 * recycleRank).coerceAtMost(0.60)
        if (!conserved) {
            val ore = inventory[oreIdx]
            if (ore.count > reqQty) inventory[oreIdx] = ore.copy(count = ore.count - reqQty)
            else inventory.removeAt(oreIdx)
        }

        // forge_quality is applied once, in DerivedStats. Baking it into the item as well used to
        // double it, and left statDesc stale after every upgrade.
        val isWep = Equipment.isWeapon(slot)
        val atk = if (isWep) tier * 12.0 else 0.0
        val def = if (!isWep) tier * 8.0 else 0.0
        val vit = if (!isWep) tier * 4.0 else 0.0
        val statDesc = if (isWep) "+${atk.toInt()} ATK" else "+${def.toInt()} DEF, +${vit.toInt()} VIT"
        inventory.add(
            InventoryItem(
                id = "craft_${nextId()}",
                name = name,
                category = ItemCatalog.CATEGORY_EQUIPMENT,
                count = 1,
                tier = tier,
                slot = slot,
                statDesc = statDesc,
                sellValue = tier * 35L,
                atk = atk,
                def = def,
                vit = vit
            )
        )

        val (xp, lvl, logs) = grantSkillXp(
            state.armory.smithingXp,
            state.armory.smithingLvl,
            50L,
            "Armory Smithing",
            "CYBER FORGE SUCCESS: Forged $name (Tier $tier)."
        )
        state = state.copy(
            inventory = inventory,
            armory = state.armory.copy(smithingXp = xp, smithingLvl = lvl),
            lifetime = state.lifetime.copy(forged = state.lifetime.forged + 1),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        if (state.automation.autoEquipBestGear) state = autoEquip(state)
        return ActionOutcome(state, true)
    }

    fun brewPotion(current: GameState, name: String, reqHerb: String, reqQty: Int): ActionOutcome {
        var state = current
        // The catalog is authoritative for a known potion; the caller only supplies the fallback.
        val recipe = ItemCatalog.potion(name)
        val herbName = if (recipe != null) {
            if (recipe.herb == ItemCatalog.HEALING_HERB && state.inventory.none { it.name == ItemCatalog.HEALING_HERB } && state.inventory.any { it.name == ItemCatalog.FELWEED }) {
                ItemCatalog.FELWEED
            } else recipe.herb
        } else reqHerb
        val herbCost = recipe?.herbCost ?: reqQty
        val inventory = state.inventory.toMutableList()
        val herbIdx = inventory.indexOfFirst { it.name == herbName }
        val owned = inventory.getOrNull(herbIdx)?.count ?: 0.0
        if (herbIdx < 0 || owned < herbCost) {
            state = state.copy(
                logs = (state.logs + log(
                    LogType.SYSTEM,
                    "ALCHEMY FAILED: requires ${herbCost}x $herbName (have ${NumberFormatter.short(owned)})."
                )).takeLast(Balance.LOG_CAP)
            )
            return ActionOutcome(state, false)
        }
        val herb = inventory[herbIdx]
        if (herb.count > herbCost) inventory[herbIdx] = herb.copy(count = herb.count - herbCost)
        else inventory.removeAt(herbIdx)

        // Brewed potions stack instead of piling up as separate one-count entries.
        val heal = ItemCatalog.potionHeal(name)
        val existing = inventory.indexOfFirst { it.name == name && it.category == ItemCatalog.CATEGORY_POTION }
        if (existing >= 0) {
            inventory[existing] = inventory[existing].copy(count = inventory[existing].count + 1)
        } else {
            inventory.add(
                InventoryItem(
                    id = "potion_${nextId()}",
                    name = name,
                    category = ItemCatalog.CATEGORY_POTION,
                    count = 1,
                    tier = 1,
                    statDesc = "Restores ${heal.toInt()} HP",
                    sellValue = 15L
                )
            )
        }

        val (xp, lvl, logs) = grantSkillXp(
            state.armory.herbloreXp,
            state.armory.herbloreLvl,
            40L,
            "Herblore",
            "ALCHEMY SYNTHESIS: Brewed $name."
        )
        state = state.copy(
            inventory = inventory,
            armory = state.armory.copy(herbloreXp = xp, herbloreLvl = lvl),
            lifetime = state.lifetime.copy(brewed = state.lifetime.brewed + 1),
            logs = (state.logs + logs).takeLast(Balance.LOG_CAP)
        )
        return ActionOutcome(state, true)
    }
}
