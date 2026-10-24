package com.aistudio.trueidle.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.ui.theme.BorderDark
import com.aistudio.trueidle.ui.theme.PrimaryNeon
import com.aistudio.trueidle.ui.theme.TextLight
import com.aistudio.trueidle.ui.theme.TextMuted

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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = skillName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLight,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(barColor.copy(alpha = 0.15f), RectangleShape)
                        .border(1.dp, barColor, RectangleShape)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "LVL $level",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }
            Text(
                text = "$currentXp / $requiredXp XP (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontSize = 9.sp
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
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
                    .fillMaxWidth(animatedProgress)
                    .background(barColor, RectangleShape)
            )
        }
    }
}
