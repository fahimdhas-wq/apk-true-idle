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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.ui.theme.BorderDark
import com.aistudio.trueidle.ui.theme.TextLight
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType

@Composable
fun StatBar(
    label: String,
    current: Double,
    max: Double,
    fillColor: Color,
    modifier: Modifier = Modifier,
    tag: String = "stat_bar"
) {
    val progress = if (max > 0) (current / max).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "BarAnim")

    val currentText = NumberFormatter.short(current.coerceAtLeast(0.0))
    val maxText = NumberFormatter.short(max.coerceAtLeast(1.0))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = TrueIdleType.label,
                color = TextMuted,
                maxLines = 1
            )
            Text(
                text = "$currentText / $maxText",
                style = TrueIdleType.value,
                color = TextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFF000000))
                .border(1.dp, BorderDark, RectangleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .background(fillColor)
            )
        }
    }
}
