package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.model.GameState
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
            .padding(12.dp)
            .testTag("gathering_screen")
    ) {
        // Tab Selector (Farming vs Mining)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = PrimaryNeon,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🌿 Farming (LVL ${state.gathering.farmingLvl})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("⛏️ Mining (LVL ${state.gathering.miningLvl})", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            // Farming Plots
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.gathering.plots, key = { it.id }) { plot ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("farm_plot_${plot.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Plot #${plot.id + 1}: ${plot.herbName}", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { onHarvestPlot(plot.id) },
                                    enabled = plot.isReady,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (plot.isReady) GreenSuccess else SurfaceVariantDark,
                                        contentColor = if (plot.isReady) BgDark else TextMuted
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (plot.isReady) "Harvest!" else "Growing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceVariantDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(plot.growthProgress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(GreenSuccess)
                                    )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (plot.isReady) "Ready to harvest (Yields 2x ${plot.herbName})" else "Maturity: ${(plot.growthProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
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
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vein #${vein.id + 1}: ${vein.oreName}", style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { onMineVein(vein.id) },
                                    enabled = vein.isReady,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (vein.isReady) SecondaryGold else SurfaceVariantDark,
                                        contentColor = if (vein.isReady) BgDark else TextMuted
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (vein.isReady) "Mine!" else "Excavating...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceVariantDark)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(vein.miningProgress)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SecondaryGold)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (vein.isReady) "Vein exposed (Yields 3x ${vein.oreName})" else "Drilling: ${(vein.miningProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
