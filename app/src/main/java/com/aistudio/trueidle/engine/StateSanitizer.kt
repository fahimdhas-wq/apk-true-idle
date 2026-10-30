package com.aistudio.trueidle.engine

import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.MiningVein
import com.aistudio.trueidle.model.Plot
import com.aistudio.trueidle.model.Talent

/** Save-load migration and clamping. Kept out of GameEngine so the facade only delegates. */
internal object StateSanitizer {

    /** Dev speed bounds shared by the tick and the panel. */
    const val DEV_MIN_SPEED = 1
    const val DEV_MAX_SPEED = 50

    /** The panel's MAX grant, large enough to exercise formatting without overflowing. */
    const val DEV_MAX_CURRENCY = 999_999_999L

    /** Skill level the unlock-everything cheat grants. */
    const val DEV_MAX_SKILL_LEVEL = 100

    /** Thresholds the potion auto-deploy control accepts (0 = off). */
    val AUTO_POTION_THRESHOLDS = listOf(0, 30, 50, 75)

    private val ID_SUFFIX = Regex("""[_-](\\d+)$""")

    private fun clampTalents(list: List<Talent>) = list.map {
                it.copy(
                    rank = it.rank.coerceIn(0, it.maxRank),
                    cooldownRemaining = it.cooldownRemaining.coerceAtLeast(0.0),
                    durationRemaining = it.durationRemaining.coerceAtLeast(0.0)
                )
            }

    fun nextIdFrom(source: GameState): Long {
                var max = Balance.RESERVED_ID_FLOOR - 1L
                source.logs.forEach { max = maxOf(max, it.id) }
                fun scan(ids: List<String>) {
                    ids.forEach { id ->
                        ID_SUFFIX.find(id)?.groupValues?.get(1)?.toLongOrNull()?.let { max = maxOf(max, it) }
                    }
                }
                scan(source.inventory.map { it.id })
                scan(source.beasts.beasts.map { it.id })
                scan(source.necro.army.map { it.id })
                return maxOf(max + 1L, source.meta.nextId, Balance.RESERVED_ID_FLOOR)
            }

    fun sanitize(input: GameState): GameState {
                val maxFloor = input.player.maxUnlockedFloor.coerceIn(1, Balance.MAX_FLOOR)
                val currentFloor = input.player.currentFloor.coerceIn(1, maxFloor)
                val required = Balance.killsRequiredFor(currentFloor)

                val player = input.player.copy(
                    lvl = input.player.lvl.coerceAtLeast(1),
                    xp = input.player.xp.coerceAtLeast(0L),
                    gold = input.player.gold.coerceAtLeast(0L),
                    diamonds = input.player.diamonds.coerceAtLeast(0L),
                    hp = input.player.hp.coerceAtLeast(0.0),
                    shield = input.player.shield.coerceAtLeast(0.0),
                    mana = input.player.mana.coerceAtLeast(0.0),
                    currentFloor = currentFloor,
                    maxUnlockedFloor = maxFloor,
                    killsOnFloor = input.player.killsOnFloor.coerceIn(0, required),
                    killsRequired = required,
                    recoverLeft = input.player.recoverLeft.coerceIn(0.0, Balance.RECOVERY_SECONDS),
                    failuresInRow = input.player.failuresInRow.coerceAtLeast(0)
                )

                val army = mutableListOf<com.aistudio.trueidle.model.Minion>()
                var remaining = Balance.MINION_CAP
                for (minion in input.necro.army) {
                    if (remaining <= 0) break
                    val count = minion.count.coerceIn(1, remaining)
                    remaining -= count
                    army += minion.copy(count = count, lvl = minion.lvl.coerceAtLeast(1))
                }

                fun clampUpgrades(list: List<DisciplineUpgrade>) =
                    list.map { it.copy(rank = it.rank.coerceIn(0, it.maxRank)) }

                val sanitizedInventory = run {
                    val seenEquippedSlots = mutableSetOf<String>()
                    input.inventory.map { item ->
                        // Legacy names ("Iron Greatsword", "Steel Plate", ...) migrate to their group.
                        val legacyGroup = Equipment.LEGACY_NAMES[item.name]
                        val migratedSlot = Equipment.canonicalSlot(legacyGroup ?: item.slot, item.name)
                        val isEquip = Equipment.isEquipment(item) || legacyGroup != null ||
                            Equipment.GROUPS.any { it.equals(migratedSlot, ignoreCase = true) }
                        if (isEquip) {
                            val isWep = Equipment.isWeapon(migratedSlot)
                            val atk = if (isWep) (if (item.atk > 0.0) item.atk else item.tier * 18.0) else 0.0
                            val def = if (!isWep) (if (item.def > 0.0) item.def else item.tier * 8.0) else 0.0
                            val vit = if (!isWep) (if (item.vit > 0.0) item.vit else item.tier * 4.0) else 0.0
                            val statDesc = if (isWep) "+${atk.toInt()} ATK" else "+${def.toInt()} DEF, +${vit.toInt()} VIT"
                            val equipped = item.isEquipped && seenEquippedSlots.add(migratedSlot.lowercase())
                            item.copy(
                                name = if (legacyGroup != null) {
                                    "${TierConfig.forOrdinal(item.tier).id} $migratedSlot"
                                } else item.name,
                                slot = migratedSlot,
                                atk = atk,
                                def = def,
                                vit = vit,
                                statDesc = statDesc,
                                isEquipped = equipped
                            )
                        } else {
                            item
                        }
                    }
                }

                return input.copy(
                    player = player,
                    monster = if (input.monster.hp <= 0.0) Balance.generateMonsterForFloor(currentFloor) else input.monster,
                    inventory = sanitizedInventory,
                    potionSettings = input.potionSettings.copy(
                        hpAutoThresholdPct = input.potionSettings.hpAutoThresholdPct
                            .takeIf { it in AUTO_POTION_THRESHOLDS } ?: 0,
                        mpAutoThresholdPct = input.potionSettings.mpAutoThresholdPct
                            .takeIf { it in AUTO_POTION_THRESHOLDS } ?: 0,
                        selectedHpPotion = input.potionSettings.selectedHpPotion
                            .takeIf { ItemCatalog.potion(it) != null } ?: "Minor Health Flask",
                        selectedMpPotion = input.potionSettings.selectedMpPotion
                            .takeIf { ItemCatalog.potion(it) != null } ?: "Mana Infusion Tonic"
                    ),
                    activeBuffs = input.activeBuffs.filter { it.durationRemaining > 0.0 },
                    dev = input.dev.copy(
                        gameSpeed = input.dev.gameSpeed.coerceIn(DEV_MIN_SPEED, DEV_MAX_SPEED)
                    ),
                    necro = input.necro.copy(
                        lvl = input.necro.lvl.coerceAtLeast(1),
                        xp = input.necro.xp.coerceAtLeast(0L),
                        boneEssence = input.necro.boneEssence.coerceAtLeast(0L),
                        army = army,
                        upgrades = clampUpgrades(input.necro.upgrades)
                    ),
                    harvest = input.harvest.copy(
                        farmingLvl = input.harvest.farmingLvl.coerceAtLeast(1),
                        farmingXp = input.harvest.farmingXp.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
                        miningLvl = input.harvest.miningLvl.coerceAtLeast(1),
                        miningXp = input.harvest.miningXp.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0,
                        plots = run {
                            val basePlots = if (input.harvest.plots.size == ItemCatalog.HERBS.size &&
                                input.harvest.plots.map { it.herbName } == ItemCatalog.HERBS) {
                                input.harvest.plots
                            } else {
                                ItemCatalog.HERBS.mapIndexed { idx, herb ->
                                    val existing = input.harvest.plots.find { it.herbName.equals(herb, ignoreCase = true) }
                                    Plot(
                                        id = idx,
                                        herbName = herb,
                                        reqLvl = if (idx == 0) 1 else idx * 5,
                                        growthProgress = existing?.growthProgress ?: 0f,
                                        durationSeconds = 10f + idx * 3f,
                                        isReady = existing?.isReady ?: false,
                                        proficiencyLevel = existing?.proficiencyLevel ?: 1,
                                        proficiencyXp = existing?.proficiencyXp ?: 0L,
                                        autoQueue = existing?.autoQueue ?: false
                                    )
                                }
                            }
                            var enabledQueues = 0
                            val queueCapacity = input.harvest.farmingLvl.coerceAtLeast(1) / 10
                            basePlots.mapIndexed { idx, p ->
                                val requiredLevel = if (idx == 0) 1 else idx * 5
                                val queueAllowed = p.autoQueue && input.harvest.farmingLvl >= requiredLevel && enabledQueues < queueCapacity
                                if (queueAllowed) enabledQueues++
                                p.copy(
                                    reqLvl = requiredLevel,
                                    growthProgress = p.growthProgress.coerceIn(0f, 1f),
                                    isReady = p.isReady && input.harvest.farmingLvl >= requiredLevel,
                                    autoQueue = queueAllowed,
                                    proficiencyLevel = p.proficiencyLevel.coerceIn(1, 100),
                                    proficiencyXp = p.proficiencyXp.coerceAtLeast(0L)
                                )
                            }
                        },
                        veins = run {
                            val baseVeins = if (input.harvest.veins.size == ItemCatalog.ORES.size &&
                                input.harvest.veins.map { it.oreName } == ItemCatalog.ORES) {
                                input.harvest.veins
                            } else {
                                ItemCatalog.ORES.mapIndexed { idx, ore ->
                                    val existing = input.harvest.veins.find { it.oreName.equals(ore, ignoreCase = true) }
                                    MiningVein(
                                        id = idx,
                                        oreName = ore,
                                        reqLvl = if (idx == 0) 1 else idx * 5,
                                        miningProgress = existing?.miningProgress ?: 0f,
                                        durationSeconds = 8f + idx * 2.5f,
                                        isReady = existing?.isReady ?: false,
                                        proficiencyLevel = existing?.proficiencyLevel ?: 1,
                                        proficiencyXp = existing?.proficiencyXp ?: 0L,
                                        autoQueue = existing?.autoQueue ?: false
                                    )
                                }
                            }
                            var enabledQueues = 0
                            val queueCapacity = input.harvest.miningLvl.coerceAtLeast(1) / 10
                            baseVeins.mapIndexed { idx, v ->
                                val requiredLevel = if (idx == 0) 1 else idx * 5
                                val queueAllowed = v.autoQueue && input.harvest.miningLvl >= requiredLevel && enabledQueues < queueCapacity
                                if (queueAllowed) enabledQueues++
                                v.copy(
                                    miningProgress = v.miningProgress.coerceIn(0f, 1f),
                                    isReady = v.isReady && input.harvest.miningLvl >= requiredLevel,
                                    reqLvl = requiredLevel,
                                    autoQueue = queueAllowed,
                                    proficiencyLevel = v.proficiencyLevel.coerceIn(1, 100),
                                    proficiencyXp = v.proficiencyXp.coerceAtLeast(0L)
                                )
                            }
                        },
                        upgrades = clampUpgrades(input.harvest.upgrades)
                    ),
                    armory = input.armory.copy(
                        smithingLvl = input.armory.smithingLvl.coerceAtLeast(1),
                        herbloreLvl = input.armory.herbloreLvl.coerceAtLeast(1),
                        smithingXp = input.armory.smithingXp.coerceAtLeast(0L),
                        herbloreXp = input.armory.herbloreXp.coerceAtLeast(0L),
                        upgrades = clampUpgrades(input.armory.upgrades)
                    ),
                    training = input.training.copy(
                        lvl = input.training.lvl.coerceAtLeast(1),
                        xp = input.training.xp.coerceAtLeast(0L),
                        nodes = input.training.nodes.map { it.copy(maxRank = 1_000_000, rank = it.rank.coerceIn(0, 1_000_000)) }
                    ),
                    essence = input.essence.copy(
                        lvl = input.essence.lvl.coerceAtLeast(1),
                        xp = input.essence.xp.coerceAtLeast(0L),
                        faith = input.essence.faith.coerceAtLeast(0L),
                        faithRemainder = input.essence.faithRemainder.coerceIn(0.0, 1.0),
                        smiteLevel = input.essence.smiteLevel.coerceAtLeast(1),
                        upgrades = clampUpgrades(input.essence.upgrades)
                    ),
                    beasts = input.beasts.copy(
                        lvl = input.beasts.lvl.coerceAtLeast(1),
                        xp = input.beasts.xp.coerceAtLeast(0L),
                        beasts = input.beasts.beasts.take(Balance.BEAST_CAP),
                        upgrades = clampUpgrades(input.beasts.upgrades)
                    ),
                    talent = input.talent.copy(
                        talentPoints = input.talent.talentPoints.coerceAtLeast(0),
                        struggleDamageTaken = input.talent.struggleDamageTaken.coerceAtLeast(0L),
                        struggleDamageFraction = input.talent.struggleDamageFraction.coerceIn(0.0, 1.0),
                        activeTalents = clampTalents(input.talent.activeTalents),
                        struggleTalents = clampTalents(input.talent.struggleTalents)
                    ),
                    slayer = input.slayer.copy(
                        lvl = input.slayer.lvl.coerceAtLeast(1),
                        xp = input.slayer.xp.coerceAtLeast(0L),
                        slayerPoints = input.slayer.slayerPoints.coerceAtLeast(0),
                        currentTask = input.slayer.currentTask.copy(
                            currentKills = input.slayer.currentTask.currentKills.coerceAtLeast(0),
                            requiredKills = input.slayer.currentTask.requiredKills.coerceAtLeast(1)
                        ),
                        tasks = input.slayer.tasks.map { task ->
                            task.copy(
                                currentKills = task.currentKills.coerceAtLeast(0),
                                requiredKills = task.requiredKills.coerceAtLeast(1)
                            )
                        }
                    ),
                    promotion = input.promotion.copy(
                        tier = input.promotion.tier.coerceIn(1, input.promotion.maxTier),
                        bonusPercent = input.promotion.bonusPercent.coerceAtLeast(0),
                        requiredGearScore = input.promotion.requiredGearScore.coerceAtLeast(0)
                    ),
                    automation = input.automation.copy(
                        craftMaterialUsagePct = input.automation.craftMaterialUsagePct.coerceIn(0, 100)
                    ),
                    meta = input.meta.copy(
                        nextId = nextIdFrom(input).coerceAtLeast(Balance.RESERVED_ID_FLOOR)
                    )
                )
            }
}
