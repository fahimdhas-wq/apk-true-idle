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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.trueidle.R
import com.aistudio.trueidle.core.BigNumberFormatter
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.BackgroundCost
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.runtime.ResourceProbe
import com.aistudio.trueidle.runtime.ResourceSample
import com.aistudio.trueidle.ui.rememberTierColorForOrdinal
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.theme.*

/** The one canonical notation used by every in-game numeric display. */
private const val ACTIVE_NOTATION_LABEL = "A-Z / AA-ZZ / AAA…"

private fun notationSummary(): String =
    "Grouped digits below 1 trillion, then A, B, C … Z, AA, AB … ZZ, AAA and onward. A = 10^12; each next label advances by 10^3."

@Composable
fun SettingsScreen(
    state: GameState,
    onExportSave: () -> String,
    onImportSave: (String) -> Boolean,
    onResetGame: () -> Unit,
    onClaimAchievement: ((String) -> Unit)? = null,
    onSetNotation: (String) -> Unit = {},
    onToggleAutoEquip: () -> Unit = {},
    onToggleAutoMerge: () -> Unit = {},
    onToggleRunInBackground: () -> Unit = {},
    onSetCraftUsage: (Int) -> Unit = {},
    onFightPromotionBoss: () -> Unit = {},
    onExportTelemetryLog: () -> String = { "" },
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val d = com.aistudio.trueidle.ui.LocalDerived.current
    val promo = state.promotion
    val auto = state.automation

    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var exportedCode by rememberSaveable { mutableStateOf("") }
    var importInputText by rememberSaveable { mutableStateOf("") }
    var copyStatus by rememberSaveable { mutableStateOf("") }
    var importError by rememberSaveable { mutableStateOf("") }
    var telemetryCopyFeedback by rememberSaveable { mutableStateOf("") }
    var showBackgroundConfirm by rememberSaveable { mutableStateOf(false) }
    var resourceSample by remember { mutableStateOf<ResourceSample?>(null) }
    LaunchedEffect(Unit) {
        val probe = ResourceProbe()
        while (true) {
            kotlinx.coroutines.delay(2_000L)
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
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. MISSION DOSSIER & PLAY TELEMETRY
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
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 2x3 Statistics Matrix
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DossierTile("PLAY TIME", NumberFormatter.duration(state.lifetime.playTimeSeconds), SecondaryGold, Modifier.weight(1f))
                    DossierTile("TOTAL ELIMINATIONS", state.lifetime.totalKills.toString(), HpRed, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DossierTile("CURRENT SECTOR", "SECTOR ${state.player.currentFloor}", PrimaryNeon, Modifier.weight(1f))
                    DossierTile("GEAR SCORE", "${d.gearScore} GS", SecondaryGold, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DossierTile("ORES MINED", state.lifetime.oresMined.toString(), SecondaryGold, Modifier.weight(1f))
                    DossierTile("HERBS HARVESTED", state.lifetime.herbsHarvested.toString(), GreenSuccess, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Telemetry Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        contentPadding = PaddingValues(vertical = 8.dp)
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
                        contentPadding = PaddingValues(vertical = 8.dp)
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
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("AUTONOMOUS STASIS RECON", style = TrueIdleType.label, color = TextLight)
                        Text(
                            text = stringResource(R.string.offline_recon_description, GameCaps.OFFLINE_CAP_HOURS, GameCaps.OFFLINE_EFFICIENCY_PCT),
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(GreenSuccess.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, GreenSuccess, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.offline_recon_cap_label, GameCaps.OFFLINE_CAP_HOURS),
                            style = TrueIdleType.numberSmall,
                            color = GreenSuccess
                        )
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
                    .padding(11.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = promo.tierName.uppercase(),
                    style = TrueIdleType.titleLarge,
                    // Promotion tier is coloured from the one global tier table.
                    color = rememberTierColorForOrdinal(promo.tier)
                )
                Text(
                    text = "CURRENT PROMOTION TIER (+${promo.bonusPercent}% ALL STATS BONUS)",
                    style = TrueIdleType.meta,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(11.dp))

                // Requirements Grid (Floor & Gear Score)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        Spacer(modifier = Modifier.height(3.dp))
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
                        Spacer(modifier = Modifier.height(3.dp))
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

                Spacer(modifier = Modifier.height(11.dp))

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
                        text = if (canPromote) "ENGAGE PROMOTION BOSS" else "LOCKED (REQUIREMENTS UNMET)",
                        style = TrueIdleType.tag,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. NUMBER NOTATION (drives the single large-number engine)
        SciFiPanel(
            title = "NUMBER NOTATION",
            accent = PrimaryNeon,
            tag = "DISPLAY"
        ) {
            Text(
                text = "How every large value is rendered across the game.",
                style = TrueIdleType.meta,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryNeon.copy(alpha = 0.12f), RectangleShape)
                    .border(1.dp, PrimaryNeon, RectangleShape)
                    .padding(vertical = 9.dp, horizontal = 10.dp)
                    .testTag("notation_alphabetic"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ACTIVE_NOTATION_LABEL,
                    style = TrueIdleType.tag,
                    color = PrimaryNeon,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notationSummary(),
                style = TrueIdleType.meta,
                color = TextMuted
            )
            Text(
                text = "Switching notation never changes stored values.",
                style = TrueIdleType.meta,
                color = TextMuted
            )
        }

        // 7. AUTOMATION PROTOCOLS
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
                    .padding(8.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(8.dp),
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

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(8.dp)
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
                Spacer(modifier = Modifier.height(4.dp))
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

        // 7b. BACKGROUND EXECUTION
        SciFiPanel(
            title = "BACKGROUND EXECUTION",
            accent = Warn,
            tag = if (auto.runInBackground) "ACTIVE" else "OFF"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000))
                    .border(BorderStroke(1.dp, BorderDark))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("RUN GAME IN BACKGROUND", style = TrueIdleType.label, color = TextLight)
                    Text(
                        "Keep fighting live while the app is not on screen",
                        style = TrueIdleType.meta,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = auto.runInBackground,
                    onCheckedChange = { enable ->
                        if (enable) showBackgroundConfirm = true else onToggleRunInBackground()
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Warn,
                        checkedTrackColor = Warn.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceVariantDark
                    ),
                    modifier = Modifier.testTag("run_in_background_switch")
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CAUTION: uses battery, RAM and CPU while running and shows a persistent " +
                    "notification. Rewards are earned at full live rate, not the " +
                    "${GameCaps.OFFLINE_EFFICIENCY_PCT}% offline rate. While the device sleeps or the " +
                    "system freezes the app, that time is paid out as an offline report on return. " +
                    "Swiping the app away stops it.",
                style = TrueIdleType.meta,
                color = Warn
            )
            Spacer(modifier = Modifier.height(6.dp))
            val sample = resourceSample
            val battery = BackgroundCost.batteryPctPerHour(sample?.cpuPercent ?: 0.0)
            ReportRowSettings("CPU (MEASURED, 1 CORE)", sample?.let { String.format(java.util.Locale.ROOT, "%.1f%%", it.cpuPercent) } ?: "SAMPLING")
            ReportRowSettings("RAM (MEASURED, PSS)", sample?.let { String.format(java.util.Locale.ROOT, "%.0f MB", it.ramMb) } ?: "SAMPLING")
            ReportRowSettings(
                "BATTERY (ESTIMATE)",
                String.format(java.util.Locale.ROOT, "%.1f-%.1f %%/HR", battery.lowPctPerHour, battery.highPctPerHour)
            )
            Text(
                text = "CPU and RAM are read from this app while it runs on screen. Battery is derived: " +
                    "measured CPU x ${BackgroundCost.ASSUMED_CORE_MA.toInt()} mA per core + " +
                    "${BackgroundCost.SERVICE_OVERHEAD_MA.toInt()} mA service overhead, against a " +
                    "${BackgroundCost.ASSUMED_BATTERY_MAH.toInt()} mAh battery, +/- device variance. " +
                    "Approximate only.",
                style = TrueIdleType.meta,
                color = TextMuted
            )
        }

        if (showBackgroundConfirm) {
            AlertDialog(
                onDismissRequest = { showBackgroundConfirm = false },
                title = { Text("ENABLE BACKGROUND RUN?", style = TrueIdleType.title, color = Warn) },
                text = {
                    Text(
                        text = "The game loop keeps running behind other apps and drains more battery. " +
                            "A persistent notification is shown while enabled. You can switch it off here at any time.",
                        style = TrueIdleType.body,
                        color = TextMuted
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBackgroundConfirm = false
                            onToggleRunInBackground()
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("ENABLE", style = TrueIdleType.tag, color = Warn) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showBackgroundConfirm = false },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("CANCEL", style = TrueIdleType.tag, color = TextMuted) }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
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
                        .padding(vertical = 3.dp)
                        .background(Color(0xFF000000))
                        .border(1.dp, BorderDark)
                        .padding(6.dp),
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
                                .padding(horizontal = 6.dp, vertical = 3.dp)
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
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                            modifier = Modifier.testTag("claim_ach_${ach.id}")
                        ) {
                            Text("CLAIM (+${NumberFormatter.short(ach.rewardGold)} CR)", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(SurfaceVariantDark, RectangleShape)
                                .border(1.dp, BorderDark, RectangleShape)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
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
                    contentPadding = PaddingValues(vertical = 8.dp)
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
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("IMPORT SAVE", style = TrueIdleType.tag)
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
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text("RESET PROTOCOL (START FRESH)", style = TrueIdleType.tag)
            }
        }
    }

    // Export Save Dialog
    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.5.dp, PrimaryNeon)
            ) {
                Column(modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)) {
                    Text("EXPORT SAVE CODE", style = TrueIdleType.title, color = PrimaryNeon)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Copy the raw JSON archive below and save it to your clipboard:", style = TrueIdleType.body, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(8.dp))

                    if (copyStatus.isNotEmpty()) {
                        Text(text = copyStatus, style = TrueIdleType.tag, color = GreenSuccess)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(1.5.dp, SecondaryGold)
            ) {
                Column(modifier = Modifier.background(Color(0xFF000000)).padding(16.dp)) {
                    Text("IMPORT SAVE ARCHIVE", style = TrueIdleType.title, color = SecondaryGold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paste a valid exported JSON save archive below:", style = TrueIdleType.body, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

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

                    Spacer(modifier = Modifier.height(6.dp))

                    if (importError.isNotEmpty()) {
                        Text(text = importError, color = HpRed, style = TrueIdleType.tag)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RectangleShape,
                border = BorderStroke(2.dp, HpRed)
            ) {
                Column(
                    modifier = Modifier.background(Color(0xFF000000)).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CRITICAL RESET PROTOCOL", style = TrueIdleType.title, color = HpRed)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "WARNING: This irreversible action will wipe all active floor progression, inventory, trained dojo ranks, tamed companions, and undead legions.",
                        style = TrueIdleType.body,
                        color = TextLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
            .padding(6.dp)
    ) {
        Column {
            Text(label, style = TrueIdleType.meta, color = TextMuted)
            Text(value, style = TrueIdleType.numberStat, color = color)
        }
    }
}

@Composable
private fun ReportRowSettings(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = TrueIdleType.label, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, style = TrueIdleType.value, color = TextLight)
    }
}
