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
    XpProgressBar(
        skillName = skillName,
        level = level,
        currentXp = currentXp.toDouble(),
        requiredXp = requiredXp.toDouble(),
        barColor = barColor,
        modifier = modifier,
        isMaxLevel = isMaxLevel
    )
}

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
    // Divide as Double before narrowing: late-game XP thresholds exceed Float.MAX_VALUE.
    val progress = when {
        isMaxLevel -> 1f
        requiredXp <= 0.0 || !requiredXp.isFinite() -> 0f
        else -> (currentXp.coerceAtLeast(0.0) / requiredXp).coerceIn(0.0, 1.0).toFloat()
    }
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "XpAnim")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Discipline name with consistent spacing and sci-fi prefix
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

            // Center-Right: Strictly aligned fixed-width Level Box
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

            // Right: XP telemetry. The width is bounded rather than merely minimum-bounded: the
            // label row is the weighted child, so an over-long readout can no longer push the
            // discipline name off a 360dp screen.
            Text(
                text = if (isMaxLevel) "MAX LEVEL" else "${NumberFormatter.short(currentXp)} / ${NumberFormatter.short(requiredXp)} (${(progress * 100).toInt()}%)",
                style = TrueIdleType.meta,
                color = TextMuted,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 96.dp, max = 136.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // High-Tech Cyber Bar with contrast background and precision borders
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
