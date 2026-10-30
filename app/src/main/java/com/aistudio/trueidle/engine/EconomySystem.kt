package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry

/**
 * Inventory and loadout actions: selling, equipping, drinking potions, auto-deploy settings and
 * auto-equipping the best gear. Each action takes the current state and returns the next one.
 */
internal class EconomySystem(
    private val logger: (String, String) -> LogEntry,
    private val deployPotion: (Frame, String) -> Boolean
) {

    private fun log(type: String, text: String): LogEntry = logger(type, text)

    fun sellItem(current: GameState, itemId: String, count: Int = 1): GameState {
            var state = current
            val inventory = state.inventory.toMutableList()
            val idx = inventory.indexOfFirst { it.id == itemId }
            if (idx < 0) return state
            val item = inventory[idx]
            // The engine refuses to sell worn gear; the UI hiding the button was not enough.
            if (item.isEquipped) {
                state = state.copy(
                    logs = (state.logs + log(LogType.SYSTEM, "SALE REFUSED: ${item.name} is equipped."))
                        .takeLast(Balance.LOG_CAP)
                )
                return state
            }
            val actual = count.toDouble().coerceIn(1.0, item.count)
            val gold = (actual * item.sellValue).toLong()
            if (item.count > actual) inventory[idx] = item.copy(count = item.count - actual)
            else inventory.removeAt(idx)

            state = state.copy(
                player = state.player.copy(gold = state.player.gold + gold),
                inventory = inventory,
                logs = (state.logs + log(LogType.GAIN, "ITEM SOLD: ${NumberFormatter.short(actual)} x ${item.name} for +$gold CREDITS."))
                    .takeLast(Balance.LOG_CAP)
            )
            return state
        }

    fun equipItem(current: GameState, itemId: String): GameState {
            var state = current
            val inventory = state.inventory.toMutableList()
            val idx = inventory.indexOfFirst { it.id == itemId }
            if (idx < 0) return state
            val item = inventory[idx]
            if (item.category == ItemCatalog.CATEGORY_POTION) return usePotion(state, itemId)
            if (!Equipment.isEquipment(item) || item.slot.isBlank()) return state

            // Rings and amulets are real slots too; they used to have no equip branch at all.
            for (i in inventory.indices) {
                if (inventory[i].slot.equals(item.slot, ignoreCase = true) && inventory[i].isEquipped) {
                    inventory[i] = inventory[i].copy(isEquipped = false)
                }
            }
            inventory[idx] = item.copy(isEquipped = true)
            state = state.copy(
                inventory = inventory,
                logs = (state.logs + log(LogType.SYSTEM, "GEAR EQUIPPED: ${item.name.uppercase()} -> ${item.slot}."))
                    .takeLast(Balance.LOG_CAP)
            )
            return state
        }

    fun unequipItem(current: GameState, itemId: String): GameState {
            var state = current
            val inventory = state.inventory.toMutableList()
            val idx = inventory.indexOfFirst { it.id == itemId }
            if (idx < 0 || !inventory[idx].isEquipped) return state
            val item = inventory[idx]
            inventory[idx] = item.copy(isEquipped = false)
            state = state.copy(
                inventory = inventory,
                logs = (state.logs + log(LogType.SYSTEM, "GEAR UNEQUIPPED: ${item.name.uppercase()}."))
                    .takeLast(Balance.LOG_CAP)
            )
            return state
        }

    fun usePotion(current: GameState, itemId: String): GameState {
            var state = current
            val item = state.inventory.firstOrNull { it.id == itemId } ?: return state
            if (item.category != ItemCatalog.CATEGORY_POTION) return state
            return usePotionByName(state, item.name)
        }

    fun usePotionByName(current: GameState, name: String): GameState {
            var state = current
            // A downed hero cannot drink.
            if (state.player.recoverLeft > 0.0) return state
            val def = ItemCatalog.potion(name) ?: return state
            val derived = DerivedStats.of(state)
            val atFull = state.player.hp >= derived.maxHp && state.player.mana >= derived.maxMana
            if (atFull) {
                state = state.copy(
                    logs = (state.logs + log(LogType.SYSTEM, "POTION HELD: already at full health."))
                        .takeLast(Balance.LOG_CAP)
                )
                return state
            }
            val frame = Frame(state)
            if (!deployPotion(frame, def.name)) return state
            val next = frame.view()
            state = next.copy(
                logs = (state.logs + log(
                    LogType.GAIN,
                    "POTION DEPLOYED: ${def.name} — ${def.effectDesc}"
                )).takeLast(Balance.LOG_CAP)
            )
            return state
        }

    fun setHpAutoThreshold(current: GameState, pct: Int): GameState {
            var state = current
            state = state.copy(
                potionSettings = state.potionSettings.copy(hpAutoThresholdPct = pct.coerceIn(0, 100))
            )
            return state
        }

    fun setMpAutoThreshold(current: GameState, pct: Int): GameState {
            var state = current
            state = state.copy(
                potionSettings = state.potionSettings.copy(mpAutoThresholdPct = pct.coerceIn(0, 100))
            )
            return state
        }

    fun selectHpPotion(current: GameState, name: String): GameState {
            var state = current
            if (ItemCatalog.potion(name) == null) return state
            state = state.copy(potionSettings = state.potionSettings.copy(selectedHpPotion = name))
            return state
        }

    fun selectMpPotion(current: GameState, name: String): GameState {
            var state = current
            if (ItemCatalog.potion(name) == null) return state
            state = state.copy(potionSettings = state.potionSettings.copy(selectedMpPotion = name))
            return state
        }

    fun autoEquipBestGear(current: GameState): GameState {
            var state = current
            val inventory = state.inventory.toMutableList()
            var changed = false
            for (slot in Equipment.SLOTS) {
                val candidates = inventory.filter {
                    Equipment.isEquipment(it) && it.slot.equals(slot, ignoreCase = true)
                }
                if (candidates.isEmpty()) continue
                // Uses the same tier fallback as DerivedStats instead of the raw atk field.
                val best = candidates.maxByOrNull {
                    if (Equipment.isWeapon(slot)) {
                        Equipment.baseAttack(it)
                    } else {
                        Equipment.baseDefense(it) + Equipment.baseVit(it)
                    }
                } ?: continue
                if (best.isEquipped) continue
                for (i in inventory.indices) {
                    if (inventory[i].slot.equals(slot, ignoreCase = true)) {
                        inventory[i] = inventory[i].copy(isEquipped = inventory[i].id == best.id)
                    }
                }
                changed = true
            }
            if (changed) {
                state = state.copy(
                    inventory = inventory,
                    logs = (state.logs + log(LogType.SYSTEM, "AUTO-EQUIP: best available gear equipped."))
                        .takeLast(Balance.LOG_CAP)
                )
            }
            return state
        }
}
