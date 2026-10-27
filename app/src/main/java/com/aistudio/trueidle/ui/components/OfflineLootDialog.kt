package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.OfflineLoot
import com.aistudio.trueidle.ui.theme.*

@Composable
fun OfflineLootDialog(
    loot: OfflineLoot,
    onClaim: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val timeFormatted = NumberFormatter.duration(loot.timeAwaySeconds)
    val efficiencyPercent = NumberFormatter.percent(loot.efficiency)
    val scrollState = rememberScrollState()

    // Back / outside tap must not silently bank the rewards: only the explicit claim button does.
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("offline_loot_dialog"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape,
            border = BorderStroke(1.5.dp, PrimaryNeon)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(11.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OFFLINE RECON REPORT",
                            style = TrueIdleType.titleLarge,
                            color = PrimaryNeon
                        )
                        Text(
                            text = "AUTONOMOUS STASIS SYSTEM · $efficiencyPercent SYNC",
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(if (loot.capApplied) Warn.copy(alpha = 0.15f) else PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, if (loot.capApplied) Warn else PrimaryNeon, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (loot.capApplied) "8H RECON CAP ($timeFormatted)" else timeFormatted,
                            style = TrueIdleType.numberSmall,
                            color = if (loot.capApplied) Warn else PrimaryNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sector progression banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (loot.floorsGained > 0) GreenSuccess.copy(alpha = 0.12f) else SurfaceVariantDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (loot.floorsGained > 0) GreenSuccess else BorderDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECTOR TRAVERSAL: SECTOR ${loot.startFloor} ➔ ${loot.finalFloor}",
                            style = TrueIdleType.tag,
                            color = if (loot.floorsGained > 0) GreenSuccess else TextLight
                        )
                        if (loot.floorsGained > 0) {
                            Text(
                                text = "+${loot.floorsGained} ADVANCED",
                                style = TrueIdleType.numberSmall,
                                color = GreenSuccess
                            )
                        } else {
                            Text(
                                text = "DEFENDED",
                                style = TrueIdleType.tag,
                                color = PrimaryNeon
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2x2 Telemetry Grid with Hourly Rates in Orbitron
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Kills
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("HOSTILES ELIMINATED", style = TrueIdleType.meta, color = TextMuted)
                                Text("${loot.kills} KILLS", style = TrueIdleType.numberStat, color = HpRed)
                                if (loot.killsPerHour > 0) {
                                    Text("${loot.killsPerHour}/hr rate", style = TrueIdleType.monoTimestamp, color = TextMuted)
                                }
                            }
                        }

                        // Gold
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("CREDITS RECOVERED", style = TrueIdleType.meta, color = TextMuted)
                                Text("+${NumberFormatter.short(loot.goldEarned)} 🪙", style = TrueIdleType.numberStat, color = SecondaryGold)
                                if (loot.goldPerHour > 0) {
                                    Text("${NumberFormatter.short(loot.goldPerHour)}/hr rate", style = TrueIdleType.monoTimestamp, color = TextMuted)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // XP
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("COMBAT EXPERIENCE", style = TrueIdleType.meta, color = TextMuted)
                                Text("+${NumberFormatter.short(loot.xpEarned)} XP", style = TrueIdleType.numberStat, color = PrimaryNeon)
                                if (loot.xpPerHour > 0) {
                                    Text("${NumberFormatter.short(loot.xpPerHour)}/hr rate", style = TrueIdleType.monoTimestamp, color = TextMuted)
                                }
                            }
                        }

                        // Bones
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(SurfaceDark)
                                .border(1.dp, BorderDark)
                                .padding(6.dp)
                        ) {
                            Column {
                                Text("BONE ESSENCE", style = TrueIdleType.meta, color = TextMuted)
                                Text("+${loot.bonesEarned} 💀", style = TrueIdleType.numberStat, color = NecroPurple)
                                Text("Reanimation salvage", style = TrueIdleType.monoTimestamp, color = TextMuted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Chronological 8-Hour Activity Timeline Log
                if (loot.timelineLog.isNotEmpty()) {
                    Text(
                        text = "ACTIVITY LOG",
                        style = TrueIdleType.tag,
                        color = PrimaryNeon
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        loot.timelineLog.forEach { entry ->
                            Text(
                                text = "› $entry",
                                style = TrueIdleType.meta,
                                color = TextLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Acquired Items List
                if (loot.items.isNotEmpty() || loot.beastsTamed > 0) {
                    Text(
                        text = "SALVAGED RESOURCE INVENTORY",
                        style = TrueIdleType.tag,
                        color = SecondaryGold
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark)
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        loot.items.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("◈ ${item.name}", style = TrueIdleType.tag, color = TextLight)
                                Text("+${item.count}", style = TrueIdleType.numberSmall, color = GreenSuccess)
                            }
                        }
                        if (loot.beastsTamed > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("◈ COMPANION BEASTS TAMED", style = TrueIdleType.tag, color = TextLight)
                                Text("+${loot.beastsTamed} 🐾", style = TrueIdleType.numberSmall, color = GreenSuccess)
                            }
                        }
                        if (loot.talentPointsGained > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("◈ TALENT POINTS EARNED", style = TrueIdleType.tag, color = TextLight)
                                Text("+${loot.talentPointsGained} TP", style = TrueIdleType.numberSmall, color = PrimaryNeon)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

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
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "CLAIM SALVAGE & RESUME MISSION",
                        style = TrueIdleType.title,
                        color = PrimaryNeon
                    )
                }
            }
        }
    }
}
