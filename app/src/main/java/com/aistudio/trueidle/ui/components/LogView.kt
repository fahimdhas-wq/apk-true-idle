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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.LogType
import com.aistudio.trueidle.core.NumberFormatter
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
    playTimeSeconds: Long,
    onExport: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var exportFeedback by remember { mutableStateOf("") }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val filteredLogs = remember(logs, currentFilter, searchQuery) {
        logs.filter { log ->
            val matchesCategory = when (currentFilter) {
                "combat" -> log.type == LogType.COMBAT
                "loot" -> log.type == LogType.LOOT || log.type == LogType.GAIN
                "skills" -> log.type == LogType.SKILL
                "system" -> log.type == LogType.SYSTEM || log.type == LogType.MILESTONE
                else -> true
            }
            matchesCategory && (searchQuery.isBlank() || log.text.contains(searchQuery, ignoreCase = true))
        }
    }

    val listState = rememberLazyListState()
    val lastId = filteredLogs.lastOrNull()?.id
    LaunchedEffect(lastId) {
        if (filteredLogs.isNotEmpty()) listState.scrollToItem(filteredLogs.size - 1)
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
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("◈", color = PrimaryNeon, style = TrueIdleType.tag)
                    Text(
                        "MISSION TELEMETRY",
                        style = TrueIdleType.title,
                        color = PrimaryNeon,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                // Play Duration Counter in Orbitron Font
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

            // Filters: five equal chips that share the full width, so none can be pushed off screen.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
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
                        else -> PrimaryNeon
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .clickable(role = Role.Tab) { onFilterSelect(key) }
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else Color(0xFF000000), RectangleShape)
                            .border(1.dp, if (isSelected) accentColor else BorderDark, RectangleShape)
                            .testTag("log_filter_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TrueIdleType.tag,
                            color = if (isSelected) accentColor else TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Actions on their own row: full-width, labelled, 48dp targets.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SciFiButton(
                    label = if (exportFeedback.isNotEmpty()) exportFeedback else "COPY LOG",
                    onClick = {
                        val report = onExport()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MissionLog", report))
                        exportFeedback = "COPIED"
                        Toast.makeText(context, "Log dossier copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).testTag("export_logs_button"),
                    color = PrimaryNeon
                )
                SciFiButton(
                    label = "CLEAR LOG",
                    onClick = onClear,
                    modifier = Modifier.weight(1f).testTag("clear_logs_button"),
                    color = HpRed
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Terminal Display Feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // Scaled by the system font size so a large-text user sees the same number of
                    // lines instead of clipped ones; bounded so it can never swallow the screen.
                    .height(
                        (145.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale)
                            .coerceIn(145.dp, 220.dp)
                    )
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
                                timeFormat.format(Date(log.timestampMs))
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
