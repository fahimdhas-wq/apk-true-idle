package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.*

@Composable
fun RosterRow(
    item: SupportItemUi,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = TrueIdleType.label,
                    color = TextLight
                )
                if (item.stars > 1) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "★".repeat(item.stars),
                        style = TrueIdleType.tag,
                        color = SecondaryGold
                    )
                }
            }
            if (item.meta.isNotEmpty()) {
                Text(
                    text = item.meta,
                    style = TrueIdleType.meta,
                    color = TextMuted
                )
            }
        }
        Text(
            text = item.dpsText,
            style = TrueIdleType.value,
            color = PrimaryNeon
        )
    }
}
