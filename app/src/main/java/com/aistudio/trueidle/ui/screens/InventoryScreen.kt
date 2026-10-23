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
            .padding(12.dp)
            .testTag("inventory_screen")
    ) {
        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = (selectedCategory == cat)
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) },
                    modifier = Modifier.testTag("inv_cat_$cat"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryNeon.copy(alpha = 0.2f),
                        selectedLabelColor = PrimaryNeon
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No items in this category.", color = TextMuted, fontSize = 14.sp)
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
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    if (item.count > 1) {
                                        Text("x${item.count}", style = MaterialTheme.typography.labelSmall, color = SecondaryGold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.statDesc, style = MaterialTheme.typography.bodySmall, color = PrimaryNeon)
                                Text("${item.category} • Value: ${item.sellValue} 🪙", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }

                            if (item.category == "Equipment") {
                                Button(
                                    onClick = { onEquipItem(item.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = BgDark),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Equip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (item.category == "Potion") {
                                Button(
                                    onClick = { onEquipItem(item.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess, contentColor = BgDark),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Use", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
