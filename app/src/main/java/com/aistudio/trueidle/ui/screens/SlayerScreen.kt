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
import com.aistudio.trueidle.core.TierConfig
import com.aistudio.trueidle.core.XpCurves
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.SlayerTask
import com.aistudio.trueidle.ui.components.SciFiBadge
import com.aistudio.trueidle.ui.components.SciFiIconType
import com.aistudio.trueidle.ui.components.SciFiGauge
import com.aistudio.trueidle.ui.components.SciFiPanel
import com.aistudio.trueidle.ui.components.XpProgressBar
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SlayerScreen(
    state: GameState,
    onRerollTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val slayer = state.slayer
    val reqXp = XpCurves.slayer(slayer.lvl)
    val tasks = slayer.tasks.ifEmpty { listOf(slayer.currentTask) }

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
                            text = "LVL ${slayer.lvl}",
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
            tag = "${slayer.slayerPoints} POINTS"
        ) {
            Text(
                text = "Targeted eradication contracts awarded by the Tactical Command. Up to 10 missions are tracked simultaneously, granting gold, points, and bonus spoils with automatic claims on completion.",
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
                    label = "${slayer.slayerPoints} PTS",
                    iconTint = SecondaryGold,
                    textColor = SecondaryGold
                )
            }
        }

        // Mastery scaling
        SciFiPanel(
            title = "MASTERY SCALING",
            accent = PrimaryNeon,
            tag = "LV ${slayer.lvl}"
        ) {
            Text(
                text = "Each level above 1 adds +2% credits to a new contract, and every ${SlayerMath.LEVELS_PER_BONUS_POINT} levels adds +1 bounty point. Contracts ranked D+ or higher grant Diamonds, Faith, and Bone Essences.",
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
                text = "NEXT POINT BONUS AT SLAYER LV ${SlayerMath.nextBonusPointLevel(slayer.lvl)}",
                style = TrueIdleType.tag,
                color = TextMuted
            )
        }

        // 10 Active Mission Directives
        Text(
            text = "ACTIVE CONTRACTS (${tasks.size} / 10)",
            style = TrueIdleType.title,
            color = TextLight,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )

        tasks.forEachIndexed { index, task ->
            SlayerTaskCard(
                task = task,
                canReroll = state.player.gold >= 1L,
                onReroll = { onRerollTask(task.id) }
            )
        }
    }
}


@Composable
private fun SlayerTaskCard(
    task: SlayerTask,
    canReroll: Boolean,
    onReroll: () -> Unit
) {
    val tierColor = Color(TierConfig.colorOf(task.tier))
    val taskProgressRatio = if (task.requiredKills > 0)
        task.currentKills.toFloat() / task.requiredKills.toFloat()
    else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("slayer_task_card_${task.id}"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape,
        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header row with Tier tag, Monster name and Sector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(tierColor.copy(alpha = 0.2f), RectangleShape)
                            .border(1.dp, tierColor, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "[${task.tier}]",
                            style = TrueIdleType.tag,
                            color = tierColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = task.targetName.uppercase(),
                        style = TrueIdleType.title,
                        color = TextLight
                    )
                }
                Text(
                    text = "SECTOR ${task.targetFloor}",
                    style = TrueIdleType.tag,
                    color = PrimaryNeon
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            SciFiGauge(
                label = "ELIMINATION PROGRESS",
                valueText = "${task.currentKills} / ${task.requiredKills}",
                ratio = taskProgressRatio.coerceIn(0f, 1f),
                color = tierColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Bounty rewards grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariantDark, RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("BOUNTY (CREDITS & PTS):", style = TrueIdleType.meta, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SciFiBadge(
                            type = SciFiIconType.CREDIT,
                            label = "+${NumberFormatter.short(task.rewardGold)}",
                            iconTint = SecondaryGold,
                            textColor = SecondaryGold
                        )
                        SciFiBadge(
                            type = SciFiIconType.WEAPON,
                            label = "+${task.rewardPoints} PTS",
                            iconTint = HpRed,
                            textColor = HpRed
                        )
                    }
                }

                // High-Tier Bonus Spoils
                if (task.rewardDiamonds > 0L || task.rewardFaith > 0L || task.rewardBones > 0L) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SPECIAL SPOILS:", style = TrueIdleType.meta, color = TextMuted)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (task.rewardDiamonds > 0L) {
                                SciFiBadge(
                                    type = SciFiIconType.DIAMOND,
                                    label = "+${task.rewardDiamonds}",
                                    iconTint = PrimaryNeon,
                                    textColor = PrimaryNeon
                                )
                            }
                            if (task.rewardFaith > 0L) {
                                SciFiBadge(
                                    type = SciFiIconType.ESSENCE,
                                    label = "+${task.rewardFaith}",
                                    iconTint = ManaTeal,
                                    textColor = ManaTeal
                                )
                            }
                            if (task.rewardBones > 0L) {
                                SciFiBadge(
                                    type = SciFiIconType.WEAPON,
                                    label = "+${task.rewardBones}",
                                    iconTint = NecroPurple,
                                    textColor = NecroPurple
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions row: auto claim notice + 1 Gold manual refresh
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUTO-CLAIM ON COMPLETION",
                    style = TrueIdleType.meta,
                    color = GreenSuccess
                )

                Button(
                    onClick = onReroll,
                    enabled = canReroll,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("reroll_task_button_${task.id}"),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryGold.copy(alpha = 0.2f),
                        contentColor = SecondaryGold,
                        disabledContainerColor = SurfaceVariantDark,
                        disabledContentColor = TextMuted
                    ),
                    border = BorderStroke(1.dp, if (canReroll) SecondaryGold else BorderDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "REROLL (1 GOLD)",
                        style = TrueIdleType.tag
                    )
                }
            }
        }
    }
}
