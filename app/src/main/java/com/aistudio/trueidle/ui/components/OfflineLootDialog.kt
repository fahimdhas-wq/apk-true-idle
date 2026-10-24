package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.OfflineLoot
import com.aistudio.trueidle.ui.theme.*

@Composable
fun OfflineLootDialog(
    loot: OfflineLoot,
    onClaim: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val timeFormatted = NumberFormatter.duration(loot.timeAwaySeconds)
    val efficiencyPercent = NumberFormatter.percent(loot.efficiency)

    Dialog(onDismissRequest = onClaim) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("offline_loot_dialog"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.5.dp, PrimaryNeon)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OFFLINE REPORT",
                            style = TrueIdleType.title,
                            color = PrimaryNeon
                        )
                        Text(
                            text = "EFFICIENCY: $efficiencyPercent",
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, PrimaryNeon, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (loot.capApplied) "CAP 8h APPLIED ($timeFormatted)" else timeFormatted,
                            style = TrueIdleType.tag,
                            color = if (loot.capApplied) Warn else PrimaryNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sector progression
                if (loot.floorsGained > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.12f)),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, GreenSuccess)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SECTORS ADVANCED: +${loot.floorsGained} (NOW SECTOR ${loot.finalFloor})",
                                style = TrueIdleType.tag,
                                color = GreenSuccess
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2x2 Telemetry Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Kills
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("HOSTILES CLEARED", style = TrueIdleType.meta, color = TextMuted)
                                Text("${loot.kills} KILLS", style = TrueIdleType.value, color = HpRed)
                            }
                        }

                        // Gold
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("CREDITS SECURED", style = TrueIdleType.meta, color = TextMuted)
                                Text("+${NumberFormatter.short(loot.goldEarned)} 🪙", style = TrueIdleType.value, color = SecondaryGold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // XP
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("EXPERIENCE", style = TrueIdleType.meta, color = TextMuted)
                                Text("+${NumberFormatter.short(loot.xpEarned)} XP", style = TrueIdleType.value, color = PrimaryNeon)
                            }
                        }

                        // Bones
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("BONE ESSENCE", style = TrueIdleType.meta, color = TextMuted)
                                Text("+${loot.bonesEarned} 💀", style = TrueIdleType.value, color = NecroPurple)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Items list
                if (loot.items.isNotEmpty() || loot.beastsTamed > 0) {
                    Text(
                        text = "ACQUIRED ASSETS",
                        style = TrueIdleType.tag,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        loot.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(item.name, style = TrueIdleType.body, color = TextLight)
                                Text("+${item.count}", style = TrueIdleType.value, color = GreenSuccess)
                            }
                        }
                        if (loot.beastsTamed > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Beasts Tamed", style = TrueIdleType.body, color = TextLight)
                                Text("+${loot.beastsTamed} 🐾", style = TrueIdleType.value, color = GreenSuccess)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Claim Button
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClaim()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("claim_offline_btn"),
                    shape = RectangleShape,
                    border = BorderStroke(1.5.dp, PrimaryNeon),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PrimaryNeon.copy(alpha = 0.2f),
                        contentColor = PrimaryNeon
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "CLAIM SALVAGE",
                        style = TrueIdleType.title,
                        color = PrimaryNeon
                    )
                }
            }
        }
    }
}
