package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.PrimaryNeon
import com.aistudio.trueidle.ui.theme.SurfaceDark
import com.aistudio.trueidle.ui.theme.SurfaceRaised
import com.aistudio.trueidle.ui.theme.TextMuted
import com.aistudio.trueidle.ui.theme.TrueIdleType

data class SegOption(val id: String, val label: String)

@Composable
fun SegmentedTabs(
    options: List<SegOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(SurfaceDark),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { opt ->
            val selected = (opt.id == selectedId)
            val bg = if (selected) SurfaceRaised else SurfaceDark
            val textCol = if (selected) PrimaryNeon else TextMuted
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(bg)
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(opt.id) },
                        role = Role.Tab
                    )
                    .drawBehind {
                        if (selected) {
                            drawRect(
                                color = PrimaryNeon,
                                topLeft = Offset(0f, size.height - 2.dp.toPx()),
                                size = Size(size.width, 2.dp.toPx())
                            )
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt.label,
                    style = TrueIdleType.tag,
                    color = textCol,
                    maxLines = 1
                )
            }
        }
    }
}
