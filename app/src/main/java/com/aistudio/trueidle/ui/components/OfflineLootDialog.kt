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
                .padding(16.dp)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFLINE REPORT",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Automated protocols continued while you were disconnected. Salvage acquired:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariantDark)
                        .border(1.dp, BorderDark, RectangleShape)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("HOSTILES NEUTRALIZED", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("${loot.kills} KILLS", style = MaterialTheme.typography.bodySmall, color = TextLight, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CREDITS ACCUMULATED", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("+${loot.goldEarned} 🪙", style = MaterialTheme.typography.bodySmall, color = SecondaryGold, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TACTICAL EXPERIENCE", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("+${loot.xpEarned} XP", style = MaterialTheme.typography.bodySmall, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("BONE ESSENCE HARVESTED", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text("+${loot.bonesEarned} 💀", style = MaterialTheme.typography.bodySmall, color = NecroPurple, fontWeight = FontWeight.Bold)
                    }
                    if (loot.dropsSummary.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RESOURCE RECOVERY", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Text(loot.dropsSummary.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = GreenSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        text = "TRANSFER TO INVENTORY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
