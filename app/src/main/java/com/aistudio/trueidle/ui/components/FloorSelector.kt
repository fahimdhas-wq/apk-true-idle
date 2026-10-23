package com.aistudio.trueidle.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = modifier.fillMaxWidth().testTag("floor_controls_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onNavFloor(-1) },
                        enabled = currentFloor > 1,
                        modifier = Modifier.size(38.dp).testTag("floor_prev_button"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("◀", fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceVariantDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FLOOR $currentFloor",
                            style = MaterialTheme.typography.titleMedium,
                            color = PrimaryNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = { onNavFloor(1) },
                        enabled = currentFloor < maxFloor,
                        modifier = Modifier.size(38.dp).testTag("floor_next_button"),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("▶", fontSize = 14.sp)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (autoAdvance) "AUTO ON" else "AUTO OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (autoAdvance) GreenSuccess else TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = autoAdvance,
                        onCheckedChange = { onToggleAuto() },
                        modifier = Modifier.testTag("auto_advance_switch")
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
                    text = "Floor Monsters",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "$killsOnFloor / $killsRequired Kills",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryGold,
                    fontWeight = FontWeight.Bold
                )
            }
            val progress = (killsOnFloor.toFloat() / killsRequired.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF090C12))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(SecondaryGold)
                )
            }
        }
    }
}
