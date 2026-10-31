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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.TrainingCaps
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
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
            .padding(8.dp)
            .testTag("training_screen")
    ) {
        // Training Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text("HERO TRAINING HALL", style = TrueIdleType.titleLarge, color = PrimaryNeon)
                        Text("Upgrade permanent hero attributes with Gold", style = TrueIdleType.meta, color = TextMuted)
                    }
                    SciFiBadge(
                        type = SciFiIconType.CREDIT,
                        label = NumberFormatter.short(state.player.gold),
                        iconTint = SecondaryGold,
                        textColor = SecondaryGold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                XpProgressBar(
                    skillName = "TRAINING MASTERY",
                    level = state.training.lvl,
                    currentXp = state.training.xp,
                    requiredXp = XpCurves.skill(state.training.lvl),
                    barColor = PrimaryNeon
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Training Nodes List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = node.name.uppercase(),
                                style = TrueIdleType.title,
                                color = TextLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = node.stat,
                                style = TrueIdleType.meta,
                                color = GreenSuccess,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            trainingEffectLabel(node.id, node.rank)?.let { effect ->
                                Text(
                                    text = effect,
                                    style = TrueIdleType.meta,
                                    color = if (isMaxed) SecondaryGold else TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Uniform matching 42.dp height for both rank badge and action button
                            Box(
                                modifier = Modifier
                                    .width(76.dp)
                                    .height(42.dp)
                                    .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                                    .border(1.dp, PrimaryNeon.copy(alpha = 0.6f), RectangleShape)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "R ${node.rank}/${node.maxRank}",
                                    style = TrueIdleType.numberSmall.copy(fontSize = 11.sp, letterSpacing = 0.2.sp),
                                    color = PrimaryNeon,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { onTrainNode(node.id) },
                                enabled = canAfford,
                                modifier = Modifier
                                    .width(136.dp)
                                    .height(42.dp)
                                    .testTag("train_btn_${node.id}"),
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, if (canAfford) PrimaryNeon else BorderDark),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (canAfford) PrimaryNeon.copy(alpha = 0.15f) else SurfaceVariantDark,
                                    contentColor = if (canAfford) PrimaryNeon else TextMuted
                                ),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = if (isMaxed) "MAX RANK" else "ENHANCE ${NumberFormatter.short(node.costGold)}",
                                    style = TrueIdleType.tag.copy(fontSize = 10.sp, letterSpacing = 0.2.sp, fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Current total effect of a percent-bounded node, shown under its description. */
internal fun trainingEffectLabel(id: String, rank: Int): String? = when (id) {
    "crit_bonus" -> "CURRENT +%.1f%% / MAX 100%% CRIT".format(rank * 0.5)
    "speed_bonus" -> "CURRENT +%d%% / MAX +%d%% SPEED".format(rank * 5, (TrainingCaps.SPEED_BONUS_CAP * 100).toInt())
    "luck_bonus" -> "CURRENT +%d%% / DROP CHANCE CAPS AT 100%%".format(rank * 5)
    "xp_bonus" -> "CURRENT +%s%% / MAX +100,000%% XP".format(NumberFormatter.short((rank * 2L).coerceAtMost(100_000L)))
    else -> null
}
