package com.aistudio.trueidle.core

import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem

/**
 * Equipped-gear lookups and definitions.
 *
 * Equipment system comprises 13 distinct equipment groups:
 * - Weapons (using STR stat in combat): Rifle, Sidearm, Blade
 * - Armor slots (providing DEF and VIT): Visor, Vest, Grips, Pants, Boots, Duster, Rig
 * - Node/Ring slots (providing DEF and VIT): Node I, Node II, Node III
 */
object Equipment {
    // Weapons
    const val SLOT_RIFLE = "Rifle"
    const val SLOT_SIDEARM = "Sidearm"
    const val SLOT_BLADE = "Blade"

    // Armor
    const val SLOT_VISOR = "Visor"
    const val SLOT_VEST = "Vest"
    const val SLOT_GRIPS = "Grips"
    const val SLOT_PANTS = "Pants"
    const val SLOT_BOOTS = "Boots"
    const val SLOT_DUSTER = "Duster"
    const val SLOT_RIG = "Rig"

    // Nodes (Ring slots)
    const val SLOT_NODE_I = "Node I"
    const val SLOT_NODE_II = "Node II"
    const val SLOT_NODE_III = "Node III"

    // Legacy slot aliases for backwards compatibility
    const val SLOT_WEAPON = "Weapon"
    const val SLOT_ARMOR = "Armor"
    const val SLOT_RING = "Ring"
    const val SLOT_AMULET = "Amulet"

    val WEAPON_SLOTS = listOf(SLOT_RIFLE, SLOT_SIDEARM, SLOT_BLADE)
    val ARMOR_SLOTS = listOf(SLOT_VISOR, SLOT_VEST, SLOT_GRIPS, SLOT_PANTS, SLOT_BOOTS, SLOT_DUSTER, SLOT_RIG)
    val NODE_SLOTS = listOf(SLOT_NODE_I, SLOT_NODE_II, SLOT_NODE_III)

    /** All 13 canonical equipment groups */
    val GROUPS = listOf(
        SLOT_NODE_I, SLOT_NODE_II, SLOT_NODE_III,
        SLOT_RIFLE, SLOT_SIDEARM, SLOT_BLADE,
        SLOT_VISOR, SLOT_VEST, SLOT_GRIPS, SLOT_PANTS, SLOT_BOOTS, SLOT_DUSTER, SLOT_RIG
    )

    val SLOTS = WEAPON_SLOTS + ARMOR_SLOTS + NODE_SLOTS

    /**
     * Legacy item names from builds before the canonical 13-group rename. Saves, starting loadouts
     * and drop tables used these strings, so [com.aistudio.trueidle.engine.GameEngine.sanitize]
     * migrates them onto the canonical group instead of leaving dead duplicates in the inventory.
     */
    val LEGACY_NAMES: Map<String, String> = mapOf(
        "Iron Greatsword" to SLOT_BLADE,
        "Steel Plate" to SLOT_VEST,
        "Rusty Blade" to SLOT_BLADE,
        "Tattered Leather" to SLOT_DUSTER,
        "Ember Band" to SLOT_NODE_I,
        "Warden Sigil" to SLOT_NODE_II
    )

    /** Canonical group for a legacy item name or old slot alias; otherwise [slot] unchanged. */
    fun canonicalSlot(slot: String, name: String = ""): String {
        LEGACY_NAMES[name]?.let { return it }
        return when (slot) {
            SLOT_WEAPON -> SLOT_RIFLE
            SLOT_ARMOR -> SLOT_VEST
            SLOT_RING -> SLOT_NODE_I
            SLOT_AMULET -> SLOT_NODE_II
            else -> slot
        }
    }

    fun equipped(state: GameState, slot: String): InventoryItem? =
        state.inventory.firstOrNull { it.isEquipped && it.slot.equals(slot, ignoreCase = true) }

    fun equippedName(state: GameState, slot: String, fallback: String): String =
        equipped(state, slot)?.name ?: fallback

    fun isWeapon(slot: String): Boolean =
        WEAPON_SLOTS.any { it.equals(slot, ignoreCase = true) } || slot.equals(SLOT_WEAPON, ignoreCase = true)

    fun isArmorOrNode(slot: String): Boolean =
        ARMOR_SLOTS.any { it.equals(slot, ignoreCase = true) } ||
        NODE_SLOTS.any { it.equals(slot, ignoreCase = true) } ||
        slot.equals(SLOT_ARMOR, ignoreCase = true)

    fun baseAttack(item: InventoryItem?): Double =
        item?.let {
            if (it.atk > 0.0) it.atk
            else if (isWeapon(it.slot)) it.tier * 12.0
            else 0.0
        } ?: 0.0

    fun baseDefense(item: InventoryItem?): Double =
        item?.let {
            if (it.def > 0.0) it.def
            else if (isArmorOrNode(it.slot)) it.tier * 8.0
            else 0.0
        } ?: 0.0

    fun baseVit(item: InventoryItem?): Double =
        item?.vit ?: 0.0

    fun isEquipment(item: InventoryItem): Boolean =
        item.category.equals("Equipment", ignoreCase = true) ||
        GROUPS.any { it.equals(item.slot, ignoreCase = true) } ||
        listOf(SLOT_WEAPON, SLOT_ARMOR, SLOT_RING, SLOT_AMULET).any { it.equals(item.slot, ignoreCase = true) }

    fun slotCategory(slot: String): String = when {
        WEAPON_SLOTS.any { it.equals(slot, ignoreCase = true) } -> "WEAPON"
        NODE_SLOTS.any { it.equals(slot, ignoreCase = true) } -> "NODE"
        else -> "ARMOR"
    }
}
