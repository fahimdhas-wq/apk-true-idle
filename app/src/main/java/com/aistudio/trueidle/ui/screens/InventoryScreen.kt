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

@Composable
fun InventoryScreen(
    state: GameState,
    onEquipItem: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Equipment", "Potion", "Resource")

    val filteredItems = when (selectedCategory) {
        "Equipment" -> state.inventory.filter { it.category == "Equipment" }
        "Potion" -> state.inventory.filter { it.category == "Potion" }
        "Resource" -> state.inventory.filter { it.category == "Resource" || it.category == "Trophy" }
        else -> state.inventory
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(10.dp)
            .testTag("inventory_screen")
    ) {
        // High-end Cyberpunk Category Filter Switchers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = (selectedCategory == cat)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedCategory = cat }
                        .background(if (isSelected) PrimaryNeon.copy(alpha = 0.15f) else SurfaceDark, RectangleShape)
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) PrimaryNeon else BorderDark
                            ),
                            RectangleShape
                        )
                        .padding(vertical = 8.dp)
                        .testTag("inv_cat_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PrimaryNeon else TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ARSENAL BAY EMPTY FOR THIS CATEGORY.", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.5.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("inventory_item_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (item.count > 1) {
                                        Text("[x${item.count}]", style = MaterialTheme.typography.labelSmall, color = SecondaryGold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.statDesc, style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                                Text("${item.category.uppercase()} • VALUE: ${item.sellValue} 🪙", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }

                            if (item.category == "Equipment") {
                                OutlinedButton(
                                    onClick = { onEquipItem(item.id) },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, PrimaryNeon),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = PrimaryNeon.copy(alpha = 0.15f),
                                        contentColor = PrimaryNeon
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("EQUIP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            } else if (item.category == "Potion") {
                                OutlinedButton(
                                    onClick = { onEquipItem(item.id) },
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, GreenSuccess),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = GreenSuccess.copy(alpha = 0.15f),
                                        contentColor = GreenSuccess
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("DEPLOY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
