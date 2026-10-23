package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.theme.*

data class CraftRecipe(
    val name: String,
    val tier: Int,
    val slot: String,
    val reqOre: String,
    val reqQty: Int,
    val desc: String
)

data class BrewRecipe(
    val name: String,
    val reqHerb: String,
    val reqQty: Int,
    val desc: String
)

@Composable
fun CraftingScreen(
    state: GameState,
    onCraftEquipment: (String, Int, String, String, Int) -> Unit,
    onBrewPotion: (String, String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Smithing, 1: Herblore

    val smithRecipes = listOf(
        CraftRecipe("Copper Shortsword", 1, "Weapon", "Copper Ore", 4, "+12 Attack Damage"),
        CraftRecipe("Bronze Breastplate", 1, "Armor", "Copper Ore", 5, "+8 Physical Armor"),
        CraftRecipe("Iron Claymore", 2, "Weapon", "Iron Ore", 5, "+24 Attack Damage"),
        CraftRecipe("Steel Hauberk", 2, "Armor", "Iron Ore", 6, "+16 Physical Armor")
    )

    val brewRecipes = listOf(
        BrewRecipe("Minor Health Flask", "Felweed", 3, "Restores 50 HP immediately"),
        BrewRecipe("Bloodroot Tonic", "Bloodroot", 3, "Restores 80 HP and clears wounds"),
        BrewRecipe("Moonlight Philter", "Moonflower", 3, "Restores 50 Mana")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(10.dp)
            .testTag("crafting_screen")
    ) {
        // High-end Cyberpunk Dual Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RectangleShape)
                .background(SurfaceDark, RectangleShape)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 }
                    .background(if (selectedTab == 0) SecondaryGold.copy(alpha = 0.15f) else Color.Transparent, RectangleShape)
                    .border(
                        BorderStroke(
                            width = if (selectedTab == 0) 1.5.dp else 0.dp,
                            color = if (selectedTab == 0) SecondaryGold else Color.Transparent
                        ),
                        RectangleShape
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔨 CYBER FORGE [LVL ${state.crafting.smithingLvl}]",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedTab == 0) SecondaryGold else TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 }
                    .background(if (selectedTab == 1) GreenSuccess.copy(alpha = 0.15f) else Color.Transparent, RectangleShape)
                    .border(
                        BorderStroke(
                            width = if (selectedTab == 1) 1.5.dp else 0.dp,
                            color = if (selectedTab == 1) GreenSuccess else Color.Transparent
                        ),
                        RectangleShape
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🧪 ALCHEMY LAB [LVL ${state.crafting.herbloreLvl}]",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedTab == 1) GreenSuccess else TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(smithRecipes) { recipe ->
                    val availableOre = state.inventory.find { it.name == recipe.reqOre }?.count ?: 0
                    val canCraft = availableOre >= recipe.reqQty

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("smith_recipe_${recipe.name}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (canCraft) SecondaryGold.copy(alpha = 0.5f) else BorderDark)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(recipe.desc, style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "REQUIRES: ${recipe.reqQty}x ${recipe.reqOre.uppercase()} (HAVE: $availableOre)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canCraft) GreenSuccess else HpRed
                                )
                            }

                            OutlinedButton(
                                onClick = { onCraftEquipment(recipe.name, recipe.tier, recipe.slot, recipe.reqOre, recipe.reqQty) },
                                enabled = canCraft,
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (canCraft) SecondaryGold else BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (canCraft) SecondaryGold.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    contentColor = if (canCraft) SecondaryGold else TextMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("FORGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(brewRecipes) { recipe ->
                    val availableHerb = state.inventory.find { it.name == recipe.reqHerb }?.count ?: 0
                    val canBrew = availableHerb >= recipe.reqQty

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("brew_recipe_${recipe.name}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (canBrew) GreenSuccess.copy(alpha = 0.5f) else BorderDark)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Text(recipe.desc, style = MaterialTheme.typography.bodySmall, color = GreenSuccess)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "REQUIRES: ${recipe.reqQty}x ${recipe.reqHerb.uppercase()} (HAVE: $availableHerb)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canBrew) GreenSuccess else HpRed
                                )
                            }

                            OutlinedButton(
                                onClick = { onBrewPotion(recipe.name, recipe.reqHerb, recipe.reqQty) },
                                enabled = canBrew,
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (canBrew) GreenSuccess else BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (canBrew) GreenSuccess.copy(alpha = 0.2f) else SurfaceVariantDark,
                                    contentColor = if (canBrew) GreenSuccess else TextMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("SYNTH", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
