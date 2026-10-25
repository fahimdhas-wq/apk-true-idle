package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("LV $level  //  $status", style = TrueIdleType.tag, color = accent)
            Text("${(progress * 100).toInt()}% SYNC", style = TrueIdleType.meta, color = TextMuted)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).background(SurfaceRaised)) {
            if (progress > 0f) Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(accent))
        }
    }
}
