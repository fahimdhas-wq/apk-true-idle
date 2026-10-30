package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.model.ActiveBuff
import com.aistudio.trueidle.model.BeastTamingState
import com.aistudio.trueidle.model.EssenceState
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.Monster
import com.aistudio.trueidle.model.NecromancyState
import com.aistudio.trueidle.model.PlayerState
import com.aistudio.trueidle.model.PotionSettings
import com.aistudio.trueidle.model.TalentState

/**
 * Working copy of one simulation frame. Systems mutate this and [commit] writes it back once.
 */
internal class Frame(val base: GameState) {
    var player: PlayerState = base.player
    var monster: Monster = base.monster
    var necro: NecromancyState = base.necro
    var beasts: BeastTamingState = base.beasts
    var harvest = base.harvest
    var armory = base.armory
    var essence: EssenceState = base.essence
    var slayer = base.slayer
    var talent: TalentState = base.talent
    var lifetime = base.lifetime
    var inventory: MutableList<InventoryItem> = base.inventory.toMutableList()
    var activeBuffs: MutableList<ActiveBuff> = base.activeBuffs.toMutableList()
    var potionSettings: com.aistudio.trueidle.model.PotionSettings = base.potionSettings
    val logs: MutableList<LogEntry> = mutableListOf()

    private var cachedDerived: Derived? = null
    fun derived(): Derived = cachedDerived ?: DerivedStats.of(view()).also { cachedDerived = it }
    fun invalidateDerived() { cachedDerived = null }

    fun view(): GameState = base.copy(
        player = player,
        monster = monster,
        necro = necro,
        beasts = beasts,
        harvest = harvest,
        armory = armory,
        essence = essence,
        slayer = slayer,
        talent = talent,
        lifetime = lifetime,
        inventory = inventory,
        potionSettings = potionSettings,
        activeBuffs = activeBuffs
    )

    fun commit(): GameState {
        return view().copy(logs = (base.logs + logs).takeLast(Balance.LOG_CAP))
    }
}
