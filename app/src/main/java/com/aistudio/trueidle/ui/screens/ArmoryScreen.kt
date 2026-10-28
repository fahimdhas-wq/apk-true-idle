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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.ContentCatalog
import com.aistudio.trueidle.core.ItemCatalog
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

private data class AlchemyRecipe(val name: String, val reqHerb: String, val reqQty: Int, val effectDesc: String)

@Composable
fun ArmoryScreen(
    state: GameState,
    onCraftEquipment: (String, Int, String, String, Int) -> Unit,
    onBrewPotion: (String, String, Int) -> Unit,
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val armory = state.armory
    val scrollState = rememberScrollState()

    // Rendered from the shared catalog so the tier and stat preview cannot drift from the recipe.
    val forgeRecipes = ContentCatalog.FORGE_RECIPES

    // Rendered from the catalog the engine charges against, so the two cannot drift.
    val alchemyRecipes = ItemCatalog.POTIONS.map { potion ->
        AlchemyRecipe(potion.name, potion.herb, potion.herbCost, "Restores ${com.aistudio.trueidle.core.NumberFormatter.short(potion.heal)} HP")
    }

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
                    Text("ARMORY & FABRICATION BAY", style = TrueIdleType.title, color = SecondaryGold)
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, SecondaryGold)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("FORGE ACTIVE", style = TrueIdleType.tag, color = SecondaryGold)
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

        // 2. CYBERNETIC FORGE (Equipment Crafting)
        SciFiPanel(
            title = "CYBER FORGE (WEAPONS & ARMOR)",
            accent = SecondaryGold,
            tag = "TIER 1-6"
        ) {
            forgeRecipes.forEach { recipe ->
                val have = state.inventory.find { it.name == recipe.reqOre }?.count ?: 0.0
                val canCraft = have >= recipe.reqQty

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (canCraft) SecondaryGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(recipe.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                // Forge grade shown with the canonical tier colour and name.
                                Text(
                                    text = "[T${recipe.tier} ${com.aistudio.trueidle.core.TierConfig.forOrdinal(recipe.tier).id}]",
                                    style = TrueIdleType.tag,
                                    color = com.aistudio.trueidle.ui.TierColors.colorForOrdinal(recipe.tier)
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
                            onClick = { onCraftEquipment(recipe.name, recipe.tier, recipe.slot, recipe.reqOre, recipe.reqQty) },
                            enabled = canCraft,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canCraft) SecondaryGold else MaterialTheme.colorScheme.surface,
                                contentColor = if (canCraft) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canCraft) SecondaryGold else MaterialTheme.colorScheme.outline)
                        ) {
                            Text("FORGE", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. ALCHEMY SYNTHESIS BAY
        SciFiPanel(
            title = "BIO-CHEMICAL SYNTHESIZER",
            accent = GreenSuccess,
            tag = "POTIONS"
        ) {
            alchemyRecipes.forEach { recipe ->
                val have = state.inventory.find { it.name == recipe.reqHerb }?.count ?: 0.0
                val canBrew = have >= recipe.reqQty

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (canBrew) GreenSuccess.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(recipe.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${recipe.effectDesc} · Requires: ${recipe.reqQty}x ${recipe.reqHerb} (Have ${NumberFormatter.short(have)})",
                                style = TrueIdleType.meta,
                                color = if (canBrew) GreenSuccess else TextMuted
                            )
                        }
                        Button(
                            onClick = { onBrewPotion(recipe.name, recipe.reqHerb, recipe.reqQty) },
                            enabled = canBrew,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canBrew) GreenSuccess else MaterialTheme.colorScheme.surface,
                                contentColor = if (canBrew) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canBrew) GreenSuccess else MaterialTheme.colorScheme.outline)
                        ) {
                            Text("SYNTHESIZE", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4. ARMORY UPGRADES BAY
        SciFiPanel(
            title = "ARMORY PROTOCOL UPGRADES",
            accent = PrimaryNeon,
            tag = "MODIFICATIONS"
        ) {
            armory.upgrades.forEach { upg ->
                val cost = UpgradeCosts.nextRank(upg.baseCost, upg.rank, upg.costMultiplier)
                val canAfford = state.player.gold >= cost
                val isMax = upg.rank >= upg.maxRank

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(upg.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RectangleShape)
                                        .border(1.dp, PrimaryNeon)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("RANK ${upg.rank}/${upg.maxRank}", style = TrueIdleType.numberSmall, color = PrimaryNeon)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(upg.description, style = TrueIdleType.meta, color = TextMuted)
                        }

                        Button(
                            onClick = { onUpgradeDiscipline(DisciplineCategory.ARMORY, upg.id) },
                            enabled = canAfford && !isMax,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) PrimaryNeon else MaterialTheme.colorScheme.surface,
                                contentColor = if (canAfford) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canAfford) PrimaryNeon else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("upgrade_armory_${upg.id}")
                        ) {
                            if (isMax) {
                                Text("MAX", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                            } else {
                                SciFiBadge(
                                    type = SciFiIconType.CREDIT,
                                    label = NumberFormatter.short(cost),
                                    iconTint = if (canAfford) Color.Black else TextMuted,
                                    textColor = if (canAfford) Color.Black else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
