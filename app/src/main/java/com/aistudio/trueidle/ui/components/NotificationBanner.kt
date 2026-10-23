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
    modifier: Modifier = Modifier
) {
    if (notifications.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        notifications.forEach { notif ->
            val accentColor = when (notif.type) {
                "gear", "craft" -> PrimaryNeon
                "level", "gain" -> GreenSuccess
                "tame", "milestone" -> SecondaryGold
                "combat" -> HpRed
                else -> NecroPurple
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
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = notif.title.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                if (notif.count > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(accentColor.copy(alpha = 0.2f), RectangleShape)
                                            .border(1.dp, accentColor, RectangleShape)
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "x${notif.count}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notif.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLight,
                                fontSize = 11.sp
                            )
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onDismiss(notif.id) }
                                .border(1.dp, BorderDark, RectangleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "✕",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
