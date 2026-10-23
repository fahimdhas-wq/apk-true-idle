package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            .padding(12.dp)
            .testTag("crafting_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = PrimaryNeon,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🔨 Smithing (LVL ${state.crafting.smithingLvl})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("🧪 Herblore (LVL ${state.crafting.herbloreLvl})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                Text(recipe.desc, style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Requires: ${recipe.reqQty}x ${recipe.reqOre} (Have: $availableOre)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canCraft) GreenSuccess else HpRed
                                )
                            }

                            Button(
                                onClick = { onCraftEquipment(recipe.name, recipe.tier, recipe.slot, recipe.reqOre, recipe.reqQty) },
                                enabled = canCraft,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canCraft) SecondaryGold else SurfaceVariantDark,
                                    contentColor = if (canCraft) BgDark else TextMuted
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Forge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.name, style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                Text(recipe.desc, style = MaterialTheme.typography.bodySmall, color = GreenSuccess)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Requires: ${recipe.reqQty}x ${recipe.reqHerb} (Have: $availableHerb)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (canBrew) GreenSuccess else HpRed
                                )
                            }

                            Button(
                                onClick = { onBrewPotion(recipe.name, recipe.reqHerb, recipe.reqQty) },
                                enabled = canBrew,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canBrew) GreenSuccess else SurfaceVariantDark,
                                    contentColor = if (canBrew) BgDark else TextMuted
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Brew", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
