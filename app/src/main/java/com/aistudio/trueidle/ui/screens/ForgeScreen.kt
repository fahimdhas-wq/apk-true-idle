package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.TierColors
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

enum class ForgeMode(val label: String, val color: Color) {
    EQUIPMENT("EQUIPMENT FORGE", SecondaryGold),
    HERBS("HERB CRAFTING", GreenSuccess)
}

@Composable
fun ForgeScreen(
    state: GameState,
    onCraftEquipment: (String, Int, String, String, Int) -> Unit,
    onBrewPotion: (String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val armory = state.armory
    val scrollState = rememberScrollState()
    var selectedMode by rememberSaveable { mutableStateOf(ForgeMode.EQUIPMENT) }
    var selectedGroup by rememberSaveable { mutableStateOf(Equipment.GROUPS.first()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("forge_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP MODE SWITCHER: EQUIPMENT FORGE vs HERB CRAFTING
        Row(
            modifier = Modifier.fillMaxWidth().testTag("forge_mode_selector"),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ForgeMode.entries.forEach { mode ->
                val selected = selectedMode == mode
                Button(
                    onClick = { selectedMode = mode },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) mode.color.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface,
                        contentColor = if (selected) mode.color else TextMuted
                    ),
                    border = BorderStroke(1.dp, if (selected) mode.color else MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f).testTag("forge_mode_${mode.name.lowercase()}")
                ) {
                    Text(
                        text = mode.label,
                        style = TrueIdleType.title,
                        color = if (selected) mode.color else TextMuted
                    )
                }
            }
        }

        // =====================================================================
        // MODE 1: EQUIPMENT FORGE (13 DEDICATED EQUIPMENT GROUP VIEWS)
        // =====================================================================
        if (selectedMode == ForgeMode.EQUIPMENT) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth().testTag("forge_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RectangleShape,
                border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CYBERNETIC FABRICATION MATRIX", style = TrueIdleType.title, color = SecondaryGold)
                        Text(
                            text = "GROUP: ${selectedGroup.uppercase()}",
                            style = TrueIdleType.tag,
                            color = SecondaryGold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    XpProgressBar(
                        skillName = "SMITHING FORGE",
                        level = armory.smithingLvl,
                        currentXp = armory.smithingXp,
                        requiredXp = XpCurves.skill(armory.smithingLvl),
                        barColor = SecondaryGold
                    )
                }
            }

            // Group Selector Chips: 13 dedicated equipment groups
            Text("SELECT EQUIPMENT GROUP", style = TrueIdleType.tag, color = TextMuted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp)
                    .testTag("equipment_group_selector"),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Equipment.GROUPS.forEach { group ->
                    val isSelected = selectedGroup == group
                    val isWep = Equipment.isWeapon(group)
                    val accent = if (isWep) HpRed else SecondaryGold
                    Box(
                        modifier = Modifier
                            .clickable(role = Role.Tab) { selectedGroup = group }
                            .background(
                                if (isSelected) accent.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                RectangleShape
                            )
                            .border(1.dp, if (isSelected) accent else MaterialTheme.colorScheme.outline)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("group_chip_${group.replace(" ", "_").lowercase()}")
                    ) {
                        Text(
                            text = group.uppercase(),
                            style = TrueIdleType.tag,
                            color = if (isSelected) accent else TextMuted
                        )
                    }
                }
            }

            // Dedicated Group View: Full Tier List for selected group
            val recipes = ContentCatalog.RECIPES_BY_GROUP[selectedGroup] ?: emptyList()
            val isWeaponGroup = Equipment.isWeapon(selectedGroup)
            val groupAccent = if (isWeaponGroup) HpRed else SecondaryGold

            SciFiPanel(
                title = "$selectedGroup FABRICATION LINE (${recipes.size} TIERS)",
                accent = groupAccent,
                tag = if (isWeaponGroup) "STR SCALED WEAPON" else "DEF + VIT ARMOR"
            ) {
                recipes.forEach { recipe ->
                    val have = state.inventory.find { it.name == recipe.reqOre }?.count ?: 0.0
                    val canCraft = have >= recipe.reqQty
                    val tierInfo = TierConfig.forOrdinal(recipe.tier)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("recipe_card_${recipe.name.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RectangleShape,
                        border = BorderStroke(
                            1.dp,
                            if (canCraft) groupAccent.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                        )
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
                                        text = recipe.name,
                                        style = TrueIdleType.title,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "[${tierInfo.id}]",
                                        style = TrueIdleType.tag,
                                        color = TierColors.colorForOrdinal(recipe.tier)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${recipe.statPreview} · Requires: ${recipe.reqQty}x ${recipe.reqOre} (Have ${NumberFormatter.short(have)})",
                                    style = TrueIdleType.meta,
                                    color = if (canCraft) GreenSuccess else TextMuted
                                )
                            }
                            Button(
                                onClick = {
                                    onCraftEquipment(
                                        recipe.name,
                                        recipe.tier,
                                        recipe.slot,
                                        recipe.reqOre,
                                        recipe.reqQty
                                    )
                                },
                                enabled = canCraft,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canCraft) groupAccent else MaterialTheme.colorScheme.surface,
                                    contentColor = if (canCraft) Color.Black else TextMuted
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (canCraft) groupAccent else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag("craft_btn_${recipe.name.replace(" ", "_")}")
                            ) {
                                Text("FORGE", style = TrueIdleType.tag)
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // MODE 2: HERB CRAFTING (DEDICATED POTION & BIO-CHEMICAL INTERFACE)
        // =====================================================================
        if (selectedMode == ForgeMode.HERBS) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth().testTag("herb_crafting_header_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RectangleShape,
                border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BIO-CHEMICAL HERB SYNTHESIS", style = TrueIdleType.title, color = GreenSuccess)
                        Text("${ItemCatalog.POTIONS.size} FORMULAS", style = TrueIdleType.tag, color = GreenSuccess)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    XpProgressBar(
                        skillName = "HERBLORE ALCHEMY",
                        level = armory.herbloreLvl,
                        currentXp = armory.herbloreXp,
                        requiredXp = XpCurves.skill(armory.herbloreLvl),
                        barColor = GreenSuccess
                    )
                }
            }

            SciFiPanel(
                title = "PHARMACEUTICAL BREW LAB (${ItemCatalog.POTIONS.size} POTIONS)",
                accent = GreenSuccess,
                tag = "HERB EXTRACTION"
            ) {
                ItemCatalog.POTIONS.forEach { potion ->
                    val have = state.inventory.find { it.name == potion.herb }?.count ?: 0.0
                    val canBrew = have >= potion.herbCost

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .testTag("potion_card_${potion.name.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RectangleShape,
                        border = BorderStroke(
                            1.dp,
                            if (canBrew) GreenSuccess.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = potion.name,
                                    style = TrueIdleType.title,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${potion.effectDesc} · Requires: ${potion.herbCost}x ${potion.herb} (Have ${NumberFormatter.short(have)})",
                                    style = TrueIdleType.meta,
                                    color = if (canBrew) GreenSuccess else TextMuted
                                )
                            }
                            Button(
                                onClick = { onBrewPotion(potion.name, potion.herb, potion.herbCost) },
                                enabled = canBrew,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canBrew) GreenSuccess else MaterialTheme.colorScheme.surface,
                                    contentColor = if (canBrew) Color.Black else TextMuted
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (canBrew) GreenSuccess else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.testTag("brew_btn_${potion.name.replace(" ", "_")}")
                            ) {
                                Text("BREW", style = TrueIdleType.tag)
                            }
                        }
                    }
                }
            }
        }
    }
}
