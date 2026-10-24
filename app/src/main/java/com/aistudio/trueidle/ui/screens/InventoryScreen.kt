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
    onUnequipItem: (String) -> Unit,
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
                        border = BorderStroke(
                            1.dp,
                            if (item.isEquipped) PrimaryNeon else BorderDark
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name.uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.isEquipped) PrimaryNeon else TextLight,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (item.count > 1) {
                                        Text("[x${item.count}]", style = MaterialTheme.typography.labelSmall, color = SecondaryGold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(item.statDesc, style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Equipment Status Indicator Badge
                                    if (item.category == "Equipment") {
                                        if (item.isEquipped) {
                                            Box(
                                                modifier = Modifier
                                                    .background(PrimaryNeon.copy(alpha = 0.2f), RectangleShape)
                                                    .border(1.dp, PrimaryNeon, RectangleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "● EQUIPPED: ${item.slot.uppercase()}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryNeon,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(SurfaceVariantDark, RectangleShape)
                                                    .border(1.dp, BorderDark, RectangleShape)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "UNEQUIPPED (${item.slot.uppercase()})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = item.category.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Text(
                                        text = "• VALUE: ${item.sellValue} 🪙",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            // Action buttons
                            if (item.category == "Equipment") {
                                if (item.isEquipped) {
                                    OutlinedButton(
                                        onClick = { onUnequipItem(item.id) },
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, HpRed),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = HpRed.copy(alpha = 0.15f),
                                            contentColor = HpRed
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("UNEQUIP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                } else {
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
