package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.ui.theme.*

@Composable
fun LogView(
    logs: List<LogEntry>,
    currentFilter: String,
    onFilterSelect: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredLogs = when (currentFilter) {
        "combat" -> logs.filter { it.type == "combat" }
        "loot" -> logs.filter { it.type == "loot" || it.type == "gain" }
        "system" -> logs.filter { it.type == "system" }
        else -> logs
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("combat_logs_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header with filters & clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("all" to "ALL", "combat" to "COMBAT", "loot" to "LOOT", "system" to "SYSTEM").forEach { (key, label) ->
                        val isSelected = (currentFilter == key)
                        val accentColor = when (key) {
                            "combat" -> HpRed
                            "loot" -> SecondaryGold
                            "system" -> PrimaryNeon
                            else -> PrimaryNeon
                        }

                        Box(
                            modifier = Modifier
                                .clickable { onFilterSelect(key) }
                                .background(if (isSelected) accentColor.copy(alpha = 0.15f) else SurfaceVariantDark, RectangleShape)
                                .border(1.dp, if (isSelected) accentColor else BorderDark, RectangleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("log_filter_$key")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) accentColor else TextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clickable { onClear() }
                        .border(1.dp, BorderDark, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("clear_logs_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CLR", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Log messages list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF000000), RectangleShape)
                    .border(1.dp, BorderDark, RectangleShape)
                    .padding(6.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredLogs, key = { index, log -> "${log.id}_$index" }) { _, log ->
                        val textColor = when (log.type) {
                            "combat" -> HpRed
                            "gain", "loot" -> GreenSuccess
                            "system" -> PrimaryNeon
                            "necro" -> NecroPurple
                            else -> TextLight
                        }
                        Text(
                            text = "› ${log.text}",
                            style = TrueIdleType.meta,
                            color = textColor,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
