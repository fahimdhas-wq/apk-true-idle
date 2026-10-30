package com.aistudio.trueidle.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.ui.theme.BorderDark
import com.aistudio.trueidle.ui.theme.PrimaryNeon
import com.aistudio.trueidle.ui.theme.TextLight
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType

/** Exact Long formatter path retained for all non-harvesting skills. */
@Composable
fun XpProgressBar(
    skillName: String,
    level: Int,
    currentXp: Long,
    requiredXp: Long,
    barColor: Color = PrimaryNeon,
    modifier: Modifier = Modifier,
    isMaxLevel: Boolean = false
) {
    val progress = when {
        isMaxLevel -> 1f
        requiredXp <= 0L -> 0f
        else -> (currentXp.toDouble() / requiredXp.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }
    XpProgressBarContent(
        skillName = skillName,
        level = level,
        currentXpText = NumberFormatter.short(currentXp),
        requiredXpText = NumberFormatter.short(requiredXp),
        progress = progress,
        barColor = barColor,
        modifier = modifier,
        isMaxLevel = isMaxLevel
    )
}

/** Double precision path for the arbitrary-scale Farming and Mining XP banks. */
@Composable
fun XpProgressBar(
    skillName: String,
    level: Int,
    currentXp: Double,
    requiredXp: Double,
    barColor: Color = PrimaryNeon,
    modifier: Modifier = Modifier,
    isMaxLevel: Boolean = false
) {
    val progress = when {
        isMaxLevel -> 1f
        requiredXp <= 0.0 || !requiredXp.isFinite() -> 0f
        else -> {
            val safeCurrentXp = when {
                currentXp.isNaN() || currentXp <= 0.0 -> 0.0
                currentXp.isInfinite() -> requiredXp
                else -> currentXp
            }
            (safeCurrentXp / requiredXp).coerceIn(0.0, 1.0).toFloat()
        }
    }
    XpProgressBarContent(
        skillName = skillName,
        level = level,
        currentXpText = NumberFormatter.short(currentXp),
        requiredXpText = NumberFormatter.short(requiredXp),
        progress = progress,
        barColor = barColor,
        modifier = modifier,
        isMaxLevel = isMaxLevel
    )
}

@Composable
private fun XpProgressBarContent(
    skillName: String,
    level: Int,
    currentXpText: String,
    requiredXpText: String,
    progress: Float,
    barColor: Color,
    modifier: Modifier,
    isMaxLevel: Boolean
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "XpAnim")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "◈ ${skillName.uppercase()}",
                    style = TrueIdleType.tag,
                    color = TextLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .width(68.dp)
                    .background(barColor.copy(alpha = 0.18f), RectangleShape)
                    .border(1.dp, barColor, RectangleShape)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMaxLevel) "MAX" else "LVL $level",
                    style = TrueIdleType.tag,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = if (isMaxLevel) "MAX LEVEL" else "$currentXpText / $requiredXpText (${(progress * 100).toInt()}%)",
                style = TrueIdleType.meta,
                color = TextMuted,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 96.dp, max = 136.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF000000), RectangleShape)
                .border(1.dp, BorderDark, RectangleShape)
        ) {
            if (animatedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .background(barColor, RectangleShape)
                )
            }
        }
    }
}
