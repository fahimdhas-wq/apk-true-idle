package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.Equipment
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.theme.*

@Composable
fun InventoryScreen(
    state: GameState,
    onEquipItem: (String) -> Unit,
    onUnequipItem: (String) -> Unit,
    onSellItem: ((String, Int) -> Unit)? = null,
    onUsePotion: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // rememberSaveable: these survive rotation and process recreation.
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    // The id is stored, not the item: a stale snapshot showed outdated stats after equip/sell.
    var inspectingItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val inspectingItem = inspectingItemId?.let { id -> state.inventory.firstOrNull { it.id == id } }
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
            .padding(8.dp)
            .testTag("inventory_screen")
    ) {
        // High-end Cyberpunk Category Filter Switchers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = (selectedCategory == cat)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .clickable { selectedCategory = cat }
                        .background(if (isSelected) PrimaryNeon.copy(alpha = 0.15f) else SurfaceDark, RectangleShape)
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) PrimaryNeon else BorderDark
                            ),
                            RectangleShape
                        )
                        .padding(vertical = 6.dp)
                        .testTag("inv_cat_$cat"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.uppercase(),
                        style = TrueIdleType.tag,
                        color = if (isSelected) PrimaryNeon else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gear Compare Card (§4.8 / §7.7)
        inspectingItem?.let { item ->
            if (item.category == "Equipment" && !item.isEquipped) {
                val usesAttack = Equipment.isWeapon(item.slot)
                val currentEquipped = state.inventory.firstOrNull {
                    it.isEquipped && it.slot.equals(item.slot, ignoreCase = true)
                }
                val curVal = if (usesAttack) Equipment.baseAttack(currentEquipped) else Equipment.baseDefense(currentEquipped)
                val newVal = if (usesAttack) Equipment.baseAttack(item) else Equipment.baseDefense(item)
                val delta = newVal - curVal
                val statLabel = if (usesAttack) "ATK" else "DEF"

                SciFiPanel(
                    title = "COMPARISON // ${item.name.uppercase()}",
                    accent = SecondaryGold,
                    tag = "DELTA EVALUATION"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "CURRENT: ${currentEquipped?.name ?: "NONE"}",
                                style = TrueIdleType.meta,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text("${com.aistudio.trueidle.core.NumberFormatter.short(curVal)} $statLabel", style = TrueIdleType.label, color = TextLight)
                        }
                        Text("➔", style = TrueIdleType.title, color = PrimaryNeon)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "NEW: ${item.name}",
                                style = TrueIdleType.meta,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${com.aistudio.trueidle.core.NumberFormatter.short(newVal)} $statLabel ", style = TrueIdleType.label, color = TextLight)
                                Text(
                                    text = if (delta >= 0) "▲ +${com.aistudio.trueidle.core.NumberFormatter.short(delta)}" else "▼ ${com.aistudio.trueidle.core.NumberFormatter.short(delta)}",
                                    style = TrueIdleType.tag,
                                    color = if (delta >= 0) GreenSuccess else HpRed
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (filteredItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("ARSENAL BAY EMPTY FOR THIS CATEGORY.", style = TrueIdleType.body, color = TextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(filteredItems, key = { _, item -> item.id }) { _, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                inspectingItemId = if (inspectingItemId == item.id) null else item.id
                            }
                            .testTag("inventory_item_${item.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RectangleShape,
                        border = BorderStroke(
                            1.dp,
                            // Equipment grades itself with the canonical tier colour; resources and
                            // potions keep the neutral border so the palette keeps meaning something.
                            when {
                                item.isEquipped -> PrimaryNeon
                                item.category == "Equipment" ->
                                    com.aistudio.trueidle.ui.TierColors.colorForOrdinal(item.tier).copy(alpha = 0.7f)
                                else -> BorderDark
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = item.name.uppercase(),
                                        style = TrueIdleType.title,
                                        color = if (item.isEquipped) PrimaryNeon else TextLight
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (item.category == "Equipment") {
                                        val tierColor = com.aistudio.trueidle.ui.TierColors.colorForOrdinal(item.tier)
                                        Box(
                                            modifier = Modifier
                                                .background(tierColor.copy(alpha = 0.16f), RectangleShape)
                                                .border(1.dp, tierColor, RectangleShape)
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                                .testTag("item_tier_${item.id}")
                                        ) {
                                            Text(
                                                text = com.aistudio.trueidle.core.TierConfig.forOrdinal(item.tier).id,
                                                style = TrueIdleType.meta,
                                                color = tierColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    if (item.count > 1) {
                                        Text("[x${item.count}]", style = TrueIdleType.tag, color = SecondaryGold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))
                                Text(item.statDesc, style = TrueIdleType.body, color = PrimaryNeon)
                                Spacer(modifier = Modifier.height(3.dp))

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
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "● EQUIPPED: ${item.slot.uppercase()}",
                                                    style = TrueIdleType.meta,
                                                    color = PrimaryNeon
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(SurfaceVariantDark, RectangleShape)
                                                    .border(1.dp, BorderDark, RectangleShape)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "UNEQUIPPED (${item.slot.uppercase()})",
                                                    style = TrueIdleType.meta,
                                                    color = TextMuted
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = item.category.uppercase(),
                                            style = TrueIdleType.meta,
                                            color = TextMuted
                                        )
                                    }

                                    Text(
                                        text = "• VALUE: ${NumberFormatter.short(item.sellValue)} CR",
                                        style = TrueIdleType.meta,
                                        color = TextMuted
                                    )
                                }
                            }

                            // Action buttons
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("UNEQUIP", style = TrueIdleType.tag)
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
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text("EQUIP", style = TrueIdleType.tag)
                                        }
                                        if (onSellItem != null) {
                                            OutlinedButton(
                                                onClick = { onSellItem(item.id, 1) },
                                                shape = RectangleShape,
                                                border = BorderStroke(1.dp, SecondaryGold),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = SecondaryGold.copy(alpha = 0.12f),
                                                    contentColor = SecondaryGold
                                                ),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                            ) {
                                                Text("SELL", style = TrueIdleType.tag)
                                            }
                                        }
                                    }
                                } else if (item.category == "Potion") {
                                    OutlinedButton(
                                        onClick = {
                                            onUsePotion?.invoke(item.id) ?: onEquipItem(item.id)
                                        },
                                        shape = RectangleShape,
                                        border = BorderStroke(1.dp, GreenSuccess),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = GreenSuccess.copy(alpha = 0.15f),
                                            contentColor = GreenSuccess
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("DEPLOY", style = TrueIdleType.tag)
                                    }
                                } else if (item.category == "Resource" || item.category == "Trophy") {
                                    if (onSellItem != null) {
                                        OutlinedButton(
                                            onClick = { onSellItem(item.id, 1) },
                                            shape = RectangleShape,
                                            border = BorderStroke(1.dp, SecondaryGold),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = SecondaryGold.copy(alpha = 0.12f),
                                                contentColor = SecondaryGold
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text("SELL (1)", style = TrueIdleType.tag)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
