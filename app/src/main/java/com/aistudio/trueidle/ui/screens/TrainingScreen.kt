package com.aistudio.trueidle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun TrainingScreen(
    state: GameState,
    onTrainNode: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("training_screen")
    ) {
        // Training Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(8.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Dojo Training", style = MaterialTheme.typography.titleLarge, color = PrimaryNeon, fontWeight = FontWeight.Bold)
                    Text("Upgrade permanent core stats with Gold", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(SecondaryGold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🪙 ${state.player.gold} Gold",
                        style = MaterialTheme.typography.titleMedium,
                        color = SecondaryGold,
                        fontWeight = FontWeight.Bold
                    )
                }
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
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(node.name, style = MaterialTheme.typography.titleMedium, color = TextLight, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rank ${node.rank}/${node.maxRank}", style = MaterialTheme.typography.labelSmall, color = PrimaryNeon)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(node.stat, style = MaterialTheme.typography.bodySmall, color = GreenSuccess)
                        }

                        Button(
                            onClick = { onTrainNode(node.id) },
                            enabled = canAfford,
                            modifier = Modifier.testTag("train_btn_${node.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMaxed) SurfaceVariantDark else PrimaryNeon,
                                contentColor = BgDark
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isMaxed) "MAX" else "Train (${node.costGold} 🪙)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
