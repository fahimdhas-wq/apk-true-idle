package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aistudio.trueidle.model.GameNotification
import com.aistudio.trueidle.ui.theme.*

@Composable
fun NotificationHistoryDialog(
    history: List<GameNotification>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "GEAR", "CRAFT", "LEVEL", "MILESTONE")

    val filteredList = when (selectedFilter) {
        "GEAR" -> history.filter { it.type == "gear" }
        "CRAFT" -> history.filter { it.type == "craft" }
        "LEVEL" -> history.filter { it.type == "level" || it.type == "gain" }
        "MILESTONE" -> history.filter { it.type == "milestone" || it.type == "tame" }
        else -> history
    }.reversed() // Most recent first

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(4.dp)
                .testTag("notification_history_dialog"),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RectangleShape,
            border = BorderStroke(1.5.dp, PrimaryNeon)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF000000))
                    .padding(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("◈ ", color = PrimaryNeon, fontSize = 14.sp)
                        Text(
                            text = "NOTIFICATION ARCHIVE",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, PrimaryNeon, RectangleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${history.size} LOGGED",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filters.forEach { f ->
                        val isSelected = (selectedFilter == f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = f }
                                .background(
                                    if (isSelected) PrimaryNeon.copy(alpha = 0.18f) else SurfaceVariantDark,
                                    RectangleShape
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryNeon else BorderDark,
                                    RectangleShape
                                )
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = f,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) PrimaryNeon else TextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 8.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content list
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(SurfaceDark, RectangleShape)
                            .border(1.dp, BorderDark, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO TELEMETRY RECORDED IN THIS LOG",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList, key = { it.id }) { notif ->
                            val accentColor = when (notif.status) {
                                "SUCCESS" -> if (notif.type == "level") GreenSuccess else PrimaryNeon
                                "FAILED" -> HpRed
                                "INFO" -> TextLight
                                else -> SecondaryGold
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RectangleShape,
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(accentColor.copy(alpha = 0.15f), RectangleShape)
                                                    .border(1.dp, accentColor, RectangleShape)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = notif.status,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = accentColor,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp
                                                )
                                            }
                                            Text(
                                                text = notif.title.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accentColor,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            if (notif.count > 1) {
                                                Text(
                                                    text = "[x${notif.count}]",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SecondaryGold,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 8.sp
                                                )
                                            }
                                        }

                                        val agoSec = ((System.currentTimeMillis() - notif.timestamp) / 1000L).coerceAtLeast(0L)
                                        val timeStr = if (agoSec < 60) "${agoSec}s ago" else "${agoSec / 60}m ago"
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 8.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = notif.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextLight,
                                        fontSize = 10.sp
                                    )
                                    if (notif.detail.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "› ${notif.detail}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier.weight(1f).testTag("clear_history_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = TextMuted
                        ),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("CLEAR HISTORY", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("close_history_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, PrimaryNeon),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PrimaryNeon.copy(alpha = 0.15f),
                            contentColor = PrimaryNeon
                        ),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text("DISMISS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
