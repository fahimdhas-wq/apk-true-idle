package com.aistudio.trueidle.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import com.aistudio.trueidle.core.DerivedStats
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
            "combat" -> log.type == "combat"
            "loot" -> log.type == "loot" || log.type == "gain"
            "skills" -> log.type == "level" || log.type == "craft" || log.type == "tame"
            "system" -> log.type == "system" || log.type == "milestone"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() || log.text.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    val listState = rememberLazyListState()
    LaunchedEffect(filteredLogs.size) {
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
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Mission Play Time Dossier & Telemetry Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("◈", color = PrimaryNeon, style = TrueIdleType.tag)
                    Text("MISSION TELEMETRY TERMINAL", style = TrueIdleType.title, color = PrimaryNeon)
                }

                // Play Duration Counter in Orbitron Font
                val playTimeSeconds = state?.lifetime?.playTimeSeconds ?: 0L
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PLAY: ${NumberFormatter.duration(playTimeSeconds)}",
                        style = TrueIdleType.numberSmall,
                        color = SecondaryGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Tabs & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                .padding(horizontal = 6.dp, vertical = 4.dp)
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

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Export Telemetry Log
                    Box(
                        modifier = Modifier
                            .clickable {
                                val report = buildLogExportText(state, logs)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MissionLog", report))
                                exportFeedback = "EXPORTED!"
                                Toast.makeText(context, "Log Dossier Copied to Clipboard", Toast.LENGTH_SHORT).show()

                                // Also launch share dialog
                                try {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, report)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Mission Telemetry"))
                                } catch (_: Exception) {}
                            }
                            .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, PrimaryNeon, RectangleShape)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("export_logs_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (exportFeedback.isNotEmpty()) exportFeedback else "EXPORT", style = TrueIdleType.tag, color = PrimaryNeon)
                    }

                    // Clear Logs
                    Box(
                        modifier = Modifier
                            .clickable { onClear() }
                            .border(1.dp, BorderDark, RectangleShape)
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("clear_logs_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CLR", style = TrueIdleType.tag, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Terminal Display Feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RectangleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RectangleShape)
                    .padding(6.dp)
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
                        itemsIndexed(filteredLogs, key = { index, log -> "${log.id}_$index" }) { _, log ->
                            val (badgeLabel, accentColor) = when (log.type) {
                                "combat" -> "CMB" to HpRed
                                "loot", "gain" -> "LOT" to SecondaryGold
                                "level", "craft", "tame" -> "SKL" to GreenSuccess
                                "system", "milestone" -> "SYS" to PrimaryNeon
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

private fun buildLogExportText(state: GameState?, logs: List<LogEntry>): String {
    val sb = StringBuilder()
    sb.append("================================================================\n")
    sb.append("TRUE IDLE // TACTICAL MISSION TELEMETRY EXPORT\n")
    sb.append("================================================================\n")
    if (state != null) {
        val d = DerivedStats.of(state)
        val playDuration = NumberFormatter.duration(state.lifetime.playTimeSeconds)
        sb.append("PLAYER MISSION DOSSIER:\n")
        sb.append("- Total Play Duration: $playDuration\n")
        sb.append("- Current Sector     : Sector ${state.player.currentFloor} (Max: ${state.player.maxUnlockedFloor})\n")
        sb.append("- Combat Level       : Level ${state.player.lvl} (${state.player.xp} XP)\n")
        sb.append("- Gear Score         : ${d.gearScore} GS\n")
        sb.append("- Monsters Defeated  : ${state.lifetime.totalKills}\n")
        sb.append("- Credits Secured    : ${state.player.gold}\n")
        sb.append("- Bone Essence       : ${state.necro.boneEssence}\n")
        sb.append("- Active Beasts      : ${state.beasts.beasts.size}/8\n")
        sb.append("- Undead Legions     : ${state.necro.army.sumOf { it.count }}\n")
        sb.append("================================================================\n")
    }
    sb.append("RECORDED EVENT STREAM (${logs.size} EVENTS):\n")
    sb.append("================================================================\n")
    logs.forEach { log ->
        val timeStr = if (log.timestampMs > 0L) {
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(log.timestampMs))
        } else "--:--:--"
        sb.append("[$timeStr] [${log.type.uppercase()}] ${log.text}\n")
    }
    sb.append("================================================================\n")
    sb.append("END OF TELEMETRY ARCHIVE\n")
    sb.append("================================================================\n")
    return sb.toString()
}
