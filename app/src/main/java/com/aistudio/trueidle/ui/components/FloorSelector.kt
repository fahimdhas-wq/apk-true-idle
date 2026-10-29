package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.trueidle.core.NumberFormatter
import com.aistudio.trueidle.ui.theme.*

@Composable
fun FloorControls(
    currentFloor: Int,
    maxFloor: Int,
    autoAdvance: Boolean,
    killsOnFloor: Int,
    killsRequired: Int,
    onNavFloor: (Int) -> Unit,
    onToggleAuto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("floor_controls_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RectangleShape,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavFloor(-1) },
                        enabled = currentFloor > 1,
                        modifier = Modifier
                            .size(width = 40.dp, height = 40.dp)
                            .testTag("floor_prev_button"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (currentFloor > 1) PrimaryNeon.copy(alpha = 0.5f) else BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = PrimaryNeon
                        )
                    ) {
                        Text("◄", style = TrueIdleType.tag)
                    }

                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .background(Color(0xFF000000), RectangleShape)
                            .border(1.dp, PrimaryNeon.copy(alpha = 0.6f), RectangleShape)
                            .padding(horizontal = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FLOOR ${currentFloor.toString().padStart(2, '0')}",
                            style = TrueIdleType.title,
                            color = PrimaryNeon
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavFloor(1) },
                        enabled = currentFloor < maxFloor,
                        modifier = Modifier
                            .size(width = 40.dp, height = 40.dp)
                            .testTag("floor_next_button"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (currentFloor < maxFloor) PrimaryNeon.copy(alpha = 0.5f) else BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = PrimaryNeon
                        )
                    ) {
                        Text("►", style = TrueIdleType.tag)
                    }
                }

                OutlinedButton(
                    onClick = { onToggleAuto() },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("auto_advance_switch"),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, if (autoAdvance) GreenSuccess else BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (autoAdvance) GreenSuccess.copy(alpha = 0.12f) else SurfaceVariantDark,
                        contentColor = if (autoAdvance) GreenSuccess else TextMuted
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (autoAdvance) "AUTO: ON" else "AUTO: OFF",
                        style = TrueIdleType.tag
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Wave Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CLEAR PROGRESS",
                    style = TrueIdleType.label,
                    color = TextMuted
                )
                Text(
                    text = "${NumberFormatter.short(killsOnFloor)} / ${NumberFormatter.short(killsRequired)} TARGETS",
                    style = TrueIdleType.value,
                    color = SecondaryGold
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            val progress = (killsOnFloor.toFloat() / killsRequired.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF000000))
                    .border(1.dp, BorderDark, RectangleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(SecondaryGold)
                )
            }
        }
    }
}
