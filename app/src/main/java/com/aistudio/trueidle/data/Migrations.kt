package com.aistudio.trueidle.data

import com.aistudio.trueidle.model.GameState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object Migrations {
    fun apply(root: JsonObject, schema: Int, json: Json): GameState {
        var state = json.decodeFromJsonElement<GameState>(root)
        if (schema < 1) {
            val updatedInventory = state.inventory.map { item ->
                var atk = item.atk
                var def = item.def
                if (item.category == "Equipment") {
                    val desc = item.statDesc
                    val num = Regex("""\+(\d+)""").find(desc)?.groupValues?.get(1)?.toDoubleOrNull()
                    if (item.slot == "Weapon") {
                        atk = num ?: (item.tier * 12.0)
                    } else if (item.slot == "Armor") {
                        def = num ?: (item.tier * 8.0)
                    }
                }
                item.copy(atk = atk, def = def)
            }

            var maxId = 100L
            state.logs.forEach { maxId = maxOf(maxId, it.id) }
            state.notifications.forEach { maxId = maxOf(maxId, it.id) }
            state.notificationHistory.forEach { maxId = maxOf(maxId, it.id) }

            val meta = state.meta.copy(
                savedWallMs = if (state.meta.savedWallMs > 0) state.meta.savedWallMs else state.lastSaveTimestamp,
                nextId = maxOf(state.meta.nextId, maxId + 1L)
            )

            state = state.copy(
                inventory = updatedInventory,
                meta = meta
            )
        }
        return state
    }
}
