package com.aistudio.trueidle.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.BackgroundCost
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.runtime.ResourceProbe
import com.aistudio.trueidle.runtime.ResourceSample
import com.aistudio.trueidle.ui.state.LocalDerived
import com.aistudio.trueidle.ui.components.SciFiButton
import com.aistudio.trueidle.ui.components.SciFiDialog
import com.aistudio.trueidle.ui.components.SciFiGauge
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.rememberTierColorForOrdinal
import com.aistudio.trueidle.ui.theme.BorderDark
import com.aistudio.trueidle.ui.theme.GreenSuccess
import com.aistudio.trueidle.ui.theme.HpRed
import com.aistudio.trueidle.ui.theme.PrimaryNeon
import com.aistudio.trueidle.ui.theme.SecondaryGold
import com.aistudio.trueidle.ui.theme.SurfaceVariantDark
import com.aistudio.trueidle.ui.theme.TextLight
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType
import com.aistudio.trueidle.ui.theme.Warn
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SettingsScreen(
    state: GameState,
    onExportSave: () -> String,
    onImportSave: (String) -> Boolean,
    onResetGame: () -> Unit,
    onClaimAchievement: ((String) -> Unit)? = null,
    onToggleAutoEquip: () -> Unit = {},
    onToggleAutoMerge: () -> Unit = {},
    onToggleRunInBackground: () -> Unit = {},
    onSetCraftUsage: (Int) -> Unit = {},
    onFightPromotionBoss: () -> Unit = {},
    onExportTelemetryLog: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val d = LocalDerived.current
    val promo = state.promotion
    val auto = state.automation

    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showBackgroundConfirm by rememberSaveable { mutableStateOf(false) }
    var exportedCode by rememberSaveable { mutableStateOf("") }
    var importInputText by rememberSaveable { mutableStateOf("") }
    var copyStatus by rememberSaveable { mutableStateOf("") }
    var importError by rememberSaveable { mutableStateOf("") }

    var resourceSample by remember { mutableStateOf<ResourceSample?>(null) }
    LaunchedEffect(Unit) {
        val probe = ResourceProbe()
        while (true) {
            delay(2_000L)
            resourceSample = probe.sample()
        }
    }

    val floorMet = state.player.currentFloor >= promo.requiredFloor
    val gearMet = d.gearScore >= promo.requiredGearScore
    val canPromote = floorMet && gearMet

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ---- 1. MISSION DOSSIER
        SciFiPanel(title = "MISSION DOSSIER", accent = SecondaryGold, tag = "PLAY RECORD") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCell("PLAY TIME", NumberFormatter.duration(state.lifetime.playTimeSeconds), SecondaryGold, Modifier.weight(1f))
                    StatCell("ELIMINATIONS", NumberFormatter.short(state.lifetime.totalKills), HpRed, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCell("CURRENT SECTOR", "SECTOR ${state.player.currentFloor}", PrimaryNeon, Modifier.weight(1f))
                    StatCell("GEAR SCORE", "${d.gearScore} GS", SecondaryGold, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatCell("ORES MINED", NumberFormatter.short(state.lifetime.oresMined), SecondaryGold, Modifier.weight(1f))
                    StatCell("HERBS HARVESTED", NumberFormatter.short(state.lifetime.herbsHarvested), GreenSuccess, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SciFiButton(
                        label = "COPY LOG",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("TelemetryReport", onExportTelemetryLog()))
                            Toast.makeText(context, "Mission telemetry copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("export_telemetry_btn"),
                        color = PrimaryNeon
                    )
                    SciFiButton(
                        label = "SHARE LOG",
                        onClick = {
                            runCatching {
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, onExportTelemetryLog())
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(send, "Share telemetry log"))
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("share_telemetry_btn"),
                        color = SecondaryGold
                    )
                }
            }
        }

        // ---- 2. PROMOTION
        SciFiPanel(title = "PROMOTION", accent = SecondaryGold, tag = "TIER ${promo.tier} / ${promo.maxTier}") {
            Text(
                text = promo.tierName.uppercase(),
                style = TrueIdleType.titleLarge,
                color = rememberTierColorForOrdinal(promo.tier)
            )
            Text(
                text = "+${promo.bonusPercent}% ALL STATS",
                style = TrueIdleType.meta,
                color = TextMuted
            )
            Spacer(Modifier.height(8.dp))
            SciFiGauge(
                label = "SECTOR REQUIREMENT",
                valueText = "${state.player.currentFloor} / ${promo.requiredFloor}",
                ratio = (state.player.currentFloor.toFloat() / promo.requiredFloor.coerceAtLeast(1)).coerceIn(0f, 1f),
                color = if (floorMet) GreenSuccess else PrimaryNeon
            )
            Spacer(Modifier.height(6.dp))
            SciFiGauge(
                label = "GEAR SCORE REQUIREMENT",
                valueText = "${d.gearScore} / ${promo.requiredGearScore}",
                ratio = (d.gearScore.toFloat() / promo.requiredGearScore.coerceAtLeast(1)).coerceIn(0f, 1f),
                color = if (gearMet) GreenSuccess else SecondaryGold
            )
            Spacer(Modifier.height(10.dp))
            SciFiButton(
                label = if (canPromote) "ENGAGE PROMOTION BOSS" else "LOCKED: REQUIREMENTS UNMET",
                onClick = onFightPromotionBoss,
                enabled = canPromote,
                filled = true,
                color = SecondaryGold,
                modifier = Modifier.fillMaxWidth().testTag("fight_promotion_boss_btn")
            )
        }

        // ---- 3. AUTOMATION
        SciFiPanel(title = "AUTOMATION", accent = PrimaryNeon, tag = "PROTOCOLS") {
            SettingSwitchRow(
                title = "AUTO-EQUIP BEST GEAR",
                subtitle = "Equip the highest-tier gear as it drops",
                checked = auto.autoEquipBestGear,
                accent = PrimaryNeon,
                onChange = { onToggleAutoEquip() },
                tag = "auto_equip_switch"
            )
            RowDivider()
            SettingSwitchRow(
                title = "AUTO-MERGE BEASTS",
                subtitle = "Merge identical companions automatically",
                checked = auto.autoMergeBeasts,
                accent = PrimaryNeon,
                onChange = { onToggleAutoMerge() },
                tag = "auto_merge_switch"
            )
            RowDivider()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("CRAFT MATERIAL USAGE", style = TrueIdleType.label, color = TextLight)
                    Text("Max share of materials auto-crafters may use", style = TrueIdleType.meta, color = TextMuted)
                }
                Text("${auto.craftMaterialUsagePct}%", style = TrueIdleType.value, color = PrimaryNeon)
            }
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
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            )
        }

        // ---- 4. BACKGROUND EXECUTION
        SciFiPanel(
            title = "BACKGROUND EXECUTION",
            accent = Warn,
            tag = if (auto.runInBackground) "ACTIVE" else "OFF"
        ) {
            SettingSwitchRow(
                title = "RUN GAME IN BACKGROUND",
                subtitle = "Keep fighting live while the app is off screen",
                checked = auto.runInBackground,
                accent = Warn,
                onChange = { enable -> if (enable) showBackgroundConfirm = true else onToggleRunInBackground() },
                tag = "run_in_background_switch"
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "CAUTION: uses battery, RAM and CPU while running and shows a persistent " +
                    "notification. Time the device sleeps or the system freezes the app is paid as an " +
                    "offline report (${GameCaps.OFFLINE_EFFICIENCY_PCT}% rate). Swiping the app away stops it.",
                style = TrueIdleType.meta,
                color = Warn
            )
            Spacer(Modifier.height(8.dp))
            val sample = resourceSample
            val battery = BackgroundCost.batteryPctPerHour(sample?.cpuPercent ?: 0.0)
            ValueRow("CPU (MEASURED, 1 CORE)", sample?.let { String.format(Locale.ROOT, "%.1f%%", it.cpuPercent) } ?: "SAMPLING")
            ValueRow("RAM (MEASURED, PSS)", sample?.let { String.format(Locale.ROOT, "%.0f MB", it.ramMb) } ?: "SAMPLING")
            ValueRow(
                "BATTERY (ESTIMATE)",
                String.format(Locale.ROOT, "%.1f-%.1f %%/HR", battery.lowPctPerHour, battery.highPctPerHour)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "CPU and RAM are read from this app. Battery is derived from measured CPU against an " +
                    "assumed ${BackgroundCost.ASSUMED_BATTERY_MAH.toInt()} mAh battery and varies by device.",
                style = TrueIdleType.meta,
                color = TextMuted
            )
        }

        // ---- 5. ACHIEVEMENTS
        val claimedCount = state.achievements.count { it.isClaimed }
        SciFiPanel(
            title = "ACHIEVEMENTS",
            accent = SecondaryGold,
            tag = "$claimedCount / ${state.achievements.size} CLAIMED"
        ) {
            state.achievements.forEachIndexed { index, ach ->
                if (index > 0) RowDivider()
                val canClaim = ach.isUnlocked && !ach.isClaimed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = ach.title.uppercase(),
                            style = TrueIdleType.label,
                            color = if (ach.isClaimed) SecondaryGold else TextLight
                        )
                        Text(ach.desc, style = TrueIdleType.meta, color = TextMuted)
                        Text(
                            text = "REWARD +${NumberFormatter.short(ach.rewardGold)} CR",
                            style = TrueIdleType.meta,
                            color = SecondaryGold
                        )
                    }
                    when {
                        ach.isClaimed -> StatusChip("CLAIMED", GreenSuccess)
                        canClaim -> SciFiButton(
                            label = "CLAIM",
                            onClick = { onClaimAchievement?.invoke(ach.id) },
                            filled = true,
                            color = SecondaryGold,
                            modifier = Modifier.testTag("claim_ach_${ach.id}")
                        )
                        else -> StatusChip("${ach.progress} / ${ach.target}", TextMuted)
                    }
                }
            }
        }

        // ---- 6. DATA & STORAGE
        SciFiPanel(title = "DATA & STORAGE", accent = SecondaryGold, tag = "SAVE") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SciFiButton(
                    label = "EXPORT SAVE",
                    onClick = {
                        exportedCode = onExportSave()
                        copyStatus = ""
                        showExportDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("export_save_btn"),
                    color = PrimaryNeon
                )
                SciFiButton(
                    label = "IMPORT SAVE",
                    onClick = {
                        importInputText = ""
                        importError = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("import_save_btn"),
                    color = SecondaryGold
                )
            }
            Spacer(Modifier.height(6.dp))
            SciFiButton(
                label = "RESET PROTOCOL: START FRESH",
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("reset_game_btn"),
                color = HpRed
            )
        }
    }

    // ---- Dialogs
    if (showBackgroundConfirm) {
        SciFiDialog(
            title = "ENABLE BACKGROUND RUN?",
            accent = Warn,
            onDismiss = { showBackgroundConfirm = false },
            actions = {
                SciFiButton("CANCEL", { showBackgroundConfirm = false }, Modifier.weight(1f), color = TextMuted)
                SciFiButton(
                    "ENABLE",
                    { showBackgroundConfirm = false; onToggleRunInBackground() },
                    Modifier.weight(1f),
                    color = Warn,
                    filled = true
                )
            }
        ) {
            Text(
                text = "The game loop keeps running behind other apps and drains more battery. A persistent " +
                    "notification is shown while enabled. Switch it off here at any time.",
                style = TrueIdleType.body,
                color = TextMuted
            )
        }
    }

    if (showExportDialog) {
        SciFiDialog(
            title = "EXPORT SAVE CODE",
            accent = PrimaryNeon,
            onDismiss = { showExportDialog = false },
            actions = {
                SciFiButton(
                    "COPY",
                    {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("TrueIdleSave", exportedCode))
                        copyStatus = "SAVE CODE COPIED"
                    },
                    Modifier.weight(1f).testTag("copy_save_clipboard_btn"),
                    color = PrimaryNeon,
                    filled = true
                )
                SciFiButton("CLOSE", { showExportDialog = false }, Modifier.weight(1f), color = TextMuted)
            }
        ) {
            Text("Copy the raw JSON archive below:", style = TrueIdleType.body, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = exportedCode,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().height(160.dp).testTag("export_text_field"),
                textStyle = TrueIdleType.meta,
                shape = RectangleShape,
                colors = fieldColors(PrimaryNeon)
            )
            if (copyStatus.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(copyStatus, style = TrueIdleType.tag, color = GreenSuccess)
            }
        }
    }

    if (showImportDialog) {
        SciFiDialog(
            title = "IMPORT SAVE ARCHIVE",
            accent = SecondaryGold,
            onDismiss = { showImportDialog = false },
            actions = {
                SciFiButton(
                    "RESTORE",
                    {
                        if (importInputText.isBlank()) {
                            importError = "ERR: INPUT ARCHIVE IS EMPTY."
                        } else if (onImportSave(importInputText.trim())) {
                            showImportDialog = false
                        } else {
                            importError = "ERR: INVALID SAVE FORMAT OR CORRUPT CRC."
                        }
                    },
                    Modifier.weight(1f).testTag("confirm_import_btn"),
                    color = SecondaryGold,
                    filled = true
                )
                SciFiButton("CANCEL", { showImportDialog = false }, Modifier.weight(1f), color = TextMuted)
            }
        ) {
            Text("Paste a valid exported JSON save archive:", style = TrueIdleType.body, color = TextMuted)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = importInputText,
                onValueChange = {
                    importInputText = it
                    importError = ""
                },
                placeholder = { Text("Paste JSON code here...", style = TrueIdleType.body, color = TextMuted) },
                modifier = Modifier.fillMaxWidth().height(140.dp).testTag("import_text_field"),
                textStyle = TrueIdleType.meta,
                shape = RectangleShape,
                colors = fieldColors(SecondaryGold)
            )
            if (importError.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(importError, style = TrueIdleType.tag, color = HpRed)
            }
        }
    }

    if (showResetDialog) {
        SciFiDialog(
            title = "CRITICAL RESET PROTOCOL",
            accent = HpRed,
            onDismiss = { showResetDialog = false },
            actions = {
                SciFiButton("ABORT", { showResetDialog = false }, Modifier.weight(1f), color = TextMuted)
                SciFiButton(
                    "PURGE ALL DATA",
                    { onResetGame(); showResetDialog = false },
                    Modifier.weight(1f).testTag("confirm_reset_btn"),
                    color = HpRed,
                    filled = true
                )
            }
        ) {
            Text(
                text = "This irreversible action wipes floor progression, inventory, trained ranks, " +
                    "tamed companions and undead legions.",
                style = TrueIdleType.body,
                color = TextLight
            )
        }
    }
}

@Composable
private fun fieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = BorderDark,
    focusedTextColor = TextLight,
    unfocusedTextColor = TextLight
)

@Composable
private fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = BorderDark)
}

/** Title + subtitle on the left, switch on the right; 56dp tall so the whole row is a clear target. */
@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    accent: Color,
    onChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = TrueIdleType.label, color = TextLight)
            Text(subtitle, style = TrueIdleType.meta, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accent,
                checkedTrackColor = accent.copy(alpha = 0.3f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceVariantDark
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF000000))
            .border(1.dp, BorderDark)
            .padding(8.dp)
    ) {
        Text(label, style = TrueIdleType.meta, color = TextMuted, maxLines = 1)
        Text(value, style = TrueIdleType.numberStat, color = color, maxLines = 1)
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .heightIn(min = 32.dp)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = TrueIdleType.tag, color = color, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = TrueIdleType.label, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = TrueIdleType.value, color = TextLight)
    }
}
