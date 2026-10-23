package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun GatheringScreen(
    state: GameState,
    onHarvestPlot: (Int) -> Unit,
    onMineVein: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Farming, 1: Mining

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(10.dp)
            .testTag("gathering_screen")
    ) {
        // High-end Cyberpunk Dual Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, BorderDark, RectangleShape)
                .background(SurfaceDark, RectangleShape)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 0 }
                    .background(if (selectedTab == 0) GreenSuccess.copy(alpha = 0.15f) else Color.Transparent, RectangleShape)
                    .border(
                        BorderStroke(
                            width = if (selectedTab == 0) 1.5.dp else 0.dp,
                            color = if (selectedTab == 0) GreenSuccess else Color.Transparent
                        ),
                        RectangleShape
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌿 BIO-FARM [LVL ${state.gathering.farmingLvl}]",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedTab == 0) GreenSuccess else TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 1 }
                    .background(if (selectedTab == 1) SecondaryGold.copy(alpha = 0.15f) else Color.Transparent, RectangleShape)
                    .border(
                        BorderStroke(
                            width = if (selectedTab == 1) 1.5.dp else 0.dp,
                            color = if (selectedTab == 1) SecondaryGold else Color.Transparent
                        ),
                        RectangleShape
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⛏ ORE EXTRACTION [LVL ${state.gathering.miningLvl}]",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selectedTab == 1) SecondaryGold else TextMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Level & XP Bar Header for Current Gathering Discipline
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, BorderDark)
        ) {
            Box(modifier = Modifier.padding(10.dp)) {
                if (selectedTab == 0) {
                    XpProgressBar(
                        skillName = "FARMING DISCIPLINE",
                        level = state.gathering.farmingLvl,
                        currentXp = state.gathering.farmingXp,
                        requiredXp = Balance.skillXpReq(state.gathering.farmingLvl),
                        barColor = GreenSuccess
                    )
                } else {
                    XpProgressBar(
                        skillName = "MINING DISCIPLINE",
                        level = state.gathering.miningLvl,
                        currentXp = state.gathering.miningXp,
                        requiredXp = Balance.skillXpReq(state.gathering.miningLvl),
                        barColor = SecondaryGold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // Bio-Farming Plots
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.gathering.plots, key = { it.id }) { plot ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("farm_plot_${plot.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (plot.isReady) GreenSuccess.copy(alpha = 0.6f) else BorderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PLOT 0${plot.id + 1}: ${plot.herbName.uppercase()}", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                OutlinedButton(
                                    onClick = { onHarvestPlot(plot.id) },
                                    enabled = plot.isReady,
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (plot.isReady) GreenSuccess else BorderDark),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (plot.isReady) GreenSuccess.copy(alpha = 0.2f) else SurfaceVariantDark,
                                        contentColor = if (plot.isReady) GreenSuccess else TextMuted
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(if (plot.isReady) "HARVEST" else "GROWING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color(0xFF000000), RectangleShape)
                                    .border(1.dp, BorderDark, RectangleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(plot.growthProgress)
                                        .background(GreenSuccess, RectangleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (plot.isReady) "STATUS: OPTIMAL BIO-MATURITY (YIELDS 2x ${plot.herbName.uppercase()})" else "BIO-SYNTHESIS: ${(plot.growthProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (plot.isReady) GreenSuccess else TextMuted
                            )
                        }
                    }
                }
            }
        } else {
            // Mining Veins
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.gathering.veins, key = { it.id }) { vein ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("mine_vein_${vein.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (vein.isReady) SecondaryGold.copy(alpha = 0.6f) else BorderDark)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("VEIN 0${vein.id + 1}: ${vein.oreName.uppercase()}", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                OutlinedButton(
                                    onClick = { onMineVein(vein.id) },
                                    enabled = vein.isReady,
                                    shape = RectangleShape,
                                    border = BorderStroke(1.dp, if (vein.isReady) SecondaryGold else BorderDark),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (vein.isReady) SecondaryGold.copy(alpha = 0.2f) else SurfaceVariantDark,
                                        contentColor = if (vein.isReady) SecondaryGold else TextMuted
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(if (vein.isReady) "EXTRACT" else "DRILLING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color(0xFF000000), RectangleShape)
                                    .border(1.dp, BorderDark, RectangleShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(vein.miningProgress)
                                        .background(SecondaryGold, RectangleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (vein.isReady) "STATUS: VEIN EXPOSED (YIELDS 3x ${vein.oreName.uppercase()})" else "DRILL PROGRESS: ${(vein.miningProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (vein.isReady) SecondaryGold else TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
