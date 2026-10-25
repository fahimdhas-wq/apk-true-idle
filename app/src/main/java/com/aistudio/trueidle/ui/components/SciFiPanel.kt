package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.*

@Composable
fun SciFiPanel(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = PrimaryNeon,
    tag: String? = null,
    containerColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedContainer = if (containerColor != Color.Unspecified) containerColor else MaterialTheme.colorScheme.surface
    val resolvedBorder = if (borderColor != Color.Unspecified) borderColor else MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(resolvedContainer)
            .border(BorderStroke(1.dp, resolvedBorder))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 14.dp)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = TrueIdleType.title,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (tag != null) {
                Text(
                    text = tag,
                    style = TrueIdleType.tag,
                    color = accent
                )
            }
        }
        content()
    }
}
