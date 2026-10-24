package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.Beast
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.XpProgressBar
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
            .background(BgDark)
            .padding(10.dp)
            .testTag("beasts_screen")
    ) {
        // Beasts Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, GreenSuccess.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("BEAST SANCTUARY", style = MaterialTheme.typography.titleLarge, color = GreenSuccess, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Bio-capture telemetry: 15% rate on kill", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Box(
                        modifier = Modifier
                            .background(GreenSuccess.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, GreenSuccess, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🐾 ${state.beasts.beasts.size}/8 SLOTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = GreenSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                XpProgressBar(
                    skillName = "BEAST TAMING",
                    level = state.beasts.lvl,
                    currentXp = state.beasts.xp,
                    requiredXp = Balance.skillXpReq(state.beasts.lvl),
                    barColor = GreenSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected merge helper notice
        if (selectedBeast1 != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                shape = RectangleShape,
                border = BorderStroke(1.dp, SecondaryGold)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SELECTED: ${selectedBeast1?.name?.uppercase()} ⭐${selectedBeast1?.stars}. TAP IDENTICAL BEAST TO MERGE", color = SecondaryGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { selectedBeast1 = null },
                        shape = RectangleShape,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("CANCEL", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Beasts List
        if (state.beasts.beasts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("NO COMPANIONS CAPTURED. ENGAGE HOSTILES TO TAME BEASTS.", color = TextMuted, fontSize = 12.sp, letterSpacing = 0.5.sp)
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
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (isSelected) SecondaryGold else BorderDark)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(beast.name.uppercase(), style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⭐".repeat(beast.stars), fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "DPS: ${beast.dps.toInt()} • HP: ${beast.hp.toInt()}/${beast.maxHp.toInt()} • TRAIT: ${beast.trait.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GreenSuccess
                                )
                            }

                            OutlinedButton(
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
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (isSelected) SecondaryGold else PrimaryNeon),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) SecondaryGold.copy(alpha = 0.2f) else PrimaryNeon.copy(alpha = 0.12f),
                                    contentColor = if (isSelected) SecondaryGold else PrimaryNeon
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(if (isSelected) "ACTIVE" else "MERGE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
