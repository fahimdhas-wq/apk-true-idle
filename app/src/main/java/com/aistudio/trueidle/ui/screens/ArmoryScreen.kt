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
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import kotlin.math.pow

private data class ForgeRecipe(val name: String, val tier: Int, val slot: String, val reqOre: String, val reqQty: Int, val statPreview: String)
private data class AlchemyRecipe(val name: String, val reqHerb: String, val reqQty: Int, val effectDesc: String)

@Composable
fun ArmoryScreen(
    state: GameState,
    onCraftEquipment: (String, Int, String, String, Int) -> Unit,
    onBrewPotion: (String, String, Int) -> Unit,
    onUpgradeDiscipline: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val armory = state.armory
    val scrollState = rememberScrollState()

    val forgeRecipes = listOf(
        ForgeRecipe("Bronze Glaive", 1, "Weapon", "Copper Ore", 6, "+12 ATK"),
        ForgeRecipe("Heavy Plate", 1, "Armor", "Copper Ore", 8, "+8 DEF"),
        ForgeRecipe("Titan Edge", 2, "Weapon", "Tin Ore", 10, "+24 ATK"),
        ForgeRecipe("Cobalt Cuirass", 2, "Armor", "Tin Ore", 12, "+16 DEF"),
        ForgeRecipe("Plasma Cleaver", 3, "Weapon", "Iron Ore", 15, "+36 ATK"),
        ForgeRecipe("Aegis Carapace", 3, "Armor", "Iron Ore", 18, "+24 DEF")
    )

    val alchemyRecipes = listOf(
        AlchemyRecipe("Minor Health Flask", "Felweed", 4, "Restores 50 HP"),
        AlchemyRecipe("Bloodroot Tonic", "Bloodroot", 6, "Restores 80 HP"),
        AlchemyRecipe("Moonlight Philter", "Moonflower", 8, "Restores 150 HP")
    )

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
                    requiredXp = Balance.skillXpReq(armory.smithingLvl),
                    barColor = SecondaryGold
                )
                Spacer(modifier = Modifier.height(6.dp))
                XpProgressBar(
                    skillName = "BIO-CHEMICAL ALCHEMY",
                    level = armory.herbloreLvl,
                    currentXp = armory.herbloreXp,
                    requiredXp = Balance.skillXpReq(armory.herbloreLvl),
                    barColor = GreenSuccess
                )
            }
        }

        // 2. CYBERNETIC FORGE (Equipment Crafting)
        SciFiPanel(
            title = "CYBER FORGE (WEAPONS & ARMOR)",
            accent = SecondaryGold,
            tag = "TIER 1-3"
        ) {
            forgeRecipes.forEach { recipe ->
                val have = state.inventory.find { it.name == recipe.reqOre }?.count ?: 0
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
                            Text("${recipe.name} [T${recipe.tier}]", style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${recipe.statPreview} · Requires: ${recipe.reqQty}x ${recipe.reqOre} (Have $have)",
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
                val have = state.inventory.find { it.name == recipe.reqHerb }?.count ?: 0
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
                                text = "${recipe.effectDesc} · Requires: ${recipe.reqQty}x ${recipe.reqHerb} (Have $have)",
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
                val cost = (upg.baseCost * upg.costMultiplier.pow(upg.rank.toDouble())).toLong()
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
                            onClick = { onUpgradeDiscipline("ARMORY", upg.id) },
                            enabled = canAfford && !isMax,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) PrimaryNeon else MaterialTheme.colorScheme.surface,
                                contentColor = if (canAfford) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canAfford) PrimaryNeon else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("upgrade_armory_${upg.id}")
                        ) {
                            Text(
                                text = if (isMax) "MAX" else "🪙 ${NumberFormatter.short(cost)}",
                                style = TrueIdleType.tag,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
