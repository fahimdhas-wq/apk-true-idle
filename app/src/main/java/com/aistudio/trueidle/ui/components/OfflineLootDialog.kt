package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.OfflineLoot
import com.aistudio.trueidle.ui.theme.*

private data class LevelProjection(val level: Int, val xp: Long, val levels: Int)

/** Mirrors the claim: banked XP rolls through the same player curve the engine uses. */
private fun projectLevel(level: Int, xp: Long, gain: Long): LevelProjection {
    var lvl = level.coerceAtLeast(1)
    var bank = xp + gain
    var levels = 0
    while (bank >= XpCurves.player(lvl)) {
        bank -= XpCurves.player(lvl)
        lvl++
        levels++
    }
    return LevelProjection(lvl, bank, levels)
}

@Composable
fun OfflineLootDialog(
    loot: OfflineLoot,
    state: GameState,
    onClaim: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    // A Dialog is its own window and its view resets LocalDensity to the raw device density, which
    // drops the app-wide UI_SCALE and makes this report render larger than the game. Re-provide the
    // scaled density captured from the main composition.
    val appDensity = LocalDensity.current
    val projection = remember(loot.xpEarned, state.player.lvl, state.player.xp) {
        projectLevel(state.player.lvl, state.player.xp, loot.xpEarned)
    }

    val realAwaySeconds = if (loot.rawAwaySeconds > 0L) loot.rawAwaySeconds else loot.timeAwaySeconds
    val effectiveSeconds =
        if (loot.effectiveSeconds > 0L) loot.effectiveSeconds
        else (loot.timeAwaySeconds * loot.efficiency).toLong()
    val netFloors = loot.finalFloor - loot.startFloor
    val sectorColor = when {
        netFloors > 0 -> GreenSuccess
        netFloors < 0 -> Warn
        else -> PrimaryNeon
    }
    val sectorStatus = when {
        netFloors > 0 -> "+$netFloors ADVANCED"
        netFloors < 0 -> "${-netFloors} RETREATED"
        else -> "DEFENDED"
    }
    val talentPoints = projection.levels + loot.talentPointsGained
    val faithNet = loot.faithEarned - loot.faithDrained
    val slayerTask = state.slayer.currentTask

    // Back / outside tap must not silently bank the rewards: only the explicit claim button does.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalDensity provides appDensity) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(MaterialTheme.colorScheme.background)
                .border(1.dp, PrimaryNeon, RectangleShape)
                .testTag("offline_loot_dialog")
        ) {
            // ---- Header (pinned)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("◈ OFFLINE RECON REPORT", style = TrueIdleType.titleLarge, color = PrimaryNeon)
                    Text(
                        text = "AUTONOMOUS STASIS SYSTEM · ${NumberFormatter.percent(loot.efficiency)} SYNC",
                        style = TrueIdleType.meta,
                        color = TextMuted
                    )
                }
                Spacer(Modifier.width(6.dp))
                val badgeColor = if (loot.capApplied) Warn else PrimaryNeon
                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, badgeColor, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (loot.capApplied) "${GameCaps.OFFLINE_CAP_HOURS}H CAP" else NumberFormatter.duration(loot.timeAwaySeconds),
                        style = TrueIdleType.tag,
                        color = badgeColor
                    )
                }
            }

            // ---- Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. TIME ACCOUNTING
                SciFiPanel(
                    title = "TIME ACCOUNTING",
                    accent = if (loot.capApplied) Warn else PrimaryNeon,
                    tag = "${NumberFormatter.percent(loot.efficiency)} EFFICIENCY"
                ) {
                    ReportRow("TIME AWAY", NumberFormatter.duration(realAwaySeconds))
                    ReportRow(
                        "COUNTED (CAP ${GameCaps.OFFLINE_CAP_HOURS}H)",
                        NumberFormatter.duration(loot.timeAwaySeconds),
                        if (loot.capApplied) Warn else TextLight
                    )
                    ReportRow(
                        "EFFECTIVE COMBAT TIME",
                        NumberFormatter.duration(effectiveSeconds),
                        PrimaryNeon
                    )
                    if (loot.capApplied && realAwaySeconds > loot.timeAwaySeconds) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${NumberFormatter.duration(realAwaySeconds - loot.timeAwaySeconds)} beyond the " +
                                "${GameCaps.OFFLINE_CAP_HOURS}H cap earned nothing.",
                            style = TrueIdleType.meta,
                            color = Warn
                        )
                    }
                }

                // 2. SECTOR TRAVERSAL
                SciFiPanel(
                    title = "SECTOR TRAVERSAL",
                    accent = sectorColor,
                    tag = sectorStatus
                ) {
                    ReportRow("START ➔ FINAL", "SECTOR ${loot.startFloor} ➔ ${loot.finalFloor}", sectorColor)
                    ReportRow("SECTORS CLEARED", "${loot.floorsGained}", if (loot.floorsGained > 0) GreenSuccess else TextLight)
                    if (loot.floorsLost > 0) {
                        ReportRow("FELL BACK (UNSURVIVABLE)", "-${loot.floorsLost}", Warn)
                    }
                    ReportRow("PROGRESS ON FINAL SECTOR", "${loot.killsOnFloor} KILLS", TextLight)
                    if (loot.dps > 0.0) {
                        ReportRow("SIMULATED OUTPUT", "${NumberFormatter.short(loot.dps)} DPS", SecondaryGold)
                    }
                }

                // 3. RECOVERED REWARDS
                SciFiPanel(
                    title = "RECOVERED REWARDS",
                    accent = SecondaryGold,
                    tag = "${NumberFormatter.short(loot.kills)} KILLS"
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportTile(
                            label = "HOSTILES ELIMINATED",
                            value = NumberFormatter.short(loot.kills),
                            sub = "${NumberFormatter.short(loot.killsPerHour)}/HR · ${loot.bossKills} BOSS",
                            color = HpRed,
                            modifier = Modifier.weight(1f)
                        )
                        ReportTile(
                            label = "CREDITS RECOVERED",
                            value = "+${NumberFormatter.short(loot.goldEarned)}",
                            sub = "${NumberFormatter.short(loot.goldPerHour)}/HR",
                            color = SecondaryGold,
                            modifier = Modifier.weight(1f),
                            icon = SciFiIconType.CREDIT
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportTile(
                            label = "COMBAT EXPERIENCE",
                            value = "+${NumberFormatter.short(loot.xpEarned)} XP",
                            sub = "${NumberFormatter.short(loot.xpPerHour)}/HR · x${String.format(java.util.Locale.ROOT, "%.2f", loot.xpMultiplier)}",
                            color = PrimaryNeon,
                            modifier = Modifier.weight(1f)
                        )
                        ReportTile(
                            label = "BONE ESSENCE",
                            value = "+${NumberFormatter.short(loot.bonesEarned)}",
                            sub = "TOTAL ${NumberFormatter.short(state.necro.boneEssence + loot.bonesEarned)}",
                            color = NecroPurple,
                            modifier = Modifier.weight(1f),
                            icon = SciFiIconType.ESSENCE
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        ReportTile(
                            label = "FAITH",
                            value = (if (faithNet >= 0) "+" else "") + NumberFormatter.short(faithNet),
                            sub = "+${NumberFormatter.short(loot.faithEarned)} EARNED · -${NumberFormatter.short(loot.faithDrained)} DRAINED",
                            color = ManaTeal,
                            modifier = Modifier.weight(1f)
                        )
                        ReportTile(
                            label = "TALENT POINTS",
                            value = "+${NumberFormatter.short(talentPoints)} TP",
                            sub = "${projection.levels} LEVELS · ${loot.talentPointsGained} BOSS",
                            color = PrimaryNeon,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    ReportRow(
                        "CREDITS AFTER CLAIM",
                        "${NumberFormatter.short(state.player.gold)} ➔ ${NumberFormatter.short(state.player.gold + loot.goldEarned)}",
                        SecondaryGold
                    )
                }

                // 4. HERO PROGRESSION
                SciFiPanel(
                    title = "HERO PROGRESSION",
                    accent = PrimaryNeon,
                    tag = if (projection.levels > 0) "LVL ${state.player.lvl} ➔ ${projection.level}" else "LVL ${state.player.lvl}"
                ) {
                    XpProgressBar(
                        skillName = "HERO EXPERIENCE (AFTER CLAIM)",
                        level = projection.level,
                        currentXp = projection.xp,
                        requiredXp = XpCurves.player(projection.level),
                        barColor = PrimaryNeon
                    )
                    Spacer(Modifier.height(4.dp))
                    ReportRow(
                        "LEVELS GAINED",
                        "+${projection.levels}",
                        if (projection.levels > 0) GreenSuccess else TextMuted
                    )
                    if (projection.levels > 0) {
                        Text(
                            text = "Level-ups fully restore HP, shield and mana on claim.",
                            style = TrueIdleType.meta,
                            color = TextMuted
                        )
                    }
                }

                // 5. AEGIS BARRIER
                if (loot.aegisCoveragePct > 0 || loot.faithDrained > 0L) {
                    SciFiPanel(
                        title = "AEGIS BARRIER",
                        accent = ManaTeal,
                        tag = if (state.essence.aegisActive) "ONLINE" else "OFFLINE"
                    ) {
                        SciFiGauge(
                            label = "BARRIER COVERAGE",
                            valueText = "${loot.aegisCoveragePct}%",
                            ratio = loot.aegisCoveragePct / 100f,
                            color = ManaTeal
                        )
                        Spacer(Modifier.height(4.dp))
                        ReportRow("FAITH DRAINED", "-${NumberFormatter.short(loot.faithDrained)}", Warn)
                        ReportRow("FAITH REMAINING", NumberFormatter.short(state.essence.faith), ManaTeal)
                        if (!state.essence.aegisActive && loot.faithDrained > 0L) {
                            Text(
                                text = "Faith depleted: Aegis shut down. Re-engage it on the Essence screen.",
                                style = TrueIdleType.meta,
                                color = Warn
                            )
                        }
                    }
                }

                // 6. SALVAGE
                SciFiPanel(
                    title = "SALVAGED RESOURCES",
                    accent = GreenSuccess,
                    tag = "${loot.items.size + (if (loot.beastsTamed > 0) 1 else 0)} TYPES"
                ) {
                    val hasSalvage = loot.items.isNotEmpty() || loot.beastsTamed > 0 || loot.slayerKills > 0
                    if (!hasSalvage) {
                        Text("NO SALVAGE RECOVERED.", style = TrueIdleType.meta, color = TextMuted)
                    }
                    loot.items.forEach { item ->
                        ReportRow("◈ ${item.name.uppercase()}", "+${NumberFormatter.short(item.count)}", GreenSuccess)
                    }
                    if (loot.beastsTamed > 0) {
                        ReportRow("◈ COMPANION BEASTS TAMED", "+${loot.beastsTamed}", GreenSuccess)
                    }
                    if (loot.slayerKills > 0) {
                        ReportRow(
                            "◈ SLAYER: ${slayerTask.targetName.uppercase()}",
                            "+${loot.slayerKills} (${minOf(slayerTask.currentKills + loot.slayerKills, slayerTask.requiredKills)}/${slayerTask.requiredKills})",
                            HpRed
                        )
                    }
                }

                // 7. ACTIVITY LOG
                if (loot.timelineLog.isNotEmpty()) {
                    SciFiPanel(
                        title = "ACTIVITY LOG",
                        accent = TextLight,
                        tag = "${loot.timelineLog.size} ENTRIES"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            loot.timelineLog.forEach { entry ->
                                Text(text = "› $entry", style = TrueIdleType.meta, color = TextLight)
                            }
                        }
                    }
                }
            }

            // ---- Footer (pinned)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .padding(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClaim()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("claim_offline_btn"),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "CLAIM SALVAGE & RESUME MISSION",
                        style = TrueIdleType.tag,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ReportRow(
    label: String,
    value: String,
    valueColor: Color = TextLight
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = TrueIdleType.label,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(6.dp))
        Text(text = value, style = TrueIdleType.value, color = valueColor)
    }
}

@Composable
private fun ReportTile(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: SciFiIconType? = null
) {
    Box(
        modifier = modifier
            .background(Color(0xFF000000), RectangleShape)
            .border(1.dp, BorderDark, RectangleShape)
            .padding(6.dp)
    ) {
        Column {
            Text(label, style = TrueIdleType.meta, color = TextMuted)
            if (icon != null) {
                SciFiBadge(
                    type = icon,
                    label = value,
                    textColor = color,
                    textStyle = TrueIdleType.numberStat
                )
            } else {
                Text(value, style = TrueIdleType.numberStat, color = color)
            }
            Text(sub, style = TrueIdleType.monoTimestamp, color = TextMuted)
        }
    }
}
