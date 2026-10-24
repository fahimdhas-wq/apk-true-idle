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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.trueidle.model.OfflineLoot
import com.aistudio.trueidle.ui.theme.*

@Composable
fun OfflineLootDialog(
    loot: OfflineLoot,
    onClaim: () -> Unit
) {
    val hours = loot.timeAwaySeconds / 3600
    val minutes = (loot.timeAwaySeconds % 3600) / 60
    val seconds = loot.timeAwaySeconds % 60
    val timeFormatted = if (hours > 0) "${hours}h ${minutes}m ${seconds}s" else "${minutes}m ${seconds}s"

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
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("◈ ", color = PrimaryNeon, fontSize = 14.sp)
                            Text(
                                text = "OFFLINE PROGRESSION MATRIX",
                                style = MaterialTheme.typography.titleMedium,
                                color = PrimaryNeon,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Background automation executed while disconnected",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 9.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, PrimaryNeon, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Level-up banner if player leveled up during offline progress
                if (loot.playerLevelsGained > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = GreenSuccess.copy(alpha = 0.15f)),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, GreenSuccess)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡ ", fontSize = 12.sp)
                            Text(
                                text = "HERO PROMOTED: +${loot.playerLevelsGained} LEVEL(S) GAINED OFFLINE!",
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenSuccess,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 2x2 Telemetry Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Kills
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("HOSTILES CLEARED", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 8.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${loot.kills} KILLS", style = MaterialTheme.typography.bodyMedium, color = HpRed, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Gold
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("CREDITS SECURED", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 8.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+${loot.goldEarned} 🪙", style = MaterialTheme.typography.bodyMedium, color = SecondaryGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // XP
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("TACTICAL EXPERIENCE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 8.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+${loot.xpEarned} XP", style = MaterialTheme.typography.bodyMedium, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Bone Essence
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("BONE ESSENCE", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 8.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("+${loot.bonesEarned} 💀", style = MaterialTheme.typography.bodyMedium, color = NecroPurple, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Gathering & Extraction telemetry
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, BorderDark)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "AUTONOMOUS LOGISTICS & GATHERING",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (loot.farmingGainsSummary.isNotEmpty()) {
                            Text("🌿 ${loot.farmingGainsSummary}", style = MaterialTheme.typography.bodySmall, color = TextLight, fontSize = 9.sp)
                        }
                        if (loot.miningGainsSummary.isNotEmpty()) {
                            Text("⛏ ${loot.miningGainsSummary}", style = MaterialTheme.typography.bodySmall, color = TextLight, fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Itemized drops summary
                if (loot.dropsSummary.isNotEmpty()) {
                    Text(
                        text = "ITEMIZED SALVAGE MANIFEST",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        loot.dropsSummary.forEach { drop ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SurfaceVariantDark, RectangleShape)
                                    .border(1.dp, BorderDark, RectangleShape)
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = drop.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GreenSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Claim Button
                OutlinedButton(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth().testTag("claim_offline_btn"),
                    shape = RectangleShape,
                    border = BorderStroke(1.5.dp, PrimaryNeon),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PrimaryNeon.copy(alpha = 0.2f),
                        contentColor = PrimaryNeon
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = "TRANSFER SALVAGE TO ARSENAL ➔",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
