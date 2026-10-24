package com.aistudio.trueidle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.trueidle.model.GameNotification
import com.aistudio.trueidle.ui.theme.*

@Composable
fun NotificationBanner(
    notifications: List<GameNotification>,
    onDismiss: (Long) -> Unit,
    onShowAll: (() -> Unit)? = null,
    historyCount: Int = 0,
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Notification Bar Header with "Show All" toggle
        if (onShowAll != null && historyCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE TELEMETRY FEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 8.sp,
                    letterSpacing = 0.8.sp
                )
                Box(
                    modifier = Modifier
                        .clickable { onShowAll() }
                        .background(PrimaryNeon.copy(alpha = 0.12f), RectangleShape)
                        .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("show_all_notifs_btn")
                ) {
                    Text(
                        text = "SHOW ALL ARCHIVE ($historyCount) ➔",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        notifications.forEach { notif ->
            val accentColor = when (notif.status) {
                "FAILED" -> HpRed
                "INFO" -> TextLight
                else -> when (notif.type) {
                    "gear", "craft" -> PrimaryNeon
                    "level", "gain" -> GreenSuccess
                    "tame", "milestone" -> SecondaryGold
                    "combat" -> HpRed
                    else -> NecroPurple
                }
            }

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("notif_${notif.id}"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF000000))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                                    letterSpacing = 0.8.sp,
                                    fontSize = 9.sp
                                )
                                if (notif.count > 1) {
                                    Box(
                                        modifier = Modifier
                                            .background(accentColor.copy(alpha = 0.25f), RectangleShape)
                                            .border(1.dp, accentColor, RectangleShape)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "x${notif.count}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight,
                                fontSize = 10.sp
                            )
                            if (notif.detail.isNotEmpty()) {
                                Text(
                                    text = "› ${notif.detail}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 8.sp
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onDismiss(notif.id) }
                                .border(1.dp, BorderDark, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
