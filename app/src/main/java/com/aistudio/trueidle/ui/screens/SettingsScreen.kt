package com.aistudio.trueidle.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showNotificationHistory by remember { mutableStateOf(false) }

    var exportedCode by remember { mutableStateOf("") }
    var importInputText by remember { mutableStateOf("") }
    var copyStatus by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf("") }

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
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(10.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Notification System & HUD Settings Card
        SciFiPanel(
            title = "NOTIFICATION PROTOCOLS",
            accent = PrimaryNeon,
            tag = if (state.notificationBannersEnabled) "BANNERS ON" else "BANNERS MUTED"
        ) {
            Text(
                text = "Configure floating combat and loot telemetry banners. Access complete chronological log history.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "FLOATING BANNERS",
                        style = TrueIdleType.label,
                        color = TextLight
                    )
                    Text(
                        text = if (state.notificationBannersEnabled) "Real-time popups active" else "Popups silenced (recorded in archive)",
                        style = TrueIdleType.meta,
                        color = if (state.notificationBannersEnabled) GreenSuccess else TextMuted
                    )
                }

                Switch(
                    checked = state.notificationBannersEnabled,
                    onCheckedChange = { onToggleNotifications() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PrimaryNeon,
                        checkedTrackColor = PrimaryNeon.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.testTag("notification_banner_switch")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // History Log Buttons Row
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

        // Data Management & Persistence Card
        SciFiPanel(
            title = "SAVE & DATA ARCHIVE",
            accent = SecondaryGold,
            tag = "PERSISTENCE"
        ) {
            Text(
                text = "Progression is automatically committed every 30s and on app lifecycle suspend. Supports export and import across devices.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

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

        // Achievements Card (§4.9)
        SciFiPanel(
            title = "ACHIEVEMENTS ARCHIVE",
            accent = SecondaryGold,
            tag = "${state.achievements.count { it.isClaimed }}/${state.achievements.size} CLAIMED"
        ) {
            state.achievements.forEach { ach ->
                val canClaim = ach.isUnlocked && !ach.isClaimed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
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
                        Text(
                            text = "REWARD: +${ach.rewardGold} 🪙",
                            style = TrueIdleType.meta,
                            color = SecondaryGold
                        )
                    }

                    if (ach.isClaimed) {
                        Box(
                            modifier = Modifier
                                .background(GreenSuccess.copy(alpha = 0.15f), RectangleShape)
                                .border(1.dp, GreenSuccess, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("CLAIMED", style = TrueIdleType.tag, color = GreenSuccess)
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
                            Text("CLAIM", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(SurfaceVariantDark, RectangleShape)
                                .border(1.dp, BorderDark, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${ach.progress}/${ach.target}",
                                style = TrueIdleType.meta,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Engine & System Telemetry
        SciFiPanel(
            title = "SYSTEM DIAGNOSTICS",
            accent = TextLight,
            tag = "TELEMETRY"
        ) {
            Text("• AUTO-SAVE: ACTIVE (ATOMIC DISK REPOSITORY)", style = TrueIdleType.meta, color = TextMuted)
            Text("• OFFLINE ACCUMULATOR: SYNCHRONIZED (MONOTONIC TIME)", style = TrueIdleType.meta, color = TextMuted)
            Text("• CORE CLOCK: 4 TICKS/SEC (250MS DETERMINISTIC LOOP)", style = TrueIdleType.meta, color = PrimaryNeon)
            Text("• BUILD REVISION: TRUE IDLE V1.0.0 (CYBERPUNK ENGINE)", style = TrueIdleType.meta, color = TextLight)
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
                Column(
                    modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)
                ) {
                    Text(
                        text = "EXPORT SAVE CODE",
                        style = TrueIdleType.title,
                        color = PrimaryNeon
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Copy the raw JSON archive below and save it to your clipboard:",
                        style = TrueIdleType.body,
                        color = TextMuted
                    )
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
                        Text(
                            text = copyStatus,
                            style = TrueIdleType.tag,
                            color = GreenSuccess
                        )
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
                Column(
                    modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)
                ) {
                    Text(
                        text = "IMPORT SAVE ARCHIVE",
                        style = TrueIdleType.title,
                        color = SecondaryGold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste a valid exported JSON save archive below:",
                        style = TrueIdleType.body,
                        color = TextMuted
                    )
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
                    Text(
                        text = "CRITICAL RESET PROTOCOL",
                        style = TrueIdleType.title,
                        color = HpRed
                    )
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
