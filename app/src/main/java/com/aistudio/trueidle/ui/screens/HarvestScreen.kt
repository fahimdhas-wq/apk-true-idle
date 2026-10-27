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
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.UpgradeCosts
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.DisciplineUpgrade
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun HarvestScreen(
    state: GameState,
    onHarvestPlot: (Int) -> Unit,
    onMineVein: (Int) -> Unit,
    onUpgradeDiscipline: (DisciplineCategory, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val harvest = state.harvest
    val scrollState = rememberScrollState()

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
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                            .border(1.dp, GreenSuccess)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (harvest.autoCollectEnabled && harvest.autoCollectUnlocked) "DRONES ACTIVE" else "MANUAL HARVEST",
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
            tag = "${harvest.plots.count { it.isReady }}/${harvest.plots.size} READY"
        ) {
            harvest.plots.forEach { plot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("plot_card_${plot.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (plot.isReady) GreenSuccess else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(plot.herbName.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (plot.isReady) "READY" else "${(plot.growthProgress * 100).toInt()}%",
                                style = TrueIdleType.tag,
                                color = if (plot.isReady) GreenSuccess else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { plot.growthProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = GreenSuccess,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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
                            modifier = Modifier.fillMaxWidth().testTag("harvest_plot_btn_${plot.id}")
                        ) {
                            Text(if (plot.isReady) "HARVEST BIO-EXTRACTS" else "SYNTHESIZING...", style = TrueIdleType.tag)
                        }
                    }
                }
            }
        }

        // 3. SUBTERRANEAN PLASMA VEINS
        SciFiPanel(
            title = "PLASMA MINING VEINS",
            accent = SecondaryGold,
            tag = "${harvest.veins.count { it.isReady }}/${harvest.veins.size} READY"
        ) {
            harvest.veins.forEach { vein ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .testTag("vein_card_${vein.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (vein.isReady) SecondaryGold else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(vein.oreName.uppercase(), style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (vein.isReady) "READY" else "${(vein.miningProgress * 100).toInt()}%",
                                style = TrueIdleType.tag,
                                color = if (vein.isReady) SecondaryGold else TextMuted
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { vein.miningProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = SecondaryGold,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = { onMineVein(vein.id) },
                            enabled = vein.isReady,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryGold,
                                contentColor = Color.Black,
                                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                disabledContentColor = TextMuted
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("mine_vein_btn_${vein.id}")
                        ) {
                            Text(if (vein.isReady) "EXTRACT ORE" else "DRILLING...", style = TrueIdleType.tag)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(upg.name, style = TrueIdleType.title, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surface, RectangleShape)
                                        .border(1.dp, PrimaryNeon)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text("RANK ${upg.rank}/${upg.maxRank}", style = TrueIdleType.numberSmall, color = PrimaryNeon)
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(upg.description, style = TrueIdleType.meta, color = TextMuted)
                        }

                        Button(
                            onClick = { onUpgradeDiscipline(DisciplineCategory.HARVEST, upg.id) },
                            enabled = canAfford && !isMax,
                            shape = RectangleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (canAfford) PrimaryNeon else MaterialTheme.colorScheme.surface,
                                contentColor = if (canAfford) Color.Black else TextMuted
                            ),
                            border = BorderStroke(1.dp, if (canAfford) PrimaryNeon else MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("upgrade_harvest_${upg.id}")
                        ) {
                            Text(
                                text = if (isMax) "MAX" else "🪙 ${NumberFormatter.short(cost)}",
                                style = TrueIdleType.tag,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
