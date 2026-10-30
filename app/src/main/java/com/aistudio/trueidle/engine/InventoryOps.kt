package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.model.InventoryItem

/** Inventory mutation shared by the engine and its systems. */
internal object InventoryOps {

    fun add(
        inventory: MutableList<InventoryItem>,
        name: String,
        qty: Double,
        nextId: () -> Long
    ) {
        val idx = inventory.indexOfFirst {
            it.name == name && it.category != ItemCatalog.CATEGORY_EQUIPMENT
        }
        if (idx >= 0) {
            inventory[idx] = inventory[idx].copy(count = inventory[idx].count + qty)
        } else {
            inventory.add(ItemCatalog.newResource("item_${nextId()}", name, qty))
        }
    }
}
