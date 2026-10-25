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
    modifier: Modifier = Modifier
) {
    val progress = if (requiredXp > 0) (currentXp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f) else 0f
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
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

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
                    text = "LVL $level",
                    style = TrueIdleType.tag,
                    color = barColor
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: XP Telemetry with fixed minimum width and right alignment
            Text(
                text = "${NumberFormatter.short(currentXp)} / ${NumberFormatter.short(requiredXp)} (${(progress * 100).toInt()}%)",
                style = TrueIdleType.meta,
                color = TextMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 100.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // High-Tech Cyber Bar with contrast background and precision borders
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
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
