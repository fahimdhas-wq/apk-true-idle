package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem

/** Equipped-gear lookups. The inventory is the single source of truth for what is worn. */
object Equipment {
    const val SLOT_WEAPON = "Weapon"
    const val SLOT_ARMOR = "Armor"
    const val SLOT_RING = "Ring"
    const val SLOT_AMULET = "Amulet"

    val SLOTS = listOf(SLOT_WEAPON, SLOT_ARMOR, SLOT_RING, SLOT_AMULET)

    fun equipped(state: GameState, slot: String): InventoryItem? =
        state.inventory.firstOrNull { it.isEquipped && it.slot.equals(slot, ignoreCase = true) }

    fun equippedName(state: GameState, slot: String, fallback: String): String =
        equipped(state, slot)?.name ?: fallback

    /**
     * Raw (unmultiplied) attack, falling back to the tier-scaled value. The parameter is nullable so
     * comparison cards can show "nothing equipped" as zero; a separate non-null overload would
     * clash on the JVM signature after erasure.
     */
    fun baseAttack(item: InventoryItem?): Double =
        item?.let { if (it.atk > 0.0) it.atk else it.tier * 12.0 } ?: 0.0

    fun baseDefense(item: InventoryItem?): Double =
        item?.let { if (it.def > 0.0) it.def else it.tier * 8.0 } ?: 0.0

    fun isEquipment(item: InventoryItem): Boolean = item.category == "Equipment"
}
