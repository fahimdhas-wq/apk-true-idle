package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.Derived
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.InventoryItem
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.model.Monster
import kotlin.random.Random

/** Combat drops, taming, boss trophies and beast auto-merging. */
internal class LootSystem(
    private val random: Random,
    private val nextId: () -> Long,
    private val logger: (String, String) -> LogEntry
) {

    fun rollLoot(frame: Frame, d: Derived) {
        if (random.nextDouble() >= d.lootChance) return
        if (random.nextDouble() < 0.35) {
            val group = Equipment.GROUPS.random(random)
            val floor = frame.player.currentFloor
            val tier = ((floor - 1) / 5 + 1).coerceIn(1, TierConfig.size)
            val tierInfo = TierConfig.forOrdinal(tier)
            val isWep = Equipment.isWeapon(group)
            val atk = if (isWep) tier * 12.0 else 0.0
            val def = if (!isWep) tier * 8.0 else 0.0
            val vit = if (!isWep) tier * 4.0 else 0.0
            val name = "${tierInfo.id} $group"
            val statDesc = if (isWep) "+${atk.toInt()} ATK" else "+${def.toInt()} DEF, +${vit.toInt()} VIT"
            frame.inventory.add(
                InventoryItem(
                    id = "loot_${nextId()}",
                    name = name,
                    category = ItemCatalog.CATEGORY_EQUIPMENT,
                    count = 1,
                    tier = tier,
                    slot = group,
                    statDesc = statDesc,
                    sellValue = tier * 35L,
                    atk = atk,
                    def = def,
                    vit = vit
                )
            )
            frame.logs += logger(LogType.LOOT, "TACTICAL DROP: Secured $name ($statDesc)")
        } else {
            val resource = if (random.nextBoolean()) {
                val oreIdx = (frame.player.currentFloor / 6).coerceIn(0, ItemCatalog.ORES.size - 1)
                ItemCatalog.ORES[oreIdx]
            } else {
                val herbIdx = (frame.player.currentFloor / 6).coerceIn(0, ItemCatalog.HERBS.size - 1)
                ItemCatalog.HERBS[herbIdx]
            }
            InventoryOps.add(frame.inventory, resource, 1.0, nextId)
            frame.logs += logger(LogType.LOOT, "SALVAGE ACQUIRED: $resource")
        }
    }

    fun grantTrophy(frame: Frame) {
        InventoryOps.add(frame.inventory, ItemCatalog.BOSS_TROPHY, 1.0, nextId)
        frame.logs += logger(LogType.LOOT, "TROPHY CLAIMED: ${ItemCatalog.BOSS_TROPHY}")
    }

    fun rollTame(frame: Frame, monster: Monster, d: Derived) {
        if (frame.beasts.beasts.size >= Balance.BEAST_CAP) return
        if (random.nextDouble() >= d.tameChance) return
        val beast = tamedBeast(monster, "Tamed Hunter")
        frame.beasts = frame.beasts.copy(beasts = frame.beasts.beasts + beast)
        frame.lifetime = frame.lifetime.copy(beastsTamed = frame.lifetime.beastsTamed + 1)
        frame.logs += logger(LogType.LOOT, "BEAST TAMED: Secured ${beast.name} into the squad!")
    }

    /**
     * Tamed beasts keep the plain species name. Embedding "Companion"/"Scout" in the name used to
     * make beasts from the live and offline paths impossible to merge together.
     */
    fun tamedBeast(monster: Monster, trait: String) = Beast(
        id = "beast_${nextId()}",
        name = monster.name,
        rank = 1,
        stars = 1,
        dps = (monster.damage * 0.6).coerceAtLeast(4.0),
        hp = monster.maxHp * 0.5,
        maxHp = monster.maxHp * 0.5,
        trait = trait
    )

    fun autoMergeBeasts(frame: Frame) {
        var beasts = frame.beasts.beasts
        var merged = true
        while (merged) {
            merged = false
            val pair = beasts.firstOrNull { a ->
                beasts.any { b -> b.id != a.id && b.name == a.name && b.stars == a.stars }
            } ?: break
            val partner = beasts.first { it.id != pair.id && it.name == pair.name && it.stars == pair.stars }
            beasts = beasts - pair - partner
            beasts = beasts + pair.copy(
                stars = pair.stars + 1,
                dps = pair.dps * 1.6,
                hp = pair.maxHp * 1.5,
                maxHp = pair.maxHp * 1.5
            )
            frame.logs += logger(
                LogType.MILESTONE,
                "AUTO-MERGE: ${pair.name} upgraded to ${pair.stars + 1} stars."
            )
            merged = true
        }
        frame.beasts = frame.beasts.copy(beasts = beasts)
    }
}
