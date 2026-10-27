package com.aistudio.trueidle.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.Telemetry
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LogEntry
import com.aistudio.trueidle.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogView(
    logs: List<LogEntry>,
    currentFilter: String,
    onFilterSelect: (String) -> Unit,
    onClear: () -> Unit,
    state: GameState? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var exportFeedback by remember { mutableStateOf("") }

    val filteredLogs = logs.filter { log ->
        val matchesCategory = when (currentFilter) {
            "combat" -> log.type == LogType.COMBAT
            "loot" -> log.type == LogType.LOOT || log.type == LogType.GAIN
            // Skill level-ups and tames are emitted as "skill"/"loot"; the old filter matched
            // types the engine never produced, so it was always empty.
            "skills" -> log.type == LogType.SKILL
            "system" -> log.type == LogType.SYSTEM || log.type == LogType.MILESTONE
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() || log.text.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val listState = rememberLazyListState()
    // Keyed on the newest id: the list is capped at 40 entries, so its size stops changing and a
    // size-keyed effect would never fire again.
    LaunchedEffect(filteredLogs.lastOrNull()?.id) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("combat_logs_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RectangleShape,
        border = BorderStroke(1.dp, PrimaryNeon.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header: Mission Play Time Dossier & Telemetry Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("◈", color = PrimaryNeon, style = TrueIdleType.tag)
                    Text("MISSION TELEMETRY TERMINAL", style = TrueIdleType.title, color = PrimaryNeon)
                }

                // Play Duration Counter in Orbitron Font
                val playTimeSeconds = state?.lifetime?.playTimeSeconds ?: 0L
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PLAY: ${NumberFormatter.duration(playTimeSeconds)}",
                        style = TrueIdleType.numberSmall,
                        color = SecondaryGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Tabs & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf(
                        "all" to "ALL",
                        "combat" to "COMBAT",
                        "loot" to "LOOT",
                        "skills" to "SKILLS",
                        "system" to "SYSTEM"
                    ).forEach { (key, label) ->
                        val isSelected = (currentFilter == key)
                        val accentColor = when (key) {
                            "combat" -> HpRed
                            "loot" -> SecondaryGold
                            "skills" -> GreenSuccess
                            "system" -> PrimaryNeon
                            else -> PrimaryNeon
                        }

                        Box(
                            modifier = Modifier
                                .clickable { onFilterSelect(key) }
                                .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF000000), RectangleShape)
                                .border(1.dp, if (isSelected) accentColor else BorderDark, RectangleShape)
                                .padding(horizontal = 4.dp, vertical = 3.dp)
                                .testTag("log_filter_$key")
                        ) {
                            Text(
                                text = label,
                                style = TrueIdleType.tag,
                                color = if (isSelected) accentColor else TextMuted
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    // Export Telemetry Log
                    Box(
                        modifier = Modifier
                            .clickable {
                                val report = Telemetry.report(state, logs)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MissionLog", report))
                                exportFeedback = "COPIED!"
                                Toast.makeText(context, "Log Dossier Copied to Clipboard", Toast.LENGTH_SHORT).show()

                                
                            }
                            .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, PrimaryNeon, RectangleShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("export_logs_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (exportFeedback.isNotEmpty()) exportFeedback else "COPY", style = TrueIdleType.tag, color = PrimaryNeon)
                    }

                    // Clear Logs
                    Box(
                        modifier = Modifier
                            .clickable { onClear() }
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("clear_logs_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CLR", style = TrueIdleType.tag, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Terminal Display Feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .padding(4.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO TELEMETRY MATCHING CURRENT PROTOCOL", style = TrueIdleType.meta, color = TextMuted)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(filteredLogs, key = { _, log -> log.id }) { _, log ->
                            val (badgeLabel, accentColor) = when (log.type) {
                                LogType.COMBAT -> "CMB" to HpRed
                                LogType.LOOT, LogType.GAIN -> "LOT" to SecondaryGold
                                LogType.SKILL -> "SKL" to GreenSuccess
                                LogType.SYSTEM, LogType.MILESTONE -> "SYS" to PrimaryNeon
                                else -> "INFO" to MaterialTheme.colorScheme.onSurface
                            }

                            val timeStr = if (log.timestampMs > 0L) {
                                SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestampMs))
                            } else "--:--:--"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[$timeStr] ",
                                    style = TrueIdleType.monoTimestamp,
                                    color = TextMuted
                                )
                                Box(
                                    modifier = Modifier
                                        .background(accentColor.copy(alpha = 0.15f), RectangleShape)
                                        .border(1.dp, accentColor.copy(alpha = 0.4f), RectangleShape)
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        style = TrueIdleType.monoTimestamp,
                                        color = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = log.text,
                                    style = TrueIdleType.meta,
                                    color = if (log.type == "combat") MaterialTheme.colorScheme.onSurface else accentColor,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
