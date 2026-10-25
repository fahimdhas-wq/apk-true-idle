package com.aistudio.trueidle.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.trueidle.core.DerivedStats
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.NotificationHistoryDialog
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SettingsScreen(
    state: GameState,
    onExportSave: () -> String,
    onImportSave: (String) -> Boolean,
    onResetGame: () -> Unit,
    onToggleNotifications: () -> Unit,
    onClearNotificationHistory: () -> Unit,
    onClaimAchievement: ((String) -> Unit)? = null,
    onToggleAutoEquip: () -> Unit = {},
    onToggleAutoMerge: () -> Unit = {},
    onSetCraftUsage: (Int) -> Unit = {},
    onFightPromotionBoss: () -> Unit = {},
    onSetThemeMode: (Boolean) -> Unit = {},
    onExportTelemetryLog: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val d = DerivedStats.of(state)
    val promo = state.promotion
    val auto = state.automation

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showNotificationHistory by remember { mutableStateOf(false) }

    var exportedCode by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    var copyStatus by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf("") }
    var telemetryCopyFeedback by remember { mutableStateOf("") }

    val floorMet = state.player.currentFloor >= promo.requiredFloor
    val gearMet = d.gearScore >= promo.requiredGearScore
    val canPromote = floorMet && gearMet

    if (showNotificationHistory) {
        NotificationHistoryDialog(
            history = state.notificationHistory,
            onClear = onClearNotificationHistory,
            onDismiss = { showNotificationHistory = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(10.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. VISUAL THEME PROTOCOL (White / Dark mode selection)
        SciFiPanel(
            title = "INTERFACE PROTOCOL",
            accent = PrimaryNeon,
            tag = if (state.isDarkMode) "OBSIDIAN DARK" else "SOLAR LIGHT"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RectangleShape)
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "VISUAL ENVIRONMENT SELECTION",
                    style = TrueIdleType.label,
                    color = TextLight
                )
                Text(
                    text = "Select high-contrast cyberpunk Obsidian dark mode or Solar laboratory light mode.",
                    style = TrueIdleType.meta,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Dark Mode Button
                    OutlinedButton(
                        onClick = { onSetThemeMode(true) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_dark_button"),
                        shape = RectangleShape,
                        border = BorderStroke(
                            1.5.dp,
                            if (state.isDarkMode) PrimaryNeon else BorderDark
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (state.isDarkMode) PrimaryNeon.copy(alpha = 0.2f) else SurfaceVariantDark,
                            contentColor = if (state.isDarkMode) PrimaryNeon else TextMuted
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☾ OBSIDIAN DARK", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                            if (state.isDarkMode) {
                                Text("[✓ ACTIVE]", style = TrueIdleType.monoTimestamp, color = PrimaryNeon)
                            }
                        }
                    }

                    // Light Mode Button
                    OutlinedButton(
                        onClick = { onSetThemeMode(false) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("theme_light_button"),
                        shape = RectangleShape,
                        border = BorderStroke(
                            1.5.dp,
                            if (!state.isDarkMode) SecondaryGold else BorderDark
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!state.isDarkMode) SecondaryGold.copy(alpha = 0.2f) else SurfaceVariantDark,
                            contentColor = if (!state.isDarkMode) SecondaryGold else TextMuted
                        ),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("☀ SOLAR LIGHT", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                            if (!state.isDarkMode) {
                                Text("[✓ ACTIVE]", style = TrueIdleType.monoTimestamp, color = SecondaryGold)
                            }
                        }
                    }
                }
            }
        }

        // 2. MISSION DOSSIER & PLAY TELEMETRY
        SciFiPanel(
            title = "MISSION DOSSIER",
            accent = SecondaryGold,
            tag = "PLAY RECORD"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 2x3 Statistics Matrix
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierTile("PLAY TIME", NumberFormatter.duration(state.lifetime.playTimeSeconds), SecondaryGold, Modifier.weight(1f))
                    DossierTile("TOTAL ELIMINATIONS", state.lifetime.totalKills.toString(), HpRed, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierTile("CURRENT SECTOR", "SECTOR ${state.player.currentFloor}", PrimaryNeon, Modifier.weight(1f))
                    DossierTile("GEAR SCORE", "${d.gearScore} GS", SecondaryGold, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DossierTile("ORES MINED", state.lifetime.oresMined.toString(), SecondaryGold, Modifier.weight(1f))
                    DossierTile("HERBS HARVESTED", state.lifetime.herbsHarvested.toString(), GreenSuccess, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Telemetry Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val report = onExportTelemetryLog()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("TelemetryReport", report))
                            telemetryCopyFeedback = "COPIED TO CLIPBOARD!"
                            Toast.makeText(context, "Full Mission Telemetry Copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_telemetry_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, PrimaryNeon),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PrimaryNeon.copy(alpha = 0.15f),
                            contentColor = PrimaryNeon
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text(
                            text = if (telemetryCopyFeedback.isNotEmpty()) telemetryCopyFeedback else "📋 EXPORT LOG",
                            style = TrueIdleType.tag
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val report = onExportTelemetryLog()
                            try {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, report)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Tactical Telemetry Log"))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_telemetry_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, SecondaryGold),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SecondaryGold.copy(alpha = 0.15f),
                            contentColor = SecondaryGold
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("↗ SHARE LOG", style = TrueIdleType.tag)
                    }
                }
            }
        }

        // 3. OFFLINE SYSTEM & SIMULATION
        SciFiPanel(
            title = "OFFLINE PROGRESSION",
            accent = GreenSuccess,
            tag = "8H RECON CAP"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AUTONOMOUS STASIS RECON", style = TrueIdleType.label, color = TextLight)
                        Text("Accumulates up to 8 hours of rewards while offline (65% efficiency rate)", style = TrueIdleType.meta, color = TextMuted)
                    }
                    Box(
                        modifier = Modifier
                            .background(GreenSuccess.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, GreenSuccess, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("8H ACTIVE", style = TrueIdleType.numberSmall, color = GreenSuccess)
                    }
                }
            }
        }

        // 4. NOTIFICATION PROTOCOLS (Verified master toggle)
        SciFiPanel(
            title = "NOTIFICATION PROTOCOLS",
            accent = PrimaryNeon,
            tag = if (state.notificationBannersEnabled) "BANNERS ON" else "BANNERS MUTED"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(1.dp, BorderDark)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FLOATING TELEMETRY BANNERS", style = TrueIdleType.label, color = TextLight)
                        Text(
                            text = if (state.notificationBannersEnabled) {
                                "Milestone popups active (Level ups, Floor clears, Retracts)"
                            } else {
                                "Popups silenced completely (all events logged to Archive & Mission Log)"
                            },
                            style = TrueIdleType.meta,
                            color = if (state.notificationBannersEnabled) GreenSuccess else TextMuted
                        )
                    }

                    Switch(
                        checked = state.notificationBannersEnabled,
                        onCheckedChange = { onToggleNotifications() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PrimaryNeon,
                            checkedTrackColor = PrimaryNeon.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceVariantDark
                        ),
                        modifier = Modifier.testTag("notification_toggle_switch")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showNotificationHistory = true },
                        modifier = Modifier.weight(1f).testTag("view_history_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, PrimaryNeon),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PrimaryNeon.copy(alpha = 0.12f),
                            contentColor = PrimaryNeon
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("ARCHIVE (${state.notificationHistory.size})", style = TrueIdleType.tag)
                    }

                    OutlinedButton(
                        onClick = { onClearNotificationHistory() },
                        modifier = Modifier.weight(1f).testTag("clear_history_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceDark,
                            contentColor = TextMuted
                        ),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("CLEAR ARCHIVE", style = TrueIdleType.tag)
                    }
                }
            }
        }

        // 5. PROMOTION SYSTEM CARD
        SciFiPanel(
            title = "PROMOTION",
            accent = SecondaryGold,
            tag = "TIER ${promo.tier} / ${promo.maxTier}"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RectangleShape)
                    .border(BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f)))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = promo.tierName.uppercase(),
                    style = TrueIdleType.titleLarge,
                    color = SecondaryGold
                )
                Text(
                    text = "CURRENT PROMOTION TIER (+${promo.bonusPercent}% ALL STATS BONUS)",
                    style = TrueIdleType.meta,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Requirements Grid (Floor & Gear Score)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("FLOOR", style = TrueIdleType.meta, color = TextMuted)
                            Text(
                                text = "${state.player.currentFloor} / ${promo.requiredFloor}",
                                style = TrueIdleType.numberSmall,
                                color = if (floorMet) GreenSuccess else PrimaryNeon
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val floorProgress = (state.player.currentFloor.toFloat() / promo.requiredFloor.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(SurfaceVariantDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(floorProgress)
                                    .background(if (floorMet) GreenSuccess else PrimaryNeon)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GEAR SCORE", style = TrueIdleType.meta, color = TextMuted)
                            Text(
                                text = "${d.gearScore} / ${promo.requiredGearScore}",
                                style = TrueIdleType.numberSmall,
                                color = if (gearMet) GreenSuccess else SecondaryGold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val gearProgress = (d.gearScore.toFloat() / promo.requiredGearScore.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(SurfaceVariantDark)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(gearProgress)
                                    .background(if (gearMet) GreenSuccess else SecondaryGold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onFightPromotionBoss,
                    enabled = canPromote,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryGold,
                        contentColor = Color(0xFF000000),
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("fight_promotion_boss_btn")
                ) {
                    Text(
                        text = if (canPromote) "⚔ FIGHT PROMOTION BOSS" else "LOCKED (REQUIREMENTS UNMET)",
                        style = TrueIdleType.tag,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. AUTOMATION PROTOCOLS
        SciFiPanel(
            title = "AUTOMATION PROTOCOLS",
            accent = PrimaryNeon,
            tag = "ARCANA"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUTO-EQUIP BEST GEAR", style = TrueIdleType.label, color = TextLight)
                    Text("Equip highest-tier gear automatically on drop", style = TrueIdleType.meta, color = TextMuted)
                }
                Switch(
                    checked = auto.autoEquipBestGear,
                    onCheckedChange = { onToggleAutoEquip() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryNeon,
                        checkedTrackColor = PrimaryNeon.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.testTag("auto_equip_switch")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AUTO-MERGE BEASTS", style = TrueIdleType.label, color = TextLight)
                    Text("Merge identical companions automatically", style = TrueIdleType.meta, color = TextMuted)
                }
                Switch(
                    checked = auto.autoMergeBeasts,
                    onCheckedChange = { onToggleAutoMerge() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryNeon,
                        checkedTrackColor = PrimaryNeon.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.testTag("auto_merge_switch")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CRAFT MATERIAL USAGE", style = TrueIdleType.label, color = TextLight)
                        Text("Max % of materials auto-crafters may use", style = TrueIdleType.meta, color = TextMuted)
                    }
                    Text("${auto.craftMaterialUsagePct}%", style = TrueIdleType.value, color = PrimaryNeon)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Slider(
                    value = auto.craftMaterialUsagePct.toFloat(),
                    onValueChange = { onSetCraftUsage(it.toInt()) },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryNeon,
                        activeTrackColor = PrimaryNeon,
                        inactiveTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 7. ACHIEVEMENTS ARCHIVE
        val claimedCount = state.achievements.count { it.isClaimed }
        SciFiPanel(
            title = "ACHIEVEMENTS",
            accent = SecondaryGold,
            tag = "$claimedCount / ${state.achievements.size} CLAIMED"
        ) {
            state.achievements.forEach { ach ->
                val canClaim = ach.isUnlocked && !ach.isClaimed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Color(0xFF000000))
                        .border(1.dp, BorderDark)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ach.title.uppercase(),
                            style = TrueIdleType.label,
                            color = if (ach.isClaimed) SecondaryGold else TextLight
                        )
                        Text(
                            text = ach.desc,
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )
                    }

                    if (ach.isClaimed) {
                        Box(
                            modifier = Modifier
                                .background(GreenSuccess.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, GreenSuccess, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("✓ CLAIMED", style = TrueIdleType.tag, color = GreenSuccess)
                        }
                    } else if (canClaim) {
                        Button(
                            onClick = { onClaimAchievement?.invoke(ach.id) },
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryGold,
                                contentColor = Color(0xFF000000)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("claim_ach_${ach.id}")
                        ) {
                            Text("CLAIM (+${ach.rewardGold} 🪙)", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(SurfaceVariantDark, RectangleShape)
                                .border(1.dp, BorderDark, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LOCKED (${ach.progress}/${ach.target})",
                                style = TrueIdleType.meta,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // 8. DATA PERSISTENCE & STORAGE
        SciFiPanel(
            title = "DATA PERSISTENCE",
            accent = SecondaryGold,
            tag = "STORAGE"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        exportedCode = onExportSave()
                        copyStatus = ""
                        showExportDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("export_save_btn"),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, PrimaryNeon),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PrimaryNeon.copy(alpha = 0.15f),
                        contentColor = PrimaryNeon
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("EXPORT SAVE", style = TrueIdleType.tag)
                }

                OutlinedButton(
                    onClick = {
                        importInputText = ""
                        importError = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("import_save_btn"),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, SecondaryGold),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SecondaryGold.copy(alpha = 0.15f),
                        contentColor = SecondaryGold
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text("IMPORT SAVE", style = TrueIdleType.tag)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("reset_game_btn"),
                shape = RectangleShape,
                border = BorderStroke(1.dp, HpRed),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HpRed.copy(alpha = 0.12f),
                    contentColor = HpRed
                ),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Text("RESET PROTOCOL (START FRESH)", style = TrueIdleType.tag)
            }
        }
    }

    // Export Save Dialog
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.5.dp, PrimaryNeon)
            ) {
                Column(modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)) {
                    Text("EXPORT SAVE CODE", style = TrueIdleType.title, color = PrimaryNeon)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Copy the raw JSON archive below and save it to your clipboard:", style = TrueIdleType.body, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = exportedCode,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("export_text_field"),
                        textStyle = TrueIdleType.meta,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryNeon,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (copyStatus.isNotEmpty()) {
                        Text(text = copyStatus, style = TrueIdleType.tag, color = GreenSuccess)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("TrueIdleSave", exportedCode)
                                clipboard.setPrimaryClip(clip)
                                copyStatus = "SAVE CODE COPIED TO CLIPBOARD!"
                            },
                            modifier = Modifier.weight(1f).testTag("copy_save_clipboard_btn"),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon, contentColor = Color(0xFF000000))
                        ) {
                            Text("COPY", style = TrueIdleType.tag)
                        }

                        OutlinedButton(
                            onClick = { showExportDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                        ) {
                            Text("CLOSE", style = TrueIdleType.tag)
                        }
                    }
                }
            }
        }
    }

    // Import Save Dialog
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.5.dp, SecondaryGold)
            ) {
                Column(modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)) {
                    Text("IMPORT SAVE ARCHIVE", style = TrueIdleType.title, color = SecondaryGold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Paste a valid exported JSON save archive below:", style = TrueIdleType.body, color = TextMuted)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            importError = ""
                        },
                        placeholder = { Text("Paste JSON code here...", style = TrueIdleType.body, color = TextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("import_text_field"),
                        textStyle = TrueIdleType.meta,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryGold,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (importError.isNotEmpty()) {
                        Text(text = importError, color = HpRed, style = TrueIdleType.tag)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (importInputText.isBlank()) {
                                    importError = "ERR: INPUT ARCHIVE IS EMPTY."
                                } else {
                                    val success = onImportSave(importInputText.trim())
                                    if (success) {
                                        showImportDialog = false
                                    } else {
                                        importError = "ERR: INVALID SAVE FORMAT OR CORRUPT CRC."
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_import_btn"),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold, contentColor = Color(0xFF000000))
                        ) {
                            Text("RESTORE", style = TrueIdleType.tag)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                        ) {
                            Text("CANCEL", style = TrueIdleType.tag)
                        }
                    }
                }
            }
        }
    }

    // Reset Confirm Dialog
    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(2.dp, HpRed)
            ) {
                Column(
                    modifier = Modifier.background(Color(0xFF000000)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CRITICAL RESET PROTOCOL", style = TrueIdleType.title, color = HpRed)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "WARNING: This irreversible action will wipe all active floor progression, inventory, trained dojo ranks, tamed companions, and undead legions.",
                        style = TrueIdleType.body,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onResetGame()
                                showResetDialog = false
                            },
                            modifier = Modifier.weight(1f).testTag("confirm_reset_btn"),
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = HpRed, contentColor = Color(0xFFFFFFFF))
                        ) {
                            Text("PURGE ALL DATA", style = TrueIdleType.tag)
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight)
                        ) {
                            Text("ABORT", style = TrueIdleType.tag)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DossierTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(SurfaceDark)
            .border(1.dp, BorderDark)
            .padding(8.dp)
    ) {
        Column {
            Text(label, style = TrueIdleType.meta, color = TextMuted)
            Text(value, style = TrueIdleType.numberStat, color = color)
        }
    }
}
