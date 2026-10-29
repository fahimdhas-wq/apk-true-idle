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
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.core.SlayerMath
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.SciFiGauge
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SlayerScreen(
    state: GameState,
    onRerollTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slayer = state.slayer
    val task = slayer.currentTask
    val reqXp = XpCurves.slayer(slayer.lvl)
    val taskProgressRatio = if (task.requiredKills > 0)
        task.currentKills.toFloat() / task.requiredKills.toFloat()
    else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Slayer Header Card with XP Bar
        Card(
            modifier = Modifier.fillMaxWidth().testTag("slayer_header_card"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.dp, HpRed.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SLAYER PROTOCOL",
                        style = TrueIdleType.title,
                        color = HpRed
                    )
                    Box(
                        modifier = Modifier
                            .background(HpRed.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, HpRed, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LVL ${NumberFormatter.short(slayer.lvl)}",
                            style = TrueIdleType.tag,
                            color = HpRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                XpProgressBar(
                    skillName = "SLAYER EXPERIENCE",
                    level = slayer.lvl,
                    currentXp = slayer.xp,
                    requiredXp = reqXp,
                    barColor = HpRed
                )
            }
        }

        // Slayer Points Badge
        SciFiPanel(
            title = "SLAYER CREDENTIALS",
            accent = SecondaryGold,
            tag = "${NumberFormatter.short(slayer.slayerPoints)} POINTS"
        ) {
            Text(
                text = "Targeted eradication contracts awarded by the Tactical Command. Complete directives to earn gold, points, and permanent mastery.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF000000), RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ACCREDITED SLAYER POINTS:", style = TrueIdleType.label, color = TextMuted)
                SciFiBadge(
                    type = SciFiIconType.WEAPON,
                    label = "${NumberFormatter.short(slayer.slayerPoints)} PTS",
                    iconTint = SecondaryGold,
                    textColor = SecondaryGold
                )
            }
        }

        // Mastery scaling: the same table the engine scales a new contract's bounty with.
        SciFiPanel(
            title = "MASTERY SCALING",
            accent = PrimaryNeon,
            tag = "LV ${NumberFormatter.short(slayer.lvl)}"
        ) {
            Text(
                text = "Each level above 1 adds +2% credits to a new contract, and every ${SlayerMath.LEVELS_PER_BONUS_POINT} levels adds +1 bounty point.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("POINT BONUS", style = TrueIdleType.meta, color = TextMuted)
                    Text("+${SlayerMath.bonusPoints(slayer.lvl)} PTS", style = TrueIdleType.value, color = PrimaryNeon)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("CREDIT BONUS", style = TrueIdleType.meta, color = TextMuted)
                    Text("+${SlayerMath.bonusGoldPercent(slayer.lvl)}%", style = TrueIdleType.value, color = SecondaryGold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "NEXT POINT BONUS AT SLAYER LV ${NumberFormatter.short(SlayerMath.nextBonusPointLevel(slayer.lvl))}",
                style = TrueIdleType.tag,
                color = TextMuted
            )
        }

        // Active Directive Card
        SciFiPanel(
            title = "ACTIVE ASSIGNMENT",
            accent = HpRed,
            tag = if (task.isCompleted) "FULFILLED" else "IN PROGRESS"
        ) {
            Text(
                text = "TARGET: ${task.targetName.uppercase()}",
                style = TrueIdleType.title,
                color = TextLight
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Locate and neutralize ${NumberFormatter.short(task.requiredKills)} specimens of this species in the dungeon sectors.",
                style = TrueIdleType.body,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))

            SciFiGauge(
                label = "ELIMINATION PROGRESS",
                valueText = "${NumberFormatter.short(task.currentKills)} / ${NumberFormatter.short(task.requiredKills)}",
                ratio = taskProgressRatio,
                color = HpRed
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariantDark, RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("BOUNTY (CREDITS):", style = TrueIdleType.meta, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    SciFiBadge(
                        type = SciFiIconType.CREDIT,
                        label = "+${NumberFormatter.short(task.rewardGold)}",
                        iconTint = SecondaryGold,
                        textColor = SecondaryGold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("BOUNTY (POINTS):", style = TrueIdleType.meta, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    SciFiBadge(
                        type = SciFiIconType.WEAPON,
                        label = "+${NumberFormatter.short(task.rewardPoints)} PTS",
                        iconTint = HpRed,
                        textColor = HpRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val canReroll = slayer.slayerPoints >= 1
            Button(
                onClick = onRerollTask,
                enabled = canReroll,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("reroll_task_button"),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondaryGold.copy(alpha = 0.2f),
                    contentColor = SecondaryGold,
                    disabledContainerColor = SurfaceVariantDark,
                    disabledContentColor = TextMuted
                ),
                border = BorderStroke(1.dp, if (canReroll) SecondaryGold else BorderDark)
            ) {
                Text(
                    text = if (canReroll) "REROLL DIRECTIVE (COST 1 POINT)" else "REROLL (REQUIRES 1 POINT)",
                    style = TrueIdleType.tag
                )
            }
        }
    }
}
