package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.TierColors
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun ArmoryScreen(
    state: GameState,
    onCraftEquipment: (String, Int, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onBrewPotion: (String, String, Int) -> Unit = { _, _, _ -> },
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    onUnequipItem: (String) -> Unit = {},
    onAutoEquip: () -> Unit = {},
    onOpenForge: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val armory = state.armory
    val scrollState = rememberScrollState()

    val equippedWeapons = state.inventory.filter { it.isEquipped && Equipment.isWeapon(it.slot) }
    val equippedArmorAndNodes = state.inventory.filter { it.isEquipped && Equipment.isArmorOrNode(it.slot) }
    val totalWepAtk = equippedWeapons.sumOf { Equipment.baseAttack(it) }
    val totalArmorDef = equippedArmorAndNodes.sumOf { Equipment.baseDefense(it) }
    val totalItemVit = equippedArmorAndNodes.sumOf { Equipment.baseVit(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("armory_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. ARMORY MASTERY OVERVIEW
        Card(
            modifier = Modifier.fillMaxWidth().testTag("armory_header_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ARMORY & TACTICAL LOADOUT", style = TrueIdleType.title, color = SecondaryGold)
                    Button(
                        onClick = onAutoEquip,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryGold.copy(alpha = 0.2f),
                            contentColor = SecondaryGold
                        ),
                        border = BorderStroke(1.dp, SecondaryGold),
                        modifier = Modifier.testTag("armory_auto_equip_btn")
                    ) {
                        Text("AUTO-EQUIP BEST", style = TrueIdleType.tag)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "SMITHING FORGE",
                    level = armory.smithingLvl,
                    currentXp = armory.smithingXp,
                    requiredXp = XpCurves.skill(armory.smithingLvl),
                    barColor = SecondaryGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                XpProgressBar(
                    skillName = "BIO-CHEMICAL ALCHEMY",
                    level = armory.herbloreLvl,
                    currentXp = armory.herbloreXp,
                    requiredXp = XpCurves.skill(armory.herbloreLvl),
                    barColor = GreenSuccess
                )
            }
        }

        // 2. LOADOUT SUMMARY STATS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RectangleShape,
                border = BorderStroke(1.dp, HpRed)
            ) {
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WEAPON ATK", style = TrueIdleType.tag, color = HpRed)
                    Text("+${NumberFormatter.short(totalWepAtk)}", style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                    Text("STR SCALED", style = TrueIdleType.meta, color = TextMuted)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RectangleShape,
                border = BorderStroke(1.dp, SecondaryGold)
            ) {
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ARMOR DEF", style = TrueIdleType.tag, color = SecondaryGold)
                    Text("+${NumberFormatter.short(totalArmorDef)}", style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                    Text("DMG MITIGATION", style = TrueIdleType.meta, color = TextMuted)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RectangleShape,
                border = BorderStroke(1.dp, GreenSuccess)
            ) {
                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GEAR VIT", style = TrueIdleType.tag, color = GreenSuccess)
                    Text("+${NumberFormatter.short(totalItemVit)}", style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                    Text("MAX HP & REGEN", style = TrueIdleType.meta, color = TextMuted)
                }
            }
        }

        // 3. WEAPONS SECTION (3 GROUPS: RIFLE, SIDEARM, BLADE)
        SciFiPanel(
            title = "WEAPON SLOTS (STR SCALED)",
            accent = HpRed,
            tag = "${equippedWeapons.size}/${Equipment.WEAPON_SLOTS.size} EQUIPPED"
        ) {
            Equipment.WEAPON_SLOTS.forEach { slot ->
                val equipped = Equipment.equipped(state, slot)
                val slotDesc = when (slot) {
                    Equipment.SLOT_RIFLE -> "PRIMARY RANGED WEAPON · STR DAMAGE"
                    Equipment.SLOT_SIDEARM -> "BACKUP RANGED WEAPON · STR DAMAGE"
                    Equipment.SLOT_BLADE -> "MELEE CLOSE-COMBAT WEAPON · STR DAMAGE"
                    else -> "WEAPON"
                }
                SlotCard(
                    slotName = slot,
                    slotRole = slotDesc,
                    equippedItem = equipped,
                    accent = HpRed,
                    onUnequip = { equipped?.let { onUnequipItem(it.id) } }
                )
            }
        }

        // 4. ARMOR SLOTS (7 GROUPS: VISOR, VEST, GRIPS, PANTS, BOOTS, DUSTER, RIG)
        SciFiPanel(
            title = "ARMOR PLATING (DEF & VIT)",
            accent = SecondaryGold,
            tag = "${state.inventory.count { it.isEquipped && Equipment.ARMOR_SLOTS.any { s -> s.equals(it.slot, ignoreCase = true) } }}/${Equipment.ARMOR_SLOTS.size} EQUIPPED"
        ) {
            Equipment.ARMOR_SLOTS.forEach { slot ->
                val equipped = Equipment.equipped(state, slot)
                val slotDesc = when (slot) {
                    Equipment.SLOT_VISOR -> "HEAD SENSORS · DEF & VIT"
                    Equipment.SLOT_VEST -> "CHEST CHASSIS · DEF & VIT"
                    Equipment.SLOT_GRIPS -> "GAUNTLETS · DEF & VIT"
                    Equipment.SLOT_PANTS -> "LEG REINFORCEMENTS · DEF & VIT"
                    Equipment.SLOT_BOOTS -> "FOOT STABILIZERS · DEF & VIT"
                    Equipment.SLOT_DUSTER -> "CLOAK / MANTLE · DEF & VIT"
                    Equipment.SLOT_RIG -> "TACTICAL BELT · DEF & VIT"
                    else -> "ARMOR"
                }
                SlotCard(
                    slotName = slot,
                    slotRole = slotDesc,
                    equippedItem = equipped,
                    accent = SecondaryGold,
                    onUnequip = { equipped?.let { onUnequipItem(it.id) } }
                )
            }
        }

        // 5. NODE SLOTS (3 GROUPS: NODE I, NODE II, NODE III)
        SciFiPanel(
            title = "BIO-NEURAL NODES (DEF & VIT)",
            accent = ManaTeal,
            tag = "${state.inventory.count { it.isEquipped && Equipment.NODE_SLOTS.any { s -> s.equals(it.slot, ignoreCase = true) } }}/${Equipment.NODE_SLOTS.size} EQUIPPED"
        ) {
            Equipment.NODE_SLOTS.forEach { slot ->
                val equipped = Equipment.equipped(state, slot)
                val slotDesc = when (slot) {
                    Equipment.SLOT_NODE_I -> "RING NODE I · DEF & VIT"
                    Equipment.SLOT_NODE_II -> "RING NODE II · DEF & VIT"
                    Equipment.SLOT_NODE_III -> "RING NODE III · DEF & VIT"
                    else -> "RING NODE"
                }
                SlotCard(
                    slotName = slot,
                    slotRole = slotDesc,
                    equippedItem = equipped,
                    accent = ManaTeal,
                    onUnequip = { equipped?.let { onUnequipItem(it.id) } }
                )
            }
        }

        // 6. ARMORY DISCIPLINE UPGRADES
        SciFiPanel(
            title = "ARMORY PROTOCOLS",
            accent = SecondaryGold,
            tag = "FABRICATION TECH"
        ) {
            armory.upgrades.forEach { upgrade ->
                ArmoryUpgradeCard(
                    upgrade = upgrade,
                    playerCredits = state.player.gold,
                    onUpgrade = { onUpgradeDiscipline(DisciplineCategory.ARMORY, upgrade.id) }
                )
            }
        }
    }
}

@Composable
private fun SlotCard(
    slotName: String,
    slotRole: String,
    equippedItem: com.aistudio.trueidle.model.InventoryItem?,
    accent: Color,
    onUnequip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .testTag("slot_card_${slotName.replace(" ", "_").lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RectangleShape,
        border = BorderStroke(1.dp, if (equippedItem != null) accent.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = slotName.uppercase(),
                        style = TrueIdleType.tag,
                        color = accent
                    )
                    if (equippedItem != null) {
                        val tierInfo = TierConfig.forOrdinal(equippedItem.tier)
                        Text(
                            text = "[${tierInfo.id}]",
                            style = TrueIdleType.tag,
                            color = TierColors.colorForOrdinal(equippedItem.tier)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                if (equippedItem != null) {
                    Text(
                        text = equippedItem.name,
                        style = TrueIdleType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${equippedItem.statDesc} · $slotRole",
                        style = TrueIdleType.meta,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = "[EMPTY SLOT]",
                        style = TrueIdleType.title,
                        color = TextMuted
                    )
                    Text(
                        text = slotRole,
                        style = TrueIdleType.meta,
                        color = TextMuted.copy(alpha = 0.6f)
                    )
                }
            }

            if (equippedItem != null) {
                Button(
                    onClick = onUnequip,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = HpRed
                    ),
                    border = BorderStroke(1.dp, HpRed),
                    modifier = Modifier.testTag("unequip_btn_${slotName.replace(" ", "_").lowercase()}")
                ) {
                    Text("UNEQUIP", style = TrueIdleType.tag)
                }
            }
        }
    }
}

@Composable
private fun ArmoryUpgradeCard(
    upgrade: DisciplineUpgrade,
    playerCredits: Long,
    onUpgrade: () -> Unit
) {
    val cost = UpgradeCosts.nextRank(upgrade.baseCost, upgrade.rank)
    val maxed = upgrade.rank >= upgrade.maxRank
    val canAfford = !maxed && playerCredits >= cost

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(upgrade.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                Text(upgrade.description, style = TrueIdleType.meta, color = TextMuted)
                Text("RANK ${upgrade.rank}/${upgrade.maxRank}", style = TrueIdleType.tag, color = SecondaryGold)
            }
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGold,
                    contentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = TextMuted
                ),
                border = BorderStroke(1.dp, if (canAfford) SecondaryGold else MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = if (maxed) "MAX" else "${NumberFormatter.short(cost)} CR",
                    style = TrueIdleType.tag
                )
            }
        }
    }
}
