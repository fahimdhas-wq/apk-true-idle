package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header with filters & clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("all" to "All", "combat" to "Combat", "loot" to "Loot", "system" to "System").forEach { (key, label) ->
                        val isSelected = (currentFilter == key)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFilterSelect(key) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("log_filter_$key"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (key) {
                                    "combat" -> HpRed.copy(alpha = 0.2f)
                                    "loot" -> SecondaryGold.copy(alpha = 0.2f)
                                    "system" -> PrimaryNeon.copy(alpha = 0.2f)
                                    else -> SurfaceVariantDark
                                },
                                selectedLabelColor = when (key) {
                                    "combat" -> HpRed
                                    "loot" -> SecondaryGold
                                    "system" -> PrimaryNeon
                                    else -> PrimaryNeon
                                }
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp).testTag("clear_logs_button")
                ) {
                    Text("✕", color = TextMuted, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Log messages list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF090C12), RoundedCornerShape(6.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                    .padding(6.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val textColor = when (log.type) {
                            "combat" -> HpRed
                            "gain", "loot" -> GreenSuccess
                            "system" -> PrimaryNeon
                            "necro" -> NecroPurple
                            else -> TextLight
                        }
                        Text(
                            text = "› ${log.text}",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
