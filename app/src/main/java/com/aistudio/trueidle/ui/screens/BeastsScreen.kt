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
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.theme.*

@Composable
fun BeastsScreen(
    state: GameState,
    onMergeBeasts: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBeast1 by remember { mutableStateOf<Beast?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("beasts_screen")
    ) {
        // Beasts Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Beast Sanctuary", style = MaterialTheme.typography.titleLarge, color = GreenSuccess, fontWeight = FontWeight.Bold)
                    Text("Monsters have 15% chance to be tamed on kill", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GreenSuccess.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🐾 ${state.beasts.beasts.size}/8 Slots",
                        style = MaterialTheme.typography.titleMedium,
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected merge helper notice
        if (selectedBeast1 != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Selected: ${selectedBeast1?.name} ⭐${selectedBeast1?.stars}. Tap an identical beast to merge!", color = SecondaryGold, fontSize = 12.sp)
                    TextButton(onClick = { selectedBeast1 = null }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Beasts List
        if (state.beasts.beasts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No beasts tamed yet. Defeat monsters in combat to capture companions!", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.beasts.beasts, key = { it.id }) { beast ->
                    val isSelected = (selectedBeast1?.id == beast.id)

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("beast_card_${beast.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) SurfaceVariantDark else SurfaceDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (isSelected) SecondaryGold else BorderDark
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(beast.name, style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐".repeat(beast.stars), fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "DPS: ${beast.dps.toInt()} • HP: ${beast.hp.toInt()}/${beast.maxHp.toInt()} • Trait: ${beast.trait}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GreenSuccess
                                )
                            }

                            Button(
                                onClick = {
                                    if (selectedBeast1 == null) {
                                        selectedBeast1 = beast
                                    } else {
                                        if (selectedBeast1?.id != beast.id && selectedBeast1?.name == beast.name && selectedBeast1?.stars == beast.stars) {
                                            onMergeBeasts(selectedBeast1!!.id, beast.id)
                                            selectedBeast1 = null
                                        } else {
                                            selectedBeast1 = beast
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) SecondaryGold else PrimaryNeon,
                                    contentColor = BgDark
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(if (isSelected) "Selected" else "Merge", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
