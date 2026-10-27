package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.ui.theme.*

enum class SupportKind { BEAST, UNDEAD }

data class SupportItemUi(
    val name: String,
    val stars: Int,
    val dpsText: String,
    val meta: String,
    val wounded: Boolean = false
)

data class SupportGroupUi(
    val count: Int,
    val cap: Int,
    val totalDps: String,
    val emptyText: String,
    val top: List<SupportItemUi>
)

@Composable
fun SupportModule(
    beasts: SupportGroupUi,
    undead: SupportGroupUi,
    onManage: (SupportKind) -> Unit,
    modifier: Modifier = Modifier
) {
    var kind by rememberSaveable { mutableStateOf(SupportKind.BEAST) }
    val group = if (kind == SupportKind.BEAST) beasts else undead
    val accent = if (kind == SupportKind.BEAST) GreenSuccess else NecroPurple

    SciFiPanel(
        title = "SUPPORT",
        accent = accent,
        modifier = modifier
    ) {
        SegmentedTabs(
            options = listOf(
                SegOption("BEAST", "BEASTS ${beasts.count}/${beasts.cap}"),
                SegOption("UNDEAD", "UNDEAD ${undead.count}/${undead.cap}")
            ),
            selectedId = kind.name,
            onSelect = { kind = SupportKind.valueOf(it) }
        )
        Spacer(Modifier.height(6.dp))
        group.top.forEach { RosterRow(it) }
        if (group.top.isEmpty()) {
            Text(
                text = group.emptyText,
                style = TrueIdleType.body,
                color = TextMuted,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TOTAL SUPPORT DPS",
                style = TrueIdleType.label,
                color = TextMuted
            )
            Text(
                text = group.totalDps,
                style = TrueIdleType.value,
                color = accent
            )
        }
        Spacer(Modifier.height(3.dp))
        TextButton(
            onClick = { onManage(kind) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            Text("MANAGE SQUAD", style = TrueIdleType.tag, color = PrimaryNeon)
        }
    }
}
