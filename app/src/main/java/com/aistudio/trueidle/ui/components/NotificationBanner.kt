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
import androidx.compose.ui.unit.dp
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
        if (onShowAll != null && historyCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TELEMETRY FEED",
                    style = TrueIdleType.meta,
                    color = TextMuted
                )
                Box(
                    modifier = Modifier
                        .clickable { onShowAll() }
                        .background(PrimaryNeon.copy(alpha = 0.12f), RectangleShape)
                        .border(1.dp, PrimaryNeon.copy(alpha = 0.5f), RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("show_all_notifs_btn")
                ) {
                    Text(
                        text = "ARCHIVE ($historyCount) ➔",
                        style = TrueIdleType.tag,
                        color = PrimaryNeon
                    )
                }
            }
        }

        notifications.forEach { notif ->
            val accentColor = when (notif.status) {
                "FAILED" -> HpRed
                "INFO" -> TextLight
                "ALERT" -> Warn
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notif_${notif.id}"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF000000))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                                    Box(
                                        modifier = Modifier
                                            .background(accentColor.copy(alpha = 0.25f), RectangleShape)
                                            .border(1.dp, accentColor, RectangleShape)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "x${notif.count}",
                                            style = TrueIdleType.tag,
                                            color = accentColor
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.message,
                                style = TrueIdleType.body,
                                color = TextLight
                            )
                            if (notif.detail.isNotEmpty()) {
                                Text(
                                    text = "› ${notif.detail}",
                                    style = TrueIdleType.meta,
                                    color = TextMuted
                                )
                            }
                        }

                        // Close button with >= 48dp touch target
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onDismiss(notif.id) }
                                .padding(8.dp)
                                .border(1.dp, BorderDark, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                style = TrueIdleType.tag,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
