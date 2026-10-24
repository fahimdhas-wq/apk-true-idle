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
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.NotificationHistoryDialog
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SettingsScreen(
    state: GameState,
    onExportSave: () -> String,
    onImportSave: (String) -> Boolean,
    onResetGame: () -> Unit,
    onToggleNotifications: () -> Unit,
    onClearNotificationHistory: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Notification System & HUD Settings Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("notifications_settings_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "NOTIFICATION & TELEMETRY PROTOCOLS",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryNeon,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure live floating banners and access complete historical progression logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 9.sp
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "REAL-TIME POPUP BANNERS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.notificationBannersEnabled) "Floating heads-up notifications active" else "Notifications muted (recorded to archive only)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 8.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onToggleNotifications,
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (state.notificationBannersEnabled) GreenSuccess else BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (state.notificationBannersEnabled) GreenSuccess.copy(alpha = 0.15f) else SurfaceVariantDark,
                            contentColor = if (state.notificationBannersEnabled) GreenSuccess else TextMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("notif_toggle_btn")
                    ) {
                        Text(
                            text = if (state.notificationBannersEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Show All Notification History Action
                OutlinedButton(
                    onClick = { showNotificationHistory = true },
                    modifier = Modifier.fillMaxWidth().testTag("open_history_btn"),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, PrimaryNeon),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = PrimaryNeon.copy(alpha = 0.12f),
                        contentColor = PrimaryNeon
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "SHOW ALL NOTIFICATIONS (${state.notificationHistory.size} IN ARCHIVE) ➔",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }

        // Save Management & Cloud Archive Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("save_management_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "DATA MANAGEMENT & BACKUP",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Export and import your game save archive at any time to transfer progress across devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    fontSize = 9.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        Text("EXPORT SAVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                        Text("IMPORT SAVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                    Text("RESET PROTOCOL (START FRESH)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Achievements Card
        Card(
            modifier = Modifier.fillMaxWidth().testTag("achievements_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, SecondaryGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ACHIEVEMENTS DATABASE",
                    style = MaterialTheme.typography.titleMedium,
                    color = SecondaryGold,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                state.achievements.forEach { ach ->
                    val isCompleted = ach.isUnlocked || ach.progress >= ach.target
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.title.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCompleted) SecondaryGold else TextLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = ach.desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isCompleted) GreenSuccess.copy(alpha = 0.15f) else SurfaceVariantDark, RectangleShape)
                                .border(1.dp, if (isCompleted) GreenSuccess else BorderDark, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isCompleted) "CLAIMED" else "${ach.progress}/${ach.target}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCompleted) GreenSuccess else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }

        // Engine & System Telemetry
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ENGINE & SYSTEM TELEMETRY",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text("• AUTO-SAVE: ACTIVE (LOCAL PERSISTENCE ENGINE)", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 9.sp)
                Text("• OFFLINE ACCUMULATOR: ENABLED (SIMULATES BACKGROUND TICKS)", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 9.sp)
                Text("• CORE CLOCK: 4 TICKS/SEC SYNCHRONIZED", style = MaterialTheme.typography.bodySmall, color = PrimaryNeon, fontSize = 9.sp)
                Text("• BUILD REVISION: TRUE IDLE V1.0.0 (CYBERPUNK ENGINE)", style = MaterialTheme.typography.bodySmall, color = TextLight, fontSize = 9.sp)
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
                Column(
                    modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)
                ) {
                    Text(
                        text = "EXPORT SAVE CODE",
                        style = MaterialTheme.typography.titleLarge,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Copy the raw JSON archive below and save it to your clipboard or notes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(SurfaceVariantDark, RectangleShape)
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = exportedCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLight,
                            fontSize = 9.sp,
                            maxLines = 15
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("TrueIdleSave", exportedCode)
                                clipboard.setPrimaryClip(clip)
                                copyStatus = "COPIED TO CLIPBOARD!"
                            },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, PrimaryNeon),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = PrimaryNeon.copy(alpha = 0.2f),
                                contentColor = PrimaryNeon
                            )
                        ) {
                            Text(if (copyStatus.isEmpty()) "COPY CODE" else copyStatus, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showExportDialog = false },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                        ) {
                            Text("CLOSE", style = MaterialTheme.typography.labelSmall)
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
                        text = "IMPORT SAVE CODE",
                        style = MaterialTheme.typography.titleLarge,
                        color = SecondaryGold,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Paste your JSON save code below to restore your progress:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RectangleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark,
                            focusedBorderColor = SecondaryGold,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        placeholder = { Text("Paste JSON save code here...", fontSize = 11.sp, color = TextMuted) }
                    )

                    if (importError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = importError, color = HpRed, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (importInputText.trim().isEmpty()) {
                                    importError = "Error: Input cannot be empty"
                                } else {
                                    val success = onImportSave(importInputText.trim())
                                    if (success) {
                                        showImportDialog = false
                                    } else {
                                        importError = "Failed to parse save JSON archive"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, SecondaryGold),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SecondaryGold.copy(alpha = 0.2f),
                                contentColor = SecondaryGold
                            )
                        ) {
                            Text("RESTORE SAVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = false },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                        ) {
                            Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.5.dp, HpRed)
            ) {
                Column(
                    modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)
                ) {
                    Text(
                        text = "CONFIRM FACTORY RESET",
                        style = MaterialTheme.typography.titleLarge,
                        color = HpRed,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to reset all game progress? This will reset all floor progress, player levels, and inventory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onResetGame()
                                showResetDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, HpRed),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = HpRed.copy(alpha = 0.2f),
                                contentColor = HpRed
                            )
                        ) {
                            Text("WIPE & RESET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showResetDialog = false },
                            shape = RectangleShape,
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                        ) {
                            Text("CANCEL", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
