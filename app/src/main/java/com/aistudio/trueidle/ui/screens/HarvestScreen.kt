package com.aistudio.trueidle.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*
import java.util.Locale

@Composable
fun HarvestScreen(
    state: GameState,
    onHarvestPlot: (Int) -> Unit,
    onMineVein: (Int) -> Unit,
    onTogglePlotAutoQueue: (Int) -> Unit = {},
    onToggleVeinAutoQueue: (Int) -> Unit = {},
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val harvest = state.harvest
    val scrollState = rememberScrollState()
    val speedRank = harvest.upgrades.firstOrNull { it.id == "harvest_speed" }?.rank ?: 0
    val miningRank = harvest.upgrades.firstOrNull { it.id == "mining_speed" }?.rank ?: 0
    val extraYield = harvest.upgrades.firstOrNull { it.id == "harvest_yield" }?.rank ?: 0
    val farmMult = (Math.pow(0.97, (harvest.farmingLvl - 1).toDouble()) * Math.pow(0.92, speedRank.toDouble()))
        .coerceAtLeast(0.20).toFloat()
    val mineMult = (Math.pow(0.97, (harvest.miningLvl - 1).toDouble()) * Math.pow(0.92, miningRank.toDouble()))
        .coerceAtLeast(0.20).toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(8.dp)
            .testTag("harvest_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. DISCIPLINES OVERVIEW
        Card(
            modifier = Modifier.fillMaxWidth().testTag("harvest_header_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RectangleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HARVEST & EXTRACTION MATRIX", style = TrueIdleType.title, color = GreenSuccess)
                    val activeAutoPlots = harvest.plots.count { it.autoQueue }
                    val activeAutoVeins = harvest.veins.count { it.autoQueue }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, GreenSuccess)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeAutoPlots > 0 || activeAutoVeins > 0 || harvest.autoCollectEnabled && harvest.autoCollectUnlocked)
                                "QUEUES ENGAGED ($activeAutoPlots/1 FARM · $activeAutoVeins/1 MINE)"
                            else "MANUAL HARVEST",
                            style = TrueIdleType.tag,
                            color = GreenSuccess
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "BIO-FARMING",
                    level = harvest.farmingLvl,
                    currentXp = harvest.farmingXp,
                    requiredXp = XpCurves.skill(harvest.farmingLvl),
                    barColor = GreenSuccess
                )
                Spacer(modifier = Modifier.height(6.dp))
                XpProgressBar(
                    skillName = "PLASMA MINING",
                    level = harvest.miningLvl,
                    currentXp = harvest.miningXp,
                    requiredXp = XpCurves.skill(harvest.miningLvl),
                    barColor = SecondaryGold
                )
            }
        }

        // 2. HYDROPONIC BIO-PLOTS
        SciFiPanel(
            title = "HYDROPONIC BIO-PLOTS",
            accent = GreenSuccess,
            tag = "${harvest.plots.count { it.isReady }}/${harvest.plots.size} READY · 1 AUTO QUEUE"
        ) {
            harvest.plots.forEach { plot ->
                val duration = (plot.durationSeconds * farmMult).coerceAtLeast(0.1f)
                val remainingSec = ((1f - plot.growthProgress) * duration).coerceAtLeast(0f)
                val timeText = if (plot.isReady) "READY" else String.format(Locale.ROOT, "%.1fs", remainingSec)
                val profMult = XpCurves.proficiencyYieldMultiplier(plot.proficiencyLevel)
                val reqProfXp = XpCurves.proficiency(plot.proficiencyLevel)
                val profRatio = if (plot.proficiencyLevel >= 100) 1f else (plot.proficiencyXp.toFloat() / reqProfXp.toFloat()).coerceIn(0f, 1f)
                val totalYield = (2.0 + extraYield) * profMult

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("plot_card_${plot.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (plot.autoQueue) GreenSuccess else if (plot.isReady) GreenSuccess.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Plot header & level badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(plot.herbName.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LVL ${plot.proficiencyLevel}/100",
                                    style = TrueIdleType.tag,
                                    color = if (plot.proficiencyLevel >= 100) SecondaryGold else PrimaryNeon
                                )
                                Text(
                                    text = "x${NumberFormatter.short(profMult)} YIELD",
                                    style = TrueIdleType.tag,
                                    color = SecondaryGold
                                )
                                Text(
                                    text = timeText,
                                    style = TrueIdleType.tag,
                                    color = if (plot.isReady) GreenSuccess else TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        // Item Proficiency Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ITEM PROFICIENCY",
                                style = TrueIdleType.meta,
                                color = TextMuted
                            )
                            Text(
                                text = if (plot.proficiencyLevel >= 100) "MAX RANK (2x/LVL)" else "${NumberFormatter.short(plot.proficiencyXp.toDouble())} / ${NumberFormatter.short(reqProfXp.toDouble())} XP",
                                style = TrueIdleType.meta,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { profRatio },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = PrimaryNeon,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        // Cycle Growth Progress
                        LinearProgressIndicator(
                            progress = { plot.growthProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = GreenSuccess,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // Action row: Auto Queue Toggle + Harvest Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dedicated 1-item Auto Queue toggle
                            Button(
                                onClick = { onTogglePlotAutoQueue(plot.id) },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (plot.autoQueue) GreenSuccess.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (plot.autoQueue) GreenSuccess else TextMuted
                                ),
                                border = BorderStroke(1.dp, if (plot.autoQueue) GreenSuccess else MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f).testTag("auto_queue_plot_${plot.id}")
                            ) {
                                Text(
                                    text = if (plot.autoQueue) "AUTO [ACTIVE]" else "AUTO [OFF]",
                                    style = TrueIdleType.tag,
                                    color = if (plot.autoQueue) GreenSuccess else TextMuted
                                )
                            }

                            // Manual / state trigger button
                            Button(
                                onClick = { onHarvestPlot(plot.id) },
                                enabled = plot.isReady,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenSuccess,
                                    contentColor = Color.Black,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    disabledContentColor = TextMuted
                                ),
                                modifier = Modifier.weight(2f).testTag("harvest_plot_btn_${plot.id}")
                            ) {
                                Text(
                                    text = when {
                                        plot.isReady -> "HARVEST (+${NumberFormatter.short(totalYield)})"
                                        plot.autoQueue -> "AUTORUN (${timeText})"
                                        else -> "GROWING (${timeText})"
                                    },
                                    style = TrueIdleType.tag
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. SUBTERRANEAN PLASMA VEINS
        SciFiPanel(
            title = "PLASMA MINING VEINS",
            accent = SecondaryGold,
            tag = "${harvest.veins.count { it.isReady }}/${harvest.veins.size} READY · 1 AUTO QUEUE"
        ) {
            harvest.veins.forEach { vein ->
                val isLocked = harvest.miningLvl < vein.reqLvl
                val duration = (vein.durationSeconds * mineMult).coerceAtLeast(0.1f)
                val remainingSec = ((1f - vein.miningProgress) * duration).coerceAtLeast(0f)
                val timeText = when {
                    isLocked -> "REQ LVL ${vein.reqLvl}"
                    vein.isReady -> "READY"
                    else -> String.format(Locale.ROOT, "%.1fs", remainingSec)
                }
                val profMult = XpCurves.proficiencyYieldMultiplier(vein.proficiencyLevel)
                val reqProfXp = XpCurves.proficiency(vein.proficiencyLevel)
                val profRatio = if (vein.proficiencyLevel >= 100) 1f else (vein.proficiencyXp.toFloat() / reqProfXp.toFloat()).coerceIn(0f, 1f)
                val totalYield = (3.0 + extraYield) * profMult

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("vein_card_${vein.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (vein.autoQueue) SecondaryGold else if (vein.isReady && !isLocked) SecondaryGold.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        // Vein header & level badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vein.oreName.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LVL ${vein.proficiencyLevel}/100",
                                    style = TrueIdleType.tag,
                                    color = if (vein.proficiencyLevel >= 100) SecondaryGold else PrimaryNeon
                                )
                                Text(
                                    text = "x${NumberFormatter.short(profMult)} YIELD",
                                    style = TrueIdleType.tag,
                                    color = SecondaryGold
                                )
                                Text(
                                    text = timeText,
                                    style = TrueIdleType.tag,
                                    color = when {
                                        isLocked -> TextMuted
                                        vein.isReady -> SecondaryGold
                                        else -> TextMuted
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        // Item Proficiency Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DRILL PROFICIENCY",
                                style = TrueIdleType.meta,
                                color = TextMuted
                            )
                            Text(
                                text = if (vein.proficiencyLevel >= 100) "MAX RANK (2x/LVL)" else "${NumberFormatter.short(vein.proficiencyXp.toDouble())} / ${NumberFormatter.short(reqProfXp.toDouble())} XP",
                                style = TrueIdleType.meta,
                                color = TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { profRatio },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = PrimaryNeon,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        // Drilling Progress
                        LinearProgressIndicator(
                            progress = { if (isLocked) 0f else vein.miningProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = SecondaryGold,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        // Action row: Auto Queue Toggle + Extract Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dedicated 1-item Auto Queue toggle
                            Button(
                                onClick = { onToggleVeinAutoQueue(vein.id) },
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (vein.autoQueue) SecondaryGold.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                    contentColor = if (vein.autoQueue) SecondaryGold else TextMuted
                                ),
                                border = BorderStroke(1.dp, if (vein.autoQueue) SecondaryGold else MaterialTheme.colorScheme.outline),
                                modifier = Modifier.weight(1f).testTag("auto_queue_vein_${vein.id}")
                            ) {
                                Text(
                                    text = if (vein.autoQueue) "AUTO [ACTIVE]" else "AUTO [OFF]",
                                    style = TrueIdleType.tag,
                                    color = if (vein.autoQueue) SecondaryGold else TextMuted
                                )
                            }

                            // Manual / state trigger button
                            Button(
                                onClick = { onMineVein(vein.id) },
                                enabled = vein.isReady && !isLocked,
                                shape = RectangleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SecondaryGold,
                                    contentColor = Color.Black,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    disabledContentColor = TextMuted
                                ),
                                modifier = Modifier.weight(2f).testTag("mine_vein_btn_${vein.id}")
                            ) {
                                Text(
                                    text = when {
                                        isLocked -> "LOCKED (MINING LVL ${vein.reqLvl})"
                                        vein.isReady -> "EXTRACT (+${NumberFormatter.short(totalYield)})"
                                        vein.autoQueue -> "AUTORUN (${timeText})"
                                        else -> "DRILLING (${timeText})"
                                    },
                                    style = TrueIdleType.tag
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. HARVEST & MINING UPGRADES BAY
        SciFiPanel(
            title = "EXTRACTION BAY UPGRADES",
            accent = PrimaryNeon,
            tag = "RESEARCH"
        ) {
            harvest.upgrades.forEach { upg ->
                val cost = UpgradeCosts.nextRank(upg.baseCost, upg.rank, upg.costMultiplier)
                val canAfford = state.player.gold >= cost
                val isMax = upg.rank >= upg.maxRank

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(upg.name.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Text(upg.description, style = TrueIdleType.meta, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("RANK: ${upg.rank}/${upg.maxRank}", style = TrueIdleType.tag, color = PrimaryNeon)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onUpgradeDiscipline(DisciplineCategory.HARVEST, upg.id) },
                            enabled = canAfford && !isMax,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryNeon,
                                contentColor = Color.Black,
                                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                disabledContentColor = TextMuted
                            )
                        ) {
                            if (isMax) {
                                Text("MAX", style = TrueIdleType.tag, fontWeight = FontWeight.Bold)
                            } else {
                                SciFiBadge(
                                    type = SciFiIconType.CREDIT,
                                    label = NumberFormatter.short(cost),
                                    textColor = if (canAfford) Color.Black else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
