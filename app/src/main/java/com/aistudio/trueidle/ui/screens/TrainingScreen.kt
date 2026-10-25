package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun TrainingScreen(
    state: GameState,
    onTrainNode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(10.dp)
            .testTag("training_screen")
    ) {
        // Training Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CYBER DOJO PROTOCOL", style = MaterialTheme.typography.titleLarge, color = PrimaryNeon, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Neural attribute enhancements via Credits", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                    Box(
                        modifier = Modifier
                            .background(SecondaryGold.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, SecondaryGold, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🪙 ${state.player.gold}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SecondaryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                XpProgressBar(
                    skillName = "PHYSICAL TRAINING",
                    level = state.training.lvl,
                    currentXp = state.training.xp,
                    requiredXp = Balance.skillXpReq(state.training.lvl),
                    barColor = PrimaryNeon
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Training Nodes List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.training.nodes, key = { it.id }) { node ->
                val canAfford = state.player.gold >= node.costGold && node.rank < node.maxRank
                val isMaxed = node.rank >= node.maxRank

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("training_node_${node.id}"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (canAfford) PrimaryNeon.copy(alpha = 0.35f) else BorderDark)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(node.name.uppercase(), style = TrueIdleType.title, color = TextLight)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(node.stat, style = TrueIdleType.meta, color = GreenSuccess)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Fixed width aligned Rank Box
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.6f), RectangleShape)
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R ${node.rank}/${node.maxRank}",
                                    style = TrueIdleType.numberSmall,
                                    color = PrimaryNeon
                                )
                            }

                            OutlinedButton(
                                onClick = { onTrainNode(node.id) },
                                enabled = canAfford,
                                modifier = Modifier.testTag("train_btn_${node.id}"),
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (canAfford) PrimaryNeon else BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (canAfford) PrimaryNeon.copy(alpha = 0.15f) else SurfaceVariantDark,
                                    contentColor = if (canAfford) PrimaryNeon else TextMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isMaxed) "MAX" else "ENHANCE [${node.costGold} 🪙]",
                                    style = TrueIdleType.tag
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
