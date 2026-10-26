package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SkillTelemetryStrip(
    status: String,
    level: Int,
    xp: Long,
    requiredXp: Long,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (requiredXp > 0) (xp.toFloat() / requiredXp.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(68.dp)
                        .background(accent.copy(alpha = 0.18f), RectangleShape)
                        .border(1.dp, accent, RectangleShape)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("LVL $level", style = TrueIdleType.tag, color = accent)
                }
                Text("// $status", style = TrueIdleType.tag, color = accent)
            }
            Text("${(progress * 100).toInt()}% SYNC", style = TrueIdleType.meta, color = TextMuted)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFF000000), RectangleShape)
                .border(1.dp, BorderDark, RectangleShape)
        ) {
            if (progress > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(accent, RectangleShape)
                )
            }
        }
    }
}
