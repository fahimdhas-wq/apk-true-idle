package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.SurfaceRaised
import com.aistudio.trueidle.ui.theme.TextLight
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType

@Composable
fun SciFiGauge(
    label: String,
    valueText: String,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val r = ratio.coerceIn(0f, 1f)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label $valueText"
            progressBarRangeInfo = ProgressBarRangeInfo(r, 0f..1f)
        }
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = label,
                style = TrueIdleType.label,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                style = TrueIdleType.value,
                color = TextLight,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 72.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(SurfaceRaised)
                .drawBehind { drawRect(color, size = Size(size.width * r, size.height)) }
        )
    }
}
