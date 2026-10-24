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
import androidx.compose.ui.unit.dp
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
    }.reversed()

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
                    .padding(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTIFICATION ARCHIVE",
                        style = TrueIdleType.title,
                        color = PrimaryNeon
                    )
                    Box(
                        modifier = Modifier
                            .background(PrimaryNeon.copy(alpha = 0.15f), RectangleShape)
                            .border(1.dp, PrimaryNeon, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${history.size} LOGGED",
                            style = TrueIdleType.tag,
                            color = PrimaryNeon
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                .heightIn(min = 40.dp)
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
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = f,
                                style = TrueIdleType.tag,
                                color = if (isSelected) PrimaryNeon else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            text = "NO TELEMETRY RECORDED",
                            style = TrueIdleType.body,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { notif ->
                            val accentColor = when (notif.status) {
                                "SUCCESS" -> if (notif.type == "level") GreenSuccess else PrimaryNeon
                                "FAILED" -> HpRed
                                "INFO" -> TextLight
                                "ALERT" -> Warn
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
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(accentColor.copy(alpha = 0.15f), RectangleShape)
                                                    .border(1.dp, accentColor, RectangleShape)
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = notif.status,
                                                    style = TrueIdleType.tag,
                                                    color = accentColor
                                                )
                                            }
                                            Text(
                                                text = notif.title.uppercase(),
                                                style = TrueIdleType.tag,
                                                color = accentColor
                                            )
                                            if (notif.count > 1) {
                                                Text(
                                                    text = "[x${notif.count}]",
                                                    style = TrueIdleType.tag,
                                                    color = SecondaryGold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notif.message,
                                        style = TrueIdleType.body,
                                        color = TextLight
                                    )
                                    if (notif.detail.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "› ${notif.detail}",
                                            style = TrueIdleType.meta,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("clear_history_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = TextMuted
                        )
                    ) {
                        Text("CLEAR HISTORY", style = TrueIdleType.tag)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("close_history_btn"),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, PrimaryNeon),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = PrimaryNeon.copy(alpha = 0.15f),
                            contentColor = PrimaryNeon
                        )
                    ) {
                        Text("DISMISS", style = TrueIdleType.tag)
                    }
                }
            }
        }
    }
}
